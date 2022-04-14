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
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.annotation.Documented;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.service.metadata.MetaData;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.service.metadata.MusicParser;
import com.tesshu.jpsonic.util.FileUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@SuppressWarnings("PMD.TooManyStaticImports")
class MediaFileServiceTest {

    private SettingsService settingsService;
    private SecurityService securityService;
    private MediaFileDao mediaFileDao;
    private MetaDataParserFactory metaDataParserFactory;
    private MediaFileService mediaFileService;
    private File dir;

    @BeforeEach
    public void setup() throws URISyntaxException {
        settingsService = mock(SettingsService.class);
        securityService = mock(SecurityService.class);
        mediaFileDao = mock(MediaFileDao.class);
        metaDataParserFactory = mock(MetaDataParserFactory.class);
        mediaFileService = new MediaFileService(settingsService, mock(MusicFolderService.class), securityService,
                mock(MediaFileCache.class), mediaFileDao, mock(AlbumDao.class), metaDataParserFactory,
                mock(MediaFileServiceUtils.class));
        dir = new File(MediaFileServiceTest.class.getResource("/MEDIAS/Music").toURI());
    }

    private File createFile(String path) throws URISyntaxException {
        return new File(MediaFileServiceTest.class.getResource(path).toURI());
    }

    @Test
    void testGetLastModified() throws URISyntaxException {

        // Defaulte (Same as legacy). File modification date.
        Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                .thenReturn(FileModifiedCheckScheme.LAST_MODIFIED.name());
        assertTrue(mediaFileService.isSchemeLastModified());

        File file = createFile("/MEDIAS/Music2/_DIR_ chrome hoof - 2004/10 telegraph hill.mp3");
        assertEquals(file.lastModified(), mediaFileService.getLastModified(file));

        // File modification date independent method (scan execution time is used)
        Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
        assertFalse(mediaFileService.isSchemeLastModified());

        /*
         * When requested by a flow other than scanning(Not usually, but when browsing before the first scan, etc.) In
         * this case, the last modified date is used
         */
        assertEquals(file.lastModified(), mediaFileService.getLastModified(file));

        /*
         * For scan flows in Scheme.LAST_SCANNED, scan execution time is used.
         *
         * @see MediaScannerService
         */
        Date scanStart = DateUtils.truncate(new Date(), Calendar.SECOND);
        MediaLibraryStatistics statistics = new MediaLibraryStatistics(scanStart);
        assertEquals(scanStart.getTime(), mediaFileService.getLastModified(file, statistics));

        /*
         * For scan flows in Scheme.LAST_MODIFIED, , the last modified date is used.
         */
        Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                .thenReturn(FileModifiedCheckScheme.LAST_MODIFIED.name());
        assertTrue(mediaFileService.isSchemeLastModified());
        assertEquals(file.lastModified(), mediaFileService.getLastModified(file, statistics));
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
            mediaFile.setPathString(dir.getPath());
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
            mediaFile.setPathString(dir.getPath());
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
            mediaFile.setPathString(dir.getPath());
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
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c04() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            mediaFile.setLastScanned(new Date(mediaFile.getChanged().getTime() + 1));

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
        @CheckLastModifiedDecision.Conditions.Scheme.LastModified.IgnoreFileTimestamps.True
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.LtLastModified
        @CheckLastModifiedDecision.Conditions.MediaFile.LastScanned.EqZeroDate
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c05() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.getPath());
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
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c06() throws ExecutionException {
            final boolean useFastCache = false;
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified() - 1L));
            mediaFile.setLastScanned(mediaFile.getChanged());

            assertThat("Changed Lt LastModified", mediaFile.getChanged().getTime(),
                    lessThan(mediaFile.getFile().lastModified()));
            assertNotEquals(ZERO_DATE, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
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
            mediaFile.setPathString(dir.getPath());
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
            mediaFile.setPathString(dir.getPath());
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
            mediaFile.setPathString(dir.getPath());
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
            mediaFile.setPathString(dir.getPath());
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
            mediaFile.setPathString(dir.getPath());
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
            mediaFile.setPathString(dir.getPath());
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
            mediaFile.setPathString(dir.getPath());
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
            mediaFile.setPathString(dir.getPath());
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
    class UpdateChildrenTest {

        @Test
        void testUpdateChildren() throws URISyntaxException {

            assertTrue(mediaFileService.isSchemeLastModified());

            Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);
            Mockito.when(settingsService.getMusicFileTypesAsArray()).thenReturn(new String[] { "mp3" });
            Mockito.when(securityService.isReadAllowed(Mockito.any(File.class))).thenReturn(true);
            File dir = createFile("/MEDIAS/Music2/_DIR_ chrome hoof - 2004");
            assertTrue(dir.isDirectory());

            MediaFile album = mediaFileService.createMediaFile(dir);
            assertEquals(ZERO_DATE, album.getChildrenLastUpdated());
            assertThat("Initial value had been assigned to 'changed'.", album.getChanged().getTime(),
                    greaterThan(album.getChildrenLastUpdated().getTime()));
            Mockito.clearInvocations(mediaFileDao);

            // Typical case where an update is performed
            mediaFileService.updateChildren(album);
            Mockito.verify(mediaFileDao, Mockito.times(1)).getChildrenOf(Mockito.anyString());
            Mockito.verify(mediaFileDao, Mockito.times(3)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.verify(mediaFileDao, Mockito.never()).deleteMediaFile(Mockito.anyString());
            Mockito.clearInvocations(mediaFileDao);

            // Typical case where an update isn't performed
            album.setChildrenLastUpdated(new Date()); // If it has already been executed, etc.
            mediaFileService.updateChildren(album);
            Mockito.verify(mediaFileDao, Mockito.never()).getChildrenOf(Mockito.anyString());
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.verify(mediaFileDao, Mockito.never()).deleteMediaFile(Mockito.anyString());

            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            assertFalse(mediaFileService.isSchemeLastModified());

            /*
             * If Scheme is set to Last Scaned, Only updated if childrenLastUpdated is zero (zero = initial value or
             * immediately after reset)
             */
            mediaFileService.updateChildren(album);
            Mockito.verify(mediaFileDao, Mockito.never()).getChildrenOf(Mockito.anyString());
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.verify(mediaFileDao, Mockito.never()).deleteMediaFile(Mockito.anyString());

            album.setChildrenLastUpdated(ZERO_DATE);
            mediaFileService.updateChildren(album);
            Mockito.verify(mediaFileDao, Mockito.times(1)).getChildrenOf(Mockito.anyString());
            Mockito.verify(mediaFileDao, Mockito.times(3)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.verify(mediaFileDao, Mockito.never()).deleteMediaFile(Mockito.anyString());
        }
    }

    @Nested
    class CreateMediaFileTest {

        @Test
        void testCreateMediaFile() throws URISyntaxException {
            File file = createFile("/MEDIAS/Music2/_DIR_ chrome hoof - 2004/10 telegraph hill.mp3");
            assertTrue(file.isFile());
            assertTrue(mediaFileService.isSchemeLastModified());

            // Newly created case
            Mockito.when(metaDataParserFactory.getParser(file)).thenReturn(null);
            Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);

            MediaFile mediaFile = mediaFileService.createMediaFile(file);
            assertEquals(file.lastModified(), mediaFile.getChanged().getTime());
            assertEquals(file.lastModified(), mediaFile.getCreated().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getLastScanned().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getChildrenLastUpdated().getTime());
            assertEquals(0, mediaFile.getPlayCount());
            assertNull(mediaFile.getLastPlayed());
            assertNull(mediaFile.getComment());

            // Update case
            mediaFile.setPlayCount(100);
            Date lastPlayed = new Date();
            mediaFile.setLastPlayed(lastPlayed);
            mediaFile.setComment("comment");
            Mockito.when(mediaFileDao.getMediaFile(file.getPath())).thenReturn(mediaFile);
            mediaFile = mediaFileService.createMediaFile(file);
            assertEquals(file.lastModified(), mediaFile.getChanged().getTime());
            assertEquals(file.lastModified(), mediaFile.getCreated().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getLastScanned().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getChildrenLastUpdated().getTime());
            assertEquals(100, mediaFile.getPlayCount());
            assertEquals(lastPlayed.getTime(), mediaFile.getLastPlayed().getTime());
            assertEquals("comment", mediaFile.getComment());

            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            assertFalse(mediaFileService.isSchemeLastModified());

            mediaFile = mediaFileService.createMediaFile(file);
            assertEquals(file.lastModified(), mediaFile.getChanged().getTime());
            assertEquals(file.lastModified(), mediaFile.getCreated().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getLastScanned().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getChildrenLastUpdated().getTime());

            // With statistics
            Date now = DateUtils.truncate(new Date(), Calendar.SECOND);
            MediaLibraryStatistics statistics = new MediaLibraryStatistics(now);
            mediaFile = mediaFileService.createMediaFile(file, statistics);
            assertEquals(now.getTime(), mediaFile.getChanged().getTime());
            assertEquals(now.getTime(), mediaFile.getCreated().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getLastScanned().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getChildrenLastUpdated().getTime());

            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_MODIFIED.name());
            assertTrue(mediaFileService.isSchemeLastModified());
            assertEquals(file.lastModified(), mediaFileService.getLastModified(file, statistics));
        }

        @Test
        void testApplyFile() throws URISyntaxException {
            File file = createFile("/MEDIAS/Music2/_DIR_ chrome hoof - 2004/10 telegraph hill.mp3");
            assertTrue(file.isFile());
            assertTrue(mediaFileService.isSchemeLastModified());
            final Date scanStart = DateUtils.truncate(new Date(), Calendar.SECOND);
            final MediaLibraryStatistics statistics = new MediaLibraryStatistics(scanStart);

            // Newly created case
            MusicParser musicParser = new MusicParser(null);
            Mockito.when(metaDataParserFactory.getParser(file)).thenReturn(musicParser);
            Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);

            MediaFile mediaFile = mediaFileService.createMediaFile(file);

            assertThat("Because the parsed time is recorded.", mediaFile.getLastScanned().getTime(),
                    greaterThan(scanStart.getTime()));
            assertEquals(ZERO_DATE.getTime(), mediaFile.getChildrenLastUpdated().getTime());

            // Update case
            Mockito.when(mediaFileDao.getMediaFile(file.getPath())).thenReturn(mediaFile);
            mediaFile = mediaFileService.createMediaFile(file);
            assertThat("Because the parsed time is set.", mediaFile.getLastScanned().getTime(),
                    greaterThan(scanStart.getTime()));
            assertEquals(ZERO_DATE.getTime(), mediaFile.getChildrenLastUpdated().getTime());

            // With statistics
            mediaFile = mediaFileService.createMediaFile(file, statistics);
            assertEquals(mediaFile.getLastScanned().getTime(), scanStart.getTime(),
                    "Because the scanStart-time is set.");
            assertEquals(ZERO_DATE.getTime(), mediaFile.getChildrenLastUpdated().getTime());
        }

        @Test
        void testApplyDirWithoutChild() throws URISyntaxException {
            File file = createFile("/MEDIAS/Music2");
            assertTrue(file.isDirectory());
            assertTrue(mediaFileService.isSchemeLastModified());
            final Date scanStart = DateUtils.truncate(new Date(), Calendar.SECOND);
            final MediaLibraryStatistics statistics = new MediaLibraryStatistics(scanStart);

            // Newly created case
            MusicParser musicParser = new MusicParser(null);
            Mockito.when(metaDataParserFactory.getParser(file)).thenReturn(musicParser);
            Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);

            MediaFile mediaFile = mediaFileService.createMediaFile(file);
            assertEquals(ZERO_DATE.getTime(), mediaFile.getLastScanned().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getChildrenLastUpdated().getTime());

            // Update case
            Mockito.when(mediaFileDao.getMediaFile(file.getPath())).thenReturn(mediaFile);
            mediaFile = mediaFileService.createMediaFile(file);
            assertEquals(ZERO_DATE.getTime(), mediaFile.getLastScanned().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getChildrenLastUpdated().getTime());

            // With statistics
            mediaFile = mediaFileService.createMediaFile(file, statistics);
            assertEquals(ZERO_DATE.getTime(), mediaFile.getLastScanned().getTime());
            assertEquals(ZERO_DATE.getTime(), mediaFile.getChildrenLastUpdated().getTime());
        }

        @Test
        void testApplyDirWithChild() throws URISyntaxException {
            MusicParser musicParser = mock(MusicParser.class);
            Mockito.when(musicParser.getMetaData(Mockito.any(File.class))).thenReturn(new MetaData());
            Mockito.when(metaDataParserFactory.getParser(Mockito.any(File.class))).thenReturn(musicParser);
            Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);
            Mockito.when(settingsService.getMusicFileTypesAsArray()).thenReturn(new String[] { "mp3" });
            Mockito.when(securityService.isReadAllowed(Mockito.any(File.class))).thenReturn(true);

            File dir = createFile("/MEDIAS/Music2/_DIR_ chrome hoof - 2004");
            assertTrue(dir.isDirectory());
            assertTrue(mediaFileService.isSchemeLastModified());

            final Date scanStart = DateUtils.truncate(new Date(), Calendar.SECOND);
            final MediaLibraryStatistics statistics = new MediaLibraryStatistics(scanStart);

            final ArgumentCaptor<String> pathsCaptor = ArgumentCaptor.forClass(String.class);
            final ArgumentCaptor<MediaFile> mediaFileCaptor = ArgumentCaptor.forClass(MediaFile.class);

            mediaFileService.createMediaFile(dir, statistics);

            // Because firstChild is parsed
            Mockito.verify(musicParser, Mockito.times(1)).getMetaData(Mockito.any(File.class));

            /*
             * Because firstChild is registered. Since firstChild is registered at this time, firstChild will not be
             * parsed when updateChildren is executed.
             */
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(mediaFileCaptor.capture());
            // [Windows] assertEquals("02 eyes like dull hazlenuts", mediaFileCaptor.getValue().getName());
            // [Linux] assertEquals("10 telegraph hill", mediaFileCaptor.getValue().getName());

            // 3times [parent, firstChild(before create), firstChild(after create)]
            Mockito.verify(mediaFileDao, Mockito.times(3)).getMediaFile(pathsCaptor.capture());
        }
    }

    @Nested
    class FindCoverArtTest {

        @Test
        void coverArtFileTypesTest() throws ExecutionException, URISyntaxException {
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
        void testIsEmbeddedArtworkApplicable() throws ExecutionException, URISyntaxException {

            Mockito.when(securityService.isReadAllowed(Mockito.any(File.class))).thenReturn(true);

            Function<File, File> stub = (file) -> {
                MediaFile mediaFile = new MediaFile();
                mediaFile.setPathString(file.getPath());
                Mockito.when(mediaFileDao.getMediaFile(file.getPath())).thenReturn(mediaFile);
                return file;
            };

            // with embedded coverArt
            final File embedded = stub.apply(createFile("/MEDIAS/Metadata/tagger3/tagged/test.flac"));

            // without embedded coverArt
            /*
             * #1453 In Jpsonic, even if the actual file does not have an embedded image, it is considered as a target
             * if the extension matches.
             */
            final File without = stub.apply(createFile("/MEDIAS/Metadata/tagger3/testdata/test.ogg"));

            // File in a format that does not support the acquisition of Artwork
            final File noSupport = stub.apply(createFile("/MEDIAS/Metadata/tagger3/blank/blank.shn"));

            assertEquals(embedded, mediaFileService.findCoverArt(embedded).get());
            assertFalse(mediaFileService.findCoverArt(without).isEmpty());
            assertEquals(without, mediaFileService.findCoverArt(without).get());
            assertTrue(mediaFileService.findCoverArt(noSupport).isEmpty());

            // Of the files in the target format, only the first file is evaluated.
            assertEquals(embedded, mediaFileService.findCoverArt(embedded, without).get());
            assertEquals(embedded, mediaFileService.findCoverArt(noSupport, embedded).get());
            assertFalse(mediaFileService.findCoverArt(without, embedded).isEmpty());
            assertEquals(without, mediaFileService.findCoverArt(without, embedded).get());
        }
    }
}
