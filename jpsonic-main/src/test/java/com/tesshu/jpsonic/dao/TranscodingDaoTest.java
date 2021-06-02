/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.Transcoding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit test of {@link TranscodingDao}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class TranscodingDaoTest {

    @Autowired
    private GenericDaoHelper daoHelper;

    @Autowired
    private TranscodingDao transcodingDao;

    @Autowired
    private PlayerDao playerDao;

    @BeforeEach
    public void setUp() {
        daoHelper.getJdbcTemplate().execute("delete from transcoding2");
    }

    @Test
    void testCreateTranscoding() {
        Transcoding transcoding = new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2",
                "step3", false);
        transcodingDao.createTranscoding(transcoding);

        Transcoding newTranscoding = transcodingDao.getAllTranscodings().get(0);
        assertTranscodingEquals(transcoding, newTranscoding);
    }

    @Test
    void testUpdateTranscoding() {
        Transcoding transcoding = new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2",
                "step3", false);
        transcodingDao.createTranscoding(transcoding);
        transcoding = transcodingDao.getAllTranscodings().get(0);

        transcoding.setName("newName");
        transcoding.setSourceFormats("newSourceFormats");
        transcoding.setTargetFormat("newTargetFormats");
        transcoding.setStep1("newStep1");
        transcoding.setStep2("newStep2");
        transcoding.setStep3("newStep3");
        transcoding.setDefaultActive(true);
        transcodingDao.updateTranscoding(transcoding);

        Transcoding newTranscoding = transcodingDao.getAllTranscodings().get(0);
        assertTranscodingEquals(transcoding, newTranscoding);
    }

    @Test
    void testDeleteTranscoding() {
        assertEquals(0, transcodingDao.getAllTranscodings().size(), "Wrong number of transcodings.");

        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        assertEquals(1, transcodingDao.getAllTranscodings().size(), "Wrong number of transcodings.");

        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        assertEquals(2, transcodingDao.getAllTranscodings().size(), "Wrong number of transcodings.");

        transcodingDao.deleteTranscoding(transcodingDao.getAllTranscodings().get(0).getId());
        assertEquals(1, transcodingDao.getAllTranscodings().size(), "Wrong number of transcodings.");

        transcodingDao.deleteTranscoding(transcodingDao.getAllTranscodings().get(0).getId());
        assertEquals(0, transcodingDao.getAllTranscodings().size(), "Wrong number of transcodings.");
    }

    @Test
    void testPlayerTranscoding() {
        Player player = new Player();
        playerDao.createPlayer(player);

        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", false));
        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", false));
        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", false));
        final Transcoding transcodingA = transcodingDao.getAllTranscodings().get(0);
        final Transcoding transcodingB = transcodingDao.getAllTranscodings().get(1);
        final Transcoding transcodingC = transcodingDao.getAllTranscodings().get(2);

        List<Transcoding> activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals(0, activeTranscodings.size(), "Wrong number of transcodings.");

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[] { transcodingA.getId() });
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals(1, activeTranscodings.size(), "Wrong number of transcodings.");
        assertTranscodingEquals(transcodingA, activeTranscodings.get(0));

        transcodingDao.setTranscodingsForPlayer(player.getId(),
                new int[] { transcodingB.getId(), transcodingC.getId() });
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals(2, activeTranscodings.size(), "Wrong number of transcodings.");
        assertTranscodingEquals(transcodingB, activeTranscodings.get(0));
        assertTranscodingEquals(transcodingC, activeTranscodings.get(1));

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[0]);
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals(0, activeTranscodings.size(), "Wrong number of transcodings.");
    }

    @Test
    void testCascadingDeletePlayer() {
        Player player = new Player();
        playerDao.createPlayer(player);

        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        Transcoding transcoding = transcodingDao.getAllTranscodings().get(0);

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[] { transcoding.getId() });
        List<Transcoding> activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals(1, activeTranscodings.size(), "Wrong number of transcodings.");

        playerDao.deletePlayer(player.getId());
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals(0, activeTranscodings.size(), "Wrong number of transcodings.");
    }

    @Test
    void testCascadingDeleteTranscoding() {
        Player player = new Player();
        playerDao.createPlayer(player);

        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        Transcoding transcoding = transcodingDao.getAllTranscodings().get(0);

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[] { transcoding.getId() });
        List<Transcoding> activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals(1, activeTranscodings.size(), "Wrong number of transcodings.");

        transcodingDao.deleteTranscoding(transcoding.getId());
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals(0, activeTranscodings.size(), "Wrong number of transcodings.");
    }

    private void assertTranscodingEquals(Transcoding expected, Transcoding actual) {
        assertEquals(expected.getName(), actual.getName(), "Wrong name.");
        assertEquals(expected.getSourceFormats(), actual.getSourceFormats(), "Wrong source formats.");
        assertEquals(expected.getTargetFormat(), actual.getTargetFormat(), "Wrong target format.");
        assertEquals(expected.getStep1(), actual.getStep1(), "Wrong step 1.");
        assertEquals(expected.getStep2(), actual.getStep2(), "Wrong step 2.");
        assertEquals(expected.getStep3(), actual.getStep3(), "Wrong step 3.");
        assertEquals(expected.isDefaultActive(), actual.isDefaultActive(), "Wrong default active.");
    }
}
