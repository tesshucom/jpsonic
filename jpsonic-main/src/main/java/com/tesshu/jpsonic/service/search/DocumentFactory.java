package com.tesshu.jpsonic.service.search;

import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

import static org.springframework.util.ObjectUtils.isEmpty;

@Component
public class DocumentFactory {

    @FunctionalInterface
    private interface Consumer<T, U, V> {
        void accept(T t, U u, V v);
    };

    /**
     * Returns the version string.
     * @since 1.0
     **/
    public static final String getVersion() {
        return "1.1";
    }

    private Consumer<Document, String, String> termField = (doc, fieldName, value) -> {
        if (isEmpty(value)) {
            return;
        }
        doc.add(new TextField(fieldName, value, Store.YES));
        doc.add(new SortedDocValuesField(fieldName, new BytesRef(value)));
    };

    private BiConsumer<Document, Integer> idField = (doc, value) -> {
        doc.add(new StringField(FieldNames.ID, Integer.toString(value), Store.YES));
    };

    private Consumer<Document, String, String> keyField = (doc, fieldName, value) -> {
        doc.add(new TextField(fieldName, value, Store.YES));
        doc.add(new SortedDocValuesField(fieldName, new BytesRef(value)));
    };

    private Consumer<Document, String, String> nullableKeyField = (doc, fieldName, value) -> {
        if (isEmpty(value)) {
            return;
        }
        doc.add(new TextField(fieldName, value, Store.YES));
        doc.add(new SortedDocValuesField(fieldName, new BytesRef(value)));
    };

    private Consumer<Document, String, Integer> numberField = (doc, fieldName, value) -> {
        if (isEmpty(value)) {
            return;
        }
        doc.add(new IntPoint(fieldName, value));
        doc.add(new StringField(fieldName, Integer.toString(value), Store.NO));//
        doc.add(new SortedDocValuesField(fieldName, new BytesRef(value)));
    };

    private BiConsumer<Document, String> pathField = (doc, value) -> {
        doc.add(new TextField(FieldNames.FOLDER, value, Store.YES));
        doc.add(new SortedDocValuesField(FieldNames.FOLDER, new BytesRef(value)));
    };

    private Document createAlbumDocument(MediaFile mediaFile) {
        Document doc = new Document();
        idField.accept(doc, mediaFile.getId());
        termField.accept(doc, FieldNames.ALBUM, mediaFile.getAlbumName());
        termField.accept(doc, FieldNames.ALBUM_EX, mediaFile.getAlbumName());
        termField.accept(doc, FieldNames.ARTIST, mediaFile.getArtist());
        termField.accept(doc, FieldNames.ARTIST_EX, mediaFile.getArtist());
        String reading = isEmpty(mediaFile.getArtistSort()) ? mediaFile.getArtistReading() : mediaFile.getArtistSort();
        termField.accept(doc, FieldNames.ARTIST_READING, reading);
        pathField.accept(doc, mediaFile.getFolder());
        return doc;
    }

    private Document createArtistDocument(MediaFile mediaFile) {
        Document doc = new Document();
        idField.accept(doc, mediaFile.getId());
        termField.accept(doc, FieldNames.ARTIST, mediaFile.getArtist());
        termField.accept(doc, FieldNames.ARTIST_EX, mediaFile.getArtist());
        String reading = isEmpty(mediaFile.getArtistSort()) ? mediaFile.getArtistReading() : mediaFile.getArtistSort();
        termField.accept(doc, FieldNames.ARTIST_READING, reading);
        pathField.accept(doc, mediaFile.getFolder());
        return doc;
    }

    public Document createDocument(Album album) {
        Document doc = new Document();
        idField.accept(doc, album.getId());
        termField.accept(doc, FieldNames.ALBUM, album.getName());
        termField.accept(doc, FieldNames.ALBUM_EX, album.getName());
        termField.accept(doc, FieldNames.ARTIST, album.getArtist());
        termField.accept(doc, FieldNames.ARTIST_EX, album.getArtist());
        termField.accept(doc, FieldNames.ARTIST_READING, album.getArtistSort());
        numberField.accept(doc, FieldNames.FOLDER_ID, album.getFolderId());
        return doc;
    }

    public Document createDocument(Artist artist, MusicFolder musicFolder) {
        Document doc = new Document();
        idField.accept(doc, artist.getId());
        termField.accept(doc, FieldNames.ARTIST, artist.getName());
        termField.accept(doc, FieldNames.ARTIST_EX, artist.getName());
        String reading = isEmpty(artist.getSort()) ? artist.getReading() : artist.getSort();
        termField.accept(doc, FieldNames.ARTIST_READING, reading);
        numberField.accept(doc, FieldNames.FOLDER_ID, musicFolder.getId());
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
        idField.accept(doc, mediaFile.getId());
        termField.accept(doc, FieldNames.ARTIST, mediaFile.getArtist());
        termField.accept(doc, FieldNames.ARTIST_EX, mediaFile.getArtist());
        String reading = isEmpty(mediaFile.getArtistSort()) ? mediaFile.getArtistReading() : mediaFile.getArtistSort();
        termField.accept(doc, FieldNames.ARTIST_READING, reading);
        keyField.accept(doc, FieldNames.MEDIA_TYPE, mediaFile.getMediaType().name());
        termField.accept(doc, FieldNames.TITLE, mediaFile.getTitle());
        termField.accept(doc, FieldNames.TITLE_EX, mediaFile.getTitle());
        nullableKeyField.accept(doc, FieldNames.GENRE, mediaFile.getGenre());
        numberField.accept(doc, FieldNames.YEAR, mediaFile.getYear());
        pathField.accept(doc, mediaFile.getFolder());
        return doc;
    }

}
