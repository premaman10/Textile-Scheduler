package com.rainbow.scheduler.model;

public enum ColorFamily {
    WHITES_PASTELS("Whites & Pastels"),
    LIGHT_COLORS("Light Colors"),
    MEDIUM_COLORS("Medium Colors"),
    DARK_COLORS("Dark Colors"),
    BLACKS_DEEP_DARKS("Blacks & Deep Darks");

    private final String displayName;

    ColorFamily(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
