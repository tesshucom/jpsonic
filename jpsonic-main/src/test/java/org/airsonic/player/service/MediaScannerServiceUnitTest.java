
package org.airsonic.player.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.airsonic.player.domain.MediaLibraryStatistics;
import org.airsonic.player.service.search.IndexManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MediaScannerServiceUnitTest {

    @InjectMocks
    MediaScannerService mediaScannerService;

    @Mock
    IndexManager indexManager;

    @Test
    public void neverScanned() {
        when(indexManager.getStatistics()).thenReturn(null);
        assertTrue(mediaScannerService.neverScanned());

        when(indexManager.getStatistics()).thenReturn(new MediaLibraryStatistics());
        assertFalse(mediaScannerService.neverScanned());
    }
}
