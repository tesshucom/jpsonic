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

package org.airsonic.player.service.search;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Test cases related to #1142.
 * The filter is not properly applied when analyzing the query,
 *
 * In the process of hardening the Analyzer implementation,
 * this problem is solved side by side.
 */
public class SearchServiceStartWithStopwardsTest extends AbstractAirsonicHomeTest {

    private List<MusicFolder> musicFolders;

    @Autowired
    private SearchService searchService;

    @Autowired
    private SearchCriteriaDirector director;

    @Autowired
    private SettingsService settingsService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();
            File musicDir = new File(resolveBaseMediaPath("Search/StartWithStopwards"));
            musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));
        }
        return musicFolders;
    }

    @Before
    public void setup() {
        settingsService.setSearchMethodLegacy(false);
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testStartWithStopwards() throws IOException {

        int offset = 0;
        int count = Integer.MAX_VALUE;
        List<MusicFolder> folders = getMusicFolders();

        SearchCriteria criteria = director.construct("will", offset, count, false, folders, IndexType.ARTIST_ID3);
        SearchResult result = searchService.search(criteria);
        // Will hit because Airsonic's stopword is defined(#1235) => This case does not hit because it is a phrase
        // search rather than a term prefix match.
        Assert.assertEquals("Williams hit by \"will\" ", 0, result.getTotalHits());

        // XXX legacy -> phrase
        criteria = director.construct("williams", offset, count, false, folders, IndexType.ARTIST_ID3);
        result = searchService.search(criteria);
        Assert.assertEquals("Williams hit by \"williams\" ", 1, result.getTotalHits());

        criteria = director.construct("the", offset, count, false, folders, IndexType.SONG);
        result = searchService.search(criteria);
        // XXX 3.x -> 8.x : The filter is properly applied to the input(Stopward)
        Assert.assertEquals("Theater hit by \"the\" ", 0, result.getTotalHits());

        criteria = director.construct("willi", offset, count, false, folders, IndexType.ARTIST_ID3);
        result = searchService.search(criteria);
        // XXX 3.x -> 8.x : Normal forward matching => This case does not hit because it is a phrase search rather than
        // a term prefix match.
        Assert.assertEquals("Williams hit by \"Williams\" ", 0, result.getTotalHits());

        criteria = director.construct("thea", offset, count, false, folders, IndexType.SONG);
        result = searchService.search(criteria);
        // XXX 3.x -> 8.x : Normal forward matching
        Assert.assertEquals("Theater hit by \"thea\" ", 0, result.getTotalHits()); // => This case does not hit because
                                                                                   // it is a phrase search rather than
                                                                                   // a term prefix match.

        // XXX legacy -> phrase
        criteria = director.construct("theater", offset, count, false, folders, IndexType.SONG);
        result = searchService.search(criteria);
        Assert.assertEquals("Theater hit by \"theater\" ", 1, result.getTotalHits());

    }
}
