
package org.airsonic.player.service.search;

import org.apache.lucene.search.Query;

/**
 * Abstract class that holds Lucene queries. Objects representing subclass search criteria must properly return parsed
 * Lucene queries.
 */
public class LuceneSearchCriteria {

    private Query parsedQuery;

    private final String query;

    private final int offset;

    private final int count;

    private final boolean includeComposer;

    protected LuceneSearchCriteria(String query, int offset, int count, boolean includeComposer) {
        this.query = query;
        this.offset = offset;
        this.count = count;
        this.includeComposer = includeComposer;
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

    public boolean isIncludeComposer() {
        return includeComposer;
    }

    final void setParsedQuery(Query parsedQuery) {
        this.parsedQuery = parsedQuery;
    }

}
