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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicIndex;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.GenreContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IndexId3ProcTest extends AbstractNeedsScan {

    private static final List<MusicFolder> MUSIC_FOLDERS = Arrays
            .asList(new MusicFolder(1, resolveBaseMediaPath("Sort/Compare"), "Artists", true, now(), 1));

    @Autowired
    private ArtistDao artistDao;
    @Autowired
    private IndexId3Proc proc;
    @Autowired
    private UpnpProcessorUtil util;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @BeforeEach
    public void setup() {
        String simpleIndex = "A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ) " // En
                + "\u3042(\u30A2\u30A4\u30A6\u30A8\u30AA) " // Jp(a)
                + "\u304B(\u30AB\u30AD\u30AF\u30B1\u30B3) " // Jp(ka)
                + "\u3055(\u30B5\u30B7\u30B9\u30BB\u30BD) " // Jp(sa)
                + "\u305F(\u30BF\u30C1\u30C4\u30C6\u30C8) " // Jp(ta)
                + "\u306A(\u30CA\u30CB\u30CC\u30CD\u30CE) " // Jp(na)
                + "\u306F(\u30CF\u30D2\u30D5\u30D8\u30DB) " // Jp(ha)
                + "\u307E(\u30DE\u30DF\u30E0\u30E1\u30E2) " // Jp(ma)
                + "\u3084(\u30E4\u30E6\u30E8) " // Jp(ya)
                + "\u3089(\u30E9\u30EA\u30EB\u30EC\u30ED) " // Jp(ra)
                + "\u308F(\u30EF\u30F2\u30F3)"; // Jp(wa)
        settingsService.setIndexString(simpleIndex);
        settingsService.save();
        populateDatabaseOnlyOnce();
    }

    @Test
    void testGetProcId() {
        assertEquals("indexId3", proc.getProcId().getValue());
    }

    @Test
    void testCreateContainer() {
        MusicIndex index = proc.getDirectChild("A");
        Container container = proc.createContainer(index);
        assertInstanceOf(GenreContainer.class, container);
        assertEquals("indexId3/A", container.getId());
        assertEquals("indexId3", container.getParentID());
        assertEquals("A", container.getTitle());
        assertEquals(3, container.getChildCount());
    }

    @Test
    void testGetDirectChildren() {
        List<MusicIndex> indexes = proc.getDirectChildren(0, Integer.MAX_VALUE);
        assertEquals(9, indexes.size());
        assertEquals("A", indexes.get(0).getIndex());
        assertEquals("B", indexes.get(1).getIndex());
        assertEquals("C", indexes.get(2).getIndex());
        assertEquals("D", indexes.get(3).getIndex());
        assertEquals("E", indexes.get(4).getIndex());
        assertEquals("あ", indexes.get(5).getIndex());
        assertEquals("さ", indexes.get(6).getIndex());
        assertEquals("は", indexes.get(7).getIndex());
        assertEquals("#", indexes.get(8).getIndex());

        indexes = proc.getDirectChildren(0, 5);
        assertEquals("A", indexes.get(0).getIndex());
        assertEquals("B", indexes.get(1).getIndex());
        assertEquals("C", indexes.get(2).getIndex());
        assertEquals("D", indexes.get(3).getIndex());
        assertEquals("E", indexes.get(4).getIndex());

        indexes = proc.getDirectChildren(5, 4);
        assertEquals("あ", indexes.get(0).getIndex());
        assertEquals("さ", indexes.get(1).getIndex());
        assertEquals("は", indexes.get(2).getIndex());
        assertEquals("#", indexes.get(3).getIndex());
    }

    @Test
    void testGetDirectChildrenCount() {

        List<MusicFolder> folders = util.getGuestFolders();
        assertEquals(1, folders.size());

        assertEquals(32, artistDao.getArtistsCount(folders));

        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
        assertEquals(32, artists.size());

        // #
        assertEquals("10", artists.get(0).getName());
        assertEquals("20", artists.get(1).getName());
        assertEquals("50", artists.get(2).getName());
        assertEquals("60", artists.get(3).getName());
        assertEquals("70", artists.get(4).getName());
        assertEquals("98", artists.get(5).getName());
        assertEquals("99", artists.get(6).getName());

        // A
        assertEquals("abcde", artists.get(7).getName());
        assertEquals("abcいうえおあ", artists.get(8).getName());
        assertEquals("abc亜伊鵜絵尾", artists.get(9).getName());

        // B
        assertEquals("ＢＣＤＥＡ", artists.get(10).getName());

        // C
        assertEquals("ĆḊÉÁḂ", artists.get(11).getName());

        // D
        assertEquals("DEABC", artists.get(12).getName());

        // E
        assertEquals("the eabcd", artists.get(13).getName());
        assertEquals("episode 1", artists.get(14).getName());
        assertEquals("episode 2", artists.get(15).getName());
        assertEquals("episode 19", artists.get(16).getName());

        // あいうえお
        assertEquals("亜伊鵜絵尾", artists.get(17).getName());
        assertEquals("αβγ", artists.get(18).getName());
        assertEquals("いうえおあ", artists.get(19).getName());
        assertEquals("ゥェォァィ", artists.get(20).getName());
        assertEquals("ｴｵｱｲｳ", artists.get(21).getName());
        assertEquals("ｪｫｧｨｩ", artists.get(22).getName());
        assertEquals("ぉぁぃぅぇ", artists.get(23).getName());
        assertEquals("オアイウエ", artists.get(24).getName());

        // さしすせそ
        assertEquals("春夏秋冬", artists.get(25).getName());

        // はひふへほ
        assertEquals("貼られる", artists.get(26).getName());
        assertEquals("パラレル", artists.get(27).getName());
        assertEquals("馬力", artists.get(28).getName());
        assertEquals("張り切る", artists.get(29).getName());
        assertEquals("はるなつあきふゆ", artists.get(30).getName());

        // #
        assertEquals("♂くんつ", artists.get(31).getName());

        assertEquals(9, proc.getDirectChildrenCount());
    }

    @Test
    void testGetDirectChild() {
        assertEquals("A", proc.getDirectChild("A").getIndex());
        assertEquals("B", proc.getDirectChild("B").getIndex());
        assertEquals("C", proc.getDirectChild("C").getIndex());
        assertEquals("D", proc.getDirectChild("D").getIndex());
        assertEquals("E", proc.getDirectChild("E").getIndex());
        assertEquals("あ", proc.getDirectChild("あ").getIndex());
        assertEquals("さ", proc.getDirectChild("さ").getIndex());
        assertEquals("は", proc.getDirectChild("は").getIndex());
        assertEquals("#", proc.getDirectChild("#").getIndex());
    }

    @Test
    void testGetChildren() {
        MusicIndex index = proc.getDirectChild("A");
        List<Artist> artists = proc.getChildren(index, 0, Integer.MAX_VALUE);
        assertEquals(3, artists.size());
        assertEquals("abcde", artists.get(0).getName());
        assertEquals("abcいうえおあ", artists.get(1).getName());
        assertEquals("abc亜伊鵜絵尾", artists.get(2).getName());
    }

    @Test
    void testGetChildSizeOf() {
        MusicIndex index = proc.getDirectChild("A");
        assertEquals(3, proc.getChildSizeOf(index));
    }

    @Test
    void testAddChild() {
        DIDLContent content = new DIDLContent();
        assertEquals(0, content.getContainers().size());

        MusicIndex index = proc.getDirectChild("A");
        proc.getChildren(index, 0, Integer.MAX_VALUE).stream().forEach(artist -> proc.addChild(content, artist));
        assertEquals(3, content.getContainers().size());
    }
}
