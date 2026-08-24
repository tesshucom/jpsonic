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

import java.util.List;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy.AlbumSortOrder;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import org.springframework.stereotype.Component;

@Component
class AlbumProviderAdapter implements AlbumProvider {

    private final AlbumDao albumDao;

    AlbumProviderAdapter(AlbumDao albumDao) {
        super();
        this.albumDao = albumDao;
    }

    @Override
    public int countAlbums(List<MusicFolder> folders) {
        return albumDao.countAlbums(folders);
    }

    @Override
    public List<Album> findAlbums(List<MusicFolder> folders, AlbumSortOrder order, long offset,
            long count) {
        return albumDao.findAlbums(folders, order, offset, count);
    }

    @Override
    public List<Album> findChildren(List<MusicFolder> folders, Artist parent, AlbumSortOrder order,
            long offset, long count) {
        return albumDao.findAlbums(folders, parent, order, offset, count);
    }

    @Override
    public List<Album> findNewestAlbums(List<MusicFolder> folders, long offset, long count) {
        return albumDao.findNewestAlbums(folders, offset, count);
    }

    @Override
    public Album requireAlbum(int id) {
        return albumDao.requireAlbum(id);
    }
}
