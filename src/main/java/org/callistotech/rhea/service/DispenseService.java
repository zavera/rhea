package org.callistotech.rhea.service;

import org.callistotech.rhea.dto.DispenseRequest;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.model.Pharmacy;
import org.callistotech.rhea.model.Prescription;
import org.callistotech.rhea.model.PrescriptionStatus;
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
 * Dispenses a prescription once unemployment is verified. The patient is billed at retail
 * price by default -- there is no automatic $0 dispense. $0 only happens retroactively, once
 * an {@link InsuranceApplication} the patient consented to is approved and the pharmacy's
 * {@link PharmacyAppeal} against it is paid (see {@link PharmacyAppealService}).
 */
@Service
public class DispenseService {

    private final PatientRepository patientRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UnemploymentVerificationRepository verificationRepository;
    private final PrescriptionRepository prescriptionRepository;

    public DispenseService(PatientRepository patientRepository,
                            PharmacyRepository pharmacyRepository,
                            UnemploymentVerificationRepository verificationRepository,
                            PrescriptionRepository prescriptionRepository) {
        this.patientRepository = patientRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.verificationRepository = verificationRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Transactional
    public Prescription dispense(DispenseRequest request) {
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
            throw new IllegalStateException("Cannot dispense -- unemployment status is not verified");
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
        return prescriptionRepository.save(prescription);
    }
}
