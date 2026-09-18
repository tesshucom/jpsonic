package com.tesshu.jpsonic.domain.model;

import java.util.Objects;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

//spotless:off
public record PodcastChannel(
        int id, @NonNull
        String title,
        ChannelPhase channelPhase,
        Optional<String> thumbUri) {

    public PodcastChannel(
            int id,
            @NonNull String title,
            String channelPhase,
            @Nullable String thumbUri) {
        this(
                id,
                Objects.requireNonNullElse(title, ""),
                ChannelPhase.of(channelPhase),
                Optional.ofNullable(thumbUri));
    }
//spotless:on

    /**
     * Defines the sequential phases in a channel lifecycle. Represents the current
     * definitive state within the chronological transition.
     */
    public enum ChannelPhase {

        // Subject to future reconsideration or additions
        NEW, DOWNLOADING, COMPLETED, ERROR, DELETED, SKIPPED;

        static ChannelPhase of(String name) {
            if (name == null) {
                return NEW;
            }
            String trimmedName = name.trim();
            for (ChannelPhase t : values()) {
                if (t.name().equals(trimmedName)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Unexpected PodcastPhase value: '" + name + "'");
        }
    }
}
