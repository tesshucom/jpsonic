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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.airsonic.player.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subclass of {@link InputStream} which provides on-the-fly transcoding. Instances of <code>TranscodeInputStream</code>
 * can be chained together, for instance to convert from OGG to WAV to MP3.
 *
 * @author Sindre Mehus
 */
public final class TranscodeInputStream extends InputStream {

    private static final Logger LOG = LoggerFactory.getLogger(TranscodeInputStream.class);

    private final InputStream processInputStream;
    private final OutputStream processOutputStream;
    private final Process process;
    private final File tmpFile;

    /**
     * Creates a transcoded input stream by executing an external process. If <code>in</code> is not null, data from it
     * is copied to the command.
     *
     * @param processBuilder
     *            Used to create the external process.
     * @param in
     *            Data to feed to the process. May be {@code null}.
     * @param tmpFile
     *            Temporary file to delete when this stream is closed. May be {@code null}.
     * 
     * @throws IOException
     *             If an I/O error occurs.
     */
    public TranscodeInputStream(ProcessBuilder processBuilder, final InputStream in, File tmpFile) throws IOException {
        super();

        this.tmpFile = tmpFile;

        StringBuilder buf = new StringBuilder("Starting transcoder: ");
        for (String s : processBuilder.command()) {
            buf.append('[').append(s).append("] ");
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(buf.toString());
        }

        process = processBuilder.start();
        processOutputStream = process.getOutputStream();
        processInputStream = process.getInputStream();

        // Must read stderr from the process, otherwise it may block.
        final String name = processBuilder.command().get(0);
        new InputStreamReaderThread(process.getErrorStream(), name, true).start();

        // Copy data in a separate thread
        if (in != null) {
            new TranscodedInputStreamThread(name, in, processOutputStream).start();
        }
    }

    @SuppressWarnings({ "PMD.UseTryWithResources", "PMD.EmptyCatchBlock" })
    /*
     * [UseTryWithResources] False positive. pmd/pmd/issues/2882 [EmptyCatchBlock] Triage in #824
     */
    private static class TranscodedInputStreamThread extends Thread {
        private final InputStream in;
        private final OutputStream out;

        public TranscodedInputStreamThread(String name, InputStream in, OutputStream out) {
            super(name + " TranscodedInputStream copy thread");
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                IOUtils.copy(in, out);
            } catch (IOException x) {
                // Intentionally ignored. Will happen if the remote player closes the stream.
            } finally {
                FileUtil.closeQuietly(in);
                FileUtil.closeQuietly(out);
            }
        }
    }

    /**
     * @see InputStream#read()
     */
    @Override
    public int read() throws IOException {
        return processInputStream.read();
    }

    /**
     * @see InputStream#read(byte[])
     */
    @Override
    public int read(byte[] b) throws IOException {
        return processInputStream.read(b);
    }

    /**
     * @see InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return processInputStream.read(b, off, len);
    }

    /**
     * @see InputStream#close()
     */
    @Override
    public void close() {
        FileUtil.closeQuietly(processInputStream);
        FileUtil.closeQuietly(processOutputStream);

        if (process != null) {
            process.destroy();
        }

        if (tmpFile != null && !tmpFile.delete() && LOG.isWarnEnabled()) {
            LOG.warn("Failed to delete tmp file: " + tmpFile);
        }
    }
}
