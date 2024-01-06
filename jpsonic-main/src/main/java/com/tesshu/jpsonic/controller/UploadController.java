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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.SuppressLint;
import com.tesshu.jpsonic.domain.TransferStatus;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.StatusService;
import com.tesshu.jpsonic.upload.UploadListener;
import com.tesshu.jpsonic.util.FileUtil;
import com.tesshu.jpsonic.util.LegacyMap;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.JakartaServletDiskFileUpload;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller which receives uploaded files.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/upload", "/upload.view" })
public class UploadController {

    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);
    public static final String FIELD_NAME_DIR = "dir";
    public static final String FIELD_NAME_UNZIP = "unzip";
    private static final String NOT_ALLOWED_MSG = "The root path is other than the Musicfolder, or the file already exists: ";

    private final SecurityService securityService;
    private final PlayerService playerService;
    private final StatusService statusService;
    private final SettingsService settingsService;
    private final ScannerStateService scannerStateService;

    public UploadController(SecurityService securityService, PlayerService playerService, StatusService statusService,
            SettingsService settingsService, ScannerStateService scannerStateService) {
        super();
        this.securityService = securityService;
        this.playerService = playerService;
        this.statusService = statusService;
        this.settingsService = settingsService;
        this.scannerStateService = scannerStateService;
    }

    @PostMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {

        Map<String, Object> model = LegacyMap.of();
        if (scannerStateService.isScanning()) {
            model.put("exception", new IllegalArgumentException("Currently scanning. Please try again after a while."));
            return new ModelAndView("upload", "model", model);
        }

        TransferStatus status = null;
        UnzipResult result = null;
        try {

            status = statusService.createUploadStatus(playerService.getPlayer(request, response, false, false));
            status.setBytesTotal(request.getContentLength());

            request.getSession().setAttribute(Attributes.Session.UPLOAD_STATUS.value(), status);

            // Check that we have a file upload request
            if (!JakartaServletFileUpload.isMultipartContent(request)) {
                throw new ExecutionException(new IOException("Illegal request."));
            }

            List<DiskFileItem> items = getUploadItems(request, status);
            Path dir = getDir(items);
            boolean unzip = isUnzip(items);
            result = doUnzip(items, dir, unzip);

        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Uploading failed. {}", e.getMessage());
            }
            model.put("exception", e);
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            if (LOG.isWarnEnabled()) {
                LOG.warn("Uploading failed. {}", e.getMessage());
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

    @SuppressWarnings("PMD.UseIOStreamsWithApacheCommonsFileItem") // #1539
    private @NonNull Path getDir(List<DiskFileItem> items) throws IOException {
        for (DiskFileItem item : items) {
            if (item.isFormField() && FIELD_NAME_DIR.equals(item.getFieldName())) {
                return Path.of(item.getString());
            }
        }
        throw new IOException("Missing 'dir' parameter.");
    }

    private boolean isUnzip(List<DiskFileItem> items) {
        for (DiskFileItem item : items) {
            if (item.isFormField() && FIELD_NAME_UNZIP.equals(item.getFieldName())) {
                return true;
            }
        }
        return false;
    }

    private List<DiskFileItem> getUploadItems(HttpServletRequest request, TransferStatus status)
            throws FileUploadException {
        // Migration code. Temporarily do not use Listner
        @SuppressWarnings({ "unused", "PMD.PrematureDeclaration" })
        UploadListener listener = new UploadListenerImpl(status, statusService, settingsService);
        DiskFileItemFactory factory = DiskFileItemFactory.builder().get();
        JakartaServletDiskFileUpload upload = new JakartaServletDiskFileUpload(factory);
        return upload.parseRequest(request);
    }

    private void afterUpload(HttpServletRequest request, TransferStatus status) {
        if (status != null) {
            statusService.removeUploadStatus(status);
            request.getSession().removeAttribute(Attributes.Session.UPLOAD_STATUS.value());
            User user = securityService.getCurrentUserStrict(request);
            securityService.updateUserByteCounts(user, 0L, 0L, status.getBytesTransfered());
        }
    }

    private static class UnzipResult {

        private final List<Path> uploadedFiles;
        private final List<Path> unzippedFiles;

        public UnzipResult(List<Path> uploadedFiles, List<Path> unzippedFiles) {
            super();
            this.uploadedFiles = uploadedFiles;
            this.unzippedFiles = unzippedFiles;
        }

        public List<Path> getUploadedFiles() {
            return uploadedFiles;
        }

        public List<Path> getUnzippedFiles() {
            return unzippedFiles;
        }
    }

    @SuppressFBWarnings(value = "FILE_UPLOAD_FILENAME", justification = "Limited features used by privileged users")
    private UnzipResult doUnzip(List<DiskFileItem> items, Path dir, boolean unzip) throws ExecutionException {
        List<Path> uploadedFiles = new ArrayList<>();
        List<Path> unzippedFiles = new ArrayList<>();

        if (!Files.exists(dir) && FileUtil.createDirectories(dir) == null && LOG.isWarnEnabled()) {
            LOG.warn("The directory '{}' could not be created.", dir);
        }

        // Look for file items.
        for (DiskFileItem item : items) {
            if (!item.isFormField() && !StringUtils.isAllBlank(item.getName())) {

                Path targetFile = Path.of(dir.toString(), item.getName());
                uploadedFiles = upload(item, targetFile);

                if (LOG.isInfoEnabled()) {
                    LOG.info("Uploaded " + targetFile);
                }

                if (unzip && StringUtils.endsWithIgnoreCase(targetFile.toString(), ".zip")) {
                    unzippedFiles = unzip(targetFile);
                }
            }
        }
        return new UnzipResult(uploadedFiles, unzippedFiles);
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // apache-commons/FileItem#write
    private List<Path> upload(DiskFileItem targetItem, Path targetFile) throws ExecutionException {

        if (!securityService.isUploadAllowed(targetFile)) {
            throw new ExecutionException(new GeneralSecurityException(
                    NOT_ALLOWED_MSG + StringEscapeUtils.escapeHtml4(targetFile.toString())));
        }

        List<Path> uploadedFiles = new ArrayList<>();
        try {
            targetItem.write(targetFile);
            uploadedFiles.add(targetFile);
        } catch (Exception e) {
            throw new ExecutionException("Unable to write item.", e);
        }
        return uploadedFiles;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (File, IOException) Not reusable
    @SuppressLint(value = "RESOURCE_LEAK", justification = "False positive. facebook/infer#619")
    private List<Path> unzip(Path file) throws ExecutionException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Unzipping " + file);
        }
        List<Path> unzippedFiles = Collections.emptyList();
        try (ZipFile zipFile = new ZipFile(file.toString())) {

            Enumeration<?> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                Path parent = file.getParent();
                if (parent == null) {
                    throw new ExecutionException(
                            new IOException("Bad zip filename: " + StringEscapeUtils.escapeHtml4(file.toString())));
                }
                ZipEntry entry = (ZipEntry) entries.nextElement();
                Path entryFile = Path.of(parent.toString(), entry.getName());
                if (!entryFile.normalize().toString().startsWith(parent.toString())) {
                    throw new ExecutionException(new IOException(
                            "Bad zip filename: " + StringEscapeUtils.escapeHtml4(entryFile.toString())));
                }
                if (!entry.isDirectory()) {
                    unzippedFiles = unzip(zipFile, entry, entryFile);
                }
            }
            zipFile.close();
            FileUtil.deleteIfExists(file);

        } catch (IOException e) {
            throw new ExecutionException("Can't unzip.", e);
        }
        return unzippedFiles;
    }

    private List<Path> unzip(ZipFile zipFile, ZipEntry entry, Path entryFile) throws ExecutionException {

        if (!securityService.isUploadAllowed(entryFile)) {
            throw new ExecutionException(new GeneralSecurityException(
                    NOT_ALLOWED_MSG + StringEscapeUtils.escapeHtml4(entryFile.toString())));
        }

        Path parent = entryFile.getParent();
        if (parent == null
                || !Files.exists(parent) && FileUtil.createDirectories(parent) == null && LOG.isWarnEnabled()) {
            LOG.warn("The directory '{}' could not be created.", parent);
        }

        List<Path> unzippedFiles = new ArrayList<>();
        try (OutputStream outputStream = Files.newOutputStream(entryFile);
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
            this.startTime = Instant.now().toEpochMilli();
        }

        @Override
        public void start(String path) {
            status.setPathString(path);
        }

        @Override
        public void bytesRead(long bytesRead) {

            // Throttle bitrate.
            long byteCount = status.getBytesTransfered() + bytesRead;
            long bitCount = byteCount * 8L;
            float elapsedMillis = Math.max(1, Instant.now().toEpochMilli() - startTime);
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
