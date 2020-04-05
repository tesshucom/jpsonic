/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package org.airsonic.player.service.upnp.processor;

import com.tesshu.jpsonic.service.JMediaFileService;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolderContent;
import org.airsonic.player.domain.MusicIndex;
import org.airsonic.player.service.MusicIndexService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.airsonic.player.spring.EhcacheConfiguration.IndexCacheKey;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.GenreContainer;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.airsonic.player.util.Util.subList;
import static org.springframework.util.ObjectUtils.isEmpty;

@Service
public class IndexUpnpProcessor extends UpnpContentProcessor<MediaFile, MediaFile> {

    private final AtomicInteger INDEX_IDS = new AtomicInteger(Integer.MIN_VALUE);

    private final UpnpProcessorUtil util;

    private final JMediaFileService mediaFileService;

    private final MusicIndexService musicIndexService;

    private final Ehcache indexCache;

    private MusicFolderContent content;

    private Map<Integer, MediaIndex> indexesMap;

    private List<MediaFile> topNodes;

    public IndexUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JMediaFileService m, MusicIndexService mi, Ehcache indexCache) {
        super(d, u);
        this.util = u;
        this.mediaFileService = m;
        this.musicIndexService = mi;
        this.indexCache = indexCache;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_INDEX_PREFIX);
    }

    public void addChild(DIDLContent didl, MediaFile child) {
        if (!child.isFile()) {
            didl.addContainer(createContainer(child));
        } else {
            didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
        }
    }

    public void addItem(DIDLContent didl, MediaFile item) {
        if (!item.isFile() || isIndex(item)) {
            didl.addContainer(createContainer(item));
        } else {
            didl.addItem(getDispatcher().getMediaFileProcessor().createItem(item));
        }
    }

    @Override
    public BrowseResult browseObjectMetadata(String id) throws Exception {
        MediaFile item = getItemById(id);
        DIDLContent didl = new DIDLContent();
        addChild(didl, item);
        return createBrowseResult(didl, 1, 1);
    }

    private void applyId(MediaFile item, Container container) {
        container.setId(UpnpProcessDispatcher.CONTAINER_ID_INDEX_PREFIX + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + item.getId());
        container.setTitle(item.getName());
        container.setChildCount(getChildSizeOf(item));
        if (!isIndex(item) && !mediaFileService.isRoot(item)) {
            MediaFile parent = mediaFileService.getParentOf(item);
            if (parent != null) {
                container.setParentID(String.valueOf(parent.getId()));
            }
        } else {
            container.setParentID(UpnpProcessDispatcher.CONTAINER_ID_INDEX_PREFIX);
        }
    }

    public Container createContainer(MediaFile item) {
        if (item.isAlbum()) {
            MusicAlbum container = new MusicAlbum();
            container.setAlbumArtURIs(new URI[] { getDispatcher().getMediaFileProcessor().createAlbumArtURI(item) });
            if (item.getArtist() != null) {
                container.setArtists(getDispatcher().getAlbumProcessor().getAlbumArtists(item.getArtist()));
            }
            container.setDescription(item.getComment());
            applyId(item, container);
            return container;
        } else if (isIndex(item.getId())) {
            GenreContainer container = new GenreContainer();
            applyId(item, container);
            return container;
        } else {
            MusicArtist container = new MusicArtist();
            applyId(item, container);
            return container;
        }
    }

    @Override
    public List<MediaFile> getChildren(MediaFile item, long offset, long maxResults) {
        if (isIndex(item)) {
            MusicIndex index = indexesMap.get(item.getId()).getDeligate();
            // refactoring(Few cases are actually a problem)
            return subList(content.getIndexedArtists().get(index).stream().flatMap(s -> s.getMediaFiles().stream()).collect(Collectors.toList()), offset, maxResults);
        }
        if (item.isAlbum()) {
            return mediaFileService.getSongsForAlbum(offset, maxResults, item);
        }
        if (MediaType.DIRECTORY == item.getMediaType()) {
            return mediaFileService.getChildrenOf(item, offset, maxResults, util.isSortAlbumsByYear(item.getArtist()));
        }
        return mediaFileService.getChildrenOf(item, offset, maxResults, false);
    }

    @Override
    public int getChildSizeOf(MediaFile item) {
        if (isIndex(item)) {
            return content.getIndexedArtists().get(indexesMap.get(item.getId()).getDeligate()).size();
        }
        return mediaFileService.getChildSizeOf(item);
    }

    public MediaFile getItemById(String ids) {
        int id = Integer.parseInt(ids);
        if (isIndex(id)) {
            return indexesMap.get(id);
        }
        return mediaFileService.getMediaFile(id);
    }

    @Override
    public int getItemCount() {
        refreshIndex();
        return topNodes.size();
    }

    @Override
    public List<MediaFile> getItems(long offset, long maxResults) {
        List<MediaFile> result = new ArrayList<MediaFile>();
        if (offset < getItemCount()) {
            int count = min((int) (offset + maxResults), getItemCount());
            for (int i = (int) offset; i < count; i++) {
                result.add(topNodes.get(i));
            }
        }
        return result;
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dlna.title.index");
    }

    private final synchronized void refreshIndex() {
        Element element = indexCache.getQuiet(IndexCacheKey.FILE_STRUCTURE);
        boolean expired = isEmpty(element) || indexCache.isExpired(element);
        if (isEmpty(content) || 0 == content.getIndexedArtists().size() || expired) {
            INDEX_IDS.set(Integer.MIN_VALUE);
            content = musicIndexService.getMusicFolderContent(util.getAllMusicFolders(), true);
            indexCache.put(new Element(IndexCacheKey.FILE_STRUCTURE, content));
            List<MediaIndex> indexes = content.getIndexedArtists().keySet().stream().map(mi -> new MediaIndex(mi)).collect(Collectors.toList());
            indexesMap = new HashMap<>();
            indexes.forEach(i -> indexesMap.put(i.getId(), i));
            topNodes = Stream.concat(indexes.stream(), content.getSingleSongs().stream()).collect(Collectors.toList());
        }
    }

    private final boolean isIndex(int id) {
        return -1 > id;
    }

    private final boolean isIndex(MediaFile item) {
        return isIndex(item.getId());
    }

    private final int min(Integer... integers) {
        int min = Integer.MAX_VALUE;
        for (int i : integers) {
            min = Integer.min(min, i);
        }
        return min;
    }

    private class MediaIndex extends MediaFile {

        private final MusicIndex deligate;
        private final int id;

        public MediaIndex(MusicIndex deligate) {
            this.deligate = deligate;
            this.id = INDEX_IDS.getAndIncrement();
        }

        public MusicIndex getDeligate() {
            return deligate;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getName() {
            return deligate.getIndex();
        }

    }

}
