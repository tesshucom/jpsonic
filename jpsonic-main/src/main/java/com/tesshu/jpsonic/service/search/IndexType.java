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

import com.ibm.icu.text.Transliterator;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;

import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * IndexType.
 * This class is a division of what was once part of SearchService
 * and added functionality.
 */
/*
 * The following fields are not fully supported,
 * and are expected to be registered only when necessary.
 * Data entry rules may change
 * from version to version to reduce data usage charges.
 * 
 * _FULL
 * May be used to avoid Japanese-specific token analysis ambiguity.
 * 
 * _READING Basically
 * "reading". However, it may be tag input value instead of server analysis.
 *  It is not necessarily a general "reading".
 *  
 * _READING_HIRAGANA
 * This is a field that is expected to be "reading" in Hiragana.
 * Since Japanese-language analysis on servers mainly deals only with the first candidate,
 * there is a limit in pursuing accuracy by itself.
 * However, carrying a huge dictionary to pursue accuracy
 * and coding academically cleverly is wasteful.
 * (small is justice in this server)
 * Short phrases consisting of only hiragana / Katakana are high in analysis difficulty,
 * so they may be complemented in this field
 * without relying on analysis results.
 */
public enum IndexType {
	
	SONG(new String[] {
			FieldNames.TITLE,
            FieldNames.TITLE_READING_HIRAGANA,
			FieldNames.ARTIST,
			FieldNames.ARTIST_FULL,
			FieldNames.ARTIST_READING,
			FieldNames.ARTIST_READING_HIRAGANA }) {

		public Document createDocument(MediaFile mediaFile) {
			Document doc = new Document();
			fieldId.accept(doc, mediaFile.getId());
			fieldArtist.accept(doc, mediaFile.getArtist());
			fieldArtistF.accept(doc, mediaFile.getArtist());
			String reading = isEmpty(mediaFile.getArtistSort())
					? mediaFile.getArtistReading()
					: mediaFile.getArtistSort();
			fieldArtistR.accept(doc, reading);
			fieldArtistRH.accept(doc, reading);
			fieldMediaType.accept(doc, mediaFile.getMediaType().name());
			fieldTitle.accept(doc, mediaFile.getTitle());
			fieldTitleRH.accept(doc, mediaFile.getTitle());
			fieldGenre.accept(doc, mediaFile.getGenre());
			fieldYear.accept(doc, mediaFile.getYear());
			fieldFolder.accept(doc, mediaFile.getFolder());
			return doc;
		}

	},

	ALBUM(new String[] {
			FieldNames.ALBUM,
			FieldNames.ALBUM_FULL,
            FieldNames.ALBUM_READING_HIRAGANA,
			FieldNames.ARTIST,
			FieldNames.ARTIST_FULL,
			FieldNames.ARTIST_READING,
			FieldNames.ARTIST_READING_HIRAGANA,
			FieldNames.FOLDER }) {

		@Override
		public Document createDocument(MediaFile mediaFile) {
			Document doc = new Document();
			fieldId.accept(doc, mediaFile.getId());
			fieldAlbum.accept(doc, mediaFile.getAlbumName());
            fieldAlbumFull.accept(doc, mediaFile.getAlbumName());
            fieldAlbumRH.accept(doc, mediaFile.getAlbumName());
			fieldArtist.accept(doc, mediaFile.getArtist());
			fieldArtistF.accept(doc, mediaFile.getArtist());
			fieldArtistR.accept(doc, isEmpty(mediaFile.getArtistSort())
					? mediaFile.getArtistReading()
					: mediaFile.getArtistSort());
			fieldArtistRH.accept(doc, isEmpty(mediaFile.getArtistSort())
					? mediaFile.getArtistReading()
					: mediaFile.getArtistSort());
			fieldFolder.accept(doc, mediaFile.getFolder());
			return doc;
		}

	},

	ALBUM_ID3(
			new String[] {
					FieldNames.ALBUM,
					FieldNames.ALBUM_FULL,
					FieldNames.ALBUM_READING_HIRAGANA,
					FieldNames.ARTIST,
					FieldNames.ARTIST_FULL,
					FieldNames.ARTIST_READING,
					FieldNames.ARTIST_READING_HIRAGANA,
					FieldNames.FOLDER_ID }) {

		@Override
		public Document createDocument(Album album) {
			Document doc = new Document();
			fieldId.accept(doc, album.getId());
			fieldAlbum.accept(doc, album.getName());
			fieldAlbumFull.accept(doc, album.getName());
			fieldAlbumRH.accept(doc, album.getName());
			fieldArtist.accept(doc, album.getArtist());
			fieldArtistF.accept(doc, album.getArtist());
			fieldArtistR.accept(doc, album.getArtistSort());
			fieldArtistRH.accept(doc, album.getArtistSort());
			fieldFolderId.accept(doc, album.getFolderId());
			return doc;
		}

	},

	ARTIST(new String[] {
			FieldNames.ARTIST,
			FieldNames.ARTIST_FULL,
			FieldNames.ARTIST_READING,
			FieldNames.ARTIST_READING_HIRAGANA,
			FieldNames.FOLDER }) {

		@Override
		public Document createDocument(MediaFile mediaFile) {
			Document doc = new Document();
			fieldId.accept(doc, mediaFile.getId());
			fieldArtist.accept(doc, mediaFile.getArtist());
			fieldArtistF.accept(doc, mediaFile.getArtist());
			String reading = isEmpty(mediaFile.getArtistSort())
					? mediaFile.getArtistReading()
					: mediaFile.getArtistSort();
			fieldArtistR.accept(doc, reading);
			fieldArtistRH.accept(doc, reading);
			fieldFolder.accept(doc, mediaFile.getFolder());
			return doc;
		}

	},

	ARTIST_ID3(
			new String[] {
					FieldNames.ARTIST,
					FieldNames.ARTIST_FULL,
					FieldNames.ARTIST_READING,
					FieldNames.ARTIST_READING_HIRAGANA }) {

		@Override
		public Document createDocument(Artist artist, MusicFolder musicFolder) {
			Document doc = new Document();
			fieldId.accept(doc, artist.getId());
			fieldArtist.accept(doc, artist.getName());
			fieldArtistF.accept(doc, artist.getName());
			String reading = isEmpty(artist.getSort())
					? artist.getReading() :
					artist.getSort();
			fieldArtistR.accept(doc, reading);
			fieldArtistRH.accept(doc, reading);
			fieldFolderId.accept(doc, musicFolder.getId());
			return doc;
		}

	};

	public static final class FieldNames {
	    /*
	     * The contents of analysis are different for each field.
	     * Defined in Analyzer.
	     * 
	     * Normal analysis              - Normal tokenizing and filtering
         * Other than Normal analysis   - No tokenize, special filtering
         * 
         * Asterisk is unconditional registration.
	     */

        /* Emphasis on complementation as artists are less data and more important */
        public static final String ARTIST =                  "art";   // * Normal analysis 
        public static final String ARTIST_FULL =             "artF";  // Registration when other than hiragana (possibility of various character types)
        public static final String ARTIST_READING =          "artR";  // * Sort key (possibility of various character types)
        public static final String ARTIST_READING_HIRAGANA = "artRH"; // Convert to Hiragana and register

        public static final String ALBUM =                   "alb";   // * Normal analysis
		public static final String ALBUM_FULL =              "albF";  // Registration when other than hiragana
        public static final String ALBUM_READING_HIRAGANA =  "albRH"; // Register when hiragana only

        public static final String TITLE =                   "tit";   // * Normal analysis
        //public static final String TITLE_FULL =            "titF";  // Do not register (consider the amount of data)
        public static final String TITLE_READING_HIRAGANA =  "titRH"; // Register when hiragana only

        public static final String ID =                      "id";
        public static final String GENRE =                   "g";
        public static final String YEAR =                    "y";
        public static final String MEDIA_TYPE =              "m";
        public static final String FOLDER =                  "f";
        public static final String FOLDER_ID =               "fId";
	}

	private static final Pattern HIRAGANA = Pattern.compile("^[\\u3040-\\u309F\\s+]+$");

	private static final Pattern KATAKANA = Pattern.compile("^[\\u30A0-\\u30FF\\s+]+$");

	public final static boolean isHiraOrKata(String arg) {
		return HIRAGANA.matcher(arg).matches() || KATAKANA.matcher(arg).matches();
	}

	private static final BiConsumer<Document, Integer> fieldId = (d, i) -> {
        d.add(new IntPoint(FieldNames.ID, i));
        d.add(new StringField(FieldNames.ID, Integer.toString(i), Store.YES));
        d.add(new StoredField(FieldNames.ID, i));
	};

    private static final BiConsumer<Document, String> fieldTitle = (d, s) -> {
		if (isEmpty(s)) {
			return;
		}
		d.add(new TextField(FieldNames.TITLE, s, Store.YES));
		d.add(new SortedDocValuesField(FieldNames.TITLE, new BytesRef(s)));
	};

    private static final BiConsumer<Document, String> fieldTitleRH = (d, s) -> {
        if (isEmpty(s) || !HIRAGANA.matcher(s).matches()) {
            return;
        }
        d.add(new StringField(FieldNames.TITLE_READING_HIRAGANA, s, Store.YES));
        d.add(new SortedDocValuesField(FieldNames.TITLE_READING_HIRAGANA, new BytesRef(s)));
    };

	private static final BiConsumer<Document, String> fieldArtist = (d, s) -> {
		if (isEmpty(s)) {
			return;
		}
		d.add(new TextField(FieldNames.ARTIST, s, Store.YES));
		d.add(new SortedDocValuesField(FieldNames.ARTIST, new BytesRef(s)));
	};

	private static final BiConsumer<Document, String> fieldArtistF = (d, s) -> {
		if (isEmpty(s) || HIRAGANA.matcher(s).matches()) {
			return;
		}
		d.add(new StringField(FieldNames.ARTIST_FULL, s.toLowerCase(), Store.YES));
		d.add(new SortedDocValuesField(FieldNames.ARTIST_FULL, new BytesRef(s)));
	};

	private static final BiConsumer<Document, String> fieldArtistR = (d, s) -> {
		if (isEmpty(s)) {
			return;
		}
		d.add(new StringField(FieldNames.ARTIST_READING, s, Store.YES));
		d.add(new SortedDocValuesField(FieldNames.ARTIST_READING, new BytesRef(s)));
	};

	private static final BiConsumer<Document, String> fieldArtistRH = (d, s) -> {
		if (isEmpty(s)) {
			return;
		}
		String hiragana = HIRAGANA.matcher(s).matches()
				? s
				: Transliterator.getInstance("Katakana-Hiragana").transliterate(s);
		d.add(new StringField(FieldNames.ARTIST_READING_HIRAGANA, hiragana, Store.YES));
		d.add(new SortedDocValuesField(FieldNames.ARTIST_READING_HIRAGANA, new BytesRef(hiragana)));
	};

	private static final BiConsumer<Document, String> fieldFolder = (d, s) -> {
		d.add(new TextField(FieldNames.FOLDER, s, Store.YES));
		d.add(new SortedDocValuesField(FieldNames.FOLDER, new BytesRef(s)));
	};

	private static final BiConsumer<Document, String> fieldAlbum = (d, s) -> {
		if (isEmpty(s)) {
			return;
		}
		d.add(new TextField(FieldNames.ALBUM, s, Store.YES));
		d.add(new SortedDocValuesField(FieldNames.ALBUM, new BytesRef(s)));
	};

	private static final BiConsumer<Document, String> fieldAlbumFull = (d, s) -> {
		if (isEmpty(s) || HIRAGANA.matcher(s).matches()) {
			return;
		}
		d.add(new StringField(FieldNames.ALBUM_FULL, s, Store.YES));
		d.add(new SortedDocValuesField(FieldNames.ALBUM_FULL, new BytesRef(s)));
	};
	
	private static final BiConsumer<Document, String> fieldAlbumRH = (d, s) -> {
        if (isEmpty(s) || !HIRAGANA.matcher(s).matches()) {
            return;
        }
        d.add(new StringField(FieldNames.ALBUM_READING_HIRAGANA, s, Store.YES));
        d.add(new SortedDocValuesField(FieldNames.ALBUM_READING_HIRAGANA, new BytesRef(s)));
    };

	private static final BiConsumer<Document, Integer> fieldFolderId = (d, i) -> {
	  d.add(new IntPoint(FieldNames.FOLDER_ID, i));
    d.add(new StringField(FieldNames.FOLDER_ID, Integer.toString(i), Store.YES));
    d.add(new StoredField(FieldNames.FOLDER_ID, i));
	};

	private static final BiConsumer<Document, Integer> fieldYear = (d, i) -> {
		if (isEmpty(i)) {
			return;
		}
    d.add(new IntPoint(FieldNames.YEAR, i));
    d.add(new StringField(FieldNames.YEAR, Integer.toString(i), Store.YES));
    d.add(new StoredField(FieldNames.YEAR, i));
	};

	private static final BiConsumer<Document, String> fieldMediaType = (d, s) -> {
		if (isEmpty(s)) {
			return;
		}
		d.add(new StringField(FieldNames.MEDIA_TYPE, s.toLowerCase(), Store.YES));
		d.add(new SortedDocValuesField(FieldNames.MEDIA_TYPE, new BytesRef(s.toLowerCase())));
	};

	private static final BiConsumer<Document, String> fieldGenre = (d, s) -> {
		if (isEmpty(s)) {
			return;
		}
		d.add(new TextField(FieldNames.GENRE, s, Store.YES));
		d.add(new SortedDocValuesField(FieldNames.GENRE, new BytesRef(s)));
	};

	private final String[] fields;

	private IndexType(String[] fieldNames) {
		this.fields = fieldNames;
	}

	public Document createDocument(Album album) {
		throw new UnsupportedOperationException();
	}

	public Document createDocument(Artist artist, MusicFolder musicFolder) {
		throw new UnsupportedOperationException();
	}

	public Document createDocument(MediaFile mediaFile) {
		throw new UnsupportedOperationException();
	}

	public String[] getFields() {
		return fields;
	}

}
