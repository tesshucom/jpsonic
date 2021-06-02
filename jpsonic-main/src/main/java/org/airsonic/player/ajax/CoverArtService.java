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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.airsonic.player.domain.LastFmCoverArt;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.LastFmService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SecurityService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provides AJAX-enabled services for changing cover art images.
 * <p/>
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxCoverArtService")
public class CoverArtService {

    private static final Logger LOG = LoggerFactory.getLogger(CoverArtService.class);

    private final SecurityService securityService;
    private final MediaFileService mediaFileService;
    private final LastFmService lastFmService;

    public CoverArtService(SecurityService securityService, MediaFileService mediaFileService,
            LastFmService lastFmService) {
        super();
        this.securityService = securityService;
        this.mediaFileService = mediaFileService;
        this.lastFmService = lastFmService;
    }

    public List<LastFmCoverArt> searchCoverArt(String artist, String album) {
        return lastFmService.searchCoverArt(artist, album);
    }

    /**
     * Downloads and saves the cover art at the given URL.
     *
     * @param albumId
     *            ID of the album in question.
     * @param url
     *            The image URL.
     * 
     * @return The error string if something goes wrong, <code>null</code> otherwise.
     */
    public String saveCoverArtImage(int albumId, String url) {
        try {
            MediaFile mediaFile = mediaFileService.getMediaFile(albumId);
            saveCoverArt(mediaFile.getPath(), url);
            return null;
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to save cover art for album " + albumId, e);
            }
            return e.toString();
        }
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    private void saveCoverArt(String path, String url) throws ExecutionException {

        // Attempt to resolve proper suffix.
        String suffix = getProperSuffix(url);

        // Check permissions.
        File newCoverFile = new File(path, "cover." + suffix);
        if (!securityService.isWriteAllowed(newCoverFile)) {
            throw new ExecutionException(new GeneralSecurityException(
                    "Permission denied: " + StringEscapeUtils.escapeHtml(newCoverFile.getPath())));
        }

        // If file exists, create a backup.
        backup(newCoverFile, new File(path, "cover." + suffix + ".backup"));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2000).setSocketTimeout(2000).build();
            HttpGet method = new HttpGet(url);
            method.setConfig(requestConfig);

            try (InputStream input = client.execute(method).getEntity().getContent()) {

                // Write file.
                try (OutputStream output = Files.newOutputStream(Paths.get(newCoverFile.toURI()))) {
                    IOUtils.copy(input, output);
                    input.close();
                }

                MediaFile dir = mediaFileService.getMediaFile(path);

                // Refresh database.
                mediaFileService.refreshMediaFile(dir);
                dir = mediaFileService.getMediaFile(dir.getId());

                // Rename existing cover files if new cover file is not the preferred.
                renameWithoutReplacement(dir, newCoverFile);
            }
        } catch (UnsupportedOperationException | IOException e) {
            throw new ExecutionException("Failed to save coverArt: " + path, e);
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (File) Not reusable
    private void renameWithoutReplacement(MediaFile mediaFile, File newCoverFile) throws IOException {
        MediaFile dir = mediaFile;
        while (true) {
            File coverFile = mediaFileService.getCoverArt(dir);
            if (coverFile == null || isMediaFile(coverFile) || newCoverFile.equals(coverFile)) {
                break;
            }

            boolean renamed = coverFile.renameTo(new File(coverFile.getCanonicalPath() + ".old"));
            if (!renamed && LOG.isWarnEnabled()) {
                LOG.warn("Unable to rename old image file " + coverFile);
            }
            if (renamed && LOG.isInfoEnabled()) {
                LOG.info("Renamed old image file " + coverFile);
            }
            if (renamed) {
                // Must refresh again.
                mediaFileService.refreshMediaFile(dir);
                dir = mediaFileService.getMediaFile(dir.getId());
            }
        }
    }

    private String getProperSuffix(String url) {
        String suffix = "jpg";
        if (url.toLowerCase(Locale.getDefault()).endsWith(".gif")) {
            suffix = "gif";
        } else if (url.toLowerCase(Locale.getDefault()).endsWith(".png")) {
            suffix = "png";
        }
        return suffix;
    }

    private boolean isMediaFile(File file) {
        return mediaFileService.includeMediaFile(file);
    }

    private void backup(File newCoverFile, File backup) {
        if (newCoverFile.exists()) {
            if (backup.exists() && !backup.delete() && LOG.isWarnEnabled()) {
                LOG.warn("Failed to delete " + backup);
            }
            if (newCoverFile.renameTo(backup) && LOG.isInfoEnabled()) {
                LOG.info("Backed up old image file to " + backup);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to create image file backup " + backup);
            }
        }
    }
}
