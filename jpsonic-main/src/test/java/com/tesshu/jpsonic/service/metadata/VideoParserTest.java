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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.service.metadata;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;

import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class VideoParserTest {

    private VideoParser parser;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class);
        Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(Arrays.asList("mp4"));
        parser = new VideoParser(settingsService, mock(MusicFolderService.class), mock(FFprobe.class));
    }

    @Test
    void testisApplicable() {
        assertThrows(IllegalArgumentException.class, () -> parser.isApplicable(null));
        assertThrows(IllegalArgumentException.class, () -> parser.isApplicable(Path.of("/")));
        assertFalse(parser.isApplicable(Path.of("/album")));
        assertFalse(parser.isApplicable(Path.of("")));
        assertTrue(parser.isApplicable(Path.of("movie.mp4")));
    }
}
