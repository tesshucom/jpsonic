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

import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Directory", propOrder = { "child" })
public class Directory {

    protected List<Child> child;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "parent")
    protected String parent;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "starred")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar starred;
    @XmlAttribute(name = "userRating")
    protected Integer userRating;
    @XmlAttribute(name = "averageRating")
    protected Double averageRating;
    @XmlAttribute(name = "playCount")
    protected Long playCount;

    public List<Child> getChild() {
        if (child == null) {
            child = new ArrayList<>();
        }
        return this.child;
    }

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String value) {
        this.parent = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public XMLGregorianCalendar getStarred() {
        return starred;
    }

    public void setStarred(XMLGregorianCalendar value) {
        this.starred = value;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public void setUserRating(Integer value) {
        this.userRating = value;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double value) {
        this.averageRating = value;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Long value) {
        this.playCount = value;
    }
}
