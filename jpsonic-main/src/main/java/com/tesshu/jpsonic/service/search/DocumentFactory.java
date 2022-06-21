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

import static com.tesshu.jpsonic.service.search.FieldNamesConstants.ALBUM;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.ALBUM_READING;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.ARTIST;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.ARTIST_READING;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.ARTIST_READING_ROMANIZED;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.COMPOSER;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.COMPOSER_READING;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.COMPOSER_READING_ROMANIZED;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.GENRE;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.GENRE_KEY;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.TITLE;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.TITLE_READING;
import static com.tesshu.jpsonic.service.search.FieldNamesConstants.YEAR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * A factory that generates the documents to be stored in the index.
 */
@Component
@DependsOn({ "settingsService", "japaneseReadingUtils" })
@SuppressWarnings("PMD.TooManyStaticImports")
public class DocumentFactory {

    private static final FieldType TYPE_ID;

    private static final FieldType TYPE_ID_NO_STORE;

    private static final FieldType TYPE_KEY;

    private final SettingsService settingsService;
    private final JapaneseReadingUtils readingUtils;

    static {

        TYPE_ID = new FieldType();
        TYPE_ID.setIndexOptions(IndexOptions.DOCS);
        TYPE_ID.setTokenized(false);
        TYPE_ID.setOmitNorms(true);
        TYPE_ID.setStored(true);
        TYPE_ID.freeze();

        TYPE_ID_NO_STORE = new FieldType();
        TYPE_ID_NO_STORE.setIndexOptions(IndexOptions.DOCS);
        TYPE_ID_NO_STORE.setTokenized(false);
        TYPE_ID_NO_STORE.setOmitNorms(true);
        TYPE_ID_NO_STORE.setStored(false);
        TYPE_ID_NO_STORE.freeze();

        TYPE_KEY = new FieldType();
        TYPE_KEY.setIndexOptions(IndexOptions.DOCS);
        TYPE_KEY.setTokenized(false);
        TYPE_KEY.setOmitNorms(true);
        TYPE_KEY.setStored(false);
        TYPE_KEY.freeze();

    }

    private void applyFieldId(@NonNull Document doc, @NonNull Integer value) {
        doc.add(new StoredField(FieldNamesConstants.ID, Integer.toString(value), TYPE_ID));
    }

    private void applyFieldFolderId(@NonNull Document doc, @NonNull Integer value) {
        doc.add(new StoredField(FieldNamesConstants.FOLDER_ID, Integer.toString(value), TYPE_ID_NO_STORE));
    }

    private void applyFieldKey(@NonNull Document doc, @NonNull String field, @NonNull String value) {
        doc.add(new StoredField(field, value, TYPE_KEY));
    }

    private void applyFieldMediatype(@NonNull Document doc, @NonNull String value) {
        applyFieldKey(doc, FieldNamesConstants.MEDIA_TYPE, value);
    }

    private void applyFieldFolderPath(@NonNull Document doc, @NonNull String value) {
        applyFieldKey(doc, FieldNamesConstants.FOLDER, value);
    }

    private List<Field> createFields(@NonNull String fieldName, @Nullable String value) {
        return Arrays.asList(new TextField(fieldName, value, Store.NO),
                new SortedDocValuesField(fieldName, new BytesRef(value)));
    }

    private void applyFieldValue(@NonNull Document doc, @NonNull String fieldName, @Nullable String value) {
        if (isEmpty(value)) {
            return;
        }
        createFields(fieldName, value).forEach(doc::add);
    }

    private void applyGenre(@NonNull Document doc, @Nullable String value) {
        if (isEmpty(value)) {
            return;
        }
        applyFieldValue(doc, GENRE, value);
    }

    private void applyFieldGenreKey(Document doc, String fieldName, String value) {
        doc.add(new TextField(fieldName, value, Store.YES));
    }

    private void applyFieldYear(@NonNull Document doc, @NonNull String fieldName, @Nullable Integer value) {
        if (isEmpty(value)) {
            return;
        }
        doc.add(new IntPoint(fieldName, value));
    }

    public static final Term createPrimarykey(Integer id) {
        return new Term(FieldNamesConstants.ID, Integer.toString(id));
    }

    public static final Term createPrimarykey(Album album) {
        return createPrimarykey(album.getId());
    }

    public static final Term createPrimarykey(Artist artist) {
        return createPrimarykey(artist.getId());
    }

    public static final Term createPrimarykey(MediaFile mediaFile) {
        return createPrimarykey(mediaFile.getId());
    }

    public DocumentFactory(SettingsService settingsService, JapaneseReadingUtils readingUtils) {
        this.settingsService = settingsService;
        this.readingUtils = readingUtils;
    }

    /**
     * Create a document.
     *
     * @param mediaFile
     *            target of document
     *
     * @return document
     *
     * @since legacy
     */
    public Document createAlbumDocument(MediaFile mediaFile) {
        Document doc = new Document();
        applyFieldId(doc, mediaFile.getId());
        applyFieldValue(doc, ARTIST, mediaFile.getArtist());
        acceptArtistReading(doc, mediaFile.getArtist(), mediaFile.getArtistSort(), mediaFile.getArtistReading());
        applyGenre(doc, mediaFile.getGenre());
        applyFieldValue(doc, ALBUM, mediaFile.getAlbumName());
        applyFieldValue(doc, ALBUM_READING, defaultIfEmpty(mediaFile.getAlbumSort(), mediaFile.getAlbumReading()));
        applyFieldFolderPath(doc, mediaFile.getFolder());
        return doc;
    }

    /**
     * Create a document.
     *
     * @param mediaFile
     *            target of document
     *
     * @return document
     *
     * @since legacy
     */
    public Document createArtistDocument(MediaFile mediaFile) {
        Document doc = new Document();
        applyFieldId(doc, mediaFile.getId());
        applyFieldValue(doc, ARTIST, mediaFile.getArtist());
        acceptArtistReading(doc, mediaFile.getArtist(), mediaFile.getArtistSort(), mediaFile.getArtistReading());
        applyFieldFolderPath(doc, mediaFile.getFolder());
        return doc;
    }

    /**
     * Create a document.
     *
     * @param album
     *            target of document
     *
     * @return document
     *
     * @since legacy
     */
    public Document createAlbumId3Document(Album album) {
        Document doc = new Document();
        applyFieldId(doc, album.getId());
        applyFieldValue(doc, ARTIST, album.getArtist());
        acceptArtistReading(doc, album.getArtist(), album.getArtistSort(), album.getArtistReading());
        applyGenre(doc, album.getGenre());
        applyFieldValue(doc, ALBUM, album.getName());
        applyFieldValue(doc, ALBUM_READING, defaultIfEmpty(album.getNameSort(), album.getNameReading()));
        applyFieldFolderId(doc, album.getFolderId());
        return doc;
    }

    /**
     * Create a document.
     *
     * @param artist
     *            target of document
     * @param musicFolder
     *            target folder exists
     *
     * @return document
     *
     * @since legacy
     */
    public Document createArtistId3Document(Artist artist, MusicFolder musicFolder) {
        Document doc = new Document();
        applyFieldId(doc, artist.getId());
        applyFieldValue(doc, ARTIST, artist.getName());
        acceptArtistReading(doc, artist.getName(), artist.getSort(), artist.getReading());
        applyFieldFolderId(doc, musicFolder.getId());
        return doc;
    }

    /**
     * Create a document.
     *
     * @param mediaFile
     *            target of document
     *
     * @return document
     *
     * @since legacy
     */
    public Document createSongDocument(MediaFile mediaFile) {
        Document doc = new Document();
        applyFieldId(doc, mediaFile.getId());
        applyFieldMediatype(doc, mediaFile.getMediaType().name());
        applyFieldValue(doc, TITLE, mediaFile.getTitle());
        applyFieldValue(doc, TITLE_READING, mediaFile.getTitle());
        applyFieldValue(doc, ARTIST, mediaFile.getArtist());
        acceptArtistReading(doc, mediaFile.getArtist(), mediaFile.getArtistSort(), mediaFile.getArtistReading());
        applyFieldValue(doc, COMPOSER, mediaFile.getComposer());
        acceptComposerReading(doc, mediaFile.getComposer(), mediaFile.getComposerSortRaw(),
                mediaFile.getComposerSort());
        applyGenre(doc, mediaFile.getGenre());
        applyFieldYear(doc, YEAR, mediaFile.getYear());
        applyFieldFolderPath(doc, mediaFile.getFolder());
        return doc;
    }

    public Document createGenreDocument(MediaFile mediaFile) {
        Document doc = new Document();
        applyFieldGenreKey(doc, GENRE_KEY, mediaFile.getGenre());
        applyGenre(doc, mediaFile.getGenre());
        return doc;
    }

    void acceptReading(Document doc, String field, String romanizedfield, String value, String sort, String reading) {
        if (isEmpty(value)) {
            return;
        }

        String result = defaultIfEmpty(sort, reading);
        if (!value.equals(result)) {

            IndexScheme scheme = IndexScheme.of(settingsService.getIndexSchemeName());
            boolean isJapaneseName = Stream.of(value.split(EMPTY)).anyMatch(s -> {
                Character.UnicodeBlock b = Character.UnicodeBlock.of(s.toCharArray()[0]);
                return Character.UnicodeBlock.HIRAGANA.equals(b) || Character.UnicodeBlock.KATAKANA.equals(b)
                        || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(b);
            });

            if (scheme == IndexScheme.WITHOUT_JP_LANG_PROCESSING) {
                applyFieldValue(doc, field, result);
            } else if (scheme == IndexScheme.ROMANIZED_JAPANESE) {
                applyFieldValue(doc, field, result);
                if (isJapaneseName) {
                    applyFieldValue(doc, romanizedfield, readingUtils.removePunctuationFromJapaneseReading(
                            settingsService.isForceInternalValueInsteadOfTags() ? reading : result));
                }
            } else {
                applyFieldValue(doc, field, readingUtils.removePunctuationFromJapaneseReading(result));
            }
        }
    }

    void acceptArtistReading(Document doc, String value, String sort, String reading) {
        acceptReading(doc, ARTIST_READING, ARTIST_READING_ROMANIZED, value, sort, reading);
    }

    void acceptComposerReading(Document doc, String value, String sort, String reading) {
        acceptReading(doc, COMPOSER_READING, COMPOSER_READING_ROMANIZED, value, sort, reading);
    }
}
