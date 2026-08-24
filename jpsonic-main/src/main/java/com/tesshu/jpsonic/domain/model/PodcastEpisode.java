package com.tesshu.jpsonic.domain.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

//spotless:off
public record PodcastEpisode(
        int id, @NonNull
        String title,
        int channelId,
        @NonNull EpisodePhase episodePhase,
        Optional<Instant> publishDate,
        Optional<Path> path) {

    public PodcastEpisode(
            int id,
            @NonNull String title,
            int channelId,
            @NonNull String episodePhase,
            @Nullable Instant publishDate,
            @Nullable String path) {

        this(
                id,
                title,
                channelId,
                EpisodePhase.of(episodePhase),
                Optional.ofNullable(publishDate),
                Optional.ofNullable(path == null ? null : Path.of(path)));
    }
//spotless:on

    /**
     * Defines the sequential phases in an episode lifecycle. Represents the current
     * definitive state within the chronological transition.
     */
    public enum EpisodePhase {

        // Subject to future reconsideration or additions
        NEW, DOWNLOADING, COMPLETED, ERROR, DELETED, SKIPPED;

        static EpisodePhase of(String name) {
            if (name == null) {
                throw new IllegalArgumentException("PodcastPhase cannot be null");
            }
            String trimmedName = name.trim();
            for (EpisodePhase t : values()) {
                if (t.name().equals(trimmedName)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Unexpected PodcastPhase value: '" + name + "'");
        }
    }
}
