/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayStatus;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

/**
 * Provides services for maintaining the list of stream, download and upload statuses.
 * <p/>
 * Note that for stream statuses, the last inactive status is also stored.
 *
 * @author Sindre Mehus
 * @see TransferStatus
 */
@SuppressWarnings("PMD.UseConcurrentHashMap") /* LinkedHashMap used in Legacy code */
@Service
@DependsOn("mediaFileService")
public class StatusService {

    private final MediaFileService mediaFileService;

    public StatusService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    private final List<TransferStatus> streamStatuses = new ArrayList<>();
    private final List<TransferStatus> downloadStatuses = new ArrayList<>();
    private final List<TransferStatus> uploadStatuses = new ArrayList<>();
    private final List<PlayStatus> remotePlays = new ArrayList<>();

    private static final Object STREAM_LOCK = new Object();
    private static final Object DOWNLOAD_LOCK = new Object();
    private static final Object UPLOAD_LOCK = new Object();
    private static final Object REMOTE_LOCK = new Object();

    // Maps from player ID to latest inactive stream status.
    private final Map<Integer, TransferStatus> inactiveStreamStatuses = new LinkedHashMap<>();

    public TransferStatus createStreamStatus(Player player) {
        synchronized (STREAM_LOCK) {
            // Reuse existing status, if possible.
            TransferStatus status = inactiveStreamStatuses.get(player.getId());
            if (status != null) {
                status.setActive(true);
            } else {
                status = createStatus(player, streamStatuses);
            }
            return status;
        }
    }

    public void removeStreamStatus(TransferStatus status) {
        synchronized (STREAM_LOCK) {
            // Move it to the map of inactive statuses.
            status.setActive(false);
            inactiveStreamStatuses.put(status.getPlayer().getId(), status);
            streamStatuses.remove(status);
        }
    }

    public List<TransferStatus> getAllStreamStatuses() {
        synchronized (STREAM_LOCK) {

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
        synchronized (STREAM_LOCK) {
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
        synchronized (DOWNLOAD_LOCK) {
            return createStatus(player, downloadStatuses);
        }
    }

    public void removeDownloadStatus(TransferStatus status) {
        synchronized (DOWNLOAD_LOCK) {
            downloadStatuses.remove(status);
        }
    }

    public List<TransferStatus> getAllDownloadStatuses() {
        synchronized (DOWNLOAD_LOCK) {
            return unmodifiableList(downloadStatuses);
        }
    }

    public TransferStatus createUploadStatus(Player player) {
        synchronized (UPLOAD_LOCK) {
            return createStatus(player, uploadStatuses);
        }
    }

    public void removeUploadStatus(TransferStatus status) {
        synchronized (UPLOAD_LOCK) {
            uploadStatuses.remove(status);
        }
    }

    public List<TransferStatus> getAllUploadStatuses() {
        synchronized (UPLOAD_LOCK) {
            return unmodifiableList(uploadStatuses);
        }
    }

    public void addRemotePlay(PlayStatus playStatus) {
        synchronized (REMOTE_LOCK) {
            remotePlays.removeIf(PlayStatus::isExpired);
            remotePlays.add(playStatus);
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (Date, PlayStatus) Not reusable
    public List<PlayStatus> getPlayStatuses() {
        synchronized (STREAM_LOCK) {
            synchronized (REMOTE_LOCK) {

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
                    File file = streamStatus.getFile();
                    if (file == null) {
                        continue;
                    }
                    MediaFile mediaFile = mediaFileService.getMediaFile(file);
                    if (player == null || mediaFile == null) {
                        continue;
                    }
                    Date time = new Date(System.currentTimeMillis() - streamStatus.getMillisSinceLastUpdate());
                    result.put(player.getId(), new PlayStatus(mediaFile, player, time));
                }
                return unmodifiableList(result.values().stream().collect(Collectors.toList()));
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
