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

import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "License")
public class License {

    @XmlAttribute(name = "valid", required = true)
    protected boolean valid;
    @XmlAttribute(name = "email")
    protected String email;
    @XmlAttribute(name = "licenseExpires")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar licenseExpires;
    @XmlAttribute(name = "trialExpires")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar trialExpires;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean value) {
        this.valid = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    public XMLGregorianCalendar getLicenseExpires() {
        return licenseExpires;
    }

    public void setLicenseExpires(XMLGregorianCalendar value) {
        this.licenseExpires = value;
    }

    public XMLGregorianCalendar getTrialExpires() {
        return trialExpires;
    }

    public void setTrialExpires(XMLGregorianCalendar value) {
        this.trialExpires = value;
    }
}
