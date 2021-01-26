/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */

package org.airsonic.player.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A play queue is a list of music files that are associated to a remote player.
 *
 * @author Sindre Mehus
 */
public class PlayQueue {

    private List<MediaFile> files = new ArrayList<>();
    private AtomicBoolean repeatEnabled = new AtomicBoolean();
    private String name = "(unnamed)";
    private Status status = Status.PLAYING;

    private RandomSearchCriteria randomSearchCriteria;
    private InternetRadio internetRadio;

    private static final Object STATUS_LOCK = new Object();
    private static final Object SEQUENCE_LOCK = new Object();

    /**
     * The index of the current song, or -1 is the end of the playlist is reached. Note that both the index and the
     * playlist size can be zero.
     */
    private int index;

    /**
     * Used for undo functionality.
     */
    private List<MediaFile> filesBackup = new ArrayList<>();
    private int indexBackup;

    /**
     * Returns the user-defined name of the playlist.
     *
     * @return The name of the playlist, or <code>null</code> if no name has been assigned.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user-defined name of the playlist.
     *
     * @param name
     *            The name of the playlist.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the current song in the playlist.
     *
     * @return The current song in the playlist, or <code>null</code> if no current song exists.
     */
    public MediaFile getCurrentFile() {
        synchronized (STATUS_LOCK) {
            synchronized (SEQUENCE_LOCK) {
                if (index == -1 || index == 0 && size() == 0) {
                    setStatus(Status.STOPPED);
                    return null;
                } else {
                    MediaFile file = files.get(index);

                    // Remove file from playlist if it doesn't exist.
                    if (!file.exists()) {
                        files.remove(index);
                        index = Math.max(0, Math.min(index, size() - 1));
                        return getCurrentFile();
                    }

                    return file;
                }
            }
        }
    }

    /**
     * Returns all music files in the playlist.
     *
     * @return All music files in the playlist.
     */
    public List<MediaFile> getFiles() {
        synchronized (SEQUENCE_LOCK) {
            return files;
        }
    }

    /**
     * Returns the music file at the given index.
     *
     * @param index
     *            The index.
     * 
     * @return The music file at the given index.
     * 
     * @throws IndexOutOfBoundsException
     *             If the index is out of range.
     */
    public MediaFile getFile(int index) {
        synchronized (SEQUENCE_LOCK) {
            return files.get(index);
        }
    }

    /**
     * Skip to the next song in the playlist.
     */
    public void next() {
        synchronized (SEQUENCE_LOCK) {
            index++;

            // Reached the end?
            if (index >= size()) {
                index = isRepeatEnabled() ? 0 : -1;
            }
        }
    }

    /**
     * Returns the number of songs in the playlists.
     *
     * @return The number of songs in the playlists.
     */
    public int size() {
        synchronized (SEQUENCE_LOCK) {
            return files.size();
        }
    }

    /**
     * Returns whether the playlist is empty.
     *
     * @return Whether the playlist is empty.
     */
    public boolean isEmpty() {
        synchronized (SEQUENCE_LOCK) {
            return files.isEmpty();
        }
    }

    /**
     * Returns the index of the current song.
     *
     * @return The index of the current song, or -1 if the end of the playlist is reached.
     */
    public int getIndex() {
        synchronized (SEQUENCE_LOCK) {
            return index;
        }
    }

    /**
     * Sets the index of the current song.
     *
     * @param index
     *            The index of the current song.
     */
    public void setIndex(int index) {
        synchronized (STATUS_LOCK) {
            synchronized (SEQUENCE_LOCK) {
                makeBackup();
                this.index = Math.max(0, Math.min(index, size() - 1));
                setStatus(Status.PLAYING);
            }
        }
    }

    /**
     * Adds one or more music file to the playlist.
     *
     * @param mediaFiles
     *            The music files to add.
     * @param index
     *            Where to add them.
     */
    public void addFilesAt(Iterable<MediaFile> mediaFiles, final int index) {
        synchronized (STATUS_LOCK) {
            synchronized (SEQUENCE_LOCK) {
                makeBackup();
                AtomicInteger i = new AtomicInteger(index);
                mediaFiles.forEach(m -> files.add(i.getAndIncrement(), m));
                setStatus(Status.PLAYING);
            }
        }
    }

    /**
     * Adds one or more music file to the playlist.
     *
     * @param append
     *            Whether existing songs in the playlist should be kept.
     * @param mediaFiles
     *            The music files to add.
     */
    public void addFiles(boolean append, Iterable<MediaFile> mediaFiles) {
        synchronized (STATUS_LOCK) {
            synchronized (SEQUENCE_LOCK) {
                makeBackup();
                if (!append) {
                    index = 0;
                    files.clear();
                }
                mediaFiles.forEach(m -> files.add(m));
                setStatus(Status.PLAYING);
            }
        }
    }

    /**
     * Convenience method, equivalent to {@link #addFiles(boolean, Iterable)}.
     */
    public void addFiles(boolean append, MediaFile... mediaFiles) {
        synchronized (STATUS_LOCK) {
            synchronized (SEQUENCE_LOCK) {
                addFiles(append, Arrays.asList(mediaFiles));
            }
        }
    }

    /**
     * Removes the music file at the given index.
     *
     * @param index
     *            The playlist index.
     */
    public void removeFileAt(final int index) {
        synchronized (SEQUENCE_LOCK) {
            makeBackup();
            int i = index;
            i = Math.max(0, Math.min(i, size() - 1));
            if (this.index > i) {
                this.index--;
            }
            files.remove(i);

            this.index = Math.max(0, Math.min(this.index, size() - 1));
        }
    }

    /**
     * Clears the playlist.
     */
    public void clear() {
        synchronized (SEQUENCE_LOCK) {
            makeBackup();
            files.clear();
            setRandomSearchCriteria(null);
            setInternetRadio(null);
            index = 0;
        }
    }

    /**
     * Shuffles the playlist.
     */
    public void shuffle() {
        synchronized (STATUS_LOCK) {
            synchronized (SEQUENCE_LOCK) {
                makeBackup();
                MediaFile currentFile = getCurrentFile();
                Collections.shuffle(files);
                if (currentFile != null) {
                    Collections.swap(files, files.indexOf(currentFile), 0);
                    index = 0;
                }
            }
        }
    }

    /**
     * Sorts the playlist according to the given sort order.
     */
    public void sort(Comparator<MediaFile> comparator) {
        synchronized (STATUS_LOCK) {
            synchronized (SEQUENCE_LOCK) {
                makeBackup();
                MediaFile currentFile = getCurrentFile();
                files.sort(comparator);
                if (currentFile != null) {
                    index = files.indexOf(currentFile);
                }
            }
        }
    }

    /**
     * Rearranges the playlist using the provided indexes.
     */
    public void rearrange(int... indexes) {
        synchronized (SEQUENCE_LOCK) {
            makeBackup();
            if (indexes == null || indexes.length != size()) {
                return;
            }
            MediaFile[] newFiles = new MediaFile[files.size()];
            for (int i = 0; i < indexes.length; i++) {
                newFiles[i] = files.get(indexes[i]);
            }
            for (int i = 0; i < indexes.length; i++) {
                if (index == indexes[i]) {
                    index = i;
                    break;
                }
            }

            files.clear();
            files.addAll(Arrays.asList(newFiles));
        }
    }

    /**
     * Moves the song at the given index one step up.
     *
     * @param index
     *            The playlist index.
     */
    public void moveUp(int index) {
        moveDown(index - 1);
    }

    /**
     * Moves the song at the given index one step down.
     *
     * @param index
     *            The playlist index.
     */
    public void moveDown(int index) {
        synchronized (SEQUENCE_LOCK) {
            makeBackup();
            if (index < 0 || index >= size() - 1) {
                return;
            }
            Collections.swap(files, index, index + 1);

            if (this.index == index) {
                this.index++;
            } else if (this.index == index + 1) {
                this.index--;
            }
        }
    }

    /**
     * Returns whether the playlist is repeating.
     *
     * @return Whether the playlist is repeating.
     */
    public boolean isRepeatEnabled() {
        return repeatEnabled.get();
    }

    /**
     * Sets whether the playlist is repeating.
     *
     * @param repeatEnabled
     *            Whether the playlist is repeating.
     */
    public void setRepeatEnabled(boolean repeatEnabled) {
        this.repeatEnabled.set(repeatEnabled);
    }

    /**
     * Returns whether the play queue is in shuffle radio mode.
     *
     * @return Whether the play queue is a shuffle radio mode.
     */
    public boolean isShuffleRadioEnabled() {
        return this.randomSearchCriteria != null;
    }

    /**
     * Returns whether the play queue is a internet radio mode.
     *
     * @return Whether the play queue is a internet radio mode.
     */
    public boolean isInternetRadioEnabled() {
        return this.internetRadio != null;
    }

    /**
     * Revert the last operation.
     */
    public void undo() {
        synchronized (SEQUENCE_LOCK) {
            int indexTmp = index;

            index = indexBackup;
            files = filesBackup;
            indexBackup = indexTmp;

            List<MediaFile> filesTmp = new ArrayList<>(files);
            filesBackup = filesTmp;
        }
    }

    /**
     * Returns the playlist status.
     *
     * @return The playlist status.
     */
    public Status getStatus() {
        synchronized (STATUS_LOCK) {
            return status;
        }
    }

    /**
     * Sets the playlist status.
     *
     * @param status
     *            The playlist status.
     */
    public void setStatus(Status status) {
        synchronized (STATUS_LOCK) {
            synchronized (SEQUENCE_LOCK) {
                this.status = status;
                if (index == -1) {
                    index = Math.max(0, Math.min(index, size() - 1));
                }
            }
        }
    }

    /**
     * Sets the current internet radio
     *
     * @param internetRadio
     *            An internet radio, or <code>null</code> if this is not an internet radio playlist
     */
    public void setInternetRadio(InternetRadio internetRadio) {
        this.internetRadio = internetRadio;
    }

    /**
     * Gets the current internet radio
     *
     * @return The current internet radio, or <code>null</code> if this is not an internet radio playlist
     */
    public InternetRadio getInternetRadio() {
        return internetRadio;
    }

    /**
     * Returns the criteria used to generate this random playlist
     *
     * @return The search criteria, or <code>null</code> if this is not a random playlist.
     */
    public RandomSearchCriteria getRandomSearchCriteria() {
        return randomSearchCriteria;
    }

    /**
     * Sets the criteria used to generate this random playlist
     *
     * @param randomSearchCriteria
     *            The search criteria, or <code>null</code> if this is not a random playlist.
     */
    public void setRandomSearchCriteria(RandomSearchCriteria randomSearchCriteria) {
        this.randomSearchCriteria = randomSearchCriteria;
    }

    /**
     * Returns the total length in bytes.
     *
     * @return The total length in bytes.
     */
    public long length() {
        long length;
        synchronized (SEQUENCE_LOCK) {
            length = files.stream().mapToLong(m -> m.getFileSize()).sum();
        }
        return length;
    }

    private void makeBackup() {
        filesBackup = new ArrayList<>(files);
        indexBackup = index;
    }

    /**
     * Playlist status.
     */
    public enum Status {
        PLAYING, STOPPED
    }

    /**
     * Playlist sort order.
     */
    public enum SortOrder {
        TRACK, ARTIST, ALBUM
    }
}
