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

package org.airsonic.player.service;

import java.io.File;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.util.HomeRule;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@SpringBootTest
public class LegacyDatabaseStartupTest {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyDatabaseStartupTest.class);

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule airsonicRule = new HomeRule() {
            @Override
            protected void before() throws Throwable {
                super.before();
                String homeParent = TestCaseUtils.jpsonicHomePathForTest();
                System.setProperty("jpsonic.home", TestCaseUtils.jpsonicHomePathForTest());
                TestCaseUtils.cleanJpsonicHomeForTest();
                File dbDirectory = new File(homeParent, "/db");
                FileUtils.forceMkdir(dbDirectory);
                org.airsonic.player.util.FileUtils
                        .copyResourcesRecursively(getClass().getResource("/db/pre-liquibase/db"), new File(homeParent));
            }
        };

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return airsonicRule.apply(spring, description);
        }
    };

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Test
    public void testStartup() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Successful startup");
        }
    }

}
