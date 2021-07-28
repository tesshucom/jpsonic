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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.metadata.JaudiotaggerParser;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.util.FileUtil;
import net.sf.ehcache.Ehcache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(NeedsHome.class)
class MediaFileServiceTest {

    @Mock
    private SettingsService settingsService;
    @Mock
    private MusicFolderService musicFolderService;
    @Mock
    private SecurityService securityService;
    @Mock
    private Ehcache mediaFileMemoryCache;
    @Mock
    private MediaFileDao mediaFileDao;
    @Mock
    private AlbumDao albumDao;
    @Mock
    private JaudiotaggerParser parser;
    @Mock
    private MetaDataParserFactory metaDataParserFactory;
    @Mock
    private MediaFileServiceUtils utils;

    private MediaFileService mediaFileService;

    private File dir;

    @BeforeEach
    public void setup() throws URISyntaxException {
        mediaFileService = new MediaFileService(settingsService, musicFolderService, securityService,
                mediaFileMemoryCache, mediaFileDao, albumDao, parser, metaDataParserFactory, utils);
        dir = new File(CheckLastModified.class.getResource("/MEDIAS/Music").toURI());
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
    class CheckLastModified {

        private MediaFile checkLastModified(final MediaFile mediaFile, boolean useFastCache) throws ExecutionException {
            Method method;
            try {
                method = mediaFileService.getClass().getDeclaredMethod("checkLastModified", MediaFile.class,
                        boolean.class);
                method.setAccessible(true);
                return (MediaFile) method.invoke(mediaFileService, mediaFile, useFastCache);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.True
        @CheckLastModifiedDecision.Result.CreateOrUpdate.False
        @Test
        void c01() throws ExecutionException {
            Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(dir.getPath());
            mediaFile.setChanged(new Date(dir.lastModified()));
            assertEquals(mediaFile, checkLastModified(mediaFile, true));
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
            assertEquals(mediaFile, checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.never()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @CheckLastModifiedDecision.Conditions.UseFastCache.False
        @CheckLastModifiedDecision.Conditions.MediaFile.Version.EqDaoVersion
        @CheckLastModifiedDecision.Conditions.MediaFile.Changed.EqLastModified
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
            assertEquals(mediaFile, checkLastModified(mediaFile, false));
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
            assertEquals(mediaFile, checkLastModified(mediaFile, false));
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
            assertEquals(mediaFile, checkLastModified(mediaFile, false));
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
            assertEquals(mediaFile, checkLastModified(mediaFile, false));
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
            assertEquals(mediaFile, checkLastModified(mediaFile, false));
            Mockito.verify(mediaFileDao, Mockito.atLeastOnce()).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }
    }
}
