package com.smartfactory.enums;

public enum Unit {
    KG("kg"),
    LITER("L"),
    MILLILITER("mL"),
    UNIT("pcs"),
    BOTTLE("btl"),
    CARTON("crt"),
    PALLET("plt"),
    METER("m");

    private final String symbol;

    Unit(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static Unit fromSymbol(String symbol) {
        for (Unit unit : values()) {
            if (unit.symbol.equalsIgnoreCase(symbol)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unknown unit symbol: " + symbol);
    }
}
