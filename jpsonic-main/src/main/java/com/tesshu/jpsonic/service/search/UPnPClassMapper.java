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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.service.search;

/**
 * Mapper class that converts UPnP class names and derived class names into the
 * IndexType used internally for search processing.
 *
 * <p>
 * This class takes UPnP object class identifiers (class names or derived class
 * names) and maps them to the corresponding IndexType used for Lucene-based
 * searching within the system. This allows UPnP search requests to be properly
 * translated into the system's internal search processing.
 * </p>
 *
 * <p>
 * For example, "object.container.person.musicArtist" is mapped to the
 * artist-related IndexType (ARTIST or ARTIST_ID3), depending on the
 * UPnPSearchMethod (FILE_STRUCTURE or ID3).
 * </p>
 */
public class UPnPClassMapper {

    private final UPnPSearchMethod searchMethod;

    public UPnPClassMapper(UPnPSearchMethod searchMethod) {
        this.searchMethod = searchMethod;
    }

    public IndexType mapDerivedFrom(String complement) {
        return switch (complement) {
        case "object.container.person", "object.container.person.musicArtist" ->
            switch (searchMethod) {
            case FILE_STRUCTURE -> IndexType.ARTIST;
            case ID3 -> IndexType.ARTIST_ID3;
            };
        case "object.container.album", "object.container.album.musicAlbum" ->
            switch (searchMethod) {
            case FILE_STRUCTURE -> IndexType.ALBUM;
            case ID3 -> IndexType.ALBUM_ID3;
            };
        case "object.item.audioItem.musicTrack" -> IndexType.SONG;
        case "object.item.audioItem" -> IndexType.SONG;
        case "object.item.videoItem" -> IndexType.SONG;
        default -> null;
        };
    }

    public IndexType mapClass(String complement) {
        return switch (complement) {
        case "object.container.person.musicArtist" -> switch (searchMethod) {
        case FILE_STRUCTURE -> IndexType.ARTIST;
        case ID3 -> IndexType.ARTIST_ID3;
        };
        case "object.container.album.musicAlbum" -> switch (searchMethod) {
        case FILE_STRUCTURE -> IndexType.ALBUM;
        case ID3 -> IndexType.ALBUM_ID3;
        };
        case "object.item.audioItem.musicTrack" -> IndexType.SONG;
        case "object.item.audioItem.audioBroadcast" -> IndexType.SONG;
        case "object.item.audioItem.audioBook" -> IndexType.SONG;
        case "object.item.videoItem.movie", "object.item.videoItem.videoBroadcast",
                "object.item.videoItem.musicVideoClip" ->
            IndexType.SONG;
        default -> null;
        };
    }
}
