package org.callistotech.rhea.service;

import org.callistotech.rhea.dto.DispenseRequest;
import org.callistotech.rhea.dto.DispenseResult;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.model.Pharmacy;
import org.callistotech.rhea.model.Prescription;
import org.callistotech.rhea.model.PrescriptionStatus;
import org.callistotech.rhea.model.ReimbursementClaim;
import org.callistotech.rhea.model.UnemploymentVerification;
import org.callistotech.rhea.model.VerificationStatus;
import org.callistotech.rhea.repository.PatientRepository;
import org.callistotech.rhea.repository.PharmacyRepository;
import org.callistotech.rhea.repository.PrescriptionRepository;
import org.callistotech.rhea.repository.UnemploymentVerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;

/**
 * Orchestrates a $0 dispense: confirms the patient has a verified state
 * unemployment claim, records the prescription as dispensed at $0 to the
 * patient, and files the pharmacy's reimbursement claim against the chosen
 * state program in one transaction.
 */
@Service
public class DispenseService {

    private static final String DEFAULT_STATE_PROGRAM = "Colorado Indigent Care Program (CICP)";

    private final PatientRepository patientRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UnemploymentVerificationRepository verificationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ReimbursementClaimService reimbursementClaimService;

    public DispenseService(PatientRepository patientRepository,
                            PharmacyRepository pharmacyRepository,
                            UnemploymentVerificationRepository verificationRepository,
                            PrescriptionRepository prescriptionRepository,
                            ReimbursementClaimService reimbursementClaimService) {
        this.patientRepository = patientRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.verificationRepository = verificationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.reimbursementClaimService = reimbursementClaimService;
    }

    @Transactional
    public DispenseResult dispense(DispenseRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new NoSuchElementException("Patient not found: " + request.patientId()));
        Pharmacy pharmacy = pharmacyRepository.findById(request.pharmacyId())
                .orElseThrow(() -> new NoSuchElementException("Pharmacy not found: " + request.pharmacyId()));
        UnemploymentVerification verification = verificationRepository.findById(request.verificationId())
                .orElseThrow(() -> new NoSuchElementException("Verification not found: " + request.verificationId()));

        if (!verification.getPatient().getId().equals(patient.getId())) {
            throw new IllegalStateException("Verification does not belong to this patient");
        }
        if (verification.getStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Cannot dispense at $0 -- unemployment status is not verified");
        }

        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setPharmacy(pharmacy);
        prescription.setDrugName(request.drugName());
        prescription.setNdcCode(request.ndcCode());
        prescription.setQuantity(request.quantity());
        prescription.setDaysSupply(request.daysSupply());
        prescription.setPrescriberNpi(request.prescriberNpi());
        prescription.setRetailPriceCents(request.retailPriceCents());
        prescription.setStatus(PrescriptionStatus.DISPENSED);
        prescription.setDispensedAt(Instant.now());
        prescription = prescriptionRepository.save(prescription);

        String stateProgram = (request.stateProgram() == null || request.stateProgram().isBlank())
                ? DEFAULT_STATE_PROGRAM
                : request.stateProgram();
        ReimbursementClaim claim = reimbursementClaimService.submitClaim(prescription, verification, stateProgram);

        return new DispenseResult(prescription, claim, 0L);
    }
}
