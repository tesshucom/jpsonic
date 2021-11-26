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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.domain;

/**
 * Reserved transcode name used to transcode master data
 */
public enum Transcodings {

    MP3("mp3 audio"), FLAC("flac resampling"), FLV("flv/h264 video"), MKV("mkv video"), MP4("mp4/h264 video");

    private final String name;

    Transcodings(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Transcodings of(String s) {
        for (Transcodings value : values()) {
            if (value.getName().equals(s)) {
                return value;
            }
        }
        return null;
    }
}
