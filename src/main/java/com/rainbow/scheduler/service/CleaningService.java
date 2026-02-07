package com.rainbow.scheduler.service;

import com.rainbow.scheduler.model.ColorFamily;
import org.springframework.stereotype.Service;

@Service
public class CleaningService {

    /**
     * Follows the Industrial Cleaning Matrix:
     * - Same Family: 2 mins (Simple reset)
     * - Moving Darker (Light -> Dark): 10-15 mins
     * - Moving Lighter (Dark -> Light): 30-45 mins (Deep purge)
     * - White <-> Black: 60 mins (Extreme penalty)
     */
    public int calculateCleaningTime(ColorFamily from, ColorFamily to) {
        if (from == to) {
            return 2; // Industrial batching (very efficient)
        }

        // Specific Industrial Matrix
        if (from == ColorFamily.WHITES_PASTELS) {
            if (to == ColorFamily.LIGHT_COLORS)
                return 10;
            if (to == ColorFamily.MEDIUM_COLORS)
                return 15;
            if (to == ColorFamily.DARK_COLORS)
                return 25;
            if (to == ColorFamily.BLACKS_DEEP_DARKS)
                return 60;
        }

        if (from == ColorFamily.LIGHT_COLORS) {
            if (to == ColorFamily.WHITES_PASTELS)
                return 30;
            if (to == ColorFamily.MEDIUM_COLORS)
                return 10;
            if (to == ColorFamily.DARK_COLORS)
                return 15;
            if (to == ColorFamily.BLACKS_DEEP_DARKS)
                return 45;
        }

        if (from == ColorFamily.MEDIUM_COLORS) {
            if (to == ColorFamily.WHITES_PASTELS)
                return 40;
            if (to == ColorFamily.LIGHT_COLORS)
                return 20;
            if (to == ColorFamily.DARK_COLORS)
                return 10;
            if (to == ColorFamily.BLACKS_DEEP_DARKS)
                return 15;
        }

        if (from == ColorFamily.DARK_COLORS) {
            if (to == ColorFamily.WHITES_PASTELS)
                return 50;
            if (to == ColorFamily.LIGHT_COLORS)
                return 35;
            if (to == ColorFamily.MEDIUM_COLORS)
                return 25;
            if (to == ColorFamily.BLACKS_DEEP_DARKS)
                return 10;
        }

        if (from == ColorFamily.BLACKS_DEEP_DARKS) {
            if (to == ColorFamily.WHITES_PASTELS)
                return 60;
            if (to == ColorFamily.LIGHT_COLORS)
                return 50;
            if (to == ColorFamily.MEDIUM_COLORS)
                return 40;
            if (to == ColorFamily.DARK_COLORS)
                return 30;
        }

        // Default heuristic
        return calculateJumpHeuristic(from, to);
    }

    private int calculateJumpHeuristic(ColorFamily from, ColorFamily to) {
        int fromIdx = from.ordinal();
        int toIdx = to.ordinal();

        if (toIdx > fromIdx) { // Moving darker
            return (toIdx - fromIdx) * 12;
        } else { // Moving lighter
            return (fromIdx - toIdx) * 20;
        }
    }
}
