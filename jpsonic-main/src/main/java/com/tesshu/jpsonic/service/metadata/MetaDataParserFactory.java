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

package com.tesshu.jpsonic.service.metadata;

import java.io.File;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Factory for creating meta-data parsers.
 *
 * @author Sindre Mehus
 */
@Component
public class MetaDataParserFactory {

    private final List<MetaDataParser> parsers;

    public MetaDataParserFactory(List<MetaDataParser> parsers) {
        super();
        this.parsers = parsers;
    }

    /**
     * Returns a meta-data parser for the given file.
     *
     * @param file
     *            The file in question.
     *
     * @return An applicable parser, or <code>null</code> if no parser is found.
     */
    public MetaDataParser getParser(File file) {
        for (MetaDataParser parser : parsers) {
            if (parser.isApplicable(file)) {
                return parser;
            }
        }
        return null;
    }
}
