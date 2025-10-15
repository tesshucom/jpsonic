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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.service.scrobbler;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.scrobbler.ListenBrainzScrobbler.RegistrationData;
import org.junit.jupiter.api.Test;

class ListenBrainzScrobblerTest {

    @Test
    void testSubmit() throws ExecutionException {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setArtist("artist");
        mediaFile.setAlbumName("album");
        mediaFile.setTitle("title");
        mediaFile.setMusicBrainzReleaseId("musicBrainzReleaseId");
        mediaFile.setMusicBrainzRecordingId("musicBrainzRecordingId");
        mediaFile.setTrackNumber(1);
        RegistrationData data = new RegistrationData(mediaFile, "token", true, Instant.now());
        assertFalse(ListenBrainzScrobbler.submit(data));
    }
}
