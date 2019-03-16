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
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

/*
 * The query syntax has not changed significantly since Lucene 1.3.
 * (A slight difference)
 */
public class QueryFactoryTestCase extends TestCase {

  public void testWithSearchCriteria() {

    String sepa = System.getProperty("file.separator");
    String path1 = sepa + "var" + sepa + "music1";

    SearchCriteria criteria = new SearchCriteria();
    criteria.setQuery("test1");

    List<MusicFolder> musicFolders = new ArrayList<MusicFolder>();
    musicFolders.add(
        new MusicFolder(Integer.valueOf(0), new File(path1), "music1", true, new java.util.Date()));

    Query querySingle = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("single",
        //"+(artist:test1* folder:test1*) " +
        "+((artist:test* artistF:test1* artistR:test* artistRH:test1*) "
        + "(artist:1* artistR:1*)) "
        + "+spanOr([folder:" + path1 + "])", querySingle.toString());

    criteria.setQuery("test1 test2");
    String path2 = sepa + "var" + sepa + "music2";
    musicFolders.add(
        new MusicFolder(Integer.valueOf(1), new File(path2), "music2", true, new java.util.Date()));
    Query queryMulti = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("multi",
        //"+((artist:test1* folder:test1*) (artist:test2* folder:test2*)) " +
        "+((artist:test* artistF:test1 test2* artistR:test* artistRH:test1 test2*) "
        + "(artist:1* artistR:1*) "
        + "(artist:test* artistR:test*) (artist:2* artistR:2*)) "
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])",
        queryMulti.toString());

    criteria.setCount(50); // Not used in queries
    criteria.setOffset(0); // Not used in queries
    Query queryFull = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("full",
        //"+((artist:test1* folder:test1*) (artist:test2* folder:test2*)) " +
        "+((artist:test* artistF:test1 test2* artistR:test* artistRH:test1 test2*) "
        + "(artist:1* artistR:1*) "
        + "(artist:test* artistR:test*) "
        + "(artist:2* artistR:2*)) "
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])",
        queryFull.toString());

    criteria = new SearchCriteria();
    criteria.setQuery("みんなの歌");
    Query queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("japanese",
        "+((artist:みんな* artistF:みんなの歌* artistR:みんな* artistRH:みんなの歌*) "
        + "(artist:歌* artistR:歌*)) "
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])", queryJapanese.toString());

    criteria.setQuery("いきものがかり");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("japanese",
        "+(artist:いきものがかり* artistF:いきものがかり* artistR:いきものがかり* artistRH:いきものがかり*) "
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])", queryJapanese.toString());

    criteria.setQuery("いきもの　がかり");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("japanese",
        "+((artist:いき* artistF:いきもの がかり* artistR:いき* artistRH:いきもの がかり*) "
        + "(artist:がかり* artistR:がかり*)) "
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])",
        queryJapanese.toString());

    criteria.setQuery("いきものガカリ");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("japanese",
        "+((artist:いき* artistF:いきものガカリ* artistR:いき* artistRH:いきものガカリ*) "
            + "(artist:ガ* artistR:ガ*) (artist:カリ* artistR:カリ*)) "
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])",
        queryJapanese.toString());

    criteria.setQuery("イキモノガカリ");
    queryJapanese = QueryFactory.createQuery(criteria, musicFolders, IndexType.ARTIST);
    assertEquals("japanese",
        "+(artist:イキモノガカリ* artistF:イキモノガカリ* artistR:イキモノガカリ* artistRH:イキモノガカリ*) "
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])",
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
        "+spanOr([folder:" + path1 + "])",
        querySingle.toString());

    String path2 = sapa + "var" + sapa + "music2";
    musicFolders.add(
        new MusicFolder(Integer.valueOf(1), new File(path2), "music2", true, new Date()));
    Query queryMulti = QueryFactory.createQuery(criteria);
    assertEquals("multi",
        "+mediaType:music "
        + "+genre:classicrock "
        + "+year:[1900 TO 2000] "
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])",
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
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])",
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
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])",
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
        + "+spanOr([folder:" + path1 + ", folder:" + path2 + "])",
        queryNullYear.toString());

  }

  public void testSearchByName() {
    Query querySingle = QueryFactory.searchByName("The Ventures", FieldNames.ARTIST);
    assertEquals("single",
        "artist:ventures*", querySingle.toString());
  }

  public void testArticle() {
    Query querySingle = QueryFactory.searchByName("THIS IS THE VENTURES", FieldNames.ARTIST);
    assertEquals("article",
        "artist:this* artist:is* artist:ventures*", querySingle.toString());
  }

  public void testAUX29() {
    Query querySingle = QueryFactory.searchByName("I'll be back.", FieldNames.ARTIST);
    assertEquals("aux",
        "artist:i* artist:ll* artist:be* artist:back*", querySingle.toString());
  }

  public void testSearchByNameMusicFolderPath() {
    
    String SEPA = System.getProperty("file.separator");
    String path1 = SEPA + "var" + SEPA + "music1";
    String path2 = SEPA + "var" + SEPA + "music2";

    List<MusicFolder> musicFolders = new ArrayList<MusicFolder>();
    musicFolders.add(
        new MusicFolder(Integer.valueOf(0), new File(path1), "music1", true, new Date()));
    Query querySingle = QueryFactory.searchByNameMusicFolderPath(musicFolders);
    assertEquals("single",
        "spanOr([folder:" + path1 + "])", querySingle.toString());

    musicFolders.add(
        new MusicFolder(Integer.valueOf(1), new File(path2), "music2", true, new Date()));
    Query queryMulti = QueryFactory.searchByNameMusicFolderPath(musicFolders);
    assertEquals("single",
        "spanOr([folder:" + path1 + ", folder:" + path2 + "])", queryMulti.toString());
  }
  
  public void testSearchByNameMusicFolderIds() {

    String sepa = System.getProperty("file.separator");
    String path1 = sepa + "var" + sepa + "music1";

    List<MusicFolder> musicFolders = new ArrayList<MusicFolder>();
    musicFolders.add(
        new MusicFolder(Integer.valueOf(0), new File(path1), "music1", true, new Date()));
    Query querySingle = QueryFactory.searchByNameMusicFolderIds(musicFolders);
    byte[] bytes = new byte[Integer.BYTES];
    NumericUtils.intToSortableBytes(Integer.valueOf(0), bytes, 0);
    BytesRef refId0 = new BytesRef(bytes);
    assertEquals("single",
        "spanOr([folderId:" + refId0 + "])", querySingle.toString());

    String path2 = sepa + "var" + sepa + "music2";
    musicFolders.add(
        new MusicFolder(Integer.valueOf(1), new File(path2), "music2", true, new Date()));
    bytes = new byte[Integer.BYTES];
    NumericUtils.intToSortableBytes(Integer.valueOf(1), bytes, 0);
    BytesRef refId1 = new BytesRef(bytes);
    Query queryMulti = QueryFactory.searchByNameMusicFolderIds(musicFolders);
    assertEquals("single",
        "spanOr([folderId:" + refId0 + ", folderId:" + refId1 + "])",
        queryMulti.toString());

  }
}