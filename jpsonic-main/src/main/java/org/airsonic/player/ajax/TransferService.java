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

import javax.servlet.http.HttpSession;

import com.tesshu.jpsonic.controller.Attributes;
import org.airsonic.player.domain.TransferStatus;
import org.directwebremoting.WebContextFactory;
import org.springframework.stereotype.Service;

/**
 * Provides AJAX-enabled services for retrieving the status of ongoing transfers. This class is used by the DWR
 * framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxTransferService")
public class TransferService {

    /**
     * Returns info about any ongoing upload within the current session.
     * 
     * @return Info about ongoing upload.
     */
    public UploadInfo getUploadInfo() {

        HttpSession session = WebContextFactory.get().getSession();
        TransferStatus status = (TransferStatus) session.getAttribute(Attributes.Session.UPLOAD_STATUS.value());

        if (status != null) {
            return new UploadInfo(status.getBytesTransfered(), status.getBytesTotal());
        }
        return new UploadInfo(0L, 0L);
    }
}
