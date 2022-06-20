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

package com.tesshu.jpsonic.domain;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Objects;

/**
 * Represents a top level directory in which music or other media is stored.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("serial")
public class MusicFolder implements Serializable {

    private final Integer id;
    private String pathString;
    private String name;
    private boolean enabled;
    private Date changed;

    public MusicFolder(Integer id, String pathString, String name, boolean enabled, Date changed) {
        this.id = id;
        this.pathString = pathString;
        this.name = name;
        this.enabled = enabled;
        this.changed = changed;
    }

    public MusicFolder(String pathString, String name, boolean enabled, Date changed) {
        this(null, pathString, name, enabled, changed);
    }

    public Integer getId() {
        return id;
    }

    public String getPathString() {
        return pathString;
    }

    public void setPathString(String pathString) {
        this.pathString = pathString;
    }

    public Path toPath() {
        return Path.of(pathString);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Date getChanged() {
        return changed;
    }

    public void setChanged(Date changed) {
        this.changed = changed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MusicFolder)) {
            return false;
        }
        return Objects.equal(id, ((MusicFolder) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public static List<Integer> toIdList(List<MusicFolder> from) {
        return from.stream().map(MusicFolder::getId).collect(Collectors.toList());
    }

    public static List<String> toPathList(List<MusicFolder> from) {
        return from.stream().map(MusicFolder::getPathString).collect(Collectors.toList());
    }
}
