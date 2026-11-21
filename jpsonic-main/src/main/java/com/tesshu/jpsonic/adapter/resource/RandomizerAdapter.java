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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.adapter.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.infrastructure.core.RandomNumberGenerator;
import com.tesshu.jpsonic.persistence.api.repository.MediaFileDao;
import org.springframework.stereotype.Component;

@Component
class RandomizerAdapter {

    private final MediaFileDao mediaFileDao;
    private final RandomNumberGenerator randomGenerator;
    private final RandomizerCache cache;

    RandomizerAdapter(MediaFileDao mediaFileDao, RandomNumberGenerator randomGenerator,
            RandomizerCache cache) {
        super();
        this.mediaFileDao = mediaFileDao;
        this.randomGenerator = randomGenerator;
        this.cache = cache;
    }

    List<MediaFile> findRandomSongsByArtist(List<MusicFolder> folders, Artist artist, long offset,
            long count, int cacheMax) {

        final List<MediaFile> result = new ArrayList<>();

        // Define logic to extract a sublist (offset + count) from the source list
        Consumer<List<MediaFile>> addSubsetToResult = files -> files
            .stream()
            .skip(offset)
            .limit(count)
            .forEach(result::add);

        // Try retrieving from cache first
        cache
            .getCache(RandomizerCacheKeys.random.songByArtist, cacheMax, folders, artist.name())
            .ifPresent(addSubsetToResult);

        if (!result.isEmpty()) {
            return result; // Return if cached result exists
        }

        // Get random songs for the artist from the database
        List<MediaFile> songs = mediaFileDao
            .getDomainRandomSongsForAlbumArtist(cacheMax, artist.name(), folders,
                    (range, limit) -> {
                        // Generate a list of unique random integers within [0, range)
                        List<Integer> randomIndices = new ArrayList<>();
                        while (randomIndices.size() < Math.min(limit, range)) {
                            int random = randomGenerator.nextInt(range);
                            if (!randomIndices.contains(random)) {
                                randomIndices.add(random);
                            }
                        }
                        return randomIndices;
                    });

        // Cache the retrieved songs for future access
        cache
            .putCache(RandomizerCacheKeys.random.songByArtist, cacheMax, folders, songs,
                    artist.name());

        // Add the subset (offset + count) to the result
        addSubsetToResult.accept(songs);

        return result;
    }
}
