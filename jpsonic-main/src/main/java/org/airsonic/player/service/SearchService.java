/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.tesshu.jpsonic.service.search.*;

import org.airsonic.player.dao.*;
import org.airsonic.player.domain.*;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.analysis.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.tesshu.jpsonic.service.search.IndexType.*;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Performs Lucene-based searching and indexing.
 *
 * @author Sindre Mehus
 * @version $Id$
 * @see MediaScannerService
 */
@Service
public class SearchService {

  private static final Logger LOG = LoggerFactory.getLogger(SearchService.class);

  private static final String LUCENE_DIR = "lucene7.4jp";

  private static final int MAX_NUM_SEGMENTS = 1;

  @Autowired
  private MediaFileService mediaFileService;
  @Autowired
  private ArtistDao artistDao;
  @Autowired
  private AlbumDao albumDao;

  private IndexWriter artistWriter;
  private IndexWriter artistId3Writer;
  private IndexWriter albumWriter;
  private IndexWriter albumId3Writer;
  private IndexWriter songWriter;

  private Analyzer analyzer = AnalyzerFactory.getInstance().getAnalyzer();

  private static Function<Long, Integer> round = (i) -> {
    // return NumericUtils.floatToSortableInt(i);
    return i.intValue();
  };

  private static Function<String, Integer> parseId = (s) -> {
    return Integer.valueOf(s);
  };

  private File getIndexDirectory(IndexType indexType) {
    return new File(getIndexRootDirectory(), indexType.toString().toLowerCase());
  }

  private File getIndexRootDirectory() {
    return new File(SettingsService.getJpsonicHome(), LUCENE_DIR);
  }

  public void startIndexing() {
    try {
      artistWriter = createIndexWriter(ARTIST);
      artistId3Writer = createIndexWriter(ARTIST_ID3);
      albumWriter = createIndexWriter(ALBUM);
      albumId3Writer = createIndexWriter(ALBUM_ID3);
      songWriter = createIndexWriter(SONG);
    } catch (Exception x) {
      LOG.error("Failed to create search index.", x);
    }
  }

  private IndexWriter createIndexWriter(IndexType indexType) throws IOException {
    File dir = getIndexDirectory(indexType);
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    return new IndexWriter(FSDirectory.open(dir.toPath()), config);
  }

  public void index(MediaFile mediaFile) {
    try {
      if (mediaFile.isFile()) {
        songWriter.addDocument(SONG.createDocument(mediaFile));
      } else if (mediaFile.isAlbum()) {
        albumWriter.addDocument(ALBUM.createDocument(mediaFile));
      } else {
        artistWriter.addDocument(ARTIST.createDocument(mediaFile));
      }
    } catch (Exception x) {
      LOG.error("Failed to create search index for " + mediaFile, x);
    }
  }

  public void index(Artist artist, MusicFolder musicFolder) {
    try {
      artistId3Writer.addDocument(ARTIST_ID3.createDocument(artist, musicFolder));
    } catch (Exception x) {
      LOG.error("Failed to create search index for " + artist, x);
    }
  }

  public void index(Album album) {
    try {
      albumId3Writer.addDocument(ALBUM_ID3.createDocument(album));
    } catch (Exception x) {
      LOG.error("Failed to create search index for " + album, x);
    }
  }

  public void stopIndexing() {
    stopIndexing(artistWriter);
    stopIndexing(artistId3Writer);
    stopIndexing(albumWriter);
    stopIndexing(albumId3Writer);
    stopIndexing(songWriter);
  }

  private void stopIndexing(IndexWriter writer) {
    try {
      writer.flush();
      writer.forceMerge(MAX_NUM_SEGMENTS);
      LOG.info("Success to merge index : " + writer.getDirectory());
      writer.close();
      LOG.info("Success to create search index : " + writer.getDirectory());
    } catch (Exception x) {
      LOG.error("Failed to create search index.", x);
    } finally {
      FileUtil.closeQuietly(writer);
    }
  }

  public SearchResult search(
      SearchCriteria criteria, List<MusicFolder> musicFolders, IndexType indexType) {

    final int offset = criteria.getOffset();
    final int count = criteria.getCount();

    final SearchResult result = new SearchResult();
    result.setOffset(offset);

    if (count <= 0) {
      return result;
    }

    final Query query = QueryFactory.createQuery(criteria, musicFolders, indexType);

    try (IndexReader reader = createIndexReader(indexType)) {

      IndexSearcher searcher = new IndexSearcher(reader);
      TopDocs topDocs = searcher.search(query, offset + count);
      result.setTotalHits(round.apply(topDocs.totalHits));

      int start = round.apply(Math.min(offset, topDocs.totalHits));
      int end = round.apply(Math.min(start + count, topDocs.totalHits));

      for (int i = start; i < end; i++) {
        Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
        int id = parseId.apply(doc.get(FieldNames.ID));
        switch (indexType) {
          case SONG:
          case ARTIST:
          case ALBUM:
            addFileIfAnyMatch(result.getMediaFiles(), id);
            break;
          case ARTIST_ID3:
            addArtistIfAnyMatch(result.getArtists(), id);
            break;
          case ALBUM_ID3:
            addAlbumIfAnyMatch(result.getAlbums(), id);
            break;
          default:
            break;
        }
      }

    } catch (IOException e) {
      LOG.error("Failed to execute Lucene search.", e);
    }
    return result;
  }

  private IndexReader createIndexReader(IndexType indexType) throws IOException {
    //TODO Use manager class
    File dir = getIndexDirectory(indexType);
    return DirectoryReader.open(FSDirectory.open(dir.toPath()));
  }

  private void addFileIfAnyMatch(List<MediaFile> dist, int id) {
    if (!dist.stream().anyMatch(m -> id == m.getId())) {
      MediaFile mediaFile = mediaFileService.getMediaFile(id);
      if (!isEmpty(mediaFile)) {
        dist.add(mediaFile);
      }
    }
  }

  private void addArtistIfAnyMatch(List<Artist> dist, int id) {
    if (!dist.stream().anyMatch(m -> id == m.getId())) {
      Artist artist = artistDao.getArtist(id);
      if (!isEmpty(artist)) {
        dist.add(artist);
      }
    }
  }

  private void addAlbumIfAnyMatch(List<Album> dist, int id) {
    if (!dist.stream().anyMatch(m -> id == m.getId())) {
      Album album = albumDao.getAlbum(id);
      if (!isEmpty(album)) {
        dist.add(album);
      }
    }
  }

  public <T> ParamSearchResult<T> searchByName(
      String name, int offset, int count, List<MusicFolder> folderList, Class<T> clazz) {

    IndexType indexType = null;
    String field = null;
    if (clazz.isAssignableFrom(Album.class)) {
      indexType = IndexType.ALBUM_ID3;
      field = FieldNames.ALBUM;
    } else if (clazz.isAssignableFrom(Artist.class)) {
      indexType = IndexType.ARTIST_ID3;
      field = FieldNames.ARTIST;
    } else if (clazz.isAssignableFrom(MediaFile.class)) {
      indexType = IndexType.SONG;
      field = FieldNames.TITLE;
    }

    ParamSearchResult<T> result = new ParamSearchResult<T>();
    // we only support album, artist, and song for now
    if (isEmpty(indexType) || isEmpty(field)) {
      return result;
    }

    result.setOffset(offset);

    try (IndexReader reader = createIndexReader(indexType)) {

      IndexSearcher searcher = new IndexSearcher(reader);
      Query query = QueryFactory.searchByName(name, field);
      Sort sort = new Sort(new SortField(field, SortField.Type.STRING));
      TopDocs topDocs = searcher.search(query, offset + count, sort);

      int start = round.apply(Math.min(offset, topDocs.totalHits));
      int end = round.apply(Math.min(start + count, topDocs.totalHits));

      for (int i = start; i < end; i++) {
        Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
        final IndexType type = indexType;
        switch (type) {
          case SONG:
            MediaFile mediaFile =
                mediaFileService.getMediaFile(parseId.apply(doc.get(FieldNames.ID)));
            CollectionUtils.addIgnoreNull(result.getItems(), clazz.cast(mediaFile));
            break;
          case ARTIST_ID3:
            Artist artist = artistDao.getArtist(parseId.apply(doc.get(FieldNames.ID)));
            CollectionUtils.addIgnoreNull(result.getItems(), clazz.cast(artist));
            break;
          case ALBUM_ID3:
            Album album = albumDao.getAlbum(parseId.apply(doc.get(FieldNames.ID)));
            CollectionUtils.addIgnoreNull(result.getItems(), clazz.cast(album));
            break;
          default:
            break;
        }
      }
    } catch (IOException e) {
      LOG.error("Failed to execute Lucene search.", e);
    }
    return result;
  }

  /**
   * Returns a number of random songs.
   *
   * @param criteria Search criteria.
   * @return List of random songs.
   */
  public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria) {

    List<MediaFile> result = new ArrayList<MediaFile>();

    try (IndexReader reader = createIndexReader(SONG)) {

      final Query query = QueryFactory.createQuery(criteria);

      IndexSearcher searcher = new IndexSearcher(reader);
      TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
      List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
      Random random = new Random(System.currentTimeMillis());

      while (!scoreDocs.isEmpty() && result.size() < criteria.getCount()) {
        int index = random.nextInt(scoreDocs.size());
        Document doc = searcher.doc(scoreDocs.remove(index).doc);
        int id = parseId.apply(doc.get(FieldNames.ID));
        try {
          CollectionUtils.addIgnoreNull(result, mediaFileService.getMediaFile(id));
        } catch (Exception x) {
          LOG.warn("Failed to get media file " + id);
        }
      }

    } catch (Throwable x) {
      LOG.error("Failed to search or random songs.", x);
    }

    return result;
  }

  /**
   * Returns a number of random albums.
   *
   * @param count        Number of albums to return.
   * @param musicFolders Only return albums from these folders.
   * @return List of random albums.
   */
  public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {

    List<MediaFile> result = new ArrayList<MediaFile>();

    try (IndexReader reader = createIndexReader(ALBUM)) {

      IndexSearcher searcher = new IndexSearcher(reader);
      Query query = QueryFactory.searchRandomAlbum(musicFolders);
      TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
      List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
      Random random = new Random(System.currentTimeMillis());

      while (!scoreDocs.isEmpty() && result.size() < count) {
        int index = random.nextInt(scoreDocs.size());
        Document doc = searcher.doc(scoreDocs.remove(index).doc);
        int id = parseId.apply(doc.get(FieldNames.ID));
        try {
          CollectionUtils.addIgnoreNull(result, mediaFileService.getMediaFile(id));
        } catch (Exception x) {
          LOG.warn("Failed to get media file " + id, x);
        }
      }

    } catch (IOException e) {
      LOG.error("Failed to search for random albums.", e);
    }

    return result;
  }

  /**
   * Returns a number of random albums, using ID3 tag.
   *
   * @param count        Number of albums to return.
   * @param musicFolders Only return albums from these folders.
   * @return List of random albums.
   */
  public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {

    List<Album> result = new ArrayList<Album>();

    try (IndexReader reader = createIndexReader(ALBUM_ID3)) {
      IndexSearcher searcher = new IndexSearcher(reader);
      Query query = QueryFactory.searchRandomAlbumId3(musicFolders);

      TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
      List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
      Random random = new Random(System.currentTimeMillis());

      while (!scoreDocs.isEmpty() && result.size() < count) {
        int index = random.nextInt(scoreDocs.size());
        Document doc = searcher.doc(scoreDocs.remove(index).doc);
        int id = parseId.apply(doc.get(FieldNames.ID));
        try {
          CollectionUtils.addIgnoreNull(result, albumDao.getAlbum(id));
        } catch (Exception x) {
          LOG.warn("Failed to get album file " + id, x);
        }
      }

    } catch (IOException e) {
      LOG.error("Failed to search for random albums.", e);
    }

    return result;
  }

}
