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

package com.tesshu.jpsonic.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.domain.TransferStatus;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.StatusService;
import com.tesshu.jpsonic.upload.MonitoredDiskFileItemFactory;
import com.tesshu.jpsonic.upload.UploadListener;
import com.tesshu.jpsonic.util.LegacyMap;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller which receives uploaded files.
 *
 * @author Sindre Mehus
 */
@org.springframework.stereotype.Controller
@RequestMapping({ "/upload", "/upload.view" })
public class UploadController {

    public static final String FIELD_NAME_DIR = "dir";
    public static final String FIELD_NAME_UNZIP = "unzip";

    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);

    private final SecurityService securityService;
    private final PlayerService playerService;
    private final StatusService statusService;
    private final SettingsService settingsService;

    public UploadController(SecurityService securityService, PlayerService playerService, StatusService statusService,
            SettingsService settingsService) {
        super();
        this.securityService = securityService;
        this.playerService = playerService;
        this.statusService = statusService;
        this.settingsService = settingsService;
    }

    @PostMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {

        TransferStatus status = null;
        UnzipResult result = null;
        Map<String, Object> model = LegacyMap.of();

        try {

            status = statusService.createUploadStatus(playerService.getPlayer(request, response, false, false));
            status.setBytesTotal(request.getContentLength());

            request.getSession().setAttribute(Attributes.Session.UPLOAD_STATUS.value(), status);

            // Check that we have a file upload request
            if (!ServletFileUpload.isMultipartContent(request)) {
                throw new ExecutionException(new IOException("Illegal request."));
            }

            List<FileItem> items = getUploadItems(request, status);

            File dir = getDir(items);
            if (dir == null) {
                throw new IOException("Missing 'dir' parameter.");
            }

            boolean unzip = isUnzip(items);

            result = doUnzip(items, dir, unzip);

        } catch (IOException | FileUploadException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Uploading failed.", e);
            }
            model.put("exception", e);
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            if (LOG.isWarnEnabled()) {
                LOG.warn("Uploading failed.", e);
            }
            model.put("exception", e);
        } finally {
            afterUpload(request, status);
        }

        if (result != null) {
            model.put("uploadedFiles", result.getUploadedFiles());
            model.put("unzippedFiles", result.getUnzippedFiles());
        }

        return new ModelAndView("upload", "model", model);
    }

    private File getDir(List<FileItem> items) {
        for (FileItem item : items) {
            if (item.isFormField() && FIELD_NAME_DIR.equals(item.getFieldName())) {
                return new File(item.getString());
            }
        }
        return null;
    }

    private boolean isUnzip(List<FileItem> items) {
        for (FileItem item : items) {
            if (item.isFormField() && FIELD_NAME_UNZIP.equals(item.getFieldName())) {
                return true;
            }
        }
        return false;
    }

    private List<FileItem> getUploadItems(HttpServletRequest request, TransferStatus status)
            throws FileUploadException {
        UploadListener listener = new UploadListenerImpl(status, statusService, settingsService);
        FileItemFactory factory = new MonitoredDiskFileItemFactory(listener);
        ServletFileUpload upload = new ServletFileUpload(factory);
        return upload.parseRequest(request);
    }

    private void afterUpload(HttpServletRequest request, TransferStatus status) {
        if (status != null) {
            statusService.removeUploadStatus(status);
            request.getSession().removeAttribute(Attributes.Session.UPLOAD_STATUS.value());
            User user = securityService.getCurrentUser(request);
            securityService.updateUserByteCounts(user, 0L, 0L, status.getBytesTransfered());
        }
    }

    private static class UnzipResult {

        private final List<File> uploadedFiles;
        private final List<File> unzippedFiles;

        public UnzipResult(List<File> uploadedFiles, List<File> unzippedFiles) {
            super();
            this.uploadedFiles = uploadedFiles;
            this.unzippedFiles = unzippedFiles;
        }

        public List<File> getUploadedFiles() {
            return uploadedFiles;
        }

        public List<File> getUnzippedFiles() {
            return unzippedFiles;
        }
    }

    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseLocaleWithCaseConversions" })
    /*
     * [AvoidInstantiatingObjectsInLoops] (File, Execution) Not reusable [UseLocaleWithCaseConversions] The locale
     * doesn't matter, as only comparing the extension literal.
     */
    private UnzipResult doUnzip(List<FileItem> items, File dir, boolean unzip) throws ExecutionException {
        List<File> uploadedFiles = new ArrayList<>();
        List<File> unzippedFiles = new ArrayList<>();

        if (!dir.exists() && !dir.mkdirs() && LOG.isWarnEnabled()) {
            LOG.warn("The directory '{}' could not be created.", dir.getAbsolutePath());
        }

        // Look for file items.
        for (FileItem item : items) {
            if (!item.isFormField() && !StringUtils.isAllBlank(item.getName())) {

                File targetFile = new File(dir, new File(item.getName()).getName());
                addUploadedFile(item, targetFile, unzippedFiles);

                if (LOG.isInfoEnabled()) {
                    LOG.info("Uploaded " + targetFile);
                }

                if (unzip && targetFile.getName().toLowerCase().endsWith(".zip")) {
                    unzippedFiles = unzip(targetFile);
                }
            }
        }
        return new UnzipResult(uploadedFiles, unzippedFiles);
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    /*
     * [AvoidCatchingGenericException] Wrap&Throw due to constraints of 'apache commons' {@link FileItem#write(File)}
     */
    private void addUploadedFile(FileItem targetItem, File targetFile, List<File> to) throws ExecutionException {
        if (!securityService.isUploadAllowed(targetFile)) {
            throw new ExecutionException(new GeneralSecurityException(
                    "Permission denied: " + StringEscapeUtils.escapeHtml(targetFile.getPath())));
        }
        try {
            targetItem.write(targetFile);
        } catch (Exception e) {
            throw new ExecutionException("Unable to write item.", e);
        }
        to.add(targetFile);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (File, IOException) Not reusable
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    private List<File> unzip(File file) throws ExecutionException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Unzipping " + file);
        }
        List<File> unzippedFiles = Collections.emptyList();
        try (ZipFile zipFile = new ZipFile(file)) {

            Enumeration<?> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File entryFile = new File(file.getParentFile(), entry.getName());
                if (!entryFile.toPath().normalize().startsWith(file.getParentFile().toPath())) {
                    throw new ExecutionException(
                            new IOException("Bad zip filename: " + StringEscapeUtils.escapeHtml(entryFile.getPath())));
                }
                if (!entry.isDirectory()) {
                    unzippedFiles = unzip(zipFile, entry, entryFile);
                }
            }

            zipFile.close();

            if (!file.delete() && LOG.isWarnEnabled()) {
                LOG.warn("The file '{}' could not be deleted.", file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new ExecutionException("Can't unzip.", e);
        }
        return unzippedFiles;
    }

    private List<File> unzip(ZipFile zipFile, ZipEntry entry, File entryFile) throws ExecutionException {

        if (!securityService.isUploadAllowed(entryFile)) {
            throw new ExecutionException(new GeneralSecurityException(
                    "Permission denied: " + StringEscapeUtils.escapeHtml(entryFile.getPath())));
        }

        if (!entryFile.getParentFile().mkdirs() && LOG.isWarnEnabled()) {
            LOG.warn("The directory '{}' could not be created.", entryFile.getParentFile().getAbsolutePath());
        }

        List<File> unzippedFiles = new ArrayList<>();
        try (OutputStream outputStream = Files.newOutputStream(Paths.get(entryFile.toURI()));
                InputStream inputStream = zipFile.getInputStream(entry)) {
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
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
        return unzippedFiles;
    }

    /**
     * Receives callbacks as the file upload progresses.
     */
    private static class UploadListenerImpl implements UploadListener {

        private final TransferStatus status;
        private final StatusService statusService;
        private final SettingsService settingsService;
        private final long startTime;

        private static final Logger LOG = LoggerFactory.getLogger(UploadListenerImpl.class);

        UploadListenerImpl(TransferStatus status, StatusService statusService, SettingsService settingsService) {
            this.status = status;
            this.statusService = statusService;
            this.settingsService = settingsService;
            this.startTime = System.currentTimeMillis();
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

            try {
                doSleep(maxBitsPerSecond, bitCount, elapsedSeconds);
            } catch (InterruptedException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to sleep.", e);
                }
            }
        }

        private void doSleep(long maxBitsPerSecond, long bitCount, float elapsedSeconds) throws InterruptedException {
            if (maxBitsPerSecond > 0) {
                float sleepMillis = bitCount * 1000.0F / (maxBitsPerSecond - elapsedSeconds) * 1000.0F;
                if (sleepMillis > 0) {
                    Thread.sleep((long) sleepMillis);
                }
            }
        }

        private long getBitrateLimit() {
            return 1024L * this.settingsService.getUploadBitrateLimit()
                    / Math.max(1, this.statusService.getAllUploadStatuses().size());
        }
    }

}
