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

package org.airsonic.player.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.ExecutionException;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.util.HomeRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AirsonicRestApiIntTest {

    private static final String CLIENT_NAME = "jpsonic";
    private static final String AIRSONIC_USER = "admin";
    private static final String AIRSONIC_PASSWORD = "admin";
    private static final String EXPECTED_FORMAT = "json";

    private static String apiVerion;

    @Autowired
    private MockMvc mvc;

    @ClassRule
    public static final HomeRule CLASS_RULE = new HomeRule(); // sets jpsonic.home to a temporary dir

    @BeforeClass
    public static void setupClass() {
        apiVerion = TestCaseUtils.restApiVersion();
    }

    @Test
    public void pingTest() throws ExecutionException {
        try {
            mvc.perform(get("/rest/ping").param("v", apiVerion).param("c", CLIENT_NAME).param("u", AIRSONIC_USER)
                    .param("p", AIRSONIC_PASSWORD).param("f", EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                    .andExpect(jsonPath("$.subsonic-response.version").value(apiVerion)).andDo(print());
        } catch (Exception e) {
            Assert.fail();
            throw new ExecutionException(e);
        }
    }
}
