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
package com.tesshu.jpsonic.service.sort;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileComparator;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.Collator;
import java.util.Comparator;
import java.util.regex.Pattern;

import static org.springframework.util.ObjectUtils.isEmpty;

@Component
public class JpsonicComparators {

    private final Pattern isVarious = Pattern.compile("^various.*$");

    @Autowired
    private CollatorProvider collatorProvider;

    @Autowired
    private SettingsService settingsService;

    public Comparator<Artist> naturalArtistOrder() {
        return new JpComparator<Artist>() {
            @Override
            public int compare(Artist o1, Artist o2) {
                return getCollator().compare(
                        StringUtils.defaultIfBlank(o1.getReading(), o1.getName()),
                        StringUtils.defaultIfBlank(o2.getReading(), o2.getName()));
            }
        };
    }

    public Comparator<Album> naturalAlbumOrder() {
        return new JpComparator<Album>() {
            @Override
            public int compare(Album o1, Album o2) {
                return getCollator().compare(
                        StringUtils.defaultIfBlank(o1.getNameReading(), o1.getName()),
                        StringUtils.defaultIfBlank(o2.getNameReading(), o2.getName()));
            }
        };
    }

    public MediaFileComparator naturalMediaFileOrder() {
        return naturalMediaFileOrder(null);
    }

    public MediaFileComparator naturalMediaFileOrder(MediaFile parent) {

        boolean isSortAlbumsByYear = 
                (isEmpty(parent)
                 || isEmpty(parent.getArtist())
                 || !settingsService.isProhibitSortVarious()
                 || !isVarious.matcher(parent.getArtist().toLowerCase()).matches())
                && settingsService.isSortAlbumsByYear();

        MediaFileComparator mediaFileComparator = new JpMediaFileComparator(
                isSortAlbumsByYear,
                settingsService.isSortAlphanum(),
                settingsService.getCollator());

        return mediaFileComparator;

    }

    private abstract class JpComparator<T> implements Comparator<T> {

        private final Collator collator = collatorProvider.getCollator();

        public Collator getCollator() {
            return collator;
        }

    }

}
