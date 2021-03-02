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

package org.airsonic.player.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.Transcoding;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unit test of {@link TranscodingDao}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class TranscodingDaoTest extends DaoTestBase {

    @Autowired
    private TranscodingDao transcodingDao;

    @Autowired
    private PlayerDao playerDao;

    @Before
    public void setUp() {
        getJdbcTemplate().execute("delete from transcoding2");
    }

    @Test
    public void testCreateTranscoding() {
        Transcoding transcoding = new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2",
                "step3", false);
        transcodingDao.createTranscoding(transcoding);

        Transcoding newTranscoding = transcodingDao.getAllTranscodings().get(0);
        assertTranscodingEquals(transcoding, newTranscoding);
    }

    @Test
    public void testUpdateTranscoding() {
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
    public void testDeleteTranscoding() {
        assertEquals("Wrong number of transcodings.", 0, transcodingDao.getAllTranscodings().size());

        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        assertEquals("Wrong number of transcodings.", 1, transcodingDao.getAllTranscodings().size());

        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        assertEquals("Wrong number of transcodings.", 2, transcodingDao.getAllTranscodings().size());

        transcodingDao.deleteTranscoding(transcodingDao.getAllTranscodings().get(0).getId());
        assertEquals("Wrong number of transcodings.", 1, transcodingDao.getAllTranscodings().size());

        transcodingDao.deleteTranscoding(transcodingDao.getAllTranscodings().get(0).getId());
        assertEquals("Wrong number of transcodings.", 0, transcodingDao.getAllTranscodings().size());
    }

    @Test
    public void testPlayerTranscoding() {
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
        assertEquals("Wrong number of transcodings.", 0, activeTranscodings.size());

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[] { transcodingA.getId() });
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 1, activeTranscodings.size());
        assertTranscodingEquals(transcodingA, activeTranscodings.get(0));

        transcodingDao.setTranscodingsForPlayer(player.getId(),
                new int[] { transcodingB.getId(), transcodingC.getId() });
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 2, activeTranscodings.size());
        assertTranscodingEquals(transcodingB, activeTranscodings.get(0));
        assertTranscodingEquals(transcodingC, activeTranscodings.get(1));

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[0]);
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 0, activeTranscodings.size());
    }

    @Test
    public void testCascadingDeletePlayer() {
        Player player = new Player();
        playerDao.createPlayer(player);

        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        Transcoding transcoding = transcodingDao.getAllTranscodings().get(0);

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[] { transcoding.getId() });
        List<Transcoding> activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 1, activeTranscodings.size());

        playerDao.deletePlayer(player.getId());
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 0, activeTranscodings.size());
    }

    @Test
    public void testCascadingDeleteTranscoding() {
        Player player = new Player();
        playerDao.createPlayer(player);

        transcodingDao.createTranscoding(
                new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        Transcoding transcoding = transcodingDao.getAllTranscodings().get(0);

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[] { transcoding.getId() });
        List<Transcoding> activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 1, activeTranscodings.size());

        transcodingDao.deleteTranscoding(transcoding.getId());
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 0, activeTranscodings.size());
    }

    private void assertTranscodingEquals(Transcoding expected, Transcoding actual) {
        assertEquals("Wrong name.", expected.getName(), actual.getName());
        assertEquals("Wrong source formats.", expected.getSourceFormats(), actual.getSourceFormats());
        assertEquals("Wrong target format.", expected.getTargetFormat(), actual.getTargetFormat());
        assertEquals("Wrong step 1.", expected.getStep1(), actual.getStep1());
        assertEquals("Wrong step 2.", expected.getStep2(), actual.getStep2());
        assertEquals("Wrong step 3.", expected.getStep3(), actual.getStep3());
        assertEquals("Wrong default active.", expected.isDefaultActive(), actual.isDefaultActive());
    }
}
