package org.callistotech.rhea.service;

import org.callistotech.rhea.model.AppealStatus;
import org.callistotech.rhea.model.ApplicationStatus;
import org.callistotech.rhea.model.BillStatus;
import org.callistotech.rhea.model.InsuranceApplication;
import org.callistotech.rhea.model.PharmacyAppeal;
import org.callistotech.rhea.model.Prescription;
import org.callistotech.rhea.repository.InsuranceApplicationRepository;
import org.callistotech.rhea.repository.PharmacyAppealRepository;
import org.callistotech.rhea.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Tracks the pharmacy's appeal against an approved {@link InsuranceApplication} for
 * reimbursement of a prescription the patient was billed for at dispense time. {@link #advance}
 * simulates the program's adjudication for demo purposes until a real payer integration
 * exists; marking an appeal PAID clears the linked prescription's bill.
 */
@Service
public class PharmacyAppealService {

    private final PharmacyAppealRepository appealRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final InsuranceApplicationRepository applicationRepository;

    public PharmacyAppealService(PharmacyAppealRepository appealRepository,
                                  PrescriptionRepository prescriptionRepository,
                                  InsuranceApplicationRepository applicationRepository) {
        this.appealRepository = appealRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public PharmacyAppeal submitAppeal(Long prescriptionId, Long insuranceApplicationId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + prescriptionId));
        InsuranceApplication application = applicationRepository.findById(insuranceApplicationId)
                .orElseThrow(() -> new NoSuchElementException("Insurance application not found: " + insuranceApplicationId));

        if (!application.getPatient().getId().equals(prescription.getPatient().getId())) {
            throw new IllegalStateException("Insurance application does not belong to this prescription's patient");
        }
        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Cannot appeal -- insurance application is not approved");
        }

        PharmacyAppeal appeal = new PharmacyAppeal();
        appeal.setPrescription(prescription);
        appeal.setInsuranceApplication(application);
        appeal.setAmountCents(prescription.getRetailPriceCents());
        appeal.setStatus(AppealStatus.SUBMITTED);
        appeal.setExternalReference("RHEA-APPEAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return appealRepository.save(appeal);
    }

    public PharmacyAppeal get(Long id) {
        return appealRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Appeal not found: " + id));
    }

    @Transactional
    public PharmacyAppeal advance(Long id, AppealStatus status) {
        PharmacyAppeal appeal = get(id);
        appeal.setStatus(status);
        appeal.setDecidedAt(Instant.now());
        appeal = appealRepository.save(appeal);

        if (status == AppealStatus.PAID) {
            Prescription prescription = appeal.getPrescription();
            prescription.setBillStatus(BillStatus.PAID_BY_INSURANCE);
            prescriptionRepository.save(prescription);
        }

        return appeal;
    }
}
