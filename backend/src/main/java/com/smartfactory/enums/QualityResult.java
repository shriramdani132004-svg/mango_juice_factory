package com.smartfactory.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QualityResult {
    PASS,
    FAIL,
    CONDITIONAL;

    @JsonCreator
    public static QualityResult fromValue(String value) {
        if (value == null) return null;
        return valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
