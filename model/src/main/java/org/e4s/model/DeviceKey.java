package org.e4s.model;

import java.io.Serializable;
import java.util.UUID;

public class DeviceKey implements Serializable {

    private static final long serialVersionUID = 7697020534877317395L;

    private final UUID key;

    public DeviceKey(UUID key) {
        this.key = key;
    }

    public UUID getKey() {
        return key;
    }

    @Override
    public String toString() {
        return key.toString();
    }
}
