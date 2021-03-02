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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.service.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.net.URISyntaxException;

import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class JaudiotaggerParserTest extends AbstractAirsonicHomeTest {

    @Autowired
    private JaudiotaggerParser parser;

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

    private File createFile(String resourcePath) throws URISyntaxException {
        return new File(getClass().getResource(resourcePath).toURI());
    }

    @Test
    public void testGetMetaDataForITunes4EN() throws URISyntaxException {
        assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes4.1.0.52.mp3"), false);
    }

    /** v2.2, UTF-16 */
    @Test
    public void testGetMetaDataForITunes4JP() throws URISyntaxException {
        assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes4.1.0.52JP.mp3"), false);
    }

    /** v2.2 */
    @Test
    public void testGetMetaDataForiTunes5() throws URISyntaxException {
        assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes5.0.1.4.mp3"), false);
    }

    /** v2.2, UTF-16 */
    @Test
    public void testGetMetaDataForiTunes5JP() throws URISyntaxException {
        assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes5.0.1.4JP.mp3"), false);
    }

    /** v2.2 */
    @Test
    public void testGetMetaDataForiTunes6() throws URISyntaxException {
        assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes6.0.0.18.mp3"), false);
    }

    /** v2.2, UTF-16 */
    @Test
    public void testGetMetaDataForiTunes6JP() throws URISyntaxException {
        assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes6.0.0.18JP.mp3"), false);
    }

    /** v2.2 */
    @Test
    public void testGetMetaDataForiTunes7() throws URISyntaxException {
        assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes7.0.0.70.mp3"), true);
    }

    /** v2.2, UTF-16 */
    @Test
    public void testGetMetaDataForiTunes7JP() throws URISyntaxException {
        assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes7.0.0.70JP.mp3"), true);
    }

    /** v2.2 */
    @Test
    public void testGetMetaDataForiTunes8() throws URISyntaxException {
        assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes8.1.0.52.mp3"), true);
    }

    /** v2.2, UTF-16 */
    @Test
    public void testGetMetaDataForiTunes8JP() throws URISyntaxException {
        assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes8.1.0.52JP.mp3"), true);
    }

    /** v2.2 */
    @Test
    public void testGetMetaDataForiTunes10() throws URISyntaxException {
        assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes10.0.0.68.mp3"), true);
    }

    /** v2.2, UTF-16 */
    @Test
    public void testGetMetaDataForiTunes10JP() throws URISyntaxException {
        assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes10.0.0.68JP.mp3"), true);
    }

    /** v2.2 */
    @Test
    public void testGetMetaDataForiTunes11() throws URISyntaxException {
        assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes11.0.0.163.mp3"), true);
    }

    /** v2.2, UTF-16 */
    @Test
    public void testGetMetaDataForiTunes11JP() throws URISyntaxException {
        assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes11.0.0.163JP.mp3"), true);
    }

    /** v2.2 */
    @Test
    public void testGetMetaDataForiTunes12() throws URISyntaxException {
        assertITunesEN(createFile("/MEDIAS/Metadata/v2.2/iTunes12.9.6.3.mp3"), true);
    }

    /** v2.2, UTF-16 */
    @Test
    public void testGetMetaDataForiTunes12JP() throws URISyntaxException {
        assertITunesJP(createFile("/MEDIAS/Metadata/v2.2/UTF-16/iTunes12.9.6.3JP.mp3"), true);
    }

    /** v2.3 v1.0 */
    @Test
    public void testGetMetaDataForMusicCenter() throws URISyntaxException {
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
    public void testGetMetaDataForMusicCenterJP() throws URISyntaxException {
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
    public void testGetMetaDataForV23WithMp3TagJP() throws URISyntaxException {
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
    public void testGetMetaDataForv24WithMp3TagJP() throws URISyntaxException {
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
