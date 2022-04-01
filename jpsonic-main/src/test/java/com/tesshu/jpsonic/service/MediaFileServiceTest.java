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

import static com.tesshu.jpsonic.dao.MediaFileDao.ZERO_DATE;
import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings("PMD.TooManyStaticImports")
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
                mock(MediaFileCache.class), mediaFileDao, mock(AlbumDao.class), mock(MetaDataParserFactory.class),
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

            @interface Scheme {
                @interface LastModified {
                    @interface IgnoreFileTimestamps {
                        @interface True {
                        }

                        @interface False {

                        }
                    }
                }

                @interface LastScanned {

                }

            }

            @interface MediaFile {
                @interface Version {
                    @interface GtEqDaoVersion {
                    }

                    @interface LtDaoVersion {
                    }
                }

                @interface Changed {
                    @interface GtEqLastModified {
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

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    @Nested
    class CheckLastModifiedTest {

        private MediaFile createMediaFile(int version) {
            return new MediaFile() {
                @Override
                public int getVersion() {
                    return version;
                }
            };
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.True
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c01() throws ExecutionException {
            final boolean useFastCache = true;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));

            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.LtDaoVersion
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c02() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION - 1);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));

            assertThat("mediaFile#version Lt MediaFileDao.VERSION", mediaFile.getVersion(),
                    lessThan(MediaFileDao.VERSION));
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified.IgnoreFileTimestamps.True
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.GtEqLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.EqZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c03() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            mediaFile.setLastScanned(ZERO_DATE);

            assertEquals(mediaFile.getChanged().getTime(), mediaFile.getFile().lastModified());
            assertEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.clearInvocations(mediaFileDao);

            mediaFile.setChanged(new Date(dir.lastModified() + 1L));
            assertThat("Changed Gt LastModified", mediaFile.getChanged().getTime(),
                    greaterThan(mediaFile.getFile().lastModified()));
            assertEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified.IgnoreFileTimestamps.True
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.GtEqLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.NeZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c04() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            mediaFile.setLastScanned(new Date(mediaFile.getChanged().getTime() + 1));

            assertEquals(mediaFile.getChanged().getTime(), mediaFile.getFile().lastModified());
            assertNotEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.clearInvocations(mediaFileDao);

            mediaFile.setChanged(new Date(dir.lastModified() + 1L));
            assertThat("Changed Gt LastModified", mediaFile.getChanged().getTime(),
                    greaterThan(mediaFile.getFile().lastModified()));
            assertNotEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified.IgnoreFileTimestamps.True
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.LtLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.EqZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c05() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified() - 1L));
            mediaFile.setLastScanned(ZERO_DATE);

            assertThat("Changed Lt LastModified", mediaFile.getChanged().getTime(),
                    lessThan(mediaFile.getFile().lastModified()));
            assertEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified.IgnoreFileTimestamps.True
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.LtLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.NeZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c06() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified() - 1L));
            mediaFile.setLastScanned(mediaFile.getChanged());

            assertThat("Changed Lt LastModified", mediaFile.getChanged().getTime(),
                    lessThan(mediaFile.getFile().lastModified()));
            assertNotEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.GtEqLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.EqZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c07() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            mediaFile.setLastScanned(ZERO_DATE);

            assertEquals(mediaFile.getChanged().getTime(), mediaFile.getFile().lastModified());
            assertEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.clearInvocations(mediaFileDao);

            mediaFile.setChanged(new Date(dir.lastModified() + 1L));
            assertThat("Changed Gt LastModified", mediaFile.getChanged().getTime(),
                    greaterThan(mediaFile.getFile().lastModified()));
            assertEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.GtEqLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.NeZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c08() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            mediaFile.setLastScanned(mediaFile.getChanged());

            assertEquals(mediaFile.getChanged().getTime(), mediaFile.getFile().lastModified());
            assertNotEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));

            mediaFile.setChanged(new Date(dir.lastModified() + 1L));
            assertThat("Changed Gt LastModified", mediaFile.getChanged().getTime(),
                    greaterThan(mediaFile.getFile().lastModified()));
            assertNotEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));

        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.LtLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.EqZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c09() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified() - 1L));
            mediaFile.setLastScanned(ZERO_DATE);

            assertThat("Changed Lt LastModified", mediaFile.getChanged().getTime(),
                    lessThan(mediaFile.getFile().lastModified()));
            assertEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified.IgnoreFileTimestamps.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.LtLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.NeZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c10() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified() - 1L));
            mediaFile.setLastScanned(mediaFile.getChanged());

            assertThat("Changed Lt LastModified", mediaFile.getChanged().getTime(),
                    lessThan(FileUtil.lastModified(mediaFile.getFile())));
            assertNotEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastScanned
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.GtEqLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.EqZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c11() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            mediaFile.setLastScanned(ZERO_DATE);

            assertEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastScanned
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.GtEqLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.NeZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c12() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            mediaFile.setLastScanned(mediaFile.getChanged());

            assertEquals(mediaFile.getChanged().getTime(), mediaFile.getFile().lastModified());
            assertNotEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.clearInvocations(mediaFileDao);

            mediaFile.setChanged(new Date(dir.lastModified() + 1L));
            assertThat("Changed Gt LastModified", mediaFile.getChanged().getTime(),
                    greaterThan(mediaFile.getFile().lastModified()));
            assertNotEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastScanned
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.LtLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.EqZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c13() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified() - 1L));
            mediaFile.setLastScanned(ZERO_DATE);

            assertThat("Changed Lt LastModified", mediaFile.getChanged().getTime(),
                    lessThan(FileUtil.lastModified(mediaFile.getFile())));
            assertEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.GtEqDaoVersion
        @CheckLastModifiedDecision.Conditions.Scheme.LastScanned
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.LtLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.NeZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c14() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified() - 1L));
            mediaFile.setLastScanned(mediaFile.getChanged());

            assertThat("Changed Lt LastModified", mediaFile.getChanged().getTime(),
                    lessThan(FileUtil.lastModified(mediaFile.getFile())));
            assertNotEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
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
        void testIsEmbeddedArtworkApplicable() throws ExecutionException, URISyntaxException, IOException {

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
