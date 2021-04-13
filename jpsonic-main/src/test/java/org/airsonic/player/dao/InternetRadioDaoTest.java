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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.airsonic.player.NeedsHome;
import org.airsonic.player.domain.InternetRadio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit test of {@link InternetRadioDao}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class InternetRadioDaoTest {

    @Autowired
    private GenericDaoHelper daoHelper;

    @Autowired
    private InternetRadioDao internetRadioDao;

    @BeforeEach
    public void setUp() {
        daoHelper.getJdbcTemplate().execute("delete from internet_radio");
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
        assertEquals(0, internetRadioDao.getAllInternetRadios().size(), "Wrong number of radios.");
        internetRadioDao.createInternetRadio(new InternetRadio("name", "streamUrl", "homePageUrl", true, new Date()));
        assertEquals(1, internetRadioDao.getAllInternetRadios().size(), "Wrong number of radios.");
        internetRadioDao.createInternetRadio(new InternetRadio("name", "streamUrl", "homePageUrl", true, new Date()));
        assertEquals(2, internetRadioDao.getAllInternetRadios().size(), "Wrong number of radios.");
        internetRadioDao.deleteInternetRadio(internetRadioDao.getAllInternetRadios().get(0).getId());
        assertEquals(1, internetRadioDao.getAllInternetRadios().size(), "Wrong number of radios.");
        internetRadioDao.deleteInternetRadio(internetRadioDao.getAllInternetRadios().get(0).getId());
        assertEquals(0, internetRadioDao.getAllInternetRadios().size(), "Wrong number of radios.");
    }

    private void assertInternetRadioEquals(InternetRadio expected, InternetRadio actual) {
        assertEquals(expected.getName(), actual.getName(), "Wrong name.");
        assertEquals(expected.getStreamUrl(), actual.getStreamUrl(), "Wrong stream url.");
        assertEquals(expected.getHomepageUrl(), actual.getHomepageUrl(), "Wrong home page url.");
        assertEquals(expected.isEnabled(), actual.isEnabled(), "Wrong enabled state.");
        assertEquals(expected.getChanged(), actual.getChanged(), "Wrong changed date.");
    }
}
