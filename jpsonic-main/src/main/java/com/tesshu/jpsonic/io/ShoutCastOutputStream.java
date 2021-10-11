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

package com.tesshu.jpsonic.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayQueue;
import org.apache.commons.lang.StringUtils;

/**
 * Implements SHOUTcast support by decorating an existing output stream.
 * <p/>
 * Based on protocol description found on <em>http://www.smackfu.com/stuff/programming/shoutcast.html</em>
 *
 * @author Sindre Mehus
 */
public class ShoutCastOutputStream extends OutputStream {

    /**
     * Maps from miscellaneous accented characters to similar-looking ASCII characters.
     */
    private static final char[][] CHAR_MAP = { { '\u00C0', 'A' }, // À
            { '\u00C1', 'A' }, // Á
            { '\u00C2', 'A' }, // Â
            { '\u00C3', 'A' }, // Ã
            { '\u00C4', 'A' }, // Ä
            { '\u00C5', 'A' }, // Å
            { '\u00C6', 'A' }, // Æ
            { '\u00C8', 'E' }, // È
            { '\u00C9', 'E' }, // É
            { '\u00CA', 'E' }, // Ê
            { '\u00CB', 'E' }, // Ë
            { '\u00CC', 'I' }, // Ì
            { '\u00CD', 'I' }, // Í
            { '\u00CE', 'I' }, // Î
            { '\u00CF', 'I' }, // Ï
            { '\u00D2', 'O' }, // Ò
            { '\u00D3', 'O' }, // Ó
            { '\u00D4', 'O' }, // Ô
            { '\u00D5', 'O' }, // Õ
            { '\u00D6', 'O' }, // Ö
            { '\u00D9', 'U' }, // Ù
            { '\u00DA', 'U' }, // Ú
            { '\u00DB', 'U' }, // Û
            { '\u00DC', 'U' }, // Ü
            { '\u00DF', 'B' }, // ß
            { '\u00E0', 'a' }, // à
            { '\u00E1', 'a' }, // á
            { '\u00E2', 'a' }, // â
            { '\u00E3', 'a' }, // ã
            { '\u00E4', 'a' }, // ä
            { '\u00E5', 'a' }, // å
            { '\u00E6', 'a' }, // æ
            { '\u00E7', 'c' }, // ç
            { '\u00E8', 'e' }, // è
            { '\u00E9', 'e' }, // é
            { '\u00EA', 'e' }, // ê
            { '\u00EB', 'e' }, // ë
            { '\u00EC', 'i' }, // ì
            { '\u00ED', 'i' }, // í
            { '\u00EE', 'i' }, // î
            { '\u00EF', 'i' }, // ï
            { '\u00F1', 'n' }, // ñ
            { '\u00F2', 'o' }, // ò
            { '\u00F3', 'o' }, // ó
            { '\u00F4', 'o' }, // ô
            { '\u00F5', 'o' }, // õ
            { '\u00F6', 'o' }, // ö
            { '\u00F8', 'o' }, // ø
            { '\u00F9', 'u' }, // ù
            { '\u00FA', 'u' }, // ú
            { '\u00FB', 'u' }, // û
            { '\u00FC', 'u' }, // ü
            { '\u2013', '-' } }; // –

    /**
     * Number of bytes between each SHOUTcast metadata block.
     */
    public static final int META_DATA_INTERVAL = 20_480;

    /**
     * The underlying output stream to decorate.
     */
    private final OutputStream out;

    /**
     * What to write in the SHOUTcast metadata is fetched from the playlist.
     */
    private final PlayQueue playQueue;

    private final String welcomeTitle;

    /**
     * Keeps track of the number of bytes written (excluding meta-data). Between 0 and {@link #META_DATA_INTERVAL}.
     */
    private int byteCount;

    /**
     * The last stream title sent.
     */
    private String previousStreamTitle;

    /**
     * Creates a new SHOUTcast-decorated stream for the given output stream.
     *
     * @param out
     *            The output stream to decorate.
     * @param playQueue
     *            Meta-data is fetched from this playlist.
     */
    public ShoutCastOutputStream(OutputStream out, PlayQueue playQueue, String welcomeTitle) {
        super();
        this.out = out;
        this.playQueue = playQueue;
        this.welcomeTitle = welcomeTitle;
    }

    /**
     * Writes the given byte array to the underlying stream, adding SHOUTcast meta-data as necessary.
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {

        int bytesWritten = 0;
        while (bytesWritten < len) {

            // 'n' is the number of bytes to write before the next potential meta-data block.
            int n = Math.min(len - bytesWritten, ShoutCastOutputStream.META_DATA_INTERVAL - byteCount);

            /*
             * False positive for cross-site scripting at LGTM.com. (Directly writing user input to a web page, without
             * properly sanitizing the input first.) In general, some podcast functions inherently require such
             * processing. In this case 'client' is the Jpsonic server, not the user.
             *
             * - Users can configure trusted podcast resources. - Users can choose not to use podcast. - Users can also
             * use virus check tool to monitor the directory where audio files are stored.
             */
            out.write(b, off + bytesWritten, n); // lgtm[java/xss]
            bytesWritten += n;
            byteCount += n;

            // Reached meta-data block?
            if (byteCount % ShoutCastOutputStream.META_DATA_INTERVAL == 0) {
                writeMetaData();
                byteCount = 0;
            }
        }
    }

    /**
     * Writes the given byte array to the underlying stream, adding SHOUTcast meta-data as necessary.
     */
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes the given byte to the underlying stream, adding SHOUTcast meta-data as necessary.
     */
    @Override
    public void write(int b) throws IOException {
        byte[] buf = { (byte) b };
        write(buf);
    }

    /**
     * Flushes the underlying stream.
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Closes the underlying stream.
     */
    @Override
    public void close() throws IOException {
        out.close();
    }

    private void writeMetaData() throws IOException {
        String streamTitle = StringUtils.trimToEmpty(welcomeTitle);

        MediaFile result;
        synchronized (playQueue) {
            result = playQueue.getCurrentFile();
        }
        MediaFile mediaFile = result;
        if (mediaFile != null) {
            streamTitle = mediaFile.getArtist() + " - " + mediaFile.getTitle();
        }

        byte[] bytes;

        if (streamTitle.equals(previousStreamTitle)) {
            bytes = new byte[0];
        } else {
            previousStreamTitle = streamTitle;
            bytes = createStreamTitle(streamTitle);
        }

        // Length in groups of 16 bytes.
        int length = bytes.length / 16;
        if (bytes.length % 16 > 0) {
            length++;
        }

        // Write the length as a single byte.
        out.write(length);

        // Write the message.
        out.write(bytes);

        // Write padding zero bytes.
        int padding = length * 16 - bytes.length;
        for (int i = 0; i < padding; i++) {
            out.write(0);
        }
    }

    private byte[] createStreamTitle(final String title) {
        // Remove any quotes from the title.
        String result = title.replaceAll("'", "");

        // Convert non-ascii characters to similar ascii characters.
        for (char[] chars : ShoutCastOutputStream.CHAR_MAP) {
            result = result.replace(chars[0], chars[1]);
        }

        result = "StreamTitle='" + result + "';";
        return result.getBytes(StandardCharsets.US_ASCII);
    }
}
