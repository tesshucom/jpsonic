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
package com.tesshu.jpsonic.domain;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileComparator;
import org.airsonic.player.service.SettingsService;
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

    public Collator naturalOrder() {
        Collator collator = collatorProvider.getCollator();
        return settingsService.isSortAlphanum() ? new AlphanumWrapper(collator) : collator;
    }

    public Comparator<Artist> artistOrder() {
        return new Comparator<Artist>() {

            private final Collator c = naturalOrder();

            @Override
            public int compare(Artist o1, Artist o2) {
                if (-1 != o1.getOrder() && -1 != o2.getOrder()) {
                    return o1.getOrder() - o2.getOrder();
                }
                return c.compare(o1.getReading(), o2.getReading());
            }
        };
    }

    public Comparator<Album> albumOrder() {
        return new Comparator<Album>() {

            private final Collator c = naturalOrder();

            @Override
            public int compare(Album o1, Album o2) {
                if (-1 != o1.getOrder() && -1 != o2.getOrder()) {
                    return o1.getOrder() - o2.getOrder();
                }
                return c.compare(o1.getNameReading(), o2.getNameReading());
            }
        };
    }

    public MediaFileComparator mediaFileOrder() {
        return mediaFileOrder(null);
    }

    public MediaFileComparator mediaFileOrder(MediaFile parent) {

        boolean isSortAlbumsByYear = settingsService.isSortAlbumsByYear()
                && ((isEmpty(parent) || isEmpty(parent.getArtist())) || !(settingsService.isProhibitSortVarious()
                        && isVarious.matcher(parent.getArtist().toLowerCase()).matches()));

        MediaFileComparator mediaFileComparator = new JpMediaFileComparator(
                isSortAlbumsByYear,
                naturalOrder());

        return mediaFileComparator;

    }

}
