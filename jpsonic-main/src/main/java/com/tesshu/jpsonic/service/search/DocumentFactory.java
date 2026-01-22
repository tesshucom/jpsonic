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
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.tesshu.jpsonic.domain.system.IndexScheme;
import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.Artist;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile.MediaType;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.language.JapaneseReadingUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * A factory that generates Lucene {@link Document} objects to be stored in the
 * search index.
 * <p>
 * This class converts domain entities such as {@link MediaFile}, {@link Album},
 * and {@link Artist} into indexed documents, applying appropriate fields and
 * handling language-specific processing (e.g., Japanese readings).
 * </p>
 */
@Component
@DependsOn({ "settingsService", "japaneseReadingUtils" })
@SuppressWarnings("PMD.TooManyStaticImports")
public class DocumentFactory {

    private static final FieldType TYPE_ID = FieldTypes.ID.type;
    private static final FieldType TYPE_ID_NO_STORE = FieldTypes.ID_NO_STORE.type;
    private static final FieldType TYPE_KEY = FieldTypes.KEY.type;

    private final SettingsService settingsService;
    private final JapaneseReadingUtils readingUtils;

    public DocumentFactory(SettingsService settingsService, JapaneseReadingUtils readingUtils) {
        this.settingsService = settingsService;
        this.readingUtils = readingUtils;
    }

    // ========= ENUM for FieldTypes =========

    private enum FieldTypes {
        ID(true), ID_NO_STORE(false), KEY(false);

        private final transient FieldType type;

        FieldTypes(boolean stored) {
            FieldType ft = new FieldType();
            ft.setIndexOptions(IndexOptions.DOCS);
            ft.setTokenized(false);
            ft.setOmitNorms(true);
            ft.setStored(stored);
            ft.freeze();
            this.type = ft;
        }
    }

    // ========= FIELD HELPERS =========

    private void applyStoredField(@NonNull Document doc, @NonNull String field,
            @NonNull String value, @NonNull FieldType type) {
        doc.add(new Field(field, value, type));
    }

    private void applyFieldId(@NonNull Document doc, @NonNull Integer value) {
        applyStoredField(doc, FieldNamesConstants.ID, value.toString(), TYPE_ID);
    }

    private void applyFieldFolderId(@NonNull Document doc, @NonNull Integer value) {
        applyStoredField(doc, FieldNamesConstants.FOLDER_ID, value.toString(), TYPE_ID_NO_STORE);
    }

    private void applyFieldKey(@NonNull Document doc, @NonNull String field,
            @NonNull String value) {
        applyStoredField(doc, field, value, TYPE_KEY);
    }

    private void applyTextAndSortedField(@NonNull Document doc, @NonNull String fieldName,
            @Nullable String value) {
        if (isEmpty(value)) {
            return;
        }
        doc.add(new TextField(fieldName, value, Store.NO));
        doc.add(new SortedDocValuesField(fieldName, new BytesRef(value)));
    }

    private void applyGenre(@NonNull Document doc, @Nullable String value) {
        applyTextAndSortedField(doc, GENRE, value);
    }

    private void applyFieldYear(@NonNull Document doc, @NonNull String fieldName,
            @Nullable Integer value) {
        if (value != null) {
            doc.add(new IntPoint(fieldName, value));
        }
    }

    private void applyFieldFolderPath(@NonNull Document doc, @NonNull String value) {
        applyFieldKey(doc, FieldNamesConstants.FOLDER, value);
    }

    private void applyFieldMediatype(@NonNull Document doc, @NonNull String value) {
        applyFieldKey(doc, FieldNamesConstants.MEDIA_TYPE, value);
    }

    private void applyFieldGenreKey(Document doc, String fieldName, String value) {
        doc.add(new TextField(fieldName, value, Store.YES));
    }

    private boolean containsJapanese(String text) {
        return text.codePoints().anyMatch(cp -> {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
            return block == Character.UnicodeBlock.HIRAGANA
                    || block == Character.UnicodeBlock.KATAKANA
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
        });
    }

    private void acceptReading(Document doc, String field, String romanizedField, String value,
            String sort, String reading) {
        if (isEmpty(value)) {
            return;
        }

        String result = defaultIfEmpty(sort, reading);
        if (value.equals(result)) {
            return;
        }

        IndexScheme scheme = IndexScheme.of(settingsService.getIndexSchemeName());
        boolean isJapanese = containsJapanese(value);
        boolean useInternal = settingsService.isForceInternalValueInsteadOfTags();

        switch (scheme) {
        case WITHOUT_JP_LANG_PROCESSING -> applyTextAndSortedField(doc, field, result);
        case ROMANIZED_JAPANESE -> {
            applyTextAndSortedField(doc, field, result);
            if (isJapanese) {
                String romanized = readingUtils
                    .removePunctuationFromJapaneseReading(useInternal ? reading : result);
                applyTextAndSortedField(doc, romanizedField, romanized);
            }
        }
        default -> {
            String normalized = readingUtils.removePunctuationFromJapaneseReading(result);
            applyTextAndSortedField(doc, field, normalized);
        }
        }
    }

    void applyArtistInfo(Document doc, String artist, String sort, String reading) {
        applyTextAndSortedField(doc, ARTIST, artist);
        acceptReading(doc, ARTIST_READING, ARTIST_READING_ROMANIZED, artist, sort, reading);
    }

    void applyComposerInfo(Document doc, String composer, String sortRaw, String sort) {
        applyTextAndSortedField(doc, COMPOSER, composer);
        acceptReading(doc, COMPOSER_READING, COMPOSER_READING_ROMANIZED, composer, sortRaw, sort);
    }

    // ========= DOCUMENT FACTORY METHODS =========

    public Document createAlbumDocument(MediaFile mediaFile) {
        Document doc = new Document();
        applyFieldId(doc, mediaFile.getId());
        applyArtistInfo(doc, mediaFile.getArtist(), mediaFile.getArtistSort(),
                mediaFile.getArtistReading());
        applyGenre(doc, mediaFile.getGenre());
        applyTextAndSortedField(doc, ALBUM, mediaFile.getAlbumName());
        applyTextAndSortedField(doc, ALBUM_READING,
                defaultIfEmpty(mediaFile.getAlbumSort(), mediaFile.getAlbumReading()));
        applyFieldFolderPath(doc, mediaFile.getFolder());
        return doc;
    }

    public Document createArtistDocument(MediaFile mediaFile) {
        Document doc = new Document();
        applyFieldId(doc, mediaFile.getId());
        applyArtistInfo(doc, mediaFile.getArtist(), mediaFile.getArtistSort(),
                mediaFile.getArtistReading());
        applyFieldFolderPath(doc, mediaFile.getFolder());
        return doc;
    }

    public Document createAlbumId3Document(Album album) {
        Document doc = new Document();
        applyFieldId(doc, album.getId());
        applyArtistInfo(doc, album.getArtist(), album.getArtistSort(), album.getArtistReading());
        applyGenre(doc, album.getGenre());
        applyTextAndSortedField(doc, ALBUM, album.getName());
        applyTextAndSortedField(doc, ALBUM_READING,
                defaultIfEmpty(album.getNameSort(), album.getNameReading()));
        applyFieldFolderId(doc, album.getFolderId());
        return doc;
    }

    public Document createArtistId3Document(Artist artist, MusicFolder folder) {
        Document doc = new Document();
        applyFieldId(doc, artist.getId());
        applyArtistInfo(doc, artist.getName(), artist.getSort(), artist.getReading());
        applyFieldFolderId(doc, folder.getId());
        return doc;
    }

    public Document createSongDocument(MediaFile mediaFile) {
        Document doc = new Document();
        applyFieldId(doc, mediaFile.getId());
        applyFieldMediatype(doc, mediaFile.getMediaType().name());
        applyTextAndSortedField(doc, TITLE, mediaFile.getTitle());
        // Due to performance concerns, the titleSort field is currently not being used.
        applyTextAndSortedField(doc, TITLE_READING, mediaFile.getTitle());
        applyArtistInfo(doc, mediaFile.getArtist(), mediaFile.getArtistSort(),
                mediaFile.getArtistReading());
        applyComposerInfo(doc, mediaFile.getComposer(), mediaFile.getComposerSortRaw(),
                mediaFile.getComposerSort());
        if (mediaFile.getMediaType() != MediaType.PODCAST) {
            applyGenre(doc, mediaFile.getGenre());
        }
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

    public Document createGenreDocument(Album album) {
        Document doc = new Document();
        applyFieldGenreKey(doc, GENRE_KEY, album.getGenre());
        applyGenre(doc, album.getGenre());
        return doc;
    }

    // ========= PRIMARY KEY CREATION =========

    public static Term createPrimarykey(Integer id) {
        return new Term(FieldNamesConstants.ID, Integer.toString(id));
    }

    public static Term createPrimarykey(String id) {
        return new Term(FieldNamesConstants.ID, id);
    }

    public static Term createPrimarykey(Album album) {
        return createPrimarykey(album.getId());
    }

    public static Term createPrimarykey(Artist artist) {
        return createPrimarykey(artist.getId());
    }

    public static Term createPrimarykey(MediaFile mediaFile) {
        return createPrimarykey(mediaFile.getId());
    }
}
