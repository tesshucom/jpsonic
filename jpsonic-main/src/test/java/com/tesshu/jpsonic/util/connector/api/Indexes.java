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
@XmlType(name = "Indexes", propOrder = { "shortcut", "index", "child" })
public class Indexes {

    protected List<Artist> shortcut;
    protected List<Index> index;
    protected List<Child> child;
    @XmlAttribute(name = "lastModified", required = true)
    protected long lastModified;
    @XmlAttribute(name = "ignoredArticles", required = true)
    protected String ignoredArticles;

    public List<Artist> getShortcut() {
        if (shortcut == null) {
            shortcut = new ArrayList<>();
        }
        return this.shortcut;
    }

    public List<Index> getIndex() {
        if (index == null) {
            index = new ArrayList<>();
        }
        return this.index;
    }

    public List<Child> getChild() {
        if (child == null) {
            child = new ArrayList<>();
        }
        return this.child;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long value) {
        this.lastModified = value;
    }

    public String getIgnoredArticles() {
        return ignoredArticles;
    }

    public void setIgnoredArticles(String value) {
        this.ignoredArticles = value;
    }
}
