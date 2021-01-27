
package org.airsonic.player.service.jukebox;

import java.io.InputStream;

import org.airsonic.player.service.JukeboxLegacySubsonicService;
import org.springframework.stereotype.Component;

@Component
public class AudioPlayerFactory {

    public AudioPlayer createAudioPlayer(InputStream in, JukeboxLegacySubsonicService jukeboxLegacySubsonicService)
            throws Exception {
        return new AudioPlayer(in, jukeboxLegacySubsonicService);
    }
}
