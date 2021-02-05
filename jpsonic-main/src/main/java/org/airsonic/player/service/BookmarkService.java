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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.service;

import java.util.List;
import java.util.Objects;

import org.airsonic.player.dao.BookmarkDao;
import org.airsonic.player.domain.Bookmark;
import org.airsonic.player.domain.MediaFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookmarkService {

    private final BookmarkDao dao;

    @Autowired
    public BookmarkService(BookmarkDao dao) {
        this.dao = dao;
    }

    public Bookmark getBookmarkForUserAndMediaFile(String username, MediaFile mediaFile) {
        return dao.getBookmarks(username).stream()
                .filter(bookmark -> Objects.equals(mediaFile.getId(), bookmark.getMediaFileId())).findFirst()
                .orElse(null);
    }

    public void createOrUpdateBookmark(Bookmark bookmark) {
        dao.createOrUpdateBookmark(bookmark);
    }

    public void deleteBookmark(String username, int mediaFileId) {
        dao.deleteBookmark(username, mediaFileId);
    }

    public List<Bookmark> getBookmarks(String username) {
        return dao.getBookmarks(username);
    }
}
