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

package com.tesshu.jpsonic.util.concurrent;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

/**
 * ReadWriteLock privilege switching is supported.
 */
public interface ReadWriteLockSupport {

    default void readLock(ReentrantReadWriteLock lock) {
        lock.readLock().lock();
    }

    /**
     * Completely release readLock
     */
    default void readUnlock(ReentrantReadWriteLock lock) {
        IntStream.range(0, lock.getReadHoldCount()).forEach(i -> lock.readLock().unlock());
    }

    /**
     * Promotion of readLock to writeLock. Interrupts on readLock#unlock and writeLock#lock are allowed.
     */
    default void writeLock(ReentrantReadWriteLock lock) {
        readUnlock(lock);
        lock.writeLock().lock();
    }

    default void writeUnlock(ReentrantReadWriteLock lock) {
        lock.writeLock().unlock();
    }
}
