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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.MusicIndex;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.upnp.processor.composite.IndexOrSong;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class IndexProc extends DirectChildrenContentProc<IndexOrSong, MediaFile> {

    private static final MediaType[] EXCLUDED_TYPES = Stream.of(MediaType.PODCAST, MediaType.VIDEO)
            .toArray(size -> new MediaType[size]);

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final MediaFileService mediaFileService;
    private final MusicIndexService musicIndexService;

    public IndexProc(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService,
            MusicIndexService musicIndexService) {
        super();
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
        this.musicIndexService = musicIndexService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.INDEX;
    }

    @Override
    public Container createContainer(IndexOrSong indexOrSong) {
        return factory.toMusicIndex(indexOrSong.getMusicIndex(), getProcId(), getChildSizeOf(indexOrSong));
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
        List<MusicFolder> folders = util.getGuestFolders();
        return Stream.concat(
                musicIndexService.getMusicFolderContentCounts(folders, EXCLUDED_TYPES).indexCounts().keySet().stream()
                        .map(IndexOrSong::new),
                mediaFileService.getDirectChildFiles(folders, 0, Integer.MAX_VALUE, EXCLUDED_TYPES).stream()
                        .map(IndexOrSong::new))
                .skip(offset).limit(count).toList();
    }

    @Override
    public int getDirectChildrenCount() {
        MusicFolderContent.Counts counts = musicIndexService.getMusicFolderContentCounts(util.getGuestFolders(),
                EXCLUDED_TYPES);
        return counts.indexCounts().size() + counts.singleSongCounts();
    }

    @Override
    public IndexOrSong getDirectChild(String id) {
        Optional<MusicIndex> op = musicIndexService.getMusicFolderContentCounts(util.getGuestFolders(), EXCLUDED_TYPES)
                .indexCounts().keySet().stream().filter(i -> i.getIndex().equals(id)).findFirst();
        if (op.isPresent()) {
            return new IndexOrSong(op.get());
        }
        MediaFile song = mediaFileService.getMediaFile(id);
        if (Objects.nonNull(song)) {
            return new IndexOrSong(song);
        }
        return null;
    }

    @Override
    public List<MediaFile> getChildren(IndexOrSong indexOrSong, long offset, long count) {
        if (indexOrSong.isMusicIndex()) {
            return mediaFileService.getChildrenOf(util.getGuestFolders(), indexOrSong.getMusicIndex(), offset, count,
                    EXCLUDED_TYPES);
        }
        return Collections.emptyList();
    }

    @Override
    public int getChildSizeOf(IndexOrSong indexOrSong) {
        if (indexOrSong.isMusicIndex()) {
            MusicFolderContent.Counts counts = musicIndexService.getMusicFolderContentCounts(util.getGuestFolders(),
                    EXCLUDED_TYPES);
            if (counts != null) {
                return counts.indexCounts().get(indexOrSong.getMusicIndex());
            }
        }
        return 0;
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile mediaFile) {
        switch (mediaFile.getMediaType()) {
        case DIRECTORY -> {
            int childCounts = mediaFileService.getChildSizeOf(mediaFile, EXCLUDED_TYPES);
            parent.addContainer(factory.toArtist(mediaFile, childCounts));
        }
        case ALBUM -> {
            int childCounts = mediaFileService.getChildSizeOf(mediaFile, EXCLUDED_TYPES);
            parent.addContainer(factory.toAlbum(mediaFile, childCounts));
        }
        case MUSIC -> parent.addItem(factory.toMusicTrack(mediaFile));
        case PODCAST, AUDIOBOOK, VIDEO -> {
        }
        default -> {
        }
        }
    }
}
