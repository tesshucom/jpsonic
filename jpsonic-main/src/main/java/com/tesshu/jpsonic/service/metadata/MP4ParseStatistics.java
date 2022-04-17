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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.service.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Statistics class for MP4 parsing. By performing analysis while switching between ffprobe and Apache Tika
 * appropriately, you can reduce the total parsing time without increasing the load on the CPU. This class keeps a time
 * history of tag analysis and can calculate estimates to decide whether to use ffprobe or Tika.
 */
public class MP4ParseStatistics {

    /*
     * Initial value of ffprobe's lead time(ms). ffprobe's processing time of has nothing to do with disk speed and file
     * size, and its correlation coefficient is almost 0.
     */
    static final long CMD_LEAD_TIME_DEFAULT = 2_000;

    /*
     * Initial value of Tika's lead time(b/ms). Tika's processing time depends on disk speed and file size, and its
     * Correlation coefficient is almost 1.
     */
    static final long TIKA_BPMS_DEFAULT = 30_000;

    /*
     * Number of historical samples to be used in the calculation
     */
    private static final int SAMPLE_SIZE_MAX = 60;

    /*
     * Maximum number of history samples to be retained
     */
    private static final int HISTORY_SIZE_MAX = 120;

    /*
     * Lower limit of sample size. If it is less than this, the default value will be adopted.
     */
    private static final int SAMPLE_SIZE_LOWER_LIMIT = 2;

    private final Object cmdLock = new Object();
    private final Object tikaLock = new Object();

    List<Long> leadTimeCmd;
    List<long[]> leadTimeTika;

    public MP4ParseStatistics() {
        leadTimeCmd = new ArrayList<>();
        leadTimeTika = new ArrayList<>();
    }

    public void addCmdLeadTime(long leadTime) {
        synchronized (cmdLock) {
            leadTimeCmd.add(leadTime);
        }
    }

    public void addTikaLeadTime(long size, long leadTime) {
        synchronized (tikaLock) {
            leadTimeTika.add(new long[] { size, leadTime });
        }
    }

    /**
     * Returns an estimate of ffprobe execution time.
     */
    long getCmdLeadTimeEstimate() {

        if (leadTimeCmd.size() < SAMPLE_SIZE_LOWER_LIMIT) {
            return CMD_LEAD_TIME_DEFAULT;
        }

        List<Long> sample;
        synchronized (cmdLock) {
            sample = new CopyOnWriteArrayList<>(
                    leadTimeCmd.subList(leadTimeCmd.size() > SAMPLE_SIZE_MAX ? leadTimeCmd.size() - SAMPLE_SIZE_MAX : 0,
                            leadTimeCmd.size()));
            Collections.sort(sample);
        }

        /*
         * #1373 When measuring the execution time of ffprobe for files existing on the NAS, there may be a tendency for
         * the population to be two (because there are multiple bottlenecks on IO).Analyzing large files is slower in
         * Tika. To avoid this and get good results, you should underestimate the estimated execution time of ffprobe.
         * Use the mean of the population below the median. To filter noise. Also to simplify the calculation.
         */
        int mid = sample.size() / 2;
        long median = sample.size() % 2 == 1 ? sample.get(mid) : (sample.get(mid) + sample.get(mid - 1)) / 2;
        long average = (long) sample.stream().filter(leadTime -> leadTime <= median).mapToLong(r -> r).average()
                .getAsDouble();

        // Rotate
        synchronized (tikaLock) {
            if (HISTORY_SIZE_MAX < leadTimeCmd.size()) {
                leadTimeCmd = leadTimeCmd.subList(leadTimeCmd.size() - SAMPLE_SIZE_MAX, leadTimeCmd.size());
            }
        }

        return average;
    }

    /**
     * Returns an estimate of Tika process speeds(bit per micro second).
     */
    long getTikaBpmsEstimate() {

        if (leadTimeTika.size() < SAMPLE_SIZE_LOWER_LIMIT) {
            return TIKA_BPMS_DEFAULT;
        }

        List<long[]> sample;
        synchronized (tikaLock) {
            sample = new CopyOnWriteArrayList<>(leadTimeTika.subList(
                    leadTimeTika.size() > SAMPLE_SIZE_MAX ? leadTimeTika.size() - SAMPLE_SIZE_MAX : 0,
                    leadTimeTika.size()));
        }

        /*
         * If the number of samples is small, the bpms average is returned. Returns the mean + standard deviation of
         * bpms if the number of samples is sufficient.
         */
        List<Long> bpmsList = sample.stream().map(r -> r[0] / r[1]).collect(Collectors.toList());
        double bpmsAverage = bpmsList.stream().mapToLong(x -> x).average().getAsDouble();
        if (sample.size() < SAMPLE_SIZE_MAX) {
            return (long) bpmsAverage;
        }

        double siguma = Math.sqrt(
                bpmsList.stream().map(x -> Math.pow(x - bpmsAverage, 2.0)).mapToDouble(x -> x).average().getAsDouble());

        // Rotate
        synchronized (tikaLock) {
            if (HISTORY_SIZE_MAX < leadTimeTika.size()) {
                leadTimeTika = leadTimeTika.subList(leadTimeTika.size() - SAMPLE_SIZE_MAX, leadTimeTika.size());
            }
        }

        return (long) (bpmsAverage + siguma);
    }

    public long getThreshold() {
        return getTikaBpmsEstimate() * getCmdLeadTimeEstimate();
    }
}
