package org.airsonic.player.service.search;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.HomeRule;
import org.airsonic.player.util.MusicFolderTestData;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.springframework.util.ObjectUtils.isEmpty;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
/*
 * Abstract class for scanning MusicFolder.
 */
public abstract class AbstractAirsonicHomeTest implements AirsonicHomeTest {

    public interface BeforeScan extends Supplier<Boolean> {
    }

    public interface AfterScan extends Supplier<Boolean> {
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAirsonicHomeTest.class);

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule homeRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return homeRule.apply(spring, description);
        }
    };

    /*
     * Currently, Maven is executing test classes in series,
     * so this class can hold the state.
     * When executing in parallel, subclasses should override this.
     */
    private static AtomicBoolean dataBasePopulated = new AtomicBoolean();

    // Above.
    private static AtomicBoolean dataBaseReady = new AtomicBoolean();

    protected final static Function<String, String> resolveBaseMediaPath = (childPath) -> {
        return MusicFolderTestData.resolveBaseMediaPath().concat(childPath);
    };

    @Autowired
    protected DaoHelper daoHelper;

    @Autowired
    protected MediaScannerService mediaScannerService;

    @Autowired
    protected MusicFolderDao musicFolderDao;

    @Autowired
    protected SettingsService settingsService;

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    public AtomicBoolean dataBasePopulated() {
        return dataBasePopulated;
    }

    @Override
    public AtomicBoolean dataBaseReady() {
        return dataBaseReady;
    }

    @Override
    public final void populateDatabaseOnlyOnce() {
        populateDatabaseOnlyOnce(null);
    }

    public final void populateDatabaseOnlyOnce(BeforeScan beforeScan) {
        populateDatabaseOnlyOnce(beforeScan, null);
    }

    public final void populateDatabaseOnlyOnce(BeforeScan beforeScan, AfterScan afterscan) {
        if (!dataBasePopulated().get()) {
            dataBasePopulated().set(true);
            getMusicFolders().forEach(musicFolderDao::createMusicFolder);
            settingsService.clearMusicFolderCache();
            try {
                // Await time to avoid scan failure.
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
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

            if (LOG.isDebugEnabled()) {
                LOG.debug("--- Report of records count per table ---");
            }
            Map<String, Integer> records = TestCaseUtils.recordsInAllTables(daoHelper);
            records.keySet().stream().filter(s ->
                    s.equals("MEDIA_FILE")
                    | s.equals("ARTIST")
                    | s.equals("MUSIC_FOLDER")
                    | s.equals("ALBUM")
                    | s.equals("GENRE"))
                    .forEach(tableName -> {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("\t" + tableName + " : " + records.get(tableName).toString());
                        }
                    });
            if (LOG.isDebugEnabled()) {
                LOG.debug("--- *********************** ---");
            }
            try {
                // Await for Lucene to finish writing(asynchronous).
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dataBaseReady().set(true);
        } else {
            while (!dataBaseReady().get()) {
                try {
                    // The subsequent test method waits while reading DB data.
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void setSortAlphanum(boolean isSortStrict) {
        settingsService.setSortAlphanum(true);
    }

    protected void setSortStrict(boolean isSortStrict) {
        settingsService.setSortStrict(isSortStrict);
    }

}