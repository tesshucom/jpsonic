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

package org.airsonic.player.domain;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit test of {@link CacheElement}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
public class CacheElementTest {

    @Test
    public void testCreateId() {

        assertTrue(CacheElement.createId(1, "/Volumes/WD Passport/music/'Til Tuesday/Welcome Home") == CacheElement
                .createId(1, "/Volumes/WD Passport/music/'Til Tuesday/Welcome Home"));

        assertTrue(CacheElement.createId(1, "/Volumes/WD Passport/music/'Til Tuesday/Welcome Home") != CacheElement
                .createId(2, "/Volumes/WD Passport/music/'Til Tuesday/Welcome Home"));

        assertTrue(
                CacheElement.createId(237462763, "/Volumes/WD Passport/music/'Til Tuesday/Welcome Home") != CacheElement
                        .createId(28374922, "/Volumes/WD Passport/music/'Til Tuesday/Welcome Home"));

        assertTrue(
                CacheElement.createId(1, "/Volumes/WD Passport/music/'Til Tuesday/Welcome Home bla bla") != CacheElement
                        .createId(1, "/Volumes/WD Passport/music/'Til Tuesday/Welcome Home"));
    }
}
