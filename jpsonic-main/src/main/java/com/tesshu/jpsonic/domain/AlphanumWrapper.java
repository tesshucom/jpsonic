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
 Based upon Alphanum Algorithm, Copyright 2007-2017 David Koelle
 */
package com.tesshu.jpsonic.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.text.CollationKey;
import java.text.Collator;

/**
 * Class to perform Collaror sort considering serial number.
 */
class AlphanumWrapper extends Collator {

    private final Collator deligate;

    AlphanumWrapper(Collator collator) {
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
            int result;
            if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
                int thisChunkLength = thisChunk.length();
                result = thisChunkLength - thatChunk.length();
                if (result == 0) {
                    for (int i = 0; i < thisChunkLength; i++) {
                        result = thisChunk.charAt(i) - thatChunk.charAt(i);
                        if (result != 0) {
                            return result;
                        }
                    }
                }
            } else {
                result = null == deligate ? thisChunk.compareToIgnoreCase(thatChunk)
                        : deligate.compare(thisChunk, thatChunk);
            }
            if (result != 0)
                return result;
        }
        return s1Length - s2Length;
    }

    private final String getChunk(String s, int slength, int marker) {
        int cursol = marker;
        StringBuilder chunk = new StringBuilder();
        char c = s.charAt(cursol);
        chunk.append(c);
        cursol++;
        if (isDigit(c)) {
            while (cursol < slength) {
                c = s.charAt(cursol);
                if (!isDigit(c))
                    break;
                chunk.append(c);
                cursol++;
            }
        } else {
            while (cursol < slength) {
                c = s.charAt(cursol);
                if (isDigit(c))
                    break;
                chunk.append(c);
                cursol++;
            }
        }
        return chunk.toString();
    }

    private final boolean isDigit(char ch) {
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AlphanumWrapper)) {
            return false;
        }
        AlphanumWrapper that = (AlphanumWrapper) o;
        return new EqualsBuilder().appendSuper(super.equals(that)).append(that, that.deligate).isEquals();
    }

}