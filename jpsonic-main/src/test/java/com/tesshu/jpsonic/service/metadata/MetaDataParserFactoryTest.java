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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.tesshu.jpsonic.infrastructure.NeedsHome;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.FileUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@NeedsHome
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MetaDataParserFactoryTest {

    private static Path someMp3;
    private static Path someFlv;
    private static Path someJunk;

    @Autowired
    private MetaDataParserFactory metaDataParserFactory;

    @Autowired
    private SettingsService settingsService;

    @BeforeAll
    public static void beforeAll() throws IOException {
        Path homePath = Path.of(System.getProperty("jpsonic.home"));
        FileUtil.createDirectories(homePath);
        someMp3 = Path.of(homePath.toString(), "some.mp3");
        someFlv = Path.of(homePath.toString(), "some.flv");
        someJunk = Path.of(homePath.toString(), "some.junk");
        Files.createFile(someMp3);
        Files.createFile(someFlv);
        Files.createFile(someJunk);
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
