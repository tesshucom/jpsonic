/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.AbstractNeedsScan;
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
import org.springframework.web.servlet.ModelAndView;

@SpringBootTest
@AutoConfigureMockMvc
class RandomPlayQueueControllerTest extends AbstractNeedsScan {

    private static final String ADMIN_NAME = "admin";
    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Music"));
        MUSIC_FOLDERS.add(new MusicFolder(1, musicDir, "Music", true, new Date()));
    }

    @Autowired
    private RandomPlayQueueController controller;

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
    void testDoSubmitAction() throws Exception {
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.post("/" + ViewName.RANDOM_PLAYQUEUE.value())
                        .param(Attributes.Request.SIZE.value(), Integer.toString(24))
                        .param(Attributes.Request.MUSIC_FOLDER_ID.value(),
                                Integer.toString(MUSIC_FOLDERS.get(0).getId()))
                        .param(Attributes.Request.NameConstants.LAST_PLAYED_VALUE, "any")
                        .param(Attributes.Request.NameConstants.YEAR, "any"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);

        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("reload", modelAndView.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get("model");
        assertNotNull(model);
    }
}
