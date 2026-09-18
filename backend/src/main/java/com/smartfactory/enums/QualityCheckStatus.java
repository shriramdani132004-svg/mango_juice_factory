package com.smartfactory.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QualityCheckStatus {
    PENDING,
    IN_PROGRESS,
    PASS,
    FAIL,
    REVIEW,
    VERIFIED,
    QUARANTINED,
    REJECTED,
    REPROCESS_REQUESTED,
    REPROCESSED;

    @JsonCreator
    public static QualityCheckStatus fromValue(String value) {
        if (value == null) return null;
        return valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
