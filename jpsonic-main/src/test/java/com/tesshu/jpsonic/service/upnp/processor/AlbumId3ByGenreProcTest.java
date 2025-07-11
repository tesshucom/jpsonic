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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.GenreContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AlbumId3ByGenreProcTest extends AbstractNeedsScan {

    private static final List<MusicFolder> MUSIC_FOLDERS = Arrays
        .asList(new MusicFolder(1, resolveBaseMediaPath("MultiGenre"), "Genres", true, now(), 1,
                false));

    @Autowired
    private AlbumId3ByGenreProc proc;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @BeforeEach
    public void setup() {
        populateDatabaseOnlyOnce();
        settingsService.setDlnaBaseLANURL("https://192.168.1.1:4040");
        settingsService.save();
    }

    @Test
    @Order(1)
    void testGetProcId() {
        assertEquals("aibg", proc.getProcId().getValue());
    }

    @Test
    @Order(2)
    void testCreateContainer() {
        // test getDirectChildren
        Genre genre = proc.getDirectChildren(0, 1).get(0);

        Container container = proc.createContainer(genre);
        assertInstanceOf(GenreContainer.class, container);
        assertEquals("aibg/GENRE_L", container.getId());
        assertEquals("aibg", container.getParentID());
        assertEquals("GENRE_L", container.getTitle());
        assertEquals(2, container.getChildCount());
    }

    @Test
    @Order(3)
    void testBrowseRoot() throws ExecutionException {
        assertEquals(12, proc.getDirectChildrenCount());
        BrowseResult browseResult = proc.browseRoot(null, 0, Integer.MAX_VALUE);
        assertEquals(proc.getDirectChildrenCount(), browseResult.getTotalMatchesLong());
    }

    @Test
    @Order(4)
    void testGetDirectChild() {
        Genre genre = proc.getDirectChildren(0, 1).get(0);
        // test getDirectChild
        // test getChildren
        Genre directChild = proc.getDirectChild(genre.getName());
        assertEquals(genre.getName(), directChild.getName());
        assertEquals(genre.getAlbumCount(), directChild.getAlbumCount());
        assertEquals(genre.getSongCount(), directChild.getSongCount());
    }

    @Test
    @Order(5)
    void testBrowseDirectChildren() throws ExecutionException {
        Genre genre = proc.getDirectChildren(0, 1).get(0);
        // test getDirectChild
        BrowseResult browseResult = proc.browseDirectChildren(genre.getName());
        assertEquals(1, browseResult.getTotalMatchesLong());
    }

    @Nested
    class BrowseLeafTest {

        @Test
        void testBrowseLeafWithGenre() throws ExecutionException {
            Genre genre = proc.getDirectChildren(0, 1).get(0);
            BrowseResult browseResult = proc
                .browseLeaf(genre.getName(), null, 0, genre.getAlbumCount());
            assertEquals(2, browseResult.getTotalMatchesLong());
        }

        @Test
        void testBrowseLeafWithCompositeId() throws ExecutionException {
            Genre genre = proc.getDirectChildren(0, 1).get(0);
            BrowseResult browseResult = proc
                .browseLeaf(genre.getName(), null, 0, genre.getAlbumCount());
            String result = browseResult.getResult();
            String firstChildIdStartKey = "container childCount=\"1\" id=\"";
            int firstChildIdStart = result.indexOf(firstChildIdStartKey)
                    + firstChildIdStartKey.length();
            String firstChildIdEndKey = "\"";
            int firstChildIdEnd = result.indexOf(firstChildIdEndKey, firstChildIdStart);
            String firstChildId = result.substring(firstChildIdStart, firstChildIdEnd);
            String leafId = firstChildId
                .substring(firstChildId.indexOf(ProcId.CID_SEPA) + ProcId.CID_SEPA.length());
            browseResult = proc.browseLeaf(leafId, null, 0, genre.getAlbumCount());
            assertEquals(1, browseResult.getTotalMatchesLong());
        }
    }
}
