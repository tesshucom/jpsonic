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

package com.tesshu.jpsonic.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.tesshu.jpsonic.util.HttpRange;
import org.junit.jupiter.api.Test;

/**
 * @author Sindre Mehus
 */
class RangeOutputStreamTest {

    @Test
    void testWrap() throws IOException {
        assertTrue(doTestWrap(0, 99, 100, 1));
        assertTrue(doTestWrap(0, 99, 100, 10));
        assertTrue(doTestWrap(0, 99, 100, 13));
        assertTrue(doTestWrap(0, 99, 100, 70));
        assertTrue(doTestWrap(0, 99, 100, 100));

        assertTrue(doTestWrap(10, 99, 100, 1));
        assertTrue(doTestWrap(10, 99, 100, 10));
        assertTrue(doTestWrap(10, 99, 100, 13));
        assertTrue(doTestWrap(10, 99, 100, 70));
        assertTrue(doTestWrap(10, 99, 100, 100));

        assertTrue(doTestWrap(66, 66, 100, 1));
        assertTrue(doTestWrap(66, 66, 100, 2));

        assertTrue(doTestWrap(10, 20, 100, 1));
        assertTrue(doTestWrap(10, 20, 100, 10));
        assertTrue(doTestWrap(10, 20, 100, 13));
        assertTrue(doTestWrap(10, 20, 100, 70));
        assertTrue(doTestWrap(10, 20, 100, 100));

        for (int start = 0; start < 10; start++) {
            for (int end = start; end < 10; end++) {
                for (int bufferSize = 1; bufferSize < 10; bufferSize++) {
                    assertTrue(doTestWrap(start, end, 10, bufferSize));
                    assertTrue(doTestWrap(start, null, 10, bufferSize));
                }
            }
        }
    }

    private boolean doTestWrap(int first, Integer last, int sourceSize, int bufferSize) throws IOException {
        byte[] source = createSource(sourceSize);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStream rangeOut = RangeOutputStream.wrap(out,
                new HttpRange(first, last == null ? null : last.longValue()))) {
            copy(source, rangeOut, bufferSize);
        }
        verify(out.toByteArray(), first, last, sourceSize);
        return true;
    }

    private void verify(byte[] bytes, int first, Integer last, int sourceSize) {
        if (last == null) {
            assertEquals(sourceSize - first, bytes.length);
        } else {
            assertEquals(last - first + 1, bytes.length);
        }
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(first + i, bytes[i]);
        }
    }

    private void copy(byte[] source, OutputStream out, int bufsz) throws IOException {
        InputStream in = new ByteArrayInputStream(source);
        byte[] buffer = new byte[bufsz];
        int n = in.read(buffer);
        while (-1 != n) {
            int split = n / 2;
            out.write(buffer, 0, split);
            out.write(buffer, split, n - split);
            n = in.read(buffer);
        }
    }

    private byte[] createSource(int size) {
        byte[] result = new byte[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) i;
        }
        return result;
    }
}
