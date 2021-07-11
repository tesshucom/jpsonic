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

import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.SecurityService;
import org.springframework.stereotype.Service;

/**
 * Provides AJAX-enabled services for starring.
 * <p/>
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxStarService")
public class StarService {

    private final SecurityService securityService;
    private final MediaFileDao mediaFileDao;
    private final AjaxHelper ajaxHelper;

    public StarService(SecurityService securityService, MediaFileDao mediaFileDao, AjaxHelper ajaxHelper) {
        super();
        this.securityService = securityService;
        this.mediaFileDao = mediaFileDao;
        this.ajaxHelper = ajaxHelper;
    }

    public void star(int id) {
        mediaFileDao.starMediaFile(id, getUser());
    }

    public void unstar(int id) {
        mediaFileDao.unstarMediaFile(id, getUser());
    }

    private String getUser() {
        User user = securityService.getCurrentUser(ajaxHelper.getHttpServletRequest());
        return user.getUsername();
    }
}
