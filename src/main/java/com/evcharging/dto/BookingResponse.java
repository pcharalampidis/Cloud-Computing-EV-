package com.evcharging.dto;

import com.evcharging.model.Booking;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BookingResponse {
    @JsonProperty("booking_id")
    private int bookingId;

    private String username;

    @JsonProperty("station_id")
    private int stationId;

    @JsonProperty("connector_id")
    private int connectorId;

    @JsonProperty("booking_date")
    private String bookingDate;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    private String status;

    public BookingResponse() {
    }

    public BookingResponse(Booking booking) {
        this.bookingId = booking.getBookingId();
        this.username = booking.getUsername();
        this.stationId = booking.getStationId();
        this.connectorId = booking.getConnectorId();
        this.bookingDate = booking.getBookingDate().toString();
        this.startTime = booking.getStartTime().toString();
        this.endTime = booking.getEndTime().toString();
        this.status = booking.getStatus();
    }

    public int getBookingId() {
        return bookingId;
    }

    public String getUsername() {
        return username;
    }

    public int getStationId() {
        return stationId;
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

    public String getStatus() {
        return status;
    }
}