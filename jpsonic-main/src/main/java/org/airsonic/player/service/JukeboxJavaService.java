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

package org.airsonic.player.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.biconou.AudioPlayer.api.PlayList;
import com.github.biconou.AudioPlayer.api.PlayerListener;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.jukebox.JavaPlayerFactory;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Rémi Cocula
 */
@Service
public class JukeboxJavaService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JukeboxJavaService.class);
    private static final float DEFAULT_GAIN = 0.75f;
    private static final String DEFAULT_MIXER_ENTRY_KEY = "_default";

    private final AudioScrobblerService audioScrobblerService;
    private final StatusService statusService;
    private final SecurityService securityService;
    private final MediaFileService mediaFileService;
    private final JavaPlayerFactory javaPlayerFactory;
    private final Map<Integer, com.github.biconou.AudioPlayer.api.Player> activeAudioPlayers;
    private final Map<String, List<com.github.biconou.AudioPlayer.api.Player>> activeAudioPlayersPerMixer;

    private TransferStatus status;

    public JukeboxJavaService(AudioScrobblerService audioScrobblerService, StatusService statusService,
            SecurityService securityService, MediaFileService mediaFileService, JavaPlayerFactory javaPlayerFactory) {
        this.audioScrobblerService = audioScrobblerService;
        this.statusService = statusService;
        this.securityService = securityService;
        this.mediaFileService = mediaFileService;
        this.javaPlayerFactory = javaPlayerFactory;
        activeAudioPlayers = new ConcurrentHashMap<>();
        activeAudioPlayersPerMixer = new ConcurrentHashMap<>();
    }

    /**
     * Finds the corresponding active audio player for a given airsonic player. If no player exists we create one. The
     * JukeboxJavaService references all active audio players in a map indexed by airsonic player id.
     *
     * @param airsonicPlayer
     *            a given airsonic player.
     * 
     * @return the corresponding active audio player.
     */
    private com.github.biconou.AudioPlayer.api.Player retrieveAudioPlayerForAirsonicPlayer(Player airsonicPlayer) {
        com.github.biconou.AudioPlayer.api.Player foundPlayer = activeAudioPlayers.get(airsonicPlayer.getId());
        if (foundPlayer == null) {
            synchronized (activeAudioPlayers) {
                com.github.biconou.AudioPlayer.api.Player newPlayer = initAudioPlayer(airsonicPlayer);
                activeAudioPlayers.put(airsonicPlayer.getId(), newPlayer);
                String mixer = airsonicPlayer.getJavaJukeboxMixer();
                if (StringUtils.isBlank(mixer)) {
                    mixer = DEFAULT_MIXER_ENTRY_KEY;
                }
                List<com.github.biconou.AudioPlayer.api.Player> playersForMixer = activeAudioPlayersPerMixer
                        .computeIfAbsent(mixer, k -> new ArrayList<>());
                playersForMixer.add(newPlayer);
                foundPlayer = newPlayer;
            }
        }
        return foundPlayer;
    }

    private String createIllegalPlayerStrings(Player player) {
        return "The player " + player.getName() + " is not a java jukebox player";
    }

    private com.github.biconou.AudioPlayer.api.Player initAudioPlayer(final Player player) {

        if (!player.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new IllegalArgumentException(createIllegalPlayerStrings(player));
        }

        LOG.info("begin initAudioPlayer");

        com.github.biconou.AudioPlayer.api.Player audioPlayer;

        if (StringUtils.isNotBlank(player.getJavaJukeboxMixer())) {
            if (LOG.isInfoEnabled()) {
                LOG.info("use mixer : {}", player.getJavaJukeboxMixer());
            }
            audioPlayer = javaPlayerFactory.createJavaPlayer(player.getJavaJukeboxMixer());
        } else {
            LOG.info("use default mixer");
            audioPlayer = javaPlayerFactory.createJavaPlayer();
        }
        if (audioPlayer == null) {
            throw new IllegalStateException("AudioPlayer has not been initialized properly");
        } else {
            audioPlayer.setGain(DEFAULT_GAIN);
            audioPlayer.registerListener(new PlayerListener() {
                @Override
                public void onBegin(int index, File currentFile) {
                    onSongStart(player);
                }

                @Override
                public void onEnd(int index, File file) {
                    onSongEnd(player);
                }

                @Override
                public void onFinished() {
                    player.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
                }

                @Override
                public void onStop() {
                    player.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
                }

                @Override
                public void onPause() {
                    // Nothing to do here
                }
            });
            if (LOG.isInfoEnabled()) {
                LOG.info("New audio player {} has been initialized.", audioPlayer.toString());
            }
        }
        return audioPlayer;
    }

    public int getPosition(final Player player) {

        if (!player.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new IllegalArgumentException(createIllegalPlayerStrings(player));
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(player);
        return audioPlayer.getPlayingInfos().currentAudioPositionInSeconds();
    }

    public void setPosition(final Player player, int positionInSeconds) {
        if (!player.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new IllegalArgumentException(createIllegalPlayerStrings(player));
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(player);
        audioPlayer.setPos(positionInSeconds);
    }

    public float getGain(final Player player) {
        if (!player.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new IllegalArgumentException(createIllegalPlayerStrings(player));
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(player);
        return audioPlayer.getGain();
    }

    public void setGain(final Player player, final float gain) {
        if (!player.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new IllegalArgumentException(createIllegalPlayerStrings(player));
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(player);
        LOG.debug("setGain : gain={}", gain);
        audioPlayer.setGain(gain);
    }

    protected final void onSongStart(Player player) {
        MediaFile file = player.getPlayQueue().getCurrentFile();
        if (LOG.isInfoEnabled()) {
            LOG.info("[onSongStart] {} starting jukebox for \"{}\"", player.getUsername(),
                    FileUtil.getShortPath(file.getFile()));
        }
        if (status != null) {
            statusService.removeStreamStatus(status);
        }
        status = statusService.createStreamStatus(player);
        status.setFile(file.getFile());
        status.addBytesTransfered(file.getFileSize());
        mediaFileService.incrementPlayCount(file);
        scrobble(player, file, false);
    }

    @SuppressWarnings("PMD.NullAssignment") // (status) Intentional allocation to show there is no status
    protected final void onSongEnd(Player player) {
        MediaFile file = player.getPlayQueue().getCurrentFile();
        if (LOG.isInfoEnabled()) {
            LOG.info("[onSongEnd] {} stopping jukebox for \"{}\"", player.getUsername(),
                    FileUtil.getShortPath(file.getFile()));
        }
        if (status != null) {
            statusService.removeStreamStatus(status);
            status = null;
        }
        scrobble(player, file, true);
    }

    private void scrobble(Player player, MediaFile file, boolean submission) {
        if (player.getClientId() == null) { // Don't scrobble REST players.
            audioScrobblerService.register(file, player.getUsername(), submission, null);
        }
    }

    /**
     * Plays the playqueue of a jukebox player starting at the beginning.
     */
    public void play(Player airsonicPlayer) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("begin play jukebox : player = id:{};name:{}", airsonicPlayer.getId(), airsonicPlayer.getName());
        }
        // Control user authorizations
        User user = securityService.getUserByName(airsonicPlayer.getUsername());
        if (!user.isJukeboxRole() && LOG.isWarnEnabled()) {
            LOG.warn("{} is not authorized for jukebox playback.", user.getUsername());
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Different file to play -> start a new play list");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        if (airsonicPlayer.getPlayQueue().getCurrentFile() != null) {
            audioPlayer.setPlayList(new PlayList() {

                @Override
                public File getNextAudioFile() {
                    airsonicPlayer.getPlayQueue().next();
                    return getCurrentAudioFile();
                }

                @Override
                public File getCurrentAudioFile() {
                    MediaFile current = airsonicPlayer.getPlayQueue().getCurrentFile();
                    if (current == null) {
                        return null;
                    } else {
                        return airsonicPlayer.getPlayQueue().getCurrentFile().getFile();
                    }
                }

                @Override
                public int getSize() {
                    return airsonicPlayer.getPlayQueue().size();
                }

                @Override
                public int getIndex() {
                    return airsonicPlayer.getPlayQueue().getIndex();
                }
            });
            synchronized (activeAudioPlayers) {
                // Close any other player using the same mixer.
                String mixer = airsonicPlayer.getJavaJukeboxMixer();
                if (StringUtils.isBlank(mixer)) {
                    mixer = DEFAULT_MIXER_ENTRY_KEY;
                }
                List<com.github.biconou.AudioPlayer.api.Player> playersForSameMixer = activeAudioPlayersPerMixer
                        .get(mixer);
                playersForSameMixer.forEach(player -> {
                    if (player != audioPlayer) {
                        player.close();
                    }
                });
            }
            audioPlayer.play();
        }
    }

    public void start(Player airsonicPlayer) {
        play(airsonicPlayer);
    }

    public void stop(Player airsonicPlayer) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("begin stop jukebox : player = id:{};name:{}", airsonicPlayer.getId(), airsonicPlayer.getName());
        }

        // Control user authorizations
        User user = securityService.getUserByName(airsonicPlayer.getUsername());
        if (!user.isJukeboxRole() && LOG.isWarnEnabled()) {
            LOG.warn("{} is not authorized for jukebox playback.", user.getUsername());
            return;
        }

        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        if (LOG.isDebugEnabled()) {
            LOG.debug("PlayQueue.Status is {}", airsonicPlayer.getPlayQueue().getStatus());
        }
        audioPlayer.pause();
    }

    public void skip(Player airsonicPlayer, int index, int offset) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("begin skip jukebox : player = id:{};name:{}", airsonicPlayer.getId(), airsonicPlayer.getName());
        }

        // Control user authorizations
        User user = securityService.getUserByName(airsonicPlayer.getUsername());
        if (!user.isJukeboxRole() && LOG.isWarnEnabled()) {
            LOG.warn("{} is not authorized for jukebox playback.", user.getUsername());
            return;
        }

        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        if (index == 0 && offset == 0) {
            play(airsonicPlayer);
        } else {
            if (offset == 0) {
                audioPlayer.stop();
                audioPlayer.play();
            } else {
                audioPlayer.setPos(offset);
            }
        }
    }
}
