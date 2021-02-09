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

package org.airsonic.player.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.airsonic.player.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class which reads everything from an input stream and optionally logs it.
 *
 * @see TranscodeInputStream
 * 
 * @author Sindre Mehus
 */
public class InputStreamReaderThread extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(InputStreamReaderThread.class);

    private final InputStream input;
    private final String name;
    private final boolean log;

    public InputStreamReaderThread(InputStream input, String name, boolean log) {
        super(name + " InputStreamLogger");
        this.input = input;
        this.name = name;
        this.log = log;
    }

    @SuppressWarnings("PMD.UseTryWithResources") // False positive. pmd/pmd/issues/2882
    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (log && LOG.isInfoEnabled()) {
                    LOG.info('(' + name + ") " + line);
                }
            }
        } catch (IOException ignored) {
        } finally {
            FileUtil.closeQuietly(input);
        }
    }
}
