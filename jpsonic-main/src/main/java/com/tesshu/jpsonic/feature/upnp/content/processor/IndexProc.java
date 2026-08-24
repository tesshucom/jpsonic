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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.model.MusicFolderContent;
import com.tesshu.jpsonic.domain.model.MusicIndex;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicIndexProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.IndexOrSong;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class IndexProc extends DirectChildrenContentProc<IndexOrSong, MediaFile> {

    private static final MediaFile.Type[] EXCLUDED_TYPES = { MediaFile.Type.PODCAST,
            MediaFile.Type.VIDEO };
    // spotless:off
    private static final MediaFile.Type[] OTHER_THAN_MUSIC_AND_VIDEO_TYPES = {
            MediaFile.Type.ALBUM,
            MediaFile.Type.AUDIOBOOK,
            MediaFile.Type.DIRECTORY,
            MediaFile.Type.PODCAST,
            MediaFile.Type.VIDEO // Not yet supported. 
            };
    // spotless:on

    private final MusicFolderProvider musicFolderProvider;
    private final MusicIndexProvider musicIndexProvider;
    private final MediaFileProvider mediaFileProvider;
    private final UPnPDIDLFactory factory;

    IndexProc(MusicFolderProvider musicFolderProvider, MusicIndexProvider musicIndexProvider,
            MediaFileProvider mediaFileProvider, UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.musicIndexProvider = musicIndexProvider;
        this.mediaFileProvider = mediaFileProvider;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.INDEX;
    }

    @Override
    public Container createContainer(IndexOrSong indexOrSong) {
        return factory
            .toMusicIndex(getProcId(), indexOrSong.getMusicIndex(), getChildSizeOf(indexOrSong));
    }

    @Override
    public void addDirectChild(DIDLContent parent, IndexOrSong indexOrSong) {
        if (indexOrSong.isMusicIndex()) {
            parent.addContainer(createContainer(indexOrSong));
        } else {
            parent.addItem(factory.toMusicTrack(indexOrSong.getSong()));
        }
    }

    @Override
    public List<IndexOrSong> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        return Stream
            .concat(musicIndexProvider
                .countMusicFolderContent(folders, MediaFile.Type.PODCAST, MediaFile.Type.VIDEO)
                .indexCounts()
                .keySet()
                .stream()
                .map(IndexOrSong::new),
                    mediaFileProvider
                        .findChildren(folders, 0, Integer.MAX_VALUE,
                                OTHER_THAN_MUSIC_AND_VIDEO_TYPES)
                        .stream()
                        .map(IndexOrSong::new))
            .skip(offset)
            .limit(count)
            .toList();
    }

    @Override
    public int getDirectChildrenCount() {
        MusicFolderContent.Counts counts = musicIndexProvider
            .countMusicFolderContent(musicFolderProvider.getGuestFolders(), EXCLUDED_TYPES);
        return counts.indexCounts().size() + counts.singleSongCounts();
    }

    @Override
    public IndexOrSong getDirectChild(String id) {
        Optional<MusicIndex> op = musicIndexProvider
            .countMusicFolderContent(musicFolderProvider.getGuestFolders(), EXCLUDED_TYPES)
            .indexCounts()
            .keySet()
            .stream()
            .filter(i -> i.index().equals(id))
            .findFirst();
        if (op.isPresent()) {
            return new IndexOrSong(op.get());
        }
        MediaFile song = mediaFileProvider.requireMediaFile(Integer.parseInt(id));
        if (Objects.nonNull(song)) {
            return new IndexOrSong(song);
        }
        return null;
    }

    @Override
    public List<MediaFile> getChildren(IndexOrSong indexOrSong, long offset, long count) {
        if (indexOrSong.isMusicIndex()) {
            return mediaFileProvider
                .findChildren(musicFolderProvider.getGuestFolders(), indexOrSong.getMusicIndex(),
                        offset, count, EXCLUDED_TYPES);
        }
        return Collections.emptyList();
    }

    @Override
    public int getChildSizeOf(IndexOrSong indexOrSong) {
        if (indexOrSong.isMusicIndex()) {
            MusicFolderContent.Counts counts = musicIndexProvider
                .countMusicFolderContent(musicFolderProvider.getGuestFolders(), EXCLUDED_TYPES);
            if (counts != null) {
                return counts.indexCounts().get(indexOrSong.getMusicIndex());
            }
        }
        return 0;
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile mediaFile) {
        switch (mediaFile.type()) {
        case DIRECTORY -> {
            int childCounts = mediaFileProvider.countChildren(mediaFile, EXCLUDED_TYPES);
            parent.addContainer(factory.toArtist(mediaFile, childCounts));
        }
        case ALBUM -> {
            int childCounts = mediaFileProvider.countChildren(mediaFile, EXCLUDED_TYPES);
            parent.addContainer(factory.toAlbum(mediaFile, childCounts));
        }
        case MUSIC -> parent.addItem(factory.toMusicTrack(mediaFile));
        case PODCAST, AUDIOBOOK, VIDEO -> {
        }
        }
    }
}
