package com.tesshu.jpsonic.domain.provider.resource;

import java.util.List;
import java.util.Locale;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Provides server-wide locale information used by the domain.
 *
 * <p>
 * The server locale determines locale-dependent behavior such as name sorting
 * and comparison, and serves as the default locale when no user-specific locale
 * is available.
 * </p>
 */
public interface ServerLocaleProvider {

    /**
     * Returns the locales supported by the server.
     * 
     * @return the list of available locales
     */
    List<Locale> getAvailableLocales();

    /**
     * * Returns the current server-wide locale.
     * <p>
     * This locale is used as the default when no user-specific locale is available.
     * </p>
     * * * @return the current server-wide locale
     */
    @NonNull
    Locale getLocale();
}
