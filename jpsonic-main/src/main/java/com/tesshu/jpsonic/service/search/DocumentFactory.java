package com.tesshu.jpsonic.service.search;

import com.ibm.icu.text.Transliterator;
import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

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
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static org.springframework.util.ObjectUtils.isEmpty;

@Component
public class DocumentFactory {

    private static final Pattern HIRAGANA = Pattern.compile("^[\\u3040-\\u309F\\s+]+$");

    private static final Pattern KATAKANA = Pattern.compile("^[\\u30A0-\\u30FF\\s+]+$");

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
        String hiragana = HIRAGANA.matcher(s).matches() ? s : Transliterator.getInstance("Katakana-Hiragana").transliterate(s);
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

    public final static boolean isHiraOrKata(String arg) {
        return HIRAGANA.matcher(arg).matches() || KATAKANA.matcher(arg).matches();
    }

    private Document createAlbumDocument(MediaFile mediaFile) {
        Document doc = new Document();
        fieldId.accept(doc, mediaFile.getId());
        fieldAlbum.accept(doc, mediaFile.getAlbumName());
        fieldAlbumFull.accept(doc, mediaFile.getAlbumName());
        fieldAlbumRH.accept(doc, mediaFile.getAlbumName());
        fieldArtist.accept(doc, mediaFile.getArtist());
        fieldArtistF.accept(doc, mediaFile.getArtist());
        fieldArtistR.accept(doc, isEmpty(mediaFile.getArtistSort()) ? mediaFile.getArtistReading() : mediaFile.getArtistSort());
        fieldArtistRH.accept(doc, isEmpty(mediaFile.getArtistSort()) ? mediaFile.getArtistReading() : mediaFile.getArtistSort());
        fieldFolder.accept(doc, mediaFile.getFolder());
        return doc;
    }

    private Document createArtistDocument(MediaFile mediaFile) {
        Document doc = new Document();
        fieldId.accept(doc, mediaFile.getId());
        fieldArtist.accept(doc, mediaFile.getArtist());
        fieldArtistF.accept(doc, mediaFile.getArtist());
        String reading = isEmpty(mediaFile.getArtistSort()) ? mediaFile.getArtistReading() : mediaFile.getArtistSort();
        fieldArtistR.accept(doc, reading);
        fieldArtistRH.accept(doc, reading);
        fieldFolder.accept(doc, mediaFile.getFolder());
        return doc;
    }

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

    public Document createDocument(Artist artist, MusicFolder musicFolder) {
        Document doc = new Document();
        fieldId.accept(doc, artist.getId());
        fieldArtist.accept(doc, artist.getName());
        fieldArtistF.accept(doc, artist.getName());
        String reading = isEmpty(artist.getSort()) ? artist.getReading() : artist.getSort();
        fieldArtistR.accept(doc, reading);
        fieldArtistRH.accept(doc, reading);
        fieldFolderId.accept(doc, musicFolder.getId());
        return doc;
    }

    public Document createDocument(IndexType indexType, MediaFile mediaFile) {
        if (IndexType.ALBUM == indexType) {
            return createAlbumDocument(mediaFile);
        } else if (IndexType.ARTIST == indexType) {
            return createArtistDocument(mediaFile);
        } else if (IndexType.SONG == indexType) {
            return createSongDocument(mediaFile);
        }
        throw new UnsupportedOperationException();
    }

    private Document createSongDocument(MediaFile mediaFile) {
        Document doc = new Document();
        fieldId.accept(doc, mediaFile.getId());
        fieldArtist.accept(doc, mediaFile.getArtist());
        fieldArtistF.accept(doc, mediaFile.getArtist());
        String reading = isEmpty(mediaFile.getArtistSort()) ? mediaFile.getArtistReading() : mediaFile.getArtistSort();
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

}
