package com.tesshu.jpsonic.domain.provider.resource;

import java.util.List;

import com.tesshu.jpsonic.domain.model.PodcastChannel;
import com.tesshu.jpsonic.domain.model.PodcastChannel.ChannelPhase;
import com.tesshu.jpsonic.domain.model.PodcastEpisode;
import com.tesshu.jpsonic.domain.model.PodcastEpisode.EpisodePhase;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface PodcastProvider {

    int countChannels(@NonNull ChannelPhase... phases);

    int countEpisodes(PodcastChannel channel, @NonNull EpisodePhase... phases);

    List<PodcastChannel> findChannels(long offset, long count, @NonNull ChannelPhase... phases);

    List<PodcastEpisode> findEpisodes(PodcastChannel channel, long offset, long count,
            @NonNull EpisodePhase... phases);

    PodcastChannel requireChannel(int channelId);
}
