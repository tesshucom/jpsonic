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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.lang.annotation.Documented;
import java.net.URISyntaxException;
import java.util.Optional;

import com.tesshu.jpsonic.domain.MediaFile;
import org.jaudiotagger.tag.images.Artwork;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.TooManyStaticImports")
class ParserUtilsTest {

    private File createFile(String resourcePath) throws URISyntaxException {
        return new File(MusicParserTest.class.getResource(resourcePath).toURI());
    }

    private MediaFile createMediaFile(String resourcePath) throws URISyntaxException {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setPath(createFile(resourcePath).toString().replace("file:", ""));
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

    @Test
    void isArtworkApplicableTest() throws URISyntaxException {

        assertFalse(ParserUtils.isArtworkApplicable(createFile("/MEDIAS/Metadata/tagger3/testdata")));

        // (Data formats for which no test resources currently exist are omitted)
        assertTrue(ParserUtils.isArtworkApplicable(createFile("/MEDIAS/Metadata/tagger3/testdata/test.ogg")));
        // oga
        assertTrue(ParserUtils.isArtworkApplicable(createFile("/MEDIAS/Metadata/tagger3/testdata/test.flac")));
        assertTrue(ParserUtils.isArtworkApplicable(createFile("/MEDIAS/Metadata/tagger3/testdata/01.mp3")));
        assertTrue(ParserUtils.isArtworkApplicable(createFile("/MEDIAS/Metadata/tagger3/testdata/test.m4a")));
        // m4b
        assertTrue(ParserUtils.isArtworkApplicable(createFile("/MEDIAS/Metadata/tagger3/testdata/test123.wav")));
        assertTrue(ParserUtils.isArtworkApplicable(createFile("/MEDIAS/Metadata/tagger3/testdata/test1.wma")));
        assertTrue(ParserUtils.isArtworkApplicable(createFile("/MEDIAS/Metadata/tagger3/testdata/test138.aiff")));
        // aifc
        // aiff
        assertTrue(ParserUtils.isArtworkApplicable(createFile("/MEDIAS/Metadata/tagger3/testdata/test122.dsf")));

        assertFalse(ParserUtils.isArtworkApplicable(createFile("/MEDIAS/Metadata/tagger3/dummy/empty.opus")));
    }

    @Nested
    class GetArtworkTest {

        @GetArtworkDecision.Conditions.File.IsDir
        @GetArtworkDecision.Results.Null
        @Test
        void testc01() throws URISyntaxException {
            Optional<Artwork> artwork = ParserUtils.getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/testdata"));
            assertTrue(artwork.isEmpty());
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsNotApplicable
        @GetArtworkDecision.Results.Null
        @Test
        void testc02() throws URISyntaxException {
            Optional<Artwork> artwork = ParserUtils
                    .getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/testdata/test.stem.mp4"));
            assertTrue(artwork.isEmpty());
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsApplicable
        @GetArtworkDecision.Conditions.AudioFile.NotReadable
        @GetArtworkDecision.Results.Null
        @Test
        void testc03() throws URISyntaxException {
            Optional<Artwork> artwork = ParserUtils
                    .getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/testdata/testV25.wav"));
            assertTrue(artwork.isEmpty());
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsApplicable
        @GetArtworkDecision.Conditions.AudioFile.Readable
        @GetArtworkDecision.Conditions.AudioFile.Tag.Null
        @GetArtworkDecision.Results.Null
        @Test
        void testc04() throws URISyntaxException {
            Optional<Artwork> artwork = ParserUtils
                    .getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/testdata/01.mp3"));
            assertTrue(artwork.isEmpty());
        }

        @GetArtworkDecision.Conditions.File.IsFile
        @GetArtworkDecision.Conditions.File.IsApplicable
        @GetArtworkDecision.Conditions.AudioFile.Readable
        @GetArtworkDecision.Conditions.AudioFile.Tag.NotNull
        @GetArtworkDecision.Conditions.AudioFile.Tag.IsWavAndNotExistingId3Tag
        @GetArtworkDecision.Results.Null
        @Test
        void testc05() throws URISyntaxException {
            Optional<Artwork> artwork = ParserUtils
                    .getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/tagged/mc4pc.wav"));
            assertTrue(artwork.isEmpty());
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
            Optional<Artwork> artwork = ParserUtils
                    .getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/tagged/test.wav"));
            assertTrue(artwork.isEmpty());
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
            Optional<Artwork> artwork = ParserUtils
                    .getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/tagged/test-with-coverart.wav"));
            assertFalse(artwork.isEmpty());
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
            Optional<Artwork> artwork = ParserUtils
                    .getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/tagged/01.mp3"));
            assertTrue(artwork.isEmpty());
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
            Optional<Artwork> artwork = ParserUtils
                    .getArtwork(createMediaFile("/MEDIAS/Metadata/tagger3/testdata/test122.dsf"));
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
