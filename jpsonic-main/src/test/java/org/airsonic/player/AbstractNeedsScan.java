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

package org.airsonic.player;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.search.AirsonicHomeTest;
import org.airsonic.player.util.MusicFolderTestDataUtils;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
// TODO Separate classes that require DirtiesContext from those that don't
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
/*
 * Abstract class for scanning MusicFolder.
 */
public abstract class AbstractNeedsScan implements AirsonicHomeTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNeedsScan.class);

    /*
     * Currently, Maven is executing test classes in series, so this class can hold the state. When executing in
     * parallel, subclasses should override this.
     */
    private static final AtomicBoolean DATA_BASE_POPULATED = new AtomicBoolean();

    // Above.
    private static final AtomicBoolean DATABASE_READY = new AtomicBoolean();

    @Autowired
    protected DaoHelper daoHelper;

    @Autowired
    protected MediaScannerService mediaScannerService;

    @Autowired
    protected MusicFolderDao musicFolderDao;

    @Autowired
    protected SettingsService settingsService;

    @BeforeAll
    public static void beforeAll() throws IOException {
        System.setProperty("jpsonic.home", TestCaseUtils.jpsonicHomePathForTest());
        TestCaseUtils.cleanJpsonicHomeForTest();
    }

    public interface BeforeScan extends Supplier<Boolean> {
    }

    public interface AfterScan extends Supplier<Boolean> {
    }

    protected static final String resolveBaseMediaPath(String childPath) {
        return MusicFolderTestDataUtils.resolveBaseMediaPath().concat(childPath);
    }

    @Override
    public boolean isDataBasePopulated() {
        return DATA_BASE_POPULATED.get();
    }

    @Override
    public boolean isDataBaseReady() {
        return DATABASE_READY.get();
    }

    @Override
    public final void populateDatabaseOnlyOnce() {
        populateDatabaseOnlyOnce(null);
    }

    public final void populateDatabaseOnlyOnce(BeforeScan beforeScan) {
        populateDatabaseOnlyOnce(beforeScan, null);
    }

    public final void populateDatabaseOnlyOnce(BeforeScan beforeScan, AfterScan afterscan) {
        if (isDataBasePopulated()) {
            while (!isDataBaseReady()) {
                try {
                    // The subsequent test method waits while reading DB data.
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    LOG.error("Database initialization was interrupted unexpectedly.", e);
                }
            }
        } else {
            DATA_BASE_POPULATED.set(true);
            getMusicFolders().forEach(musicFolderDao::createMusicFolder);
            settingsService.clearMusicFolderCache();
            try {
                // Await time to avoid scan failure.
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                LOG.error("Database initialization was interrupted unexpectedly.", e);
            }

            if (!isEmpty(beforeScan)) {
                if (beforeScan.get()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Pre-processing of scan was called.");
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Pre-scan processing may have a problem with the call.");
                    }
                }
            }

            TestCaseUtils.execScan(mediaScannerService);

            supplyIfNotEmpty(afterscan);

            logRecordsPerTables();

            try {
                // Await for Lucene to finish writing(asynchronous).
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                LOG.error("Database initialization was interrupted unexpectedly.", e);
            }
            DATABASE_READY.set(true);
        }
    }

    private void supplyIfNotEmpty(AfterScan afterscan) {
        if (!isEmpty(afterscan)) {
            if (afterscan.get()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Post-processing of scan was called.");
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Post-scan processing may have a problem with the call.");
                }
            }
        }
    }

    private void logRecordsPerTables() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("--- Report of records count per table ---");
        }
        Map<String, Integer> records = TestCaseUtils.recordsInAllTables(daoHelper);
        records.keySet().stream().filter(s -> "MEDIA_FILE".equals(s) | "ARTIST".equals(s) | "MUSIC_FOLDER".equals(s)
                | "ALBUM".equals(s) | "GENRE".equals(s)).forEach(tableName -> {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("\t" + tableName + " : " + records.get(tableName).toString());
                    }
                });
        if (LOG.isDebugEnabled()) {
            LOG.debug("--- *********************** ---");
        }

    }

    protected void setSortAlphanum(boolean isSortStrict) {
        settingsService.setSortAlphanum(true);
    }

    protected void setSortStrict(boolean isSortStrict) {
        settingsService.setSortStrict(isSortStrict);
    }

}
