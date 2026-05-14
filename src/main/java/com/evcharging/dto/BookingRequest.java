package com.evcharging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BookingRequest {

    private String username;

    @JsonProperty("connector_id")
    private int connectorId;

    @JsonProperty("booking_date")
    private String bookingDate;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    public BookingRequest() {
    }

    public String getUsername() {
        return username;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
}