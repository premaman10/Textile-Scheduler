package com.rainbow.scheduler.service;

import com.rainbow.scheduler.model.ColorFamily;
import org.springframework.stereotype.Service;

@Service
public class CleaningService {

    public int calculateCleaningTime(ColorFamily from, ColorFamily to) {
        if (from == to) {
            return 7; // Same family average
        }

        if (from == ColorFamily.WHITES_PASTELS && to == ColorFamily.LIGHT_COLORS)
            return 10;
        if (from == ColorFamily.LIGHT_COLORS && to == ColorFamily.MEDIUM_COLORS)
            return 10;
        if (from == ColorFamily.MEDIUM_COLORS && to == ColorFamily.DARK_COLORS)
            return 15;
        if (from == ColorFamily.DARK_COLORS && to == ColorFamily.BLACKS_DEEP_DARKS)
            return 15;

        if (from == ColorFamily.BLACKS_DEEP_DARKS && to == ColorFamily.DARK_COLORS)
            return 30;
        if (from == ColorFamily.DARK_COLORS && to == ColorFamily.MEDIUM_COLORS)
            return 25;
        if (from == ColorFamily.MEDIUM_COLORS && to == ColorFamily.LIGHT_COLORS)
            return 20;
        if (from == ColorFamily.LIGHT_COLORS && to == ColorFamily.WHITES_PASTELS)
            return 45;

        // White ↔ Black
        if ((from == ColorFamily.WHITES_PASTELS && to == ColorFamily.BLACKS_DEEP_DARKS) ||
                (from == ColorFamily.BLACKS_DEEP_DARKS && to == ColorFamily.WHITES_PASTELS)) {
            return 60;
        }

        // Default heuristic for other jumps (proportional to distance)
        return calculateJumpHeuristic(from, to);
    }

    private int calculateJumpHeuristic(ColorFamily from, ColorFamily to) {
        int fromIdx = from.ordinal();
        int toIdx = to.ordinal();

        if (toIdx > fromIdx) { // Moving darker
            return (toIdx - fromIdx) * 12; // Average darker jump
        } else { // Moving lighter
            return (fromIdx - toIdx) * 20; // Average lighter jump needs more cleaning
        }
    }
}
