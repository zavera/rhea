package org.callistotech.rhea.dto;

public record SubmitAppealRequest(
        Long prescriptionId,
        Long insuranceApplicationId) {
}
