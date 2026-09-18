package com.smartfactory.enums;

public enum Role {
    ADMIN,
    PRODUCTION_WORKER,
    MAINTENANCE_WORKER,
    QUALITY_WORKER;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
