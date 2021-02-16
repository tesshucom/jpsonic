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

package com.tesshu.jpsonic.service;

import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileComparator;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Utility class for injecting into legacy MediaFileService. Supplement processing that is lacking in legacy services.
 */
@Component
@DependsOn({ "japaneseReadingUtils", "jpsonicComparators" })
public class MediaFileServiceUtils {

    private final JapaneseReadingUtils utils;
    private final JpsonicComparators jpsonicComparator;

    public MediaFileServiceUtils(JapaneseReadingUtils utils, JpsonicComparators jpsonicComparator) {
        super();
        this.utils = utils;
        this.jpsonicComparator = jpsonicComparator;
    }

    /**
     * Compensate for missing properties when initial creation of MediaFile (when performing meta-analysis).
     */
    public void analyze(MediaFile m) {
        utils.analyze(m);
    }

    /**
     * Returns the sorting rules for the child elements of the specified MediaFile. MediaFile's sorting rules depend on
     * the hierarchy to which it belongs.
     */
    public MediaFileComparator mediaFileOrder(MediaFile parent) {
        return jpsonicComparator.mediaFileOrder(parent);
    }

}
