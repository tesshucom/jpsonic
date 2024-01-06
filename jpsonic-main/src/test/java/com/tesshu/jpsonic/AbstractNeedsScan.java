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

package com.tesshu.jpsonic;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.dao.base.DaoHelper;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.scanner.ExpungeService;
import com.tesshu.jpsonic.service.scanner.MediaScannerServiceImpl;
import com.tesshu.jpsonic.service.scanner.MusicFolderServiceImpl;
import com.tesshu.jpsonic.service.scanner.ScannerProcedureService;
import com.tesshu.jpsonic.service.scanner.ScannerStateServiceImpl;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;

/*
 * Abstract class for scanning MusicFolder.
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractNeedsScan implements NeedsScan {

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
    protected MusicFolderDao musicFolderDao;
    @Autowired
    protected SettingsService settingsService;
    @Autowired
    protected MusicFolderServiceImpl musicFolderService;
    @Autowired
    protected SecurityService securityService;
    @Autowired
    private ScannerStateServiceImpl scannerStateService;
    @Autowired
    private ScannerProcedureService procedure;
    @Autowired
    private ExpungeService expungeService;
    @Autowired
    private StaticsDao staticsDao;

    private final ThreadPoolTaskExecutor scanExecutor = ServiceMockUtils.mockNoAsyncTaskExecutor();

    protected MediaScannerService mediaScannerService;

    @PostConstruct
    public void init() {
        mediaScannerService = new MediaScannerServiceImpl(settingsService, scannerStateService, procedure,
                expungeService, staticsDao, scanExecutor);
    }

    public interface BeforeScan extends Supplier<Boolean> {
    }

    public interface AfterScan extends Supplier<Boolean> {
    }

    protected static final String resolveBaseMediaPath(String childPath) {
        return Path.of(MusicFolderTestDataUtils.resolveBaseMediaPath().concat(childPath)).toString();
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
    public final void populateDatabase() {
        DATA_BASE_POPULATED.set(false);
        DATABASE_READY.set(false);
        populateDatabaseOnlyOnce(null);
    }

    public final void populateDatabase(BeforeScan beforeScan, AfterScan afterscan) {
        DATA_BASE_POPULATED.set(false);
        DATABASE_READY.set(false);
        populateDatabaseOnlyOnce(beforeScan, afterscan);
    }

    @Override
    public final void populateDatabaseOnlyOnce() {
        populateDatabaseOnlyOnce(null);
    }

    public final void populateDatabaseOnlyOnce(BeforeScan beforeScan) {
        populateDatabaseOnlyOnce(beforeScan, null);
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
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
            musicFolderService.clearMusicFolderCache();
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
