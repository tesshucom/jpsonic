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

package com.tesshu.jpsonic.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItemHeaders;
import org.apache.commons.io.output.DeferredFileOutputStream;

/**
 * Extension of Commons FileUpload for monitoring the upload progress.
 *
 * @author Pierre-Alexandre Losson -- http://www.telio.be/blog -- plosson@users.sourceforge.net
 */
public class MonitoredDiskFileItem implements FileItem<DiskFileItem> {

    private MonitoredOutputStream mos;
    private final UploadListener listener;
    private final DiskFileItem deligate;

    public MonitoredDiskFileItem(String fieldName, String contentType, boolean isFormField, String fileName,
            int sizeThreshold, File repository, UploadListener listener) {
        deligate = DiskFileItem.builder().setFieldName(fieldName).setContentType(contentType).setFormField(isFormField)
                .setFileName(fileName)
                .setOutputStream(
                        DeferredFileOutputStream.builder().setThreshold(sizeThreshold).setOutputFile(repository).get())
                .get();
        this.listener = listener;
        if (fileName != null) {
            listener.start(fileName);
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (mos == null) {
            mos = new MonitoredOutputStream(deligate.getOutputStream(), listener);
        }
        return mos;
    }

    @Override
    public FileItemHeaders getHeaders() {
        return deligate.getHeaders();
    }

    @Override
    public DiskFileItem setHeaders(FileItemHeaders headers) {
        return deligate.setHeaders(headers);
    }

    @Override
    public DiskFileItem delete() throws IOException {
        return deligate.delete();
    }

    @Override
    public byte[] get() {
        return deligate.get();
    }

    @Override
    public String getContentType() {
        return deligate.getContentType();
    }

    @Override
    public String getFieldName() {
        return deligate.getFieldName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return deligate.getInputStream();
    }

    @Override
    public String getName() {
        return deligate.getName();
    }

    @Override
    public long getSize() {
        return deligate.getSize();
    }

    @Override
    public String getString() {
        return deligate.getString();
    }

    @Override
    public String getString(Charset toCharset) throws IOException {
        return deligate.getString(toCharset);
    }

    @Override
    public boolean isFormField() {
        return deligate.isFormField();
    }

    @Override
    public boolean isInMemory() {
        return deligate.isInMemory();
    }

    @Override
    public DiskFileItem setFieldName(String name) {
        return deligate.setFieldName(name);
    }

    @Override
    public DiskFileItem setFormField(boolean state) {
        return deligate.setFormField(state);
    }

    @Override
    public DiskFileItem write(Path file) throws IOException {
        return deligate.write(file);
    }
}
