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

package com.tesshu.jpsonic.ajax;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.service.metadata.MusicParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class TagServiceTest {

    private MetaDataParserFactory metaDataParserFactory;
    private MediaFileService mediaFileService;
    private TagService tagService;
    private MusicParser parser;

    @BeforeEach
    public void setup() {
        metaDataParserFactory = mock(MetaDataParserFactory.class);
        mediaFileService = mock(MediaFileService.class);
        tagService = new TagService(metaDataParserFactory, mediaFileService);
        parser = new MusicParser(mock(MusicFolderService.class));
    }

    @Test
    void testUpdateTags(@TempDir Path tempDir) throws URISyntaxException, IOException {
        Path org = Path
                .of(TagServiceTest.class.getResource("/MEDIAS/Metadata/v2.3+v1.1/MusicCenter2.1.0JP.mp3").toURI());
        Path copy = Path.of(tempDir.toString(), org.getFileName().toString());

        try (OutputStream out = Files.newOutputStream(copy);) {
            Files.copy(org, out);
        }

        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(0);
        mediaFile.setPathString(copy.toString());
        mediaFile.setParentPathString(copy.getParent().toString());
        MediaFile parent = new MediaFile();
        parent.setId(1);
        parent.setPathString(copy.getParent().toString());

        Mockito.when(mediaFileService.getMediaFile(mediaFile.getId())).thenReturn(mediaFile);
        Mockito.when(mediaFileService.getParentOf(mediaFile)).thenReturn(parent);
        Mockito.when(metaDataParserFactory.getParser(Path.of(mediaFile.getPathString()))).thenReturn(parser);

        String result = tagService.updateTags(0, "1", "artist", "album", "title", "2022", "genre");
        assertEquals("UPDATED", result);
        Mockito.verify(mediaFileService, Mockito.times(1)).refreshMediaFile(mediaFile);
        Mockito.verify(mediaFileService, Mockito.times(1)).refreshMediaFile(parent);
    }
}
