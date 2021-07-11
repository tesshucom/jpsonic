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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@ExtendWith(NeedsHome.class)
class StarServiceTest {

    private static final String ADMIN_NAME = "admin";
    private static final int MEDIA_FILE_ID = 0;

    @Autowired
    private SecurityService securityService;
    @Mock
    private MediaFileDao mediaFileDao;
    @Mock
    private AjaxHelper ajaxHelper;
    @Autowired
    private MockHttpServletRequest httpServletRequest;

    private StarService starService;

    @BeforeEach
    public void setup() {
        Mockito.when(ajaxHelper.getHttpServletRequest()).thenReturn(httpServletRequest);
        starService = new StarService(securityService, mediaFileDao, ajaxHelper);
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testStar() {
        ArgumentCaptor<Integer> id = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(mediaFileDao).starMediaFile(id.capture(), name.capture());
        starService.star(MEDIA_FILE_ID);
        assertEquals(MEDIA_FILE_ID, id.getValue());
        assertEquals(ADMIN_NAME, name.getValue());
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testUnstar() {
        ArgumentCaptor<Integer> id = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(mediaFileDao).unstarMediaFile(id.capture(), name.capture());
        starService.unstar(MEDIA_FILE_ID);
        assertEquals(MEDIA_FILE_ID, id.getValue());
        assertEquals(ADMIN_NAME, name.getValue());
    }
}
