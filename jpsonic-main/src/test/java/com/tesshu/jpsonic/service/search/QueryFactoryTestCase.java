/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package com.tesshu.jpsonic.service.search;

import com.tesshu.jpsonic.service.search.IndexType;
import com.tesshu.jpsonic.service.search.IndexType.FieldNames;
import com.tesshu.jpsonic.service.search.QueryFactory;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.apache.lucene.search.Query;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

/*
 * The query syntax has not changed significantly since Lucene 1.3.
 * (A slight difference)
 * If you face a problem reaping from 3.x to 7.x
 * It may be faster to look at the query than to look at the API.
 */
public class QueryFactoryTestCase extends TestCase {

    private static final String SEPA = System.getProperty("file.separator");
    String path1 = SEPA + "var" + SEPA + "music1";
    String path2 = SEPA + "var" + SEPA + "music2";
    MusicFolder music1 = new MusicFolder(Integer.valueOf(0), new File(path1), "music1", true, new java.util.Date());
    MusicFolder music2 = new MusicFolder(Integer.valueOf(0), new File(path2), "music2", true, new java.util.Date());

  public void testWithSearchCriteriaSingleParam() {

    SearchCriteria criteria = new SearchCriteria();
    criteria.setQuery("test1");

    List<MusicFolder> musicFolders = new ArrayList<MusicFolder>();
    musicFolders.add(music1);
    Query querySingle = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("single",
        "+(artist:test* artist:1* artistF:test1* artistR:test* artistR:1* artistRH:test1*) "
        + "+(folder:" + path1 + ")",
        querySingle.toString());
  }

  public void testWithSearchCriteriaMultiParam() {

   SearchCriteria criteria = new SearchCriteria();
   criteria.setQuery("test1 test2");
   List<MusicFolder> musicFolders = new ArrayList<MusicFolder>();
   musicFolders.add(music1);
   musicFolders.add(music2);
    Query queryMulti = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("artist",
        "+(artist:test* artist:1* artist:test* artist:2* artistF:test1test2* "
        + "artistR:test* artistR:1* artistR:test* artistR:2* artistRH:test1test2*) "
        + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryMulti.toString());

    criteria.setCount(50); // Not used in queries
    criteria.setOffset(0); // Not used in queries
    Query queryFull = QueryFactory.createQuery(criteria, musicFolders, IndexType.ALBUM);
    assertEquals("album",
        "+(album:test* album:1* album:test* album:2* albumF:test1test2* artist:test* artist:1* "
        + "artist:test* artist:2* artistF:test1test2* artistR:test* artistR:1* "
        + "artistR:test* artistR:2* artistRH:test1test2*) "
        + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryFull.toString());
    
    criteria.setCount(50); // Not used in queries
    criteria.setOffset(0); // Not used in queries
    queryFull = QueryFactory.createQuery(criteria, musicFolders, IndexType.SONG);
    assertEquals("song",
        "+(title:test* title:1* title:test* title:2* artist:test* artist:1* artist:test* artist:2* "
        + "artistF:test1test2* artistR:test* artistR:1* artistR:test* artistR:2* artistRH:test1test2*) "
        + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryFull.toString());
  }

  public void testWithSearchCriteriaJapaneseParam() {

    SearchCriteria criteria = new SearchCriteria();
    criteria.setQuery("test1 test2");
    List<MusicFolder> musicFolders = new ArrayList<MusicFolder>();
    musicFolders.add(music1);
    musicFolders.add(music2);

    criteria = new SearchCriteria();
    criteria.setQuery("みんなの歌");
    Query queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("japanese",
        "+(artist:みんな* artist:歌* artistF:みんなの歌* artistR:みんな* artistR:歌* artistRH:みんなの歌*) "
        + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryJapanese.toString());

    criteria.setQuery("いきものがかり");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("いきものがかり",
        "+(artist:いき* artist:かり* artistF:いきものがかり* artistR:いき* artistR:かり* artistRH:いきものがかり*) "
        + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryJapanese.toString());

    criteria.setQuery("いきもの がかり");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("いきもの がかり",
        "+(artist:いき* artist:がかり* artistF:いきものがかり* artistR:いき* artistR:がかり* artistRH:いきものがかり*) "
        + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryJapanese.toString());

    criteria.setQuery("いきものガカリ");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("いきものガカリ",
        "+(artist:いき* artist:ガ* artist:カリ* artistF:いきものガカリ* artistR:いき* artistR:ガ* artistR:カリ* artistRH:いきものガカリ*) "
        + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryJapanese.toString());

    criteria.setQuery("イキモノガカリ");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("イキモノガカリ",
        "+(artist:イキモノガカリ* artistF:イキモノガカリ* artistR:イキモノガカリ* artistRH:イキモノガカリ*) "
            + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryJapanese.toString());

  }

  public void testWithRandomSearchCriteria() {

    String sapa = System.getProperty("file.separator");
    String path1 = sapa + "var" + sapa + "music1";

    List<MusicFolder> musicFolders = new ArrayList<MusicFolder>();
    musicFolders.add(
        new MusicFolder(Integer.valueOf(0), new File(path1), "music1", true, new Date()));

    RandomSearchCriteria criteria = new RandomSearchCriteria(
        50,            // count
        "Classic Rock",      // genre, 
        Integer.valueOf(1900),  // fromYear, 
        Integer.valueOf(2000),  // toYear, 
        musicFolders);       // musicFolders
    Query querySingle = QueryFactory.createQuery(criteria);
    assertEquals("single",
        "+mediaType:music "+
        "+genre:classicrock " +
        "+year:[1900 TO 2000] " +
        "+(folder:" + path1+ ")",
        querySingle.toString());

    String path2 = sapa + "var" + sapa + "music2";
    musicFolders.add(
        new MusicFolder(Integer.valueOf(1), new File(path2), "music2", true, new Date()));
    Query queryMulti = QueryFactory.createQuery(criteria);
    assertEquals("multi",
        "+mediaType:music "
        + "+genre:classicrock "
        + "+year:[1900 TO 2000] "
        + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryMulti.toString());

    criteria = new RandomSearchCriteria(
        50,            // count
        null,          // genre, 
        null,          // fromYear, 
        Integer.valueOf(2000),  // toYear, 
        musicFolders);       // musicFolders

    Query queryNullFrom = QueryFactory.createQuery(criteria);

    assertEquals("NullFrom",
        "+mediaType:music "
        + "+year:[-2147483648 TO 2000] "
        + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryNullFrom.toString());

    criteria = new RandomSearchCriteria(
        50,            // count
        null,          // genre, 
        Integer.valueOf(1900),  // fromYear, 
        null,          // toYear, 
        musicFolders);       // musicFolders
    Query queryNullTo = QueryFactory.createQuery(criteria);
    assertEquals("NullTo",
        "+mediaType:music "
        + "+year:[1900 TO 2147483647] "
        + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryNullTo.toString());

    criteria = new RandomSearchCriteria(
        50,            // count
        null,          // genre, 
        null,          // fromYear, 
        null,          // toYear, 
        musicFolders);       // musicFolders
    Query queryNullYear = QueryFactory.createQuery(criteria);
    assertEquals("NullYear",
        "+mediaType:music "
            + "+(folder:" + path1 + " folder:" + path2 + ")",
        queryNullYear.toString());

  }

  public void testSearchByName() {
    Query querySingle = QueryFactory.searchByName("The Ventures", FieldNames.ARTIST);
    assertEquals("single",
        "+artist:ventures*", querySingle.toString());
  }

  public void testArticle() {
    Query querySingle = QueryFactory.searchByName("THIS IS THE VENTURES", FieldNames.ARTIST);
    assertEquals("article",
        "+artist:this* +artist:is* +artist:ventures*", querySingle.toString());
  }

  public void testAUX29() {
    Query querySingle = QueryFactory.searchByName("I'll be back.", FieldNames.ARTIST);
    assertEquals("aux",
        "+artist:i* +artist:ll* +artist:be* +artist:back*", querySingle.toString());
  }

  public void testSearchByNameMusicFolderPath() {
    
    String SEPA = System.getProperty("file.separator");
    String path1 = SEPA + "var" + SEPA + "music1";
    String path2 = SEPA + "var" + SEPA + "music2";

    List<MusicFolder> musicFolders = new ArrayList<MusicFolder>();
    musicFolders.add(
        new MusicFolder(Integer.valueOf(0), new File(path1), "music1", true, new Date()));
    Query querySingle = QueryFactory.searchRandomAlbum(musicFolders);
    assertEquals("single",
        "+(folder:" + path1 + ")", querySingle.toString());

    musicFolders.add(
        new MusicFolder(Integer.valueOf(1), new File(path2), "music2", true, new Date()));
    Query queryMulti = QueryFactory.searchRandomAlbum(musicFolders);
    assertEquals("multi",
        "+(folder:" + path1 + " folder:" + path2 + ")",
        queryMulti.toString());
  }
  
  public void testSearchByNameMusicFolderIds() {

    String sepa = System.getProperty("file.separator");
    String path1 = sepa + "var" + sepa + "music1";

    List<MusicFolder> musicFolders = new ArrayList<MusicFolder>();
    musicFolders.add(
        new MusicFolder(Integer.valueOf(0), new File(path1), "music1", true, new Date()));
    Query querySingle = QueryFactory.searchRandomAlbumId3(musicFolders);
    assertEquals("single",
        "+(folderId:0)",
        querySingle.toString());

    String path2 = sepa + "var" + sepa + "music2";
    musicFolders.add(
        new MusicFolder(Integer.valueOf(1), new File(path2), "music2", true, new Date()));
    Query queryMulti = QueryFactory.searchRandomAlbumId3(musicFolders);
    assertEquals("single",
        "+(folderId:0 folderId:1)",
        queryMulti.toString());

  }
}