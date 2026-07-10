package org.callistotech.rhea.dto;

import java.time.LocalDate;

public record CreatePatientRequest(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String stateCaseNumber) {
}
