
package org.airsonic.player;

import java.io.IOException;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeedsHome implements BeforeAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(NeedsHome.class);

    @Override
    public void beforeAll(ExtensionContext context) throws InterruptedException {
        System.setProperty("jpsonic.home", TestCaseUtils.jpsonicHomePathForTest());
        try {
            /*
             * Atomic is not guaranteed for file operations. Especially on Windows, you may have problems with case of
             * Junit continuous exec. This is a technical topic when doing integration testing with Junit, and it's a
             * separate topic from the integrity of artifact.
             */
            TestCaseUtils.cleanJpsonicHomeForTest();
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("The behavior of this callback process depends on the platform.", e);
            }
        }
    }
}
