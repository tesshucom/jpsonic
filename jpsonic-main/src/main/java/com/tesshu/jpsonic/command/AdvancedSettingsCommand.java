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

package com.tesshu.jpsonic.command;

import com.tesshu.jpsonic.controller.AdvancedSettingsController;
import com.tesshu.jpsonic.domain.system.IndexScheme;

/**
 * Command used in {@link AdvancedSettingsController}.
 *
 * @author Sindre Mehus
 */
public class AdvancedSettingsCommand extends SettingsPageCommons {

    // Bandwidth control
    private String downloadLimit;
    private String uploadLimit;
    private String bufferSize;

    // Email notification
    private String smtpFrom;
    private String smtpServer;
    private String smtpPort;
    private String smtpEncryption;
    private String smtpUser;
    private String smtpPassword;

    // LDAP authentication
    private boolean ldapEnabled;
    private String ldapUrl;
    private String ldapSearchFilter;
    private String ldapManagerDn;
    private String ldapManagerPassword;
    private boolean ldapAutoShadowing;
    private String brand;

    // Account recovery assistant
    private boolean captchaEnabled;
    private String recaptchaSiteKey;
    private String recaptchaSecretKey;

    // Scan log
    private boolean useScanLog;
    private int scanLogRetention;
    private boolean useScanEvents;
    private boolean measureMemory;

    // Danger Zone
    private IndexScheme indexScheme;
    private boolean forceInternalValueInsteadOfTags;
    private boolean sortAlphanum;
    private boolean sortStrict;
    private boolean defaultSortAlphanum;
    private boolean defaultSortStrict;

    public String getDownloadLimit() {
        return downloadLimit;
    }

    public void setDownloadLimit(String downloadLimit) {
        this.downloadLimit = downloadLimit;
    }

    public String getUploadLimit() {
        return uploadLimit;
    }

    public void setUploadLimit(String uploadLimit) {
        this.uploadLimit = uploadLimit;
    }

    public String getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(String bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getSmtpFrom() {
        return smtpFrom;
    }

    public void setSmtpFrom(String smtpFrom) {
        this.smtpFrom = smtpFrom;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpEncryption() {
        return smtpEncryption;
    }

    public void setSmtpEncryption(String smtpEncryption) {
        this.smtpEncryption = smtpEncryption;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public boolean isLdapEnabled() {
        return ldapEnabled;
    }

    public void setLdapEnabled(boolean ldapEnabled) {
        this.ldapEnabled = ldapEnabled;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getLdapSearchFilter() {
        return ldapSearchFilter;
    }

    public void setLdapSearchFilter(String ldapSearchFilter) {
        this.ldapSearchFilter = ldapSearchFilter;
    }

    public String getLdapManagerDn() {
        return ldapManagerDn;
    }

    public void setLdapManagerDn(String ldapManagerDn) {
        this.ldapManagerDn = ldapManagerDn;
    }

    public String getLdapManagerPassword() {
        return ldapManagerPassword;
    }

    public void setLdapManagerPassword(String ldapManagerPassword) {
        this.ldapManagerPassword = ldapManagerPassword;
    }

    public boolean isLdapAutoShadowing() {
        return ldapAutoShadowing;
    }

    public void setLdapAutoShadowing(boolean ldapAutoShadowing) {
        this.ldapAutoShadowing = ldapAutoShadowing;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getBrand() {
        return brand;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public void setCaptchaEnabled(boolean captchaEnabled) {
        this.captchaEnabled = captchaEnabled;
    }

    public String getRecaptchaSiteKey() {
        return recaptchaSiteKey;
    }

    public void setRecaptchaSiteKey(String recaptchaSiteKey) {
        this.recaptchaSiteKey = recaptchaSiteKey;
    }

    public String getRecaptchaSecretKey() {
        return recaptchaSecretKey;
    }

    public void setRecaptchaSecretKey(String recaptchaSecretKey) {
        this.recaptchaSecretKey = recaptchaSecretKey;
    }

    public boolean isUseScanLog() {
        return useScanLog;
    }

    public void setUseScanLog(boolean useScanLog) {
        this.useScanLog = useScanLog;
    }

    public int getScanLogRetention() {
        return scanLogRetention;
    }

    public void setScanLogRetention(int scanLogRetention) {
        this.scanLogRetention = scanLogRetention;
    }

    public boolean isUseScanEvents() {
        return useScanEvents;
    }

    public void setUseScanEvents(boolean useScanEvents) {
        this.useScanEvents = useScanEvents;
    }

    public boolean isMeasureMemory() {
        return measureMemory;
    }

    public void setMeasureMemory(boolean measureMemory) {
        this.measureMemory = measureMemory;
    }

    public IndexScheme getIndexScheme() {
        return indexScheme;
    }

    public void setIndexScheme(IndexScheme indexScheme) {
        this.indexScheme = indexScheme;
    }

    public boolean isForceInternalValueInsteadOfTags() {
        return forceInternalValueInsteadOfTags;
    }

    public void setForceInternalValueInsteadOfTags(boolean forceInternalValueInsteadOfTags) {
        this.forceInternalValueInsteadOfTags = forceInternalValueInsteadOfTags;
    }

    public boolean isSortAlphanum() {
        return sortAlphanum;
    }

    public void setSortAlphanum(boolean sortAlphanum) {
        this.sortAlphanum = sortAlphanum;
    }

    public boolean isSortStrict() {
        return sortStrict;
    }

    public void setSortStrict(boolean sortStrict) {
        this.sortStrict = sortStrict;
    }

    public boolean isDefaultSortAlphanum() {
        return defaultSortAlphanum;
    }

    public void setDefaultSortAlphanum(boolean defaultSortAlphanum) {
        this.defaultSortAlphanum = defaultSortAlphanum;
    }

    public boolean isDefaultSortStrict() {
        return defaultSortStrict;
    }

    public void setDefaultSortStrict(boolean defaultSortStrict) {
        this.defaultSortStrict = defaultSortStrict;
    }
}
