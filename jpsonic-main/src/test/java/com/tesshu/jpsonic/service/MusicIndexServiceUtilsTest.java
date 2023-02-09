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

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MusicIndexServiceUtilsTest {

    private SettingsService settingsService;
    private MediaFileService mediaFileService;
    private MusicIndexServiceUtils musicIndexServiceUtils;

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        mediaFileService = mock(MediaFileService.class);
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

        assertEquals("abcde, \u0069", // i
                musicIndexServiceUtils.createSortableName(//
                        "\u0130 abcde", // İ
                        Arrays.asList("\u0069", // i
                                "\u0131"))); // ı

        assertEquals("abcde, \u0130", // İ
                musicIndexServiceUtils.createSortableName("\u0130 abcde", // İ
                        Arrays.asList("\u0130", // İ
                                "\u0049"))); // I

        assertEquals("abcde, \u0069", // i
                musicIndexServiceUtils.createSortableName("\u0049 abcde", // I
                        Arrays.asList("\u0069", // i
                                "\u0131"))); // ı

        assertEquals("abcde, \u0130", // İ
                musicIndexServiceUtils.createSortableName("\u0049 abcde", // I
                        Arrays.asList("\u0130", // İ
                                "\u0049"))); // I
    }

    @Test
    void testCreateSortableArtists() throws URISyntaxException {
        Path path = Path.of(MusicIndexServiceTest.class.getResource("/MEDIAS").toURI());
        MusicFolder musicFolder = new MusicFolder(path.toString(), "musicFolder", false, null);
        assertEquals(path, musicFolder.toPath());

        final List<MusicFolder> musicFolders = Arrays.asList(musicFolder);
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(0);
        mediaFile.setPathString(path.toString());

        Mockito.when(mediaFileService.getMediaFile(path)).thenReturn(null);
        musicIndexServiceUtils.createSortableArtists(musicFolders);
        Mockito.verify(mediaFileService, Mockito.never()).getChildrenOf(mediaFile, false, true);

        Mockito.when(mediaFileService.getMediaFile(path)).thenReturn(mediaFile);
        musicIndexServiceUtils.createSortableArtists(musicFolders);
        Mockito.verify(mediaFileService, Mockito.times(1)).getChildrenOf(mediaFile, false, true);
    }
}
