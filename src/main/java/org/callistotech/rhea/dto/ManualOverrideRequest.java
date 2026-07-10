package org.callistotech.rhea.dto;

public record ManualOverrideRequest(
        Long patientId,
        String managerName,
        String managerNotes) {
}
