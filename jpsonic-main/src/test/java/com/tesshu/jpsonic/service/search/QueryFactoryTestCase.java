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
        "+(art:test* art:1* artF:test1* artR:test* artR:1* artRH:test1*) "
        + "+(f:" + path1 + ")",
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
        "+(art:test* art:1* art:test* art:2* artF:test1test2* "
        + "artR:test* artR:1* artR:test* artR:2* artRH:test1test2*) "
        + "+(f:" + path1 + " f:" + path2 + ")",
        queryMulti.toString());

    criteria.setCount(50); // Not used in queries
    criteria.setOffset(0); // Not used in queries
    Query queryFull = QueryFactory.createQuery(criteria, musicFolders, IndexType.ALBUM);
    assertEquals("album",
        "+(alb:test* alb:1* alb:test* alb:2* albF:test1test2* albRH:test* albRH:1* albRH:test* albRH:2* "
        + "art:test* art:1* art:test* art:2* artF:test1test2* artR:test* artR:1* artR:test* artR:2* artRH:test1test2*) "
        + "+(f:" + path1 + " f:" + path2 + ")",
        queryFull.toString());
    
    criteria.setCount(50); // Not used in queries
    criteria.setOffset(0); // Not used in queries
    queryFull = QueryFactory.createQuery(criteria, musicFolders, IndexType.SONG);
    assertEquals("song",
        "+(tit:test* tit:1* tit:test* tit:2* titRH:test* titRH:1* titRH:test* titRH:2* "
        + "art:test* art:1* art:test* art:2* artF:test1test2* artR:test* artR:1* artR:test* artR:2* artRH:test1test2*) "
        + "+(f:" + path1 + " f:" + path2 + ")",
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
        "+(art:みんな* art:歌* artF:みんなの歌* artR:みんな* artR:歌* artRH:みんなの歌*) "
        + "+(f:" + path1 + " f:" + path2 + ")",
        queryJapanese.toString());

    criteria.setQuery("いきものがかり");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("いきものがかり",
        "+(art:いき* art:かり* artF:いきものがかり* artR:いき* artR:かり* artRH:いきものがかり*) "
        + "+(f:" + path1 + " f:" + path2 + ")",
        queryJapanese.toString());

    criteria.setQuery("いきもの がかり");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("いきもの がかり",
        "+(art:いき* art:がかり* artF:いきものがかり* artR:いき* artR:がかり* artRH:いきものがかり*) "
        + "+(f:" + path1 + " f:" + path2 + ")",
        queryJapanese.toString());

    criteria.setQuery("いきものガカリ");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("いきものガカリ",
        "+(art:いき* art:ガ* art:カリ* artF:いきものガカリ* artR:いき* artR:ガ* artR:カリ* artRH:いきものガカリ*) "
        + "+(f:" + path1 + " f:" + path2 + ")",
        queryJapanese.toString());

    criteria.setQuery("イキモノガカリ");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("イキモノガカリ",
        "+(art:イキモノガカリ* artF:イキモノガカリ* artR:イキモノガカリ* artRH:イキモノガカリ*) "
            + "+(f:" + path1 + " f:" + path2 + ")",
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
        "+m:music "+
        "+g:classicrock " +
        "+y:[1900 TO 2000] " +
        "+(f:" + path1+ ")",
        querySingle.toString());

    String path2 = sapa + "var" + sapa + "music2";
    musicFolders.add(
        new MusicFolder(Integer.valueOf(1), new File(path2), "music2", true, new Date()));
    Query queryMulti = QueryFactory.createQuery(criteria);
    assertEquals("multi",
        "+m:music "
        + "+g:classicrock "
        + "+y:[1900 TO 2000] "
        + "+(f:" + path1 + " f:" + path2 + ")",
        queryMulti.toString());

    criteria = new RandomSearchCriteria(
        50,            // count
        null,          // genre, 
        null,          // fromYear, 
        Integer.valueOf(2000),  // toYear, 
        musicFolders);       // musicFolders

    Query queryNullFrom = QueryFactory.createQuery(criteria);

    assertEquals("NullFrom",
        "+m:music "
        + "+y:[-2147483648 TO 2000] "
        + "+(f:" + path1 + " f:" + path2 + ")",
        queryNullFrom.toString());

    criteria = new RandomSearchCriteria(
        50,            // count
        null,          // genre, 
        Integer.valueOf(1900),  // fromYear, 
        null,          // toYear, 
        musicFolders);       // musicFolders
    Query queryNullTo = QueryFactory.createQuery(criteria);
    assertEquals("NullTo",
        "+m:music "
        + "+y:[1900 TO 2147483647] "
        + "+(f:" + path1 + " f:" + path2 + ")",
        queryNullTo.toString());

    criteria = new RandomSearchCriteria(
        50,            // count
        null,          // genre, 
        null,          // fromYear, 
        null,          // toYear, 
        musicFolders);       // musicFolders
    Query queryNullYear = QueryFactory.createQuery(criteria);
    assertEquals("NullYear",
        "+m:music "
            + "+(f:" + path1 + " f:" + path2 + ")",
        queryNullYear.toString());

  }

  public void testSearchByName() {
    Query querySingle = QueryFactory.searchByName("The Ventures", FieldNames.ARTIST);
    assertEquals("single",
        "+art:ventures*", querySingle.toString());
  }

  public void testArticle() {
    Query querySingle = QueryFactory.searchByName("THIS IS THE VENTURES", FieldNames.ARTIST);
    assertEquals("article",
        "+art:this* +art:is* +art:ventures*", querySingle.toString());
  }

  public void testAUX29() {
    Query querySingle = QueryFactory.searchByName("I'll be back.", FieldNames.ARTIST);
    assertEquals("aux",
        "+art:i* +art:ll* +art:be* +art:back*", querySingle.toString());
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
        "+(f:" + path1 + ")", querySingle.toString());

    musicFolders.add(
        new MusicFolder(Integer.valueOf(1), new File(path2), "music2", true, new Date()));
    Query queryMulti = QueryFactory.searchRandomAlbum(musicFolders);
    assertEquals("multi",
        "+(f:" + path1 + " f:" + path2 + ")",
        queryMulti.toString());
  }
  
  public void testSearchByNameMusicfIds() {

    String sepa = System.getProperty("file.separator");
    String path1 = sepa + "var" + sepa + "music1";

    List<MusicFolder> musicFolders = new ArrayList<MusicFolder>();
    musicFolders.add(
        new MusicFolder(Integer.valueOf(0), new File(path1), "music1", true, new Date()));
    Query querySingle = QueryFactory.searchRandomAlbumId3(musicFolders);
    assertEquals("single",
        "+(fId:0)",
        querySingle.toString());

    String path2 = sepa + "var" + sepa + "music2";
    musicFolders.add(
        new MusicFolder(Integer.valueOf(1), new File(path2), "music2", true, new Date()));
    Query queryMulti = QueryFactory.searchRandomAlbumId3(musicFolders);
    assertEquals("single",
        "+(fId:0 fId:1)",
        queryMulti.toString());

  }
}