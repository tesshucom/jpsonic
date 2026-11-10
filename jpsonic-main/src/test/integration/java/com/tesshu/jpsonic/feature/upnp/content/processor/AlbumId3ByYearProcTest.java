package com.tesshu.jpsonic.feature.upnp.content.processor;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.model.summary.YearAlbumEntry;
import com.tesshu.jpsonic.domain.provider.master.YearInterval;
import com.tesshu.jpsonic.domain.provider.master.YearsEntry;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicAlbum;
import org.jupnp.support.model.container.PlaylistContainer;
import org.jupnp.support.model.item.Item;
import org.jupnp.support.model.item.MusicTrack;
import org.springframework.beans.factory.annotation.Autowired;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AlbumId3ByYearProcTest extends AbstractNeedsScan {

    @Autowired
    private AlbumId3ByYearProc proc;

    @BeforeEach
    void setup() {
        populateDatabaseOnlyOnce();
        settingsFacade.commit(UPnPSKeys.basic.baseLanUrl, "https://192.168.1.1:4040");
    }

    @Test
    @Order(1)
    public void testGetProcId() {
        assertEquals("aiby", proc.getProcId().getValue());
    }

    @Test
    @Order(2)
    public void testCreateContainer() {
        YearsEntry yearsEntry = proc.getDirectChildren(0, 1).get(0);

        Container container = proc.createContainer(yearsEntry);
        assertInstanceOf(PlaylistContainer.class, container);
        assertEquals("aiby/TEN_YEARS,1990", container.getId());
        assertEquals("aiby", container.getParentID());
        assertEquals("1990 - 1999", container.getTitle());
        assertEquals(1, container.getChildCount());
    }

    @Test
    @Order(3)
    public void testGetDirectChildren() {
        List<YearsEntry> yearsEntries =  proc.getDirectChildren(0, Integer.MAX_VALUE);

        assertEquals(3, yearsEntries.size());

        assertEquals(YearInterval.TEN_YEARS, yearsEntries.get(0).yearGroup().interval());
        assertEquals(1990, yearsEntries.get(0).yearGroup().value());
        assertEquals(1, yearsEntries.get(0).count());
        
        assertEquals(YearInterval.TEN_YEARS, yearsEntries.get(1).yearGroup().interval());
        assertEquals(2000, yearsEntries.get(1).yearGroup().value());
        assertEquals(2, yearsEntries.get(1).count());

        assertEquals(YearInterval.UNDEFINED, yearsEntries.get(2).yearGroup().interval());
        assertEquals(2, yearsEntries.get(2).count());
    }

    @Test
    @Order(4)
    public void testGetDirectChildrenCount() {
        assertEquals(3, proc.getDirectChildrenCount());
    }

    @Test
    @Order(5)
    public void testGetDirectChild() {
        YearsEntry yearsEntry =  proc.getDirectChild("TEN_YEARS,1990");
        assertEquals(YearInterval.TEN_YEARS, yearsEntry.yearGroup().interval());
        assertEquals(1990, yearsEntry.yearGroup().value());
        assertEquals(1, yearsEntry.count());
    }

    @Test
    @Order(6)
    public void testAddChild() {
        YearsEntry yearsEntry = proc.getDirectChild("TEN_YEARS,1990");
        YearAlbumEntry entry = new YearAlbumEntry(yearsEntry.yearGroup(), 99, "artist",
                "album title", "comment", 10);

        DIDLContent content = new DIDLContent();
        proc.addChild(content, entry);

        assertEquals(1, content.getCount());
        Container container = content.getFirstContainer();
        assertEquals(MusicAlbum.class, container.getClass());

        assertEquals("aiby/yae:99;TEN_YEARS,1990", container.getId());
        assertEquals("album title", container.getTitle());
        assertEquals(0, container.getItems().size());
        assertEquals(0, container.getResources().size());
        assertEquals(3, container.getProperties().size());

        assertTrue(container
            .getProperties()
            .get(0)
            .getValue()
            .toString()
            .startsWith("https://192.168.1.1:4040/ext/upnp/art/"));
        assertEquals("artist", container.getProperties().get(1).getValue().toString());
        assertEquals("comment", container.getProperties().get(2).getValue().toString());
    }

    @Test
    @Order(7)
    public void testGetChildSizeOf() {
        List<YearsEntry> yearsEntries =  proc.getDirectChildren(0, Integer.MAX_VALUE);

        assertEquals(1990, yearsEntries.get(0).yearGroup().value());
        assertEquals(1, yearsEntries.get(0).count());
        assertEquals(1, proc.getChildSizeOf(yearsEntries.get(0)));
        
        assertEquals(2000, yearsEntries.get(1).yearGroup().value());
        assertEquals(2, yearsEntries.get(1).count());
        assertEquals(2, proc.getChildSizeOf(yearsEntries.get(1)));
    }

    @Test
    @Order(8)
    public void testGetChildren() {
        List<YearsEntry> yearsEntries =  proc.getDirectChildren(0, Integer.MAX_VALUE);

        assertEquals(1990, yearsEntries.get(0).yearGroup().value());
        assertEquals(1, yearsEntries.get(0).count());
        assertEquals(1, proc.getChildSizeOf(yearsEntries.get(0)));
        List<YearAlbumEntry> yearAlbumEntry = proc.getChildren(yearsEntries.get(0), 0, Integer.MAX_VALUE);
        assertEquals(1, yearAlbumEntry.size());
        assertEquals(1990, yearAlbumEntry.get(0).yearGroup().value());
        assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", yearAlbumEntry.get(0).albumName());

        assertEquals(2000, yearsEntries.get(1).yearGroup().value());
        assertEquals(2, yearsEntries.get(1).count());
        assertEquals(2, proc.getChildSizeOf(yearsEntries.get(1)));
        yearAlbumEntry = proc.getChildren(yearsEntries.get(1), 0, Integer.MAX_VALUE);
        assertEquals(2, yearAlbumEntry.size());
        assertEquals(2000, yearAlbumEntry.get(0).yearGroup().value());
        assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]", yearAlbumEntry.get(0).albumName());
        assertEquals("_ID3_ALBUM_ Chrome Hoof", yearAlbumEntry.get(1).albumName());
    }

    @Nested
    class BrowseLeafTest {
        // (music:year)
        // 1996 _ID3_ALBUM_ Sackcloth 'n' Ashes
        // 1996 _ID3_ALBUM_ Sackcloth 'n' Ashes
        // 1996 _ID3_ALBUM_ Sackcloth 'n' Ashes
        // 2001 _ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]
        // 2001 _ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]
        // 2004 _ID3_ALBUM_ Chrome Hoof
        // 2005 _ID3_ALBUM_ Chrome Hoof
        @Test
        void testBrowseLeafWithYearsEntry1() throws Exception {
            List<YearsEntry> yearsEntries = proc.getDirectChildren(1, 1);
            assertEquals(2000, yearsEntries.get(0).yearGroup().value());

            BrowseResult browseResult = proc
                .browseLeaf(yearsEntries.get(0).yearGroup().id(), null, 0, 1);
            assertEquals(2, browseResult.getTotalMatchesLong());
            assertEquals(1, browseResult.getCount().getValue());

            browseResult = proc.browseLeaf(yearsEntries.get(0).yearGroup().id(), null, 0, 2);
            assertEquals(2, browseResult.getTotalMatchesLong());
            assertEquals(2, browseResult.getCount().getValue());

            browseResult = proc.browseLeaf(yearsEntries.get(0).yearGroup().id(), null, 0, Integer.MAX_VALUE);
            assertEquals(2, browseResult.getTotalMatchesLong());
            assertEquals(2, browseResult.getCount().getValue());
        }

        @Test
        void testBrowseLeafWithYearsEntry2() throws Exception {
            List<YearsEntry> yearsEntries = proc.getDirectChildren(0, 1);
            assertEquals(1990, yearsEntries.get(0).yearGroup().value());
            BrowseResult browseResult = proc
                .browseLeaf(yearsEntries.get(0).yearGroup().id(), null, 0, 1);
            assertEquals(1, browseResult.getCount().getValue());

            DIDLParser parser = new DIDLParser();
            DIDLContent content = parser.parse(browseResult.getResult());
            List<Container> containers = content.getContainers();
            assertEquals(1, containers.size());

            Container container = containers.get(0);
            assertEquals("aiby", container.getParentID());
            assertEquals("aiby/yae:3;TEN_YEARS,1990", container.getId());
            assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", container.getTitle());
            assertEquals(3, container.getChildCount());
        }

        @Test
        void testBrowseLeafWithCompositeId() throws Exception {
            List<YearsEntry> yearsEntries = proc.getDirectChildren(0, 1);
            assertEquals(1990, yearsEntries.get(0).yearGroup().value());
            BrowseResult browseResult = proc
                .browseLeaf(yearsEntries.get(0).yearGroup().id(), null, 0, 1);

            DIDLParser parser = new DIDLParser();
            DIDLContent content = parser.parse(browseResult.getResult());
            String containerId = content.getContainers().get(0).getId();
            String yearAlbumEntryId = containerId
                .substring(containerId.indexOf(ProcId.CID_SEPA) + 1);

            browseResult = proc.browseLeaf(yearAlbumEntryId, null, 0, 1);

            content = parser.parse(browseResult.getResult());
            List<Container> containers = content.getContainers();
            assertEquals(0, containers.size());
            List<Item> items = content.getItems();
            assertEquals(1, items.size());
            Item item = items.get(0);
            assertInstanceOf(MusicTrack.class, item);
            MusicTrack musicTrack = (MusicTrack) item;
            assertEquals(1, musicTrack.getResources().size());
            assertTrue(musicTrack
                .getResources()
                .get(0)
                .getValue()
                .startsWith("https://192.168.1.1:4040/ext/upnp/stream/"));
            assertEquals(1, content.getCount());

            browseResult = proc.browseLeaf(yearAlbumEntryId, null, 0, Integer.MAX_VALUE);
            content = parser.parse(browseResult.getResult());
            containers = content.getContainers();
            assertEquals(0, containers.size());
            items = content.getItems();
            assertEquals(3, items.size());
            assertEquals(3, content.getCount());
        }
    }
}
