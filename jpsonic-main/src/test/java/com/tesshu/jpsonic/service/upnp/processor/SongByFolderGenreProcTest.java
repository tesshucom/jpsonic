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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.domain.CoverArtScheme;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.upnp.processor.composite.FGenreOrSong;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderGenre;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFGenre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.GenreContainer;
import org.jupnp.support.model.item.Item;
import org.jupnp.support.model.item.MusicTrack;
import org.mockito.ArgumentMatchers;
import org.springframework.web.util.UriComponentsBuilder;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class SongByFolderGenreProcTest {

    private UpnpProcessorUtil util;
    private SettingsService settingsService;
    private SearchService searchService;
    private SongByFolderGenreProc proc;

    @BeforeEach
    public void setup() {
        util = mock(UpnpProcessorUtil.class);
        settingsService = mock(SettingsService.class);
        when(settingsService.getDlnaBaseLANURL()).thenReturn("https://192.168.1.1:4040");
        JWTSecurityService jwtSecurityService = mock(JWTSecurityService.class);
        UriComponentsBuilder dummyCoverArtbuilder = UriComponentsBuilder
            .fromUriString(
                    settingsService.getDlnaBaseLANURL() + "/ext/" + ViewName.COVER_ART.value())
            .queryParam("id", "99")
            .queryParam(Attributes.Request.SIZE.value(), CoverArtScheme.LARGE.getSize());
        when(jwtSecurityService.addJWTToken(any(UriComponentsBuilder.class)))
            .thenReturn(dummyCoverArtbuilder);
        UpnpDIDLFactory factory = new UpnpDIDLFactory(settingsService, jwtSecurityService,
                mock(MediaFileService.class), mock(PlayerService.class),
                mock(TranscodingService.class));
        searchService = mock(SearchService.class);
        FolderOrGenreLogic deligate = new FolderOrGenreLogic(searchService, util, factory);
        proc = new SongByFolderGenreProc(settingsService, searchService, factory, deligate);
    }

    @Test
    void testGetProcId() {
        assertEquals("sbfg", proc.getProcId().getValue());
    }

    @Test
    void testCreateContainer() {
        UpnpDIDLFactory factory = mock(UpnpDIDLFactory.class);
        FolderOrGenreLogic deligate = new FolderOrGenreLogic(searchService, util, factory);
        proc = new SongByFolderGenreProc(settingsService, searchService, factory, deligate);

        MusicFolder folder = new MusicFolder(99, "path", "name", true, Instant.now(), 0, false);
        FolderOrFGenre folderOrGenre = new FolderOrFGenre(folder);
        proc.createContainer(folderOrGenre);
        verify(factory, never()).toGenre(any(ProcId.class), any(FolderGenre.class), anyInt());
        verify(factory, times(1))
            .toMusicFolder(any(ProcId.class), any(MusicFolder.class), anyInt());
        clearInvocations(factory);

        folderOrGenre = new FolderOrFGenre(new FolderGenre(folder, new Genre("genre", 0, 0)));
        proc.createContainer(folderOrGenre);
        verify(factory, times(1)).toGenre(any(ProcId.class), any(FolderGenre.class), anyInt());
        verify(factory, never()).toMusicFolder(any(ProcId.class), any(MusicFolder.class), anyInt());
    }

    @Test
    void testGetDirectChildren() {
        MusicFolder folder0 = new MusicFolder(0, null, null, false, null, null, false);
        when(util.getGuestFolders()).thenReturn(Arrays.asList(folder0));
        proc.getDirectChildren(0, Integer.MAX_VALUE);
        verify(searchService, times(1))
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
        clearInvocations(searchService);

        MusicFolder folder1 = new MusicFolder(0, null, null, false, null, null, false);
        when(util.getGuestFolders()).thenReturn(Arrays.asList(folder0, folder1));
        proc.getDirectChildren(0, Integer.MAX_VALUE);
        verify(searchService, never())
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
    }

    @Test
    void testGetDirectChildrenCount() {
        MusicFolder folder0 = new MusicFolder(0, null, null, false, null, null, false);
        when(util.getGuestFolders()).thenReturn(Arrays.asList(folder0));
        proc.getDirectChildrenCount();
        verify(searchService, times(1)).getGenresCount(any(GenreMasterCriteria.class));
        clearInvocations(searchService);

        MusicFolder folder1 = new MusicFolder(0, null, null, false, null, null, false);
        when(util.getGuestFolders()).thenReturn(Arrays.asList(folder0, folder1));
        proc.getDirectChildrenCount();
        verify(searchService, never()).getGenresCount(any(GenreMasterCriteria.class));
    }

    @Test
    void testGetDirectChild() {
        MusicFolder folder = new MusicFolder(0, null, null, false, null, null, false);
        when(util.getGuestFolders()).thenReturn(Arrays.asList(folder));
        Genre genre = new Genre("genre", 0, 0);
        when(searchService.getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong()))
            .thenReturn(Arrays.asList(genre));
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        proc.getDirectChild(folderGenre.createCompositeId());
        verify(searchService, times(1))
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
        clearInvocations(searchService);

        proc.getDirectChild(Integer.toString(folder.getId()));
        verify(searchService, never())
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
    }

    @Test
    void testGetChildren() {
        MusicFolder folder = new MusicFolder(0, null, null, false, null, null, false);
        FolderOrFGenre folderOrFGenre = new FolderOrFGenre(folder);
        proc.getChildren(folderOrFGenre, 0, Integer.MAX_VALUE);
        verify(searchService, times(1))
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
        clearInvocations(searchService);

        Genre genre = new Genre("genre", 0, 0);
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        folderOrFGenre = new FolderOrFGenre(folderGenre);
        proc.getChildren(folderOrFGenre, 0, Integer.MAX_VALUE);
        verify(searchService, times(1))
            .getSongsByGenres(anyString(), anyInt(), anyInt(),
                    ArgumentMatchers.<MusicFolder>anyList());
    }

    @Test
    void testGetChildSizeOf() {
        MusicFolder folder = new MusicFolder(0, null, null, false, null, null, false);
        FolderOrFGenre folderOrFGenre = new FolderOrFGenre(folder);
        proc.getChildSizeOf(folderOrFGenre);
        verify(searchService, times(1)).getGenresCount(any(GenreMasterCriteria.class));
        clearInvocations(searchService);

        Genre genre = new Genre("genre", 0, 0);
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        folderOrFGenre = new FolderOrFGenre(folderGenre);
        proc.getChildSizeOf(folderOrFGenre);
        verify(searchService, never()).getGenresCount(any(GenreMasterCriteria.class));
    }

    @Test
    void testAddChild() {
        MusicFolder folder = new MusicFolder(0, null, null, false, null, null, false);
        Genre genre = new Genre("genre", 0, 0);
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        FGenreOrSong genreOrSong = new FGenreOrSong(folderGenre);
        DIDLContent parent = new DIDLContent();
        proc.addChild(parent, genreOrSong);
        assertEquals(1, parent.getCount());
        List<Container> containers = parent.getContainers();
        assertEquals(1, containers.size());
        assertEquals(GenreContainer.class, containers.get(0).getClass());

        MediaFile song = new MediaFile();
        song.setId(0);
        genreOrSong = new FGenreOrSong(song);
        parent = new DIDLContent();
        proc.addChild(parent, genreOrSong);
        assertEquals(1, parent.getCount());
        assertEquals(0, parent.getContainers().size());
        List<Item> items = parent.getItems();
        assertEquals(1, items.size());
        assertEquals(MusicTrack.class, items.get(0).getClass());
    }

    @Test
    void testBrowseLeaf() throws ExecutionException {
        // Browse Genre
        MusicFolder folder = new MusicFolder(0, null, null, false, null, null, false);
        when(util.getGuestFolders()).thenReturn(Arrays.asList(folder));
        Genre genre = new Genre("genre", 0, 0);
        when(searchService.getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong()))
            .thenReturn(Arrays.asList(genre));
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        proc.browseLeaf(folderGenre.createCompositeId(), null, 0, Integer.MAX_VALUE);
        verify(searchService, times(1))
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
        clearInvocations(searchService);
        proc.getDirectChild(Integer.toString(folder.getId()));
        verify(searchService, never())
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());

        // Browse Folder
        proc.browseLeaf(Integer.toString(folder.getId()), null, 0, Integer.MAX_VALUE);
        verify(searchService, times(1))
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
    }
}
