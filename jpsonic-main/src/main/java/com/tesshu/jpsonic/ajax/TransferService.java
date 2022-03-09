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

package com.tesshu.jpsonic.ajax;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.domain.TransferStatus;
import org.springframework.stereotype.Service;

/**
 * Provides AJAX-enabled services for retrieving the status of ongoing transfers. This class is used by the DWR
 * framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxTransferService")
public class TransferService {

    private final AjaxHelper ajaxHelper;

    public TransferService(AjaxHelper ajaxHelper) {
        super();
        this.ajaxHelper = ajaxHelper;
    }

    /**
     * Returns info about any ongoing upload within the current session.
     *
     * @return Info about ongoing upload.
     */
    public UploadInfo getUploadInfo() {

        TransferStatus status = (TransferStatus) ajaxHelper.getSession()
                .getAttribute(Attributes.Session.UPLOAD_STATUS.value());

        if (status != null) {
            return new UploadInfo(status.getBytesTransfered(), status.getBytesTotal());
        }
        return new UploadInfo(0L, 0L);
    }
}
