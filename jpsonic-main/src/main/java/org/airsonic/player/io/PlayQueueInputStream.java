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

package org.airsonic.player.io;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.domain.VideoTranscodingSettings;
import org.airsonic.player.service.AudioScrobblerService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.service.sonos.SonosHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Implementation of {@link InputStream} which reads from a {@link PlayQueue}.
 *
 * @author Sindre Mehus
 */
public class PlayQueueInputStream extends InputStream {

    private static final Logger LOG = LoggerFactory.getLogger(PlayQueueInputStream.class);

    private final Player player;
    private final TransferStatus status;
    private final TranscodingService.Parameters transParam;
    private final TranscodingService transcodingService;
    private final AudioScrobblerService audioScrobblerService;
    private final MediaFileService mediaFileService;
    private final SearchService searchService;
    private final SettingsService settingsService;
    private final AsyncTaskExecutor executor;

    private AtomicReference<MediaFile> currentFile;
    private AtomicReference<InputStream> delegate;

    public PlayQueueInputStream(Player player, TransferStatus status, Integer maxBitRate, String preferredTargetFormat,
            VideoTranscodingSettings videoTranscodingSettings, TranscodingService transcodingService,
            AudioScrobblerService audioScrobblerService, MediaFileService mediaFileService, SearchService searchService,
            SettingsService settingsService, AsyncTaskExecutor executor) {
        super();
        this.player = player;
        this.status = status;
        this.transcodingService = transcodingService;
        this.audioScrobblerService = audioScrobblerService;
        this.mediaFileService = mediaFileService;
        this.searchService = searchService;
        this.settingsService = settingsService;
        this.executor = executor;
        transParam = transcodingService.getParameters(player.getPlayQueue().getCurrentFile(), player, maxBitRate,
                preferredTargetFormat, videoTranscodingSettings);
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int n = read(b);
        return n == -1 ? -1 : b[0];
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {

        if (isEmpty(currentFile)) {
            // Prepare currentInputStream.
            Future<Boolean> prepare = executor.submit(new Prepare());
            try {
                boolean isPrepare = prepare.get();
                if (!isPrepare) {
                    return -1;
                }
            } catch (InterruptedException e) {
                LOG.error("Transcoding was interrupted.", e);
                return -1;
            } catch (ExecutionException e) {
                LOG.error("Error during transcoding.", e);
                return -1;
            }
        }

        // If end of song reached, skip to next song and call read() again.
        int n = delegate.get().read(b, off, len);
        if (n == -1) {
            player.getPlayQueue().next();
            internalClose();
            return read(b, off, len);
        } else {
            status.addBytesTransfered(n);
        }
        return n;
    }

    private class Prepare implements Callable<Boolean> {

        @Override
        public Boolean call() {
            PlayQueue playQueue = player.getPlayQueue();

            // If playlist is in auto-random mode, populate it with new random songs.
            if (playQueue.getIndex() == -1 && !isEmpty(playQueue.getRandomSearchCriteria())) {
                populateRandomPlaylist(playQueue);
            }

            MediaFile file = playQueue.getCurrentFile();
            if (isEmpty(file)) {
                internalClose();
                return false;
            }

            if (isEmpty(currentFile) || !file.equals(currentFile.get())) {

                internalClose();
                scrobble();
                mediaFileService.incrementPlayCount(file);
                writeLog(file);

                try {
                    delegate = new AtomicReference<>(transcodingService.getTranscodedInputStream(transParam));
                    if (!isEmpty(delegate) || player.getPlayQueue().getStatus() != PlayQueue.Status.STOPPED) {
                        currentFile = new AtomicReference<>(file);
                        status.setFile(currentFile.get().getFile());
                        return true;
                    }
                } catch (IOException e) {
                    LOG.error("Unable to get transcode output.", e);
                }
            }
            return false;
        }
    }

    private void writeLog(MediaFile file) {
        if (settingsService.isVerboseLogPlaying() && LOG.isInfoEnabled()) {
            String address = player.getIpAddress();
            String user = player.getUsername();
            String title = file.getTitle();
            String thread = Thread.currentThread().getName();
            LOG.info("{}({}): Transcoding {} in {}", address, user, title, thread);
        }
    }

    protected void populateRandomPlaylist(PlayQueue playQueue) {
        List<MediaFile> files = searchService.getRandomSongs(playQueue.getRandomSearchCriteria());
        playQueue.addFiles(false, files);
        if (LOG.isInfoEnabled()) {
            LOG.info("Recreated random playlist with " + playQueue.size() + " songs.");
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (!isEmpty(delegate)) {
                delegate.get().close();
            }
        } finally {
            closeAfter();
        }
    }

    /*
     * If the closing process performed in this class fails, it can hardly be restored by bubbling to a higher
     * level(file have been moved, etc?).
     */
    private void internalClose() {
        try {
            if (!isEmpty(delegate)) {
                delegate.get().close();
            }
        } catch (IOException e) {
            LOG.error("Unable to close stream currently in use.", e);
        } finally {
            closeAfter();
        }
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void closeAfter() {
        scrobble();
        delegate = null;
        currentFile = null;
    }

    private void scrobble() {
        // Don't scrobble REST players (except Sonos)
        if (!isEmpty(currentFile)
                && (isEmpty(player.getClientId()) || player.getClientId().equals(SonosHelper.JPSONIC_CLIENT_ID))) {
            audioScrobblerService.register(currentFile.get(), player.getUsername(), true, null);
        }
    }
}
