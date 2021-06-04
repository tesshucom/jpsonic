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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.upnp.UpnpProcessDispatcher.CONTAINER_ID_RANDOM_SONG_BY_ARTIST;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import com.tesshu.jpsonic.dao.JArtistDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class RandomSongByArtistUpnpProcessor extends UpnpContentProcessor<Artist, MediaFile> {

    private final UpnpProcessorUtil util;
    private final JArtistDao artistDao;
    private final SearchService searchService;
    private final SettingsService settingsService;

    public RandomSongByArtistUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JArtistDao a,
            SearchService s, SettingsService ss) {
        super(d, u);
        util = u;
        artistDao = a;
        searchService = s;
        settingsService = ss;
        setRootId(CONTAINER_ID_RANDOM_SONG_BY_ARTIST);
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.randomSongByArtist");
    }

    @Override
    public Container createContainer(Artist artist) {
        MusicArtist container = new MusicArtist();
        container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + artist.getId());
        container.setParentID(getRootId());
        container.setTitle(artist.getName());
        container.setChildCount(artist.getAlbumCount());
        if (artist.getCoverArtPath() != null) {
            container.setProperties(
                    Arrays.asList(new ALBUM_ART_URI(getDispatcher().getArtistProcessor().createArtistArtURI(artist))));
        }
        return container;
    }

    @Override
    public int getItemCount() {
        return artistDao.getArtistsCount(util.getAllMusicFolders());
    }

    @Override
    public List<Artist> getItems(long offset, long count) {
        return artistDao.getAlphabetialArtists((int) offset, (int) count, util.getAllMusicFolders());
    }

    @Override
    public Artist getItemById(String id) {
        return artistDao.getArtist(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(Artist artist) {
        return settingsService.getDlnaRandomMax();
    }

    @Override
    public List<MediaFile> getChildren(Artist artist, long first, long maxResults) {
        int randomMax = settingsService.getDlnaRandomMax();
        int offset = (int) first;
        int count = (offset + (int) maxResults) > randomMax ? randomMax - offset : (int) maxResults;
        return searchService.getRandomSongsByArtist(artist, count, offset, randomMax, util.getAllMusicFolders());
    }

    @Override
    public void addChild(DIDLContent didl, MediaFile child) {
        didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
    }

}
