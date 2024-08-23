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

package com.tesshu.jpsonic.util.connector.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchResult", propOrder = { "match" })
public class SearchResult {

    protected List<Child> match;
    @XmlAttribute(name = "offset", required = true)
    protected int offset;
    @XmlAttribute(name = "totalHits", required = true)
    protected int totalHits;

    public List<Child> getMatch() {
        if (match == null) {
            match = new ArrayList<>();
        }
        return this.match;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int value) {
        this.offset = value;
    }

    public int getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(int value) {
        this.totalHits = value;
    }
}
