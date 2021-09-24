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

package com.tesshu.jpsonic.service;

import static org.springframework.web.bind.ServletRequestUtils.getIntParameter;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.dao.PlayerDao;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.domain.TransferStatus;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.security.JWTAuthenticationToken;
import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.ServletRequestBindingException;

/**
 * Provides services for maintaining the set of players.
 *
 * @author Sindre Mehus
 * 
 * @see Player
 */
@Service
@DependsOn("liquibase")
public class PlayerService {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerService.class);

    private static final Object LOCK = new Object();
    private static final String COOKIE_NAME = "player";
    private static final String ATTRIBUTE_SESSION_KEY = "player";
    private static final String GUEST_PLAYER_TYPE = "UPnP Processor";
    private static final int COOKIE_EXPIRY = 365 * 24 * 3600; // One year

    private final PlayerDao playerDao;
    private final StatusService statusService;
    private final SecurityService securityService;
    private final TranscodingService transcodingService;

    public PlayerService(PlayerDao playerDao, StatusService statusService, SecurityService securityService,
            TranscodingService transcodingService) {
        super();
        this.playerDao = playerDao;
        this.statusService = statusService;
        this.securityService = securityService;
        this.transcodingService = transcodingService;
    }

    @PostConstruct
    public void init() {
        playerDao.deleteOldPlayers(60);
    }

    /**
     * Equivalent to <code>getPlayer(request, response, true)</code> .
     */
    public Player getPlayer(HttpServletRequest request, HttpServletResponse response) {
        return getPlayer(request, response, true, false);
    }

    /**
     * Returns the player associated with the given HTTP request. If no such player exists, a new one is created.
     *
     * @param request
     *            The HTTP request.
     * @param response
     *            The HTTP response.
     * @param remoteControlEnabled
     *            Whether this method should return a remote-controlled player.
     * @param isStreamRequest
     *            Whether the HTTP request is a request for streaming data.
     * 
     * @return The player associated with the given HTTP request.
     */
    public Player getPlayer(HttpServletRequest request, HttpServletResponse response, boolean remoteControlEnabled,
            boolean isStreamRequest) {

        synchronized (LOCK) {

            // Get player.
            Player player = findOrCreatePlayer(request, remoteControlEnabled);

            // Check if player data should be updated.
            boolean isUpdate = isToBeUpdate(request, isStreamRequest, player);

            // Update player data.
            if (isUpdate) {
                updatePlayer(player);
            }

            // Set cookie in response.
            if (response != null) {
                String username = securityService.getCurrentUsername(request);
                String cookieName = COOKIE_NAME + "-" + StringUtil.utf8HexEncode(username);
                Cookie cookie = new Cookie(cookieName, String.valueOf(player.getId()));
                cookie.setMaxAge(COOKIE_EXPIRY);
                cookie.setHttpOnly(true);
                String path = request.getContextPath();
                if (StringUtils.isEmpty(path)) {
                    path = "/";
                }
                cookie.setPath(path);
                cookie.setSecure(true);
                response.addCookie(cookie);
            }

            // Save player in session context.
            if (remoteControlEnabled) {
                request.getSession().setAttribute(Attributes.Session.PLAYER.value(), player.getId());
            }

            return player;
        }
    }

    private Player findOrCreatePlayer(HttpServletRequest request, boolean remoteControlEnabled) {
        String username = securityService.getCurrentUsername(request);
        Player player = findPlayer(request, remoteControlEnabled, username);

        // Look for player with same IP address and user name.
        if (player == null) {
            player = getNonRestPlayerByIpAddressAndUsername(request.getRemoteAddr(), username);
        }

        // If no player was found, create it.
        if (player == null) {
            player = new Player();
            player.setUsername(username);
            createPlayer(player);
        }
        return player;
    }

    private @Nullable Player findPlayer(HttpServletRequest request, boolean remoteControlEnabled, String username) {
        // Find by 'player' request parameter.
        Player player = null;
        try {
            player = getPlayerById(getIntParameter(request, Attributes.Request.PLAYER.value()));
        } catch (ServletRequestBindingException e) {
            warn("An unrecoverable error occurred while searching for the player.", e);
        }

        // Find in session context.
        if (player == null && remoteControlEnabled) {
            player = findPlayerInSession(request);
        }

        // Find by cookie.
        if (player == null && remoteControlEnabled) {
            player = getPlayerById(getPlayerIdFromCookie(request, username));
        }

        // Make sure we're not hijacking the player of another user.
        if (player != null && player.getUsername() != null && username != null
                && !player.getUsername().equals(username)) {
            return null;
        }
        return player;
    }

    private void warn(String m, Throwable t) {
        if (LOG.isWarnEnabled()) {
            LOG.warn(m, t);
        }
    }

    @SuppressFBWarnings(value = "UC_USELESS_CONDITION", justification = "False positive. The value of isStreamRequest is not always false.")
    private boolean isToBeUpdate(HttpServletRequest request, boolean isStreamRequest, Player player) {
        boolean isToBeUpdate = false;
        String username = securityService.getCurrentUsername(request);
        if (username != null && player.getUsername() == null) {
            player.setUsername(username);
            isToBeUpdate = true;
        }
        if (player.getIpAddress() == null || isStreamRequest || !isPlayerConnected(player) && player.isDynamicIp()
                && !request.getRemoteAddr().equals(player.getIpAddress())) {
            player.setIpAddress(request.getRemoteAddr());
            isToBeUpdate = true;
        }
        String userAgent = request.getHeader("user-agent");
        if (isStreamRequest) {
            player.setType(userAgent);
            player.setLastSeen(new Date());
            isToBeUpdate = true;
        }
        return isToBeUpdate;
    }

    private @Nullable Player findPlayerInSession(HttpServletRequest request) {
        Integer playerId = (Integer) request.getSession().getAttribute(ATTRIBUTE_SESSION_KEY);
        if (playerId != null) {
            return getPlayerById(playerId);
        }
        return null;
    }

    /**
     * Updates the given player.
     *
     * @param player
     *            The player to update.
     */
    public void updatePlayer(Player player) {
        playerDao.updatePlayer(player);
    }

    /**
     * Returns the player with the given ID.
     *
     * @param id
     *            The unique player ID.
     * 
     * @return The player with the given ID, or <code>null</code> if no such player exists.
     */
    public Player getPlayerById(Integer id) {
        if (id == null) {
            return null;
        } else {
            return playerDao.getPlayerById(id);
        }
    }

    /**
     * Returns whether the given player is connected.
     *
     * @param player
     *            The player in question.
     * 
     * @return Whether the player is connected.
     */
    private boolean isPlayerConnected(Player player) {
        for (TransferStatus status : statusService.getStreamStatusesForPlayer(player)) {
            if (status.isActive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the (non-REST) player with the given IP address and username. If no username is given, only IP address is
     * used as search criteria.
     *
     * @param ipAddress
     *            The IP address.
     * @param username
     *            The remote user.
     * 
     * @return The player with the given IP address, or <code>null</code> if no such player exists.
     */
    private Player getNonRestPlayerByIpAddressAndUsername(final String ipAddress, final String username) {
        if (ipAddress == null) {
            return null;
        }
        for (Player player : getAllPlayers()) {
            boolean isRest = player.getClientId() != null;
            boolean ipMatches = ipAddress.equals(player.getIpAddress());
            boolean userMatches = username == null || username.equals(player.getUsername());
            if (!isRest && ipMatches && userMatches) {
                return player;
            }
        }
        return null;
    }

    /**
     * Reads the player ID from the cookie in the HTTP request.
     *
     * @param request
     *            The HTTP request.
     * @param username
     *            The name of the current user.
     * 
     * @return The player ID embedded in the cookie, or <code>null</code> if cookie is not present.
     */
    private Integer getPlayerIdFromCookie(HttpServletRequest request, String username) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String cookieName = COOKIE_NAME + "-" + StringUtil.utf8HexEncode(username);
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                try {
                    return Integer.valueOf(cookie.getValue());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Returns all players owned by the given username and client ID.
     *
     * @param username
     *            The name of the user.
     * @param clientId
     *            The third-party client ID (used if this player is managed over the Airsonic REST API). May be
     *            <code>null</code>.
     * 
     * @return All relevant players.
     */
    public List<Player> getPlayersForUserAndClientId(String username, String clientId) {
        return playerDao.getPlayersForUserAndClientId(username, clientId);
    }

    /**
     * Returns all currently registered players.
     *
     * @return All currently registered players.
     */
    public List<Player> getAllPlayers() {
        return playerDao.getAllPlayers();
    }

    /**
     * Removes the player with the given ID.
     *
     * @param id
     *            The unique player ID.
     */
    public void removePlayerById(int id) {
        synchronized (LOCK) {
            playerDao.deletePlayer(id);
        }
    }

    /**
     * Creates and returns a clone of the given player.
     *
     * @param playerId
     *            The ID of the player to clone.
     * 
     * @return The cloned player.
     */
    public Player clonePlayer(int playerId) {
        Player player = getPlayerById(playerId);
        if (player.getName() != null) {
            player.setName(player.getName() + " (copy)");
        }

        createPlayer(player);
        return player;
    }

    /**
     * Creates the given player, and activates all transcodings.
     *
     * @param player
     *            The player to create.
     */
    public void createPlayer(Player player) {
        createPlayer(player, true);
    }

    private void createPlayer(Player player, boolean isInitTranscoding) {
        UserSettings userSettings = securityService
                .getUserSettings(JWTAuthenticationToken.USERNAME_ANONYMOUS.equals(player.getUsername())
                        ? User.USERNAME_GUEST : player.getUsername());
        player.setTranscodeScheme(userSettings.getTranscodeScheme());
        playerDao.createPlayer(player);
        if (isInitTranscoding) {
            transcodingService.setTranscodingsForPlayer(player, transcodingService.getAllTranscodings().stream()
                    .filter(Transcoding::isDefaultActive).collect(Collectors.toList()));
        }
    }

    /**
     * Returns a player associated to the special "guest" user, creating it if necessary.
     */
    public Player getGuestPlayer(HttpServletRequest request) {

        User user = securityService.getGuestUser();
        Date now = new Date();

        // Look for existing player.
        List<Player> players = getPlayersForUserAndClientId(user.getUsername(), null);

        Optional<Player> oldPlayer = request == null
                ? players.stream().filter(p -> p.getIpAddress() == null).findFirst()
                : players.stream()
                        .filter(p -> p.getIpAddress() != null && p.getIpAddress().equals(request.getRemoteAddr()))
                        .findFirst();

        if (oldPlayer.isPresent()) {
            // Update date only if more than 24 hours have passed
            Player player = oldPlayer.get();
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(player.getLastSeen());
            if (now.getTime() - player.getLastSeen().getTime() > 1000 * 60 * 60 * 24) {
                player.setLastSeen(now);
                playerDao.updatePlayer(player);
            }
            return player;
        }

        // Create player if necessary.
        Player player = new Player();
        if (request != null) {
            player.setIpAddress(request.getRemoteAddr());
        }
        player.setUsername(user.getUsername());
        player.setType(GUEST_PLAYER_TYPE);
        player.setLastSeen(new Date());
        createPlayer(player, false);

        return player;
    }
}
