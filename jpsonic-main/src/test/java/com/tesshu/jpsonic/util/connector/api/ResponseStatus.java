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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tesshu.jpsonic.util.connector.deserializer.ResponseStatusDeserializer;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "ResponseStatus")
@XmlEnum
@JsonDeserialize(using = ResponseStatusDeserializer.class)
public enum ResponseStatus {

    @XmlEnumValue("ok")
    OK("ok"), @XmlEnumValue("failed")
    FAILED("failed");

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // XJC Naming Conventions
    private final String value;

    ResponseStatus(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ResponseStatus fromValue(String v) {
        for (ResponseStatus c : values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
