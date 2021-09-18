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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.Share;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate the set of shared media.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/shareSettings", "/shareSettings.view" })
public class ShareSettingsController {

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final ShareService shareService;
    private final MediaFileService mediaFileService;

    public ShareSettingsController(SettingsService settingsService, MusicFolderService musicFolderService,
            SecurityService securityService, ShareService shareService, MediaFileService mediaFileService) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.shareService = shareService;
        this.mediaFileService = mediaFileService;
    }

    @GetMapping
    public String doGet(HttpServletRequest request, Model model) {
        model.addAttribute("model",
                LegacyMap.of("shareInfos", getShareInfos(request), "user", securityService.getCurrentUser(request),
                        "useRadio", settingsService.isUseRadio(), "shareCount", shareService.getAllShares().size()));
        return "shareSettings";
    }

    @PostMapping
    public ModelAndView doPost(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        handleParameters(request);
        redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
        return new ModelAndView(new RedirectView(ViewName.SHARE_SETTINGS.value()));
    }

    private void handleParameters(HttpServletRequest request) {
        User user = securityService.getCurrentUser(request);
        for (Share share : shareService.getSharesForUser(user)) {
            int id = share.getId();

            String description = getParameter(request, Attributes.Request.DESCRIPTION.value(), id);
            boolean delete = getParameter(request, Attributes.Request.DELETE.value(), id) != null;
            String expireIn = getParameter(request, Attributes.Request.EXPIRE_IN.value(), id);

            if (delete) {
                shareService.deleteShare(id);
            } else {
                if (expireIn != null) {
                    share.setExpires(parseExpireIn(expireIn));
                }
                share.setDescription(description);
                shareService.updateShare(share);
            }
        }

        boolean deleteExpired = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DELETE_EXPIRED.value(), false);
        if (deleteExpired) {
            Date now = new Date();
            for (Share share : shareService.getSharesForUser(user)) {
                Date expires = share.getExpires();
                if (expires != null && expires.before(now)) {
                    shareService.deleteShare(share.getId());
                }
            }
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (ShareInfo) Not reusable
    private List<ShareInfo> getShareInfos(HttpServletRequest request) {
        List<ShareInfo> result = new ArrayList<>();
        User user = securityService.getCurrentUser(request);
        List<MusicFolder> musicFolders = musicFolderService.getMusicFoldersForUser(user.getUsername());

        for (Share share : shareService.getSharesForUser(user)) {
            List<MediaFile> files = shareService.getSharedFiles(share.getId(), musicFolders);
            if (!files.isEmpty()) {
                MediaFile file = files.get(0);
                result.add(new ShareInfo(shareService.getShareUrl(request, share), share,
                        file.isDirectory() ? file : mediaFileService.getParentOf(file)));
            }
        }
        return result;
    }

    private String getParameter(HttpServletRequest request, String name, int id) {
        return StringUtils.trimToNull(request.getParameter(name + "[" + id + "]"));
    }

    private Date parseExpireIn(String expireIn) {
        int days = Integer.parseInt(expireIn);
        if (days == 0) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTime();
    }

    public static class ShareInfo {
        private final String shareUrl;
        private final Share share;
        private final MediaFile dir;

        public ShareInfo(String shareUrl, Share share, MediaFile dir) {
            this.shareUrl = shareUrl;
            this.share = share;
            this.dir = dir;
        }

        public Share getShare() {
            return share;
        }

        public String getShareUrl() {
            return shareUrl;
        }

        public MediaFile getDir() {
            return dir;
        }
    }
}
