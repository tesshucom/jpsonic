package org.airsonic.player.service.jukebox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletionException;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class PlayerTest implements AudioPlayer.Listener {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerTest.class);

    private AudioPlayer player;

    public PlayerTest() {
        createGUI();
    }

    private void createGUI() {
        JFrame frame = new JFrame();

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        JButton resetButton = new JButton("Reset");
        final JSlider gainSlider = new JSlider(0, 1000);

        startButton.addActionListener(e -> {
            createPlayer();
            player.play();
        });
        stopButton.addActionListener(e -> player.pause());
        resetButton.addActionListener(e -> {
            player.close();
            createPlayer();
        });
        gainSlider.addChangeListener(e -> {
            float gain = gainSlider.getValue() / 1000.0F;
            player.setGain(gain);
        });

        frame.setLayout(new FlowLayout());
        frame.add(startButton);
        frame.add(stopButton);
        frame.add(resetButton);
        frame.add(gainSlider);

        frame.pack();
        frame.setVisible(true);
    }

    private void createPlayer() {
        try {
            File f = new File("/Users/sindre/Downloads/sample.au");
            player = new AudioPlayer(Files.newInputStream(Paths.get(f.toURI())), this);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    public static void main(String[] args) {
        new PlayerTest();
    }

    public void stateChanged(AudioPlayer player, AudioPlayer.State state) {
        if (LOG.isInfoEnabled()) {
            LOG.info(state.name());
        }
    }
}

