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

package com.tesshu.jpsonic.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Date;
import java.util.List;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.domain.PodcastStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit test of {@link PodcastDao}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class PodcastDaoTest {

    @Autowired
    private GenericDaoHelper daoHelper;

    @Autowired
    private PodcastDao podcastDao;

    @BeforeEach
    public void setUp() {
        daoHelper.getJdbcTemplate().execute("delete from podcast_channel");
    }

    @Test
    void testCreateChannel() {
        PodcastChannel channel = new PodcastChannel("http://foo");
        podcastDao.createChannel(channel);

        PodcastChannel newChannel = podcastDao.getAllChannels().get(0);
        assertNotNull(newChannel.getId(), "Wrong ID.");
        assertChannelEquals(channel, newChannel);
    }

    @Test
    void testChannelId() {
        int channelId = podcastDao.createChannel(new PodcastChannel("http://foo"));

        assertEquals(channelId + 1, podcastDao.createChannel(new PodcastChannel("http://foo")),
                "Error in createChannel.");
        assertEquals(channelId + 2, podcastDao.createChannel(new PodcastChannel("http://foo")),
                "Error in createChannel.");
        assertEquals(channelId + 3, podcastDao.createChannel(new PodcastChannel("http://foo")),
                "Error in createChannel.");

        podcastDao.deleteChannel(channelId + 1);
        assertEquals(channelId + 4, podcastDao.createChannel(new PodcastChannel("http://foo")),
                "Error in createChannel.");

        podcastDao.deleteChannel(channelId + 4);
        assertEquals(channelId + 5, podcastDao.createChannel(new PodcastChannel("http://foo")),
                "Error in createChannel.");
    }

    @Test
    void testUpdateChannel() {
        PodcastChannel channel = new PodcastChannel("http://foo");
        podcastDao.createChannel(channel);
        channel = podcastDao.getAllChannels().get(0);

        channel.setUrl("http://bar");
        channel.setTitle("Title");
        channel.setDescription("Description");
        channel.setImageUrl("http://foo/bar.jpg");
        channel.setStatus(PodcastStatus.ERROR);
        channel.setErrorMessage("Something went terribly wrong.");

        podcastDao.updateChannel(channel);
        PodcastChannel newChannel = podcastDao.getAllChannels().get(0);

        assertEquals(channel.getId(), newChannel.getId(), "Wrong ID.");
        assertChannelEquals(channel, newChannel);
    }

    @Test
    void testDeleteChannel() {
        assertEquals(0, podcastDao.getAllChannels().size(), "Wrong number of channels.");

        PodcastChannel channel = new PodcastChannel("http://foo");
        podcastDao.createChannel(channel);
        assertEquals(1, podcastDao.getAllChannels().size(), "Wrong number of channels.");

        podcastDao.createChannel(channel);
        assertEquals(2, podcastDao.getAllChannels().size(), "Wrong number of channels.");

        podcastDao.deleteChannel(podcastDao.getAllChannels().get(0).getId());
        assertEquals(1, podcastDao.getAllChannels().size(), "Wrong number of channels.");

        podcastDao.deleteChannel(podcastDao.getAllChannels().get(0).getId());
        assertEquals(0, podcastDao.getAllChannels().size(), "Wrong number of channels.");
    }

    @Test
    void testCreateEpisode() {
        int channelId = createChannel();
        PodcastEpisode episode = new PodcastEpisode(null, channelId, "http://bar", "path", "title", "description",
                new Date(), "12:34", null, null, PodcastStatus.NEW, null);
        podcastDao.createEpisode(episode);

        PodcastEpisode newEpisode = podcastDao.getEpisodes(channelId).get(0);
        assertNotNull(newEpisode.getId(), "Wrong ID.");
        assertEpisodeEquals(episode, newEpisode);
    }

    @Test
    void testGetEpisode() {
        assertNull(podcastDao.getEpisode(23), "Error in getEpisode()");

        int channelId = createChannel();
        PodcastEpisode episode = new PodcastEpisode(null, channelId, "http://bar", "path", "title", "description",
                new Date(), "12:34", 3_276_213L, 2_341_234L, PodcastStatus.NEW, "error");
        podcastDao.createEpisode(episode);

        int episodeId = podcastDao.getEpisodes(channelId).get(0).getId();
        PodcastEpisode newEpisode = podcastDao.getEpisode(episodeId);
        assertEpisodeEquals(episode, newEpisode);
    }

    @Test
    void testGetEpisodes() {
        int channelId = createChannel();
        PodcastEpisode a = new PodcastEpisode(null, channelId, "a", null, null, null, new Date(3000), null, null, null,
                PodcastStatus.NEW, null);
        PodcastEpisode b = new PodcastEpisode(null, channelId, "b", null, null, null, new Date(1000), null, null, null,
                PodcastStatus.NEW, "error");
        PodcastEpisode c = new PodcastEpisode(null, channelId, "c", null, null, null, new Date(2000), null, null, null,
                PodcastStatus.NEW, null);
        PodcastEpisode d = new PodcastEpisode(null, channelId, "c", null, null, null, null, null, null, null,
                PodcastStatus.NEW, "");
        podcastDao.createEpisode(a);
        podcastDao.createEpisode(b);
        podcastDao.createEpisode(c);
        podcastDao.createEpisode(d);

        List<PodcastEpisode> episodes = podcastDao.getEpisodes(channelId);
        assertEquals(4, episodes.size(), "Error in getEpisodes().");
        assertEpisodeEquals(d, episodes.get(0));
        assertEpisodeEquals(a, episodes.get(1));
        assertEpisodeEquals(c, episodes.get(2));
        assertEpisodeEquals(b, episodes.get(3));
    }

    @Test
    void testUpdateEpisode() {
        int channelId = createChannel();
        PodcastEpisode episode = new PodcastEpisode(null, channelId, "http://bar", null, null, null, null, null, null,
                null, PodcastStatus.NEW, null);
        podcastDao.createEpisode(episode);
        episode = podcastDao.getEpisodes(channelId).get(0);

        episode.setUrl("http://bar");
        episode.setPath("c:/tmp");
        episode.setTitle("Title");
        episode.setDescription("Description");
        episode.setPublishDate(new Date());
        episode.setDuration("1:20");
        episode.setBytesTotal(87_628_374_612L);
        episode.setBytesDownloaded(9086L);
        episode.setStatus(PodcastStatus.DOWNLOADING);
        episode.setErrorMessage("Some error");

        podcastDao.updateEpisode(episode);
        PodcastEpisode newEpisode = podcastDao.getEpisodes(channelId).get(0);
        assertEquals(episode.getId(), newEpisode.getId(), "Wrong ID.");
        assertEpisodeEquals(episode, newEpisode);
    }

    @Test
    void testDeleteEpisode() {
        int channelId = createChannel();

        assertEquals(0, podcastDao.getEpisodes(channelId).size(), "Wrong number of episodes.");

        PodcastEpisode episode = new PodcastEpisode(null, channelId, "http://bar", null, null, null, null, null, null,
                null, PodcastStatus.NEW, null);

        podcastDao.createEpisode(episode);
        assertEquals(1, podcastDao.getEpisodes(channelId).size(), "Wrong number of episodes.");

        podcastDao.createEpisode(episode);
        assertEquals(2, podcastDao.getEpisodes(channelId).size(), "Wrong number of episodes.");

        podcastDao.deleteEpisode(podcastDao.getEpisodes(channelId).get(0).getId());
        assertEquals(1, podcastDao.getEpisodes(channelId).size(), "Wrong number of episodes.");

        podcastDao.deleteEpisode(podcastDao.getEpisodes(channelId).get(0).getId());
        assertEquals(0, podcastDao.getEpisodes(channelId).size(), "Wrong number of episodes.");
    }

    @Test
    void testCascadingDelete() {
        int channelId = createChannel();
        PodcastEpisode episode = new PodcastEpisode(null, channelId, "http://bar", null, null, null, null, null, null,
                null, PodcastStatus.NEW, null);
        podcastDao.createEpisode(episode);
        podcastDao.createEpisode(episode);
        assertEquals(2, podcastDao.getEpisodes(channelId).size(), "Wrong number of episodes.");

        podcastDao.deleteChannel(channelId);
        assertEquals(0, podcastDao.getEpisodes(channelId).size(), "Wrong number of episodes.");
    }

    private int createChannel() {
        PodcastChannel channel = new PodcastChannel("http://foo");
        podcastDao.createChannel(channel);
        channel = podcastDao.getAllChannels().get(0);
        return channel.getId();
    }

    private void assertChannelEquals(PodcastChannel expected, PodcastChannel actual) {
        assertEquals(expected.getUrl(), actual.getUrl(), "Wrong URL.");
        assertEquals(expected.getTitle(), actual.getTitle(), "Wrong title.");
        assertEquals(expected.getDescription(), actual.getDescription(), "Wrong description.");
        assertEquals(expected.getImageUrl(), actual.getImageUrl(), "Wrong image URL.");
        assertSame(expected.getStatus(), actual.getStatus(), "Wrong status.");
        assertEquals(expected.getErrorMessage(), actual.getErrorMessage(), "Wrong error message.");
    }

    private void assertEpisodeEquals(PodcastEpisode expected, PodcastEpisode actual) {
        assertEquals(expected.getUrl(), actual.getUrl(), "Wrong URL.");
        assertEquals(expected.getPath(), actual.getPath(), "Wrong path.");
        assertEquals(expected.getTitle(), actual.getTitle(), "Wrong title.");
        assertEquals(expected.getDescription(), actual.getDescription(), "Wrong description.");
        assertEquals(expected.getPublishDate(), actual.getPublishDate(), "Wrong date.");
        assertEquals(expected.getDuration(), actual.getDuration(), "Wrong duration.");
        assertEquals(expected.getBytesTotal(), actual.getBytesTotal(), "Wrong bytes total.");
        assertEquals(expected.getBytesDownloaded(), actual.getBytesDownloaded(), "Wrong bytes downloaded.");
        assertSame(expected.getStatus(), actual.getStatus(), "Wrong status.");
        assertEquals(expected.getErrorMessage(), actual.getErrorMessage(), "Wrong error message.");
    }

}
