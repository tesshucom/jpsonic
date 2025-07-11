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

package com.tesshu.jpsonic;

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.domain.MusicFolder;

public final class MusicFolderTestDataUtils {

    private static final String BASE_RESOURCES = "/MEDIAS/";

    private MusicFolderTestDataUtils() {
    }

    public static String resolveBaseMediaPath() {
        try {
            return Path
                .of(MusicFolderTestDataUtils.class.getResource(BASE_RESOURCES).toURI())
                .toString() + File.separator;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(BASE_RESOURCES + "is not found", e);
        }
    }

    public static String resolveMusicFolderPath() {
        return resolveBaseMediaPath() + "Music";
    }

    public static String resolveMusic2FolderPath() {
        return resolveBaseMediaPath() + "Music2";
    }

    public static String resolveMusic3FolderPath() {
        return resolveBaseMediaPath() + "Music3";
    }

    public static List<MusicFolder> getTestMusicFolders() {
        return Arrays
            .asList(new MusicFolder(1, resolveMusicFolderPath(), "Music", true, now(), 0, false),
                    new MusicFolder(2, resolveMusic2FolderPath(), "Music2", true, now(), 1, false));
    }
}
