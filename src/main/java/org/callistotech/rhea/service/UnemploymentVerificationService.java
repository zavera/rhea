package org.callistotech.rhea.service;

import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.model.UnemploymentVerification;

/**
 * Verifies a patient's active unemployment claim with the State of Colorado.
 * The production implementation should call the Colorado Department of Labor
 * and Employment (CDLE) claimant lookup once that integration is provisioned;
 * see {@link CdleStubVerificationService} for the interim stub.
 */
public interface UnemploymentVerificationService {

    UnemploymentVerification verify(Patient patient, String cdleClaimNumber);

    /**
     * Records a pharmacy manager's manual confirmation of unemployment status
     * (e.g. from a printed CDLE letter) when the automated lookup is
     * unavailable or returns no match. This bypasses the automated check, so
     * the confirming manager's name and reasoning are always recorded for
     * the audit trail.
     */
    UnemploymentVerification manualOverride(Patient patient, String managerName, String managerNotes);
}
