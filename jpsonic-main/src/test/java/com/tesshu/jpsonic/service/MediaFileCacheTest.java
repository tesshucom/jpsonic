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

package com.tesshu.jpsonic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.tesshu.jpsonic.domain.MediaFile;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.TooManyStaticImports")
public class MediaFileCacheTest {

    private Ehcache cache;
    private MediaFileCache mediaFileCache;
    private Path path;
    private MediaFile mediaFile;

    @BeforeEach
    void setUp() {
        cache = mock(Ehcache.class);
        mediaFileCache = new MediaFileCache(cache);
        path = Paths.get("test.mp3");
        mediaFile = new MediaFile();
    }

    @Nested
    class PutTest {

        @Test
        void testPutDoesNothingWhenDisabled() {
            mediaFileCache.setEnabled(false);

            mediaFileCache.put(path, mediaFile);

            verify(cache, never()).put(any());
        }

        @Test
        void testPutDoesNothingWhenCacheNotAlive() {
            mediaFileCache.setEnabled(true);
            when(cache.getStatus()).thenReturn(Status.STATUS_SHUTDOWN);

            mediaFileCache.put(path, mediaFile);

            verify(cache, never()).put(any());
        }

        @Test
        void testPutCallsCachePutWhenEnabledAndAlive() {
            mediaFileCache.setEnabled(true);
            when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);

            mediaFileCache.put(path, mediaFile);

            verify(cache)
                .put(argThat(element -> path.equals(element.getObjectKey())
                        && mediaFile.equals(element.getObjectValue())));
        }
    }

    @Nested
    class GetTest {

        @Test
        void testGetReturnsNullWhenDisabled() {
            mediaFileCache.setEnabled(false);

            assertNull(mediaFileCache.get(path));
        }

        @Test
        void testGetReturnsNullWhenCacheNotAlive() {
            mediaFileCache.setEnabled(true);
            when(cache.getStatus()).thenReturn(Status.STATUS_SHUTDOWN);

            assertNull(mediaFileCache.get(path));

            verify(cache).getStatus();
        }

        @Test
        void testGetReturnsNullWhenElementIsNull() {
            mediaFileCache.setEnabled(true);
            when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);
            when(cache.get(path)).thenReturn(null);

            assertNull(mediaFileCache.get(path));

            verify(cache).get(path);
        }

        @Test
        void testGetReturnsMediaFileWhenElementExists() {
            mediaFileCache.setEnabled(true);
            when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);
            when(cache.get(path)).thenReturn(new Element(path, mediaFile));

            MediaFile result = mediaFileCache.get(path);

            assertNotNull(result);
            assertEquals(mediaFile, result);
            verify(cache).get(path);
        }
    }

    @Nested
    class RemoveAllTest {

        @Test
        void testRemoveAllCallsCacheWhenAlive() {
            when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);

            mediaFileCache.removeAll();

            verify(cache).removeAll();
        }

        @Test
        void testRemoveAllDoesNothingWhenNotAlive() {
            when(cache.getStatus()).thenReturn(Status.STATUS_SHUTDOWN);

            mediaFileCache.removeAll();

            verify(cache, never()).removeAll();
        }
    }

    @Nested
    class RemoveTest {

        @Test
        void testRemoveReturnsTrueWhenAliveAndCacheReturnsTrue() {
            when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);
            when(cache.remove(path)).thenReturn(true);

            boolean result = mediaFileCache.remove(path);

            assertTrue(result);
            verify(cache).remove(path);
        }

        @Test
        void testRemoveReturnsFalseWhenAliveAndCacheReturnsFalse() {
            when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);
            when(cache.remove(path)).thenReturn(false);

            boolean result = mediaFileCache.remove(path);

            assertFalse(result);
            verify(cache).remove(path);
        }

        @Test
        void testRemoveReturnsFalseWhenNotAlive() {
            when(cache.getStatus()).thenReturn(Status.STATUS_SHUTDOWN);

            boolean result = mediaFileCache.remove(path);

            assertFalse(result);
            verify(cache, never()).remove(any());
        }
    }
}