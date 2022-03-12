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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Documented;
import java.net.URISyntaxException;

import com.tesshu.jpsonic.domain.MediaFile;
import org.jaudiotagger.tag.images.Artwork;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ParserUtilsTest {

    private MediaFile createMediaFile(String resourcePath) throws URISyntaxException {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setPath(MusicParserTest.class.getResource(resourcePath).toURI().toString().replace("file:", ""));
        return mediaFile;
    }

    @Documented
    private @interface GetArtworkDecision {
        @interface Conditions {
            @interface File {
                @interface IsDir {
                }

                @interface IsFile {
                }

                @interface IsNotApplicable {
                }

                @interface IsApplicable {
                }
            }

            @interface AudioFile {
                @interface NotReadable {
                }

                @interface Readable {
                }

                @interface Tag {
                    @interface Null {
                    }

                    @interface NotNull {
                    }

                    @interface IsWavAndNotExistingId3Tag {
                    }

                    @interface IsWavAndExistingId3Tag {
                    }

                    @interface IsNotWav {
                    }

                    @interface APIC {
                        @interface NotExist {
                        }

                        @interface Exist {
                        }
                    }
                }
            }
        }

        @interface Results {
            @interface Null {
            }

            @interface NotNull {
            }
        }
    }

    @Nested
    class GetArtworkTest {

        @GetArtworkDecision.Conditions.File.IsDir
        @GetArtworkDecision.Results.Null
        @Test
        void testc01() throws URISyntaxException {
            Artwork artwork = ParserUtils.getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/testdata"));
            assertNull(artwork);
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsNotApplicable
        @GetArtworkDecision.Results.Null
        @Test
        void testc02() throws URISyntaxException {
            Artwork artwork = ParserUtils
                    .getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/testdata/test.stem.mp4"));
            assertNull(artwork);
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsApplicable
        @GetArtworkDecision.Conditions.AudioFile.NotReadable
        @GetArtworkDecision.Results.Null
        @Test
        void testc03() throws URISyntaxException {
            Artwork artwork = ParserUtils.getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/testdata/testV25.wav"));
            assertNull(artwork);
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsApplicable
        @GetArtworkDecision.Conditions.AudioFile.Readable
        @GetArtworkDecision.Conditions.AudioFile.Tag.Null
        @GetArtworkDecision.Results.Null
        @Test
        void testc04() throws URISyntaxException {
            Artwork artwork = ParserUtils.getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/testdata/01.mp3"));
            assertNull(artwork);
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsApplicable
        @GetArtworkDecision.Conditions.AudioFile.Readable
        @GetArtworkDecision.Conditions.AudioFile.Tag.NotNull
        @GetArtworkDecision.Conditions.AudioFile.Tag.IsWavAndNotExistingId3Tag
        @GetArtworkDecision.Results.Null
        @Test
        void testc05() throws URISyntaxException {
            Artwork artwork = ParserUtils.getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/tagged/mc4pc.wav"));
            assertNull(artwork);
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsApplicable
        @GetArtworkDecision.Conditions.AudioFile.Readable
        @GetArtworkDecision.Conditions.AudioFile.Tag.NotNull
        @GetArtworkDecision.Conditions.AudioFile.Tag.IsWavAndExistingId3Tag
        @GetArtworkDecision.Conditions.AudioFile.Tag.APIC.NotExist
        @GetArtworkDecision.Results.Null
        @Test
        void testc06() throws URISyntaxException {
            Artwork artwork = ParserUtils.getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/tagged/test.wav"));
            assertNull(artwork);
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsApplicable
        @GetArtworkDecision.Conditions.AudioFile.Readable
        @GetArtworkDecision.Conditions.AudioFile.Tag.NotNull
        @GetArtworkDecision.Conditions.AudioFile.Tag.IsWavAndExistingId3Tag
        @GetArtworkDecision.Conditions.AudioFile.Tag.APIC.Exist
        @GetArtworkDecision.Results.NotNull
        @Test
        void testc07() throws URISyntaxException {
            Artwork artwork = ParserUtils
                    .getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/tagged/test-with-coverart.wav"));
            assertNotNull(artwork);
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsApplicable
        @GetArtworkDecision.Conditions.AudioFile.Readable
        @GetArtworkDecision.Conditions.AudioFile.Tag.NotNull
        @GetArtworkDecision.Conditions.AudioFile.Tag.IsNotWav
        @GetArtworkDecision.Conditions.AudioFile.Tag.APIC.NotExist
        @GetArtworkDecision.Results.Null
        @Test
        void testc08() throws URISyntaxException {
            Artwork artwork = ParserUtils.getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/tagged/01.mp3"));
            assertNull(artwork);
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsApplicable
        @GetArtworkDecision.Conditions.AudioFile.Readable
        @GetArtworkDecision.Conditions.AudioFile.Tag.NotNull
        @GetArtworkDecision.Conditions.AudioFile.Tag.IsNotWav
        @GetArtworkDecision.Conditions.AudioFile.Tag.APIC.Exist
        @GetArtworkDecision.Results.NotNull
        @Test
        void testc09() throws URISyntaxException {
            Artwork artwork = ParserUtils.getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/testdata/test122.dsf"));
            assertNotNull(artwork);
        }
    }

    @Test
    void testParseDoubleToInt() {
        assertEquals(12, ParserUtils.parseDoubleToInt("12"));
        assertEquals(12, ParserUtils.parseDoubleToInt("12.34"));
        assertNull(ParserUtils.parseDoubleToInt("12L"));
    }

}
