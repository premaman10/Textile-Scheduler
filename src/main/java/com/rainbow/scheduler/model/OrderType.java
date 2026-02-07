package com.rainbow.scheduler.model;

public enum OrderType {
    RUSH(1.3, "High urgency, tight deadline"),
    STANDARD(1.0, "Normal priority"),
    BULK(0.95, "Flexible, discount pricing");

    private final double priceMultiplier;
    private final String description;

    OrderType(double priceMultiplier, String description) {
        this.priceMultiplier = priceMultiplier;
        this.description = description;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    public String getDescription() {
        return description;
    }
}
