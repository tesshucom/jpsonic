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

package com.tesshu.jpsonic.service.jukebox;

import static com.tesshu.jpsonic.service.jukebox.AudioPlayer.State.CLOSED;
import static com.tesshu.jpsonic.service.jukebox.AudioPlayer.State.EOM;
import static com.tesshu.jpsonic.service.jukebox.AudioPlayer.State.PAUSED;
import static com.tesshu.jpsonic.service.jukebox.AudioPlayer.State.PLAYING;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple wrapper for playing sound from an input stream.
 * <p/>
 * Supports pause and resume, but not restarting.
 *
 * @author Sindre Mehus
 */
public class AudioPlayer {

    private static final Logger LOG = LoggerFactory.getLogger(AudioPlayer.class);
    public static final float DEFAULT_GAIN = 0.75f;
    private static final Object LINE_LOCK = new Object();

    private final Listener listener;
    private final boolean isVerboseLogShutdown;

    private final AtomicReference<State> state;
    private final AtomicReference<InputStream> in;
    private final AtomicReference<SourceDataLine> line;

    private FloatControl gainControl;

    public AudioPlayer(InputStream in, Listener listener, Executor executor, boolean isVerboseLogShutdown)
            throws LineUnavailableException {
        this.in = new AtomicReference<>(in);
        this.listener = listener;
        this.isVerboseLogShutdown = isVerboseLogShutdown;
        state = new AtomicReference<>(PAUSED);

        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44_100.0F, 16, 2, 4, 44_100.0F, true);
        line = new AtomicReference<>(AudioSystem.getSourceDataLine(format));
        line.get().open(format);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Opened line " + line);
        }

        if (line.get().isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            gainControl = (FloatControl) line.get().getControl(FloatControl.Type.MASTER_GAIN);
            setGain(DEFAULT_GAIN);
        }
        executor.execute(new AudioDataWriteTask());
    }

    /**
     * Starts (or resumes) the player. This only has effect if the current state is {@link State#PAUSED}.
     */
    public void play() {
        if (state.get() == PAUSED) {
            synchronized (LINE_LOCK) {
                line.get().start();
            }
            setState(PLAYING);
        }
    }

    /**
     * Pauses the player. This only has effect if the current state is {@link State#PLAYING}.
     */
    public void pause() {
        if (state.get() == PLAYING) {
            setState(PAUSED);
            synchronized (LINE_LOCK) {
                line.get().stop();
                line.get().flush();
            }
        }
    }

    /**
     * Closes the player, releasing all resources. After this the player state is {@link State#CLOSED} (unless the
     * current state is {@link State#EOM}).
     */
    public void close() {
        if (state.get() != CLOSED && state.get() != EOM) {
            setState(CLOSED);
        }

        synchronized (LINE_LOCK) {

            line.get().stop();

            try {
                if (line.get().isOpen()) {
                    line.get().close();
                    if (isVerboseLogShutdown && LOG.isInfoEnabled()) {
                        LOG.info("Closed jukebox audio source data Line.");
                    }
                }
            } catch (SecurityException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to close player: ", e);
                }
            }

            try {
                in.get().close();
            } catch (IOException e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Fail to close jukebox stream.", e);
                }
            }
        }
    }

    /**
     * Returns the player state.
     */
    public State getState() {
        return state.get();
    }

    /**
     * Sets the gain.
     *
     * @param gain
     *            The gain between 0.0 and 1.0.
     */
    public void setGain(float gain) {
        if (gainControl != null) {

            double minGainDB = gainControl.getMinimum();
            double maxGainDB = Math.min(0.0, gainControl.getMaximum()); // Don't use positive gain to avoid distortion.
            double ampGainDB = 0.5f * maxGainDB - minGainDB;
            double cste = Math.log(10.0) / 20;
            double valueDB = minGainDB + (1 / cste) * Math.log(1 + (Math.exp(cste * ampGainDB) - 1) * gain);

            valueDB = Math.min(valueDB, maxGainDB);
            valueDB = Math.max(valueDB, minGainDB);

            gainControl.setValue((float) valueDB);
        }
    }

    /**
     * Returns the position in seconds.
     */
    public int getPosition() {
        return (int) (line.get().getMicrosecondPosition() / 1_000_000L);
    }

    private void setState(State state) {
        if (this.state.getAndSet(state) != state && listener != null) {
            listener.stateChanged(this, state);
        }
    }

    @SuppressWarnings("PMD.AccessorMethodGeneration")
    /*
     * It is problematic and needs to be redesigned. At Jpsonic, the jukebox is one of the suppressed legacy features.
     */
    private class AudioDataWriteTask implements Runnable {

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[line.get().getBufferSize()];

                while (true) {

                    switch (state.get()) {
                    case CLOSED:
                    case EOM:
                        return;
                    case PAUSED:
                        Thread.sleep(250);
                        break;
                    case PLAYING:
                        // Fill buffer in order to ensure that write() receives an integral number of frames.
                        int n = fill(buffer);
                        if (n == -1) {
                            setState(EOM);
                            return;
                        }
                        line.get().write(buffer, 0, n);
                        break;
                    default:
                        throw new AssertionError("Unreachable code.");
                    }
                }
            } catch (InterruptedException | IOException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Error when copying audio data: ", e);
                }
            } finally {
                close();
            }
        }

        private int fill(byte[] buffer) throws IOException {
            int bytesRead = 0;
            while (bytesRead < buffer.length) {
                int n = in.get().read(buffer, bytesRead, buffer.length - bytesRead);
                if (n == -1) {
                    return bytesRead == 0 ? -1 : bytesRead;
                }
                bytesRead += n;
            }
            return bytesRead;
        }
    }

    public interface Listener {
        void stateChanged(AudioPlayer player, State state);
    }

    public enum State {
        PAUSED, PLAYING, CLOSED, EOM
    }
}
