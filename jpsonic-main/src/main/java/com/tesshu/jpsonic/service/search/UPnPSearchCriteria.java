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

package com.tesshu.jpsonic.service.search;

/**
 * Criteria that abstracts field search of UPnP.
 *
 * A UPnP query structure analysis is always required for a UPnP search. Instances of this class are created by Builders
 * that have UPnP message analysis capabilities. The UPnP query for field search including complex compound statements
 * can be obtained with lucene query.
 */
public class UPnPSearchCriteria extends LuceneSearchCriteria {

    private IndexType indexType;

    UPnPSearchCriteria(String upnpSearchQuery, int offset, int count) {
        super(upnpSearchQuery, offset, count);
    }

    /**
     * Returns the target index. For UPnP searches, this value can only be determined after parsing UPnP messages.
     *
     * @return T target index
     */
    public IndexType getIndexType() {
        return indexType;
    }

    public void setIndexType(IndexType assignableIndexType) {
        this.indexType = assignableIndexType;
    }
}
