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
package org.airsonic.player.ajax;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    @Autowired
    private SecurityService securityService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private LastFmService lastFmService;

    public List<LastFmCoverArt> searchCoverArt(String artist, String album) {
        return lastFmService.searchCoverArt(artist, album);
    }

    /**
     * Downloads and saves the cover art at the given URL.
     *
     * @param albumId ID of the album in question.
     * @param url  The image URL.
     * @return The error string if something goes wrong, <code>null</code> otherwise.
     */
    public String setCoverArtImage(int albumId, String url) {
        try {
            MediaFile mediaFile = mediaFileService.getMediaFile(albumId);
            saveCoverArt(mediaFile.getPath(), url);
            return null;
        } catch (Exception x) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to save cover art for album " + albumId, x);
            }
            return x.toString();
        }
    }

    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseLocaleWithCaseConversions" })
    private void saveCoverArt(String path, String url) throws Exception {

        // Attempt to resolve proper suffix.
        String suffix = "jpg";
        if (url.toLowerCase().endsWith(".gif")) {
            suffix = "gif";
        } else if (url.toLowerCase().endsWith(".png")) {
            suffix = "png";
        }

        // Check permissions.
        File newCoverFile = new File(path, "cover." + suffix);
        if (!securityService.isWriteAllowed(newCoverFile)) {
            throw new ExecutionException(new GeneralSecurityException(
                    "Permission denied: " + StringEscapeUtils.escapeHtml(newCoverFile.getPath())));
        }

        // If file exists, create a backup.
        backup(newCoverFile, new File(path, "cover." + suffix + ".backup"));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(2000)
                    .setSocketTimeout(2000)
                    .build();
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
                try {
                    while (true) {
                        File coverFile = mediaFileService.getCoverArt(dir);
                        if (coverFile != null && !isMediaFile(coverFile) && !newCoverFile.equals(coverFile)) {
                            if (!coverFile.renameTo(new File(coverFile.getCanonicalPath() + ".old"))) {
                                if (LOG.isWarnEnabled()) {
                                    LOG.warn("Unable to rename old image file " + coverFile);
                                }
                                break;
                            }
                            if (LOG.isInfoEnabled()) {
                                LOG.info("Renamed old image file " + coverFile);
                            }

                            // Must refresh again.
                            mediaFileService.refreshMediaFile(dir);
                            dir = mediaFileService.getMediaFile(dir.getId());
                        } else {
                            break;
                        }
                    }
                } catch (Exception x) {
                    LOG.warn("Failed to rename existing cover file.", x);
                }
            }
        }
    }

    private boolean isMediaFile(File file) {
        return mediaFileService.includeMediaFile(file);
    }

    private void backup(File newCoverFile, File backup) {
        if (newCoverFile.exists()) {
            if (backup.exists()) {
                if (!backup.delete()) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Failed to delete " + backup);
                    }
                }
            }
            if (newCoverFile.renameTo(backup)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Backed up old image file to " + backup);
                }
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to create image file backup " + backup);
                }
            }
        }
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setLastFmService(LastFmService lastFmService) {
        this.lastFmService = lastFmService;
    }
}
