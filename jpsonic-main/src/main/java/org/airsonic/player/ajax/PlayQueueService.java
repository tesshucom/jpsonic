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

package org.airsonic.player.ajax;

import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.ALBUM;
import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.ARTIST;
import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.TRACK;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.dao.InternetRadioDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlayQueueDao;
import org.airsonic.player.domain.InternetRadio;
import org.airsonic.player.domain.InternetRadioSource;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastStatus;
import org.airsonic.player.domain.SavedPlayQueue;
import org.airsonic.player.service.InternetRadioService;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.JukeboxService;
import org.airsonic.player.service.LastFmService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.NetworkUtils;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.PodcastService;
import org.airsonic.player.service.RatingService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.StringUtil;
import org.directwebremoting.WebContextFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Provides AJAX-enabled services for manipulating the play queue of a player. This class is used by the DWR framework
 * (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxPlayQueueService")
public class PlayQueueService {

    private final PlayerService playerService;
    private final JukeboxService jukeboxService;
    private final SettingsService settingsService;
    private final JpsonicComparators comparators;
    private final MediaFileService mediaFileService;
    private final LastFmService lastFmService;
    private final SecurityService securityService;
    private final SearchService searchService;
    private final RatingService ratingService;
    private final PodcastService podcastService;
    private final PlaylistService playlistService;
    private final MediaFileDao mediaFileDao;
    private final PlayQueueDao playQueueDao;
    private final InternetRadioDao internetRadioDao;
    private final JWTSecurityService jwtSecurityService;
    private final InternetRadioService internetRadioService;

    public PlayQueueService(PlayerService playerService, JukeboxService jukeboxService, SettingsService settingsService,
            JpsonicComparators comparators, MediaFileService mediaFileService, LastFmService lastFmService,
            SecurityService securityService, SearchService searchService, RatingService ratingService,
            PodcastService podcastService, PlaylistService playlistService, MediaFileDao mediaFileDao,
            PlayQueueDao playQueueDao, InternetRadioDao internetRadioDao, JWTSecurityService jwtSecurityService,
            InternetRadioService internetRadioService) {
        super();
        this.playerService = playerService;
        this.jukeboxService = jukeboxService;
        this.settingsService = settingsService;
        this.comparators = comparators;
        this.mediaFileService = mediaFileService;
        this.lastFmService = lastFmService;
        this.securityService = securityService;
        this.searchService = searchService;
        this.ratingService = ratingService;
        this.podcastService = podcastService;
        this.playlistService = playlistService;
        this.mediaFileDao = mediaFileDao;
        this.playQueueDao = playQueueDao;
        this.internetRadioDao = internetRadioDao;
        this.jwtSecurityService = jwtSecurityService;
        this.internetRadioService = internetRadioService;
    }

    /**
     * Returns the play queue for the player of the current user.
     *
     * @return The play queue.
     * 
     * @throws ServletRequestBindingException
     */
    public PlayQueueInfo getPlayQueue() throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        return convert(request, player, false);
    }

    public PlayQueueInfo start() throws ServletRequestBindingException {
        Player player = resolvePlayer();
        player.getPlayQueue().setStatus(PlayQueue.Status.PLAYING);
        if (player.isJukebox()) {
            jukeboxService.start(player);
        }
        return convert(resolveHttpServletRequest(), player, true);
    }

    public PlayQueueInfo stop() throws ServletRequestBindingException {
        Player player = resolvePlayer();
        player.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
        if (player.isJukebox()) {
            jukeboxService.stop(player);
        }
        return convert(resolveHttpServletRequest(), player, true);
    }

    public PlayQueueInfo toggleStartStop() throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doToggleStartStop(request, response);
    }

    public PlayQueueInfo doToggleStartStop(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException {
        Player player = getCurrentPlayer(request, response);
        if (player.getPlayQueue().getStatus() == PlayQueue.Status.STOPPED) {
            player.getPlayQueue().setStatus(PlayQueue.Status.PLAYING);
        } else if (player.getPlayQueue().getStatus() == PlayQueue.Status.PLAYING) {
            player.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
        }
        return convert(request, player, true);
    }

    public PlayQueueInfo skip(int index) throws ServletRequestBindingException {
        return doSkip(index, 0);
    }

    public PlayQueueInfo doSkip(int index, int offset) throws ServletRequestBindingException {
        Player player = resolvePlayer();
        player.getPlayQueue().setIndex(index);
        boolean serverSidePlaylist = !player.isExternalWithPlaylist();
        if (serverSidePlaylist && player.isJukebox()) {
            jukeboxService.skip(player, index, offset);
        }
        return convert(resolveHttpServletRequest(), player, serverSidePlaylist);
    }

    public PlayQueueInfo reloadSearchCriteria() throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        String username = securityService.getCurrentUsername(request);
        Player player = getCurrentPlayer(request, response);
        PlayQueue playQueue = player.getPlayQueue();
        playQueue.setInternetRadio(null);
        if (playQueue.getRandomSearchCriteria() != null) {
            playQueue.addFiles(true, mediaFileService.getRandomSongs(playQueue.getRandomSearchCriteria(), username));
        }
        return convert(request, player, false);
    }

    public void savePlayQueue(int currentSongIndex, long positionMillis) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        Player player = getCurrentPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        PlayQueue playQueue = player.getPlayQueue();
        List<Integer> ids = MediaFile.toIdList(playQueue.getFiles());

        Integer currentId = currentSongIndex == -1 ? null : playQueue.getFile(currentSongIndex).getId();
        SavedPlayQueue savedPlayQueue = new SavedPlayQueue(null, username, ids, currentId, positionMillis, new Date(),
                "Airsonic");
        playQueueDao.savePlayQueue(savedPlayQueue);
    }

    public PlayQueueInfo loadPlayQueue() throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        SavedPlayQueue savedPlayQueue = playQueueDao.getPlayQueue(username);

        if (savedPlayQueue == null) {
            return convert(request, player, false);
        }

        PlayQueue playQueue = player.getPlayQueue();
        playQueue.clear();
        for (Integer mediaFileId : savedPlayQueue.getMediaFileIds()) {
            MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
            if (mediaFile != null) {
                playQueue.addFiles(true, mediaFile);
            }
        }
        PlayQueueInfo result = convert(request, player, false);

        Integer currentId = savedPlayQueue.getCurrentMediaFileId();
        int currentIndex = -1;
        long positionMillis = savedPlayQueue.getPositionMillis() == null ? 0L : savedPlayQueue.getPositionMillis();
        if (currentId != null) {
            MediaFile current = mediaFileService.getMediaFile(currentId);
            currentIndex = playQueue.getFiles().indexOf(current);
            if (currentIndex != -1) {
                result.startPlayerAtAndGetInfo(currentIndex);
                result.startPlayerAtPositionAndGetInfo(positionMillis);
            }
        }

        boolean serverSidePlaylist = !player.isExternalWithPlaylist();
        if (serverSidePlaylist && currentIndex != -1) {
            doSkip(currentIndex, (int) (positionMillis / 1000L));
        }

        return result;
    }

    public PlayQueueInfo play(int id) throws ServletRequestBindingException {
        HttpServletRequest request = resolveHttpServletRequest();
        HttpServletResponse response = resolveHttpServletResponse();

        Player player = getCurrentPlayer(request, response);
        MediaFile file = mediaFileService.getMediaFile(id);

        List<MediaFile> songs;

        if (file.isFile()) {
            String username = securityService.getCurrentUsername(request);
            boolean queueFollowingSongs = settingsService.getUserSettings(username).isQueueFollowingSongs();
            if (queueFollowingSongs) {
                MediaFile dir = mediaFileService.getParentOf(file);
                songs = mediaFileService.getChildrenOf(dir, true, false, true);
                if (!songs.isEmpty()) {
                    int index = songs.indexOf(file);
                    songs = songs.subList(index, songs.size());
                }
            } else {
                songs = Arrays.asList(file);
            }
        } else {
            songs = mediaFileService.getDescendantsOf(file, true);
        }
        return doPlay(request, player, songs).startPlayerAtAndGetInfo(0);
    }

    /**
     * @param startIndex
     *            Start playing at this index in the list of radio streams, or play whole radio playlist if
     *            {@code null}.
     * 
     * @throws ExecutionException
     */
    public PlayQueueInfo playInternetRadio(int id, Integer startIndex)
            throws ServletRequestBindingException, ExecutionException {

        InternetRadio radio = internetRadioDao.getInternetRadioById(id);
        if (!radio.isEnabled()) {
            throw new ExecutionException(new IOException("Radio is not enabled"));
        }

        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        return doPlayInternetRadio(request, resolvePlayer(), radio).startPlayerAtAndGetInfo(0);
    }

    public PlayQueueInfo addPlaylist(int id) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        List<MediaFile> files = playlistService.getFilesInPlaylist(id, true);

        // Remove non-present files
        files.removeIf(file -> !file.isPresent());

        // Add to the play queue
        int[] ids = files.stream().mapToInt(MediaFile::getId).toArray();
        return doAdd(request, response, ids, null);
    }

    /**
     * @param startIndex
     *            Start playing at this index, or play whole playlist if {@code null}.
     * 
     * @throws ServletRequestBindingException
     */
    public PlayQueueInfo playPlaylist(int id, Integer startIndex) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        String username = securityService.getCurrentUsername(request);
        boolean queueFollowingSongs = settingsService.getUserSettings(username).isQueueFollowingSongs();

        List<MediaFile> files = playlistService.getFilesInPlaylist(id, true);
        if (!files.isEmpty() && startIndex != null) {
            if (queueFollowingSongs) {
                files = files.subList(startIndex, files.size());
            } else {
                files = Arrays.asList(files.get(startIndex));
            }
        }

        // Remove non-present files
        files.removeIf(file -> !file.isPresent());

        // Play now
        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).startPlayerAtAndGetInfo(0);
    }

    /**
     * @param startIndex
     *            Start playing at this index, or play all top songs if {@code null}.
     * 
     * @throws ServletRequestBindingException
     */
    public PlayQueueInfo playTopSong(int id, Integer startIndex) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        String username = securityService.getCurrentUsername(request);
        boolean queueFollowingSongs = settingsService.getUserSettings(username).isQueueFollowingSongs();

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> files = lastFmService.getTopSongs(mediaFileService.getMediaFile(id), 50, musicFolders);
        if (!files.isEmpty() && startIndex != null) {
            if (queueFollowingSongs) {
                files = files.subList(startIndex, files.size());
            } else {
                files = Arrays.asList(files.get(startIndex));
            }
        }

        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).startPlayerAtAndGetInfo(0);
    }

    public PlayQueueInfo playPodcastChannel(int id) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        List<PodcastEpisode> episodes = podcastService.getEpisodes(id);
        List<MediaFile> files = new ArrayList<>();
        for (PodcastEpisode episode : episodes) {
            if (episode.getStatus() == PodcastStatus.COMPLETED) {
                MediaFile mediaFile = mediaFileService.getMediaFile(episode.getMediaFileId());
                if (mediaFile != null && mediaFile.isPresent()) {
                    files.add(mediaFile);
                }
            }
        }
        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).startPlayerAtAndGetInfo(0);
    }

    public PlayQueueInfo playPodcastEpisode(int id) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        PodcastEpisode episode = podcastService.getEpisode(id, false);
        List<PodcastEpisode> allEpisodes = podcastService.getEpisodes(episode.getChannelId());
        List<MediaFile> files = new ArrayList<>();

        String username = securityService.getCurrentUsername(request);
        boolean queueFollowingSongs = settingsService.getUserSettings(username).isQueueFollowingSongs();

        for (PodcastEpisode ep : allEpisodes) {
            if (ep.getStatus() == PodcastStatus.COMPLETED) {
                MediaFile mediaFile = mediaFileService.getMediaFile(ep.getMediaFileId());
                if (mediaFile != null && mediaFile.isPresent()
                        && (ep.getId().equals(episode.getId()) || queueFollowingSongs && !files.isEmpty())) {
                    files.add(mediaFile);
                }
            }
        }
        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).startPlayerAtAndGetInfo(0);
    }

    public PlayQueueInfo playNewestPodcastEpisode(Integer startIndex) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        List<PodcastEpisode> episodes = podcastService.getNewestEpisodes(10);
        List<MediaFile> files = Lists.transform(episodes,
                episode -> mediaFileService.getMediaFile(episode.getMediaFileId()));

        String username = securityService.getCurrentUsername(request);
        boolean queueFollowingSongs = settingsService.getUserSettings(username).isQueueFollowingSongs();

        if (!files.isEmpty() && startIndex != null) {
            if (queueFollowingSongs) {
                files = files.subList(startIndex, files.size());
            } else {
                files = Arrays.asList(files.get(startIndex));
            }
        }

        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).startPlayerAtAndGetInfo(0);
    }

    public PlayQueueInfo playStarred() throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> files = mediaFileDao.getStarredFiles(0, Integer.MAX_VALUE, username, musicFolders);
        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).startPlayerAtAndGetInfo(0);
    }

    public PlayQueueInfo playShuffle(String albumListType, int offset, int count, String genre, String decade)
            throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);

        MusicFolder selectedMusicFolder = settingsService.getSelectedMusicFolder(username);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username,
                selectedMusicFolder == null ? null : selectedMusicFolder.getId());
        List<MediaFile> albums;
        if ("highest".equals(albumListType)) {
            albums = ratingService.getHighestRatedAlbums(offset, count, musicFolders);
        } else if ("frequent".equals(albumListType)) {
            albums = mediaFileService.getMostFrequentlyPlayedAlbums(offset, count, musicFolders);
        } else if ("recent".equals(albumListType)) {
            albums = mediaFileService.getMostRecentlyPlayedAlbums(offset, count, musicFolders);
        } else if ("newest".equals(albumListType)) {
            albums = mediaFileService.getNewestAlbums(offset, count, musicFolders);
        } else if ("starred".equals(albumListType)) {
            albums = mediaFileService.getStarredAlbums(offset, count, username, musicFolders);
        } else if ("random".equals(albumListType)) {
            albums = searchService.getRandomAlbums(count, musicFolders);
        } else if ("alphabetical".equals(albumListType)) {
            albums = mediaFileService.getAlphabeticalAlbums(offset, count, true, musicFolders);
        } else if ("decade".equals(albumListType)) {
            int fromYear = Integer.parseInt(decade);
            int toYear = fromYear + 9;
            albums = mediaFileService.getAlbumsByYear(offset, count, fromYear, toYear, musicFolders);
        } else if ("genre".equals(albumListType)) {
            albums = searchService.getAlbumsByGenres(genre, offset, count, musicFolders);
        } else {
            albums = Collections.emptyList();
        }

        List<MediaFile> songs = new ArrayList<>();
        for (MediaFile album : albums) {
            songs.addAll(mediaFileService.getChildrenOf(album, true, false, false));
        }
        Collections.shuffle(songs);
        songs = songs.subList(0, Math.min(40, songs.size()));

        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, songs).startPlayerAtAndGetInfo(0);
    }

    private PlayQueueInfo doPlay(HttpServletRequest request, Player player, List<MediaFile> files) {
        if (player.isWeb()) {
            mediaFileService.removeVideoFiles(files);
        }
        player.getPlayQueue().addFiles(false, files);
        player.getPlayQueue().setRandomSearchCriteria(null);
        player.getPlayQueue().setInternetRadio(null);
        if (player.isJukebox()) {
            jukeboxService.play(player);
        }
        return convert(request, player, true);
    }

    private PlayQueueInfo doPlayInternetRadio(HttpServletRequest request, Player player, InternetRadio radio) {
        internetRadioService.clearInternetRadioSourceCache(radio.getId());
        player.getPlayQueue().clear();
        player.getPlayQueue().setRandomSearchCriteria(null);
        player.getPlayQueue().setInternetRadio(radio);
        if (player.isJukebox()) {
            jukeboxService.play(player);
        }
        return convert(request, player, true);
    }

    public PlayQueueInfo playRandom(int id, int count) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        MediaFile file = mediaFileService.getMediaFile(id);
        List<MediaFile> randomFiles = mediaFileService.getRandomSongsForParent(file, count);
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().addFiles(false, randomFiles);
        player.getPlayQueue().setRandomSearchCriteria(null);
        player.getPlayQueue().setInternetRadio(null);
        return convert(request, player, true).startPlayerAtAndGetInfo(0);
    }

    public PlayQueueInfo playSimilar(int id, int count) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        MediaFile artist = mediaFileService.getMediaFile(id);
        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> similarSongs = lastFmService.getSimilarSongs(artist, count, musicFolders);
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().addFiles(false, similarSongs);
        player.getPlayQueue().setRandomSearchCriteria(null);
        player.getPlayQueue().setInternetRadio(null);
        return convert(request, player, true).startPlayerAtAndGetInfo(0);
    }

    public PlayQueueInfo add(int id) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doAdd(request, response, new int[] { id }, null);
    }

    public PlayQueueInfo addAt(int id, int index) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doAdd(request, response, new int[] { id }, index);
    }

    /**
     * TODO This method should be moved to a real PlayQueueService not dedicated to Ajax DWR.
     * 
     * @param addAtIndex
     *            if not null, insert the media files at the specified index otherwise, append the media files at the
     *            end of the play queue
     */
    public PlayQueue addMediaFilesToPlayQueue(PlayQueue playQueue, int[] ids, Integer addAtIndex,
            boolean removeVideoFiles) {
        List<MediaFile> files = new ArrayList<>(ids.length);
        for (int id : ids) {
            MediaFile ancestor = mediaFileService.getMediaFile(id);
            files.addAll(mediaFileService.getDescendantsOf(ancestor, true));
        }
        if (removeVideoFiles) {
            mediaFileService.removeVideoFiles(files);
        }
        if (addAtIndex == null) {
            playQueue.addFiles(true, files);
        } else {
            playQueue.addFilesAt(files, addAtIndex);
        }
        playQueue.setRandomSearchCriteria(null);
        playQueue.setInternetRadio(null);
        return playQueue;
    }

    /**
     * @param addAtIndex
     *            if not null, insert the media files at the specified index otherwise, append the media files at the
     *            end of the play queue
     */
    public PlayQueueInfo doAdd(HttpServletRequest request, HttpServletResponse response, int[] ids, Integer addAtIndex)
            throws ServletRequestBindingException {
        Player player = getCurrentPlayer(request, response);
        boolean removeVideoFiles = false;
        if (player.isWeb()) {
            removeVideoFiles = true;
        }
        addMediaFilesToPlayQueue(player.getPlayQueue(), ids, addAtIndex, removeVideoFiles);
        return convert(request, player, false);
    }

    /**
     * TODO This method should be moved to a real PlayQueueService not dedicated to Ajax DWR.
     */
    public PlayQueue resetPlayQueue(PlayQueue playQueue, int[] ids, boolean removeVideoFiles) {
        MediaFile currentFile = playQueue.getCurrentFile();

        playQueue.clear();
        addMediaFilesToPlayQueue(playQueue, ids, null, removeVideoFiles);

        int index = currentFile == null ? -1 : playQueue.getFiles().indexOf(currentFile);
        playQueue.setIndex(index);
        playQueue.setStatus(playQueue.getStatus());
        return playQueue;
    }

    public PlayQueueInfo clear() throws ServletRequestBindingException {
        Player player = resolvePlayer();
        player.getPlayQueue().clear();
        boolean serverSidePlaylist = !player.isExternalWithPlaylist();
        return convert(resolveHttpServletRequest(), player, serverSidePlaylist);
    }

    public PlayQueueInfo shuffle() throws ServletRequestBindingException {
        Player player = resolvePlayer();
        player.getPlayQueue().shuffle();
        return convert(resolveHttpServletRequest(), player, false);
    }

    public PlayQueueInfo remove(int index) throws ServletRequestBindingException {
        Player player = resolvePlayer();
        player.getPlayQueue().removeFileAt(index);
        return convert(resolveHttpServletRequest(), player, false);
    }

    public PlayQueueInfo toggleStar(int index) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);

        MediaFile file = player.getPlayQueue().getFile(index);
        String username = securityService.getCurrentUsername(request);
        boolean starred = mediaFileDao.getMediaFileStarredDate(file.getId(), username) != null;
        if (starred) {
            mediaFileDao.unstarMediaFile(file.getId(), username);
        } else {
            mediaFileDao.starMediaFile(file.getId(), username);
        }
        return convert(request, player, false);
    }

    public PlayQueueInfo doRemove(HttpServletRequest request, HttpServletResponse response, int index)
            throws ServletRequestBindingException {
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().removeFileAt(index);
        return convert(request, player, false);
    }

    public PlayQueueInfo removeMany(int... indexes) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        for (int i = indexes.length - 1; i >= 0; i--) {
            player.getPlayQueue().removeFileAt(indexes[i]);
        }
        return convert(request, player, false);
    }

    public PlayQueueInfo rearrange(int... indexes) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().rearrange(indexes);
        return convert(request, player, false);
    }

    public PlayQueueInfo up(int index) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().moveUp(index);
        return convert(request, player, false);
    }

    public PlayQueueInfo down(int index) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().moveDown(index);
        return convert(request, player, false);
    }

    public PlayQueueInfo toggleRepeat() throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        PlayQueue playQueue = player.getPlayQueue();
        if (playQueue.isShuffleRadioEnabled()) {
            playQueue.setRandomSearchCriteria(null);
            playQueue.setRepeatEnabled(false);
        } else {
            playQueue.setRepeatEnabled(!player.getPlayQueue().isRepeatEnabled());
        }
        return convert(request, player, false);
    }

    public PlayQueueInfo undo() throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().undo();
        boolean serverSidePlaylist = !player.isExternalWithPlaylist();
        return convert(request, player, serverSidePlaylist);
    }

    public PlayQueueInfo sortByTrack() throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().sort(comparators.mediaFileOrderBy(TRACK));
        return convert(request, player, false);
    }

    public PlayQueueInfo sortByArtist() throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().sort(comparators.mediaFileOrderBy(ARTIST));
        return convert(request, player, false);
    }

    public PlayQueueInfo sortByAlbum() throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().sort(comparators.mediaFileOrderBy(ALBUM));
        return convert(request, player, false);
    }

    private PlayQueueInfo convert(HttpServletRequest request, Player player, final boolean serverSidePlaylist) {

        PlayQueue playQueue = player.getPlayQueue();

        List<PlayQueueInfo.Entry> entries;
        if (playQueue.isInternetRadioEnabled()) {
            entries = convertInternetRadio(player);
        } else {
            entries = convertMediaFileList(request, player);
        }

        boolean isCurrentPlayer = player.getIpAddress() != null
                && player.getIpAddress().equals(request.getRemoteAddr());
        boolean isStopEnabled = playQueue.getStatus() == PlayQueue.Status.PLAYING && !player.isExternalWithPlaylist();
        boolean m3uSupported = player.isExternal() || player.isExternalWithPlaylist();
        boolean isServerSidePlaylist = player.isAutoControlEnabled() && m3uSupported && isCurrentPlayer
                && serverSidePlaylist;

        float gain = jukeboxService.getGain(player);

        return new PlayQueueInfo(entries, isStopEnabled, playQueue.isRepeatEnabled(), playQueue.isShuffleRadioEnabled(),
                playQueue.isInternetRadioEnabled(), isServerSidePlaylist, gain);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (Entry) Not reusable
    private List<PlayQueueInfo.Entry> convertMediaFileList(HttpServletRequest request, Player player) {

        String url = NetworkUtils.getBaseUrl(request);
        Locale locale = RequestContextUtils.getLocale(request);
        PlayQueue playQueue = player.getPlayQueue();

        List<PlayQueueInfo.Entry> entries = new ArrayList<>();
        for (MediaFile file : playQueue.getFiles()) {

            String albumUrl = url + ViewName.MAIN.value() + "?id=" + file.getId();
            String streamUrl = url + "stream?player=" + player.getId() + "&id=" + file.getId();
            String coverArtUrl = url + ViewName.COVER_ART.value() + "?id=" + file.getId();

            String remoteStreamUrl = jwtSecurityService
                    .addJWTToken(url + "ext/stream?player=" + player.getId() + "&id=" + file.getId());
            String remoteCoverArtUrl = jwtSecurityService
                    .addJWTToken(url + "ext/" + ViewName.COVER_ART.value() + "?id=" + file.getId());

            String format = file.getFormat();
            String username = securityService.getCurrentUsername(request);
            boolean starred = mediaFileService.getMediaFileStarredDate(file.getId(), username) != null;
            entries.add(new PlayQueueInfo.Entry(file.getId(), file.getTrackNumber(), file.getTitle(), file.getArtist(),
                    file.getComposer(), file.getAlbumName(), file.getGenre(), file.getYear(), formatBitRate(file),
                    file.getDurationSeconds(), file.getDurationString(), format, formatContentType(format),
                    formatFileSize(file.getFileSize(), locale), starred, albumUrl, streamUrl, remoteStreamUrl,
                    coverArtUrl, remoteCoverArtUrl));
        }

        return entries;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (Entry) Not reusable
    private List<PlayQueueInfo.Entry> convertInternetRadio(Player player) {

        PlayQueue playQueue = player.getPlayQueue();
        InternetRadio radio = playQueue.getInternetRadio();

        final String radioHomepageUrl = radio.getHomepageUrl();
        final String radioName = radio.getName();

        List<PlayQueueInfo.Entry> entries = new ArrayList<>();
        for (InternetRadioSource streamSource : internetRadioService.getInternetRadioSources(radio)) {
            // Fake entry id so that the source can be selected in the UI
            int streamId = -(1 + entries.size());
            Integer streamTrackNumber = entries.size();
            String streamUrl = streamSource.getStreamUrl();
            entries.add(new PlayQueueInfo.Entry(streamId, // Entry id
                    streamTrackNumber, // Track number
                    streamUrl, // Track title (use radio stream URL for now)
                    "", // Track artist
                    "", // Track composer
                    radioName, // Album name (use radio name)
                    "Internet Radio", // Genre
                    0, // Year
                    "", // Bit rate
                    0, // Duration
                    "", // Duration (as string)
                    "", // Format
                    "", // Content Type
                    "", // File size
                    false, // Starred
                    radioHomepageUrl, // Album URL (use radio home page URL)
                    streamUrl, // Stream URL
                    streamUrl, // Remote stream URL
                    null, // Cover art URL
                    null // Remote cover art URL
            ));
        }

        return entries;
    }

    private String formatFileSize(Long fileSize, Locale locale) {
        if (fileSize == null) {
            return null;
        }
        return StringUtil.formatBytes(fileSize, locale);
    }

    private String formatContentType(String format) {
        return StringUtil.getMimeType(format);
    }

    private String formatBitRate(MediaFile mediaFile) {
        if (mediaFile.getBitRate() == null) {
            return null;
        }
        if (mediaFile.isVariableBitRate()) {
            return mediaFile.getBitRate() + " Kbps vbr";
        }
        return mediaFile.getBitRate() + " Kbps";
    }

    private Player getCurrentPlayer(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException {
        return playerService.getPlayer(request, response);
    }

    private Player resolvePlayer() throws ServletRequestBindingException {
        return getCurrentPlayer(resolveHttpServletRequest(), resolveHttpServletResponse());
    }

    private HttpServletRequest resolveHttpServletRequest() {
        return WebContextFactory.get().getHttpServletRequest();
    }

    private HttpServletResponse resolveHttpServletResponse() {
        return WebContextFactory.get().getHttpServletResponse();
    }

    //
    // Methods dedicated to jukebox
    //

    public void setGain(float gain) throws ServletRequestBindingException {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        if (player != null) {
            jukeboxService.setGain(player, gain);
        }
    }

    public void setJukeboxPosition(int positionInSeconds) throws ServletRequestBindingException {
        Player player = resolvePlayer();
        jukeboxService.setPosition(player, positionInSeconds);
    }
}
