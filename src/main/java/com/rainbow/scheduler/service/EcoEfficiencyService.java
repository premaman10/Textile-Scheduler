package com.rainbow.scheduler.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EcoEfficiencyService {

    @Value("${eco.water-per-cleaning-minute:15}")
    private int waterPerMinute;

    @Value("${eco.chemical-waste-per-cleaning-minute:0.2}")
    private double chemicalWastePerMinute;

    public int calculateWaterUsage(int cleaningMinutes) {
        return cleaningMinutes * waterPerMinute;
    }

    public double calculateChemicalWaste(int cleaningMinutes) {
        return cleaningMinutes * chemicalWastePerMinute;
    }

    public char calculateEcoGrade(int totalCleaningMinutes, int totalProductionMinutes) {
        if (totalProductionMinutes == 0)
            return 'E';

        double ratio = (double) totalCleaningMinutes / totalProductionMinutes;

        if (ratio < 0.1)
            return 'A'; // Less than 10% downtime
        if (ratio < 0.2)
            return 'B';
        if (ratio < 0.3)
            return 'C';
        if (ratio < 0.45)
            return 'D';
        return 'E';
    }
}
