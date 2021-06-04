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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.tesshu.jpsonic.domain.MusicFolder;

public final class MusicFolderTestDataUtils {

    private static final String BASE_RESOURCES = "/MEDIAS/";

    private MusicFolderTestDataUtils() {
    }

    public static String resolveBaseMediaPath() {
        return MusicFolderTestDataUtils.class.getResource(BASE_RESOURCES).toString().replace("file:", "");
    }

    public static String resolveMusicFolderPath() {
        return MusicFolderTestDataUtils.resolveBaseMediaPath() + "Music";
    }

    public static String resolveMusic2FolderPath() {
        return MusicFolderTestDataUtils.resolveBaseMediaPath() + "Music2";
    }

    public static String resolveMusic3FolderPath() {
        return MusicFolderTestDataUtils.resolveBaseMediaPath() + "Music3";
    }

    public static List<MusicFolder> getTestMusicFolders() {
        List<MusicFolder> liste = new ArrayList<>();
        File musicDir = new File(MusicFolderTestDataUtils.resolveMusicFolderPath());
        MusicFolder musicFolder = new MusicFolder(1, musicDir, "Music", true, new Date());
        liste.add(musicFolder);

        File music2Dir = new File(MusicFolderTestDataUtils.resolveMusic2FolderPath());
        MusicFolder musicFolder2 = new MusicFolder(2, music2Dir, "Music2", true, new Date());
        liste.add(musicFolder2);
        return liste;
    }
}
