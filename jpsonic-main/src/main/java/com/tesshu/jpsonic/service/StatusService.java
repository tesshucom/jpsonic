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

import static java.util.Collections.unmodifiableList;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayStatus;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TransferStatus;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * Provides services for maintaining the list of stream, download and upload statuses.
 * <p/>
 * Note that for stream statuses, the last inactive status is also stored.
 *
 * @author Sindre Mehus
 *
 * @see TransferStatus
 */
@SuppressWarnings("PMD.UseConcurrentHashMap")
/*
 * LinkedHashMap used in Legacy code. Should be triaged in #831.
 */
@Service
@DependsOn("mediaFileService")
public class StatusService {

    private final MediaFileService mediaFileService;
    private final transient List<TransferStatus> streamStatuses;
    private final transient List<TransferStatus> downloadStatuses;
    private final transient List<TransferStatus> uploadStatuses;
    private final transient List<PlayStatus> remotePlays;

    // Maps from player ID to latest inactive stream status.
    private final Map<Integer, TransferStatus> inactiveStreamStatuses;

    private final Object streamLock = new Object();
    private final Object downloadLock = new Object();
    private final Object uploadLock = new Object();
    private final Object remotelock = new Object();

    public StatusService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
        streamStatuses = new ArrayList<>();
        downloadStatuses = new ArrayList<>();
        uploadStatuses = new ArrayList<>();
        remotePlays = new ArrayList<>();
        inactiveStreamStatuses = new LinkedHashMap<>();
    }

    public TransferStatus createStreamStatus(Player player) {
        synchronized (streamLock) {
            // Reuse existing status, if possible.
            TransferStatus status = inactiveStreamStatuses.get(player.getId());
            if (status == null) {
                status = createStatus(player, streamStatuses);
            } else {
                status.setActive(true);
            }
            return status;
        }
    }

    public void removeStreamStatus(TransferStatus status) {
        synchronized (streamLock) {
            // Move it to the map of inactive statuses.
            status.setActive(false);
            inactiveStreamStatuses.put(status.getPlayer().getId(), status);
            streamStatuses.remove(status);
        }
    }

    public List<TransferStatus> getAllStreamStatuses() {
        synchronized (streamLock) {

            List<TransferStatus> result = new ArrayList<>(streamStatuses);

            // Add inactive status for those players that have no active status.
            Set<Integer> activePlayers = new HashSet<>();
            for (TransferStatus status : streamStatuses) {
                activePlayers.add(status.getPlayer().getId());
            }

            for (Map.Entry<Integer, TransferStatus> entry : inactiveStreamStatuses.entrySet()) {
                if (!activePlayers.contains(entry.getKey())) {
                    result.add(entry.getValue());
                }
            }
            return unmodifiableList(result);
        }
    }

    public List<TransferStatus> getStreamStatusesForPlayer(Player player) {
        synchronized (streamLock) {
            List<TransferStatus> result = new ArrayList<>();
            for (TransferStatus status : streamStatuses) {
                if (status.getPlayer().getId().equals(player.getId())) {
                    result.add(status);
                }
            }

            // If no active statuses exists, add the inactive one.
            if (result.isEmpty()) {
                TransferStatus inactiveStatus = inactiveStreamStatuses.get(player.getId());
                if (inactiveStatus != null) {
                    result.add(inactiveStatus);
                }
            }

            return unmodifiableList(result);
        }
    }

    public TransferStatus createDownloadStatus(Player player) {
        synchronized (downloadLock) {
            return createStatus(player, downloadStatuses);
        }
    }

    public void removeDownloadStatus(TransferStatus status) {
        synchronized (downloadLock) {
            downloadStatuses.remove(status);
        }
    }

    public List<TransferStatus> getAllDownloadStatuses() {
        synchronized (downloadLock) {
            return unmodifiableList(downloadStatuses);
        }
    }

    public TransferStatus createUploadStatus(Player player) {
        synchronized (uploadLock) {
            return createStatus(player, uploadStatuses);
        }
    }

    public void removeUploadStatus(TransferStatus status) {
        synchronized (uploadLock) {
            uploadStatuses.remove(status);
        }
    }

    public List<TransferStatus> getAllUploadStatuses() {
        synchronized (uploadLock) {
            return unmodifiableList(uploadStatuses);
        }
    }

    public void addRemotePlay(PlayStatus playStatus) {
        synchronized (remotelock) {
            remotePlays.removeIf(PlayStatus::isExpired);
            remotePlays.add(playStatus);
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (Date, PlayStatus) Not reusable
    public List<PlayStatus> getPlayStatuses() {
        synchronized (streamLock) {
            synchronized (remotelock) {

                Map<Integer, PlayStatus> result = new LinkedHashMap<>();
                for (PlayStatus remotePlay : remotePlays) {
                    if (!remotePlay.isExpired()) {
                        result.put(remotePlay.getPlayer().getId(), remotePlay);
                    }
                }

                List<TransferStatus> statuses = new ArrayList<>();
                statuses.addAll(inactiveStreamStatuses.values());
                statuses.addAll(streamStatuses);

                for (TransferStatus streamStatus : statuses) {
                    Player player = streamStatus.getPlayer();
                    Path path = streamStatus.toPath();
                    if (path == null) {
                        continue;
                    }
                    MediaFile mediaFile = mediaFileService.getMediaFile(path);
                    if (player == null || mediaFile == null) {
                        continue;
                    }
                    Instant time = Instant
                            .ofEpochMilli(Instant.now().toEpochMilli() - streamStatus.getMillisSinceLastUpdate());
                    result.put(player.getId(), new PlayStatus(mediaFile, player, time));
                }
                return unmodifiableList(new ArrayList<>(result.values()));
            }
        }
    }

    private TransferStatus createStatus(Player player, List<TransferStatus> statusList) {
        TransferStatus status = new TransferStatus();
        status.setPlayer(player);
        statusList.add(status);
        return status;
    }

}
