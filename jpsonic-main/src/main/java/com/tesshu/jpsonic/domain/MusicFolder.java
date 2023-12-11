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
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private Instant changed;
    private Integer folderOrder;
    private boolean archived;

    public MusicFolder(Integer id, String pathString, String name, boolean enabled, Instant changed,
            Integer folderOrder, boolean archived) {
        this.id = id;
        this.pathString = pathString;
        this.name = name;
        this.enabled = enabled;
        this.changed = changed;
        this.folderOrder = folderOrder;
        this.archived = archived;
    }

    public MusicFolder(String pathString, String name, boolean enabled, Instant changed, boolean archived) {
        this(null, pathString, name, enabled, changed, null, archived);
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

    public Instant getChanged() {
        return changed;
    }

    public void setChanged(Instant changed) {
        this.changed = changed;
    }

    public Integer getFolderOrder() {
        return folderOrder;
    }

    public void setFolderOrder(int folderOrder) {
        this.folderOrder = folderOrder;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (id == null) {
            return false;
        } else if (o instanceof MusicFolder that) {
            return id.equals(that.id);
        }
        return false;
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
