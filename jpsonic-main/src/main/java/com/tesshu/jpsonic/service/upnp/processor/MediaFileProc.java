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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import com.tesshu.jpsonic.dao.MediaFileDao.ChildOrder;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ParamSearchResult;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class MediaFileProc extends DirectChildrenContentProc<MediaFile, MediaFile> {

    private static final MediaType[] EXCLUDED_TYPES = Stream.of(MediaType.PODCAST, MediaType.VIDEO)
            .toArray(size -> new MediaType[size]);
    public static final int SINGLE_MUSIC_FOLDER = 1;

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final MediaFileService mediaFileService;

    public MediaFileProc(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService) {
        super();
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.MEDIA_FILE;
    }

    @Override
    public Container createContainer(MediaFile entity) {
        int childSize = getChildSizeOf(entity);
        return switch (entity.getMediaType()) {
            case ALBUM -> factory.toAlbum(entity, childSize);
            case DIRECTORY -> factory.toArtist(entity, childSize);
            default -> throw new IllegalArgumentException("Unexpected value: " + entity.getMediaType());
        };
    }

    @Override
    public void addDirectChild(DIDLContent parent, MediaFile entity) {
        if (entity.isFile()) {
            parent.addItem(factory.toMusicTrack(entity));
        } else {
            parent.addContainer(createContainer(entity));
        }
    }

    @Override
    public List<MediaFile> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        return mediaFileService.getChildrenOf(util.getGuestFolders(), offset, count, EXCLUDED_TYPES);
    }

    @Override
    public int getDirectChildrenCount() {
        return mediaFileService.getChildSizeOf(util.getGuestFolders(), EXCLUDED_TYPES);
    }

    @Override
    public MediaFile getDirectChild(String id) {
        return mediaFileService.getMediaFileStrict(Integer.parseInt(id));
    }

    @Override
    public List<MediaFile> getChildren(MediaFile entity, long offset, long count) {
        ChildOrder childOrder = ChildOrder.BY_ALPHA;
        if (entity.isAlbum()) {
            childOrder = ChildOrder.BY_TRACK;
        } else if (util.isSortAlbumsByYear(entity.getName())) {
            childOrder = ChildOrder.BY_YEAR;
        }
        return mediaFileService.getChildrenOf(entity, offset, count, childOrder, EXCLUDED_TYPES);
    }

    @Override
    public int getChildSizeOf(MediaFile entity) {
        return mediaFileService.getChildSizeOf(entity, EXCLUDED_TYPES);
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile entity) {
        addDirectChild(parent, entity);
    }

    public BrowseResult toBrowseResult(ParamSearchResult<MediaFile> searchResult) {
        DIDLContent parent = new DIDLContent();
        try {
            searchResult.getItems().forEach(song -> addChild(parent, song));
            return createBrowseResult(parent, (int) parent.getCount(), searchResult.getTotalHits());
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            return null;
        }
    }
}
