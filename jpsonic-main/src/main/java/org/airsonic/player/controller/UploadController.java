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
package org.airsonic.player.controller;

import com.tesshu.jpsonic.SuppressFBWarnings;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.StatusService;
import org.airsonic.player.upload.MonitoredDiskFileItemFactory;
import org.airsonic.player.upload.UploadListener;
import org.airsonic.player.util.LegacyMap;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Controller which receives uploaded files.
 *
 * @author Sindre Mehus
 */
@org.springframework.stereotype.Controller
@RequestMapping("/upload")
public class UploadController {

    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private StatusService statusService;
    @Autowired
    private SettingsService settingsService;
    public static final String UPLOAD_STATUS = "uploadStatus";
    public static final String FIELD_NAME_DIR = "dir";
    public static final String FIELD_NAME_UNZIP = "unzip";

    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseLocaleWithCaseConversions" })
    /*
     * [AvoidInstantiatingObjectsInLoops]
     * (File, GeneralSecurityException) Not reusable
     * [UseLocaleWithCaseConversions]
     * The locale doesn't matter, as only comparing the extension literal.
     */
    @PostMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {

        Map<String, Object> map = LegacyMap.of();
        List<File> uploadedFiles = new ArrayList<>();
        List<File> unzippedFiles = new ArrayList<>();
        TransferStatus status = null;

        try {

            status = statusService.createUploadStatus(playerService.getPlayer(request, response, false, false));
            status.setBytesTotal(request.getContentLength());

            request.getSession().setAttribute(UPLOAD_STATUS, status);

            // Check that we have a file upload request
            if (!ServletFileUpload.isMultipartContent(request)) {
                throw new ExecutionException(new IOException("Illegal request."));
            }

            File dir = null;
            boolean unzip = false;

            UploadListener listener = new UploadListenerImpl(status);

            FileItemFactory factory = new MonitoredDiskFileItemFactory(listener);
            ServletFileUpload upload = new ServletFileUpload(factory);

            List<FileItem> items = upload.parseRequest(request);

            // First, look for "dir" and "unzip" parameters.
            for (FileItem item : items) {
                if (item.isFormField() && FIELD_NAME_DIR.equals(item.getFieldName())) {
                    dir = new File(item.getString());
                } else if (item.isFormField() && FIELD_NAME_UNZIP.equals(item.getFieldName())) {
                    unzip = true;
                }
            }

            if (dir == null) {
                throw new ExecutionException(new IOException("Missing 'dir' parameter."));
            }

            // Look for file items.
            for (Object o : items) {
                FileItem item = (FileItem) o;

                if (!item.isFormField()) {
                    if (!StringUtils.isAllBlank(item.getName())) {

                        File targetFile = new File(dir, new File(item.getName()).getName());

                        if (!securityService.isUploadAllowed(targetFile)) {
                            throw new ExecutionException(new GeneralSecurityException("Permission denied: " + StringEscapeUtils.escapeHtml(targetFile.getPath())));
                        }

                        if (!dir.exists()) {
                            if (!dir.mkdirs() && LOG.isWarnEnabled()) {
                                LOG.warn("The directory '{}' could not be created.", dir.getAbsolutePath());
                            }
                        }

                        item.write(targetFile);
                        uploadedFiles.add(targetFile);
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Uploaded " + targetFile);
                        }

                        if (unzip && targetFile.getName().toLowerCase().endsWith(".zip")) {
                            unzip(targetFile, unzippedFiles);
                        }
                    }
                }
            }

        } catch (Exception x) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Uploading failed.", x);
            }
            map.put("exception", x);
        } finally {
            if (status != null) {
                statusService.removeUploadStatus(status);
                request.getSession().removeAttribute(UPLOAD_STATUS);
                User user = securityService.getCurrentUser(request);
                securityService.updateUserByteCounts(user, 0L, 0L, status.getBytesTransfered());
            }
        }

        map.put("uploadedFiles", uploadedFiles);
        map.put("unzippedFiles", unzippedFiles);

        return new ModelAndView("upload","model",map);
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") //  (File, IOException, GeneralSecurityException, byte[]) Not reusable
    private void unzip(File file, List<File> unzippedFiles) throws Exception {
        if (LOG.isInfoEnabled()) {
            LOG.info("Unzipping " + file);
        }

        try (ZipFile zipFile = new ZipFile(file)) {

            Enumeration<?> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File entryFile = new File(file.getParentFile(), entry.getName());
                if (!entryFile.toPath().normalize().startsWith(file.getParentFile().toPath())) {
                    throw new ExecutionException(new IOException("Bad zip filename: " + StringEscapeUtils.escapeHtml(entryFile.getPath())));
                }

                if (!entry.isDirectory()) {

                    if (!securityService.isUploadAllowed(entryFile)) {
                        throw new ExecutionException(new GeneralSecurityException("Permission denied: " + StringEscapeUtils.escapeHtml(entryFile.getPath())));
                    }

                    if (!entryFile.getParentFile().mkdirs() && LOG.isWarnEnabled()) {
                        LOG.warn("The directory '{}' could not be created.",
                                entryFile.getParentFile().getAbsolutePath());
                    }

                    try (
                            OutputStream outputStream = Files.newOutputStream(Paths.get(entryFile.toURI()));
                            InputStream inputStream = zipFile.getInputStream(entry)
                    ) {
                        byte[] buf = new byte[8192];
                        while (true) {
                            int n = inputStream.read(buf);
                            if (n == -1) {
                                break;
                            }
                            outputStream.write(buf, 0, n);
                        }
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Unzipped " + entryFile);
                        }
                        unzippedFiles.add(entryFile);
                    }
                }
            }

            zipFile.close();
            if (!file.delete() && LOG.isWarnEnabled()) {
                LOG.warn("The file '{}' could not be deleted.", file.getAbsolutePath());
            }
        }
    }





    /**
     * Receives callbacks as the file upload progresses.
     */
    @SuppressWarnings("PMD.AccessorMethodGeneration")
    private class UploadListenerImpl implements UploadListener {
        private TransferStatus status;
        private long startTime;

        UploadListenerImpl(TransferStatus status) {
            this.status = status;
            startTime = System.currentTimeMillis();
        }

        @Override
        public void start(String fileName) {
            status.setFile(new File(fileName));
        }

        @Override
        public void bytesRead(long bytesRead) {

            // Throttle bitrate.

            long byteCount = status.getBytesTransfered() + bytesRead;
            long bitCount = byteCount * 8L;

            float elapsedMillis = Math.max(1, System.currentTimeMillis() - startTime);
            float elapsedSeconds = elapsedMillis / 1000.0F;
            long maxBitsPerSecond = getBitrateLimit();

            status.setBytesTransfered(byteCount);

            if (maxBitsPerSecond > 0) {
                float sleepMillis = bitCount * 1000.0F / (maxBitsPerSecond - elapsedSeconds) * 1000.0F;
                if (sleepMillis > 0) {
                    try {
                        Thread.sleep((long) sleepMillis);
                    } catch (InterruptedException x) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Failed to sleep.", x);
                        }
                    }
                }
            }
        }

        private long getBitrateLimit() {
            return 1024L * settingsService.getUploadBitrateLimit() / Math.max(1, statusService.getAllUploadStatuses().size());
        }
    }

}
