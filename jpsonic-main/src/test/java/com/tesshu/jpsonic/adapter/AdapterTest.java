package com.tesshu.jpsonic.adapter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MediaFile.BitRate;
import com.tesshu.jpsonic.domain.model.MediaFile.DurationSeconds;
import com.tesshu.jpsonic.domain.model.MediaFile.Format;
import com.tesshu.jpsonic.domain.model.MediaFile.Type;
import com.tesshu.jpsonic.domain.model.Player;
import com.tesshu.jpsonic.domain.model.TranscodingDefinition;
import com.tesshu.jpsonic.domain.model.TranscodingDefinition.BitRateLimit;
import com.tesshu.jpsonic.domain.model.User;
import com.tesshu.jpsonic.domain.model.UserSettings;
import com.tesshu.jpsonic.domain.provider.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.PlayerProvider;
import com.tesshu.jpsonic.domain.provider.TranscodingProvider;
import com.tesshu.jpsonic.domain.provider.UserProvider;
import com.tesshu.jpsonic.feature.crypt.upnp.StreamPayload;
import com.tesshu.jpsonic.feature.crypt.upnp.StreamPayload.StreamType;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpPayloadCodec;
import com.tesshu.jpsonic.persistence.NeedsDB;
import com.tesshu.jpsonic.persistence.api.entity.Transcoding;
import com.tesshu.jpsonic.persistence.api.repository.PlayerDao;
import com.tesshu.jpsonic.persistence.core.repository.UserDao;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@NeedsDB
class AdapterTest extends AbstractNeedsScan {

    @Autowired
    private MediaFileProvider mediaFileRepository;
    @Autowired
    private PlayerProvider playerProvider;

    @Autowired
    private TranscodingService transcodingService;

    @Autowired
    private TranscodingProvider transcodingRepository;
    @Autowired
    private UserDao userDao;
    @Autowired
    private UserService userService;
    @Autowired
    private UserProvider userProvider;
    @Autowired
    private PlayerDao playerDao;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private UpnpPayloadCodec upnpPayloadCodec;

    @BeforeEach
    void setup() {
        populateDatabaseOnlyOnce();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testRequireMediaFile() {
        final MediaFile mediaFile1 = mediaFileRepository.requireMediaFile(2);
        final MediaFile mediaFile2 = mediaFileRepository.requireMediaFile(6);
        final MediaFile mediaFile3 = mediaFileRepository.requireMediaFile(4);

        assertEquals(2, mediaFile1.id());
        assertEquals(1, mediaFile1.folderId());
        assertEquals("_DIR_ Ravel", mediaFile1.name());
        assertEquals(MediaFile.Format.UNDEFINED, mediaFile1.format());
        assertEquals(MediaFile.Type.DIRECTORY, mediaFile1.type());
        assertEquals(MediaFile.BitRate.UNDEFINED, mediaFile1.bitRate());
        assertEquals(MediaFile.DurationSeconds.UNDEFINED, mediaFile1.durationSeconds());
        assertEquals(0, mediaFile1.fileSize());
        assertEquals("_DIR_ Ravel", mediaFile1.artist());
        assertNull(mediaFile1.album());
        assertNull(mediaFile1.title());

        assertEquals(6, mediaFile2.id());
        assertEquals(1, mediaFile2.folderId());
        assertNull(mediaFile2.title());
        assertEquals("_DIR_ Ravel - Chamber Music With Voice", mediaFile2.name());
        assertEquals(Format.UNDEFINED, mediaFile2.format());
        assertEquals(Type.ALBUM, mediaFile2.type());
        assertEquals(BitRate.UNDEFINED, mediaFile2.bitRate());
        assertEquals(DurationSeconds.UNDEFINED, mediaFile2.durationSeconds());
        assertEquals(0, mediaFile2.fileSize());
        assertEquals("_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", mediaFile2.artist());
        assertEquals("_ID3_ALBUM_ Ravel - Chamber Music With Voice", mediaFile2.album());
        assertNull(mediaFile2.title());

        assertEquals(4, mediaFile3.id());
        assertEquals(1, mediaFile3.folderId());
        assertEquals("Bach: Goldberg Variations, BWV 988 - Aria", mediaFile3.title());
        assertEquals("Bach: Goldberg Variations, BWV 988 - Aria", mediaFile3.name());
        assertEquals("flac", mediaFile3.format().value());
        assertEquals(Type.MUSIC, mediaFile3.type());
        assertEquals(756, mediaFile3.bitRate().value());
        assertEquals(4, mediaFile3.durationSeconds().value());
        assertEquals(358_406, mediaFile3.fileSize());
        assertEquals("_ID3_ARTIST_ Céline Frisch: Café Zimmermann", mediaFile3.artist());
        assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]", mediaFile3.album());
        assertEquals("Bach: Goldberg Variations, BWV 988 - Aria", mediaFile3.title());
    }

    @Test
    void testPlayerAndUser() {
        assertEquals(1, playerDao.getAllPlayers().size());
        Player player = playerProvider.getUPnPPlayer();
        assertEquals(1, playerDao.getAllPlayers().size());

        assertNotNull(player);
        assertEquals("guest", player.userName());
        assertEquals(BitRateLimit.OFF, player.bitRateLimit());
        assertNull(player.ipAddress());
        assertNotNull(player.lastSeen());

        UserSettings settings = userProvider.getUserSettings(User.USERNAME_GUEST);
        assertNotNull(settings);
        assertEquals(BitRateLimit.OFF, settings.bitRateLimit());
    }

    @Test
    void testUpnpPayloadCodec() {
        int id = 15_555_500;
        String payloadStr = upnpPayloadCodec.encodeStream(id, StreamType.MUSIC);
        StreamPayload payload = upnpPayloadCodec.decodeStream(payloadStr);
        assertEquals(id, payload.id());
        assertEquals(StreamType.MUSIC, payload.streamType());
    }

    @Test
    void testTranscodingProviderAdapter() {
        Player domainPlayer = playerProvider.getUPnPPlayer();
        List<TranscodingDefinition> transcodingDefinitions = transcodingRepository
            .get(domainPlayer);
        assertEquals(0, transcodingDefinitions.size());

        com.tesshu.jpsonic.persistence.api.entity.Player player = playerService.getUPnPPlayer();
        List<Transcoding> transcodings = transcodingService.getTranscodingsForPlayer(player);
        assertEquals(0, transcodings.size());
    }
}
