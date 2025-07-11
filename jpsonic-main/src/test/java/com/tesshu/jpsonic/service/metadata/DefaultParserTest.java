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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import com.tesshu.jpsonic.service.MusicFolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultParserTest {

    private DefaultParser defaultParser;

    @BeforeEach
    void setUp() {
        defaultParser = new DefaultParser(mock(MusicFolderService.class));
    }

    @Test
    void testGetRawMetaData() {
        Path path = Path
            .of("/MEDIAS/Music/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg Variations, Canons [Disc 1]/01 - Bach- Goldberg Variations, BWV 988 - Aria.flac");
        MetaData metaData = defaultParser.getRawMetaData(path);
        assertEquals("Music", metaData.getArtist());
        assertEquals("Music", metaData.getAlbumArtist());
        assertEquals(
                "_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg Variations, Canons [Disc 1]",
                metaData.getAlbumName());
        assertEquals("01 - Bach- Goldberg Variations, BWV 988 - Aria", metaData.getTitle());
    }
}
