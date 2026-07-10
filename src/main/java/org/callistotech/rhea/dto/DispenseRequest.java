package org.callistotech.rhea.dto;

public record DispenseRequest(
        Long patientId,
        Long pharmacyId,
        Long verificationId,
        String drugName,
        String ndcCode,
        Integer quantity,
        Integer daysSupply,
        String prescriberNpi,
        Long retailPriceCents,
        String stateProgram) {
}
