package org.callistotech.rhea.dto;

public record CreateVerificationRequest(
        Long patientId,
        String cdleClaimNumber) {
}
