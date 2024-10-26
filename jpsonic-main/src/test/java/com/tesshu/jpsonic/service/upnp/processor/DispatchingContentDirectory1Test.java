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
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.search.UPnPSearchMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.jupnp.support.model.BrowseResult;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * UPnP File Structure Search Test
 */
class DispatchingContentDirectory1Test extends AbstractNeedsScan {

    @Autowired
    private DispatchingContentDirectory contentDirectory;

    @Autowired
    private SettingsService settingsService;

    private List<MusicFolder> musicFolders;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @BeforeEach
    public void setup() throws URISyntaxException, InterruptedException {
        musicFolders = Arrays.asList(new MusicFolder(1,
                Path.of(DispatchingContentDirectory1Test.class.getResource("/MEDIAS/Sort/Pagination/Artists").toURI())
                        .toString(),
                "Artists", true, now(), 1, false));
        settingsService.setDlnaBaseLANURL("https://192.168.1.1:4040");
        settingsService.save();
        populateDatabaseOnlyOnce();
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void testFindProcessor() {
        Stream.of(ProcId.values()).forEach(id -> assertNotNull(contentDirectory.findProcessor(id)));
    }

    @Test
    void testSearch() throws ContentDirectoryException {

        // File Structure
        String query = """
                (upnp:class = "object.container.person.musicArtist" \
                and dc:title contains "はるなつあきふゆ")\
                """;
        BrowseResult result = contentDirectory.search(null, query, null, 0, Integer.MAX_VALUE, null);
        assertEquals(1, result.getCount().getValue());
        assertEquals(1, result.getTotalMatches().getValue());

        query = """
                (upnp:class = "object.container.album.musicAlbum") \
                and (dc:title contains "ALBUM" \
                        or dc:creator contains "ALBUM" \
                        or upnp:artist contains "ALBUM" )\
                """;
        result = contentDirectory.search(null, query, null, 0, 10, null);
        assertEquals(10, result.getCount().getValue());
        assertEquals(30, result.getTotalMatches().getValue());

        // Song
        query = """
                (upnp:class derivedfrom "object.item.audioItem" \
                        or upnp:class derivedfrom "object.item.videoItem ") \
                and (dc:title contains "empty" \
                        or dc:creator contains "empty" \
                        or upnp:artist contains "empty" \
                        or upnp:albumArtist contains "empty" \
                        or upnp:album contains "empty" \
                        or upnp:author contains "empty" \
                        or upnp:genre contains "empty" )\
                """;
        result = contentDirectory.search(null, query, null, 0, 30, null);
        assertEquals(30, result.getCount().getValue());
        assertEquals(61, result.getTotalMatches().getValue());

        // ID3
        // Like Subsonic and Airsonic, the correction is based on the Artist/Album/Song three-level
        // management. Therefore, if a tag is missing, the same result as the File Structure will be
        // returned, preventing missing results.
        settingsService.setUPnPSearchMethod(UPnPSearchMethod.ID3.name());
        query = """
                (upnp:class = "object.container.person.musicArtist" \
                and dc:title contains "はるなつあきふゆ")\
                """;
        result = contentDirectory.search(null, query, null, 0, Integer.MAX_VALUE, null);
        assertEquals(1, result.getCount().getValue());
        assertEquals(1, result.getTotalMatches().getValue());

        query = """
                (upnp:class = "object.container.album.musicAlbum") \
                and (dc:title contains "ALBUM" \
                        or dc:creator contains "ALBUM" \
                        or upnp:artist contains "ALBUM" )\
                """;
        result = contentDirectory.search(null, query, null, 0, 10, null);
        assertEquals(10, result.getCount().getValue());
        assertEquals(30, result.getTotalMatches().getValue());
    }
}
