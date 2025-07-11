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
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.commons.io.FilenameUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
class MusicParserTest {

    private static final MusicParser PARSER = new MusicParser(null);

    private static Path createPath(String resourcePath) throws URISyntaxException {
        return Path.of(MusicParserTest.class.getResource(resourcePath).toURI());
    }

    @Nested
    class GetRawMetaDataTest {

        boolean isEmptyMetaData(MetaData metaData) {
            return isEmpty(metaData.getAlbumArtist()) && isEmpty(metaData.getAlbumName())
                    && isEmpty(metaData.getArtist()) && isEmpty(metaData.getDiscNumber())
                    && isEmpty(metaData.getGenre()) && isEmpty(metaData.getMusicBrainzRecordingId())
                    && isEmpty(metaData.getMusicBrainzReleaseId()) && isEmpty(metaData.getTitle())
                    && isEmpty(metaData.getTrackNumber()) && isEmpty(metaData.getYear())
                    && isEmpty(metaData.getArtistSort()) && isEmpty(metaData.getAlbumSort())
                    && isEmpty(metaData.getTitleSort()) && isEmpty(metaData.getAlbumArtistSort())
                    && isEmpty(metaData.getComposer()) && isEmpty(metaData.getComposerSort());
        }

        /*
         * Diverted Jaudiotagger test data (/MEDIAS/Metadata/tagger3/testdata). Case
         * omitted to avoid size bloat.
         */
        @Nested
        class SupportedFormatsTest {

            @Nested
            class CannotReadTest {

                @Test
                void testNoAudioHeader() throws URISyntaxException {
                    assertNotNull(PARSER
                        .getRawMetaData(
                                createPath("/MEDIAS/Metadata/tagger3/testdata/corrupt.mp3")));
                    assertNotNull(PARSER
                        .getRawMetaData(
                                createPath("/MEDIAS/Metadata/tagger3/testdata/Issue79.mp3")));
                    assertNotNull(PARSER
                        .getRawMetaData(
                                createPath("/MEDIAS/Metadata/tagger3/testdata/Issue81.mp3")));
                }

                @Test
                void testNoOggSHeader() throws URISyntaxException {
                    assertNotNull(PARSER
                        .getRawMetaData(
                                createPath("/MEDIAS/Metadata/tagger3/testdata/test36.ogg")));
                }

                @Test
                void testInvalidOggHeader() throws URISyntaxException {
                    assertNotNull(PARSER
                        .getRawMetaData(
                                createPath("/MEDIAS/Metadata/tagger3/testdata/test508.ogg")));
                }

                @Test
                void testInvalidRIFFHeader() throws URISyntaxException {
                    assertNotNull(PARSER
                        .getRawMetaData(
                                createPath("/MEDIAS/Metadata/tagger3/testdata/testV25.wav")));
                }
            }

            @Test
            void testMp3() throws URISyntaxException {
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/01.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/issue52.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test23.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test302.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test303.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test47.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test48.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test51.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test52.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test53.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test74.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr128.mp3")));
                // assertNotNull(parser.getRawMetaData(
                // createPath("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr128ID3v1.mp3")));
                // assertNotNull(parser.getRawMetaData(
                // createPath("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr128ID3v1v2.mp3")));
                // assertNotNull(parser.getRawMetaData(
                // createPath("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr128ID3v2.mp3")));
                // assertNotNull(parser.getRawMetaData(
                // createPath("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr128ID3v2pad.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1Cbr192.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1L2mono.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1L2stereo.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew0.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew1.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew2.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew3.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew4.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew5.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew6.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew7.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew8.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrNew9.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld0.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrold1.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld2.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld3.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld4.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld5.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld6.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld7.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld8.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV1vbrOld9.mp3")));
                // assertNotNull(parser.getRawMetaData(
                // createPath("/MEDIAS/Metadata/tagger3/testdata/testV24-comments-utf8.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV25.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV2Cbr128.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV25vbrNew0.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV25vbrOld0.mp3")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV2L2.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV2L3Stereo.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV2vbrNew0.mp3")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testV2vbrOld0.mp3")));
            }

            @Test
            void testOgg() throws URISyntaxException {
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test.ogg")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test3.ogg")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test5.ogg")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test76.ogg")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test77.ogg")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testlargeimage.ogg")));
                // assertNotNull(parser
                // .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/testsmallimage.ogg")));
            }

            @Test
            void testFlac() throws URISyntaxException {
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test.flac")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test2.flac")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test3.flac")));
            }

            @Test
            void testM4a() throws URISyntaxException {
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test14.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test15.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test16.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test164.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test19.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test2.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test21.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test3.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test32.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test33.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test38.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test39.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test4.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test41.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test42.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test44.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test5.m4a")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test8.m4a")));
            }

            @Test
            void testWav() throws URISyntaxException {

                // isExistingInfoTag
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test123.wav")));

                // !isExistingId3
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test.wav")));

                // isExistingId3
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test126.wav")));

                // assertNotNull(parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test125.wav")));
                // assertNotNull(parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test127.wav")));
                // assertNotNull(parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test128.wav")));
                // assertNotNull(parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test129.wav")));
                // assertNotNull(parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test130.wav")));
                // assertNotNull(parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test131.wav")));
                // assertNotNull(parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test153.wav")));
                // assertNotNull(parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/bug153.wav")));
            }

            @Test
            void testWma() throws URISyntaxException {
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test1.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test2.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test3.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test4.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test5.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test509.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test6.wma")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test7.wma")));
            }

            @Test
            void testAif() throws URISyntaxException {

                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test119.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test120.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test121.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test124.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test132.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test133.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test134.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test135.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test136.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test137.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test151.aif")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test157.aif")));
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test138.aiff")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test152.aiff")));
            }

            @Test
            void testDsf() throws URISyntaxException {
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test122.dsf")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test156.dsf")));
            }

            @Test
            void testDff() throws URISyntaxException {
                assertNotNull(PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test229.dff")));
            }

            /*
             * Add tags to Jaudiotagger test data(/MEDIAS/Metadata/tagger3/tagged). Due to
             * its reliability, Music Center for PC and Mp3tag were used to edit the tags.
             * These software use different Wav tag formats by default.
             */
            @Nested
            class TaggedTest {

                void assertTagsWrittenByMp3tag(MetaData metaData) {
                    assertEquals("Mp3tag:AlbumArtist", metaData.getAlbumArtist());
                    assertEquals("Mp3tag:Album", metaData.getAlbumName());
                    assertEquals("Mp3tag:Artist", metaData.getArtist());
                    assertEquals(98, metaData.getDiscNumber());
                    assertEquals("Mp3tag:Genre", metaData.getGenre());
                    assertEquals("Mp3tag:MusicBrainzTrackId", metaData.getMusicBrainzRecordingId());
                    assertEquals("Mp3tag:MusicBrainzReleaseId", metaData.getMusicBrainzReleaseId());
                    assertEquals("Mp3tag:Title", metaData.getTitle());
                    assertEquals(96, metaData.getTrackNumber());
                    assertEquals(2022, metaData.getYear());
                    assertEquals("Mp3tag:ArtistReading", metaData.getArtistSort());
                    assertEquals("Mp3tag:AlbumReading", metaData.getAlbumSort());
                    assertEquals("Mp3tag:TitleReading", metaData.getTitleSort());
                    assertEquals("Mp3tag:AlbumArtistReading", metaData.getAlbumArtistSort());
                    assertEquals("Mp3tag:Composer", metaData.getComposer());
                    assertEquals("Mp3tag:ComposerReading", metaData.getComposerSort());
                }

                void assertTagsWrittenByMC4PCAndMp3tag(MetaData metaData) {
                    assertEquals("MC4PC:AlbumArtist", metaData.getAlbumArtist());
                    assertEquals("MC4PC:Album", metaData.getAlbumName());
                    assertEquals("MC4PC:Artist", metaData.getArtist());
                    assertEquals(98, metaData.getDiscNumber());
                    assertEquals("MC4PC:Genre", metaData.getGenre());
                    assertEquals("Mp3tag:MusicBrainzTrackId", metaData.getMusicBrainzRecordingId());
                    assertEquals("Mp3tag:MusicBrainzReleaseId", metaData.getMusicBrainzReleaseId());
                    assertEquals("MC4PC:Title", metaData.getTitle());
                    assertEquals(96, metaData.getTrackNumber());
                    assertEquals(2022, metaData.getYear());
                    assertEquals("MC4PC:ArtistReading", metaData.getArtistSort());
                    assertEquals("MC4PC:AlbumReading", metaData.getAlbumSort());
                    assertEquals("MC4PC:TitleReading", metaData.getTitleSort());
                    assertEquals("MC4PC:AlbumArtistReading", metaData.getAlbumArtistSort());
                    assertEquals("MC4PC:Composer", metaData.getComposer());
                    assertEquals("Mp3tag:ComposerReading", metaData.getComposerSort());
                }

                @Test
                void testOgg() throws URISyntaxException, CannotReadException, IOException,
                        TagException, ReadOnlyFileException, InvalidAudioFrameException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/tagged/test.ogg"));
                    assertNotNull(metaData);
                    assertTagsWrittenByMp3tag(metaData);
                    assertTrue(metaData.isVariableBitRate());
                    assertEquals(192, metaData.getBitRate());
                    assertEquals(1, metaData.getDurationSeconds());
                }

                @Test
                void testFlac() throws URISyntaxException, CannotReadException, IOException,
                        TagException, ReadOnlyFileException, InvalidAudioFrameException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/tagged/test.flac"));
                    assertNotNull(metaData);
                    assertTagsWrittenByMC4PCAndMp3tag(metaData);
                    assertTrue(metaData.isVariableBitRate());
                    assertEquals(192, metaData.getBitRate());
                    assertEquals(5, metaData.getDurationSeconds());
                }

                @Test
                void testMp3() throws URISyntaxException, CannotReadException, IOException,
                        TagException, ReadOnlyFileException, InvalidAudioFrameException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/tagged/01.mp3"));
                    assertNotNull(metaData);
                    assertTagsWrittenByMp3tag(metaData);
                    assertFalse(metaData.isVariableBitRate());
                    assertEquals(64, metaData.getBitRate());
                    assertEquals(14, metaData.getDurationSeconds());
                }

                @Test
                void testMp3v1() throws URISyntaxException, CannotReadException, IOException,
                        TagException, ReadOnlyFileException, InvalidAudioFrameException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/v1/Mp3tag3.12.mp3"));
                    assertNotNull(metaData);

                    assertEquals("Mp3tag:Artist", metaData.getAlbumArtist()); // Because the value
                                                                              // is copied
                    assertEquals("Mp3tag:Album", metaData.getAlbumName());
                    assertEquals("Mp3tag:Artist", metaData.getArtist());
                    assertNull(metaData.getDiscNumber());
                    assertEquals("Acoustic", metaData.getGenre());
                    assertNull(metaData.getMusicBrainzRecordingId());
                    assertNull(metaData.getMusicBrainzReleaseId());
                    assertEquals("Mp3tag:Title", metaData.getTitle());
                    assertEquals(96, metaData.getTrackNumber());
                    assertEquals(2022, metaData.getYear());
                    assertNull(metaData.getArtistSort());
                    assertNull(metaData.getAlbumSort());
                    assertNull(metaData.getTitleSort());
                    assertNull(metaData.getAlbumArtistSort());
                    assertNull(metaData.getComposer());
                    assertNull(metaData.getComposerSort());

                    assertFalse(metaData.isVariableBitRate());
                    assertEquals(320, metaData.getBitRate());
                    assertEquals(0, metaData.getDurationSeconds());
                }

                @Test
                void testM4a() throws URISyntaxException, CannotReadException, IOException,
                        TagException, ReadOnlyFileException, InvalidAudioFrameException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/tagged/test.m4a"));
                    assertNotNull(metaData);
                    assertTagsWrittenByMp3tag(metaData);
                    assertTrue(metaData.isVariableBitRate());
                    assertEquals(128, metaData.getBitRate());
                    assertEquals(242, metaData.getDurationSeconds());
                }

                /*
                 * Wav files that are not ID3 chunks are treated as empty. (The process of
                 * reloading the tag is not implemented). In other words, it works the same as
                 * Unsupported Formats. This is because tag management with Wav is not common
                 * anyway.
                 */
                @Test
                void testWavMC4PCTag() throws CannotReadException, IOException, TagException,
                        ReadOnlyFileException, InvalidAudioFrameException, URISyntaxException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/tagged/mc4pc.wav"));
                    assertNotNull(metaData);
                    // Tags are not loaded.
                    assertTrue(isEmptyMetaData(metaData));
                    // Only audio headers will be used.
                    assertFalse(metaData.isVariableBitRate());
                    assertEquals(176, metaData.getBitRate());
                    assertEquals(14, metaData.getDurationSeconds());
                }

                @Test
                void testWavTag() throws URISyntaxException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/tagged/test.wav"));
                    assertNotNull(metaData);
                    assertFalse(isEmptyMetaData(metaData));
                    assertFalse(metaData.isVariableBitRate());
                    assertEquals(176, metaData.getBitRate());
                    assertEquals(14, metaData.getDurationSeconds());
                }

                @Test
                void testWma() throws CannotReadException, IOException, TagException,
                        ReadOnlyFileException, InvalidAudioFrameException, URISyntaxException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/tagged/test1.wma"));
                    assertNotNull(metaData);
                    assertTagsWrittenByMC4PCAndMp3tag(metaData);
                    assertFalse(metaData.isVariableBitRate());
                    assertEquals(32, metaData.getBitRate());
                    assertEquals(220, metaData.getDurationSeconds());
                }

                @Test
                void testAif() throws CannotReadException, IOException, TagException,
                        ReadOnlyFileException, InvalidAudioFrameException, URISyntaxException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/tagged/test119.aif"));
                    assertNotNull(metaData);
                    assertFalse(isEmptyMetaData(metaData));
                    assertFalse(metaData.isVariableBitRate());
                    assertEquals(128, metaData.getBitRate());
                    assertEquals(3, metaData.getDurationSeconds());
                }

                @Test
                void testDsf() throws URISyntaxException, CannotReadException, IOException,
                        TagException, ReadOnlyFileException, InvalidAudioFrameException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/tagged/test122.dsf"));
                    assertNotNull(metaData);
                    assertTagsWrittenByMC4PCAndMp3tag(metaData);
                    assertFalse(metaData.isVariableBitRate());
                    assertEquals(5_644_800, metaData.getBitRate());
                    assertEquals(5, metaData.getDurationSeconds());
                }

                @Test
                void testDff() throws URISyntaxException, CannotReadException, IOException,
                        TagException, ReadOnlyFileException, InvalidAudioFrameException {
                    MetaData metaData = PARSER
                        .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/tagged/test229.dff"));
                    // Tags are not loaded.
                    assertNotNull(metaData);
                    assertTrue(isEmptyMetaData(metaData));
                    // Only audio headers will be used.
                    assertFalse(metaData.isVariableBitRate());
                    assertEquals(5_644_800, metaData.getBitRate());
                    assertEquals(5, metaData.getDurationSeconds());
                }
            }
        }

        @Nested
        class UnsupportedFormatsTest {

            @Test
            void testMp4() throws URISyntaxException {
                MetaData metaData = PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test.stem.mp4"));
                assertNotNull(metaData);
                assertTrue(isEmptyMetaData(metaData));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test218.mp4")));
            }

            /*
             * RealAudio has a hierarchical tag specification like Wav. Jpaonic has no plans
             * to implement a parser at this time (because Jaudiotagger has not officially
             * announced support). If you do support, you will need to pay attention to the
             * tag specifications.
             */
            @Test
            void testRm() throws URISyntaxException {
                MetaData metaData = PARSER
                    .getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test05.rm"));
                assertNotNull(metaData);
                assertTrue(isEmptyMetaData(metaData));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test06.rm")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test07.rm")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test08.rm")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test09.rm")));
                // assertNotNull(
                // parser.getRawMetaData(createPath("/MEDIAS/Metadata/tagger3/testdata/test10.rm")));
            }
        }
    }

    @Test
    void testIsApplicable() throws URISyntaxException {

        assertFalse(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/")));

        // @see SettingsConstants.General.Extension.MUSIC_FILE_TYPES
        // mp3 ogg oga aac m4a m4b flac wav wma aif aiff ape mpc shn mka opus

        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.mp3")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.ogg")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.oga")));
        assertFalse(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.aac")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.m4a")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.m4b")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.flac")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.wav")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.wma")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.aif")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.aiff")));
        assertFalse(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.ape")));
        assertFalse(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.mpc")));
        assertFalse(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.shn")));
        assertFalse(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.mka")));
        assertFalse(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.opus")));

        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.aifc")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.dsf")));
        assertTrue(PARSER.isApplicable(createPath("/MEDIAS/Metadata/tagger3/blank/blank.dff")));
    }

    @Test
    void testIsEditingSupported() throws URISyntaxException {

        assertFalse(PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/")));

        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.mp3")));
        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.ogg")));
        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.oga")));
        assertFalse(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.aac")));
        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.m4a")));
        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.m4b")));
        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.flac")));
        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.wav")));
        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.wma")));
        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.aif")));
        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.aiff")));
        assertFalse(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.ape")));
        assertFalse(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.mpc")));
        assertFalse(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.shn")));
        assertFalse(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.mka")));
        assertFalse(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.opus")));

        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.aifc")));
        assertTrue(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.dsf")));
        assertFalse(
                PARSER.isEditingSupported(createPath("/MEDIAS/Metadata/tagger3/blank/blank.dff"))); // false
    }

    /*
     * Typical IO read error in Jaudiotagger 3.0.1.
     */
    @Nested
    class JaudiotaggerImplementationTest {

        private String createTooSmallMessage(Path file) {
            return "Unable to read file because it is too small to be valid audio file: "
                .concat(file.toString());
        }

        private Path createPath(String resourcePath) throws URISyntaxException {
            return Path.of(MusicParserTest.class.getResource(resourcePath).toURI());
        }

        @Test
        @Order(1)
        void testAudioFileIOExeptionTooSmall() throws URISyntaxException {

            // Neither is readable(InvalidAudioFrameException/CannotReadException).

            /*
             * MP3 files always contain at least 1 frame of audio.Therefore, even if the
             * error content is the same, the exception type may be different.
             */
            Path mp3 = createPath("/MEDIAS/Metadata/tagger3/blank/blank.mp3");
            assertThrows(InvalidAudioFrameException.class, () -> AudioFileIO.read(mp3.toFile()),
                    createTooSmallMessage(mp3));

            Path ogg = createPath("/MEDIAS/Metadata/tagger3/blank/blank.ogg");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(ogg.toFile()),
                    createTooSmallMessage(ogg));

            Path oga = createPath("/MEDIAS/Metadata/tagger3/blank/blank.oga");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(oga.toFile()),
                    createTooSmallMessage(oga));

            Path aac = createPath("/MEDIAS/Metadata/tagger3/blank/blank.aac");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(aac.toFile()),
                    createTooSmallMessage(aac));

            Path m4a = createPath("/MEDIAS/Metadata/tagger3/blank/blank.m4a");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(m4a.toFile()),
                    createTooSmallMessage(m4a));

            Path m4b = createPath("/MEDIAS/Metadata/tagger3/blank/blank.m4b");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(m4b.toFile()),
                    createTooSmallMessage(m4b));

            Path flac = createPath("/MEDIAS/Metadata/tagger3/blank/blank.flac");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(flac.toFile()),
                    createTooSmallMessage(flac));

            Path wav = createPath("/MEDIAS/Metadata/tagger3/blank/blank.wav");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(wav.toFile()),
                    createTooSmallMessage(wav));

            Path wma = createPath("/MEDIAS/Metadata/tagger3/blank/blank.wma");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(wma.toFile()),
                    createTooSmallMessage(wav));

            Path aif = createPath("/MEDIAS/Metadata/tagger3/blank/blank.aif");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(aif.toFile()),
                    createTooSmallMessage(aif));

            Path aiff = createPath("/MEDIAS/Metadata/tagger3/blank/blank.aiff");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(aiff.toFile()),
                    createTooSmallMessage(aiff));

            Path ape = createPath("/MEDIAS/Metadata/tagger3/blank/blank.ape");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(ape.toFile()),
                    createTooSmallMessage(ape));

            Path mpc = createPath("/MEDIAS/Metadata/tagger3/blank/blank.mpc");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(mpc.toFile()),
                    createTooSmallMessage(mpc));

            Path shn = createPath("/MEDIAS/Metadata/tagger3/blank/blank.shn");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(shn.toFile()),
                    createTooSmallMessage(shn));

            Path mka = createPath("/MEDIAS/Metadata/tagger3/blank/blank.mka");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(mka.toFile()),
                    createTooSmallMessage(mka));

            Path opus = createPath("/MEDIAS/Metadata/tagger3/blank/blank.opus");
            assertThrows(CannotReadException.class, () -> AudioFileIO.read(opus.toFile()),
                    createTooSmallMessage(opus));
        }

        @Test
        @Order(2)
        void testAudioFileIOExeptionNoHeader() throws URISyntaxException {

            Path mp3 = createPath("/MEDIAS/Metadata/tagger3/noheader/empty.mp3");
            Throwable e = assertThrows(InvalidAudioFrameException.class,
                    () -> AudioFileIO.read(mp3.toFile()));
            assertEquals("No audio header found within empty.mp3", e.getMessage());

            Path empty1c = createPath("/MEDIAS/Metadata/tagger3/noheader/empty_1c.wav");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(empty1c.toFile()));
            assertEquals(createTooSmallMessage(empty1c), e.getMessage());

            Path empty2c = createPath("/MEDIAS/Metadata/tagger3/noheader/empty_2c.wav");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(empty2c.toFile()));
            assertEquals(createTooSmallMessage(empty2c), e.getMessage());
        }

        @Test
        @Order(3)
        void testAudioFileIOExeptionNotsupported() throws URISyntaxException, CannotReadException,
                IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {

            Function<Path, String> notsupportedMsg = (file) -> {
                return "No Reader associated with this extension:"
                    .concat(FilenameUtils.getExtension(file.toString()));
            };

            Path aac = createPath("/MEDIAS/Metadata/tagger3/dummy/empty.aac");
            Throwable e = assertThrows(CannotReadException.class,
                    () -> AudioFileIO.read(aac.toFile()));
            assertEquals(notsupportedMsg.apply(aac), e.getMessage());

            Path opus = createPath("/MEDIAS/Metadata/tagger3/dummy/empty.opus");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(opus.toFile()));
            assertEquals(notsupportedMsg.apply(opus), e.getMessage());

            Path ape = createPath("/MEDIAS/Metadata/tagger3/dummy/empty.ape");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(ape.toFile()));
            assertEquals(notsupportedMsg.apply(ape), e.getMessage());

            Path mka = createPath("/MEDIAS/Metadata/tagger3/dummy/empty.mka");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(mka.toFile()));
            assertEquals(notsupportedMsg.apply(mka), e.getMessage());

            Path mpc = createPath("/MEDIAS/Metadata/tagger3/dummy/empty.mpc");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(mpc.toFile()));
            assertEquals(notsupportedMsg.apply(mpc), e.getMessage());

            Path shn = createPath("/MEDIAS/Metadata/tagger3/dummy/empty.shn");
            e = assertThrows(CannotReadException.class, () -> AudioFileIO.read(shn.toFile()));
            assertEquals(notsupportedMsg.apply(shn), e.getMessage());
        }

        @Nested
        class TaggedTest {

            void assertTagsWrittenByMp3tag(Tag tag) {
                assertEquals("Mp3tag:AlbumArtist", tag.getFirst(FieldKey.ALBUM_ARTIST));
                assertEquals("Mp3tag:Album", tag.getFirst(FieldKey.ALBUM));
                assertEquals("Mp3tag:Artist", tag.getFirst(FieldKey.ARTIST));
                assertEquals("98", tag.getFirst(FieldKey.DISC_NO));
                assertEquals("Mp3tag:Genre", tag.getFirst(FieldKey.GENRE));
                assertEquals("Mp3tag:MusicBrainzTrackId",
                        tag.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID));
                assertEquals("Mp3tag:MusicBrainzReleaseId",
                        tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID));
                assertEquals("Mp3tag:Title", tag.getFirst(FieldKey.TITLE));
                assertEquals("96", tag.getFirst(FieldKey.TRACK));
                assertEquals("2022", tag.getFirst(FieldKey.YEAR));
                assertEquals("Mp3tag:ArtistReading", tag.getFirst(FieldKey.ARTIST_SORT));
                assertEquals("Mp3tag:AlbumReading", tag.getFirst(FieldKey.ALBUM_SORT));
                assertEquals("Mp3tag:TitleReading", tag.getFirst(FieldKey.TITLE_SORT));
                assertEquals("Mp3tag:AlbumArtistReading", tag.getFirst(FieldKey.ALBUM_ARTIST_SORT));
                assertEquals("Mp3tag:Composer", tag.getFirst(FieldKey.COMPOSER));
                assertEquals("Mp3tag:ComposerReading", tag.getFirst(FieldKey.COMPOSER_SORT));
            }

            void assertTagsWrittenByMC4PCAndMp3tag(Tag tag) {
                assertEquals("MC4PC:AlbumArtist", tag.getFirst(FieldKey.ALBUM_ARTIST));
                assertEquals("MC4PC:Album", tag.getFirst(FieldKey.ALBUM));
                assertEquals("MC4PC:Artist", tag.getFirst(FieldKey.ARTIST));
                assertEquals("98", tag.getFirst(FieldKey.DISC_NO));
                assertEquals("MC4PC:Genre", tag.getFirst(FieldKey.GENRE));
                assertEquals("Mp3tag:MusicBrainzTrackId",
                        tag.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID));
                assertEquals("Mp3tag:MusicBrainzReleaseId",
                        tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID));
                assertEquals("MC4PC:Title", tag.getFirst(FieldKey.TITLE));
                assertEquals("96", tag.getFirst(FieldKey.TRACK));
                assertEquals("2022-01-01", tag.getFirst(FieldKey.YEAR));
                assertEquals("MC4PC:ArtistReading", tag.getFirst(FieldKey.ARTIST_SORT));
                assertEquals("MC4PC:AlbumReading", tag.getFirst(FieldKey.ALBUM_SORT));
                assertEquals("MC4PC:TitleReading", tag.getFirst(FieldKey.TITLE_SORT));
                assertEquals("MC4PC:AlbumArtistReading", tag.getFirst(FieldKey.ALBUM_ARTIST_SORT));
                assertEquals("MC4PC:Composer", tag.getFirst(FieldKey.COMPOSER));
                assertEquals("Mp3tag:ComposerReading", tag.getFirst(FieldKey.COMPOSER_SORT));
            }

            @Test
            void testOgg() throws URISyntaxException, CannotReadException, IOException,
                    TagException, ReadOnlyFileException, InvalidAudioFrameException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/tagger3/tagged/test.ogg").toFile());
                Tag tag = af.getTag();
                assertNotNull(tag);
                assertTagsWrittenByMp3tag(tag);
            }

            @Test
            void testFlac() throws URISyntaxException, CannotReadException, IOException,
                    TagException, ReadOnlyFileException, InvalidAudioFrameException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/tagger3/tagged/test.flac").toFile());
                Tag tag = af.getTag();
                assertNotNull(tag);
                assertTagsWrittenByMC4PCAndMp3tag(tag);
            }

            @Test
            void testMp3() throws URISyntaxException, CannotReadException, IOException,
                    TagException, ReadOnlyFileException, InvalidAudioFrameException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/tagger3/tagged/01.mp3").toFile());
                Tag tag = af.getTag();
                assertNotNull(tag);
                assertTagsWrittenByMp3tag(tag);
            }

            @Test
            void testMp3v1() throws URISyntaxException, CannotReadException, IOException,
                    TagException, ReadOnlyFileException, InvalidAudioFrameException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/v1/Mp3tag3.12.mp3").toFile());
                Tag tag = af.getTag();
                assertNotNull(tag);

                /*
                 * Earlier versions of Jaudiotagger threw NotFound when using incompatible keys.
                 * Currently blank are returned.
                 */
                assertEquals("", tag.getFirst(FieldKey.ALBUM_ARTIST));
                assertEquals("Mp3tag:Album", tag.getFirst(FieldKey.ALBUM));
                assertEquals("Mp3tag:Artist", tag.getFirst(FieldKey.ARTIST));
                assertEquals("", tag.getFirst(FieldKey.DISC_NO));
                assertEquals("Acoustic", tag.getFirst(FieldKey.GENRE));
                assertEquals("", tag.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID));
                assertEquals("", tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID));
                assertEquals("Mp3tag:Title", tag.getFirst(FieldKey.TITLE));
                assertEquals("96", tag.getFirst(FieldKey.TRACK));
                assertEquals("2022", tag.getFirst(FieldKey.YEAR));
                assertEquals("", tag.getFirst(FieldKey.ARTIST_SORT));
                assertEquals("", tag.getFirst(FieldKey.ALBUM_SORT));
                assertEquals("", tag.getFirst(FieldKey.TITLE_SORT));
                assertEquals("", tag.getFirst(FieldKey.ALBUM_ARTIST_SORT));
                assertEquals("", tag.getFirst(FieldKey.COMPOSER));
                assertEquals("", tag.getFirst(FieldKey.COMPOSER_SORT));
            }

            @Test
            void testM4a() throws URISyntaxException, CannotReadException, IOException,
                    TagException, ReadOnlyFileException, InvalidAudioFrameException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/tagger3/tagged/test.m4a").toFile());
                Tag tag = af.getTag();
                assertNotNull(tag);
                assertTagsWrittenByMp3tag(tag);
            }

            @Test
            void testWavMC4PC() throws CannotReadException, IOException, TagException,
                    ReadOnlyFileException, InvalidAudioFrameException, URISyntaxException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/tagger3/tagged/mc4pc.wav").toFile());
                Tag tag = af.getTag();
                assertEquals("", tag.getFirst(FieldKey.ALBUM_ARTIST));
            }

            @Test
            void testWav() throws CannotReadException, IOException, TagException,
                    ReadOnlyFileException, InvalidAudioFrameException, URISyntaxException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/tagger3/tagged/test.wav").toFile());
                Tag tag = af.getTag();
                assertNotNull(tag);
                assertTagsWrittenByMp3tag(tag);
            }

            /*
             * COMPOSER_SORT(WM/ComposerSortOrder) is different from the published document.
             */
            @Test
            void testWma() throws CannotReadException, IOException, TagException,
                    ReadOnlyFileException, InvalidAudioFrameException, URISyntaxException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/tagger3/tagged/test1.wma").toFile());
                Tag tag = af.getTag();
                assertNotNull(tag);
                assertTagsWrittenByMC4PCAndMp3tag(tag);
            }

            @Test
            void testAif() throws CannotReadException, IOException, TagException,
                    ReadOnlyFileException, InvalidAudioFrameException, URISyntaxException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/tagger3/tagged/test119.aif").toFile());
                Tag tag = af.getTag();
                assertNotNull(tag);
                assertTagsWrittenByMp3tag(tag);
            }

            @Test
            void testDsf() throws URISyntaxException, CannotReadException, IOException,
                    TagException, ReadOnlyFileException, InvalidAudioFrameException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/tagger3/tagged/test122.dsf").toFile());
                Tag tag = af.getTag();
                assertNotNull(tag);
                assertTagsWrittenByMC4PCAndMp3tag(tag);
            }

            @Test
            void testDff() throws URISyntaxException, CannotReadException, IOException,
                    TagException, ReadOnlyFileException, InvalidAudioFrameException {
                AudioFile af = AudioFileIO
                    .read(createPath("/MEDIAS/Metadata/tagger3/tagged/test229.dff").toFile());
                // Tags are not loaded. Only audio headers will be used.
                assertNull(af.getTag());
            }
        }
    }

    /*
     * ITunes has many character code defects, so create files from old versions to
     * new versions in Japanese and check if they can be read(Checking iTunes, not
     * Jaudiotagger).
     */
    @Nested
    class ITunesMP3Test {

        private final MusicParser parser = new MusicParser(null);

        private void assertITunesEN(Path path, boolean isAlbumArtist) {
            MetaData metaData = parser.getRawMetaData(path);
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
            assertEquals(2019, metaData.getYear());
            assertEquals(1, metaData.getTrackNumber());
            assertEquals(3, metaData.getDiscNumber());
            assertEquals(0, metaData.getDurationSeconds());
            assertEquals(320, metaData.getBitRate());
        }

        private void assertITunesJP(Path path, boolean isAlbumArtist) {
            MetaData metaData = parser.getRawMetaData(path);
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
            assertEquals(2019, metaData.getYear());
            assertEquals(1, metaData.getTrackNumber());
            assertEquals(3, metaData.getDiscNumber());
            assertEquals(0, metaData.getDurationSeconds());
            assertEquals(320, metaData.getBitRate());
        }

        @Test
        void testGetMetaDataForITunes4EN() throws URISyntaxException {
            assertITunesEN(createPath("/MEDIAS/Metadata/v2.2/iTunes4.1.0.52.mp3"), false);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForITunes4JP() throws URISyntaxException {
            assertITunesJP(createPath("/MEDIAS/Metadata/v2.2/UTF-16/iTunes4.1.0.52JP.mp3"), false);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes5() throws URISyntaxException {
            assertITunesEN(createPath("/MEDIAS/Metadata/v2.2/iTunes5.0.1.4.mp3"), false);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes5JP() throws URISyntaxException {
            assertITunesJP(createPath("/MEDIAS/Metadata/v2.2/UTF-16/iTunes5.0.1.4JP.mp3"), false);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes6() throws URISyntaxException {
            assertITunesEN(createPath("/MEDIAS/Metadata/v2.2/iTunes6.0.0.18.mp3"), false);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes6JP() throws URISyntaxException {
            assertITunesJP(createPath("/MEDIAS/Metadata/v2.2/UTF-16/iTunes6.0.0.18JP.mp3"), false);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes7() throws URISyntaxException {
            assertITunesEN(createPath("/MEDIAS/Metadata/v2.2/iTunes7.0.0.70.mp3"), true);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes7JP() throws URISyntaxException {
            assertITunesJP(createPath("/MEDIAS/Metadata/v2.2/UTF-16/iTunes7.0.0.70JP.mp3"), true);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes8() throws URISyntaxException {
            assertITunesEN(createPath("/MEDIAS/Metadata/v2.2/iTunes8.1.0.52.mp3"), true);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes8JP() throws URISyntaxException {
            assertITunesJP(createPath("/MEDIAS/Metadata/v2.2/UTF-16/iTunes8.1.0.52JP.mp3"), true);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes10() throws URISyntaxException {
            assertITunesEN(createPath("/MEDIAS/Metadata/v2.2/iTunes10.0.0.68.mp3"), true);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes10JP() throws URISyntaxException {
            assertITunesJP(createPath("/MEDIAS/Metadata/v2.2/UTF-16/iTunes10.0.0.68JP.mp3"), true);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes11() throws URISyntaxException {
            assertITunesEN(createPath("/MEDIAS/Metadata/v2.2/iTunes11.0.0.163.mp3"), true);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes11JP() throws URISyntaxException {
            assertITunesJP(createPath("/MEDIAS/Metadata/v2.2/UTF-16/iTunes11.0.0.163JP.mp3"), true);
        }

        /** v2.2 */
        @Test
        void testGetMetaDataForiTunes12() throws URISyntaxException {
            assertITunesEN(createPath("/MEDIAS/Metadata/v2.2/iTunes12.9.6.3.mp3"), true);
        }

        /** v2.2, UTF-16 */
        @Test
        void testGetMetaDataForiTunes12JP() throws URISyntaxException {
            assertITunesJP(createPath("/MEDIAS/Metadata/v2.2/UTF-16/iTunes12.9.6.3JP.mp3"), true);
        }

        /** v2.3 v1.0 */
        @Test
        void testGetMetaDataForMusicCenter() throws URISyntaxException {
            MetaData metaData = parser
                .getRawMetaData(createPath("/MEDIAS/Metadata/v2.3+v1.0/MusicCenter2.1.0.mp3"));
            assertEquals("MusicCenter-Title", metaData.getTitle());
            assertEquals("MusicCenter-Title(Reading)", metaData.getTitleSort());
            assertEquals("MusicCenter-Artist", metaData.getArtist());
            assertEquals("MusicCenter-Artist(Reading)", metaData.getArtistSort());
            assertEquals("MusicCenter-AlbumArtist", metaData.getAlbumArtist());
            assertEquals("MusicCenter-AlbumArtist(Reading)", metaData.getAlbumArtistSort());
            assertEquals("MusicCenter-Album", metaData.getAlbumName());
            assertEquals("MusicCenter-Album(Reading)", metaData.getAlbumSort());
            assertEquals("Rock", metaData.getGenre());
            assertEquals(2019, metaData.getYear());
            assertEquals("MusicCenter-Composer", metaData.getComposer());
            assertNull(metaData.getComposerSort());
            assertNull(metaData.getTrackNumber()); // If track is not input, output 1.0.
            assertEquals(3, metaData.getDiscNumber());
            assertEquals(0, metaData.getDurationSeconds());
            assertEquals(320, metaData.getBitRate());
            assertNull(metaData.getHeight());
            assertNull(metaData.getMusicBrainzReleaseId());
            assertFalse(metaData.isVariableBitRate());
        }

        /** v2.3 v1.1 */
        @Test
        void testGetMetaDataForMusicCenterJP() throws URISyntaxException {
            MetaData metaData = parser
                .getRawMetaData(createPath("/MEDIAS/Metadata/v2.3+v1.1/MusicCenter2.1.0JP.mp3"));
            assertEquals("MusicCenter～タイトル", metaData.getTitle());
            assertEquals("MusicCenter～タイトル(読み)", metaData.getTitleSort());
            assertEquals("MusicCenter～アーティスト", metaData.getArtist());
            assertEquals("MusicCenter～アーティスト(読み)", metaData.getArtistSort());
            assertEquals("MusicCenter～アルバムアーティスト", metaData.getAlbumArtist());
            assertEquals("MusicCenter～アルバムアーティスト(読み)", metaData.getAlbumArtistSort());
            assertEquals("MusicCenter～アルバム", metaData.getAlbumName());
            assertEquals("MusicCenter～アルバム(読み)", metaData.getAlbumSort());
            assertEquals("ロック", metaData.getGenre());
            assertEquals(2019, metaData.getYear());
            assertEquals("作曲者", metaData.getComposer());
            assertNull(metaData.getComposerSort());
            assertEquals(1, metaData.getTrackNumber());
            assertEquals(3, metaData.getDiscNumber());
            assertEquals(0, metaData.getDurationSeconds());
            assertEquals(320, metaData.getBitRate());
            assertNull(metaData.getHeight());
            assertNull(metaData.getMusicBrainzReleaseId());
            assertFalse(metaData.isVariableBitRate());
        }

        /** v2.3 */
        @Test
        void testGetMetaDataForV23WithMp3TagJP() throws URISyntaxException {
            MetaData metaData = parser
                .getRawMetaData(createPath("/MEDIAS/Metadata/v2.3/Mp3tag2.9.7.mp3"));
            assertEquals("MusicCenter～タイトル", metaData.getTitle());
            assertEquals("MusicCenter～タイトル(読み)", metaData.getTitleSort());
            assertEquals("MusicCenter～アーティスト", metaData.getArtist());
            assertEquals("MusicCenter～アーティスト(読み)", metaData.getArtistSort());
            assertEquals("MusicCenter～アルバムアーティスト", metaData.getAlbumArtist());
            assertEquals("MusicCenter～アルバムアーティスト(読み)", metaData.getAlbumArtistSort());
            assertEquals("MusicCenter～アルバム", metaData.getAlbumName());
            assertEquals("MusicCenter～アルバム(読み)", metaData.getAlbumSort());
            assertEquals("ロック", metaData.getGenre());
            assertEquals(2019, metaData.getYear());
            assertEquals("作曲者", metaData.getComposer());
            assertEquals("作曲者(読み)", metaData.getComposerSort());
            assertEquals(1, metaData.getTrackNumber());
            assertEquals(3, metaData.getDiscNumber());
            assertEquals(0, metaData.getDurationSeconds());
            assertEquals(320, metaData.getBitRate());
            assertNull(metaData.getHeight());
            assertNull(metaData.getMusicBrainzReleaseId());
            assertFalse(metaData.isVariableBitRate());
        }

        /** v2.4 */
        @Test
        void testGetMetaDataForv24WithMp3TagJP() throws URISyntaxException {
            MetaData metaData = parser
                .getRawMetaData(createPath("/MEDIAS/Metadata/v2.4/Mp3tag2.9.7.mp3"));
            assertEquals("MusicCenter～タイトル", metaData.getTitle());
            assertEquals("MusicCenter～タイトル(読み)", metaData.getTitleSort());
            assertEquals("MusicCenter～アーティスト", metaData.getArtist());
            assertEquals("MusicCenter～アーティスト(読み)", metaData.getArtistSort());
            assertEquals("MusicCenter～アルバムアーティスト", metaData.getAlbumArtist());
            assertEquals("MusicCenter～アルバムアーティスト(読み)", metaData.getAlbumArtistSort());
            assertEquals("MusicCenter～アルバム", metaData.getAlbumName());
            assertEquals("MusicCenter～アルバム(読み)", metaData.getAlbumSort());
            assertEquals("ロック", metaData.getGenre());
            assertEquals(2019, metaData.getYear());
            assertEquals("作曲者", metaData.getComposer());
            assertEquals("作曲者(読み)", metaData.getComposerSort());
            assertEquals(1, metaData.getTrackNumber());
            assertEquals(3, metaData.getDiscNumber());
            assertEquals(0, metaData.getDurationSeconds());
            assertEquals(320, metaData.getBitRate());
            assertNull(metaData.getHeight());
            assertNull(metaData.getMusicBrainzReleaseId());
            assertFalse(metaData.isVariableBitRate());
        }

    }
}
