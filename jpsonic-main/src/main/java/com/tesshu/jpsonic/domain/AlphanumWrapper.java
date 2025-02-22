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

package com.tesshu.jpsonic.domain;

import java.text.CollationKey;
import java.text.Collator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Class to perform Collaror sort considering serial number.
 */
class AlphanumWrapper extends Collator {

    private final Collator deligate;

    AlphanumWrapper(Collator collator) {
        super();
        this.deligate = collator;
    }

    @Override
    public int compare(String s1, String s2) {

        int thisMarker = 0;
        int thatMarker = 0;
        int s1Length = s1.length();
        int s2Length = s2.length();

        while (thisMarker < s1Length && thatMarker < s2Length) {
            String thisChunk = getChunk(s1, s1Length, thisMarker);
            thisMarker += thisChunk.length();
            String thatChunk = getChunk(s2, s2Length, thatMarker);
            thatMarker += thatChunk.length();
            int result = isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))
                    ? compareDigit(thisChunk, thatChunk)
                    : deligate.compare(thisChunk, thatChunk);
            if (result != 0) {
                return result;
            }
        }
        return s1Length - s2Length;
    }

    private int compareDigit(String thisChunk, String thatChunk) {
        int result;
        int thisChunkLength = thisChunk.length();
        result = thisChunkLength - thatChunk.length();
        if (result != 0) {
            return result;
        }
        for (int i = 0; i < thisChunkLength; i++) {
            result = thisChunk.charAt(i) - thatChunk.charAt(i);
            if (result != 0) {
                return result;
            }
        }
        return result;
    }

    private String getChunk(String s, int slength, int marker) {
        int cursol = marker;
        StringBuilder chunk = new StringBuilder();
        char c = s.charAt(cursol);
        chunk.append(c);
        cursol++;
        if (isDigit(c)) {
            while (cursol < slength) {
                c = s.charAt(cursol);
                if (!isDigit(c)) {
                    break;
                }
                chunk.append(c);
                cursol++;
            }
        } else {
            while (cursol < slength) {
                c = s.charAt(cursol);
                if (isDigit(c)) {
                    break;
                }
                chunk.append(c);
                cursol++;
            }
        }
        return chunk.toString();
    }

    private boolean isDigit(char ch) {
        return ch >= 48 && ch <= 57;
    }

    @Override
    public CollationKey getCollationKey(String source) {
        return deligate.getCollationKey(source);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(deligate).toHashCode();
    }

    @Override
    @SuppressWarnings("PMD.SimplifyBooleanReturns")
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AlphanumWrapper that)) {
            return false;
        }
        return new EqualsBuilder().appendSuper(super.equals(that)).append(that, that.deligate).isEquals();
    }

}
