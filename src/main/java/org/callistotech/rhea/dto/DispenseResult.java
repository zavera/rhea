package org.callistotech.rhea.dto;

import org.callistotech.rhea.model.Prescription;
import org.callistotech.rhea.model.ReimbursementClaim;

public record DispenseResult(
        Prescription prescription,
        ReimbursementClaim claim,
        long patientOwesCents) {
}
