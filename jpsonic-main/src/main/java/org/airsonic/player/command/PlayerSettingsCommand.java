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

package org.airsonic.player.command;

import java.util.Date;
import java.util.List;

import org.airsonic.player.controller.PlayerSettingsController;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.airsonic.player.domain.TranscodeScheme;
import org.airsonic.player.domain.Transcoding;

/**
 * Command used in {@link PlayerSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PlayerSettingsCommand {

    private Integer playerId;
    private String name;
    private String description;
    private String type;
    private Date lastSeen;
    private boolean dynamicIp;
    private boolean autoControlEnabled;
    private boolean m3uBomEnabled;
    private String technologyName;
    private String transcodeSchemeName;
    private boolean transcodingSupported;
    private String transcodeDirectory;
    private List<Transcoding> allTranscodings;
    private int[] activeTranscodingIds;
    private EnumHolder[] technologyHolders;
    private EnumHolder[] transcodeSchemeHolders;
    private Player[] players;
    private boolean admin;
    private String javaJukeboxMixer;
    private String[] javaJukeboxMixers;
    private boolean openDetailSetting;
    private boolean useRadio;
    private boolean useSonos;
    private boolean showToast;

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isDynamicIp() {
        return dynamicIp;
    }

    public void setDynamicIp(boolean dynamicIp) {
        this.dynamicIp = dynamicIp;
    }

    public boolean isAutoControlEnabled() {
        return autoControlEnabled;
    }

    public void setAutoControlEnabled(boolean autoControlEnabled) {
        this.autoControlEnabled = autoControlEnabled;
    }

    public boolean isM3uBomEnabled() {
        return m3uBomEnabled;
    }

    public void setM3uBomEnabled(boolean m3uBomEnabled) {
        this.m3uBomEnabled = m3uBomEnabled;
    }

    public String getTranscodeSchemeName() {
        return transcodeSchemeName;
    }

    public void setTranscodeSchemeName(String transcodeSchemeName) {
        this.transcodeSchemeName = transcodeSchemeName;
    }

    public boolean isTranscodingSupported() {
        return transcodingSupported;
    }

    public void setTranscodingSupported(boolean transcodingSupported) {
        this.transcodingSupported = transcodingSupported;
    }

    public String getTranscodeDirectory() {
        return transcodeDirectory;
    }

    public void setTranscodeDirectory(String transcodeDirectory) {
        this.transcodeDirectory = transcodeDirectory;
    }

    public List<Transcoding> getAllTranscodings() {
        return allTranscodings;
    }

    public void setAllTranscodings(List<Transcoding> allTranscodings) {
        this.allTranscodings = allTranscodings;
    }

    public int[] getActiveTranscodingIds() {
        return activeTranscodingIds;
    }

    public void setActiveTranscodingIds(int... activeTranscodingIds) {
        if (activeTranscodingIds != null) {
            this.activeTranscodingIds = activeTranscodingIds.clone();
        }
    }

    public EnumHolder[] getTechnologyHolders() {
        return technologyHolders;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (EnumHolder) Not reusable -> #832
    public void setTechnologies(PlayerTechnology... technologies) {
        technologyHolders = new EnumHolder[technologies.length];
        for (int i = 0; i < technologies.length; i++) {
            PlayerTechnology technology = technologies[i];
            technologyHolders[i] = new EnumHolder(technology.name(), technology.toString());
        }
    }

    public EnumHolder[] getTranscodeSchemeHolders() {
        return transcodeSchemeHolders;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (EnumHolder) Not reusable -> #832
    public void setTranscodeSchemes(TranscodeScheme... transcodeSchemes) {
        transcodeSchemeHolders = new EnumHolder[transcodeSchemes.length];
        for (int i = 0; i < transcodeSchemes.length; i++) {
            TranscodeScheme scheme = transcodeSchemes[i];
            transcodeSchemeHolders[i] = new EnumHolder(scheme.name(), scheme.toString());
        }
    }

    public String getTechnologyName() {
        return technologyName;
    }

    public void setTechnologyName(String technologyName) {
        this.technologyName = technologyName;
    }

    public Player[] getPlayers() {
        return players;
    }

    public void setPlayers(Player... players) {
        if (players != null) {
            this.players = players.clone();
        }
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public String getJavaJukeboxMixer() {
        return javaJukeboxMixer;
    }

    public void setJavaJukeboxMixer(String javaJukeboxMixer) {
        this.javaJukeboxMixer = javaJukeboxMixer;
    }

    public String[] getJavaJukeboxMixers() {
        return javaJukeboxMixers;
    }

    public void setJavaJukeboxMixers(String... javaJukeboxMixers) {
        if (javaJukeboxMixers != null) {
            this.javaJukeboxMixers = javaJukeboxMixers.clone();
        }
    }

    public boolean isOpenDetailSetting() {
        return openDetailSetting;
    }

    public void setOpenDetailSetting(boolean openDetailSetting) {
        this.openDetailSetting = openDetailSetting;
    }

    public boolean isUseRadio() {
        return useRadio;
    }

    public void setUseRadio(boolean useRadio) {
        this.useRadio = useRadio;
    }

    public boolean isUseSonos() {
        return useSonos;
    }

    public void setUseSonos(boolean useSonos) {
        this.useSonos = useSonos;
    }

    public boolean isShowToast() {
        return showToast;
    }

    public void setShowToast(boolean showToast) {
        this.showToast = showToast;
    }

    /**
     * Holds the transcoding and whether it is active for the given player.
     */
    public static class TranscodingHolder {
        private final Transcoding transcoding;
        private final boolean active;

        public TranscodingHolder(Transcoding transcoding, boolean isActive) {
            this.transcoding = transcoding;
            this.active = isActive;
        }

        public Transcoding getTranscoding() {
            return transcoding;
        }

        public boolean isActive() {
            return active;
        }
    }
}
