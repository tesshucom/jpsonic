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

package com.tesshu.jpsonic.controller;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class ChangeCoverArtControllerTest extends AbstractNeedsScan {

    private static final String ADMIN_NAME = "admin";
    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Music"));
        MUSIC_FOLDERS.add(new MusicFolder(1, musicDir, "Music", true, new Date()));
    }

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private ChangeCoverArtController controller;

    private MockMvc mockMvc;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @BeforeEach
    public void setup() throws ExecutionException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        populateDatabaseOnlyOnce();
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testHandleRequest() throws Exception {

        MediaFile album = mediaFileDao.getAlphabeticalAlbums(0, 1, false, MUSIC_FOLDERS).get(0);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/changeCoverArt.view")
                .param(Attributes.Request.ID.value(), Integer.toString(album.getId())))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
    }
}
