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

package com.tesshu.jpsonic.domain;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Enumeration of transcoding schemes. Transcoding is the process of converting
 * an audio stream to a lower bit rate.
 *
 * @author Sindre Mehus
 */
public enum TranscodeScheme {

    MAX_128(128), MAX_256(256), MAX_320(320), MAX_1411(1_411), OFF(0);

    private final int maxBitRate;

    TranscodeScheme(int maxBitRate) {
        this.maxBitRate = maxBitRate;
    }

    /**
     * Returns the maximum bit rate for this transcoding scheme.
     *
     * @return The maximum bit rate for this transcoding scheme.
     */
    public int getMaxBitRate() {
        return maxBitRate;
    }

    /**
     * Returns the strictest transcode scheme (i.e., the scheme with the lowest max
     * bitrate).
     *
     * @param other The other transcode scheme. May be <code>null</code>, in which
     *              case 'this' is returned.
     *
     * @return The strictest scheme.
     */
    public @NonNull TranscodeScheme strictest(TranscodeScheme other) {
        if (other == null || other == OFF) {
            return this;
        }

        if (this == OFF) {
            return other;
        }

        return maxBitRate < other.maxBitRate ? this : other;
    }

    /**
     * Returns a human-readable string representation of this object.
     *
     * @return A human-readable string representation of this object.
     */
    @Override
    public String toString() {
        if (this == OFF) {
            return "No limit";
        } else if (this == MAX_128) {
            return getMaxBitRate() + " Kbps";
        } else if (this == MAX_256) {
            return getMaxBitRate() + " Kbps";
        } else if (this == MAX_320) {
            return getMaxBitRate() + " Kbps (Maximum for MP3)";
        } else if (this == MAX_1411) {
            return getMaxBitRate() + " Kbps (Uncompressed CD)";
        }
        return getMaxBitRate() + " Kbps";
    }

    /**
     * Returns the enum constant corresponding to the specified scheme name.
     *
     * @param schemeName The schemeName.
     *
     * @return The corresponding enum, or default value(OFF).
     */
    public static @NonNull TranscodeScheme of(String schemeName) {
        if (MAX_128.name().equals(schemeName)) {
            return MAX_128;
        } else if (MAX_256.name().equals(schemeName)) {
            return MAX_256;
        } else if (MAX_320.name().equals(schemeName)) {
            return MAX_320;
        } else if (MAX_1411.name().equals(schemeName)) {
            return MAX_1411;
        }
        return OFF;
    }

    public static @Nullable TranscodeScheme fromMaxBitRate(int maxBitRate) {
        List<TranscodeScheme> sorted = Arrays.stream(values()).sorted((a, b) -> {
            return Integer.compare(a.getMaxBitRate(), b.getMaxBitRate());
        }).collect(Collectors.toList());
        for (TranscodeScheme transcodeScheme : sorted) {
            if (maxBitRate <= transcodeScheme.getMaxBitRate()) {
                return transcodeScheme;
            }
        }
        return null;
    }
}
