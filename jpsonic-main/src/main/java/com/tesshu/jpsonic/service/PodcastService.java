
package com.tesshu.jpsonic.service;

import java.util.List;

import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.domain.PodcastStatus;

/**
 * Interface for PodcastService. All methods exist in legacy code.
 */
public interface PodcastService {

    /**
     * Creates a new Podcast channel.
     *
     * @param url
     *            The URL of the Podcast channel.
     */
    void createChannel(String url);

    /**
     * Returns a single Podcast channel.
     */
    PodcastChannel getChannel(int channelId);

    /**
     * Returns all Podcast channels.
     *
     * @return Possibly empty list of all Podcast channels.
     */
    List<PodcastChannel> getAllChannels();

    /**
     * Returns all Podcast episodes for a given channel.
     *
     * @param channelId
     *            The Podcast channel ID.
     *
     * @return Possibly empty list of all Podcast episodes for the given channel, sorted in reverse chronological order
     *         (newest episode first).
     */
    List<PodcastEpisode> getEpisodes(int channelId);

    /**
     * Returns the N newest episodes.
     *
     * @return Possibly empty list of the newest Podcast episodes, sorted in reverse chronological order (newest episode
     *         first).
     */
    List<PodcastEpisode> getNewestEpisodes(int count);

    PodcastEpisode getEpisode(int episodeId, boolean includeDeleted);

    PodcastEpisode getEpisodeStrict(int episodeId, boolean includeDeleted);

    void refreshChannel(int channelId, boolean downloadEpisodes);

    void refreshAllChannels(boolean downloadEpisodes);

    void downloadEpisode(PodcastEpisode episode);

    /**
     * Deletes the Podcast channel with the given ID.
     *
     * @param channelId
     *            The Podcast channel ID.
     */
    void deleteChannel(int channelId);

    /**
     * Deletes the Podcast episode with the given ID.
     *
     * @param episodeId
     *            The Podcast episode ID.
     * @param logicalDelete
     *            Whether to perform a logical delete by setting the episode status to {@link PodcastStatus#DELETED}.
     */
    void deleteEpisode(int episodeId, boolean logicalDelete);

}
