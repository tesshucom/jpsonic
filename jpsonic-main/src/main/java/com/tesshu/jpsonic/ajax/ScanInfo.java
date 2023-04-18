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

package com.tesshu.jpsonic.ajax;

/**
 * Media folder scanning status.
 *
 * @author Sindre Mehus
 */
public class ScanInfo {

    private final boolean scanning;
    private final int scanningCount;
    private int phase = -1;
    private int phaseMax = -1;
    private String phaseName;
    private int thread = -1;

    public ScanInfo(boolean scanning, int scanningCount) {
        super();
        this.scanning = scanning;
        this.scanningCount = scanningCount;
    }

    public boolean isScanning() {
        return scanning;
    }

    public int getScanningCount() {
        return scanningCount;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public int getPhaseMax() {
        return phaseMax;
    }

    public void setPhaseMax(int phaseMax) {
        this.phaseMax = phaseMax;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public int getThread() {
        return thread;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }
}
