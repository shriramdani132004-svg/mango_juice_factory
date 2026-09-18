package com.smartfactory.service;

import com.smartfactory.enums.QualityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class QualityRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(QualityRuleEngine.class);

    public QualityResult evaluate(BigDecimal observedValue, BigDecimal expectedMin, BigDecimal expectedMax) {
        if (observedValue == null) {
            return QualityResult.FAIL;
        }
        if (expectedMin == null || expectedMax == null) {
            log.warn("No expected range configured, marking as CONDITIONAL");
            return QualityResult.CONDITIONAL;
        }
        if (observedValue.compareTo(expectedMin) >= 0 && observedValue.compareTo(expectedMax) <= 0) {
            return QualityResult.PASS;
        }
        return QualityResult.FAIL;
    }

    public QualityResult evaluateExact(BigDecimal observedValue, BigDecimal expectedValue, BigDecimal tolerance) {
        if (observedValue == null || expectedValue == null) {
            return QualityResult.FAIL;
        }
        BigDecimal diff = observedValue.subtract(expectedValue).abs();
        if (tolerance == null) {
            tolerance = BigDecimal.ZERO;
        }
        if (diff.compareTo(tolerance) <= 0) {
            return QualityResult.PASS;
        }
        return QualityResult.FAIL;
    }

    public String getExpectedRangeText(BigDecimal expectedMin, BigDecimal expectedMax) {
        if (expectedMin == null || expectedMax == null) {
            return "No range configured";
        }
        return expectedMin.toPlainString() + " - " + expectedMax.toPlainString();
    }

    public String formatResult(String parameterName, BigDecimal observed, BigDecimal expectedMin, BigDecimal expectedMax, QualityResult result) {
        String range = getExpectedRangeText(expectedMin, expectedMax);
        String obsStr = observed != null ? observed.toPlainString() : "N/A";
        return parameterName + ": observed=" + obsStr + ", expected=[" + range + "], result=" + result;
    }
}
