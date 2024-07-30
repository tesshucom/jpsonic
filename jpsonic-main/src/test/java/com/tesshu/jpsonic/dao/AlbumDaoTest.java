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

package com.tesshu.jpsonic.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.dao.dialect.DialectAlbumDao;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.RowMapper;

class AlbumDaoTest {

    private TemplateWrapper templateWrapper;
    private AlbumDao albumDao;

    @BeforeEach
    public void setup() {
        this.templateWrapper = Mockito.mock(TemplateWrapper.class);
        albumDao = new AlbumDao(templateWrapper, Mockito.mock(DialectAlbumDao.class));
    }

    @Documented
    private @interface AlphabeticalOps {
        @interface Conditions {
            @interface ByArtist {
                @interface True {
                }

                @interface False {
                }
            }

            @interface IgnoreCase {
                @interface True {
                }

                /**
                 * Using this case is deprecated. #2441
                 */
                @interface False {
                }
            }
        }
    }

    @Nested
    class GetAlphabeticalAlbumsTest {

        List<MusicFolder> folders;

        @BeforeEach
        public void setup() {
            MusicFolder folder = new MusicFolder("/Music", "Music", true, null, false);
            folders = List.of(folder);
        }

        private String getJoin(ArgumentCaptor<String> queryCaptor) {
            String query = queryCaptor.getValue();
            return query.replaceAll("\n", "").replaceAll("^.*from\salbum", "").replaceAll("where.*$", "").trim();
        }

        private String getOrderBy(ArgumentCaptor<String> queryCaptor) {
            String query = queryCaptor.getValue();
            return query.replaceAll("\n", "").replaceAll("^.*order\sby", "").replaceAll("limit.*$", "").trim();
        }

        @SuppressWarnings("unchecked")
        private ArgumentCaptor<String> getQuery(boolean byArtist, boolean ignoreCase) {
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.when(
                    templateWrapper.namedQuery(queryCaptor.capture(), Mockito.any(RowMapper.class), Mockito.anyMap()))
                    .thenReturn(Collections.emptyList());
            albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, byArtist, ignoreCase, folders);
            return queryCaptor;
        }

        @AlphabeticalOps.Conditions.ByArtist.True
        @AlphabeticalOps.Conditions.IgnoreCase.True
        @Test
        void c00() {
            ArgumentCaptor<String> query = getQuery(true, true);
            assertEquals("left join artist on artist.present and artist.name = album.artist", getJoin(query));
            assertEquals("artist_order, album_order", getOrderBy(query));
        }

        @AlphabeticalOps.Conditions.ByArtist.True
        @AlphabeticalOps.Conditions.IgnoreCase.False
        @Test
        void c01() {
            ArgumentCaptor<String> query = getQuery(true, false);
            assertEquals("left join artist on artist.present and artist.name = album.artist", getJoin(query));
            assertEquals("artist.reading, album.name_reading", getOrderBy(query));
        }

        @AlphabeticalOps.Conditions.ByArtist.False
        @AlphabeticalOps.Conditions.IgnoreCase.True
        @Test
        void c02() {
            ArgumentCaptor<String> query = getQuery(false, true);
            assertEquals("", getJoin(query));
            assertEquals("album_order", getOrderBy(query));
        }

        @AlphabeticalOps.Conditions.ByArtist.False
        @AlphabeticalOps.Conditions.IgnoreCase.False
        @Test
        void c03() {
            ArgumentCaptor<String> query = getQuery(false, false);
            assertEquals("", getJoin(query));
            assertEquals("album.name_reading", getOrderBy(query));
        }
    }
}
