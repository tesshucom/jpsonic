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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
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

    private final Executor executor;

    private final Process process;
    private final AtomicReference<InputStream> processInputStream;
    private final AtomicReference<OutputStream> processOutputStream;
    private @Nullable AtomicReference<File> tmpFile;

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
    public TranscodeInputStream(ProcessBuilder processBuilder, @Nullable final InputStream in, @Nullable File tmpFile,
            Executor executor, boolean isVerboseLogPlaying) throws IOException {
        super();
        this.executor = executor;
        if (!isEmpty(tmpFile)) {
            this.tmpFile = new AtomicReference<>(tmpFile);
        }

        StringBuilder buf = new StringBuilder("Starting transcoder: ");
        for (String s : processBuilder.command()) {
            buf.append('[').append(s).append("] ");
        }
        if (isVerboseLogPlaying && LOG.isInfoEnabled()) {
            LOG.info(buf.toString());
        }

        process = processBuilder.start();
        processOutputStream = new AtomicReference<>(process.getOutputStream());
        processInputStream = new AtomicReference<>(process.getInputStream());

        // Must read stderr from the process, otherwise it may block.
        final String name = processBuilder.command().get(0);
        executor.execute(new TranscodedErrorStreamTask(process.getErrorStream(), name, true));

        // Copy data in a separate thread
        if (!isEmpty(in)) {
            executor.execute(new TranscodedOutputStreamTask(in, processOutputStream.get()));
        }
    }

    public static class TranscodedErrorStreamTask implements Runnable {

        private static final Logger LOG = LoggerFactory.getLogger(TranscodedErrorStreamTask.class);

        private final InputStream errorStream;
        private final String name;
        private final boolean log;

        public TranscodedErrorStreamTask(InputStream input, String name, boolean log) {
            this.errorStream = input;
            this.name = name;
            this.log = log;
        }

        @SuppressWarnings("PMD.UseTryWithResources") // False positive. pmd/pmd/issues/2882
        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (log && LOG.isInfoEnabled()) {
                        LOG.info('(' + name + ") " + line);
                    }
                }
            } catch (IOException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Error in reading process out.", e);
                }
            } finally {
                try {
                    errorStream.close();
                } catch (IOException e) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Error in reading process out.", e);
                    }
                }
            }
        }
    }

    @SuppressWarnings("PMD.UseTryWithResources") // False positive. pmd/pmd/issues/2882
    private static class TranscodedOutputStreamTask implements Runnable {
        private final InputStream in;
        private final OutputStream out;

        public TranscodedOutputStreamTask(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                IOUtils.copy(in, out);
            } catch (IOException e) {
                trace("Ignored. Will happen if the remote player closes the stream.", e);
            } finally {
                try {
                    in.close();
                    out.close();
                } catch (IOException e) {
                    trace("Error in TranscodedInputStream#close().", e);
                }
            }
        }
    }

    private static void trace(String s, Exception e) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(s, e);
        }
    }

    @Override
    public int read() throws IOException {
        return processInputStream.get().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return processInputStream.get().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return processInputStream.get().read(b, off, len);
    }

    @Override
    public void close() {
        try {
            processInputStream.get().close();
            processOutputStream.get().close();
        } catch (IOException e) {
            trace("Error in Stream#close() of ProcessBuilder.", e);
        } finally {
            if (!isEmpty(process)) {
                process.destroy();
            }
        }

        if (!isEmpty(tmpFile)) {
            /*
             * If it fails, will be removed when the VM is shut down, but once started, this product will not shut down
             * for a very long time. Therefore, it will retry up to 3 times and delete it as soon as possible.
             */
            executor.execute(() -> {
                boolean isDelete = false;
                for (int i = 0; i < 3; i++) {
                    isDelete = isEmpty(tmpFile) || tmpFile.get().delete();
                    if (isDelete) {
                        break;
                    } else {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("The deleting tmp file has been interrupted.: " + tmpFile.get(), e);
                            }
                        }
                    }
                }
                if (!isDelete && LOG.isWarnEnabled()) {
                    LOG.warn("Failed to delete tmp file: " + tmpFile.get());
                }
            });
        }
    }
}
