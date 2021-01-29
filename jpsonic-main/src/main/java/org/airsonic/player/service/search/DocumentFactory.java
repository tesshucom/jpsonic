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

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SettingsService;
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

    private BiConsumer<@NonNull Document, @NonNull Integer> fieldId = (doc, value) -> {
        doc.add(new StoredField(FieldNamesConstants.ID, Integer.toString(value), TYPE_ID));
    };

    private BiConsumer<@NonNull Document, @NonNull Integer> fieldFolderId = (doc, value) -> {
        doc.add(new StoredField(FieldNamesConstants.FOLDER_ID, Integer.toString(value), TYPE_ID_NO_STORE));
    };

    private Consumer<@NonNull Document, @NonNull String, @NonNull String> fieldKey = (doc, field, value) -> {
        doc.add(new StoredField(field, value, TYPE_KEY));
    };

    private BiConsumer<@NonNull Document, @NonNull String> fieldMediatype = (doc, value) -> fieldKey.accept(doc,
            FieldNamesConstants.MEDIA_TYPE, value);

    private BiConsumer<@NonNull Document, @NonNull String> fieldFolderPath = (doc, value) -> fieldKey.accept(doc,
            FieldNamesConstants.FOLDER, value);

    public BiFunction<@NonNull String, @Nullable String, List<Field>> createWordsFields = (fieldName, value) -> Arrays
            .asList(new TextField(fieldName, value, Store.NO),
                    new SortedDocValuesField(fieldName, new BytesRef(value)));

    private Consumer<@NonNull Document, @NonNull String, @Nullable String> fieldWords = (doc, fieldName, value) -> {
        if (isEmpty(value)) {
            return;
        }
        createWordsFields.apply(fieldName, value).forEach(f -> doc.add(f));
    };

    private BiConsumer<@NonNull Document, @Nullable String> fieldGenre = (doc, value) -> {
        if (isEmpty(value)) {
            return;
        }
        fieldWords.accept(doc, FieldNamesConstants.GENRE, value);
    };

    private Consumer<Document, String, String> fieldGenreKey = (doc, fieldName, value) -> {
        doc.add(new TextField(fieldName, value, Store.YES));
    };

    private Consumer<@NonNull Document, @NonNull String, @Nullable Integer> fieldYear = (doc, fieldName, value) -> {
        if (isEmpty(value)) {
            return;
        }
        doc.add(new IntPoint(fieldName, value));
    };

    public DocumentFactory(SettingsService settingsService, JapaneseReadingUtils readingUtils) {
        this.settingsService = settingsService;
        this.readingUtils = readingUtils;
    }

    public final Term createPrimarykey(Integer id) {
        return new Term(FieldNamesConstants.ID, Integer.toString(id));
    }

    public final Term createPrimarykey(Album album) {
        return createPrimarykey(album.getId());
    }

    public final Term createPrimarykey(Artist artist) {
        return createPrimarykey(artist.getId());
    }

    public final Term createPrimarykey(MediaFile mediaFile) {
        return createPrimarykey(mediaFile.getId());
    }

    @FunctionalInterface
    private interface Consumer<T, U, V> {
        void accept(T t, U u, V v);
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
        fieldId.accept(doc, mediaFile.getId());
        fieldWords.accept(doc, FieldNamesConstants.ARTIST, mediaFile.getArtist());
        acceptArtistReading(doc, mediaFile);
        fieldGenre.accept(doc, mediaFile.getGenre());
        fieldWords.accept(doc, FieldNamesConstants.ALBUM, mediaFile.getAlbumName());
        fieldWords.accept(doc, FieldNamesConstants.ALBUM_EX, mediaFile.getAlbumName());
        fieldFolderPath.accept(doc, mediaFile.getFolder());
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
        fieldId.accept(doc, mediaFile.getId());
        fieldWords.accept(doc, FieldNamesConstants.ARTIST, mediaFile.getArtist());
        acceptArtistReading(doc, mediaFile);
        fieldFolderPath.accept(doc, mediaFile.getFolder());
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
        fieldId.accept(doc, album.getId());
        fieldWords.accept(doc, FieldNamesConstants.ARTIST, album.getArtist());
        acceptArtistReading(doc, album);
        fieldGenre.accept(doc, album.getGenre());
        fieldWords.accept(doc, FieldNamesConstants.ALBUM, album.getName());
        fieldWords.accept(doc, FieldNamesConstants.ALBUM_EX, album.getName());
        fieldFolderId.accept(doc, album.getFolderId());
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
    /*
     * XXX 3.x -> 8.x : Only null check specification of createArtistId3Document is different from legacy. (The reason
     * is only to simplify the function.)
     *
     * Since the field of domain object Album is nonnull, null check was not performed.
     *
     * In implementation ARTIST and ALBUM became nullable, but null is not input at this point in data flow.
     */
    public Document createArtistId3Document(Artist artist, MusicFolder musicFolder) {
        Document doc = new Document();
        fieldId.accept(doc, artist.getId());
        fieldWords.accept(doc, FieldNamesConstants.ARTIST, artist.getName());
        acceptArtistReading(doc, artist);
        fieldFolderId.accept(doc, musicFolder.getId());
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
        fieldId.accept(doc, mediaFile.getId());
        fieldMediatype.accept(doc, mediaFile.getMediaType().name());
        fieldWords.accept(doc, FieldNamesConstants.TITLE, mediaFile.getTitle());
        fieldWords.accept(doc, FieldNamesConstants.TITLE_EX, mediaFile.getTitle());
        fieldWords.accept(doc, FieldNamesConstants.ARTIST, mediaFile.getArtist());
        acceptArtistReading(doc, mediaFile);
        fieldWords.accept(doc, FieldNamesConstants.COMPOSER, mediaFile.getComposer());
        acceptComposerReading(doc, mediaFile);
        fieldGenre.accept(doc, mediaFile.getGenre());
        fieldYear.accept(doc, FieldNamesConstants.YEAR, mediaFile.getYear());
        fieldFolderPath.accept(doc, mediaFile.getFolder());
        return doc;
    }

    public Document createGenreDocument(MediaFile mediaFile) {
        Document doc = new Document();
        fieldGenreKey.accept(doc, FieldNamesConstants.GENRE_KEY, mediaFile.getGenre());
        fieldGenre.accept(doc, mediaFile.getGenre());
        return doc;
    }

    private void acceptArtistReading(Document doc, String artist, String sort, String reading) {
        String result = defaultIfEmpty(sort, reading);
        if (!isEmpty(artist) && !artist.equals(result)) {
            fieldWords.accept(doc, FieldNamesConstants.ARTIST_READING, settingsService.isSearchMethodLegacy() ? result
                    : readingUtils.removePunctuationFromJapaneseReading(result));
        }
        fieldWords.accept(doc, FieldNamesConstants.ARTIST_EX, artist);
    }

    private void acceptArtistReading(Document doc, MediaFile mediaFile) {
        acceptArtistReading(doc, mediaFile.getArtist(), mediaFile.getArtistSort(), mediaFile.getArtistReading());
    }

    private void acceptArtistReading(Document doc, Artist artist) {
        acceptArtistReading(doc, artist.getName(), artist.getSort(), artist.getReading());
    }

    private void acceptArtistReading(Document doc, Album album) {
        acceptArtistReading(doc, album.getArtist(), album.getArtistSort(), album.getArtistReading());
    }

    private void acceptComposerReading(Document doc, MediaFile mediaFile) {
        if (settingsService.isSearchMethodLegacy()) {
            fieldWords.accept(doc, FieldNamesConstants.COMPOSER_READING, mediaFile.getComposerSort());
        } else {
            if (!isEmpty(mediaFile.getComposer()) && !mediaFile.getComposer().equals(mediaFile.getComposerSort())) {
                fieldWords.accept(doc, FieldNamesConstants.COMPOSER_READING, mediaFile.getComposerSort());
            }
        }
    }

}
