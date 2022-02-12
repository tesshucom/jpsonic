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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Function;

import org.apache.commons.io.FilenameUtils;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
class Jaudiotagger3ParserTest {

    private Jaudiotagger3Parser parser = new Jaudiotagger3Parser(null);

    private static File createFile(String resourcePath) throws URISyntaxException {
        return new File(Jaudiotagger3ParserTest.class.getResource(resourcePath).toURI());
    }

    @Nested
    class GetRawMetaDataTest {

        /*
         * Diverted Jaudiotagger test data (/MEDIAS/Metadata/tagger3/testdata). Case omitted to avoid size bloat.
         */
        @Nested
        class SupportedFormatsTest {

            @Nested
            class CannotReadTest {

                @Test
                void testNoAudioHeader() throws URISyntaxException {
                    assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/corrupt.mp3")));
                    assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/Issue79.mp3")));
                    assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/Issue81.mp3")));
                }

                @Test
                void testNoOggSHeader() throws URISyntaxException {
                    assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test36.ogg")));
                }

                @Test
                void testInvalidOggHeader() throws URISyntaxException {
                    assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test508.ogg")));
                }

                @Test
                void testInvalidRIFFHeader() throws URISyntaxException {
                    assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV25.wav")));
                }
            }

            @Test
            void testMp3() throws URISyntaxException {
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/01.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/issue52.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test23.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test302.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test303.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test47.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test48.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test51.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test52.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test53.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test74.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr128.mp3")));
                // assertNotNull(parser.getRawMetaData(
                // createFile("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr128ID3v1.mp3")));
                // assertNotNull(parser.getRawMetaData(
                // createFile("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr128ID3v1v2.mp3")));
                // assertNotNull(parser.getRawMetaData(
                // createFile("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr128ID3v2.mp3")));
                // assertNotNull(parser.getRawMetaData(
                // createFile("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr128ID3v2pad.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr192.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1L2mono.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1L2stereo.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew0.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew1.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew2.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew3.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew4.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew5.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew6.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew7.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew8.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew9.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld0.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrold1.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld2.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld3.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld4.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld5.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld6.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld7.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld8.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld9.mp3")));
                // assertNotNull(parser.getRawMetaData(
                // createFile("/MEDIAS/Metadata/tagger3/testdata/testV24-comments-utf8.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV25.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV2Cbr128.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV25vbrNew0.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV25vbrOld0.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV2L2.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV2L3Stereo.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV2vbrNew0.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testV2vbrOld0.mp3")));
            }

            @Test
            void testOgg() throws URISyntaxException {
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test.ogg")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test3.ogg")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test5.ogg")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test76.ogg")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test77.ogg")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testlargeimage.ogg")));
                // assertNotNull(parser
                // .getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/testsmallimage.ogg")));
            }

            @Test
            void testFlac() throws URISyntaxException {
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test.flac")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test2.flac")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test3.flac")));
            }

            @Test
            void testM4a() throws URISyntaxException {
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test14.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test15.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test16.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test164.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test19.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test2.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test21.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test3.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test32.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test33.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test38.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test39.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test4.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test41.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test42.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test44.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test5.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test8.m4a")));
            }

            @Test
            void testWav() throws URISyntaxException {

                // isExistingInfoTag
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test123.wav")));

                // !isExistingId3
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test.wav")));

                // isExistingId3
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test126.wav")));

                // assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test125.wav")));
                // assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test127.wav")));
                // assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test128.wav")));
                // assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test129.wav")));
                // assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test130.wav")));
                // assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test131.wav")));
                // assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test153.wav")));
                // assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/bug153.wav")));
            }

            @Test
            void testWma() throws URISyntaxException {
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test1.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test2.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test3.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test4.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test5.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test509.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test6.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test7.wma")));
            }

            @Test
            void testAif() throws URISyntaxException {

                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test119.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test120.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test121.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test124.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test132.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test133.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test134.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test135.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test136.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test137.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test151.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test157.aif")));
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test138.aiff")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test152.aiff")));
            }

            @Test
            void testDsf() throws URISyntaxException {
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test122.dsf")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test156.dsf")));
            }

            @Test
            void testDff() throws URISyntaxException {
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test229.dff")));
            }
        }

        @Nested
        class UnsupportedFormatsTest {

            @Test
            void testMp4() throws URISyntaxException {
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test.stem.mp4")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test218.mp4")));
            }

            @Test
            void testRm() throws URISyntaxException {
                assertNotNull(parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test05.rm")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test06.rm")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test07.rm")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test08.rm")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test09.rm")));
                // assertNotNull(
                // parser.getRawMetaData(createFile("/MEDIAS/Metadata/tagger3/testdata/test10.rm")));
            }
        }
    }

    @Test
    void testIsApplicable() throws URISyntaxException {

        assertFalse(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/")));

        // @see SettingsConstants.General.Extension.MUSIC_FILE_TYPES
        // mp3 ogg oga aac m4a m4b flac wav wma aif aiff ape mpc shn mka opus

        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.mp3")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.ogg")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.oga")));
        assertFalse(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.aac")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.m4a")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.m4b")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.flac")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.wav")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.wma")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.aif")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.aiff")));
        assertFalse(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.ape")));
        assertFalse(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.mpc")));
        assertFalse(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.shn")));
        assertFalse(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.mka")));
        assertFalse(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.opus")));

        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.aifc")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.dsf")));
        assertTrue(parser.isApplicable(createFile("/MEDIAS/Metadata/tagger3/blank/blank.dff")));
    }

    @Test
    void testIsEditingSupported() throws URISyntaxException {

        assertFalse(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/")));

        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.mp3")));
        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.ogg")));
        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.oga")));
        assertFalse(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.aac")));
        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.m4a")));
        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.m4b")));
        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.flac")));
        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.wav")));
        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.wma")));
        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.aif")));
        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.aiff")));
        assertFalse(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.ape")));
        assertFalse(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.mpc")));
        assertFalse(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.shn")));
        assertFalse(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.mka")));
        assertFalse(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.opus")));

        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.aifc")));
        assertTrue(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.dsf")));
        assertFalse(parser.isEditingSupported(createFile("/MEDIAS/Metadata/tagger3/blank/blank.dff"))); // false
    }

    /*
     * Typical IO read error in Jaudiotagger 3.0.1.
     */
    @Nested
    class JaudiotaggerImplementationTest {

        private String createTooSmallMessage(File file) {
            return "Unable to read file because it is too small to be valid audio file: "
                    .concat(file.getAbsolutePath());
        }

        @Test
        @Order(1)
        void testAudioFileIOExeptionTooSmall() throws URISyntaxException {

            // Neither is readable(InvalidAudioFrameException/CannotReadException).

            /*
             * MP3 files always contain at least 1 frame of audio.Therefore, even if the error content is the same, the
             * exception type may be different.
             */
            File mp3 = createFile("/MEDIAS/Metadata/tagger3/blank/blank.mp3");
            assertThrows(InvalidAudioFrameException.class, () -> AudioFileIO.read(mp3), createTooSmallMessage(mp3));

            File ogg = createFile("/MEDIAS/Metadata/tagger3/blank/blank.ogg");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(ogg), createTooSmallMessage(ogg));

            File oga = createFile("/MEDIAS/Metadata/tagger3/blank/blank.oga");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(oga), createTooSmallMessage(oga));

            File aac = createFile("/MEDIAS/Metadata/tagger3/blank/blank.aac");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(aac), createTooSmallMessage(aac));

            File m4a = createFile("/MEDIAS/Metadata/tagger3/blank/blank.m4a");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(m4a), createTooSmallMessage(m4a));

            File m4b = createFile("/MEDIAS/Metadata/tagger3/blank/blank.m4b");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(m4b), createTooSmallMessage(m4b));

            File flac = createFile("/MEDIAS/Metadata/tagger3/blank/blank.flac");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(flac), createTooSmallMessage(flac));

            File wav = createFile("/MEDIAS/Metadata/tagger3/blank/blank.wav");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(wav), createTooSmallMessage(wav));

            File wma = createFile("/MEDIAS/Metadata/tagger3/blank/blank.wma");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(wma), createTooSmallMessage(wav));

            File aif = createFile("/MEDIAS/Metadata/tagger3/blank/blank.aif");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(aif), createTooSmallMessage(aif));

            File aiff = createFile("/MEDIAS/Metadata/tagger3/blank/blank.aiff");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(aiff), createTooSmallMessage(aiff));

            File ape = createFile("/MEDIAS/Metadata/tagger3/blank/blank.ape");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(ape), createTooSmallMessage(ape));

            File mpc = createFile("/MEDIAS/Metadata/tagger3/blank/blank.mpc");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(mpc), createTooSmallMessage(mpc));

            File shn = createFile("/MEDIAS/Metadata/tagger3/blank/blank.shn");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(shn), createTooSmallMessage(shn));

            File mka = createFile("/MEDIAS/Metadata/tagger3/blank/blank.mka");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(mka), createTooSmallMessage(mka));

            File opus = createFile("/MEDIAS/Metadata/tagger3/blank/blank.opus");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(opus), createTooSmallMessage(opus));
        }

        @Test
        @Order(2)
        void testAudioFileIOExeptionNoHeader() throws URISyntaxException {

            File mp3 = createFile("/MEDIAS/Metadata/tagger3/noheader/empty.mp3");
            Throwable e = assertThrows(InvalidAudioFrameException.class, () -> AudioFileIO.read(mp3));
            assertEquals("No audio header found within empty.mp3", e.getMessage());

            File empty1c = createFile("/MEDIAS/Metadata/tagger3/noheader/empty_1c.wav");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(empty1c));
            assertEquals(createTooSmallMessage(empty1c), e.getMessage());

            File empty2c = createFile("/MEDIAS/Metadata/tagger3/noheader/empty_2c.wav");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(empty2c));
            assertEquals(createTooSmallMessage(empty2c), e.getMessage());
        }

        @Test
        @Order(3)
        void testAudioFileIOExeptionNotsupported() throws URISyntaxException, CannotReadException, IOException,
                TagException, ReadOnlyFileException, InvalidAudioFrameException {

            Function<File, String> notsupportedMsg = (file) -> {
                return "No Reader associated with this extension:".concat(FilenameUtils.getExtension(file.getName()));
            };

            File aac = createFile("/MEDIAS/Metadata/tagger3/dummy/empty.aac");
            Throwable e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(aac));
            assertEquals(notsupportedMsg.apply(aac), e.getMessage());

            File opus = createFile("/MEDIAS/Metadata/tagger3/dummy/empty.opus");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(opus));
            assertEquals(notsupportedMsg.apply(opus), e.getMessage());

            File ape = createFile("/MEDIAS/Metadata/tagger3/dummy/empty.ape");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(ape));
            assertEquals(notsupportedMsg.apply(ape), e.getMessage());

            File mka = createFile("/MEDIAS/Metadata/tagger3/dummy/empty.mka");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(mka));
            assertEquals(notsupportedMsg.apply(mka), e.getMessage());

            File mpc = createFile("/MEDIAS/Metadata/tagger3/dummy/empty.mpc");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(mpc));
            assertEquals(notsupportedMsg.apply(mpc), e.getMessage());

            File shn = createFile("/MEDIAS/Metadata/tagger3/dummy/empty.shn");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(shn));
            assertEquals(notsupportedMsg.apply(shn), e.getMessage());
        }
    }

    /*
     * ITunes has many character code defects, so create files from old versions to new versions in Japanese and check
     * if they can be read(Checking iTunes, not Jaudiotagger).
     */
    @Nested
    class ITunesMP3Test {

        private Jaudiotagger3Parser parser = new Jaudiotagger3Parser(null);

        private void assertITunesEN(File file, boolean isAlbumArtist) {
            MetaData metaData = parser.getRawMetaData(file);
            assertEquals("iTunes-Name", metaData.getTitle());
            assertEquals("iTunes-Artist", metaData.getArtist());
            if (isAlbumArtist) {
                assertEquals("iTunes-AlbumArtist", metaData.getAlbumArtist());
            } else {
                assertEquals("iTunes-Artist", metaData.getAlbumArtist());
            }
            assertEquals("iTunes-Album", metaData.getAlbumName());
            assertEquals("Rock", metaData.getGenre());
            assertNull(metaData.getHeight());
            assertNull(metaData.getMusicBrainzReleaseId());
            assertFalse(metaData.isVariableBitRate());
            assertEquals(Integer.valueOf(2019), metaData.getYear());
            assertEquals(Integer.valueOf(1), metaData.getTrackNumber());
            assertEquals(Integer.valueOf(3), metaData.getDiscNumber());
            assertEquals(Integer.valueOf(0), metaData.getDurationSeconds());
            assertEquals(Integer.valueOf(320), metaData.getBitRate());
        }

        private void assertITunesJP(File file, boolean isAlbumArtist) {
            MetaData metaData = parser.getRawMetaData(file);
            assertEquals("iTunes～名前", metaData.getTitle());
            assertEquals("iTunes～アーティスト", metaData.getArtist());
            if (isAlbumArtist) {
                assertEquals("iTunes～アルバムアーティスト", metaData.getAlbumArtist());
            } else {
                assertEquals("iTunes～アーティスト", metaData.getAlbumArtist());
            }
            assertEquals("iTunes～アルバム", metaData.getAlbumName());
            assertEquals("ロック", metaData.getGenre());
            assertNull(metaData.getHeight());
            assertNull(metaData.getMusicBrainzReleaseId());
            assertFalse(metaData.isVariableBitRate());
            assertEquals(Integer.valueOf(2019), metaData.getYear());
            assertEquals(Integer.valueOf(1), metaData.getTrackNumber());
            assertEquals(Integer.valueOf(3), metaData.getDiscNumber());
            assertEquals(Integer.valueOf(0), metaData.getDurationSeconds());
            assertEquals(Integer.valueOf(320), metaData.getBitRate());
        }

        @Test
        void testGetMetaDataForITunes4EN() throws URISyntaxException {
            assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes4.1.0.52.mp3"), false);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForITunes4JP() throws URISyntaxException {
            assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes4.1.0.52JP.mp3"), false);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes5() throws URISyntaxException {
            assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes5.0.1.4.mp3"), false);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes5JP() throws URISyntaxException {
            assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes5.0.1.4JP.mp3"), false);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes6() throws URISyntaxException {
            assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes6.0.0.18.mp3"), false);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes6JP() throws URISyntaxException {
            assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes6.0.0.18JP.mp3"), false);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes7() throws URISyntaxException {
            assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes7.0.0.70.mp3"), true);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes7JP() throws URISyntaxException {
            assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes7.0.0.70JP.mp3"), true);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes8() throws URISyntaxException {
            assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes8.1.0.52.mp3"), true);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes8JP() throws URISyntaxException {
            assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes8.1.0.52JP.mp3"), true);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes10() throws URISyntaxException {
            assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes10.0.0.68.mp3"), true);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes10JP() throws URISyntaxException {
            assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes10.0.0.68JP.mp3"), true);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes11() throws URISyntaxException {
            assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes11.0.0.163.mp3"), true);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes11JP() throws URISyntaxException {
            assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes11.0.0.163JP.mp3"), true);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes12() throws URISyntaxException {
            assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes12.9.6.3.mp3"), true);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes12JP() throws URISyntaxException {
            assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes12.9.6.3JP.mp3"), true);
        }

        /** v2.3 v1.0 */
        @Test
        void testGetMetaDataForMusicCenter() throws URISyntaxException {
            MetaData metaData = parser.getRawMetaData(createFile("/MEDIAS/Metadata/v2.3+v1.0/MusicCenter2.1.0.mp3"));
            assertEquals("MusicCenter-Title", metaData.getTitle());
            assertEquals("MusicCenter-Title(Reading)", metaData.getTitleSort());
            assertEquals("MusicCenter-Artist", metaData.getArtist());
            assertEquals("MusicCenter-Artist(Reading)", metaData.getArtistSort());
            assertEquals("MusicCenter-AlbumArtist", metaData.getAlbumArtist());
            assertEquals("MusicCenter-AlbumArtist(Reading)", metaData.getAlbumArtistSort());
            assertEquals("MusicCenter-Album", metaData.getAlbumName());
            assertEquals("MusicCenter-Album(Reading)", metaData.getAlbumSort());
            assertEquals("Rock", metaData.getGenre());
            assertEquals(Integer.valueOf(2019), metaData.getYear());
            assertEquals("MusicCenter-Composer", metaData.getComposer());
            assertNull(metaData.getComposerSort());
            assertNull(metaData.getTrackNumber()); // If track is not input, output 1.0.
            assertEquals(Integer.valueOf(3), metaData.getDiscNumber());
            assertEquals(Integer.valueOf(0), metaData.getDurationSeconds());
            assertEquals(Integer.valueOf(320), metaData.getBitRate());
            assertNull(metaData.getHeight());
            assertNull(metaData.getMusicBrainzReleaseId());
            assertFalse(metaData.isVariableBitRate());
        }

        /** v2.3 v1.1 */
        @Test
        void testGetMetaDataForMusicCenterJP() throws URISyntaxException {
            MetaData metaData = parser.getRawMetaData(createFile("/MEDIAS/Metadata/v2.3+v1.1/MusicCenter2.1.0JP.mp3"));
            assertEquals("MusicCenter～タイトル", metaData.getTitle());
            assertEquals("MusicCenter～タイトル(読み)", metaData.getTitleSort());
            assertEquals("MusicCenter～アーティスト", metaData.getArtist());
            assertEquals("MusicCenter～アーティスト(読み)", metaData.getArtistSort());
            assertEquals("MusicCenter～アルバムアーティスト", metaData.getAlbumArtist());
            assertEquals("MusicCenter～アルバムアーティスト(読み)", metaData.getAlbumArtistSort());
            assertEquals("MusicCenter～アルバム", metaData.getAlbumName());
            assertEquals("MusicCenter～アルバム(読み)", metaData.getAlbumSort());
            assertEquals("ロック", metaData.getGenre());
            assertEquals(Integer.valueOf(2019), metaData.getYear());
            assertEquals("作曲者", metaData.getComposer());
            assertNull(metaData.getComposerSort());
            assertEquals(Integer.valueOf(1), metaData.getTrackNumber());
            assertEquals(Integer.valueOf(3), metaData.getDiscNumber());
            assertEquals(Integer.valueOf(0), metaData.getDurationSeconds());
            assertEquals(Integer.valueOf(320), metaData.getBitRate());
            assertNull(metaData.getHeight());
            assertNull(metaData.getMusicBrainzReleaseId());
            assertFalse(metaData.isVariableBitRate());
        }

        /** v2.3 */
        @Test
        void testGetMetaDataForV23WithMp3TagJP() throws URISyntaxException {
            MetaData metaData = parser.getRawMetaData(createFile("/MEDIAS/Metadata/v2.3/Mp3tag2.9.7.mp3"));
            assertEquals("MusicCenter～タイトル", metaData.getTitle());
            assertEquals("MusicCenter～タイトル(読み)", metaData.getTitleSort());
            assertEquals("MusicCenter～アーティスト", metaData.getArtist());
            assertEquals("MusicCenter～アーティスト(読み)", metaData.getArtistSort());
            assertEquals("MusicCenter～アルバムアーティスト", metaData.getAlbumArtist());
            assertEquals("MusicCenter～アルバムアーティスト(読み)", metaData.getAlbumArtistSort());
            assertEquals("MusicCenter～アルバム", metaData.getAlbumName());
            assertEquals("MusicCenter～アルバム(読み)", metaData.getAlbumSort());
            assertEquals("ロック", metaData.getGenre());
            assertEquals(Integer.valueOf(2019), metaData.getYear());
            assertEquals("作曲者", metaData.getComposer());
            assertEquals("作曲者(読み)", metaData.getComposerSort());
            assertEquals(Integer.valueOf(1), metaData.getTrackNumber());
            assertEquals(Integer.valueOf(3), metaData.getDiscNumber());
            assertEquals(Integer.valueOf(0), metaData.getDurationSeconds());
            assertEquals(Integer.valueOf(320), metaData.getBitRate());
            assertNull(metaData.getHeight());
            assertNull(metaData.getMusicBrainzReleaseId());
            assertFalse(metaData.isVariableBitRate());
        }

        /** v2.4 */
        @Test
        void testGetMetaDataForv24WithMp3TagJP() throws URISyntaxException {
            MetaData metaData = parser.getRawMetaData(createFile("/MEDIAS/Metadata/v2.4/Mp3tag2.9.7.mp3"));
            assertEquals("MusicCenter～タイトル", metaData.getTitle());
            assertEquals("MusicCenter～タイトル(読み)", metaData.getTitleSort());
            assertEquals("MusicCenter～アーティスト", metaData.getArtist());
            assertEquals("MusicCenter～アーティスト(読み)", metaData.getArtistSort());
            assertEquals("MusicCenter～アルバムアーティスト", metaData.getAlbumArtist());
            assertEquals("MusicCenter～アルバムアーティスト(読み)", metaData.getAlbumArtistSort());
            assertEquals("MusicCenter～アルバム", metaData.getAlbumName());
            assertEquals("MusicCenter～アルバム(読み)", metaData.getAlbumSort());
            assertEquals("ロック", metaData.getGenre());
            assertEquals(Integer.valueOf(2019), metaData.getYear());
            assertEquals("作曲者", metaData.getComposer());
            assertEquals("作曲者(読み)", metaData.getComposerSort());
            assertEquals(Integer.valueOf(1), metaData.getTrackNumber());
            assertEquals(Integer.valueOf(3), metaData.getDiscNumber());
            assertEquals(Integer.valueOf(0), metaData.getDurationSeconds());
            assertEquals(Integer.valueOf(320), metaData.getBitRate());
            assertNull(metaData.getHeight());
            assertNull(metaData.getMusicBrainzReleaseId());
            assertFalse(metaData.isVariableBitRate());
        }

    }
}
