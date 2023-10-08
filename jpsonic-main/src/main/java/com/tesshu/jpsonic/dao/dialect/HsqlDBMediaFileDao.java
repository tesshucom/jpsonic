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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.dao.dialect;

import java.util.List;
import java.util.function.BiFunction;

import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SortCandidate;
import com.tesshu.jpsonic.spring.DatabaseConfiguration.ProfileNameConstants;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@Component
@Profile({ ProfileNameConstants.HOST })
public class HsqlDBMediaFileDao implements DialectMediaFileDao {

    private final AnsiMediaFileDao deligate;

    public HsqlDBMediaFileDao(TemplateWrapper templateWrapper) {
        deligate = new AnsiMediaFileDao(templateWrapper);
    }

    @Override
    public List<MediaFile> getChangedId3Artists(final int count, List<MusicFolder> folders, boolean withPodcast) {
        return deligate.getChangedId3Artists(count, folders, withPodcast);
    }

    @Override
    public List<MediaFile> getUnregisteredId3Artists(final int count, List<MusicFolder> folders, boolean withPodcast) {
        return deligate.getUnregisteredId3Artists(count, folders, withPodcast);
    }

    @Override
    public List<MediaFile> getChangedId3Albums(final int count, List<MusicFolder> musicFolders, boolean withPodcast) {
        return deligate.getChangedId3Albums(count, musicFolders, withPodcast);
    }

    @Override
    public List<MediaFile> getUnregisteredId3Albums(final int count, List<MusicFolder> musicFolders,
            boolean withPodcast) {
        return deligate.getUnregisteredId3Albums(count, musicFolders, withPodcast);
    }

    @Override
    public List<SortCandidate> getCopyableSortForAlbums(List<MusicFolder> folders) {
        return deligate.getCopyableSortForAlbums(folders);
    }

    @Override
    public List<SortCandidate> getCopyableSortForPersons(List<MusicFolder> folders) {
        return deligate.getCopyableSortForPersons(folders);
    }

    @Override
    public List<MediaFile> getRandomSongsForAlbumArtist(int limit, String albumArtist, List<MusicFolder> musicFolders,
            BiFunction<Integer, Integer, List<Integer>> randomCallback) {
        return deligate.getRandomSongsForAlbumArtist(limit, albumArtist, musicFolders, randomCallback);
    }

    @Override
    public List<SortCandidate> getSortForPersonWithoutSorts(List<MusicFolder> folders) {
        return deligate.getSortForPersonWithoutSorts(folders);
    }

    @Override
    public List<SortCandidate> getSortOfArtistToBeFixed(@NonNull List<SortCandidate> candidates) {
        return deligate.getSortOfArtistToBeFixed(candidates);
    }

    @Override
    public List<SortCandidate> getSortForAlbumWithoutSorts(List<MusicFolder> folders) {
        return deligate.getSortForAlbumWithoutSorts(folders);
    }

    @Override
    public List<SortCandidate> guessAlbumSorts(List<MusicFolder> folders) {
        return deligate.guessAlbumSorts(folders);
    }

    @Override
    public List<SortCandidate> guessPersonsSorts(List<MusicFolder> folders) {
        return deligate.guessPersonsSorts(folders);
    }
}
