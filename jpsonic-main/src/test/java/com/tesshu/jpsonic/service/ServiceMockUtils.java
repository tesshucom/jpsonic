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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service;

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.feature.auth.jwt.JWTAuthenticationToken;
import com.tesshu.jpsonic.persistence.api.entity.Player;
import com.tesshu.jpsonic.persistence.api.entity.Transcoding;
import com.tesshu.jpsonic.persistence.api.repository.TranscodingDao;
import com.tesshu.jpsonic.persistence.core.entity.User;
import com.tesshu.jpsonic.persistence.core.entity.UserSettings;
import jakarta.servlet.http.HttpServletRequest;
import org.mockito.Mockito;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public final class ServiceMockUtils {

    public static final String ADMIN_NAME = "admin";

    private static final List<Transcoding> DEFAULT_TRANSCODINGS = Arrays
        .asList(new Transcoding(0, "mp3 audio",
                "mp3 ogg oga aac m4a flac wav wma aif aiff ape mpc shn", "mp3",
                "ffmpeg -i %s -map 0:0 -b:a %bk -v 0 -f mp3 -", null, null, true),
                new Transcoding(1, "flv/h264 video",
                        "avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts", "flv",
                        "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -",
                        null, null, true),
                new Transcoding(2, "mkv video", "avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts",
                        "mkv",
                        "ffmpeg -ss %o -i %s -c:v libx264 -preset superfast -b:v %bk -c:a libvorbis -f matroska -threads 0 -",
                        null, null, true),
                new Transcoding(3, "mp4/h264 video",
                        "avi flv mpg mpeg m4v mkv mov wmv ogv divx m2ts", "mp4",
                        "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f mp4 -vcodec libx264 -preset superfast -threads 0 -movflags frag_keyframe+empty_moov -",
                        null, null, true));

    private ServiceMockUtils() {

    }

    @SuppressWarnings("unchecked")
    public static <T> T mock(Class<T> classToMock) {
        Object mock;
        if (UserService.class == classToMock) {
            UserService userService = Mockito.mock(UserService.class);
            Mockito
                .when(userService
                    .getCurrentUsernameStrict(Mockito.nullable(HttpServletRequest.class)))
                .thenReturn(ADMIN_NAME);
            Mockito
                .when(userService.getCurrentUserStrict(Mockito.nullable(HttpServletRequest.class)))
                .thenReturn(new User(ADMIN_NAME, ADMIN_NAME, ""));
            Mockito
                .when(userService.getUserSettings(ADMIN_NAME))
                .thenReturn(new UserSettings(ADMIN_NAME));

            User guestUser = new User(User.USERNAME_GUEST, User.USERNAME_GUEST, "");
            guestUser.setStreamRole(true);
            Mockito.when(userService.getGuestUser()).thenReturn(guestUser);
            Mockito
                .when(userService.getUserSettings(User.USERNAME_GUEST))
                .thenReturn(new UserSettings(User.USERNAME_GUEST));

            Mockito
                .when(userService.getUserSettings(JWTAuthenticationToken.USERNAME_ANONYMOUS))
                .thenReturn(new UserSettings(JWTAuthenticationToken.USERNAME_ANONYMOUS));

            mock = userService;
        } else if (PlayerService.class == classToMock) {
            PlayerService playerService = Mockito.mock(PlayerService.class);
            Player player = new Player();
            player.setId(99);
            player.setUsername(User.USERNAME_GUEST);
            Mockito
                .when(playerService.getGuestPlayer(Mockito.nullable(HttpServletRequest.class)))
                .thenReturn(player);
            Player upnpPlayer = new Player();
            upnpPlayer.setId(999);
            upnpPlayer.setUsername(User.USERNAME_GUEST);
            upnpPlayer.setClientId("Jpsonic UPnP Player");
            Mockito.when(playerService.getUPnPPlayer()).thenReturn(upnpPlayer);
            mock = playerService;
        } else if (TranscodingDao.class == classToMock) {
            TranscodingDao transcodingDao = Mockito.mock(TranscodingDao.class);
            Mockito.when(transcodingDao.getAllTranscodings()).thenReturn(DEFAULT_TRANSCODINGS);
            mock = transcodingDao;
        } else {
            mock = Mockito.mock(classToMock);
        }
        return (T) mock;
    }

    /**
     * Non-asynchronous executor, mainly used for testing scans. JUnit forks the
     * execution thread to its own worker thread to continue processing, which is
     * incompatible with some threading logic. There are some workarounds, but this
     * is the simplest. (However, asynchronous operation check needs to be done
     * separately)
     */
    public static ThreadPoolTaskExecutor mockNoAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = Mockito.mock(ThreadPoolTaskExecutor.class);
        Mockito.doAnswer((invocation) -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(executor).execute(Mockito.any(Runnable.class));
        return executor;
    }
}
