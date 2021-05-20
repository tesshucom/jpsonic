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

package org.airsonic.player.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.airsonic.player.dao.ShareDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Share;
import org.airsonic.player.domain.User;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Provides services for sharing media.
 *
 * @author Sindre Mehus
 * 
 * @see Share
 */
@Service
public class ShareService {

    private static final Logger LOG = LoggerFactory.getLogger(ShareService.class);

    private final ShareDao shareDao;
    private final SecurityService securityService;
    private final MediaFileService mediaFileService;
    private final JWTSecurityService jwtSecurityService;

    public ShareService(ShareDao shareDao, SecurityService securityService, MediaFileService mediaFileService,
            JWTSecurityService jwtSecurityService) {
        super();
        this.shareDao = shareDao;
        this.securityService = securityService;
        this.mediaFileService = mediaFileService;
        this.jwtSecurityService = jwtSecurityService;
    }

    public List<Share> getAllShares() {
        return shareDao.getAllShares();
    }

    public List<Share> getSharesForUser(User user) {
        List<Share> result = new ArrayList<>();
        for (Share share : getAllShares()) {
            if (user.isAdminRole() || ObjectUtils.equals(user.getUsername(), share.getUsername())) {
                result.add(share);
            }
        }
        return result;
    }

    public Share getShareById(int id) {
        return shareDao.getShareById(id);
    }

    public Share getShareByName(String name) {
        return shareDao.getShareByName(name);
    }

    public List<MediaFile> getSharedFiles(int id, List<MusicFolder> musicFolders) {
        List<MediaFile> result = new ArrayList<>();
        for (String path : shareDao.getSharedFiles(id, musicFolders)) {
            try {
                MediaFile mediaFile = mediaFileService.getMediaFile(path);
                if (mediaFile != null) {
                    result.add(mediaFile);
                }
            } catch (SecurityException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Error in getSharedFiles(int, List<MusicFolder>).", e);
                }
            }
        }
        return result;
    }

    public Share createShare(HttpServletRequest request, List<MediaFile> files) {

        Share share = new Share();
        share.setName(RandomStringUtils.random(5, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        share.setCreated(new Date());
        share.setUsername(securityService.getCurrentUsername(request));

        Calendar expires = Calendar.getInstance();
        expires.add(Calendar.YEAR, 1);
        share.setExpires(expires.getTime());

        shareDao.createShare(share);
        for (MediaFile file : files) {
            shareDao.createSharedFiles(share.getId(), file.getPath());
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Created share '" + share.getName() + "' with " + files.size() + " file(s).");
        }

        return share;
    }

    public void updateShare(Share share) {
        shareDao.updateShare(share);
    }

    public void deleteShare(int id) {
        shareDao.deleteShare(id);
    }

    public String getShareUrl(HttpServletRequest request, Share share) {
        String shareUrl = NetworkUtils.getBaseUrl(request) + "ext/share/" + share.getName();
        return jwtSecurityService.addJWTToken(UriComponentsBuilder.fromUriString(shareUrl), share.getExpires()).build()
                .toUriString();
    }
}
