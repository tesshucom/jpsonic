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

import static com.tesshu.jpsonic.service.metadata.ParserUtils.createSimplePath;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.util.PlayerUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@SuppressWarnings("PMD.TooManyStaticImports")
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
        File cmdFile = new File(transcodingService.getTranscodeDirectory(),
                PlayerUtils.isWindows() ? "ffmpeg.exe" : "ffmpeg");
        if (cmdFile.exists()) {
            return cmdFile.getAbsolutePath();
        }
        return null;
    }

    public @Nullable BufferedImage createImage(@NonNull File file, int width, int height, int offset) {

        String cmdPath = getCommandPath();
        if (isEmpty(cmdPath)) {
            return null;
        }

        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList(cmdPath, "-i", file.getAbsolutePath()));
        String options = offset < 1 ? THUMBNAIL_OPTIONS : SEEKED_OPTIONS;
        Stream.of(options.split(" ")).forEach(c -> command.add(c //
                .replaceAll("%w", Integer.toString(width)) //
                .replaceAll("%h", Integer.toString(height)) //
                .replaceAll("%o", Integer.toString(offset))));
        command.add("-");

        BufferedImage result;
        try {
            Process process = new ProcessBuilder(command).start();
            try (InputStream is = process.getInputStream(); OutputStream os = process.getOutputStream();
                    InputStream es = process.getErrorStream(); BufferedInputStream bis = new BufferedInputStream(is);) {
                result = ImageIO.read(bis);
            } finally {
                process.destroy();
            }
        } catch (IOException e) {
            String simplePath = createSimplePath(file);
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to create thumbnail({}): {}", simplePath, e.getMessage());
            }
            return null;
        }
        return result;
    }
}
