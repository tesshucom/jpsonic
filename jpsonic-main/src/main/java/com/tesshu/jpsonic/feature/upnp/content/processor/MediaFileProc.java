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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.model.SearchResult;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.SearchResultProcessor;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.concurrent.ConcurrentUtils;
import com.tesshu.jpsonic.infrastructure.policy.RuntimeOrderResolver;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class MediaFileProc extends DirectChildrenContentProc<MediaFile, MediaFile>
        implements SearchResultProcessor<MediaFile>, RuntimeOrderResolver {

    private static final MediaFile.Type[] EXCLUDED_TYPES = Stream
        .of(MediaFile.Type.PODCAST, MediaFile.Type.VIDEO)
        .toArray(size -> new MediaFile.Type[size]);

    private final MusicFolderProvider musicFolderProvider;
    private final MediaFileProvider mediaFileProvider;
    private final SettingsFacade settingsFacade;
    private final UPnPDIDLFactory factory;

    MediaFileProc(MusicFolderProvider musicFolderProvider, MediaFileProvider mediaFileProvider,
            SettingsFacade settingsFacade, UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.mediaFileProvider = mediaFileProvider;
        this.settingsFacade = settingsFacade;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.MEDIA_FILE;
    }

    @Override
    public Container createContainer(MediaFile entity) {
        int childSize = getChildSizeOf(entity);
        return switch (entity.type()) {
        case ALBUM -> factory.toAlbum(entity, childSize);
        case DIRECTORY -> factory.toArtist(entity, childSize);
        default -> throw new IllegalArgumentException("Unexpected value: " + entity.type());
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
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        return mediaFileProvider
            .findChildren(musicFolderProvider.getGuestFolders(), offset, count, EXCLUDED_TYPES);
    }

    @Override
    public int getDirectChildrenCount() {
        return mediaFileProvider
            .countChildren(musicFolderProvider.getGuestFolders(), EXCLUDED_TYPES);
    }

    @Override
    public MediaFile getDirectChild(String id) {
        return mediaFileProvider.requireMediaFile(Integer.parseInt(id));
    }

    @Override
    public List<MediaFile> getChildren(MediaFile entity, long offset, long count) {
        ChildOrder order = resolveChildOrder(entity, settingsFacade);
        return mediaFileProvider.findChildren(entity, order, offset, count, EXCLUDED_TYPES);
    }

    @Override
    public int getChildSizeOf(MediaFile entity) {
        return mediaFileProvider.countChildren(entity, EXCLUDED_TYPES);
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile entity) {
        addDirectChild(parent, entity);
    }

    @Override
    public BrowseResult toBrowseResult(SearchResult<MediaFile> searchResult) {
        DIDLContent parent = new DIDLContent();
        try {
            searchResult.items().forEach(song -> addChild(parent, song));
            return createBrowseResult(parent, (int) parent.getCount(), searchResult.totalHits());
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            return null;
        }
    }
}
