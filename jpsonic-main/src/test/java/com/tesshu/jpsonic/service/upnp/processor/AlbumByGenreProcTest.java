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

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.GenreContainer;

//pmd/pmd#4616
@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals", "PMD.SingularField" })
class AlbumByGenreProcTest {

    private SettingsService settingsService;
    private UpnpProcessorUtil util;
    private UpnpDIDLFactory factory;
    private MediaFileService mediaFileService;
    private SearchService searchService;
    private AlbumByGenreProc proc;

    @BeforeEach
    public void setup() {
        settingsService = mock(SettingsService.class);
        JWTSecurityService jwtSecurityService = mock(JWTSecurityService.class);
        PlayerService playerService = mock(PlayerService.class);
        TranscodingService transcodingService = mock(TranscodingService.class);
        factory = new UpnpDIDLFactory(settingsService, jwtSecurityService, mediaFileService, playerService,
                transcodingService);
        mediaFileService = mock(MediaFileService.class);
        searchService = mock(SearchService.class);
        util = new UpnpProcessorUtil(mock(MusicFolderService.class), mock(SecurityService.class),
                mock(JpsonicComparators.class));
        proc = new AlbumByGenreProc(util, factory, mediaFileService, searchService);
    }

    @Test
    void testGetProcId() {
        assertEquals("abg", proc.getProcId().getValue());
    }

    @Test
    void testCreateContainer() {
        Genre genre = new Genre("English/Japanese", 50, 100);
        Container container = proc.createContainer(genre);
        assertInstanceOf(GenreContainer.class, container);
        assertEquals("abg/English/Japanese", container.getId());
        assertEquals("abg", container.getParentID());
        assertEquals("English/Japanese", container.getTitle());
        assertEquals(50, container.getChildCount());
    }

    @Test
    void testGetChildren() {
        Genre genre = new Genre("English/Japanese", 50, 100);
        assertEquals(Collections.emptyList(), proc.getChildren(genre, 0, 0));
        verify(searchService, times(1)).getAlbumsByGenres(anyString(), anyInt(), anyInt(), anyList());
    }

    @Test
    void testGetChildSizeOf() {
        Genre genre = new Genre("English/Japanese", 50, 100);
        assertEquals(100, proc.getChildSizeOf(genre));
    }

    @Test
    void testAddChild() {
        DIDLContent content = new DIDLContent();
        MediaFile song = new MediaFile();
        factory = mock(UpnpDIDLFactory.class);
        proc = new AlbumByGenreProc(util, factory, mediaFileService, searchService);
        proc.addChild(content, song);
        verify(factory, times(1)).toAlbum(any(MediaFile.class), anyInt());
        assertEquals(1, content.getCount());
        assertEquals(1, content.getContainers().size());
        assertEquals(0, content.getItems().size());
    }
}
