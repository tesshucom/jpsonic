package org.airsonic.player.service.search;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.search.lucene.UPnPSearchCriteria;
import org.airsonic.player.util.HomeRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ContextConfiguration(locations = {
        "/applicationContext-service.xml",
        "/applicationContext-cache.xml",
        "/applicationContext-testdb.xml",
        "/applicationContext-mockSonos.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UPnPCriteriaDirectorTestCase {

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule homeRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return homeRule.apply(spring, description);
        }
    };

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Resource
    SettingsService settingsService;

    @Resource
    UPnPCriteriaDirector builder;
    
    private String path;

    @Before
    public void setUp() {
        settingsService.setSearchComposer(true);
        assertEquals(1, settingsService.getAllMusicFolders().size());
        path = settingsService.getAllMusicFolders().get(0).getPath().getPath();
    }

    /* 
     * For BuubbleUPnP, use the following search queries.(Same for voice input.)
     * and albums by -> searchQuery3, tracs by -> searchQuery5
     */

    @Test
    public void testSearchQuery1() {
        String searchQuery1 = "(upnp:class = \"object.container.album.musicAlbum\" and dc:title contains \"にほんごはむずかしい\")";
        UPnPSearchCriteria criteria = builder.construct(0, 50, searchQuery1);
        assertEquals(Album.class, criteria.getAssignableClass());
        assertEquals(0, criteria.getOffset());
        assertEquals(50, criteria.getCount());
        assertTrue(criteria.isIncludeComposer());
        assertEquals(searchQuery1, criteria.getQuery());
        assertEquals("+((((albEX:にほんごはむずかしい*)^2.3 (alb:ほん*)^2.3) ((alb:ご*)^2.3) ((alb:むずかしい*)^2.3))) +(fId:0)", criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchQuery2() {
        String searchQuery2 = "(upnp:class = \"object.container.person.musicArtist\" and dc:title contains \"いきものがかり\")";
        UPnPSearchCriteria criteria = builder.construct(1, 51, searchQuery2);
        assertEquals(Artist.class, criteria.getAssignableClass());
        assertEquals(1, criteria.getOffset());
        assertEquals(51, criteria.getCount());
        assertTrue(criteria.isIncludeComposer());
        assertEquals(searchQuery2, criteria.getQuery());
        assertEquals("+((((artR:いきものがかり*)^1.1 art:いき*) (art:もの*) (art:かり*))) +(fId:0)", criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchQuery3() {
        String searchQuery3 = "(upnp:class = \"object.container.album.musicAlbum\" and upnp:artist contains \"日本語テスト\")";
        UPnPSearchCriteria criteria = builder.construct(2, 52, searchQuery3);
        assertEquals(Album.class, criteria.getAssignableClass());
        assertEquals(2, criteria.getOffset());
        assertEquals(52, criteria.getCount());
        assertTrue(criteria.isIncludeComposer());
        assertEquals(searchQuery3, criteria.getQuery());
        assertEquals("+((((artR:日本語てすと*)^1.1 art:日本語*) (art:テスト*))) +(fId:0)", criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchQuery4() {
        String searchQuery4 = "(upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"なくもんか\")";
        UPnPSearchCriteria criteria = builder.construct(3, 53, searchQuery4);
        assertEquals(MediaFile.class, criteria.getAssignableClass());
        assertEquals(3, criteria.getOffset());
        assertEquals(53, criteria.getCount());
        assertTrue(criteria.isIncludeComposer());
        assertEquals(searchQuery4, criteria.getQuery());
        assertEquals("+((((titEX:なくもんか*)^2.3 (tit:もん*)^2.3))) +(f:" + path + ")", criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchQuery5() {
        String searchQuery5 = "(upnp:class derivedfrom \"object.item.audioItem\" and (dc:creator contains \"日本語テスト\" or upnp:artist contains \"日本語テスト\"))";
        UPnPSearchCriteria criteria = builder.construct(4, 54, searchQuery5);
        assertEquals(MediaFile.class, criteria.getAssignableClass());
        assertEquals(4, criteria.getOffset());
        assertEquals(54, criteria.getCount());
        assertTrue(criteria.isIncludeComposer());
        assertEquals(searchQuery5, criteria.getQuery());
        assertEquals("+(((cmpR:日本語てすと* cmp:日本語*) (cmp:テスト*)) (((artR:日本語てすと*)^1.1 art:日本語*) (art:テスト*))) +(f:" + path + ")", criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchQuery6() {
        String searchQuery6 = "(upnp:class derivedfrom \"object.item.videoItem\" and dc:title contains \"日本語テスト\")";
        UPnPSearchCriteria criteria = builder.construct(5, 55, searchQuery6);
        assertEquals(MediaFile.class, criteria.getAssignableClass());
        assertEquals(5, criteria.getOffset());
        assertEquals(55, criteria.getCount());
        assertTrue(criteria.isIncludeComposer());
        assertEquals(searchQuery6, criteria.getQuery());
        assertEquals("+((((tit:日本語*)^2.3) ((tit:テスト*)^2.3))) +(f:" + path + ")", criteria.getParsedQuery().toString());
    }

}
