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

package com.tesshu.jpsonic.feature.upnp.content.processor;

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

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.Genre;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.provider.resource.PlayerProvider;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpPayloadCodec;
import com.tesshu.jpsonic.feature.transcoding.TranscodingParametersPlanner;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FGenreOrFGAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderGenre;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderGenreAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFGenre;
import com.tesshu.jpsonic.feature.upnp.content.processor.logic.FolderOrGenreLogic;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.infrastructure.search.criteria.GenreMasterCriteria;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.GenreContainer;
import org.jupnp.support.model.container.MusicAlbum;
import org.mockito.ArgumentMatchers;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class AlbumId3ByFolderGenreProcTest {

    private SettingsFacade settingsFacade;
    private MediaSearchProvider mediaSearchProvider;
    private AlbumId3ByFolderGenreProc proc;
    private AlbumProvider albumProvider;
    private MusicFolderProvider musicFolderProvider;

    @BeforeEach
    void setup() {
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(UPnPSKeys.basic.baseLanUrl, "https://192.168.1.1:4040")
            .build();
        MediaFileProvider mediaFileProvider = mock(MediaFileProvider.class);
        PlayerProvider playerProvider = mock(PlayerProvider.class);
        TranscodingParametersPlanner transcodingParametersPlanner = mock(
                TranscodingParametersPlanner.class);
        UPnPDIDLFactory factory = new UPnPDIDLFactory(settingsFacade, mock(UpnpPayloadCodec.class),
                mediaFileProvider, playerProvider, transcodingParametersPlanner);
        mediaSearchProvider = mock(MediaSearchProvider.class);
        musicFolderProvider = mock(MusicFolderProvider.class);
        FolderOrGenreLogic deligate = new FolderOrGenreLogic(mediaSearchProvider,
                musicFolderProvider, factory);
        albumProvider = mock(AlbumProvider.class);
        proc = new AlbumId3ByFolderGenreProc(musicFolderProvider, factory, settingsFacade,
                mediaSearchProvider, albumProvider, deligate);
    }

    @Test
    void testGetProcId() {
        assertEquals("aibfg", proc.getProcId().getValue());
    }

    @Test
    void testCreateContainer() {
        UPnPDIDLFactory factory = mock(UPnPDIDLFactory.class);
        FolderOrGenreLogic deligate = new FolderOrGenreLogic(mediaSearchProvider,
                musicFolderProvider, factory);
        proc = new AlbumId3ByFolderGenreProc(musicFolderProvider, factory, settingsFacade,
                mediaSearchProvider, albumProvider, deligate);

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
        MusicFolder folder0 = new MusicFolder(0, "path1", "folder", false, null, 0, false);
        when(musicFolderProvider.getGuestFolders()).thenReturn(Arrays.asList(folder0));
        proc.getDirectChildren(0, Integer.MAX_VALUE);
        verify(mediaSearchProvider, times(1))
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
        clearInvocations(mediaSearchProvider);

        MusicFolder folder1 = new MusicFolder(0, "path1", "folder", false, null, 0, false);
        when(musicFolderProvider.getGuestFolders()).thenReturn(Arrays.asList(folder0, folder1));
        proc.getDirectChildren(0, Integer.MAX_VALUE);
        verify(mediaSearchProvider, never())
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
    }

    @Test
    void testGetDirectChildrenCount() {
        MusicFolder folder0 = new MusicFolder(0, "path1", "folder", false, null, 0, false);
        when(musicFolderProvider.getGuestFolders()).thenReturn(Arrays.asList(folder0));
        proc.getDirectChildrenCount();
        verify(mediaSearchProvider, times(1)).getGenresCount(any(GenreMasterCriteria.class));
        clearInvocations(mediaSearchProvider);

        MusicFolder folder1 = new MusicFolder(0, "path1", "folder", false, null, 0, false);
        when(musicFolderProvider.getGuestFolders()).thenReturn(Arrays.asList(folder0, folder1));
        proc.getDirectChildrenCount();
        verify(mediaSearchProvider, never()).getGenresCount(any(GenreMasterCriteria.class));
    }

    @Test
    void testGetDirectChild() {
        MusicFolder folder = new MusicFolder(0, "path1", "folder", false, null, 0, false);
        when(musicFolderProvider.getGuestFolders()).thenReturn(Arrays.asList(folder));
        Genre genre = new Genre("genre", 0, 0);
        when(mediaSearchProvider.getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong()))
            .thenReturn(Arrays.asList(genre));
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        proc.getDirectChild(folderGenre.createCompositeId());
        verify(mediaSearchProvider, times(1))
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
        clearInvocations(mediaSearchProvider);

        proc.getDirectChild(Integer.toString(folder.id()));
        verify(mediaSearchProvider, never())
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
    }

    @Test
    void testGetChildren() {
        MusicFolder folder = new MusicFolder(0, "path1", "folder", false, null, 0, false);
        FolderOrFGenre folderOrFGenre = new FolderOrFGenre(folder);
        proc.getChildren(folderOrFGenre, 0, Integer.MAX_VALUE);
        verify(mediaSearchProvider, times(1))
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
        clearInvocations(mediaSearchProvider);

        Genre genre = new Genre("genre", 0, 0);
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        folderOrFGenre = new FolderOrFGenre(folderGenre);
        proc.getChildren(folderOrFGenre, 0, Integer.MAX_VALUE);
        verify(mediaSearchProvider, times(1))
            .findAlbumId3sByGenres(ArgumentMatchers.<MusicFolder>anyList(), anyString(), anyLong(),
                    anyLong());
    }

    @Test
    void testGetChildSizeOf() {
        MusicFolder folder = new MusicFolder(0, "path1", "folder", false, null, 0, false);
        FolderOrFGenre folderOrFGenre = new FolderOrFGenre(folder);
        proc.getChildSizeOf(folderOrFGenre);
        verify(mediaSearchProvider, times(1)).getGenresCount(any(GenreMasterCriteria.class));
        clearInvocations(mediaSearchProvider);

        Genre genre = new Genre("genre", 0, 0);
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        folderOrFGenre = new FolderOrFGenre(folderGenre);
        proc.getChildSizeOf(folderOrFGenre);
        verify(mediaSearchProvider, never()).getGenresCount(any(GenreMasterCriteria.class));
    }

    @Test
    void testAddChild() {
        MusicFolder folder = new MusicFolder(0, "path1", "folder", false, null, 0, false);
        Genre genre = new Genre("genre", 0, 0);
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        FGenreOrFGAlbum genreOrAlbum = new FGenreOrFGAlbum(folderGenre);
        DIDLContent parent = new DIDLContent();
        proc.addChild(parent, genreOrAlbum);
        assertEquals(1, parent.getCount());
        List<Container> containers = parent.getContainers();
        assertEquals(1, containers.size());
        assertEquals(GenreContainer.class, containers.get(0).getClass());

        Album album = new Album(0, "album", "artist", 1, null);
        FolderGenreAlbum folderGenreAlbum = new FolderGenreAlbum(folder, genre, album);
        genreOrAlbum = new FGenreOrFGAlbum(folderGenreAlbum);
        parent = new DIDLContent();
        proc.addChild(parent, genreOrAlbum);
        assertEquals(1, parent.getCount());
        containers = parent.getContainers();
        assertEquals(1, containers.size());
        assertEquals(MusicAlbum.class, containers.get(0).getClass());
    }

    @Test
    void testBrowseLeaf() throws ExecutionException {
        // Browse Album
        MusicFolder folder = new MusicFolder(0, "path1", "folder", false, null, 0, false);
        when(musicFolderProvider.getGuestFolders()).thenReturn(Arrays.asList(folder));
        Genre genre = new Genre("genre", 0, 0);
        when(mediaSearchProvider.getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong()))
            .thenReturn(Arrays.asList(genre));
        Album album = new Album(0, "album", "artist", 1, null);
        when(albumProvider.requireAlbum(anyInt())).thenReturn(album);
        FolderGenreAlbum fgAlbum = new FolderGenreAlbum(folder, genre, album);
        proc.browseLeaf(fgAlbum.createCompositeId(), null, 0, Integer.MAX_VALUE);
        verify(albumProvider, times(1)).requireAlbum(anyInt());
        verify(mediaSearchProvider, times(1))
            .findChildren(ArgumentMatchers.<MusicFolder>anyList(), anyString(), any(Album.class),
                    anyLong(), anyLong(), any(MediaFile.Type[].class));
        verify(mediaSearchProvider, times(1))
            .countChldren(ArgumentMatchers.<MusicFolder>anyList(), anyString(), any(Album.class),
                    any(MediaFile.Type[].class));
        clearInvocations(albumProvider, mediaSearchProvider);

        // Browse Genre
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        proc.browseLeaf(folderGenre.createCompositeId(), null, 0, Integer.MAX_VALUE);
        verify(mediaSearchProvider, times(1))
            .findAlbumId3sByGenres(ArgumentMatchers.<MusicFolder>anyList(), anyString(), anyLong(),
                    anyLong());
        clearInvocations(mediaSearchProvider);

        // Browse Folder
        proc.browseLeaf(Integer.toString(folder.id()), null, 0, Integer.MAX_VALUE);
        verify(mediaSearchProvider, times(1))
            .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
    }
}
