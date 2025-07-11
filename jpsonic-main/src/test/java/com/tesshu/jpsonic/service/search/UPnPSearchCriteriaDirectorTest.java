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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.Documented;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.processor.UpnpProcessorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class UPnPSearchCriteriaDirectorTest {

    @Documented
    private @interface DirectorDecisions {
        @interface Conditions {
            @interface Params {
                @interface upnpSearchQuery {
                    @interface Class {
                        @interface derivedFrom {
                            @interface objectContainerPerson {
                            }

                            @interface objectContainerPersonMusicArtist {
                            }

                            @interface objectContainerAlbum {
                            }

                            @interface objectContainerAlbumMusicAlbum {
                            }

                            @interface objectItemAudioItem {
                            }

                            @interface objectItemVideoItem {
                            }

                            @interface objectContainerAlbumPhotoAlbum {
                            }

                            @interface objectContainerPlaylistContainer {
                            }

                            @interface objectContainerGenre {
                            }

                            @interface objectContainerGenreMusicGenre {
                            }

                            @interface objectContainerGenreMovieGenre {
                            }

                            @interface objectContainerStorageSystem {
                            }

                            @interface objectContainerStorageVolume {
                            }

                            @interface objectContainerStorageFolder {
                            }
                        }

                        @interface equal {
                            @interface objectContainerPersonMusicArtist {
                            }

                            @interface objectContainerAlbumMusicAlbum {
                            }

                            @interface objectItemAudioItemMusicTrack {
                            }

                            @interface objectItemAudioItemAudioBroadcast {
                            }

                            @interface objectItemAudioItemAudioBook {
                            }

                            @interface objectItemVideoItemMovie {
                            }

                            @interface objectItemVideoItemVideoBroadcast {
                            }

                            @interface objectItemVideoItemMusicVideoClip {
                            }

                            @interface objectContainerAlbumPhotoAlbum {
                            }

                            @interface objectContainerPlaylistContainer {
                            }

                            @interface objectContainerGenre {
                            }

                            @interface objectContainerGenreMusicGenre {
                            }

                            @interface objectContainerGenreMovieGenre {
                            }

                            @interface objectContainerStorageSystem {
                            }

                            @interface objectContainerStorageVolume {
                            }

                            @interface objectContainerStorageFolder {
                            }

                            @interface objectContainerAlbum {
                            }

                            @interface objectItemAudioItem {
                            }

                            @interface objectItemVideoItem {
                            }
                        }
                    }

                    @interface creator {
                    }
                }

                @SuppressWarnings("unused")
                @interface offset {
                }

                @SuppressWarnings("unused")
                @interface count {
                }
            }

            @interface Settings {
                @interface searchComposer {
                    @interface FALSE {
                    }

                    @interface TRUE {
                    }
                }

                @SuppressWarnings("unused")
                @interface musicFolders {
                    @interface SINGLE_FOLDERS {
                    }

                    @interface MULTI_FOLDERS {
                    }
                }

                @interface searchMethod {
                    @interface FileStructure {
                    }

                    @interface Id3 {
                    }
                }
            }
        }

        @interface Actions {
            @interface construct {
            }
        }

        @interface Result {
            @interface Criteria {
                @interface TargetType {
                    @interface Song {
                    }

                    @interface ArtistId3 {
                    }

                    @interface AlbumId3 {
                    }

                    @interface Artist {
                    }

                    @interface Album {
                    }
                }

                @interface ParsedQuery {
                    @interface MediaType {
                        @interface MUSIC {
                        }

                        @interface PODCAST {
                        }

                        @interface AUDIOBOOK {
                        }

                        @interface VIDEO {
                        }
                    }
                }
            }

            @interface IllegalArgument {
            }
        }
    }

    private SettingsService settingsService;
    private UpnpProcessorUtil util;
    private MusicFolderService musicFolderService;
    private UPnPSearchCriteriaDirector director;

    private String path = "";
    private String fid = "";

    @BeforeEach
    public void setup() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        settingsService = mock(SettingsService.class);
        Mockito.when(settingsService.isSearchComposer()).thenReturn(true);
        Mockito.when(settingsService.getUPnPSearchMethod()).thenReturn(UPnPSearchMethod.ID3.name());

        List<MusicFolder> musicFolders = new ArrayList<>();
        musicFolders.add(new MusicFolder(1, "dummy", "accessible", true, now(), 1, false));
        musicFolderService = mock(MusicFolderService.class);
        Mockito
            .when(musicFolderService.getMusicFoldersForUser(User.USERNAME_GUEST))
            .thenReturn(musicFolders);

        for (MusicFolder m : musicFolders) {
            path = path.concat("f:").concat(m.getPathString()).concat(" ");
            fid = fid.concat("fId:").concat(Integer.toString(m.getId())).concat(" ");
        }
        path = path.trim();
        fid = fid.trim();

        util = new UpnpProcessorUtil(musicFolderService, mock(SecurityService.class),
                settingsService, null);
        director = new UPnPSearchCriteriaDirector(util.getUPnPSearchMethod(),
                util.getGuestFolders(),
                new QueryFactory(settingsService, new AnalyzerFactory(settingsService)));
    }

    @Nested
    class ClassHierarchyTest {

        @Nested
        class OpDerivedTest {

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerPerson
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.ArtistId3
            @Test
            public void h01() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.person\" and dc:title contains \"test\")");
                assertEquals(IndexType.ARTIST_ID3, criteria.targetType());
                assertEquals("+((art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(" + fid + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerPersonMusicArtist
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.ArtistId3
            @Test
            public void h02() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.person.musicArtist\" and dc:title contains \"test\")");
                assertEquals(IndexType.ARTIST_ID3, criteria.targetType());
                assertEquals("+((art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(" + fid + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerAlbum
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.AlbumId3
            @Test
            public void h03() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.album\" and dc:title contains \"test\")");
                assertEquals(IndexType.ALBUM_ID3, criteria.targetType());
                assertEquals("+(((alb:\"test\"~1)^4.0)) +(" + fid + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerAlbumMusicAlbum
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.AlbumId3
            @Test
            public void h04() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.album.musicAlbum\" and dc:title contains \"test\")");
                assertEquals(IndexType.ALBUM_ID3, criteria.targetType());
                assertEquals("+(((alb:\"test\"~1)^4.0)) +(" + fid + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Song
            @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.MUSIC
            @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.PODCAST
            @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.AUDIOBOOK
            @Test
            public void h05() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"test\")");
                assertEquals(IndexType.SONG, criteria.targetType());
                assertEquals("+(((tit:\"test\"~1)^6.0)) +(m:MUSIC m:PODCAST m:AUDIOBOOK) +(" + path
                        + ")", criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemVideoItem
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Song
            @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.VIDEO
            @Test
            public void h06() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.item.videoItem\" and dc:title contains \"test\")");
                assertEquals(IndexType.SONG, criteria.targetType());
                assertEquals("+(((tit:\"test\"~1)^6.0)) +(m:VIDEO) +(" + path + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerPersonMusicArtist
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.ArtistId3
            @Test
            public void h07() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.person.musicArtist\" and dc:title contains \"test\")");
                assertEquals(IndexType.ARTIST_ID3, criteria.targetType());
                assertEquals("+((art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(" + fid + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerPerson
            @DirectorDecisions.Conditions.Settings.searchMethod.FileStructure
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Artist
            @Test
            public void h08() {
                Mockito
                    .when(settingsService.getUPnPSearchMethod())
                    .thenReturn(UPnPSearchMethod.FILE_STRUCTURE.name());
                director = new UPnPSearchCriteriaDirector(util.getUPnPSearchMethod(),
                        util.getGuestFolders(),
                        new QueryFactory(settingsService, new AnalyzerFactory(settingsService)));

                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.person\" and dc:title contains \"test\")");
                assertEquals(IndexType.ARTIST, criteria.targetType());
                assertEquals("+((art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(f:dummy)",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerAlbum
            @DirectorDecisions.Conditions.Settings.searchMethod.FileStructure
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Album
            @Test
            public void h09() {
                Mockito
                    .when(settingsService.getUPnPSearchMethod())
                    .thenReturn(UPnPSearchMethod.FILE_STRUCTURE.name());
                director = new UPnPSearchCriteriaDirector(util.getUPnPSearchMethod(),
                        util.getGuestFolders(),
                        new QueryFactory(settingsService, new AnalyzerFactory(settingsService)));

                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.album\" and dc:title contains \"test\")");
                assertEquals(IndexType.ALBUM, criteria.targetType());
                assertEquals("+(((alb:\"test\"~1)^4.0)) +(f:dummy)",
                        criteria.parsedQuery().toString());
            }
        }

        @Nested
        class OpEqClassTest {

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerPersonMusicArtist
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.ArtistId3
            @Test
            public void h10() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.person.musicArtist\" and dc:title contains \"test\")");
                assertEquals(IndexType.ARTIST_ID3, criteria.targetType());
                assertEquals("+((art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(" + fid + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbumMusicAlbum
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.AlbumId3
            @Test
            public void h11() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.album.musicAlbum\" and dc:title contains \"test\")");
                assertEquals(IndexType.ALBUM_ID3, criteria.targetType());
                assertEquals("+(((alb:\"test\"~1)^4.0)) +(" + fid + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemAudioItemMusicTrack
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Song
            @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.MUSIC
            @Test
            public void h12() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"test\")");
                assertEquals(IndexType.SONG, criteria.targetType());
                assertEquals("+(((tit:\"test\"~1)^6.0)) +(m:MUSIC) +(" + path + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemAudioItemAudioBroadcast
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Song
            @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.PODCAST
            @Test
            public void h13() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.item.audioItem.audioBroadcast\" and dc:title contains \"test\")");
                assertEquals(IndexType.SONG, criteria.targetType());
                assertEquals("+(((tit:\"test\"~1)^6.0)) +(m:PODCAST) +(" + path + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemAudioItemAudioBook
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Song
            @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.AUDIOBOOK
            @Test
            public void h14() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.item.audioItem.audioBook\" and dc:title contains \"test\")");
                assertEquals(IndexType.SONG, criteria.targetType());
                assertEquals("+(((tit:\"test\"~1)^6.0)) +(m:AUDIOBOOK) +(" + path + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemVideoItemMovie
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Song
            @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.VIDEO
            @Test
            public void h15() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.item.videoItem.movie\" and dc:title contains \"test\")");
                assertEquals(IndexType.SONG, criteria.targetType());
                assertEquals("+(((tit:\"test\"~1)^6.0)) +(+m:VIDEO) +(" + path + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemVideoItemVideoBroadcast
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Song
            @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.VIDEO
            @Test
            public void h16() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.item.videoItem.videoBroadcast\" and dc:title contains \"test\")");
                assertEquals(IndexType.SONG, criteria.targetType());
                assertEquals("+(((tit:\"test\"~1)^6.0)) +(+m:VIDEO) +(" + path + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemVideoItemMusicVideoClip
            @DirectorDecisions.Conditions.Settings.searchMethod.Id3
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Song
            @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.VIDEO
            @Test
            public void h17() {
                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.item.videoItem.musicVideoClip\" and dc:title contains \"test\")");
                assertEquals(IndexType.SONG, criteria.targetType());
                assertEquals("+(((tit:\"test\"~1)^6.0)) +(+m:VIDEO) +(" + path + ")",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerPersonMusicArtist
            @DirectorDecisions.Conditions.Settings.searchMethod.FileStructure
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Artist
            @Test
            public void h18() {
                Mockito
                    .when(settingsService.getUPnPSearchMethod())
                    .thenReturn(UPnPSearchMethod.FILE_STRUCTURE.name());
                director = new UPnPSearchCriteriaDirector(util.getUPnPSearchMethod(),
                        util.getGuestFolders(),
                        new QueryFactory(settingsService, new AnalyzerFactory(settingsService)));

                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.person.musicArtist\" and dc:title contains \"test\")");
                assertEquals(IndexType.ARTIST, criteria.targetType());
                assertEquals("+((art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(f:dummy)",
                        criteria.parsedQuery().toString());
            }

            @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbumMusicAlbum
            @DirectorDecisions.Conditions.Settings.searchMethod.FileStructure
            @DirectorDecisions.Actions.construct
            @DirectorDecisions.Result.Criteria.TargetType.Album
            @Test
            public void h19() {
                Mockito
                    .when(settingsService.getUPnPSearchMethod())
                    .thenReturn(UPnPSearchMethod.FILE_STRUCTURE.name());
                director = new UPnPSearchCriteriaDirector(util.getUPnPSearchMethod(),
                        util.getGuestFolders(),
                        new QueryFactory(settingsService, new AnalyzerFactory(settingsService)));

                UPnPSearchCriteria criteria = director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.album.musicAlbum\" and dc:title contains \"test\")");
                assertEquals(IndexType.ALBUM, criteria.targetType());
                assertEquals("+(((alb:\"test\"~1)^4.0)) +(f:dummy)",
                        criteria.parsedQuery().toString());
            }
        }
    }

    @Nested
    class Exception4AssignableClassTest {

        // testAmbiguousCase
        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbum
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void a01() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.album\" and dc:title contains \"test\")");
            }, "An insufficient class hierarchy from derivedfrom or a class not supported by the server was specified. : upnp:class = object.container.album");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemAudioItem
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void a02() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.item.audioItem\" and dc:title contains \"test\")");
            }, "An insufficient class hierarchy from derivedfrom or a class not supported by the server was specified. : upnp:class = object.item.audioItem");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemVideoItem
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void a03() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.item.videoItem\" and dc:title contains \"test\")");
            }, "An insufficient class hierarchy from derivedfrom or a class not supported by the server was specified. : upnp:class = object.item.videoItem");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerAlbumPhotoAlbum
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e01() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.album.photoAlbum\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.album.photoAlbum");

        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerPlaylistContainer
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e02() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.playlistContainer\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.playlistContainer");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerGenre
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e03() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.genre\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.genre");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerGenreMusicGenre
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e04() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.genre.musicGenre\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.genre.musicGenre");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerGenreMovieGenre
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e05() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.genre.movieGenre\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.genre.movieGenre");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerStorageSystem
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e06() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.storageSystem\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.storageSystem");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerStorageVolume
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e07() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.storageVolume\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.storageVolume");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerStorageFolder
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e08() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class derivedfrom \"object.container.storageFolder\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.storageFolder");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbumPhotoAlbum
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e09() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.album.photoAlbum\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class = object.container.album.photoAlbum");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerPlaylistContainer
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e10() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.playlistContainer\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class = object.container.playlistContainer");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerGenre
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e11() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.genre\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class = object.container.genre");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerGenreMusicGenre
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e12() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.genre.musicGenre\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class = object.container.genre.musicGenre");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerGenreMovieGenre
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e13() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.genre.movieGenre\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class = object.container.genre.movieGenre");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerStorageSystem
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e14() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.storageSystem\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class = object.container.storageSystem");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerStorageVolume
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e15() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.storageVolume\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class = object.container.storageVolume");
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerStorageFolder
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.IllegalArgument
        @Test
        public void e16() {
            assertThrows(IllegalArgumentException.class, () -> {
                director
                    .construct(0, 50,
                            "(upnp:class = \"object.container.storageVolume\" and dc:title contains \"test\")");
            }, "The current version does not support searching for this class. : upnp:class = object.container.storageVolume");
        }
    }

    @Nested
    class BubbleUPnPTest {

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbumMusicAlbum
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.AlbumId3
        @Test
        public void b01() {
            String searchQuery1 = "(upnp:class = \"object.container.album.musicAlbum\" and dc:title contains \"にほんごはむずかしい\")";
            UPnPSearchCriteria criteria = director.construct(0, 50, searchQuery1);
            assertEquals(IndexType.ALBUM_ID3, criteria.targetType());
            assertEquals(0, criteria.offset());
            assertEquals(50, criteria.count());
            assertEquals(searchQuery1, criteria.input());
            assertEquals(
                    "+(((alb:\"に ほん ご は むずかしい\"~1)^4.0 (albR:\"にほ ほん んご ごは はむ むず ずか かし しい\"~1)^4.2)) +("
                            + fid + ")",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerPersonMusicArtist
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.ArtistId3
        @Test
        public void b02() {
            String searchQuery2 = "(upnp:class = \"object.container.person.musicArtist\" and dc:title contains \"いきものがかり\")";
            UPnPSearchCriteria criteria = director.construct(1, 51, searchQuery2);
            assertEquals(IndexType.ARTIST_ID3, criteria.targetType());
            assertEquals(1, criteria.offset());
            assertEquals(51, criteria.count());
            assertEquals(searchQuery2, criteria.input());
            assertEquals(
                    "+((art:\"いき もの が かり\"~1 (artR:\"いき きも もの のが がか かり\"~1)^2.2)) +(" + fid + ")",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbumMusicAlbum
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.AlbumId3
        @Test
        public void b03() {
            String searchQuery3 = "(upnp:class = \"object.container.album.musicAlbum\" and upnp:artist contains \"日本語テスト\")";
            UPnPSearchCriteria criteria = director.construct(2, 52, searchQuery3);
            assertEquals(IndexType.ALBUM_ID3, criteria.targetType());
            assertEquals(2, criteria.offset());
            assertEquals(52, criteria.count());
            assertEquals(searchQuery3, criteria.input());
            assertEquals("+((art:\"日本語 テスト\"~1 (artR:\"日本 本語 語て てす すと\"~1)^2.2)) +(" + fid + ")",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
        @DirectorDecisions.Conditions.Settings.searchComposer.TRUE
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.Song
        @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.MUSIC
        @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.PODCAST
        @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.AUDIOBOOK
        @Test
        public void b04() {
            String searchQuery4 = "(upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"なくもんか\")";
            UPnPSearchCriteria criteria = director.construct(3, 53, searchQuery4);
            assertEquals(IndexType.SONG, criteria.targetType());
            assertEquals(3, criteria.offset());
            assertEquals(53, criteria.count());
            assertEquals(searchQuery4, criteria.input());
            assertEquals(
                    "+(((tit:\"なく もん か\"~1)^6.0 (titR:\"なく くも もん んか\"~1)^6.2)) +(m:MUSIC m:PODCAST m:AUDIOBOOK) +("
                            + path + ")",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
        @DirectorDecisions.Conditions.Params.upnpSearchQuery.creator
        @DirectorDecisions.Conditions.Settings.searchComposer.TRUE
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.Song
        @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.MUSIC
        @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.PODCAST
        @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.AUDIOBOOK
        @Test
        public void b05() {
            String searchQuery5 = "(upnp:class derivedfrom \"object.item.audioItem\" and (dc:creator contains \"日本語テスト\" or upnp:artist contains \"日本語テスト\"))";
            UPnPSearchCriteria criteria = director.construct(4, 54, searchQuery5);
            assertEquals(IndexType.SONG, criteria.targetType());
            assertEquals(4, criteria.offset());
            assertEquals(54, criteria.count());
            assertEquals(searchQuery5, criteria.input());
            assertEquals(
                    "+((cmp:\"日本語 テスト\"~1 (cmpR:\"日本 本語 語て てす すと\"~1)^2.2) ((art:\"日本語 テスト\"~1)^4.0 (artR:\"日本 本語 語て てす すと\"~1)^4.2)) +(m:MUSIC m:PODCAST m:AUDIOBOOK) +("
                            + path + ")",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
        @DirectorDecisions.Conditions.Params.upnpSearchQuery.creator
        @DirectorDecisions.Conditions.Settings.searchComposer.FALSE
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.Song
        @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.MUSIC
        @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.PODCAST
        @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.AUDIOBOOK
        @Test
        public void b06() {
            Mockito.when(settingsService.isSearchComposer()).thenReturn(false);
            String searchQuery5 = "(upnp:class derivedfrom \"object.item.audioItem\" and (dc:creator contains \"日本語テスト\" or upnp:artist contains \"日本語テスト\"))";
            UPnPSearchCriteria criteria = director.construct(4, 54, searchQuery5);
            assertEquals(IndexType.SONG, criteria.targetType());
            assertEquals(4, criteria.offset());
            assertEquals(54, criteria.count());
            assertEquals(searchQuery5, criteria.input());
            assertEquals(
                    "+(((art:\"日本語 テスト\"~1)^4.0 (artR:\"日本 本語 語て てす すと\"~1)^4.2)) +(m:MUSIC m:PODCAST m:AUDIOBOOK) +("
                            + path + ")",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemVideoItem
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.Song
        @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.VIDEO
        @Test
        public void b07() {
            String searchQuery6 = "(upnp:class derivedfrom \"object.item.videoItem\" and dc:title contains \"日本語テスト\")";
            UPnPSearchCriteria criteria = director.construct(5, 55, searchQuery6);
            assertEquals(IndexType.SONG, criteria.targetType());
            assertEquals(5, criteria.offset());
            assertEquals(55, criteria.count());
            assertEquals(searchQuery6, criteria.input());
            assertEquals("+(((tit:\"日本語 テスト\"~1)^6.0)) +(m:VIDEO) +(" + path + ")",
                    criteria.parsedQuery().toString());
        }
    }

    @Nested
    class HiFiCastTest {

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.Song
        @Test
        public void h01() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        "upnp:class derivedfrom \"object.item.audioItem.musicTrack\" and dc:title contains \"test\"");
            assertEquals(IndexType.SONG, criteria.targetType());
            assertEquals("+(((tit:\"test\"~1)^6.0)) +(m:MUSIC) +(f:dummy)",
                    criteria.parsedQuery().toString());
        }
    }

    @Nested
    class MediaMonkey4AndroidTest {

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerPersonMusicArtist
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.ArtistId3
        @Test
        public void m01() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        "(upnp:class = \"object.container.person.musicArtist\") and (dc:title contains \"test\" or upnp:genre contains \"test\" )");
            assertEquals(IndexType.ARTIST_ID3, criteria.targetType());
            assertEquals("+((art:\"test\"~1 (artR:\"test\"~1)^2.2) (g:\"test\"~1)) +(fId:1)",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerAlbumMusicAlbum
        @DirectorDecisions.Conditions.Params.upnpSearchQuery.creator
        @DirectorDecisions.Conditions.Settings.searchComposer.TRUE
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.AlbumId3
        @Test
        public void m02() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        """
                                (upnp:class = "object.container.album.musicAlbum") \
                                and (dc:title contains "test" or dc:creator contains "test" or upnp:artist contains "test" )\
                                """);
            assertEquals(IndexType.ALBUM_ID3, criteria.targetType());
            assertEquals(
                    "+(((alb:\"test\"~1)^4.0) (cmp:\"test\"~1 cmpR:\"test\"~1) (art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(fId:1)",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerAlbumMusicAlbum
        @DirectorDecisions.Conditions.Params.upnpSearchQuery.creator
        @DirectorDecisions.Conditions.Settings.searchComposer.FALSE
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.AlbumId3
        @Test
        public void m03() {
            Mockito.when(settingsService.isSearchComposer()).thenReturn(false);
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        """
                                (upnp:class = "object.container.album.musicAlbum") \
                                and (dc:title contains "test" or dc:creator contains "test" or upnp:artist contains "test" )\
                                """);
            assertEquals(IndexType.ALBUM_ID3, criteria.targetType());
            assertEquals(
                    "+(((alb:\"test\"~1)^4.0) (art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(fId:1)",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
        @DirectorDecisions.Conditions.Params.upnpSearchQuery.creator
        @DirectorDecisions.Conditions.Settings.searchComposer.TRUE
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.Song
        @Test
        public void m04() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        """
                                (upnp:class derivedfrom "object.item.audioItem" or upnp:class derivedfrom "object.item.videoItem ") \
                                and (dc:title contains "test" \
                                or dc:creator contains "test" \
                                or upnp:artist contains "test" \
                                or upnp:albumArtist contains "test" \
                                or upnp:album contains "test" \
                                or upnp:author contains "test" \
                                or upnp:genre contains "test" )\
                                """);
            assertEquals(IndexType.SONG, criteria.targetType());
            assertEquals("+(((tit:\"test\"~1)^6.0) " //
                    + "(cmp:\"test\"~1 (cmpR:\"test\"~1)^2.2) " //
                    + "((art:\"test\"~1)^4.0 (artR:\"test\"~1)^4.2) " //
                    + "(g:\"test\"~1)) " //
                    + "+(m:MUSIC m:PODCAST m:AUDIOBOOK m:VIDEO) " // audio or video
                    + "+(f:dummy)", criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
        @DirectorDecisions.Conditions.Params.upnpSearchQuery.creator
        @DirectorDecisions.Conditions.Settings.searchComposer.FALSE
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.Song
        @Test
        public void m05() {
            Mockito.when(settingsService.isSearchComposer()).thenReturn(false);
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        """
                                (upnp:class derivedfrom "object.item.audioItem" or upnp:class derivedfrom "object.item.videoItem ") \
                                and (dc:title contains "test" \
                                or dc:creator contains "test" \
                                or upnp:artist contains "test" \
                                or upnp:albumArtist contains "test" \
                                or upnp:album contains "test" \
                                or upnp:author contains "test" \
                                or upnp:genre contains "test" )\
                                """);
            assertEquals(IndexType.SONG, criteria.targetType());
            assertEquals("+(((tit:\"test\"~1)^6.0) " //
                    + "((art:\"test\"~1)^4.0 (artR:\"test\"~1)^4.2) " //
                    + "(g:\"test\"~1)) " //
                    + "+(m:MUSIC m:PODCAST m:AUDIOBOOK m:VIDEO) " // audio or video
                    + "+(f:dummy)", criteria.parsedQuery().toString());
        }
    }

    @Nested
    class AKConnectTest {

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemAudioItemMusicTrack
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.Song
        @Test
        public void ak01() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        "upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"test\"");
            assertEquals(IndexType.SONG, criteria.targetType());
            assertEquals("+(((tit:\"test\"~1)^6.0)) +(m:MUSIC) +(f:dummy)", // music only
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbumMusicAlbum
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.AlbumId3
        @Test
        public void ak02() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        "upnp:class = \"object.container.album.musicAlbum\" and dc:title contains \"test\"");
            assertEquals(IndexType.ALBUM_ID3, criteria.targetType());
            assertEquals("+(((alb:\"test\"~1)^4.0)) +(fId:1)", criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerPersonMusicArtist
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.ArtistId3
        @Test
        public void ak03() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        "upnp:class = \"object.container.person.musicArtist\" and dc:title contains \"test\"");
            assertEquals(IndexType.ARTIST_ID3, criteria.targetType());
            assertEquals("+((art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(fId:1)",
                    criteria.parsedQuery().toString());
        }
    }

    @Nested
    class Foobar2k4WinTest {

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.Song
        @Test
        public void f01() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50, //
                        "upnp:class derivedfrom \"object.item.audioItem\" " //
                                + "and (dc:title contains \"test\" " //
                                + "or upnp:genre contains \"test\" " //
                                + "or upnp:album contains \"test\" " //
                                + "or upnp:artist contains \"test\" " //
                                + "or dc:creator contains \"test\" " //
                                + "or dc:publisher contains \"test\" " //
                                + "or dc:language contains \"test\" " //
                                + "or upnp:producer contains \"test\" " //
                                + "or upnp:actor contains \"test\" " //
                                + "or upnp:director contains \"test\" " //
                                + "or dc:description contains \"test\" " //
                                + "or microsoft:artistAlbumArtist contains \"test\" " //
                                + "or microsoft:artistPerformer contains \"test\" " //
                                + "or microsoft:artistConductor contains \"test\" " //
                                + "or microsoft:authorComposer contains \"test\" " //
                                + "or microsoft:authorOriginalLyricist contains \"test\" " //
                                + "or microsoft:authorWriter contains \"test\" " //
                                + "or upnp:userAnnotation contains \"test\" " //
                                + "or upnp:longDescription contains \"test\")");
            assertEquals(IndexType.SONG, criteria.targetType());
            assertEquals("+(((tit:\"test\"~1)^6.0) " //
                    + "(g:\"test\"~1) " //
                    + "((art:\"test\"~1)^4.0 (artR:\"test\"~1)^4.2) " //
                    + "(cmp:\"test\"~1 (cmpR:\"test\"~1)^2.2)) " //
                    + "+(m:MUSIC m:PODCAST m:AUDIOBOOK) +(f:dummy)",
                    criteria.parsedQuery().toString());
        }
    }

    @Nested
    class KazooTest {

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerPersonMusicArtist
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.ArtistId3
        @Test
        public void k01() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        "upnp:class derivedfrom \"object.container.person.musicArtist\" and dc:title contains \"test\"");
            assertEquals(IndexType.ARTIST_ID3, criteria.targetType());
            assertEquals("+((art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(fId:1)",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerPersonMusicArtist
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.ArtistId3
        @Test
        public void k02() {
            // This case is a query that doesn't make much sense to Jpsonic
            UPnPSearchCriteria criteria = director.construct(0, 50, """
                    upnp:class derivedfrom "object.container.person.musicArtist" \
                    and dc:title contains "test" and upnp:genre contains "classical"\
                    """);
            assertEquals(IndexType.ARTIST_ID3, criteria.targetType());
            assertEquals("+((art:\"test\"~1 (artR:\"test\"~1)^2.2)) +(fId:1)",
                    criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerAlbum
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.AlbumId3
        @Test
        public void k03() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50,
                        "upnp:class derivedfrom \"object.container.album\" and dc:title contains \"test\"");
            assertEquals(IndexType.ALBUM_ID3, criteria.targetType());
            assertEquals("+(((alb:\"test\"~1)^4.0)) +(fId:1)", criteria.parsedQuery().toString());
        }

        @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
        @DirectorDecisions.Conditions.Settings.searchMethod.Id3
        @DirectorDecisions.Actions.construct
        @DirectorDecisions.Result.Criteria.TargetType.Song
        @Test
        public void k04() {
            UPnPSearchCriteria criteria = director
                .construct(0, 50, //
                        "upnp:class derivedfrom \"object.item.audioItem\" " //
                                + "and ( dc:title contains \"test\" " //
                                + "or upnp:album contains \"test\" " //
                                + "or upnp:artist contains \"test\" " //
                                + "or upnp:genre contains \"test\" )");
            assertEquals(IndexType.SONG, criteria.targetType());
            assertEquals("+(((tit:\"test\"~1)^6.0) " //
                    + "((art:\"test\"~1)^4.0 (artR:\"test\"~1)^4.2) " //
                    + "(g:\"test\"~1)) " //
                    + "+(m:MUSIC m:PODCAST m:AUDIOBOOK) +(f:dummy)", //
                    criteria.parsedQuery().toString());
        }
    }
}
