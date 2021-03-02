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

package org.airsonic.player.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.airsonic.player.domain.InternetRadio;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unit test of {@link InternetRadioDao}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class InternetRadioDaoTest extends DaoTestBase {

    @Autowired
    private InternetRadioDao internetRadioDao;

    @Before
    public void setUp() {
        getJdbcTemplate().execute("delete from internet_radio");
    }

    @Test
    public void testCreateInternetRadio() {
        InternetRadio radio = new InternetRadio("name", "streamUrl", "homePageUrl", true, new Date());
        internetRadioDao.createInternetRadio(radio);

        InternetRadio newRadio = internetRadioDao.getAllInternetRadios().get(0);
        assertInternetRadioEquals(radio, newRadio);
    }

    @Test
    public void testUpdateInternetRadio() {
        InternetRadio radio = new InternetRadio("name", "streamUrl", "homePageUrl", true, new Date());
        internetRadioDao.createInternetRadio(radio);
        radio = internetRadioDao.getAllInternetRadios().get(0);

        radio.setName("newName");
        radio.setStreamUrl("newStreamUrl");
        radio.setHomepageUrl("newHomePageUrl");
        radio.setEnabled(false);
        radio.setChanged(new Date(234_234L));
        internetRadioDao.updateInternetRadio(radio);

        InternetRadio newRadio = internetRadioDao.getAllInternetRadios().get(0);
        assertInternetRadioEquals(radio, newRadio);
    }

    @Test
    public void testDeleteInternetRadio() {
        assertEquals("Wrong number of radios.", 0, internetRadioDao.getAllInternetRadios().size());

        internetRadioDao.createInternetRadio(new InternetRadio("name", "streamUrl", "homePageUrl", true, new Date()));
        assertEquals("Wrong number of radios.", 1, internetRadioDao.getAllInternetRadios().size());

        internetRadioDao.createInternetRadio(new InternetRadio("name", "streamUrl", "homePageUrl", true, new Date()));
        assertEquals("Wrong number of radios.", 2, internetRadioDao.getAllInternetRadios().size());

        internetRadioDao.deleteInternetRadio(internetRadioDao.getAllInternetRadios().get(0).getId());
        assertEquals("Wrong number of radios.", 1, internetRadioDao.getAllInternetRadios().size());

        internetRadioDao.deleteInternetRadio(internetRadioDao.getAllInternetRadios().get(0).getId());
        assertEquals("Wrong number of radios.", 0, internetRadioDao.getAllInternetRadios().size());
    }

    private void assertInternetRadioEquals(InternetRadio expected, InternetRadio actual) {
        assertEquals("Wrong name.", expected.getName(), actual.getName());
        assertEquals("Wrong stream url.", expected.getStreamUrl(), actual.getStreamUrl());
        assertEquals("Wrong home page url.", expected.getHomepageUrl(), actual.getHomepageUrl());
        assertEquals("Wrong enabled state.", expected.isEnabled(), actual.isEnabled());
        assertEquals("Wrong changed date.", expected.getChanged(), actual.getChanged());
    }

}
