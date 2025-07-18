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

import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

public final class AjaxMockUtils {

    private AjaxMockUtils() {

    }

    @SuppressWarnings("unchecked")
    public static <T> T mock(Class<T> classToMock) {
        Object mock;
        if (AjaxHelper.class == classToMock) {
            AjaxHelper ajaxHelper = Mockito.mock(AjaxHelper.class);
            Mockito
                .when(ajaxHelper.getHttpServletRequest())
                .thenReturn(new MockHttpServletRequest());
            Mockito
                .when(ajaxHelper.getHttpServletResponse())
                .thenReturn(new MockHttpServletResponse());
            Mockito.when(ajaxHelper.getSession()).thenReturn(new MockHttpSession());
            mock = ajaxHelper;
        } else {
            mock = Mockito.mock(classToMock);
        }
        return (T) mock;
    }
}
