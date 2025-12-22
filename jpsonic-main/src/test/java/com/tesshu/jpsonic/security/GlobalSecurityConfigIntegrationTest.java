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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

/*
 * Integration test using RestTemplate to verify behavior with context-path.
 * Due to Spring constraints, POST /logout test is not included here.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "server.servlet.context-path=/jpsonic" })
@ExtendWith(NeedsHome.class)
class GlobalSecurityConfigIntegrationTest {

    @LocalServerPort
    int port;

    RestTemplate restTemplate = new RestTemplate();

    @Test
    void logoutShouldBeHandledEvenWithContextPath() {
        String url = "http://localhost:" + port + "/jpsonic/logout";
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
        /*
         * If normal, it should result in a redirect (3xx) to /jpsonic/login?logout or
         * something similar.
         */
        assertThat(resp.getStatusCode().is3xxRedirection()).isFalse();
        assertThat(resp.getStatusCode().value()).isNotEqualTo(404);
    }
}