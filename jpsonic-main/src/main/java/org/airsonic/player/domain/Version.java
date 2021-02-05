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

package org.airsonic.player.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * Represents the version number of Airsonic.
 *
 * @author Sindre Mehus
 * 
 * @version $Revision: 1.3 $ $Date: 2006/01/20 21:25:16 $
 */
public class Version implements Comparable<Version> {
    private final DefaultArtifactVersion internalVersion;

    /**
     * Creates a new version instance by parsing the given string.
     * 
     * @param version
     *            A string of the format "1.27", "1.27.2" or "1.27.beta3".
     */
    public Version(String version) {
        this.internalVersion = new DefaultArtifactVersion(version);
    }

    public int getMajor() {
        return internalVersion.getMajorVersion();
    }

    public int getMinor() {
        return internalVersion.getMinorVersion();
    }

    /**
     * Return whether this object is equal to another.
     * 
     * @param o
     *            Object to compare to.
     * 
     * @return Whether this object is equals to another.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Version) {
            return internalVersion.equals(((Version) o).internalVersion);
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code for this object.
     * 
     * @return A hash code for this object.
     */
    @Override
    public int hashCode() {
        return internalVersion.hashCode();
    }

    /**
     * Returns a string representation of the form "1.27", "1.27.2" or "1.27.beta3".
     * 
     * @return A string representation of the form "1.27", "1.27.2" or "1.27.beta3".
     */
    @Override
    public String toString() {
        return internalVersion.toString();
    }

    /**
     * Compares this object with the specified object for order.
     * 
     * @param version
     *            The object to compare to.
     * 
     * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     */
    @Override
    public int compareTo(Version version) {
        return internalVersion.compareTo(version.internalVersion);
    }

    public boolean isPreview() {
        return StringUtils.isNotBlank(internalVersion.getQualifier())
                && !StringUtils.equalsIgnoreCase(internalVersion.getQualifier(), Artifact.RELEASE_VERSION);
    }
}
