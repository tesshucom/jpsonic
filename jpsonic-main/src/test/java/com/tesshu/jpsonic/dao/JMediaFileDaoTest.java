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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.lang.annotation.Documented;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.domain.SortCandidate;
import org.airsonic.player.AbstractNeedsScan;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.MediaScannerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;

class JMediaFileDaoTest extends AbstractNeedsScan {

    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Sort/Cleansing/ArtistSort/Merge"));
        MUSIC_FOLDERS.add(new MusicFolder(1, musicDir, "Duplicate", true, new Date()));
    }

    @Autowired
    private JMediaFileDao mediaFileDao;

    @Autowired
    private JArtistDao artistDao;

    @Autowired
    private JAlbumDao albumDao;

    @Autowired
    private MediaScannerService mediaScannerService;

    private List<SortCandidate> candidates;

    @Documented
    private @interface ComparatorsDecisions {
        @interface Actions {
            @interface GetDuplicateSort {
            }
        }

        @interface DataConditions {
            @interface FieldToSetDifferentSortValue {
                @interface AlbumArtist {
                }

                @interface Artist {
                }

                @interface Composer {
                }
            }

            @interface NumberOfFiles {
                @interface Multi {
                }

                @interface Single {
                }
            }

            @interface SetChangeDate {
            }

            @interface TagArtistAndDirectoryArtist {
                @interface Match {
                }

                @interface NoMatch {
                }
            }
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.NoMatch
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Single
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Artist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Composer
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    void c01() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case01".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            // artist-sort is adopted instead of composer-sort
            assertEquals("artistA", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.NoMatch
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Single
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Artist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.AlbumArtist
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    void c02() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case02".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            // album-artist-sort is adopted instead of artist-sort
            assertEquals("artistD", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.NoMatch
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Single
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.AlbumArtist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Composer
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    void c03() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case03".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            // album-artist-sort is adopted instead of composer-sort
            assertEquals("artistE", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.NoMatch
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Multi
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Artist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Composer
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    void c04() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case04".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            // artist-sort is adopted instead of composer-sort
            assertEquals("artistH", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.NoMatch
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Multi
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Artist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.AlbumArtist
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    void c05() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case05".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            // album-artist-sort is adopted instead of artist-sort
            assertEquals("artistJ", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.NoMatch
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Multi
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.AlbumArtist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Composer
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void c06() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case06".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            // album-artist-sort is adopted instead of composer-sort
            assertEquals("artistL", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.NoMatch
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Multi
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Artist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Composer
    @ComparatorsDecisions.DataConditions.SetChangeDate
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void c07() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case07".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            // artist-sort is adopted instead of composer-sort
            // but if change-date of the file is newer, artist-sort may come first
            assertEquals("artistM", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.NoMatch
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Multi
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Artist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.AlbumArtist
    @ComparatorsDecisions.DataConditions.SetChangeDate
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    void c08() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case08".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            // album-artist-sort is adopted instead of artist-sort
            // but if change-date of the file is newer, artist-sort may come first
            assertEquals("artistO", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.NoMatch
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Multi
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.AlbumArtist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Composer
    @ComparatorsDecisions.DataConditions.SetChangeDate
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    void c09() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case09".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            // album-artist-sort is adopted instead of composer-sort
            // but if change-date of the file is newer, artist-sort may come first
            assertEquals("artistQ", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.Match
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Single
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Artist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.AlbumArtist
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    void c10() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case10".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            assertEquals("artistT", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @ComparatorsDecisions.DataConditions.TagArtistAndDirectoryArtist.Match
    @ComparatorsDecisions.DataConditions.NumberOfFiles.Multi
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Artist
    @ComparatorsDecisions.DataConditions.FieldToSetDifferentSortValue.Composer
    @ComparatorsDecisions.DataConditions.SetChangeDate
    @ComparatorsDecisions.Actions.GetDuplicateSort
    @Test
    void c11() {
        Optional<SortCandidate> candidate = candidates.stream().filter(s -> "case11".equals(s.getName())).findFirst();
        if (candidate.isPresent()) {
            assertEquals("artistU", candidate.get().getSort());
        } else {
            Assertions.fail();
        }
    }

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @BeforeEach
    public void setup() {
        Date now = new Date();

        mediaScannerService.setJpsonicCleansingProcess(false);

        populateDatabaseOnlyOnce(null, () -> {
            List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, MUSIC_FOLDERS);
            albums.forEach(a -> {
                List<MediaFile> files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, a.getPath(), false);
                files.stream().filter(m -> "file10".equals(m.getName()) || "file12".equals(m.getName())
                        || "file14".equals(m.getName()) || "file17".equals(m.getName())).forEach(m -> {
                            m.setChanged(now);
                            mediaFileDao.createOrUpdateMediaFile(m);
                        });
            });
            return true;
        });

        mediaScannerService.setJpsonicCleansingProcess(true);

        candidates = mediaFileDao.guessPersonsSorts();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testGetDirtySorts() {

        assertEquals(11, candidates.size());

        List<MediaFile> dirtySortsAll = candidates.stream().flatMap(c -> mediaFileDao.getDirtySorts(c).stream())
                .collect(Collectors.toList());
        assertEquals(22, dirtySortsAll.size());
        assertEquals(2, dirtySortsAll.stream().filter(m -> m.getMediaType() == MediaType.DIRECTORY).count());
        assertEquals(5, dirtySortsAll.stream().filter(m -> m.getMediaType() == MediaType.ALBUM).count());
        assertEquals(15, dirtySortsAll.stream().filter(m -> m.getMediaType() == MediaType.MUSIC).count());

        candidates.forEach(c -> {
            List<MediaFile> dirtySortsFiles = mediaFileDao.getDirtySorts(c);
            dirtySortsFiles.forEach(m -> {
                final String name = m.getName();
                switch (name) {
                case "file1":
                    assertEquals(c.getName(), m.getArtist());
                    assertEquals(c.getSort(), m.getArtistSort());
                    assertEquals(c.getName(), m.getComposer());
                    assertNotEquals(c.getSort(), m.getComposerSort());
                    break;
                case "file2":
                    assertEquals(c.getName(), m.getArtist());
                    assertNotEquals(c.getSort(), m.getArtistSort());
                    assertEquals(c.getName(), m.getAlbumArtist());
                    assertEquals(c.getSort(), m.getAlbumArtistSort());
                    break;
                case "file3":
                    assertEquals(c.getName(), m.getAlbumArtist());
                    assertEquals(c.getSort(), m.getAlbumArtistSort());
                    assertEquals(c.getName(), m.getComposer());
                    assertNotEquals(c.getSort(), m.getComposerSort());
                    break;
                case "file4":
                    assertEquals(c.getName(), m.getComposer());
                    assertNotEquals(c.getSort(), m.getComposerSort());
                    break;
                case "file5":
                    assertEquals(c.getName(), m.getComposer());
                    assertNull(m.getComposerSort());
                    break;
                case "file6":
                    assertEquals(c.getName(), m.getArtist());
                    assertNotEquals(c.getSort(), m.getArtistSort());
                    break;
                case "file8":
                    assertEquals(c.getName(), m.getComposer());
                    assertNotEquals(c.getSort(), m.getComposerSort());
                    break;
                case "file11":
                    assertEquals(c.getName(), m.getComposer());
                    assertNull(m.getComposerSort());
                    break;
                case "file12":
                    assertEquals(c.getName(), m.getAlbumArtist());
                    assertNull(m.getAlbumArtistSort());
                    break;
                case "file13":
                    assertEquals(c.getName(), m.getArtist());
                    assertNotEquals(c.getSort(), m.getArtistSort());
                    break;
                case "file14":
                    assertEquals(c.getName(), m.getArtist());
                    assertNull(m.getArtistSort());
                    break;
                case "file15":
                    assertEquals(c.getName(), m.getAlbumArtist());
                    assertNotEquals(c.getSort(), m.getAlbumArtistSort());
                    break;
                case "file16":
                    assertEquals(c.getName(), m.getArtist());
                    assertNotEquals(c.getSort(), m.getArtistSort());
                    break;
                case "file17":
                    assertEquals(c.getName(), m.getArtist());
                    assertNull(m.getArtistSort());
                    assertEquals(c.getName(), m.getComposer());
                    assertEquals(c.getSort(), m.getComposerSort());
                    break;
                case "file18":
                    assertEquals(c.getName(), m.getArtist());
                    assertNotEquals(c.getSort(), m.getArtistSort());
                    break;
                case "case10":
                    assertEquals(c.getName(), m.getArtist());
                    assertNotEquals(c.getSort(), m.getArtistSort());
                    break;
                case "case11":
                    assertEquals(c.getName(), m.getArtist());
                    assertNotEquals(c.getSort(), m.getArtistSort());
                    break;
                case "ALBUM5":
                    assertEquals(c.getName(), m.getArtist());
                    assertNull(m.getArtistSort());
                    break;
                case "ALBUM6":
                    assertEquals(c.getName(), m.getArtist());
                    assertNull(m.getArtistSort());
                    break;
                case "ALBUM8":
                    assertEquals(c.getName(), m.getArtist());
                    assertNull(m.getArtistSort());
                    break;
                case "ALBUM9":
                    assertEquals(c.getName(), m.getArtist());
                    assertNull(m.getArtistSort());
                    break;
                case "ALBUM11":
                    assertEquals(c.getName(), m.getArtist());
                    assertNull(m.getArtistSort());
                    break;

                default:
                    Assertions.fail();
                    break;
                }

            });
        });
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testGetToBeFixedSort() {
        assertEquals(0, mediaFileDao.getSortOfArtistToBeFixed(null).size());
        assertEquals(0, mediaFileDao.getSortOfArtistToBeFixed(Collections.emptyList()).size());
        assertEquals(22, mediaFileDao.getSortOfArtistToBeFixed(candidates).size());
        assertEquals(0, albumDao.getSortOfArtistToBeFixed(null).size());
        assertEquals(0, albumDao.getSortOfArtistToBeFixed(Collections.emptyList()).size());
        assertEquals(5, albumDao.getSortOfArtistToBeFixed(candidates).size());
        assertEquals(0, artistDao.getSortOfArtistToBeFixed(null).size());
        assertEquals(0, artistDao.getSortOfArtistToBeFixed(Collections.emptyList()).size());
        assertEquals(5, artistDao.getSortOfArtistToBeFixed(candidates).size());
    }

}
