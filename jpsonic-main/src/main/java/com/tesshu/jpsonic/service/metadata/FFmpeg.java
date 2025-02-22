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

import static com.tesshu.jpsonic.util.FileUtil.getShortPath;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.util.PlayerUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FFmpeg {

    private static final Logger LOG = LoggerFactory.getLogger(FFmpeg.class);

    // Automatically generate thumbnails from 100 frames
    private static final String THUMBNAIL_OPTIONS = "-v quiet -vf thumbnail=100,scale=%w:%h -frames:v 1 -f image2";

    // Same as legacy. Frame at offset position.
    private static final String SEEKED_OPTIONS = "-v quiet -r 1 -ss %o -t 1 -s %wx%h -v 0 -f image2";

    private final TranscodingService transcodingService;

    public FFmpeg(TranscodingService transcodingService) {
        super();
        this.transcodingService = transcodingService;
    }

    private @Nullable String getCommandPath() {
        Path cmdFile = Path.of(transcodingService.getTranscodeDirectory().toString(),
                PlayerUtils.isWindows() ? "ffmpeg.exe" : "ffmpeg");
        if (Files.exists(cmdFile)) {
            return cmdFile.toString();
        }
        return null;
    }

    public @Nullable BufferedImage createImage(@NonNull Path path, int width, int height, int offset) {

        String cmdPath = getCommandPath();
        if (isEmpty(cmdPath)) {
            return null;
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add(cmdPath);
        pb.command().add("-i");
        pb.command().add(path.toString());
        String options = offset < 1 ? THUMBNAIL_OPTIONS : SEEKED_OPTIONS;
        Stream.of(options.split(" ")).forEach(c -> pb.command().add(c //
                .replaceAll("%w", Integer.toString(width)) //
                .replaceAll("%h", Integer.toString(height)) //
                .replaceAll("%o", Integer.toString(offset))));
        pb.command().add("-");

        BufferedImage result;
        try {
            Process process = pb.start();
            try (InputStream is = process.getInputStream();
                    OutputStream os = process.getOutputStream();
                    InputStream es = process.getErrorStream();
                    BufferedInputStream bis = new BufferedInputStream(is);) {
                result = ImageIO.read(bis);
                os.close();
                es.close();
            } finally {
                process.destroy();
            }
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to create thumbnail({}): {}", getShortPath(path), e.getMessage());
            }
            return null;
        }
        return result;
    }
}
