
package org.airsonic.player.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public class InternalHelpControllerIntTest extends AbstractAirsonicHomeTest {

    @Autowired
    private MockMvc mvc;

    @Before
    public void setup() throws Exception {
        populateDatabaseOnlyOnce();
    }

    @Test
    @WithMockUser(username = "admin", roles = { "USER", "ADMIN" })
    public void testOkForAdmins() throws Exception {
        mvc.perform(get("/internalhelp").contentType(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    public void testNotOkForUsers() throws Exception {
        mvc.perform(get("/internalhelp").contentType(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
    }
}
