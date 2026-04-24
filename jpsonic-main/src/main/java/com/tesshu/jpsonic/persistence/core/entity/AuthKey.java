package com.tesshu.jpsonic.persistence.core.entity;

import java.time.Instant;

public class AuthKey {

    private int keyType;
    private String value;
    private Instant lastUpdate;

    public AuthKey(int keyType, String value, Instant lastUpdate) {
        super();
        this.keyType = keyType;
        this.value = value;
        this.lastUpdate = lastUpdate;
    }

    public int getKeyType() {
        return keyType;
    }

    public void setKeyType(int keyType) {
        this.keyType = keyType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
