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

package com.tesshu.jpsonic.service.upnp;

import java.util.concurrent.ExecutionException;

import com.google.common.collect.Lists;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;

/**
 * @author Sindre Mehus
 */
public abstract class CustomContentDirectory extends AbstractContentDirectoryService {

    public CustomContentDirectory() {
        super(Lists.newArrayList("*"), Lists.newArrayList());
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    /*
     * Wrap and rethrow due to constraints of 'fourthline' {@link DIDLParser#generate(DIDLContent)}
     */
    protected BrowseResult createBrowseResult(DIDLContent didl, int count, int totalMatches) throws ExecutionException {
        try {
            return new BrowseResult(new DIDLParser().generate(didl), count, totalMatches);
        } catch (Exception e) {
            throw new ExecutionException("Unable to generate XML representation of content model.", e);
        }
    }

    @Override
    public BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult,
            long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {
        // You can override this method to implement searching!
        return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
    }

}
