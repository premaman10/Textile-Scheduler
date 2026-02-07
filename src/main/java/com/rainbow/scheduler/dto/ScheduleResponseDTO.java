package com.rainbow.scheduler.dto;

import com.rainbow.scheduler.model.ColorFamily;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ScheduleResponseDTO {
    private int optimizedCleaningTimeMinutes;
    private int fifoCleaningTimeMinutes;
    private int timeSavedMinutes;
    private String deadlineCompliance;
    private String machineEfficiency;
    private List<SlotDTO> schedule;

    @Data
    @Builder
    public static class SlotDTO {
        private Long orderId;
        private ColorFamily colorFamily;
        private String startTime;
        private String endTime;
        private int cleaningBeforeMinutes;
    }
}
