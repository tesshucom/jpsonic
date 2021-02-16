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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.search.analysis;

import java.util.Map;

import com.tesshu.jpsonic.service.search.analysis.ComplementaryFilter.Mode;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class ComplementaryFilterFactory extends TokenFilterFactory {

    private final String stopwards;

    private Mode mode;

    public ComplementaryFilterFactory(Map<String, String> args) {
        super(args);
        stopwards = get(args, "stopwards");
        String maybeMode = get(args, "mode");
        Mode.fromValue(maybeMode).ifPresent(m -> this.mode = m);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public ComplementaryFilter create(TokenStream input) {
        return new ComplementaryFilter(input, mode, stopwards);
    }

}
