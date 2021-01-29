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

package org.airsonic.player.service.upnp.processor;

import static java.util.stream.Collectors.toList;
import static org.airsonic.player.service.upnp.UpnpProcessDispatcher.CONTAINER_ID_INDEX_ID3_PREFIX;
import static org.airsonic.player.service.upnp.UpnpProcessDispatcher.OBJECT_ID_SEPARATOR;
import static org.airsonic.player.util.PlayerUtils.subList;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.service.JMediaFileService;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicIndex;
import org.airsonic.player.service.MusicIndexService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.airsonic.player.spring.EhcacheConfiguration.IndexCacheKey;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.GenreContainer;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.subsonic.restapi.ArtistID3;
import org.subsonic.restapi.ArtistsID3;
import org.subsonic.restapi.IndexID3;

@Service
public class IndexId3UpnpProcessor extends UpnpContentProcessor<Id3Wrapper, Id3Wrapper> {

    private static final AtomicInteger INDEX_IDS = new AtomicInteger(Integer.MIN_VALUE);

    private final UpnpProcessorUtil util;
    private final JMediaFileService mediaFileService;
    private final MusicIndexService musicIndexService;
    private final ArtistDao artistDao;
    private final JAlbumDao albumDao;

    private final Ehcache indexCache;

    // Only on write (because it can be explicitly reloaded on the client and is less risky)
    private static final Object LOCK = new Object();

    private ArtistsID3 content;

    private Map<String, Id3Wrapper> indexesMap;

    private List<Id3Wrapper> topNodes;

    public IndexId3UpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JMediaFileService m,
            MusicIndexService mi, ArtistDao ad, JAlbumDao ald, Ehcache indexCache) {
        super(d, u);
        this.util = u;
        this.mediaFileService = m;
        this.musicIndexService = mi;
        this.artistDao = ad;
        this.albumDao = ald;
        this.indexCache = indexCache;
        setRootId(CONTAINER_ID_INDEX_ID3_PREFIX);
    }

    static final int getIDAndIncrement() {
        return INDEX_IDS.getAndIncrement();
    }

    @Override
    public void addChild(DIDLContent didl, Id3Wrapper item) {
        if (!item.isSong()) {
            didl.addContainer(createContainer(item));
        } else {
            didl.addItem(getDispatcher().getMediaFileProcessor().createItem(item.getSong()));
        }
    }

    @Override
    public void addItem(DIDLContent didl, Id3Wrapper item) {
        if (!item.isSong() || item.isIndex()) {
            didl.addContainer(createContainer(item));
        } else {
            didl.addItem(getDispatcher().getMediaFileProcessor().createItem(item.getSong()));
        }
    }

    @Override
    public BrowseResult browseObjectMetadata(String id) throws Exception {
        Id3Wrapper item = getItemById(id);
        DIDLContent didl = new DIDLContent();
        addChild(didl, item);
        return createBrowseResult(didl, 1, 1);
    }

    private void applyId(Id3Wrapper item, Container container) {
        container.setId(CONTAINER_ID_INDEX_ID3_PREFIX + OBJECT_ID_SEPARATOR + item.getId());
        container.setTitle(item.getName());
        container.setChildCount(getChildSizeOf(item));
    }

    @Override
    public Container createContainer(Id3Wrapper item) {
        int id = toRawId(item.getId());
        if (item.isAlbum()) {
            MusicAlbum container = new MusicAlbum();
            Album album = new Album();
            album.setId(id);
            container.setAlbumArtURIs(new URI[] { getDispatcher().getAlbumProcessor().createAlbumArtURI(album) });
            if (item.getArtist() != null) {
                container.setArtists(getDispatcher().getAlbumProcessor().getAlbumArtists(item.getArtist()));
            }
            container.setDescription(item.getComment());
            applyParentId(item, container);
            applyId(item, container);
            return container;
        } else if (item.isArtist()) {
            MusicArtist container = new MusicArtist();
            Artist artist = new Artist();
            artist.setId(id);
            URI uri = getDispatcher().getArtistProcessor().createArtistArtURI(artist);
            container.setProperties(Arrays.asList(new ALBUM_ART_URI(uri)));
            applyParentId(item, container);
            applyId(item, container);
            return container;
        } else {
            GenreContainer container = new GenreContainer();
            container.setParentID(CONTAINER_ID_INDEX_ID3_PREFIX);
            applyId(item, container);
            return container;
        }
    }

    @Override
    public List<Id3Wrapper> getChildren(Id3Wrapper item, long offset, long maxResults) {
        if (item.isIndex()) {
            return subList(indexesMap.get(item.getId()).getIndexId3().getArtist().stream().map(Id3Content::new)
                    .collect(toList()), offset, maxResults);
        } else if (item.isAlbum()) {
            return mediaFileService.getSongsForAlbum(offset, maxResults, item.getArtist(), item.getName()).stream()
                    .map(Id3Content::new).collect(toList());
        } else if (item.isArtist()) {
            return albumDao.getAlbumsForArtist(offset, maxResults, item.getArtist(),
                    util.isSortAlbumsByYear(item.getArtist()), util.getAllMusicFolders()).stream().map(Id3Content::new)
                    .collect(toList());
        }
        return mediaFileService.getSongsForAlbum(offset, maxResults, item.getArtist(), item.getName()).stream()
                .map(Id3Content::new).collect(toList());
    }

    @Override
    public int getChildSizeOf(Id3Wrapper item) {
        if (item.isIndex()) {
            return indexesMap.get(item.getId()).getChildSize();
        }
        return item.getChildSize();
    }

    @Override
    public Id3Wrapper getItemById(String ids) {
        int id = toRawId(ids);
        if (-1 > id) {
            return indexesMap.get(ids);
        } else if (isArtistId(ids)) {
            return new Id3Content(artistDao.getArtist(id));
        } else if (isAlbumId(ids)) {
            return new Id3Content(albumDao.getAlbum(id));
        }
        return new Id3Content(mediaFileService.getMediaFile(id));
    }

    @Override
    public int getItemCount() {
        refreshIndex();
        return topNodes.size();
    }

    @Override
    public List<Id3Wrapper> getItems(long offset, long maxResults) {
        List<Id3Wrapper> result = new ArrayList<>();
        if (offset < getItemCount()) {
            int count = min((int) (offset + maxResults), getItemCount());
            for (int i = (int) offset; i < count; i++) {
                result.add(topNodes.get(i));
            }
        }
        return result;
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.index");
    }

    private void applyParentId(Id3Wrapper artist, MusicArtist container) {
        indexesMap.entrySet()
                .forEach(e -> indexesMap.get(e.getKey()).getIndexId3().getArtist().stream()
                        .filter(a -> a.getId().equals(artist.getId())).findFirst()
                        .ifPresent(a -> container.setParentID(e.getKey())));
    }

    private void applyParentId(Id3Wrapper album, MusicAlbum container) {
        Artist artist = artistDao.getArtist(album.getArtist());
        if (!isEmpty(artist)) {
            container.setParentID(createArtistId(artist.getId()));
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (IndexID3) Not reusable
    private void refreshIndex() {
        Element element = indexCache.getQuiet(IndexCacheKey.ID3);
        boolean expired = isEmpty(element) || indexCache.isExpired(element);
        synchronized (LOCK) {
            if (isEmpty(content) || 0 == content.getIndex().stream().flatMap(i -> i.getArtist().stream()).count()
                    || expired) {
                INDEX_IDS.set(Integer.MIN_VALUE);
                content = new ArtistsID3();
                List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, util.getAllMusicFolders());
                SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> indexedArtists = musicIndexService
                        .getIndexedArtists(artists);
                final Function<Artist, ArtistID3> toId3 = (a) -> {
                    ArtistID3 result = new ArtistID3();
                    result.setId(createArtistId(a.getId()));
                    result.setName(a.getName());
                    result.setAlbumCount(a.getAlbumCount());
                    return result;
                };
                for (Map.Entry<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> entry : indexedArtists
                        .entrySet()) {
                    IndexID3 index = new IndexID3();
                    index.setName(entry.getKey().getIndex());
                    content.getIndex().add(index);
                    entry.getValue().forEach(s -> index.getArtist().add(toId3.apply(s.getArtist())));
                }
                indexCache.put(new Element(IndexCacheKey.ID3, content));
                topNodes = content.getIndex().stream().map(i -> new Id3Content(i)).collect(toList());
                indexesMap = new ConcurrentHashMap<>();
                topNodes.forEach(i -> indexesMap.put(i.getId(), i));
            }
        }
    }

    private int min(Integer... integers) {
        int min = Integer.MAX_VALUE;
        for (int i : integers) {
            min = Integer.min(min, i);
        }
        return min;
    }

    private int toRawId(String prefixed) {
        return Integer.parseInt(prefixed.replaceAll("^.*:", ""));
    }

    private static final String TYPE_PREFIX_ARTIST = "artist:";
    private static final String TYPE_PREFIX_ALBUM = "album:";

    static final boolean isArtistId(String id) {
        return id.startsWith(TYPE_PREFIX_ARTIST);
    }

    static final String createArtistId(String id) {
        if (isArtistId(id)) {
            return id;
        }
        return TYPE_PREFIX_ARTIST.concat(id);
    }

    final String createArtistId(int id) {
        return TYPE_PREFIX_ARTIST.concat(String.valueOf(id));
    }

    static final boolean isAlbumId(String id) {
        return id.startsWith(TYPE_PREFIX_ALBUM);
    }

    static final String createAlbumId(String id) {
        if (isArtistId(id)) {
            return id;
        }
        return TYPE_PREFIX_ALBUM.concat(id);
    }

    static class Id3Content implements Id3Wrapper {

        private final String id;
        private IndexID3 index;
        private MediaFile song;

        private String name;
        private String artist;
        private String comment;
        private int childCount;

        public Id3Content(IndexID3 index) {
            this.id = String.valueOf(getIDAndIncrement());
            name = index.getName();
            childCount = index.getArtist().size();
            this.index = index;
        }

        public Id3Content(ArtistID3 a) {
            id = createArtistId(a.getId());
            name = a.getName();
            artist = a.getName();
            childCount = a.getAlbumCount();
        }

        public Id3Content(Artist a) {
            id = createArtistId(String.valueOf(a.getId()));
            name = a.getName();
            artist = a.getName();
            childCount = a.getAlbumCount();
        }

        public Id3Content(Album album) {
            id = createAlbumId(String.valueOf(album.getId()));
            name = album.getName();
            artist = album.getArtist();
            comment = album.getComment();
            childCount = album.getSongCount();
        }

        public Id3Content(MediaFile song) {
            id = String.valueOf(song.getId());
            name = song.getTitle();
            artist = song.getAlbumArtist();
            comment = song.getComment();
            this.song = song;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public IndexID3 getIndexId3() {
            return index;
        }

        @Override
        public String getArtist() {
            return artist;
        }

        @Override
        public boolean isIndex() {
            return !isEmpty(index);
        }

        @Override
        public boolean isArtist() {
            return isArtistId(id);
        }

        @Override
        public boolean isAlbum() {
            return isAlbumId(id);
        }

        @Override
        public boolean isSong() {
            return !isEmpty(song);
        }

        @Override
        public String getComment() {
            return comment;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public MediaFile getSong() {
            return song;
        }

        @Override
        public int getChildSize() {
            return childCount;
        }

    }

}
