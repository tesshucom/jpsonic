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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import com.tesshu.jpsonic.controller.CoverArtController;
import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.dao.AvatarDao;
import com.tesshu.jpsonic.domain.Avatar;
import com.tesshu.jpsonic.domain.AvatarScheme;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AvatarService {

    private static final Logger LOG = LoggerFactory.getLogger(AvatarService.class);

    private static final int MAX_AVATAR_SIZE = 64;

    private final AvatarDao avatarDao;

    public AvatarService(AvatarDao avatarDao) {
        this.avatarDao = avatarDao;
    }

    /**
     * Returns all system avatars.
     *
     * @return All system avatars.
     */
    public List<Avatar> getAllSystemAvatars() {
        return avatarDao.getAllSystemAvatars();
    }

    /**
     * Returns the system avatar with the given ID.
     *
     * @param id
     *            The system avatar ID.
     * 
     * @return The avatar or <code>null</code> if not found.
     */
    public Avatar getSystemAvatar(int id) {
        return avatarDao.getSystemAvatar(id);
    }

    /**
     * Returns the custom avatar for the given user.
     *
     * @param username
     *            The username.
     * 
     * @return The avatar or <code>null</code> if not found.
     */
    public Avatar getCustomAvatar(String username) {
        return avatarDao.getCustomAvatar(username);
    }

    /**
     * Sets the custom avatar for the given user.
     *
     * @param avatar
     *            The avatar, or <code>null</code> to remove the avatar.
     * @param username
     *            The username.
     */
    public void setCustomAvatar(Avatar avatar, String username) {
        avatarDao.setCustomAvatar(avatar, username);
    }

    public String createAvatarUrl(@NonNull String baseUrlString, @NonNull UserSettings userSettings) {
        String avatarUrl = null;
        if (userSettings.getAvatarScheme() == AvatarScheme.SYSTEM) {
            avatarUrl = baseUrlString + ViewName.AVATAR.value() + "?id=" + userSettings.getSystemAvatarId();
        } else if (userSettings.getAvatarScheme() == AvatarScheme.CUSTOM
                && getCustomAvatar(userSettings.getUsername()) != null) {
            avatarUrl = baseUrlString + ViewName.AVATAR.value() + "?usernameUtf8Hex="
                    + StringUtil.utf8HexEncode(userSettings.getUsername());
        }
        return avatarUrl;
    }

    public boolean createAvatar(@NonNull String fileName, @NonNull InputStream inputStream, long size, String username)
            throws IOException {
        BufferedImage image;
        image = ImageIO.read(new ByteArrayInputStream(IOUtils.toByteArray(inputStream)));
        if (image == null) {
            throw new IOException("Failed to decode incoming image: " + fileName + " (" + size + " bytes).");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        String mimeType = StringUtil.getMimeType(FilenameUtils.getExtension(fileName));

        boolean resized = false;
        byte[] imageData = new byte[0];
        // Scale down image if necessary.
        if (width > MAX_AVATAR_SIZE || height > MAX_AVATAR_SIZE) {
            double scaleFactor = MAX_AVATAR_SIZE / (double) Math.max(width, height);
            height = (int) (height * scaleFactor);
            width = (int) (width * scaleFactor);
            image = CoverArtController.scale(image, width, height);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", out);
            imageData = out.toByteArray();
            mimeType = StringUtil.getMimeType("jpeg");
            resized = true;
        }
        Avatar avatar = new Avatar(0, fileName, new Date(), mimeType, width, height, imageData);
        setCustomAvatar(avatar, username);
        if (LOG.isInfoEnabled()) {
            LOG.info("Created avatar '" + fileName + "' (" + imageData.length + " bytes) for user " + username);
        }
        return resized;
    }
}
