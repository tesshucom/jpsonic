/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package org.airsonic.player.service.search.lucene;

import org.airsonic.player.domain.SearchCriteria;
import org.apache.lucene.search.Query;

/**
 * Criteria that abstracts field search of UPnP.
 * <p />
 * 
 * The abstract design of field search including compound sentences
 * iscomplex.<br />
 * It's similar to "wheel reinvention" of Lucene's QeryBuilder.
 * <p />
 * 
 * A UPnP query structure analysis is always required for a UPnP search. <br />
 * This class depends on the Lucene implementation and assumes that the Lucene
 * query will be converted at the same time the UPnP message is analyzed.
 */
public class UPnPFieldSearchCriteria<T> extends SearchCriteria {

    private Class<T> assignableClass;

    private Query parsedQuery;

    /**
     * Returns a class that represents the search target and return value. For UPnP
     * searches, this value can only be determined after parsing UPnP.
     * 
     * @return T that represents search target and return value
     */
    public Class<T> getAssignableClass() {
        return assignableClass;
    }

    /**
     * Returns a Lucene query generated from a UPnP query.
     * 
     * @return Lucene query generated from UPnP query based on UPnP v1.0 service
     *         template version
     */
    public Query getParsedQuery() {
        return parsedQuery;
    }

    public void setAssignableClass(Class<T> assignableClass) {
        this.assignableClass = assignableClass;
    }

    public void setParsedQuery(Query parsedQuery) {
        this.parsedQuery = parsedQuery;
    }

}
