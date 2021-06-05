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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Tests to prove what kind of strings/chars can be used in the genre field.
 */
public class SearchServiceSpecialGenreTest extends AbstractNeedsScan {

    private List<MusicFolder> musicFolders;

    @Autowired
    private SearchService searchService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();
            File musicDir = new File(resolveBaseMediaPath("Search/SpecialGenre"));
            musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));
        }
        return musicFolders;
    }

    @BeforeEach
    public void setup() {
        populateDatabaseOnlyOnce();
    }

    /*
     * There are 19 files in src/test/resources/MEDIAS/Search/SpecialGenre/ARTIST1/ALBUM_A. In FILE01 to FILE16, Special
     * strings for Lucene syntax are stored as tag values ​​of Genre.
     *
     * Legacy can not search all these genres. (Strictly speaking, the genre field is not created at index creation.)
     *
     * // XXX 3.x -> 8.x : Do the process more strictly.
     *
     * - Values ​​that can be cross-referenced with DB are stored in the index. - Search is also possible with user's
     * readable value (file tag value). - However, there is an exception in parentheses.
     */
    @Test
    public void testQueryEscapeRequires() {

        Function<String, RandomSearchCriteria> simpleStringCriteria = s -> new RandomSearchCriteria(Integer.MAX_VALUE, // count
                Arrays.asList(s), // genre,
                null, // fromYear
                null, // toYear
                getMusicFolders() // musicFolders
        );

        List<MediaFile> songs = searchService.getRandomSongs(simpleStringCriteria.apply("+"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("+", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 1", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("-"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("-", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 2", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("&&"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("&&", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 3", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("||"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("||", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 4", songs.get(0).getTitle());

        /*
         * // XXX 3.x -> 8.x : Brackets ()
         *
         * Lucene can handle these. However, brackets are specially parsed before the index creation process.
         *
         * This string is never stored in the index. This is the only exception.
         */
        songs = searchService.getRandomSongs(simpleStringCriteria.apply(" (")); // space & bracket
        assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply(")"));
        assertEquals(0, songs.size());

        /*
         * // XXX 3.x -> 8.x : Brackets {}[]
         *
         * Lucene can handle these. However, brackets are specially parsed before the index creation process.
         *
         * This can be done with a filter that performs the reverse process on the input values ​​when searching. As a
         * result, the values ​​stored in the file can be retrieved by search.
         *
         * @see AnalyzerFactory <<<<<<<
         * HEAD:jpsonic-main/src/test/java/org/airsonic/player/service/search/SearchServiceSpecialGenreTestCase.java
         * 
         *
         * =======
         *
         * >>>>> >>>>>>>
         * ec426fc:airsonic-main/src/test/java/org/airsonic/player/service/search/SearchServiceSpecialGenreTestCase.java
         */
        songs = searchService.getRandomSongs(simpleStringCriteria.apply("{}"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        /*
         * This is the result of the tag parser and domain value. It is different from the tag value in file.
         */
        assertEquals("{ }", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 7", songs.get(0).getTitle());
        songs = searchService.getRandomSongs(simpleStringCriteria.apply("{ }"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("Query Escape Requires 7", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("[]"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        /*
         * This is the result of the tag parser and domain value. It is different from the tag value in file.
         */
        assertEquals("[ ]", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 8", songs.get(0).getTitle());
        songs = searchService.getRandomSongs(simpleStringCriteria.apply("[ ]"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("Query Escape Requires 8", songs.get(0).getTitle());
        // <<<<<

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("^"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("^", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 9", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("\""));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("\"", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 10", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("~"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("~", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 11", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("*"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("*", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 12", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("?"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("?", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 13", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply(":"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals(":", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 14", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("\\"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("\\", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 15", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("/"));
        assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
        assertEquals("/", songs.get(0).getGenre());
        assertEquals("Query Escape Requires 16", songs.get(0).getTitle());

    }

    /*
     * Jaudiotagger applies special treatment to bracket (FILE17). XXX 3.x -> 8.x : Specification of genre search became
     * more natural.
     */
    @Test
    public void testBrackets() {

        Function<String, RandomSearchCriteria> simpleStringCriteria = s -> new RandomSearchCriteria(Integer.MAX_VALUE, // count
                Arrays.asList(s), // genre,
                null, // fromYear
                null, // toYear
                getMusicFolders() // musicFolders
        );

        // -(GENRE)- is registered as genre of FILE17.

        /*
         * Search by genre string registered in file.
         *
         * The value stored in the index is different from legacy. Domain value is kept as it is.
         */
        List<MediaFile> songs = searchService.getRandomSongs(simpleStringCriteria.apply("-(GENRE)-"));
        assertEquals(1, songs.size());
        assertEquals("-GENRE -", songs.get(0).getGenre());
        assertEquals("Consistency with Tag Parser 1", songs.get(0).getTitle());

        /*
         * Search by Domain value.
         */
        songs = searchService.getRandomSongs(simpleStringCriteria.apply("-GENRE -"));
        assertEquals(1, songs.size());
        assertEquals("-GENRE -", songs.get(0).getGenre());
        assertEquals("Consistency with Tag Parser 1", songs.get(0).getTitle());

        /*
         * Legacy genre search
         */
        songs = searchService.getRandomSongs(simpleStringCriteria.apply(" genre"));
        // Strong unique parsing rules have been removed.
        assertEquals(0, songs.size());

    }

    /*
     * Jaudiotagger applies special treatment to numeric. (FILE18)
     */
    @Test
    public void testNumericMapping() {

        List<MusicFolder> folders = getMusicFolders();

        RandomSearchCriteria criteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                Arrays.asList("Rock"), // genre,
                null, // fromYear
                null, // toYear
                folders // musicFolders
        );

        List<MediaFile> songs = searchService.getRandomSongs(criteria);
        assertEquals(1, songs.size());
        assertEquals("Numeric mapping specification of genre 1", songs.get(0).getTitle());

        // The value registered in the file is 17
        assertEquals("Rock", songs.get(0).getGenre());

    }

    /*
     * Other special strings. (FILE19)
     *
     * {'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's
     *
     * Legacy stores with Analyze, so searchable characters are different.
     *
     */
    @Test
    public void testOthers() {

        Function<String, RandomSearchCriteria> simpleStringCriteria = s -> new RandomSearchCriteria(Integer.MAX_VALUE, // count
                Arrays.asList(s), // genre,
                null, // fromYear
                null, // toYear
                getMusicFolders() // musicFolders
        );

        // XXX 3.x -> 8.x : Do the process more strictly.
        List<MediaFile> songs = searchService
                .getRandomSongs(simpleStringCriteria.apply("{'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's"));
        assertEquals(1, songs.size());
        assertEquals(1, songs.size());
        assertEquals("Other special strings 1", songs.get(0).getTitle());
        assertEquals("{'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's", songs.get(0).getGenre());

        /*
         * Legacy kept "widthcaesar" using their own rules. The previous rule has been discarded.
         */
        songs = searchService.getRandomSongs(simpleStringCriteria.apply("widthcaesar"));
        assertEquals(0, songs.size());

    }
}
