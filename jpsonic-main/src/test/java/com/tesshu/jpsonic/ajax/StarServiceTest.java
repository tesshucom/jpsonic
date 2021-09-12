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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;

class StarServiceTest {

    private MediaFileDao mediaFileDao;
    private StarService starService;

    @BeforeEach
    public void setup() {
        mediaFileDao = mock(MediaFileDao.class);
        starService = new StarService(mock(SecurityService.class), mediaFileDao, AjaxMockUtils.mock(AjaxHelper.class));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testStar() {

        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(int.class);
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(mediaFileDao).starMediaFile(idCaptor.capture(), nameCaptor.capture());
        int mediaFileId = 0;
        starService.star(mediaFileId);
        assertEquals(mediaFileId, idCaptor.getValue());
        assertEquals(ServiceMockUtils.ADMIN_NAME, nameCaptor.getValue());
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testUnstar() {
        ArgumentCaptor<Integer> id = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(mediaFileDao).unstarMediaFile(id.capture(), name.capture());
        int mediaFileId = 0;
        starService.unstar(mediaFileId);
        assertEquals(mediaFileId, id.getValue());
        assertEquals(ServiceMockUtils.ADMIN_NAME, name.getValue());
    }
}
