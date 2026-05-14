package com.evcharging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TimeSlotResponse {
    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    public TimeSlotResponse() {
    }

    public TimeSlotResponse(String startTime, String endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
}