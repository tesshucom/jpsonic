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

package com.tesshu.jpsonic.service.search;

import org.apache.lucene.search.Query;

/**
 * Abstract class that holds Lucene queries. Objects representing subclass search criteria must properly return parsed
 * Lucene queries.
 */
public class LuceneSearchCriteria {

    private final String query;
    private final int offset;
    private final int count;

    private Query parsedQuery;

    protected LuceneSearchCriteria(String query, int offset, int count) {
        this.query = query;
        this.offset = offset;
        this.count = count;
    }

    public final int getCount() {
        return count;
    }

    public final int getOffset() {
        return offset;
    }

    public final Query getParsedQuery() {
        return parsedQuery;
    }

    public final String getQuery() {
        return query;
    }

    protected final void setParsedQuery(Query parsedQuery) {
        this.parsedQuery = parsedQuery;
    }
}
