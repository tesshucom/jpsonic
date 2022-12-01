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
import static com.tesshu.jpsonic.util.PlayerUtils.FAR_PAST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Documented;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.service.metadata.MetaData;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.service.metadata.MusicParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@SuppressWarnings("PMD.TooManyStaticImports")
class MediaFileServiceTest {

    private SettingsService settingsService;
    private SecurityService securityService;
    private MediaFileDao mediaFileDao;
    private MetaDataParserFactory metaDataParserFactory;
    private MediaFileService mediaFileService;
    private Path dir;

    @BeforeEach
    public void setup() throws URISyntaxException {
        settingsService = mock(SettingsService.class);
        securityService = mock(SecurityService.class);
        mediaFileDao = mock(MediaFileDao.class);
        metaDataParserFactory = mock(MetaDataParserFactory.class);
        mediaFileService = new MediaFileService(settingsService, mock(MusicFolderService.class), securityService,
                mock(MediaFileCache.class), mediaFileDao, metaDataParserFactory, mock(MediaFileServiceUtils.class));
        dir = Path.of(MediaFileServiceTest.class.getResource("/MEDIAS/Music").toURI());

        Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);
        Mockito.when(settingsService.getMusicFileTypesAsArray()).thenReturn(new String[] { "mp3" });
        Mockito.when(securityService.isReadAllowed(Mockito.any(Path.class))).thenReturn(true);
    }

    private Path createPath(String path) throws URISyntaxException {
        return Path.of(MediaFileServiceTest.class.getResource(path).toURI());
    }

    @Test
    void testGetLastModified() throws URISyntaxException, IOException {

        // Defaulte (Same as legacy). File modification date.
        Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                .thenReturn(FileModifiedCheckScheme.LAST_MODIFIED.name());
        assertTrue(mediaFileService.isSchemeLastModified());

        Path path = createPath("/MEDIAS/Music2/_DIR_ chrome hoof - 2004/10 telegraph hill.mp3");
        assertEquals(Files.getLastModifiedTime(path).toMillis(), mediaFileService.getLastModified(path));
        // File modification date independent method (scan execution time is used)
        Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
        assertFalse(mediaFileService.isSchemeLastModified());

        /*
         * When requested by a flow other than scanning(Not usually, but when browsing before the first scan, etc.) In
         * this case, the last modified date is used
         */
        assertEquals(Files.getLastModifiedTime(path).toMillis(), mediaFileService.getLastModified(path));

        /*
         * For scan flows in Scheme.LAST_SCANNED, scan execution time is used.
         *
         * @see MediaScannerService
         */
        Instant scanStart = now();
        MediaLibraryStatistics statistics = new MediaLibraryStatistics(scanStart);
        assertEquals(scanStart.toEpochMilli(), mediaFileService.getLastModified(path, statistics));

        /*
         * For scan flows in Scheme.LAST_MODIFIED, , the last modified date is used.
         */
        Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                .thenReturn(FileModifiedCheckScheme.LAST_MODIFIED.name());
        assertTrue(mediaFileService.isSchemeLastModified());
        assertEquals(Files.getLastModifiedTime(path).toMillis(), mediaFileService.getLastModified(path, statistics));
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
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            boolean useFastCache = true;

            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.LtDaoVersion
        @CheckLastModifiedDecision.Result.CreateOrUpdate.True
        @Test
        void c02() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION - 1);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            boolean useFastCache = false;

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
        void c03() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(FAR_PAST);

            boolean useFastCache = false;

            assertEquals(mediaFile.getChanged(), Files.getLastModifiedTime(mediaFile.toPath()).toInstant());
            assertEquals(FAR_PAST, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.clearInvocations(mediaFileDao);

            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().plus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            assertThat("Changed Gt LastModified", mediaFile.getChanged().toEpochMilli(),
                    greaterThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertEquals(FAR_PAST, mediaFile.getLastScanned());
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
        void c04() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(mediaFile.getChanged().plus(1, ChronoUnit.DAYS));

            boolean useFastCache = false;

            assertEquals(mediaFile.getChanged(), Files.getLastModifiedTime(mediaFile.toPath()).toInstant());
            assertNotEquals(FAR_PAST, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));

            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().plus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            assertThat("Changed Gt LastModified", mediaFile.getChanged().toEpochMilli(),
                    greaterThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertNotEquals(FAR_PAST, mediaFile.getLastScanned());
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
        void c05() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().minus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(FAR_PAST);

            boolean useFastCache = false;

            assertThat("Changed Lt LastModified", mediaFile.getChanged().toEpochMilli(),
                    lessThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertEquals(FAR_PAST, mediaFile.getLastScanned());
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
        void c06() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().minus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(mediaFile.getChanged());

            boolean useFastCache = false;

            assertThat("Changed Lt LastModified", mediaFile.getChanged().toEpochMilli(),
                    lessThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertNotEquals(FAR_PAST, mediaFile.getLastScanned());
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
        void c07() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(FAR_PAST);

            boolean useFastCache = false;

            assertEquals(mediaFile.getChanged().toEpochMilli(),
                    Files.getLastModifiedTime(mediaFile.toPath()).toMillis());
            assertEquals(FAR_PAST, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.clearInvocations(mediaFileDao);

            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().plus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            assertThat("Changed Gt LastModified", mediaFile.getChanged().toEpochMilli(),
                    greaterThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertEquals(FAR_PAST, mediaFile.getLastScanned());
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
        void c08() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(mediaFile.getChanged());

            boolean useFastCache = false;

            assertEquals(mediaFile.getChanged().toEpochMilli(),
                    Files.getLastModifiedTime(mediaFile.toPath()).toMillis());
            assertNotEquals(FAR_PAST, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));

            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().plus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            assertThat("Changed Gt LastModified", mediaFile.getChanged().toEpochMilli(),
                    greaterThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertNotEquals(FAR_PAST, mediaFile.getLastScanned());
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
        void c09() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().minus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(FAR_PAST);

            boolean useFastCache = false;

            assertThat("Changed Lt LastModified", mediaFile.getChanged().toEpochMilli(),
                    lessThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertEquals(FAR_PAST, mediaFile.getLastScanned());
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
        void c10() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().minus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(mediaFile.getChanged());

            boolean useFastCache = false;

            assertThat("Changed Lt LastModified", mediaFile.getChanged().toEpochMilli(),
                    lessThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertNotEquals(FAR_PAST, mediaFile.getLastScanned());
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
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(FAR_PAST);

            boolean useFastCache = false;

            assertEquals(FAR_PAST, mediaFile.getLastScanned());
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
        void c12() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(mediaFile.getChanged());

            boolean useFastCache = false;

            assertEquals(mediaFile.getChanged().toEpochMilli(),
                    Files.getLastModifiedTime(mediaFile.toPath()).toMillis());
            assertNotEquals(FAR_PAST, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.clearInvocations(mediaFileDao);

            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().plus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            assertThat("Changed Gt LastModified", mediaFile.getChanged().toEpochMilli(),
                    greaterThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertNotEquals(FAR_PAST, mediaFile.getLastScanned());
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
        void c13() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().minus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(FAR_PAST);

            boolean useFastCache = false;

            assertThat("Changed Lt LastModified", mediaFile.getChanged().toEpochMilli(),
                    lessThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertEquals(FAR_PAST, mediaFile.getLastScanned());
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
        void c14() throws ExecutionException, IOException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            MediaFile mediaFile = createMediaFile(MediaFileDao.VERSION);
            mediaFile.setPathString(dir.toString());
            try {
                mediaFile.setChanged(Files.getLastModifiedTime(dir).toInstant().minus(1, ChronoUnit.DAYS));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mediaFile.setLastScanned(mediaFile.getChanged());

            boolean useFastCache = false;

            assertThat("Changed Lt LastModified", mediaFile.getChanged().toEpochMilli(),
                    lessThan(Files.getLastModifiedTime(mediaFile.toPath()).toMillis()));
            assertNotEquals(FAR_PAST, mediaFile.getLastScanned());
            assertEquals(mediaFile, mediaFileService.checkLastModified(mediaFile, useFastCache));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }
    }

    @Nested
    class UpdateChildrenTest {

        @Test
        void testUpdateChildren() throws URISyntaxException {

            assertTrue(mediaFileService.isSchemeLastModified());
            Path dir = createPath("/MEDIAS/Music2/_DIR_ chrome hoof - 2004");
            assertTrue(Files.isDirectory(dir));

            MediaFile album = mediaFileService.createMediaFile(dir);
            assertEquals(FAR_PAST, album.getChildrenLastUpdated());
            assertThat("Initial value had been assigned to 'changed'.", album.getChanged().toEpochMilli(),
                    greaterThan(album.getChildrenLastUpdated().toEpochMilli()));
            Mockito.clearInvocations(mediaFileDao);

            // Typical case where an update is performed
            mediaFileService.updateChildren(album);
            Mockito.verify(mediaFileDao, Mockito.times(1)).getChildrenOf(Mockito.anyString());
            Mockito.verify(mediaFileDao, Mockito.times(3)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.verify(mediaFileDao, Mockito.never()).deleteMediaFile(Mockito.anyString());
            Mockito.clearInvocations(mediaFileDao);

            // Typical case where an update isn't performed
            album.setChildrenLastUpdated(now()); // If it has already been executed, etc.
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

            album.setChildrenLastUpdated(FAR_PAST);
            mediaFileService.updateChildren(album);
            Mockito.verify(mediaFileDao, Mockito.times(1)).getChildrenOf(Mockito.anyString());
            Mockito.verify(mediaFileDao, Mockito.times(3)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
            Mockito.verify(mediaFileDao, Mockito.never()).deleteMediaFile(Mockito.anyString());
        }
    }

    @Nested
    class CreateMediaFileTest {

        @Test
        void testCreateMediaFile() throws URISyntaxException, IOException {
            Path path = createPath("/MEDIAS/Music2/_DIR_ chrome hoof - 2004/10 telegraph hill.mp3");
            assertFalse(Files.isDirectory(path));
            assertTrue(mediaFileService.isSchemeLastModified());

            // Newly created case
            Mockito.when(metaDataParserFactory.getParser(path)).thenReturn(null);
            Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);

            MediaFile mediaFile = mediaFileService.createMediaFile(path);
            assertEquals(Files.getLastModifiedTime(mediaFile.toPath()).toMillis(),
                    mediaFile.getChanged().toEpochMilli());
            assertEquals(Files.getLastModifiedTime(mediaFile.toPath()).toMillis(),
                    mediaFile.getCreated().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getLastScanned().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getChildrenLastUpdated().toEpochMilli());
            assertEquals(0, mediaFile.getPlayCount());
            assertNull(mediaFile.getLastPlayed());
            assertNull(mediaFile.getComment());

            // Update case
            mediaFile.setPlayCount(100);
            Instant lastPlayed = now();
            mediaFile.setLastPlayed(lastPlayed);
            mediaFile.setComment("comment");
            Mockito.when(mediaFileDao.getMediaFile(path.toString())).thenReturn(mediaFile);
            mediaFile = mediaFileService.createMediaFile(path);
            assertEquals(Files.getLastModifiedTime(mediaFile.toPath()).toMillis(),
                    mediaFile.getChanged().toEpochMilli());
            assertEquals(Files.getLastModifiedTime(mediaFile.toPath()).toMillis(),
                    mediaFile.getCreated().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getLastScanned().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getChildrenLastUpdated().toEpochMilli());
            assertEquals(100, mediaFile.getPlayCount());
            assertEquals(lastPlayed.toEpochMilli(), mediaFile.getLastPlayed().toEpochMilli());
            assertEquals("comment", mediaFile.getComment());

            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_SCANNED.name());
            assertFalse(mediaFileService.isSchemeLastModified());

            mediaFile = mediaFileService.createMediaFile(path);
            assertEquals(Files.getLastModifiedTime(mediaFile.toPath()).toMillis(),
                    mediaFile.getChanged().toEpochMilli());
            assertEquals(Files.getLastModifiedTime(mediaFile.toPath()).toMillis(),
                    mediaFile.getCreated().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getLastScanned().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getChildrenLastUpdated().toEpochMilli());

            // With statistics
            Instant now = now();
            MediaLibraryStatistics statistics = new MediaLibraryStatistics(now);
            mediaFile = mediaFileService.createMediaFile(path, statistics);
            assertEquals(now.toEpochMilli(), mediaFile.getChanged().toEpochMilli());
            assertEquals(now.toEpochMilli(), mediaFile.getCreated().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getLastScanned().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getChildrenLastUpdated().toEpochMilli());

            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_MODIFIED.name());
            assertTrue(mediaFileService.isSchemeLastModified());
            assertEquals(Files.getLastModifiedTime(mediaFile.toPath()).toMillis(),
                    mediaFileService.getLastModified(path, statistics));
        }

        @Test
        void testApplyFile() throws URISyntaxException {
            Path path = createPath("/MEDIAS/Music2/_DIR_ chrome hoof - 2004/10 telegraph hill.mp3");
            assertFalse(Files.isDirectory(path));
            assertTrue(mediaFileService.isSchemeLastModified());
            final Instant scanStart = now();
            final MediaLibraryStatistics statistics = new MediaLibraryStatistics(scanStart);

            // Newly created case
            MusicParser musicParser = new MusicParser(mock(SettingsService.class), null);
            Mockito.when(metaDataParserFactory.getParser(path)).thenReturn(musicParser);
            Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);

            MediaFile mediaFile = mediaFileService.createMediaFile(path);

            assertThat("Because the parsed time is recorded.", mediaFile.getLastScanned().toEpochMilli(),
                    greaterThan(scanStart.toEpochMilli()));
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getChildrenLastUpdated().toEpochMilli());

            // Update case
            Mockito.when(mediaFileDao.getMediaFile(path.toString())).thenReturn(mediaFile);
            mediaFile = mediaFileService.createMediaFile(path);
            assertThat("Because the parsed time is set.", mediaFile.getLastScanned().toEpochMilli(),
                    greaterThan(scanStart.toEpochMilli()));
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getChildrenLastUpdated().toEpochMilli());

            // With statistics
            mediaFile = mediaFileService.createMediaFile(path, statistics);
            assertEquals(mediaFile.getLastScanned().toEpochMilli(), scanStart.toEpochMilli(),
                    "Because the scanStart-time is set.");
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getChildrenLastUpdated().toEpochMilli());
        }

        @Test
        void testApplyDirWithoutChild() throws URISyntaxException {
            Path path = createPath("/MEDIAS/Music2");
            assertTrue(Files.isDirectory(path));
            assertTrue(mediaFileService.isSchemeLastModified());
            final Instant scanStart = now();
            final MediaLibraryStatistics statistics = new MediaLibraryStatistics(scanStart);

            // Newly created case
            MusicParser musicParser = new MusicParser(null, null);
            Mockito.when(metaDataParserFactory.getParser(path)).thenReturn(musicParser);
            Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);

            MediaFile mediaFile = mediaFileService.createMediaFile(path);
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getLastScanned().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getChildrenLastUpdated().toEpochMilli());

            // Update case
            Mockito.when(mediaFileDao.getMediaFile(path.toString())).thenReturn(mediaFile);
            mediaFile = mediaFileService.createMediaFile(path);
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getLastScanned().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getChildrenLastUpdated().toEpochMilli());

            // With statistics
            mediaFile = mediaFileService.createMediaFile(path, statistics);
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getLastScanned().toEpochMilli());
            assertEquals(FAR_PAST.toEpochMilli(), mediaFile.getChildrenLastUpdated().toEpochMilli());
        }

        @Test
        void testApplyDirWithChild() throws URISyntaxException {
            MusicParser musicParser = mock(MusicParser.class);
            Mockito.when(musicParser.getMetaData(Mockito.any(Path.class))).thenReturn(new MetaData());
            Mockito.when(metaDataParserFactory.getParser(Mockito.any(Path.class))).thenReturn(musicParser);
            Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);
            Mockito.when(settingsService.getMusicFileTypesAsArray()).thenReturn(new String[] { "mp3" });
            Mockito.when(securityService.isReadAllowed(Mockito.any(Path.class))).thenReturn(true);

            Path dir = createPath("/MEDIAS/Music2/_DIR_ chrome hoof - 2004");
            assertTrue(Files.isDirectory(dir));
            assertTrue(mediaFileService.isSchemeLastModified());

            final MediaLibraryStatistics statistics = new MediaLibraryStatistics(now());

            final ArgumentCaptor<String> pathsCaptor = ArgumentCaptor.forClass(String.class);
            final ArgumentCaptor<MediaFile> mediaFileCaptor = ArgumentCaptor.forClass(MediaFile.class);

            mediaFileService.createMediaFile(dir, statistics);

            // Because firstChild is parsed
            Mockito.verify(musicParser, Mockito.times(1)).getMetaData(Mockito.any(Path.class));

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
            Path path = createPath("/MEDIAS/Metadata/coverart/cover.jpg");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/cover.png");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/cover.gif");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/folder.gif");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());

            // extensions
            path = createPath("/MEDIAS/Metadata/coverart/album.gif");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/album.jpeg");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/album.gif");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/album.png");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());

            // letter case
            path = createPath("/MEDIAS/Metadata/coverart/coveratrt.GIF");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());

            // dir
            path = createPath("/MEDIAS/Metadata/coverart/coveratrt.jpg");
            assertTrue(Files.exists(path));
            assertTrue(mediaFileService.findCoverArt(path).isEmpty());
        }

        @Test
        @DisabledOnOs(OS.LINUX)
        void testIsEmbeddedArtworkApplicableOnWin() throws ExecutionException, URISyntaxException {

            Mockito.when(securityService.isReadAllowed(Mockito.any(Path.class))).thenReturn(true);

            // coverArt
            Path parent = createPath("/MEDIAS/Metadata/coverart");
            Path firstChild = createPath("/MEDIAS/Metadata/coverart/album.gif"); // OS dependent
            assertEquals(firstChild, mediaFileService.findCoverArt(parent).get());

            // coverArt
            Path containsEmbeddedFormats = createPath("/MEDIAS/Metadata/v2.4");
            Path embeddedFormat = createPath("/MEDIAS/Metadata/v2.4/Mp3tag2.9.7.mp3");
            assertEquals(embeddedFormat, mediaFileService.findCoverArt(containsEmbeddedFormats).get());

            // empty
            Path containsDirOnly = createPath("/MEDIAS/Metadata/tagger3");
            assertTrue(mediaFileService.findCoverArt(containsDirOnly).isEmpty());
        }

        @SuppressWarnings("PMD.DetachedTestCase")
        // @Test
        // @Disabled
        // @DisabledOnOs(OS.WINDOWS)
        void testIsEmbeddedArtworkApplicableOnLinux() throws ExecutionException, URISyntaxException {

            Mockito.when(securityService.isReadAllowed(Mockito.any(Path.class))).thenReturn(true);

            // coverArt
            Path parent = createPath("/MEDIAS/Metadata/coverart");
            Path firstChild = createPath("/MEDIAS/Metadata/coverart/coveratrt.GIF"); // OS dependent
            assertEquals(firstChild, mediaFileService.findCoverArt(parent).get());

            // coverArt
            Path containsEmbeddedFormats = createPath("/MEDIAS/Metadata/v2.4");
            Path embeddedFormat = createPath("/MEDIAS/Metadata/v2.4/Mp3tag2.9.7.mp3");
            assertEquals(embeddedFormat, mediaFileService.findCoverArt(containsEmbeddedFormats).get());

            // empty
            Path containsDirOnly = createPath("/MEDIAS/Metadata/tagger3");
            assertTrue(mediaFileService.findCoverArt(containsDirOnly).isEmpty());
        }

    }
}
