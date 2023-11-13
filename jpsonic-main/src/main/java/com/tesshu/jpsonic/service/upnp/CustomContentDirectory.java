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

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;

public abstract class CustomContentDirectory extends AbstractContentDirectoryService {

    public CustomContentDirectory() {
        super(Arrays.asList("*"), Collections.emptyList());
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // fourthline/DIDLParser#generate
    protected BrowseResult createBrowseResult(DIDLContent content, int count, int totalMatches)
            throws ExecutionException {
        try {
            return new BrowseResult(new DIDLParser().generate(content), count, totalMatches);
        } catch (Exception e) {
            throw new ExecutionException("Unable to generate XML representation of content model.", e);
        }
    }

    @Override
    public abstract BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult,
            long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException;

}
