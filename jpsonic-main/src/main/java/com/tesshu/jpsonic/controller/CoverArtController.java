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

package com.tesshu.jpsonic.controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.CoverArtScheme;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.domain.logic.CoverArtLogic;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.metadata.FFmpeg;
import com.tesshu.jpsonic.service.metadata.ParserUtils;
import com.tesshu.jpsonic.util.StringUtil;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jaudiotagger.tag.images.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller which produces cover art images.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/coverArt.view", "/ext/coverArt.view" })
@SuppressWarnings("PMD.AccessorMethodGeneration") // Triaged in #834
public class CoverArtController {

    private static final Logger LOG = LoggerFactory.getLogger(CoverArtController.class);
    private static final int COVER_ART_CONCURRENCY = 4;
    private static final Object DIRS_LOCK = new Object();
    private static final Map<String, Object> IMG_LOCKS = new ConcurrentHashMap<>();

    private final MediaFileService mediaFileService;
    private final FFmpeg ffmpeg;
    private final PlaylistService playlistService;
    private final PodcastService podcastService;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final CoverArtLogic logic;
    private final FontLoader fontLoader;

    private Semaphore semaphore;

    public CoverArtController(MediaFileService mediaFileService, FFmpeg ffmpeg, PlaylistService playlistService,
            PodcastService podcastService, ArtistDao artistDao, AlbumDao albumDao, CoverArtLogic logic,
            FontLoader fontLoader) {
        super();
        this.mediaFileService = mediaFileService;
        this.ffmpeg = ffmpeg;
        this.playlistService = playlistService;
        this.podcastService = podcastService;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.logic = logic;
        this.fontLoader = fontLoader;
    }

    @PostConstruct
    public void init() {
        semaphore = new Semaphore(COVER_ART_CONCURRENCY);
    }

    /*
     * To be triaged in #1122.
     */
    public long getLastModified(HttpServletRequest request) {
        CoverArtRequest coverArtRequest = createCoverArtRequest(request);
        if (null == coverArtRequest) {
            return -1L;
        }
        return coverArtRequest.lastModified();
    }

    @GetMapping
    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException {

        CoverArtRequest coverArtRequest = createCoverArtRequest(request);
        Integer size = ServletRequestUtils.getIntParameter(request, Attributes.Request.SIZE.value());
        if (coverArtRequest == null) {
            sendFallback(size, response);
            return;
        }

        try {
            // Optimize if no scaling is required.
            if (size == null && coverArtRequest.getCoverArt() != null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("sendUnscaled - " + coverArtRequest);
                }
                sendUnscaled(coverArtRequest, response);
                return;
            }

            // Send cached image, creating it if necessary.
            if (size == null) {
                size = CoverArtScheme.LARGE.getSize() * 2;
            }
            File cachedImage = getCachedImage(coverArtRequest, size);
            sendImage(cachedImage, response);
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending fallback as an exception was encountered during normal cover art processing", e);
            }
            sendFallback(size, response);
        }
    }

    private CoverArtRequest createCoverArtRequest(HttpServletRequest request) {
        String id = request.getParameter(Attributes.Request.ID.value());
        if (id == null) {
            return null;
        }
        if (logic.isAlbum(id)) {
            return createAlbumCoverArtRequest(logic.getAlbumId(id));
        }
        if (logic.isArtist(id)) {
            return createArtistCoverArtRequest(logic.getArtistId(id));
        }
        if (logic.isPlaylist(id)) {
            return createPlaylistCoverArtRequest(logic.getPlaylistId(id));
        }
        if (logic.isPodcast(id)) {
            return createPodcastCoverArtRequest(logic.getPodcastId(id), request);
        }
        return createMediaFileCoverArtRequest(Integer.parseInt(id), request);
    }

    private CoverArtRequest createAlbumCoverArtRequest(int id) {
        Album album = albumDao.getAlbum(id);
        return album == null ? null : new AlbumCoverArtRequest(album);
    }

    private CoverArtRequest createArtistCoverArtRequest(int id) {
        Artist artist = artistDao.getArtist(id);
        return artist == null ? null : new ArtistCoverArtRequest(artist);
    }

    private PlaylistCoverArtRequest createPlaylistCoverArtRequest(int id) {
        Playlist playlist = playlistService.getPlaylist(id);
        return playlist == null ? null : new PlaylistCoverArtRequest(playlist);
    }

    private CoverArtRequest createPodcastCoverArtRequest(int id, HttpServletRequest request) {
        PodcastChannel channel = podcastService.getChannel(id);
        if (channel.getMediaFileId() == null) {
            return new PodcastCoverArtRequest(channel);
        }
        return createMediaFileCoverArtRequest(channel.getMediaFileId(), request);
    }

    private CoverArtRequest createMediaFileCoverArtRequest(int id, HttpServletRequest request) {
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        if (mediaFile == null) {
            return null;
        }
        if (mediaFile.isVideo()) {
            int offset = ServletRequestUtils.getIntParameter(request, Attributes.Request.OFFSET.value(), 0);
            return new VideoCoverArtRequest(mediaFile, offset);
        }
        return new MediaFileCoverArtRequest(mediaFile);
    }

    private void sendImage(File file, HttpServletResponse response) throws ExecutionException {
        response.setContentType(StringUtil.getMimeType(FilenameUtils.getExtension(file.getName())));
        try (InputStream in = Files.newInputStream(Paths.get(file.toURI()))) {
            IOUtils.copy(in, response.getOutputStream());
        } catch (IOException e) {
            throw new ExecutionException("Cannot copy image: " + file.getPath(), e);
        }
    }

    private void sendFallback(Integer size, HttpServletResponse response) {
        if (response.getContentType() == null) {
            response.setContentType(StringUtil.getMimeType("jpeg"));
        }
        try (InputStream in = CoverArtController.class.getResourceAsStream("default_cover.jpg");
                ServletOutputStream sos = response.getOutputStream()) {
            if (!sos.isReady()) {
                return;
            }
            BufferedImage image = ImageIO.read(in);
            if (size != null) {
                image = scale(image, size, size);
            }
            ImageIO.write(image, "jpeg", sos);
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error reading default_cover.jpg", e);
            }
        }
    }

    void sendUnscaled(CoverArtRequest coverArtRequest, HttpServletResponse response) throws ExecutionException {
        File file = coverArtRequest.getCoverArt();
        Pair<InputStream, String> imageInputStreamWithType = getImageInputStreamWithType(file);
        response.setContentType(imageInputStreamWithType.getRight());
        try (InputStream in = imageInputStreamWithType.getLeft()) {
            IOUtils.copy(in, response.getOutputStream());
        } catch (IOException e) {
            throw new ExecutionException("Cannot copy image: " + file.getPath(), e);
        }
    }

    private File getCachedImage(CoverArtRequest request, int size) throws ExecutionException {
        String encoding = request.getCoverArt() == null ? "png" : "jpeg";
        File cachedImage = new File(getImageCacheDirectory(size),
                DigestUtils.md5Hex(request.getKey()) + "." + encoding);
        String lockKey = cachedImage.getPath();

        Object lock = new Object();
        IMG_LOCKS.putIfAbsent(lockKey, lock);

        synchronized (IMG_LOCKS.get(lockKey)) {
            if (IMG_LOCKS.get(lockKey) != null && IMG_LOCKS.get(lockKey).equals(lock)
                    && (!cachedImage.exists() || request.lastModified() > cachedImage.lastModified())) {
                try (OutputStream out = Files.newOutputStream(Paths.get(cachedImage.toURI()))) {
                    semaphore.acquire();
                    BufferedImage image = request.createImage(size);
                    ImageIO.write(image, encoding, out);
                } catch (InterruptedException | IOException e) {
                    if (!cachedImage.delete() && LOG.isWarnEnabled()) {
                        LOG.warn("The cached image '{}' could not be deleted.", cachedImage.getAbsolutePath());
                    }
                    throw new ExecutionException("Failed to create thumbnail for " + request + ". ", e);
                } finally {
                    semaphore.release();
                    IMG_LOCKS.remove(lockKey, lock);
                }
            }
            return cachedImage;
        }
    }

    /**
     * Returns an input stream to the image in the given file. If the file is an audio file, the embedded album art is
     * returned.
     */
    @NonNull
    InputStream getImageInputStream(File file) throws ExecutionException {
        return getImageInputStreamWithType(file).getLeft();
    }

    /**
     * Returns an input stream to the image in the given file. If the file is an audio file, the embedded album art is
     * returned. In addition returns the mime type
     */
    @SuppressWarnings("PMD.CloseResource")
    /*
     * False positive. This method is an intermediate function used internally by createImage, sendUnscaled. The methods
     * calling this method auto-closes the resource after this method completes.
     */
    @NonNull
    Pair<InputStream, String> getImageInputStreamWithType(File file) throws ExecutionException {

        if (!ParserUtils.isArtworkApplicable(file)) {
            InputStream is;
            try {
                is = Files.newInputStream(Paths.get(file.toURI()));
            } catch (IOException e) {
                throw new ExecutionException("Image cannot be read: " + file.getPath(), e);
            }
            String mimeType = StringUtil.getMimeType(FilenameUtils.getExtension(file.getName()));
            return Pair.of(is, mimeType);
        }

        Optional<Artwork> op = ParserUtils.getEmbeddedArtwork(file);
        if (op.isEmpty()) {
            throw new ExecutionException(new IOException("Embeded image cannot be read: " + file.getPath()));
        }

        Artwork artwork = op.get();
        return Pair.of(new ByteArrayInputStream(artwork.getBinaryData()), artwork.getMimeType());
    }

    @Nullable
    BufferedImage getImageInputStreamForVideo(MediaFile mediaFile, int width, int height, int offset) {
        return ffmpeg.createImage(mediaFile.getFile(), width, height, offset);
    }

    private File getImageCacheDirectory(int size) {
        File dir = new File(SettingsService.getJpsonicHome(), "thumbs");
        dir = new File(dir, String.valueOf(size));
        if (!dir.exists()) {
            synchronized (DIRS_LOCK) {
                if (dir.mkdirs()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Created thumbnail cache " + dir);
                    }
                } else {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to create thumbnail cache " + dir);
                    }
                }
            }
        }
        return dir;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (BufferedImage) Not reusable
    public static BufferedImage scale(BufferedImage image, int width, int height) {

        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage thumb = image;

        // For optimal results, use step by step bilinear resampling - halfing the size at each step.
        do {
            w /= 2;
            h /= 2;
            if (w < width) {
                w = width;
            }
            if (h < height) {
                h = height;
            }

            BufferedImage temp = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = temp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(thumb, 0, 0, temp.getWidth(), temp.getHeight(), null);
            g2.dispose();

            thumb = temp;
        } while (w != width);

        return thumb;
    }

    private abstract class CoverArtRequest {

        protected File coverArt;

        private CoverArtRequest() {
        }

        private CoverArtRequest(String coverArtPath) {
            this.coverArt = coverArtPath == null ? null : new File(coverArtPath);
        }

        private File getCoverArt() {
            return coverArt;
        }

        public abstract String getKey();

        public abstract long lastModified();

        @SuppressWarnings("PMD.GuardLogStatement")
        public BufferedImage createImage(int size) {
            if (coverArt != null) {
                try (InputStream in = getImageInputStream(coverArt)) {

                    BufferedImage image = ImageIO.read(in);
                    if (image == null) {
                        LOG.warn("Empty Image? :" + coverArt);
                    } else {
                        return scale(image, size, size);
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to process cover art " + coverArt + ": ", e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof IOException) {
                        LOG.warn("Empty embeded image or Non-existent file? :" + coverArt + " ", e.getMessage());
                    } else {
                        LOG.warn("Failed to process cover art " + coverArt + ": ");
                        ConcurrentUtils.handleCauseUnchecked(e);
                    }
                }
            }
            return createAutoCover(size, size);
        }

        protected BufferedImage createAutoCover(int width, int height) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            float fontSize = (height - height * 0.02f) * 0.1f;
            AutoCover autoCover = new AutoCover(graphics, getKey(), getArtist(), getAlbum(), width, height,
                    fontLoader.getFont(fontSize));
            autoCover.paintCover();
            graphics.dispose();
            return image;
        }

        public abstract String getAlbum();

        public abstract String getArtist();
    }

    private class ArtistCoverArtRequest extends CoverArtRequest {

        private final Artist artist;

        ArtistCoverArtRequest(Artist artist) {
            super(artist.getCoverArtPath());
            this.artist = artist;
        }

        @Override
        public String getKey() {
            return artist.getCoverArtPath() == null ? logic.createKey(artist) : artist.getCoverArtPath();
        }

        @Override
        public long lastModified() {
            return coverArt == null ? artist.getLastScanned().getTime() : coverArt.lastModified();
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return artist.getName();
        }
    }

    private class AlbumCoverArtRequest extends CoverArtRequest {

        private final Album album;

        AlbumCoverArtRequest(Album album) {
            super(album.getCoverArtPath());
            this.album = album;
        }

        @Override
        public String getKey() {
            return album.getCoverArtPath() == null ? logic.createKey(album) : album.getCoverArtPath();
        }

        @Override
        public long lastModified() {
            return coverArt == null ? album.getLastScanned().getTime() : coverArt.lastModified();
        }

        @Override
        public String getAlbum() {
            return album.getName();
        }

        @Override
        public String getArtist() {
            return album.getArtist();
        }
    }

    private class PlaylistCoverArtRequest extends CoverArtRequest {

        private static final int IMAGE_COMPOSITES_THRESHOLD = 4;

        private final Playlist playlist;

        PlaylistCoverArtRequest(Playlist playlist) {
            super(null);
            this.playlist = playlist;
        }

        @Override
        public String getKey() {
            return logic.createKey(playlist);
        }

        @Override
        public long lastModified() {
            return playlist.getChanged().getTime();
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return playlist.getName();
        }

        @Override
        public BufferedImage createImage(int size) {
            List<MediaFile> albums = getRepresentativeAlbums();
            if (albums.isEmpty()) {
                return createAutoCover(size, size);
            }
            if (albums.size() < IMAGE_COMPOSITES_THRESHOLD) {
                return new MediaFileCoverArtRequest(albums.get(0)).createImage(size);
            }

            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            int half = size / 2;
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(0)).createImage(half), null, 0, 0);
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(1)).createImage(half), null, half, 0);
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(2)).createImage(half), null, 0, half);
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(3)).createImage(half), null, half, half);
            graphics.dispose();
            return image;
        }

        private List<MediaFile> getRepresentativeAlbums() {
            Set<MediaFile> albums = new LinkedHashSet<>();
            for (MediaFile song : playlistService.getFilesInPlaylist(playlist.getId())) {
                MediaFile album = mediaFileService.getParentOf(song);
                if (album != null && !mediaFileService.isRoot(album)) {
                    albums.add(album);
                }
            }
            return new ArrayList<>(albums);
        }
    }

    private class PodcastCoverArtRequest extends CoverArtRequest {

        private final PodcastChannel channel;

        PodcastCoverArtRequest(PodcastChannel channel) {
            super(null);
            this.channel = channel;
        }

        @Override
        public String getKey() {
            return logic.createKey(channel);
        }

        @Override
        public long lastModified() {
            return -1;
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return channel.getTitle() == null ? channel.getUrl() : channel.getTitle();
        }
    }

    class MediaFileCoverArtRequest extends CoverArtRequest {

        private final MediaFile dir;

        MediaFileCoverArtRequest(MediaFile mediaFile) {
            super(mediaFile.getCoverArtPath());
            dir = mediaFile.isDirectory() ? mediaFile : mediaFileService.getParentOf(mediaFile);
            coverArt = mediaFileService.getCoverArt(mediaFile);
        }

        @Override
        public String getKey() {
            return coverArt == null ? dir.getPath() : coverArt.getPath();
        }

        @Override
        public long lastModified() {
            return coverArt == null ? dir.getChanged().getTime() : coverArt.lastModified();
        }

        @Override
        public String getAlbum() {
            return dir.getName();
        }

        @Override
        public String getArtist() {
            return dir.getAlbumArtist() == null ? dir.getArtist() : dir.getAlbumArtist();
        }
    }

    private class VideoCoverArtRequest extends CoverArtRequest {

        private final MediaFile mediaFile;
        private final int offset;

        VideoCoverArtRequest(MediaFile mediaFile, int offset) {
            super(mediaFile.getCoverArtPath());
            this.mediaFile = mediaFile;
            this.offset = offset;
        }

        @Override
        public BufferedImage createImage(int size) {
            int height = size;
            int width = height * 16 / 9;

            BufferedImage result = getImageInputStreamForVideo(mediaFile, width, height, offset);
            if (result != null) {
                return result;
            }

            if (LOG.isWarnEnabled()) {
                LOG.warn("Unable to create video thumbnails : " + mediaFile);
            }
            return createAutoCover(width, height);
        }

        @Override
        public String getKey() {
            return mediaFile.getPath() + "/" + offset;
        }

        @Override
        public long lastModified() {
            return mediaFile.getChanged().getTime();
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return mediaFile.getName();
        }
    }

    static class AutoCover {

        private static final int[] COLORS = { 0x33B5E5, 0xAA66CC, 0x99CC00, 0xFFBB33, 0xFF4444 };
        private final Graphics2D graphics;
        private final String artist;
        private final String album;
        private final int width;
        private final int height;
        private final Color color;
        private final Font font;

        AutoCover(Graphics2D graphics, String key, String artist, String album, int width, int height, Font font) {
            this.graphics = graphics;
            this.artist = artist;
            this.album = album;
            this.width = width;
            this.height = height;
            this.font = font;

            int hash = key.hashCode();
            hash = hash < 0 ? Math.abs(hash) : hash;
            int rgb = COLORS[hash % COLORS.length];
            this.color = new Color(rgb);
        }

        public void paintCover() {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            graphics.setPaint(color);
            graphics.fillRect(0, 0, width, height);

            int y = height * 2 / 3;
            graphics.setPaint(new GradientPaint(0, y, new Color(82, 82, 82), 0, height, Color.BLACK));
            graphics.fillRect(0, y, width, height / 3);

            graphics.setPaint(Color.WHITE);
            graphics.setFont(font);
            if (album != null) {
                graphics.drawString(album, width * 0.05f, height * 0.6f);
            }
            if (artist != null) {
                graphics.drawString(artist, width * 0.05f, height * 0.8f);
            }

            int borderWidth = height / 50;
            graphics.fillRect(0, 0, borderWidth, height);
            graphics.fillRect(width - borderWidth, 0, height - borderWidth, height);
            graphics.fillRect(0, 0, width, borderWidth);
            graphics.fillRect(0, height - borderWidth, width, height);
        }
    }
}
