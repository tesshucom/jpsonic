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

package org.airsonic.player.upload;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.fileupload.disk.DiskFileItem;

/**
 * Extension of Commons FileUpload for monitoring the upload progress.
 *
 * @author Pierre-Alexandre Losson -- http://www.telio.be/blog -- plosson@users.sourceforge.net
 */
public class MonitoredDiskFileItem extends DiskFileItem {

    private MonitoredOutputStream mos;
    private final UploadListener listener;

    public MonitoredDiskFileItem(String fieldName, String contentType, boolean isFormField, String fileName,
            int sizeThreshold, File repository, UploadListener listener) {
        super(fieldName, contentType, isFormField, fileName, sizeThreshold, repository);
        this.listener = listener;
        if (fileName != null) {
            listener.start(fileName);
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (mos == null) {
            mos = new MonitoredOutputStream(super.getOutputStream(), listener);
        }
        return mos;
    }
}
