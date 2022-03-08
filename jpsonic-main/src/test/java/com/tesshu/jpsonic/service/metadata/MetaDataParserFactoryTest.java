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

package com.tesshu.jpsonic.service.metadata;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@SpringBootConfiguration
@ComponentScan(basePackages = "com.tesshu.jpsonic")
@ExtendWith(NeedsHome.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MetaDataParserFactoryTest {

    private static File someMp3;
    private static File someFlv;
    private static File someJunk;

    @Autowired
    private MetaDataParserFactory metaDataParserFactory;

    @Autowired
    private SettingsService settingsService;

    @BeforeAll
    public static void beforeAll() throws IOException {
        String homePath = System.getProperty("jpsonic.home");
        File home = new File(homePath);
        if (!home.exists()) {
            home.mkdir();
        }
        someMp3 = new File(homePath, "some.mp3");
        someFlv = new File(homePath, "some.flv");
        someJunk = new File(homePath, "some.junk");
        someMp3.createNewFile();
        someFlv.createNewFile();
        someJunk.createNewFile();
    }

    @Test
    void testorder() {
        MetaDataParser parser;

        settingsService.setVideoFileTypes("mp3 flv");

        parser = metaDataParserFactory.getParser(someMp3);
        assertThat(parser, instanceOf(MusicParser.class));

        parser = metaDataParserFactory.getParser(someFlv);
        assertThat(parser, instanceOf(VideoParser.class));

        parser = metaDataParserFactory.getParser(someJunk);
        assertThat(parser, instanceOf(DefaultParser.class));
    }
}
