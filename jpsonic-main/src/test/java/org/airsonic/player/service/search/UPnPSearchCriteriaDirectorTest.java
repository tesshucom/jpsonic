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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.airsonic.player.AbstractNeedsScan;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UPnPSearchCriteriaDirectorTest extends AbstractNeedsScan {

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
            }
        }

        @interface Actions {
            @interface construct {
            }
        }

        @interface Result {
            @interface Criteria {
                @interface AssignableClass {
                    @interface MediaFile {
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

                @interface includeComposer {
                    @interface FALSE {
                    }

                    @interface TRUE {
                    }
                }
            }

            @interface IllegalArgument {
            }
        }
    }

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private AnalyzerFactory analyzerFactory;

    @Autowired
    private UPnPSearchCriteriaDirector director;

    private String path = "";
    private String fid = "";

    @BeforeEach
    public void setup() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Method setSearchMethodLegacy = analyzerFactory.getClass().getDeclaredMethod("setSearchMethodLegacy",
                boolean.class);
        setSearchMethodLegacy.setAccessible(true);
        setSearchMethodLegacy.invoke(analyzerFactory, false);
        settingsService.setSearchMethodLegacy(false);
        populateDatabaseOnlyOnce();
        settingsService.setSearchComposer(true);
        for (MusicFolder m : settingsService.getAllMusicFolders()) {
            path = path.concat("f:").concat(m.getPath().getPath()).concat(" ");
            fid = fid.concat("fId:").concat(Integer.toString(m.getId())).concat(" ");
        }
        path = path.trim();
        fid = fid.trim();
    }

    // testClassHierarchy
    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerPerson
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.Artist
    @Test
    public void h01() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class derivedfrom \"object.container.person\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.Artist.class, criteria.getAssignableClass());
        assertEquals("+(((artR:\"test\"~1)^2.2 art:\"test\"~1)) +(" + fid + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerPersonMusicArtist
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.Artist
    @Test
    public void h02() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class derivedfrom \"object.container.person.musicArtist\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.Artist.class, criteria.getAssignableClass());
        assertEquals("+(((artR:\"test\"~1)^2.2 art:\"test\"~1)) +(" + fid + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerAlbum
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.Album
    @Test
    public void h03() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class derivedfrom \"object.container.album\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.Album.class, criteria.getAssignableClass());
        assertEquals("+(((alb:\"test\"~1)^4.6)) +(" + fid + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerAlbumMusicAlbum
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.Album
    @Test
    public void h04() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class derivedfrom \"object.container.album.musicAlbum\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.Album.class, criteria.getAssignableClass());
        assertEquals("+(((alb:\"test\"~1)^4.6)) +(" + fid + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.MUSIC
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.PODCAST
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.AUDIOBOOK
    @Test
    public void h05() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.MediaFile.class, criteria.getAssignableClass());
        assertEquals("+(((tit:\"test\"~1)^4.4)) +(m:MUSIC m:PODCAST m:AUDIOBOOK) +(" + path + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemVideoItem
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.VIDEO
    @Test
    public void h06() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class derivedfrom \"object.item.videoItem\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.MediaFile.class, criteria.getAssignableClass());
        assertEquals("+(((tit:\"test\"~1)^4.4)) +(+m:VIDEO) +(" + path + ")", criteria.getParsedQuery().toString());
    }

    // testException
    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerAlbumPhotoAlbum
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e01() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class derivedfrom \"object.container.album.photoAlbum\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.album.photoAlbum");

    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerPlaylistContainer
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e02() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class derivedfrom \"object.container.playlistContainer\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.playlistContainer");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerGenre
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e03() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class derivedfrom \"object.container.genre\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.genre");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerGenreMusicGenre
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e04() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class derivedfrom \"object.container.genre.musicGenre\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.genre.musicGenre");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerGenreMovieGenre
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e05() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class derivedfrom \"object.container.genre.movieGenre\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.genre.movieGenre");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerStorageSystem
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e06() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class derivedfrom \"object.container.storageSystem\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.storageSystem");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerStorageVolume
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e07() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class derivedfrom \"object.container.storageVolume\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.storageVolume");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectContainerStorageFolder
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e08() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class derivedfrom \"object.container.storageFolder\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class derivedfrom object.container.storageFolder");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerPersonMusicArtist
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.Artist
    @Test
    public void h07() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class = \"object.container.person.musicArtist\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.Artist.class, criteria.getAssignableClass());
        assertEquals("+(((artR:\"test\"~1)^2.2 art:\"test\"~1)) +(" + fid + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbumMusicAlbum
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.Album
    @Test
    public void h08() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class = \"object.container.album.musicAlbum\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.Album.class, criteria.getAssignableClass());
        assertEquals("+(((alb:\"test\"~1)^4.6)) +(" + fid + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemAudioItemMusicTrack
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.MUSIC
    @Test
    public void h09() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.MediaFile.class, criteria.getAssignableClass());
        assertEquals("+(((tit:\"test\"~1)^4.4)) +(m:MUSIC) +(" + path + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemAudioItemAudioBroadcast
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.PODCAST
    @Test
    public void h10() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class = \"object.item.audioItem.audioBroadcast\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.MediaFile.class, criteria.getAssignableClass());
        assertEquals("+(((tit:\"test\"~1)^4.4)) +(m:PODCAST) +(" + path + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemAudioItemAudioBook
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.AUDIOBOOK
    @Test
    public void h11() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class = \"object.item.audioItem.audioBook\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.MediaFile.class, criteria.getAssignableClass());
        assertEquals("+(((tit:\"test\"~1)^4.4)) +(m:AUDIOBOOK) +(" + path + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemVideoItemMovie
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.VIDEO
    @Test
    public void h12() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class = \"object.item.videoItem.movie\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.MediaFile.class, criteria.getAssignableClass());
        assertEquals("+(((tit:\"test\"~1)^4.4)) +(+m:VIDEO) +(" + path + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemVideoItemVideoBroadcast
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.VIDEO
    @Test
    public void h13() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class = \"object.item.videoItem.videoBroadcast\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.MediaFile.class, criteria.getAssignableClass());
        assertEquals("+(((tit:\"test\"~1)^4.4)) +(+m:VIDEO) +(" + path + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemVideoItemMusicVideoClip
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.VIDEO
    @Test
    public void h14() {
        UPnPSearchCriteria criteria = director.construct(0, 50,
                "(upnp:class = \"object.item.videoItem.musicVideoClip\" and dc:title contains \"test\")");
        assertEquals(org.airsonic.player.domain.MediaFile.class, criteria.getAssignableClass());
        assertEquals("+(((tit:\"test\"~1)^4.4)) +(+m:VIDEO) +(" + path + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbumPhotoAlbum
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e09() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class = \"object.container.album.photoAlbum\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class = object.container.album.photoAlbum");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerPlaylistContainer
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e10() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class = \"object.container.playlistContainer\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class = object.container.playlistContainer");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerGenre
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e11() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50, "(upnp:class = \"object.container.genre\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class = object.container.genre");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerGenreMusicGenre
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e12() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class = \"object.container.genre.musicGenre\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class = object.container.genre.musicGenre");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerGenreMovieGenre
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e13() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class = \"object.container.genre.movieGenre\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class = object.container.genre.movieGenre");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerStorageSystem
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e14() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class = \"object.container.storageSystem\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class = object.container.storageSystem");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerStorageVolume
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e15() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class = \"object.container.storageVolume\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class = object.container.storageVolume");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerStorageFolder
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void e16() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50,
                    "(upnp:class = \"object.container.storageVolume\" and dc:title contains \"test\")");
        }, "The current version does not support searching for this class. : upnp:class = object.container.storageVolume");
    }

    // testAmbiguousCase
    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbum
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void a01() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50, "(upnp:class = \"object.container.album\" and dc:title contains \"test\")");
        }, "An insufficient class hierarchy from derivedfrom or a class not supported by the server was specified. : upnp:class = object.container.album");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemAudioItem
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void a02() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50, "(upnp:class = \"object.item.audioItem\" and dc:title contains \"test\")");
        }, "An insufficient class hierarchy from derivedfrom or a class not supported by the server was specified. : upnp:class = object.item.audioItem");
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectItemVideoItem
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.IllegalArgument
    @Test
    public void a03() {
        assertThrows(IllegalArgumentException.class, () -> {
            director.construct(0, 50, "(upnp:class = \"object.item.videoItem\" and dc:title contains \"test\")");
        }, "An insufficient class hierarchy from derivedfrom or a class not supported by the server was specified. : upnp:class = object.item.videoItem");
    }

    // Typical case of Bubble UPnP

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbumMusicAlbum
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.Album
    @DirectorDecisions.Result.Criteria.includeComposer.FALSE
    @Test
    public void b01() {
        String searchQuery1 = "(upnp:class = \"object.container.album.musicAlbum\" and dc:title contains \"にほんごはむずかしい\")";
        UPnPSearchCriteria criteria = director.construct(0, 50, searchQuery1);
        assertEquals(Album.class, criteria.getAssignableClass());
        assertEquals(0, criteria.getOffset());
        assertEquals(50, criteria.getCount());
        assertFalse(criteria.isIncludeComposer()); // MediaFile.class only
        assertEquals(searchQuery1, criteria.getQuery());
        assertEquals("+(((albEX:\"にほ ほん んご ごは はむ むず ずか かし しい\"~1)^4.6 (alb:\"に ほん ご は むずかしい\"~1)^4.6)) +(" + fid + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerPersonMusicArtist
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.Artist
    @DirectorDecisions.Result.Criteria.includeComposer.FALSE
    @Test
    public void b02() {
        String searchQuery2 = "(upnp:class = \"object.container.person.musicArtist\" and dc:title contains \"いきものがかり\")";
        UPnPSearchCriteria criteria = director.construct(1, 51, searchQuery2);
        assertEquals(Artist.class, criteria.getAssignableClass());
        assertEquals(1, criteria.getOffset());
        assertEquals(51, criteria.getCount());
        assertFalse(criteria.isIncludeComposer()); // MediaFile.class only
        assertEquals(searchQuery2, criteria.getQuery());
        assertEquals("+(((artR:\"いき きも もの のが がか かり\"~1)^2.2 artEX:\"いき きも もの のが がか かり\"~1 art:\"いき もの が かり\"~1)) +("
                + fid + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.equal.objectContainerAlbumMusicAlbum
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.Album
    @DirectorDecisions.Result.Criteria.includeComposer.FALSE
    @Test
    public void b03() {
        String searchQuery3 = "(upnp:class = \"object.container.album.musicAlbum\" and upnp:artist contains \"日本語テスト\")";
        UPnPSearchCriteria criteria = director.construct(2, 52, searchQuery3);
        assertEquals(Album.class, criteria.getAssignableClass());
        assertEquals(2, criteria.getOffset());
        assertEquals(52, criteria.getCount());
        assertFalse(criteria.isIncludeComposer()); // MediaFile.class only
        assertEquals(searchQuery3, criteria.getQuery());
        assertEquals("+(((artR:\"日本 本語 語て てす すと\"~1)^2.2 art:\"日本語 テスト\"~1)) +(" + fid + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
    @DirectorDecisions.Conditions.Settings.searchComposer.TRUE
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.MUSIC
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.PODCAST
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.AUDIOBOOK
    @DirectorDecisions.Result.Criteria.includeComposer.TRUE
    @Test
    public void b04() {
        String searchQuery4 = "(upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"なくもんか\")";
        UPnPSearchCriteria criteria = director.construct(3, 53, searchQuery4);
        assertEquals(MediaFile.class, criteria.getAssignableClass());
        assertEquals(3, criteria.getOffset());
        assertEquals(53, criteria.getCount());
        assertTrue(criteria.isIncludeComposer());
        assertEquals(searchQuery4, criteria.getQuery());
        assertEquals("+(((titEX:\"なく くも もん んか\"~1)^4.6 (tit:\"なく もん か\"~1)^4.4)) +(m:MUSIC m:PODCAST m:AUDIOBOOK) +("
                + path + ")", criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
    @DirectorDecisions.Conditions.Params.upnpSearchQuery.creator
    @DirectorDecisions.Conditions.Settings.searchComposer.TRUE
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.MUSIC
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.PODCAST
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.AUDIOBOOK
    @DirectorDecisions.Result.Criteria.includeComposer.TRUE
    @Test
    public void b05() {
        String searchQuery5 = "(upnp:class derivedfrom \"object.item.audioItem\" and (dc:creator contains \"日本語テスト\" or upnp:artist contains \"日本語テスト\"))";
        UPnPSearchCriteria criteria = director.construct(4, 54, searchQuery5);
        assertEquals(MediaFile.class, criteria.getAssignableClass());
        assertEquals(4, criteria.getOffset());
        assertEquals(54, criteria.getCount());
        assertTrue(criteria.isIncludeComposer());
        assertEquals(searchQuery5, criteria.getQuery());
        assertEquals(
                "+(((cmpR:\"日本 本語 語て てす すと\"~1)^2.2 cmp:\"日本語 テスト\"~1) ((artR:\"日本 本語 語て てす すと\"~1)^2.8 (art:\"日本語 テスト\"~1)^2.4)) +(m:MUSIC m:PODCAST m:AUDIOBOOK) +("
                        + path + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemAudioItem
    @DirectorDecisions.Conditions.Params.upnpSearchQuery.creator
    @DirectorDecisions.Conditions.Settings.searchComposer.FALSE
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.MUSIC
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.PODCAST
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.AUDIOBOOK
    @DirectorDecisions.Result.Criteria.includeComposer.FALSE
    @Test
    public void b06() {
        settingsService.setSearchComposer(false);
        String searchQuery5 = "(upnp:class derivedfrom \"object.item.audioItem\" and (dc:creator contains \"日本語テスト\" or upnp:artist contains \"日本語テスト\"))";
        UPnPSearchCriteria criteria = director.construct(4, 54, searchQuery5);
        assertEquals(MediaFile.class, criteria.getAssignableClass());
        assertEquals(4, criteria.getOffset());
        assertEquals(54, criteria.getCount());
        assertFalse(criteria.isIncludeComposer());
        assertEquals(searchQuery5, criteria.getQuery());
        assertEquals(
                "+(() ((artR:\"日本 本語 語て てす すと\"~1)^2.8 (art:\"日本語 テスト\"~1)^2.4)) +(m:MUSIC m:PODCAST m:AUDIOBOOK) +("
                        + path + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.Params.upnpSearchQuery.Class.derivedFrom.objectItemVideoItem
    @DirectorDecisions.Actions.construct
    @DirectorDecisions.Result.Criteria.AssignableClass.MediaFile
    @DirectorDecisions.Result.Criteria.ParsedQuery.MediaType.VIDEO
    @DirectorDecisions.Result.Criteria.includeComposer.TRUE
    @Test
    public void b07() {
        String searchQuery6 = "(upnp:class derivedfrom \"object.item.videoItem\" and dc:title contains \"日本語テスト\")";
        UPnPSearchCriteria criteria = director.construct(5, 55, searchQuery6);
        assertEquals(MediaFile.class, criteria.getAssignableClass());
        assertEquals(5, criteria.getOffset());
        assertEquals(55, criteria.getCount());
        assertTrue(criteria.isIncludeComposer());
        assertEquals(searchQuery6, criteria.getQuery());
        assertEquals("+(((tit:\"日本語 テスト\"~1)^4.4)) +(+m:VIDEO) +(" + path + ")", criteria.getParsedQuery().toString());
    }

}
