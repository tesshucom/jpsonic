package org.airsonic.player.service.upnp.processor;

import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.seamless.util.MimeType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class UpnpProcessorUtil {

    private final JpsonicComparators comparators;

    private final JWTSecurityService securityService;

    private final SettingsService settingsService;

    private final TranscodingService transcodingService;
    
    private final MusicFolderDao musicFolderDao;

    private static ResourceBundle resourceBundle;

    private static AtomicBoolean resourceLoaded = new AtomicBoolean();

    private static Object lock = new Object();

    public UpnpProcessorUtil(JpsonicComparators c, JWTSecurityService jwt, SettingsService ss, TranscodingService ts, MusicFolderDao md) {
        settingsService = ss;
        securityService = jwt;
        comparators = c;
        musicFolderDao = md;
        transcodingService = ts;
    }

    public UriComponentsBuilder addJWTToken(UriComponentsBuilder builder) {
        return securityService.addJWTToken(builder);
    }

    public String createURIStringWithToken(UriComponentsBuilder builder) {
        return addJWTToken(builder).toUriString();
    }

    public URI createURIWithToken(UriComponentsBuilder builder) {
        return addJWTToken(builder).build().encode().toUri();
    }

    public List<MusicFolder> getAllMusicFolders() {
        if (settingsService.isDlnaGuestPublish()) {
            return musicFolderDao.getMusicFoldersForUser(User.USERNAME_GUEST);
        }
        return settingsService.getAllMusicFolders();
    }

    public String getBaseUrl() {
        String dlnaBaseLANURL = settingsService.getDlnaBaseLANURL();
        if (StringUtils.isBlank(dlnaBaseLANURL)) {
            throw new IllegalArgumentException("DLNA Base LAN URL is not set correctly");
        }
        return dlnaBaseLANURL;
    }

    public MimeType getMimeType(MediaFile song, Player player) {
        String suffix = song.isVideo() ? FilenameUtils.getExtension(song.getPath()) : transcodingService.getSuffix(player, song, null);
        String mimeTypeString = StringUtil.getMimeType(suffix);
        return mimeTypeString == null ? null : MimeType.valueOf(mimeTypeString);
    }

    public String getResource(String key) {
        if (!resourceLoaded.get()) {
            synchronized (lock) {
                resourceBundle = ResourceBundle.getBundle("org.airsonic.player.i18n.ResourceBundle",
                        settingsService.getLocale());
                resourceLoaded.set(true);
            }
        }
        return resourceBundle.getString(key);
    }

    public boolean isDlnaGenreCountVisible() {
        return settingsService.isDlnaGenreCountVisible();
    }

    public boolean isProhibitSortVarious() {
        return settingsService.isProhibitSortVarious();
    }

    public boolean isSortAlbumsByYear() {
        return settingsService.isSortAlbumsByYear();
    }

    public boolean isSortAlbumsByYear(String artist) {
        return comparators.isSortAlbumsByYear(artist);
    }

}
