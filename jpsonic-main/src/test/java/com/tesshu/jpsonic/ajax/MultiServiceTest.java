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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.ajax;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.service.LastFmService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;

class MultiServiceTest extends AbstractNeedsScan {

    private static final String ADMIN_NAME = "admin";

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private LastFmService lastFmService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private AirsonicLocaleResolver airsonicLocaleResolver;
    @Mock
    private AjaxHelper ajaxHelper;
    @Autowired
    private MockHttpServletRequest httpServletRequest;
    @Autowired
    private MediaFileDao mediaFileDao;

    private MultiService multiService;
    private List<MusicFolder> musicFolders;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();
            File musicDir = new File(resolveBaseMediaPath("Music"));
            musicFolders.add(new MusicFolder(1, musicDir, "Music", true, new Date()));
        }
        return musicFolders;
    }

    @BeforeEach
    public void setup() {
        Mockito.when(ajaxHelper.getHttpServletRequest()).thenReturn(httpServletRequest);
        populateDatabaseOnlyOnce();
        multiService = new MultiService(musicFolderService, securityService, mediaFileService, lastFmService,
                airsonicLocaleResolver, ajaxHelper);
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testGetArtistInfo() {
        RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
        MediaFile song = mediaFileDao.getRandomSongs(criteria, ADMIN_NAME).get(0);
        MediaFile artist = mediaFileDao.getSongsByArtist(song.getArtist(), 0, 1).get(0);
        assertNotNull(multiService.getArtistInfo(artist.getId(), 1, 20));
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testSetCloseDrawer() {
        UserSettings userSettings = securityService.getUserSettings(ADMIN_NAME);
        assertFalse(userSettings.isCloseDrawer());
        multiService.setCloseDrawer(true);
        userSettings = securityService.getUserSettings(ADMIN_NAME);
        assertTrue(userSettings.isCloseDrawer());
    }
}
