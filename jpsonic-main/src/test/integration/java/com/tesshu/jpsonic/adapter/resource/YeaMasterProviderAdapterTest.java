package com.tesshu.jpsonic.adapter.resource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.provider.master.YearInterval;
import com.tesshu.jpsonic.domain.provider.master.YearMasterCriteria;
import com.tesshu.jpsonic.domain.provider.master.YearMasterProvider;
import com.tesshu.jpsonic.domain.provider.master.YearMasterCriteria.YearMasterScope;
import com.tesshu.jpsonic.domain.provider.master.YearMasterCriteria.YearMasterSort;
import com.tesshu.jpsonic.domain.provider.master.YearMasterProvider.YearsEntry;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.persistence.NeedsDB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@NeedsDB
public class YeaMasterProviderAdapterTest extends AbstractNeedsScan {

    @Autowired
    private MusicFolderProvider musicFolderProvider;
    @Autowired
    private YearMasterProvider yeaMasterProvider;

    @BeforeEach
    void setup() {
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testFindYearsEntries() {
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        YearInterval interval = YearInterval.TEN_YEARS;
        YearMasterScope scope = YearMasterScope.SONG;
        YearMasterSort sort = YearMasterSort.ASC;
        YearMasterCriteria criteria = new YearMasterCriteria(folders, interval, scope, sort,
                MediaFile.Type.MUSIC);

        List<YearsEntry> entries = yeaMasterProvider
            .findYearsEntries(criteria, 0, Integer.MAX_VALUE);
        assertEquals(3, entries.size());

        assertEquals(YearInterval.TEN_YEARS, entries.get(0).yearGroup().interval());
        assertEquals(1990, entries.get(0).yearGroup().value());
        assertEquals(3, entries.get(0).count());

        assertEquals(YearInterval.TEN_YEARS, entries.get(1).yearGroup().interval());
        assertEquals(2000, entries.get(1).yearGroup().value());
        assertEquals(4, entries.get(1).count());

        assertEquals(YearInterval.UNDEFINED, entries.get(2).yearGroup().interval());
        assertEquals(4, entries.get(2).count());

        
        criteria = new YearMasterCriteria(folders, interval, YearMasterScope.ALBUM, sort,
                MediaFile.Type.MUSIC);
        entries = yeaMasterProvider
            .findYearsEntries(criteria, 0, Integer.MAX_VALUE);
        assertEquals(3, entries.size());

        assertEquals(YearInterval.TEN_YEARS, entries.get(0).yearGroup().interval());
        assertEquals(1990, entries.get(0).yearGroup().value());
        assertEquals(1, entries.get(0).count());

        assertEquals(YearInterval.TEN_YEARS, entries.get(1).yearGroup().interval());
        assertEquals(2000, entries.get(1).yearGroup().value());
        assertEquals(2, entries.get(1).count());

        assertEquals(YearInterval.UNDEFINED, entries.get(2).yearGroup().interval());
        assertEquals(2, entries.get(2).count());
    }

    @Test
    public void testCountYearsEntries() {
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        YearInterval interval = YearInterval.TEN_YEARS;
        YearMasterScope scope = YearMasterScope.SONG;
        YearMasterSort sort = YearMasterSort.ASC;
        YearMasterCriteria criteria = new YearMasterCriteria(folders, interval, scope, sort,
                MediaFile.Type.MUSIC);

        int count = yeaMasterProvider.countYearsEntries(criteria);
        assertEquals(3, count);

        criteria = new YearMasterCriteria(folders, interval, YearMasterScope.ALBUM, sort,
                MediaFile.Type.MUSIC);

        count = yeaMasterProvider.countYearsEntries(criteria);
        assertEquals(3, count);
    }

    @Test
    public void testCountChildren() {
        fail("Not yet implemented");
    }

}
