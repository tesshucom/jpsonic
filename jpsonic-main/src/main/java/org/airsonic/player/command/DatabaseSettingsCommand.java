
package org.airsonic.player.command;

import javax.validation.constraints.NotNull;

import org.airsonic.player.spring.DataSourceConfigType;

public class DatabaseSettingsCommand {

    @NotNull
    private DataSourceConfigType configType;
    private String embedDriver;
    private String embedPassword;
    private String embedUrl;
    private String embedUsername;
    private String jndiName;
    private int mysqlVarcharMaxlength;
    private String usertableQuote;
    private boolean useRadio;
    private boolean useSonos;
    private boolean showOutlineHelp;

    public DataSourceConfigType getConfigType() {
        return configType;
    }

    public void setConfigType(DataSourceConfigType configType) {
        this.configType = configType;
    }

    public String getEmbedDriver() {
        return embedDriver;
    }

    public void setEmbedDriver(String embedDriver) {
        this.embedDriver = embedDriver;
    }

    public String getEmbedPassword() {
        return embedPassword;
    }

    public void setEmbedPassword(String embedPassword) {
        this.embedPassword = embedPassword;
    }

    public String getEmbedUrl() {
        return embedUrl;
    }

    public void setEmbedUrl(String embedUrl) {
        this.embedUrl = embedUrl;
    }

    public String getEmbedUsername() {
        return embedUsername;
    }

    public void setEmbedUsername(String embedUsername) {
        this.embedUsername = embedUsername;
    }

    public String getJNDIName() {
        return jndiName;
    }

    public void setJNDIName(String jndiName) {
        this.jndiName = jndiName;
    }

    public int getMysqlVarcharMaxlength() {
        return mysqlVarcharMaxlength;
    }

    public void setMysqlVarcharMaxlength(int mysqlVarcharMaxlength) {
        this.mysqlVarcharMaxlength = mysqlVarcharMaxlength;
    }

    public String getUsertableQuote() {
        return usertableQuote;
    }

    public void setUsertableQuote(String usertableQuote) {
        this.usertableQuote = usertableQuote;
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

    public boolean isShowOutlineHelp() {
        return showOutlineHelp;
    }

    public void setShowOutlineHelp(boolean showOutlineHelp) {
        this.showOutlineHelp = showOutlineHelp;
    }

}
