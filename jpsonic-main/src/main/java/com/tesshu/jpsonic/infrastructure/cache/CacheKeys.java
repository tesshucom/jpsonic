package com.tesshu.jpsonic.infrastructure.cache;

@SuppressWarnings({ "PMD.ImplicitFunctionalInterface", "PMD.ShortClassName",
        "PMD.ClassNamingConventions", "PMD.FieldNamingConventions",
        "PMD.MissingStaticMethodInNonInstantiatableClass" })
public final class CacheKeys {

    private CacheKeys() {
    }

    public enum genre implements CacheKey {
        albumCount, songCount, albumAlphabetical, songAlphabetical
    }

    public enum random implements CacheKey {
        album, song, songByArtist
    }

    public interface CacheKey {
        String name();
    }
}
