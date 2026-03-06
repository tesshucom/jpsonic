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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.service.search;

import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.springframework.stereotype.Component;

@Component
public class LuceneUtils {

    /*
     * Depends on Lucene10
     */
    long getTotalHits(TopDocs topDocs) {
        return topDocs.totalHits.value();
    }

    /*
     * Depends on Lucene10
     */
    int getCount(IndexSearcher searcher, Query query) throws IOException {
        return searcher.count(query);
    }
}
