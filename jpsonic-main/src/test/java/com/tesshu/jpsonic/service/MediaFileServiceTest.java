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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.util.FileUtil;
import net.sf.ehcache.Ehcache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MediaFileServiceTest {

    private SettingsService settingsService;
    private SecurityService securityService;
    private MediaFileDao mediaFileDao;
    private MediaFileService mediaFileService;
    private File dir;

    @BeforeEach
    public void setup() throws URISyntaxException {
        settingsService = mock(SettingsService.class);
        securityService = mock(SecurityService.class);
        mediaFileDao = mock(MediaFileDao.class);
        mediaFileService = new MediaFileService(settingsService, mock(MusicFolderService.class), securityService,
                mock(Ehcache.class), mediaFileDao, mock(AlbumDao.class), mock(MetaDataParserFactory.class),
                mock(MediaFileServiceUtils.class));
        dir = new File(MediaFileServiceTest.class.getResource("/MEDIAS/Music").toURI());
    }

    @Documented
    private @interface CheckLastModifiedDecision {

        @interface Conditions {
            @interface UseFastCache {
                @interface True {
                }

                @interface False {
                }
            }

            @interface IgnoreFileTimestamps {
                @interface True {
                }

                @interface False {

                }
            }

            @interface MediaFile {
                @interface Version {
                    @interface GtDaoVersion {
                    }

                    @interface EqDaoVersion {
                    }

                    @interface LtDaoVersion {
                    }
                }

                @interface Changed {
                    @interface GtLastModified {
                    }

                    @interface EqLastModified {
                    }

                    @interface LtLastModified {
                    }

                }

                @interface LastScanned {
                    @interface EqZeroDate {
                    }

                    @interface NeZeroDate {
                    }
                }
            }
        }

        @interface Result {
            @interface CreateOrUpdate {
                @interface True {
                }

                @interface False {
                }
            }
        }

    }

    @Nested
    class CheckLastModifiedWithSchemeOfLastModified {

        @CheckLastModifiedDecision.Conditions.UseFastCache.True
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c01() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, true));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtDaoVersion
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.EqLastModified
        @CheckLastModifiedDecision.Conditions.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c02() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = new MediaFile() {
                @Override
                public int getVersion() {
                    return MediaFileDao.VERSION + 1;
                }
            };
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.EqDaoVersion
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.EqLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.NeZeroDate
        @CheckLastModifiedDecision.Conditions.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c03() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = new MediaFile() {
                @Override
                public int getVersion() {
                    return MediaFileDao.VERSION;
                }
            };
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.LtDaoVersion
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.EqLastModified
        @CheckLastModifiedDecision.Conditions.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c04() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = new MediaFile() {
                @Override
                public int getVersion() {
                    return MediaFileDao.VERSION - 1;
                }
            };
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.atLeastOnce()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.EqDaoVersion
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.EqLastModified
        @CheckLastModifiedDecision.Conditions.IgnoreFileTimestamps.True
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c05() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = new MediaFile() {
                @Override
                public int getVersion() {
                    return MediaFileDao.VERSION;
                }
            };
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.atLeastOnce()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.EqDaoVersion
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.GtLastModified
        @CheckLastModifiedDecision.Conditions.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c06() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = new MediaFile() {
                @Override
                public int getVersion() {
                    return MediaFileDao.VERSION;
                }
            };
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified() + 1_000L));
            assertTrue(mediaFile.getChanged().getTime() > FileUtil.lastModified(mediaFile.getFile()));
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.EqDaoVersion
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.LtLastModified
        @CheckLastModifiedDecision.Conditions.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c07() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = new MediaFile() {
                @Override
                public int getVersion() {
                    return MediaFileDao.VERSION;
                }
            };
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified() - 1_000L));
            assertTrue(mediaFile.getChanged().getTime() < FileUtil.lastModified(mediaFile.getFile()));
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.atLeastOnce()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.EqDaoVersion
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.EqLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.EqZeroDate
        @CheckLastModifiedDecision.Conditions.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c08() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = new MediaFile() {
                @Override
                public int getVersion() {
                    return MediaFileDao.VERSION;
                }
            };
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            mediaFile.setLastScanned(MediaFileDao.ZERO_DATE);
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.atLeastOnce()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }
    }

    /*
     * @see #1101. For HDD rather than SSD, accessing file modification dates can be the biggest bottleneck. And even
     * with SSD, there is the same risk when going through an external network. Furthermore, since the spec of the
     * update date of the directory depends on the OS, the check itself that relies on the update date may not be
     * effective in the first place. Therefore, Jpsonic provides a method for the user to explicitly specify the scan
     * target so that the update date check can be avoided. CheckLastModifiedWithSchemeOfLastModified test cases are
     * based on traditional scanning specifications. On the other hand, the following test cases are extracted only from
     * cases where the expected results differ due to the change in the check method.
     */
    @Nested
    class CheckLastModifiedWithSchemeOfLastScanned {

        /*
         * Because update-date is not checked
         */
        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.EqDaoVersion
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.EqLastModified
        @CheckLastModifiedDecision.Conditions.IgnoreFileTimestamps.True
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c05() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            MediaFile mediaFile = new MediaFile() {
                @Override
                public int getVersion() {
                    return MediaFileDao.VERSION;
                }
            };
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        /*
         * Because update-date is not checked
         */
        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.EqDaoVersion
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.LtLastModified
        @CheckLastModifiedDecision.Conditions.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c07() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            MediaFile mediaFile = new MediaFile() {
                @Override
                public int getVersion() {
                    return MediaFileDao.VERSION;
                }
            };
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified() - 1_000L));
            assertTrue(mediaFile.getChanged().getTime() < FileUtil.lastModified(mediaFile.getFile()));
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }
    }

    @Nested
    class FindCoverArtTest {

        private File createFile(String path) throws URISyntaxException, IOException {
            return new File(MediaFileServiceTest.class.getResource(path).toURI());
        }

        @Test
        void coverArtFileTypesTest() throws ExecutionException, URISyntaxException, IOException {
            // fileNames
            File file = createFile("/MEDIAS/Metadata/coverart/cover.jpg");
            assertEquals(file, mediaFileService.findCoverArt(file).get());
            file = createFile("/MEDIAS/Metadata/coverart/cover.png");
            assertEquals(file, mediaFileService.findCoverArt(file).get());
            file = createFile("/MEDIAS/Metadata/coverart/cover.gif");
            assertEquals(file, mediaFileService.findCoverArt(file).get());
            file = createFile("/MEDIAS/Metadata/coverart/folder.gif");
            assertEquals(file, mediaFileService.findCoverArt(file).get());

            // extensions
            file = createFile("/MEDIAS/Metadata/coverart/album.gif");
            assertEquals(file, mediaFileService.findCoverArt(file).get());
            file = createFile("/MEDIAS/Metadata/coverart/album.jpeg");
            assertEquals(file, mediaFileService.findCoverArt(file).get());
            file = createFile("/MEDIAS/Metadata/coverart/album.gif");
            assertEquals(file, mediaFileService.findCoverArt(file).get());
            file = createFile("/MEDIAS/Metadata/coverart/album.png");
            assertEquals(file, mediaFileService.findCoverArt(file).get());

            // letter case
            file = createFile("/MEDIAS/Metadata/coverart/coveratrt.GIF");
            assertEquals(file, mediaFileService.findCoverArt(file).get());

            // hidden
            file = createFile("/MEDIAS/Metadata/coverart/.hidden");
            assertTrue(file.exists());
            assertTrue(mediaFileService.findCoverArt(file).isEmpty());

            // dir
            file = createFile("/MEDIAS/Metadata/coverart/coveratrt.jpg");
            assertTrue(file.exists());
            assertTrue(mediaFileService.findCoverArt(file).isEmpty());
        }

        @Test
        void artworkApplicableTest() throws ExecutionException, URISyntaxException, IOException {

            Mockito.when(securityService.isReadAllowed(Mockito.any(File.class))).thenReturn(true);
            Mockito.when(settingsService.isFastCacheEnabled()).thenReturn(true);

            Function<File, File> stub = (file) -> {
                MediaFile mediaFile = new MediaFile();
                mediaFile.setPath(file.getPath());
                Mockito.when(mediaFileDao.getMediaFile(file.getPath())).thenReturn(mediaFile);
                return file;
            };

            // with embedded coverArt
            File embedded = stub.apply(createFile("/MEDIAS/Metadata/tagger3/tagged/test.flac"));

            // without embedded coverArt
            File without = stub.apply(createFile("/MEDIAS/Metadata/tagger3/testdata/test.ogg"));

            // File in a format that does not support the acquisition of Artwork
            File noSupport = stub.apply(createFile("/MEDIAS/Metadata/tagger3/blank/blank.shn"));

            assertEquals(embedded, mediaFileService.findCoverArt(embedded).get());
            assertTrue(mediaFileService.findCoverArt(without).isEmpty());
            assertTrue(mediaFileService.findCoverArt(noSupport).isEmpty());

            // Of the files in the target format, only the first file is evaluated.
            assertEquals(embedded, mediaFileService.findCoverArt(embedded, without).get());
            assertEquals(embedded, mediaFileService.findCoverArt(noSupport, embedded).get());
            assertTrue(mediaFileService.findCoverArt(without, embedded).isEmpty());
        }
    }
}
