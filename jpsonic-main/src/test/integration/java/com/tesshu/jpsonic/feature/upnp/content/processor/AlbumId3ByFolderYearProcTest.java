package com.tesshu.jpsonic.feature.upnp.content.processor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.model.summary.YearAlbumEntry;
import com.tesshu.jpsonic.domain.provider.master.YearInterval;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FYearOrFYAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFYear;
import com.tesshu.jpsonic.infrastructure.scanner.MusicFolderServiceImpl;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.MusicFolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.PlaylistContainer;
import org.jupnp.support.model.container.StorageFolder;
import org.jupnp.support.model.item.Item;
import org.springframework.beans.factory.annotation.Autowired;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AlbumId3ByFolderYearProcTest extends AbstractNeedsScan {
    
    @Autowired
    private AlbumId3ByFolderYearProc proc;
    @Autowired
    private MusicFolderService musicFolderService;
    @Autowired
    private MusicFolderProvider musicFolderProvider;
    @Autowired
    private SettingsFacade settingsFacade;

    @BeforeEach
    void setup() {
        populateDatabaseOnlyOnce();
        settingsFacade.commit(UPnPSKeys.basic.baseLanUrl, "https://192.168.1.1:4040");
    }

    @Test
    @Order(1)
    public void testGetProcId() {
        assertEquals("aibfy", proc.getProcId().getValue());
    }

    private List<Integer> setSingleFolder() {
        List<Integer> allIds = musicFolderService
            .getAllMusicFolders()
            .stream()
            .map(MusicFolder::getId)
            .toList();
        musicFolderService
            .getAllMusicFolders()
            .stream()
            .filter(f -> "Music".equals(f.getName()))
            .findFirst()
            .ifPresent(f -> {
                musicFolderService
                    .setMusicFoldersForUser(MusicFolderServiceImpl.USERNAME_GUEST,
                            List.of(f.getId()));
            });
        return allIds;
    }
    
    private void revertFolder(List<Integer> allIds) {
        musicFolderService.setMusicFoldersForUser(MusicFolderServiceImpl.USERNAME_GUEST, allIds);
    }

    @Test
    @Order(2)
    public void testCreateContainer() {
        FolderOrFYear folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertFalse(folderOrFYear.isFolderYear());

        Container container = proc.createContainer(folderOrFYear);
        assertInstanceOf(StorageFolder.class, container);
        assertEquals("aibfy/1", container.getId());
        assertEquals("aibfy", container.getParentID());
        assertEquals("Music", container.getTitle());
        assertEquals(3, container.getChildCount());

        // single folder
        List<Integer> allIds = setSingleFolder();
        assertEquals(1, musicFolderProvider.getGuestFolders().size());

        // single folder test
        folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertTrue(folderOrFYear.isFolderYear());

        container = proc.createContainer(folderOrFYear);
        assertInstanceOf(PlaylistContainer.class, container);
        assertEquals("aibfy/fy:1;TEN_YEARS,1990", container.getId());
        assertEquals("aibfy", container.getParentID());
        assertEquals("1990 - 1999", container.getTitle());
        assertEquals(3, container.getChildCount());

        // revert
        revertFolder(allIds);
        assertEquals(2, musicFolderProvider.getGuestFolders().size());
    }

    @Test
    @Order(3)
    public void testGetDirectChildren() {
        List<FolderOrFYear> folderOrFYear =  proc.getDirectChildren(0, Integer.MAX_VALUE);
        assertEquals(2, folderOrFYear.size());
        assertEquals("Music", folderOrFYear.get(0).getFolder().name());
        assertEquals("Music2", folderOrFYear.get(1).getFolder().name());
        
        // single folder
        List<Integer> allIds = setSingleFolder();
        assertEquals(1, musicFolderProvider.getGuestFolders().size());

        // single folder test
        folderOrFYear = proc.getDirectChildren(0, Integer.MAX_VALUE);
        assertEquals(3, folderOrFYear.size());
        assertEquals("1990 - 1999", folderOrFYear.get(0).getFolderYear().year().viewName());
        assertEquals("2000 - 2009", folderOrFYear.get(1).getFolderYear().year().viewName());
        assertEquals("Undefined", folderOrFYear.get(2).getFolderYear().year().viewName());

        // revert
        revertFolder(allIds);
        assertEquals(2, musicFolderProvider.getGuestFolders().size());
    }

    @Test
    @Order(4)
    public void testGetDirectChildrenCount() {
        assertEquals(2, proc.getDirectChildrenCount());

        // single folder
        List<Integer> allIds = setSingleFolder();
        assertEquals(1, musicFolderProvider.getGuestFolders().size());

        // single folder test
        assertEquals(3, proc.getDirectChildrenCount());

        // revert
        revertFolder(allIds);
        assertEquals(2, musicFolderProvider.getGuestFolders().size());
    }

    @Test
    @Order(5)
    public void testGetDirectChild() {
        FolderOrFYear folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertFalse(folderOrFYear.isFolderYear());
        Container container = proc.createContainer(folderOrFYear);
        assertInstanceOf(StorageFolder.class, container);

        String childId = container
            .getId()
            .substring(container.getId().indexOf(ProcId.CID_SEPA) + 1);
        folderOrFYear = proc.getDirectChild(childId);
        assertFalse(folderOrFYear.isFolderYear());
        assertEquals("Music", folderOrFYear.getFolder().name());

        // single folder
        List<Integer> allIds = setSingleFolder();
        assertEquals(1, musicFolderProvider.getGuestFolders().size());

        // single folder test
        folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertTrue(folderOrFYear.isFolderYear());
        container = proc.createContainer(folderOrFYear);
        assertInstanceOf(PlaylistContainer.class, container);

        childId = container.getId().substring(container.getId().indexOf(ProcId.CID_SEPA) + 1);
        folderOrFYear = proc.getDirectChild(childId);
        assertTrue(folderOrFYear.isFolderYear());
        assertEquals("1990 - 1999", folderOrFYear.getFolderYear().year().viewName());

        // revert
        revertFolder(allIds);
        assertEquals(2, musicFolderProvider.getGuestFolders().size());
    }

    @Test
    @Order(6)
    public void testGetChildSizeOf() {
        FolderOrFYear folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertFalse(folderOrFYear.isFolderYear());
        Container container = proc.createContainer(folderOrFYear);
        assertInstanceOf(StorageFolder.class, container);

        assertFalse(folderOrFYear.isFolderYear());
        assertEquals(3, proc.getChildSizeOf(folderOrFYear));

        // single folder
        List<Integer> allIds = setSingleFolder();
        assertEquals(1, musicFolderProvider.getGuestFolders().size());

        // single folder test
        folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertTrue(folderOrFYear.isFolderYear());
        container = proc.createContainer(folderOrFYear);
        assertInstanceOf(PlaylistContainer.class, container);

        assertTrue(folderOrFYear.isFolderYear());
        assertEquals(1, proc.getChildSizeOf(folderOrFYear));

        // revert
        revertFolder(allIds);
        assertEquals(2, musicFolderProvider.getGuestFolders().size());
    }

    @Test
    @Order(7)
    public void testGetChildren() {
        FolderOrFYear folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertFalse(folderOrFYear.isFolderYear());
        Container container = proc.createContainer(folderOrFYear);
        assertInstanceOf(StorageFolder.class, container);

        String childId = container
            .getId()
            .substring(container.getId().indexOf(ProcId.CID_SEPA) + 1);
        folderOrFYear = proc.getDirectChild(childId);
        assertFalse(folderOrFYear.isFolderYear());
        assertEquals("Music", folderOrFYear.getFolder().name());

        List<FYearOrFYAlbum> fYearOrFYAlbums = proc
            .getChildren(folderOrFYear, 0, Integer.MAX_VALUE);
        fYearOrFYAlbums.stream().forEach(f -> assertFalse(f.isFolderYearAlbum()));
        assertEquals(3, fYearOrFYAlbums.size());
        assertEquals("1990 - 1999", fYearOrFYAlbums.get(0).getFolderYear().year().viewName());

        // single folder
        List<Integer> allIds = setSingleFolder();
        assertEquals(1, musicFolderProvider.getGuestFolders().size());

        // single folder test
        folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertTrue(folderOrFYear.isFolderYear());
        container = proc.createContainer(folderOrFYear);
        assertInstanceOf(PlaylistContainer.class, container);

        childId = container
            .getId()
            .substring(container.getId().indexOf(ProcId.CID_SEPA) + 1);
        folderOrFYear = proc.getDirectChild(childId);
        assertEquals("Music", folderOrFYear.getFolderYear().folder().name());
        assertEquals(YearInterval.TEN_YEARS, folderOrFYear.getFolderYear().year().interval());
        assertEquals(1990, folderOrFYear.getFolderYear().year().value());
        assertEquals("1990 - 1999", folderOrFYear.getFolderYear().year().viewName());

        fYearOrFYAlbums = proc
            .getChildren(folderOrFYear, 0, Integer.MAX_VALUE);
        fYearOrFYAlbums.stream().forEach(f -> assertTrue(f.isFolderYearAlbum()));
        assertEquals(1, fYearOrFYAlbums.size());
        com.tesshu.jpsonic.domain.model.MusicFolder musicFolder =  fYearOrFYAlbums.get(0).getFolderYearAlbum().folder();
        assertEquals("Music" , musicFolder.name());
        YearAlbumEntry yearAlbumEntry = fYearOrFYAlbums.get(0).getFolderYearAlbum().yearAlbumEntry();
        assertEquals(YearInterval.TEN_YEARS , yearAlbumEntry.yearGroup().interval());
        assertEquals(1990 , yearAlbumEntry.yearGroup().value());
        assertEquals("1990 - 1999" , yearAlbumEntry.yearGroup().viewName());
        assertEquals("_ID3_ARTIST_ Sixteen Horsepower", yearAlbumEntry.artistName().get());
        assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", yearAlbumEntry.albumName());

        // revert
        revertFolder(allIds);
        assertEquals(2, musicFolderProvider.getGuestFolders().size());
    }

    @Test
    @Order(8)
    public void testAddChild() {
        FolderOrFYear folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertFalse(folderOrFYear.isFolderYear());
        Container container = proc.createContainer(folderOrFYear);
        assertInstanceOf(StorageFolder.class, container);

        String childId = container
            .getId()
            .substring(container.getId().indexOf(ProcId.CID_SEPA) + 1);
        folderOrFYear = proc.getDirectChild(childId);
        assertFalse(folderOrFYear.isFolderYear());
        List<FYearOrFYAlbum> fYearOrFYAlbums = proc
            .getChildren(folderOrFYear, 0, Integer.MAX_VALUE);
        assertEquals(3, fYearOrFYAlbums.size());
        DIDLContent content = new DIDLContent();
        proc.addChild(content, fYearOrFYAlbums.get(0));
        assertEquals(1, content.getCount());
        container = content.getFirstContainer();
        assertEquals("aibfy/fy:1;TEN_YEARS,1990", container.getId());
        assertEquals("aibfy", container.getParentID());
        assertEquals("1990 - 1999", container.getTitle());
        assertEquals(1, container.getChildCount());

        // single folder
        List<Integer> allIds = setSingleFolder();
        assertEquals(1, musicFolderProvider.getGuestFolders().size());

        // single folder test
        folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertTrue(folderOrFYear.isFolderYear());
        container = proc.createContainer(folderOrFYear);
        assertInstanceOf(PlaylistContainer.class, container);
        childId = container.getId().substring(container.getId().indexOf(ProcId.CID_SEPA) + 1);

        folderOrFYear = proc.getDirectChild(childId);
        fYearOrFYAlbums = proc.getChildren(folderOrFYear, 0, Integer.MAX_VALUE);
        assertEquals(1, fYearOrFYAlbums.size());
        FYearOrFYAlbum fYearOrFYAlbum = fYearOrFYAlbums.get(0);
        assertTrue(fYearOrFYAlbum.isFolderYearAlbum());
        content = new DIDLContent();
        proc.addChild(content, fYearOrFYAlbum);

        assertEquals(1, content.getCount());
        container = content.getFirstContainer();
        assertEquals("aibfy/fya:1;yae:3;TEN_YEARS,1990", container.getId());
        assertEquals("aibfy", container.getParentID());
        assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", container.getTitle());
        assertEquals(3, container.getChildCount());

        // revert
        revertFolder(allIds);
        assertEquals(2, musicFolderProvider.getGuestFolders().size());
    }

    @Test
    @Order(9)
    public void testBrowseLeaf() throws Exception {
        FolderOrFYear folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertFalse(folderOrFYear.isFolderYear());
        Container container = proc.createContainer(folderOrFYear);
        assertInstanceOf(StorageFolder.class, container);

        String childId = container
            .getId()
            .substring(container.getId().indexOf(ProcId.CID_SEPA) + 1);
        folderOrFYear = proc.getDirectChild(childId);
        assertFalse(folderOrFYear.isFolderYear());
        List<FYearOrFYAlbum> fYearOrFYAlbums = proc
            .getChildren(folderOrFYear, 0, Integer.MAX_VALUE);
        assertEquals(3, fYearOrFYAlbums.size());
        DIDLContent content = new DIDLContent();
        proc.addChild(content, fYearOrFYAlbums.get(0));
        assertEquals(1, content.getCount());
        container = content.getFirstContainer();
        assertEquals("aibfy/fy:1;TEN_YEARS,1990", container.getId());
        String id = container.getId().substring(container.getId().indexOf(ProcId.CID_SEPA) + 1);

        BrowseResult browseResult = proc.browseLeaf(id, null, 0, Integer.MAX_VALUE);
        assertEquals(1, browseResult.getCount().getValue());

        // single folder
        List<Integer> allIds = setSingleFolder();
        assertEquals(1, musicFolderProvider.getGuestFolders().size());

        // single folder test
        folderOrFYear = proc.getDirectChildren(0, 1).get(0);
        assertTrue(folderOrFYear.isFolderYear());
        container = proc.createContainer(folderOrFYear);
        assertInstanceOf(PlaylistContainer.class, container);
        childId = container.getId().substring(container.getId().indexOf(ProcId.CID_SEPA) + 1);

        folderOrFYear = proc.getDirectChild(childId);
        fYearOrFYAlbums = proc.getChildren(folderOrFYear, 0, Integer.MAX_VALUE);
        assertEquals(1, fYearOrFYAlbums.size());
        FYearOrFYAlbum fYearOrFYAlbum = fYearOrFYAlbums.get(0);
        assertTrue(fYearOrFYAlbum.isFolderYearAlbum());
        content = new DIDLContent();
        proc.addChild(content, fYearOrFYAlbum);
        assertEquals(1, content.getCount());
        container = content.getFirstContainer();
        assertEquals("aibfy/fya:1;yae:3;TEN_YEARS,1990", container.getId());
        id = container.getId().substring(container.getId().indexOf(ProcId.CID_SEPA) + 1);

        browseResult = proc.browseLeaf(id, null, 0, Integer.MAX_VALUE);
        assertEquals(3, browseResult.getCount().getValue());
        DIDLParser parser = new DIDLParser();
        content = parser.parse(browseResult.getResult());
        List<Item> Items = content.getItems();
        assertEquals(3, Items.size());

        // revert
        revertFolder(allIds);
        assertEquals(2, musicFolderProvider.getGuestFolders().size());
    }

}
