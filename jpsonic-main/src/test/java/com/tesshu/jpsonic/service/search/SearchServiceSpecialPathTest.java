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

package com.tesshu.jpsonic.service.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Test cases related to #1139.
 * Confirming whether shuffle search can be performed correctly in MusicFolder containing special strings.
 *
 * (Since the query of getRandomAlbums consists of folder paths only,
 * this verification is easy to perform.)
 *
 * This test case is a FalsePattern for search,
 * but there may be problems with the data flow prior to creating the search index.
 */
class SearchServiceSpecialPathTest extends AbstractNeedsScan {

    private List<MusicFolder> musicFolders;

    @Autowired
    private SearchService searchService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();

            File musicDir = new File(resolveBaseMediaPath("Search/SpecialPath/accessible"));
            musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));

            File music2Dir = new File(resolveBaseMediaPath("Search/SpecialPath/accessible's"));
            musicFolders.add(new MusicFolder(2, music2Dir, "accessible's", true, new Date()));

            File music3Dir = new File(resolveBaseMediaPath("Search/SpecialPath/accessible+s"));
            musicFolders.add(new MusicFolder(3, music3Dir, "accessible+s", true, new Date()));
        }
        return musicFolders;
    }

    @BeforeEach
    public void setup() {
        populateDatabaseOnlyOnce();
    }

    @Test
    void testSpecialCharactersInDirName() {

        List<MusicFolder> folders = getMusicFolders();

        // ALL Songs
        List<MediaFile> randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folders);
        assertEquals(3, randomAlbums.size(), "ALL Albums ");

        // dir - accessible
        List<MusicFolder> folder01 = folders.stream().filter(m -> "accessible".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder01);
        assertEquals(1, randomAlbums.size(), "Albums in \"accessible\" ");

        // dir - accessible's
        List<MusicFolder> folder02 = folders.stream().filter(m -> "accessible's".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder02);
        assertEquals(1, randomAlbums.size(), "Albums in \"accessible's\" ");

        // dir - accessible+s
        List<MusicFolder> folder03 = folders.stream().filter(m -> "accessible+s".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder03);
        assertEquals(1, randomAlbums.size(), "Albums in \"accessible+s\" ");
    }
}
