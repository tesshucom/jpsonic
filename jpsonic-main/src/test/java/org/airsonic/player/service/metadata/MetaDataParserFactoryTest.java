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

package org.airsonic.player.service.metadata;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.service.SettingsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MetaDataParserFactoryTest {

    private static File someMp3;
    private static File someFlv;
    private static File someJunk;

    @Autowired
    private MetaDataParserFactory metaDataParserFactory;

    @Autowired
    private SettingsService settingsService;

    @BeforeAll
    public static void beforeAll() throws IOException {
        String homePath = TestCaseUtils.jpsonicHomePathForTest();
        System.setProperty("jpsonic.home", homePath);
        someMp3 = new File(homePath, "some.mp3");
        someFlv = new File(homePath, "some.flv");
        someJunk = new File(homePath, "some.junk");
        someMp3.createNewFile();
        someFlv.createNewFile();
        someJunk.createNewFile();
    }

    @Test
    public void testorder() {
        MetaDataParser parser;

        settingsService.setVideoFileTypes("mp3 flv");

        parser = metaDataParserFactory.getParser(someMp3);
        assertThat(parser, instanceOf(JaudiotaggerParser.class));

        parser = metaDataParserFactory.getParser(someFlv);
        assertThat(parser, instanceOf(FFmpegParser.class));

        parser = metaDataParserFactory.getParser(someJunk);
        assertThat(parser, instanceOf(DefaultMetaDataParser.class));
    }
}
