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

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MusicIndexServiceUtilsTest {

    private SettingsService settingsService;
    private MusicIndexServiceUtils musicIndexServiceUtils;

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        MediaFileService mediaFileService = mock(MediaFileService.class);
        JapaneseReadingUtils readingUtils = new JapaneseReadingUtils(settingsService);
        JpsonicComparators comparators = new JpsonicComparators(settingsService, readingUtils);
        musicIndexServiceUtils = new MusicIndexServiceUtils(settingsService, mediaFileService, readingUtils,
                comparators);
    }

    /*
     * #852. https://wiki.sei.cmu.edu/confluence/display/java/STR02-J.+Specify+an+appropriate+locale+when+
     * comparing+locale-dependent+data
     */
    @Test
    void testCreateSortableNameSTR02J() {
        Mockito.when(settingsService.getLocale()).thenReturn(Locale.ENGLISH);

        assertEquals("\u0130 abcde", // İ
                musicIndexServiceUtils.createSortableName("\u0130 abcde", // İ
                        "\u0069", // i
                        "\u0131")); // ı

        assertEquals("abcde, \u0130", // İ
                musicIndexServiceUtils.createSortableName("\u0130 abcde", // İ
                        "\u0130", // İ
                        "\u0049")); // I

        assertEquals("abcde, \u0069", // i
                musicIndexServiceUtils.createSortableName("\u0049 abcde", // I
                        "\u0069", // i
                        "\u0131")); // ı

        assertEquals("abcde, \u0049", // I
                musicIndexServiceUtils.createSortableName("\u0049 abcde", // I
                        "\u0130", // İ
                        "\u0049")); // I
    }
}
