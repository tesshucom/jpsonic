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

package com.tesshu.jpsonic.service;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sound.sampled.LineUnavailableException;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayQueue;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.domain.TransferStatus;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.VideoTranscodingSettings;
import com.tesshu.jpsonic.service.jukebox.AudioPlayer;
import com.tesshu.jpsonic.service.jukebox.AudioPlayerFactory;
import com.tesshu.jpsonic.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Plays music on the local audio device.
 *
 * @author Sindre Mehus
 */
@Service
public class JukeboxLegacySubsonicService implements AudioPlayer.Listener {

    private static final Logger LOG = LoggerFactory.getLogger(JukeboxLegacySubsonicService.class);
    private static final Object PLAYER_LOCK = new Object();

    private final AudioScrobblerService audioScrobblerService;
    private final StatusService statusService;
    private final MediaFileService mediaFileService;
    private final AudioPlayerFactory audioPlayerFactory;
    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final TranscodingService transcodingService;
    private final AtomicInteger gain = new AtomicInteger();
    private final AtomicBoolean destroy = new AtomicBoolean();

    private AudioPlayer audioPlayer;
    private Player player;
    private TransferStatus status;
    private MediaFile currentPlayingFile;
    private int offset;

    public JukeboxLegacySubsonicService(AudioScrobblerService audioScrobblerService, StatusService statusService,
            MediaFileService mediaFileService, AudioPlayerFactory audioPlayerFactory, SettingsService settingsService,
            SecurityService securityService, TranscodingService transcodingService) {
        super();
        this.audioScrobblerService = audioScrobblerService;
        this.statusService = statusService;
        this.mediaFileService = mediaFileService;
        this.audioPlayerFactory = audioPlayerFactory;
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.transcodingService = transcodingService;
    }

    @PostConstruct
    public void init() {
        gain.set(floatToIntBits(AudioPlayer.DEFAULT_GAIN));
        destroy.set(false);
    }

    @PreDestroy
    public void onDestroy() {
        destroy.set(true);
        if (audioPlayer != null) {
            audioPlayer.close();
        }
    }

    /**
     * Updates the jukebox by starting or pausing playback on the local audio device.
     *
     * @param player
     *            The player in question.
     * @param offset
     *            Start playing after this many seconds into the track.
     */
    public void updateJukebox(Player player, int offset) {
        if (destroy.get()) {
            return;
        }
        User user = securityService.getUserByName(player.getUsername());
        if (!user.isJukeboxRole()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(user.getUsername() + " is not authorized for jukebox playback.");
            }
            return;
        }

        synchronized (PLAYER_LOCK) {
            if (player.getPlayQueue().getStatus() == PlayQueue.Status.PLAYING) {
                this.player = player;
                MediaFile result;
                synchronized (player.getPlayQueue()) {
                    result = player.getPlayQueue().getCurrentFile();
                }
                play(result, offset);
            } else {
                if (audioPlayer != null) {
                    audioPlayer.pause();
                }
            }
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    /*
     * This class opens the resource, but due to the nature of the media manipulation logic, the close is done at a
     * different location / timing. See AudioPlayer#close. Do not explicitly close it in this class.
     */
    private void play(MediaFile file, int offset) {
        if (destroy.get()) {
            return;
        }
        InputStream in = null;
        try {
            synchronized (PLAYER_LOCK) {
                // Resume if possible.
                boolean sameFile = file != null && file.equals(currentPlayingFile);
                boolean paused = audioPlayer != null && audioPlayer.getState() == AudioPlayer.State.PAUSED;
                if (sameFile && paused && offset == 0) {
                    audioPlayer.play();
                } else {
                    this.offset = offset;
                    if (audioPlayer != null) {
                        audioPlayer.close();
                        if (currentPlayingFile != null) {
                            onSongEnd(currentPlayingFile);
                        }
                    }

                    if (file != null) {
                        int duration = file.getDurationSeconds() == null ? 0 : file.getDurationSeconds() - offset;
                        TranscodingService.Parameters parameters = new TranscodingService.Parameters(file,
                                new VideoTranscodingSettings(0, 0, offset, duration, false));
                        String command = settingsService.getJukeboxCommand();
                        parameters.setTranscoding(new Transcoding(null, null, null, null, command, null, null, false));
                        in = transcodingService.getTranscodedInputStream(parameters);
                        audioPlayer = audioPlayerFactory.createAudioPlayer(in, this);
                        audioPlayer.setGain(intBitsToFloat(gain.get()));
                        audioPlayer.play();
                        onSongStart(file);
                    }
                }
                currentPlayingFile = file;
            }

        } catch (LineUnavailableException | IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error in jukebox: ", e);
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Cannot be close.", ioe);
                    }
                }
            }
        }
    }

    @Override
    public void stateChanged(AudioPlayer audioPlayer, AudioPlayer.State state) {
        if (destroy.get()) {
            return;
        }
        synchronized (PLAYER_LOCK) {
            if (state == AudioPlayer.State.EOM) {
                player.getPlayQueue().next();
                MediaFile result;
                synchronized (player.getPlayQueue()) {
                    result = player.getPlayQueue().getCurrentFile();
                }
                play(result, 0);
            }
        }
    }

    public float getGain() {
        if (destroy.get()) {
            return 0;
        }
        return intBitsToFloat(gain.get());
    }

    public int getPosition() {
        if (audioPlayer == null) {
            return 0;
        }
        synchronized (PLAYER_LOCK) {
            return offset + audioPlayer.getPosition();
        }
    }

    /**
     * Returns the player which currently uses the jukebox.
     *
     * @return The player, may be {@code null}.
     */
    public Player getPlayer() {
        return player;
    }

    private void onSongStart(MediaFile file) {
        if (destroy.get()) {
            return;
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(player.getUsername() + " starting jukebox for \"" + FileUtil.getShortPath(file.getFile()) + "\"");
        }
        status = statusService.createStreamStatus(player);
        status.setFile(file.getFile());
        status.addBytesTransfered(file.getFileSize());
        mediaFileService.incrementPlayCount(file);
        scrobble(file, false);
    }

    private void onSongEnd(MediaFile file) {
        if (destroy.get()) {
            return;
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(player.getUsername() + " stopping jukebox for \"" + FileUtil.getShortPath(file.getFile()) + "\"");
        }
        if (status != null) {
            statusService.removeStreamStatus(status);
        }
        scrobble(file, true);
    }

    private void scrobble(MediaFile file, boolean submission) {
        if (destroy.get()) {
            return;
        }
        if (player.getClientId() == null) { // Don't scrobble REST players.
            audioScrobblerService.register(file, player.getUsername(), submission, null);
        }
    }

    public void setGain(float gain) {
        if (destroy.get()) {
            return;
        }
        this.gain.set(floatToIntBits(gain));
        synchronized (PLAYER_LOCK) {
            if (audioPlayer != null) {
                audioPlayer.setGain(gain);
            }
        }
    }
}
