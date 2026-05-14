package com.evcharging.service;

import com.evcharging.dao.AvailabilityDAO;
import com.evcharging.dao.ConnectorDAO;
import com.evcharging.dto.TimeSlotResponse;
import com.evcharging.model.Connector;
import com.evcharging.model.TimeSlot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class AvailabilityService {

    private final AvailabilityDAO availabilityDAO = new AvailabilityDAO();
    private final ConnectorDAO connectorDAO = new ConnectorDAO();

    public List<TimeSlotResponse> getAvailableSlots(int connectorId, String dateText) throws Exception {
        if (connectorId <= 0) {
            throw new IllegalArgumentException("Valid connector ID is required.");
        }

        if (dateText == null || dateText.isBlank()) {
            throw new IllegalArgumentException("Date query parameter is required. Use YYYY-MM-DD.");
        }

        Connector connector = connectorDAO.findById(connectorId);

        if (connector == null) {
            throw new NoSuchElementException("Connector not found.");
        }

        LocalDate date = LocalDate.parse(dateText);

        List<TimeSlot> slots = availabilityDAO.getAvailableSlots(connectorId, date);
        List<TimeSlotResponse> response = new ArrayList<>();

        for (TimeSlot slot : slots) {
            response.add(new TimeSlotResponse(
                    slot.getStartTime().toString(),
                    slot.getEndTime().toString()
            ));
        }

        return response;
    }
}