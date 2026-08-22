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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.infrastructure.language;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.text.Normalizer;

import com.tesshu.jpsonic.domain.system.IndexScheme;
import com.tesshu.jpsonic.infrastructure.language.JapaneseReadingUtils.ID;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.Playlist;
import com.tesshu.jpsonic.persistence.contract.Indexable;
import com.tesshu.jpsonic.persistence.result.SortCandidate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

@Component
public class JapaneseReadingProcessor {

    private static final char WAVY_LINE = '~';

    private final SettingsFacade settingsFacade;
    private final JapaneseReadingUtils japaneseReadingUtils;

    public JapaneseReadingProcessor(SettingsFacade settingsFacade,
            JapaneseReadingUtils japaneseReadingUtils) {
        super();
        this.settingsFacade = settingsFacade;
        this.japaneseReadingUtils = japaneseReadingUtils;
    }

    public void analyze(@NonNull Genre g) {
        String reading = defaultIfBlank(g.getName(), g.getReading());
        g
            .setReading(
                    japaneseReadingUtils.getIndexScheme() == IndexScheme.WITHOUT_JP_LANG_PROCESSING
                            ? reading
                            : japaneseReadingUtils.createJapaneseReading(reading));
    }

    /**
     * Parse MediaFile and set sort and read to appropriate values. This analyze is
     * the initial analysis of the meta processing performed in the entire system.
     * "Reading" is used for sorting and indexing in later processing. "Sort" is
     * originally a string derived from a sort tag and is used as a search index. If
     * there is no "Sort" tag, "Reading" is created based on the name, and that
     * value is also used in the search index.
     */
    public void analyze(@NonNull MediaFile m) {
        m.setArtistSort(japaneseReadingUtils.normalize(m.getArtistSort()));
        m.setArtistReading(japaneseReadingUtils.createReading(m.getArtist(), m.getArtistSort()));
        m.setAlbumArtistSort(japaneseReadingUtils.normalize(m.getAlbumArtistSort()));
        m
            .setAlbumArtistReading(
                    japaneseReadingUtils.createReading(m.getAlbumArtist(), m.getAlbumArtistSort()));
        m.setAlbumSort(japaneseReadingUtils.normalize(m.getAlbumSort()));
        m.setAlbumReading(japaneseReadingUtils.createReading(m.getAlbumName(), m.getAlbumSort()));
    }

    public void analyze(@NonNull Playlist p) {
        String reading = defaultIfBlank(p.getName(), p.getReading());
        p
            .setReading(
                    japaneseReadingUtils.getIndexScheme() == IndexScheme.WITHOUT_JP_LANG_PROCESSING
                            ? reading
                            : japaneseReadingUtils.createJapaneseReading(reading));
    }

    public void analyze(@NonNull SortCandidate c) {
        c.setReading(japaneseReadingUtils.createReading(c.getName(), c.getSort()));
        if (isEmpty(c.getSort())) {
            c.setSort(c.getReading());
        }
    }

    /**
     * This method returns the normalized Artist name that can also be used to
     * create the index prefix.
     *
     * @return indexable Name
     */
    String createIndexableName(@Nullable String value) {
        String indexableName = value;
        IndexScheme scheme = japaneseReadingUtils.getIndexScheme();
        if (scheme == IndexScheme.NATIVE_JAPANESE && value.charAt(0) > WAVY_LINE) {
            indexableName = japaneseReadingUtils.transliterate(ID.TO_HALFWIDTH, indexableName);
            indexableName = japaneseReadingUtils.transliterate(ID.TO_KATAKANA, indexableName);
        } else if (scheme == IndexScheme.ROMANIZED_JAPANESE
                || settingsFacade.get(SKeys.advanced.index.ignoreFullWidth)) {
            indexableName = japaneseReadingUtils.transliterate(ID.TO_HALFWIDTH, indexableName);
        }

        if (scheme == IndexScheme.NATIVE_JAPANESE
                || settingsFacade.get(SKeys.advanced.index.deleteDiacritic)) {
            indexableName = Normalizer
                .normalize(indexableName, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        }
        return indexableName;
    }

    public @NonNull String createIndexableName(@NonNull Indexable indexable) {
        IndexScheme scheme = japaneseReadingUtils.getIndexScheme();
        @NonNull
        String name = japaneseReadingUtils.removeArticles(indexable.getName());
        if (scheme == IndexScheme.WITHOUT_JP_LANG_PROCESSING || isEmpty(indexable.getReading())
                || indexable.getName().equals(indexable.getReading())
                || !japaneseReadingUtils.isJapaneseReadable(name)) {
            return createIndexableName(name);
        }
        return createIndexableName(japaneseReadingUtils.removeArticles(indexable.getReading()));
    }

    public void clear() {
        japaneseReadingUtils.clear();
    }
}
