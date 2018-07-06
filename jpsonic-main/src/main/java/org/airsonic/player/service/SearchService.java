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

 Copyright 2018 (C) tesshu.com
 Based upon Airsonic  Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.function.BiConsumer;

import static org.airsonic.player.service.SearchService.IndexType.*;
import static org.apache.lucene.analysis.standard.StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;
import static org.apache.lucene.analysis.standard.StandardAnalyzer.STOP_WORDS_SET;
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

	@SuppressWarnings("unused")
	private static final Version LUCENE_VERSION = Version.LUCENE_7_4_0;

	private static final String LUCENE_DIR = "lucene7.4jp";

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

	private static Function<Long, Integer> round = (i) -> {
		// return NumericUtils.floatToSortableInt(i);
		return i.intValue();
	};

	private static Function<String, Integer> parseId = (s) -> {
		return Integer.valueOf(s);
	};

	private static Function<String, String> normalizeGenre = (genre) -> {
		return genre.toLowerCase().replace(" ", "").replace("-", "");
	};

	public SearchService() {
		removeLocks();
	}

	private void removeLocks() {
		for (IndexType indexType : IndexType.values()) {
			Directory dir = null;
			try {
				dir = FSDirectory.open(getIndexDirectory(indexType).toPath());
			} catch (Exception x) {
				LOG.warn("Failed to remove Lucene lock file in " + dir, x);
			} finally {
				FileUtil.closeQuietly(dir);
			}
		}
	}

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
		IndexWriterConfig config = new IndexWriterConfig(createAnalyzer());
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
		try {
			artistWriter.close();
			artistId3Writer.close();
			albumWriter.close();
			albumId3Writer.close();
			songWriter.close();
			LOG.error("Success to create search index.");
		} catch (Exception x) {
			LOG.error("Failed to create search index.", x);
		} finally {
			FileUtil.closeQuietly(artistId3Writer);
			FileUtil.closeQuietly(artistWriter);
			FileUtil.closeQuietly(albumWriter);
			FileUtil.closeQuietly(albumId3Writer);
			FileUtil.closeQuietly(songWriter);
		}
	}

	public SearchResult search(SearchCriteria criteria, List<MusicFolder> musicFolders, IndexType indexType) {
		SearchResult result = new SearchResult();
		int offset = criteria.getOffset();
		int count = criteria.getCount();
		result.setOffset(offset);
		if (count <= 0)
			return result;
		IndexReader reader = null;
		try {
			reader = createIndexReader(indexType);
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = createAnalyzer();
			MultiFieldQueryParser queryParser = new MultiFieldQueryParser(indexType.getFields(), analyzer,
					indexType.getBoosts());
			BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

			booleanQuery.add(queryParser.parse(analyzeJPQuery(criteria.getQuery())), BooleanClause.Occur.MUST);

			List<PhraseQuery> phraseQueries = new ArrayList<>();
			for (MusicFolder musicFolder : musicFolders) {
				if (indexType == ALBUM_ID3 || indexType == ARTIST_ID3) {
					phraseQueries.add(new PhraseQuery(Fields.name.FOLDER_ID, new BytesRef(musicFolder.getId())));
				} else {
					phraseQueries.add(new PhraseQuery(Fields.name.FOLDER, musicFolder.getPath().getPath()));
				}
			}
			for (PhraseQuery phrase : phraseQueries) {
				booleanQuery.add(phrase, BooleanClause.Occur.MUST);
			}

			TopDocs topDocs = searcher.search(booleanQuery.build(), offset + count);

			result.setTotalHits(round.apply(topDocs.totalHits));
			int start = round.apply(Math.min(offset, topDocs.totalHits));
			int end = round.apply(Math.min(start + count, topDocs.totalHits));
			for (int i = start; i < end; i++) {
				Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
				switch (indexType) {
				case SONG:
				case ARTIST:
				case ALBUM:
					MediaFile mediaFile = mediaFileService.getMediaFile(parseId.apply(doc.get(Fields.name.ID)));
					CollectionUtils.addIgnoreNull(result.getMediaFiles(), mediaFile);
					break;
				case ARTIST_ID3:
					Artist artist = artistDao.getArtist(parseId.apply(doc.get(Fields.name.ID)));
					CollectionUtils.addIgnoreNull(result.getArtists(), artist);
					break;
				case ALBUM_ID3:
					Album album = albumDao.getAlbum(parseId.apply(doc.get(Fields.name.ID)));
					CollectionUtils.addIgnoreNull(result.getAlbums(), album);
					break;
				default:
					break;
				}
			}

		} catch (Throwable x) {
			LOG.error("Failed to execute Lucene search.", x);
		} finally {
			FileUtil.closeQuietly(reader);
		}
		return result;
	}

	private IndexReader createIndexReader(IndexType indexType) throws IOException {
		File dir = getIndexDirectory(indexType);
		return DirectoryReader.open(FSDirectory.open(dir.toPath()));
	}

	@Deprecated
	@SuppressWarnings("unused")
	private String analyzeQuery(String query) throws IOException {
		StringBuilder result = new StringBuilder();
		StandardTokenizer tokenizer = new StandardTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY);
		tokenizer.setReader(new StringReader(query));
		tokenizer.reset();
		ASCIIFoldingFilter filter = new ASCIIFoldingFilter(tokenizer);
		while (filter.incrementToken())
			result.append(filter.getAttribute(CharTermAttribute.class).toString()).append("* ");
		filter.close();
		return result.toString();
	}

	private String analyzeJPQuery(String query) throws IOException {
		StringBuilder result = new StringBuilder();
		JapaneseTokenizer tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.NORMAL);
		TokenStream tokenStream = new LowerCaseFilter(tokenizer);
		tokenizer.setReader(new StringReader(query));
		tokenizer.reset();
		tokenStream = new CJKWidthFilter(tokenStream);
		tokenStream = new JapaneseKatakanaStemFilter(tokenStream);
		ASCIIFoldingFilter filter = new ASCIIFoldingFilter(tokenStream);
		while (filter.incrementToken())
			result.append(filter.getAttribute(CharTermAttribute.class).toString()).append("* ");
		filter.close();
		return result.toString();
	}

	public <T> ParamSearchResult<T> searchByName(String name, int offset, int count, List<MusicFolder> folderList,
			Class<T> clazz) {
		IndexType indexType = null;
		String field = null;
		if (clazz.isAssignableFrom(Album.class)) {
			indexType = IndexType.ALBUM_ID3;
			field = Fields.name.ALBUM;
		} else if (clazz.isAssignableFrom(Artist.class)) {
			indexType = IndexType.ARTIST_ID3;
			field = Fields.name.ARTIST;
		} else if (clazz.isAssignableFrom(MediaFile.class)) {
			indexType = IndexType.SONG;
			field = Fields.name.TITLE;
		}
		ParamSearchResult<T> result = new ParamSearchResult<T>();
		// we only support album, artist, and song for now
		if (isEmpty(indexType) || isEmpty(field))
			return result;

		result.setOffset(offset);

		IndexReader reader = null;

		try {
			reader = createIndexReader(indexType);
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = createAnalyzer();
			QueryParser queryParser = new QueryParser(field, analyzer);

			Query q = queryParser.parse(name + "*");

			Sort sort = new Sort(new SortField(field, SortField.Type.STRING));
			TopDocs topDocs = searcher.search(q, offset + count, sort);

			int start = round.apply(Math.min(offset, topDocs.totalHits));
			int end = round.apply(Math.min(start + count, topDocs.totalHits));

			for (int i = start; i < end; i++) {
				Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
				switch (indexType) {
				case SONG:
					MediaFile mediaFile = mediaFileService.getMediaFile(parseId.apply(doc.get(Fields.name.ID)));
					CollectionUtils.addIgnoreNull(result.getItems(), clazz.cast(mediaFile));
					break;
				case ARTIST_ID3:
					Artist artist = artistDao.getArtist(parseId.apply(doc.get(Fields.name.ID)));
					CollectionUtils.addIgnoreNull(result.getItems(), clazz.cast(artist));
					break;
				case ALBUM_ID3:
					Album album = albumDao.getAlbum(parseId.apply(doc.get(Fields.name.ID)));
					CollectionUtils.addIgnoreNull(result.getItems(), clazz.cast(album));
					break;
				default:
					break;
				}
			}
		} catch (Throwable x) {
			LOG.error("Failed to execute Lucene search.", x);
		} finally {
			FileUtil.closeQuietly(reader);
		}
		return result;
	}

	/**
	 * Returns a number of random songs.
	 *
	 * @param criteria
	 *            Search criteria.
	 * @return List of random songs.
	 */
	public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria) {
		List<MediaFile> result = new ArrayList<MediaFile>();

		IndexReader reader = null;
		try {
			reader = createIndexReader(SONG);
			IndexSearcher searcher = new IndexSearcher(reader);

			BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
			booleanQuery.add(
					new TermQuery(new Term(Fields.name.MEDIA_TYPE, MediaFile.MediaType.MUSIC.name().toLowerCase())),
					BooleanClause.Occur.MUST);
			if (!isEmpty(criteria.getGenre())) {
				String genre = normalizeGenre.apply(criteria.getGenre());
				booleanQuery.add(new TermQuery(new Term(Fields.name.GENRE, genre)), BooleanClause.Occur.MUST);
			}
			if (!isEmpty(criteria.getFromYear()) || !isEmpty(criteria.getToYear())) {
				Query rangeQuery = IntPoint.newRangeQuery(Fields.name.YEAR, criteria.getFromYear(),
						criteria.getToYear());
				booleanQuery.add(rangeQuery, BooleanClause.Occur.MUST);
			}
			for (MusicFolder musicFolder : criteria.getMusicFolders()) {
				booleanQuery.add(new PhraseQuery(Fields.name.FOLDER, musicFolder.getPath().getPath()),
						BooleanClause.Occur.MUST);
			}

			TopDocs topDocs = searcher.search(booleanQuery.build(), Integer.MAX_VALUE);
			List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
			Random random = new Random(System.currentTimeMillis());

			while (!scoreDocs.isEmpty() && result.size() < criteria.getCount()) {
				int index = random.nextInt(scoreDocs.size());
				Document doc = searcher.doc(scoreDocs.remove(index).doc);
				int id = parseId.apply(doc.get(Fields.name.ID));
				try {
					CollectionUtils.addIgnoreNull(result, mediaFileService.getMediaFile(id));
				} catch (Exception x) {
					LOG.warn("Failed to get media file " + id);
				}
			}

		} catch (Throwable x) {
			LOG.error("Failed to search or random songs.", x);
		} finally {
			FileUtil.closeQuietly(reader);
		}
		return result;
	}

	/**
	 * Returns a number of random albums.
	 *
	 * @param count
	 *            Number of albums to return.
	 * @param musicFolders
	 *            Only return albums from these folders.
	 * @return List of random albums.
	 */
	public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {
		List<MediaFile> result = new ArrayList<MediaFile>();

		IndexReader reader = null;
		try {
			reader = createIndexReader(ALBUM);
			IndexSearcher searcher = new IndexSearcher(reader);

			BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
			for (MusicFolder musicFolder : musicFolders) {
				booleanQuery.add(new PhraseQuery(Fields.name.FOLDER, musicFolder.getPath().getPath()),
						BooleanClause.Occur.MUST);
			}

			TopDocs topDocs = searcher.search(booleanQuery.build(), Integer.MAX_VALUE);
			List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
			Random random = new Random(System.currentTimeMillis());

			while (!scoreDocs.isEmpty() && result.size() < count) {
				int index = random.nextInt(scoreDocs.size());
				Document doc = searcher.doc(scoreDocs.remove(index).doc);
				int id = parseId.apply(doc.get(Fields.name.ID));
				try {
					CollectionUtils.addIgnoreNull(result, mediaFileService.getMediaFile(id));
				} catch (Exception x) {
					LOG.warn("Failed to get media file " + id, x);
				}
			}

		} catch (Throwable x) {
			LOG.error("Failed to search for random albums.", x);
		} finally {
			FileUtil.closeQuietly(reader);
		}
		return result;
	}

	/**
	 * Returns a number of random albums, using ID3 tag.
	 *
	 * @param count
	 *            Number of albums to return.
	 * @param musicFolders
	 *            Only return albums from these folders.
	 * @return List of random albums.
	 */
	public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {
		List<Album> result = new ArrayList<Album>();
		IndexReader reader = null;
		try {
			reader = createIndexReader(ALBUM_ID3);
			IndexSearcher searcher = new IndexSearcher(reader);

			BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
			for (MusicFolder musicFolder : musicFolders) {
				booleanQuery.add(new PhraseQuery(Fields.name.FOLDER, musicFolder.getPath().getPath()),
						BooleanClause.Occur.MUST);
			}

			TopDocs topDocs = searcher.search(booleanQuery.build(), Integer.MAX_VALUE);
			List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
			Random random = new Random(System.currentTimeMillis());

			while (!scoreDocs.isEmpty() && result.size() < count) {
				int index = random.nextInt(scoreDocs.size());
				Document doc = searcher.doc(scoreDocs.remove(index).doc);
				int id = parseId.apply(doc.get(Fields.name.ID));
				try {
					CollectionUtils.addIgnoreNull(result, albumDao.getAlbum(id));
				} catch (Exception x) {
					LOG.warn("Failed to get album file " + id, x);
				}
			}

		} catch (Throwable x) {
			LOG.error("Failed to search for random albums.", x);
		} finally {
			FileUtil.closeQuietly(reader);
		}
		return result;
	}

	private static final class Fields {

		private static final class name {
			private static final String ID = "id";
			private static final String TITLE = "title";
			private static final String ALBUM = "album";
			private static final String ARTIST = "artist";
			private static final String GENRE = "genre";
			private static final String YEAR = "year";
			private static final String MEDIA_TYPE = "mediaType";
			private static final String FOLDER = "folder";
			private static final String FOLDER_ID = "folderId";
		}

		private static final BiConsumer<Document, Integer> id = (d, i) -> {
			d.add(new NumericDocValuesField(name.ID, i));
			d.add(new StoredField(name.ID, i));
		};

		private static final BiConsumer<Document, String> title = (d, s) -> {
			if (isEmpty(s))
				return;
			d.add(new TextField(name.TITLE, s, Store.YES));
			d.add(new SortedDocValuesField(name.TITLE, new BytesRef(s)));
		};

		private static final BiConsumer<Document, String> artist = (d, s) -> {
			if (isEmpty(s))
				return;
			d.add(new TextField(name.ARTIST, s, Store.YES));
			d.add(new SortedDocValuesField(name.ARTIST, new BytesRef(s)));
		};

		private static final BiConsumer<Document, String> folder = (d, s) -> {
			d.add(new StringField(name.FOLDER, s, Store.YES));
			d.add(new SortedDocValuesField(name.FOLDER, new BytesRef(s)));
		};

		private static final BiConsumer<Document, String> album = (d, s) -> {
			if (isEmpty(s))
				return;
			d.add(new TextField(name.ALBUM, s, Store.YES));
			d.add(new SortedDocValuesField(name.ALBUM, new BytesRef(s)));
		};

		private static final BiConsumer<Document, Integer> folderId = (d, i) -> d
				.add(new NumericDocValuesField(name.FOLDER_ID, i));;

		private static final BiConsumer<Document, Integer> year = (d, i) -> {
			if (isEmpty(i))
				return;
			d.add(new NumericDocValuesField(name.YEAR, i));
		};

		private static final BiConsumer<Document, String> mediaType = (d, s) -> {
			if (isEmpty(s))
				return;
			d.add(new StringField(name.MEDIA_TYPE, s, Store.YES));
			d.add(new SortedDocValuesField(name.MEDIA_TYPE, new BytesRef(s)));
		};

		private static final BiConsumer<Document, String> genre = (d, s) -> {
			if (isEmpty(s))
				return;
			d.add(new TextField(name.GENRE, s, Store.YES));
			d.add(new SortedDocValuesField(name.GENRE, new BytesRef(normalizeGenre.apply(s))));
		};

	}

	public static enum IndexType {
		SONG(new String[] { Fields.name.TITLE, Fields.name.ARTIST }, Fields.name.TITLE) {
			public Document createDocument(MediaFile mediaFile) {
				Document doc = new Document();
				Fields.id.accept(doc, mediaFile.getId());
				Fields.mediaType.accept(doc, mediaFile.getMediaType().name());
				Fields.title.accept(doc, mediaFile.getTitle());
				Fields.artist.accept(doc, mediaFile.getArtist());
				Fields.genre.accept(doc, mediaFile.getGenre());
				Fields.year.accept(doc, mediaFile.getYear());
				Fields.folder.accept(doc, mediaFile.getFolder());
				return doc;
			}
		},
		ALBUM(new String[] { Fields.name.ALBUM, Fields.name.ARTIST, Fields.name.FOLDER }, Fields.name.ALBUM) {
			public Document createDocument(MediaFile mediaFile) {
				Document doc = new Document();
				Fields.id.accept(doc, mediaFile.getId());
				Fields.artist.accept(doc, mediaFile.getArtist());
				Fields.album.accept(doc, mediaFile.getAlbumName());
				Fields.folder.accept(doc, mediaFile.getFolder());
				return doc;
			}
		},
		ALBUM_ID3(new String[] { Fields.name.ALBUM, Fields.name.ARTIST, Fields.name.FOLDER_ID }, Fields.name.ALBUM) {
			@Override
			public Document createDocument(Album album) {
				Document doc = new Document();
				Fields.id.accept(doc, album.getId());
				Fields.artist.accept(doc, album.getArtist());
				Fields.album.accept(doc, album.getName());
				Fields.folderId.accept(doc, album.getFolderId());
				return doc;
			}
		},
		ARTIST(new String[] { Fields.name.ARTIST, Fields.name.FOLDER }, null) {
			@Override
			public Document createDocument(MediaFile mediaFile) {
				Document doc = new Document();
				doc.add(new StoredField(Fields.name.ID, mediaFile.getId()));
				Fields.artist.accept(doc, mediaFile.getArtist());
				Fields.folder.accept(doc, mediaFile.getFolder());
				return doc;
			}
		},

		ARTIST_ID3(new String[] { Fields.name.ARTIST }, null) {
			@Override
			public Document createDocument(Artist artist, MusicFolder musicFolder) {
				Document doc = new Document();
				Fields.id.accept(doc, artist.getId());
				Fields.artist.accept(doc, artist.getName());
				Fields.folderId.accept(doc, musicFolder.getId());
				return doc;
			}
		};

		private final String[] fields;

		private final Map<String, Float> boosts;

		private IndexType(String[] fields, String boostedField) {
			this.fields = fields;
			boosts = new HashMap<String, Float>();
			if (boostedField != null) {
				boosts.put(boostedField, 2.0F);
			}
		}

		public String[] getFields() {
			return fields;
		}

		protected Document createDocument(MediaFile mediaFile) {
			throw new UnsupportedOperationException();
		}

		protected Document createDocument(Artist artist, MusicFolder musicFolder) {
			throw new UnsupportedOperationException();
		}

		protected Document createDocument(Album album) {
			throw new UnsupportedOperationException();
		}

		public Map<String, Float> getBoosts() {
			return boosts;
		}
	}

	private Analyzer createAnalyzer() {
		return new JapaneseAnalyzer();
	}

	@Deprecated
	@SuppressWarnings("unused")
	private class AirsonicAnalyzer extends StopwordAnalyzerBase {

		public AirsonicAnalyzer(CharArraySet stopWords) {
			super(stopWords);
		}

		public AirsonicAnalyzer() {
			this(STOP_WORDS_SET);
		}

		@Override
		protected TokenStream normalize(String fieldName, TokenStream in) {
			TokenStream result = new StandardFilter(in);
			return new ASCIIFoldingFilter(new LowerCaseFilter(result));
		}

		@Override
		protected TokenStreamComponents createComponents(String fieldName) {
			StandardTokenizer tokenizer = new StandardTokenizer();
			tokenizer.setMaxTokenLength(DEFAULT_MAX_TOKEN_LENGTH);
			TokenStream tokenStream = new StandardFilter(tokenizer);
			tokenStream = new LowerCaseFilter(tokenStream);
			tokenStream = new StopFilter(tokenStream, STOP_WORDS_SET);
			tokenStream = new ASCIIFoldingFilter(tokenStream);
			return new TokenStreamComponents(tokenizer, tokenStream) {
				@Override
				protected void setReader(final Reader reader) {
					tokenizer.setMaxTokenLength(DEFAULT_MAX_TOKEN_LENGTH);
					super.setReader(reader);
				}
			};
		}

	}

}
