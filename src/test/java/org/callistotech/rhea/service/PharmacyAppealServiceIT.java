package org.callistotech.rhea.service;

import org.callistotech.rhea.model.AppealStatus;
import org.callistotech.rhea.model.ApplicationStatus;
import org.callistotech.rhea.model.BillStatus;
import org.callistotech.rhea.model.InsuranceApplication;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.model.Pharmacy;
import org.callistotech.rhea.model.PharmacyAppeal;
import org.callistotech.rhea.model.Prescription;
import org.callistotech.rhea.model.PrescriptionStatus;
import org.callistotech.rhea.repository.InsuranceApplicationRepository;
import org.callistotech.rhea.repository.PatientRepository;
import org.callistotech.rhea.repository.PharmacyRepository;
import org.callistotech.rhea.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PharmacyAppealServiceIT {

    @Autowired
    private PharmacyAppealService appealService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private InsuranceApplicationRepository applicationRepository;

    @Test
    void cannotAppealAgainstAnUnapprovedApplication() {
        Patient patient = patient("CASE-APPEAL-1");
        Prescription prescription = prescription(patient, 4250L);
        InsuranceApplication application = application(patient, ApplicationStatus.SUBMITTED);

        assertThatThrownBy(() -> appealService.submitAppeal(prescription.getId(), application.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not approved");
    }

    @Test
    void payingAnAppealClearsThePrescriptionBill() {
        Patient patient = patient("CASE-APPEAL-2");
        Prescription prescription = prescription(patient, 4250L);
        InsuranceApplication application = application(patient, ApplicationStatus.APPROVED);
        assertThat(prescription.getBillStatus()).isEqualTo(BillStatus.PATIENT_OWES);

        PharmacyAppeal appeal = appealService.submitAppeal(prescription.getId(), application.getId());
        assertThat(appeal.getStatus()).isEqualTo(AppealStatus.SUBMITTED);
        assertThat(appeal.getAmountCents()).isEqualTo(4250L);

        PharmacyAppeal paid = appealService.advance(appeal.getId(), AppealStatus.PAID);

        assertThat(paid.getStatus()).isEqualTo(AppealStatus.PAID);
        Prescription reloaded = prescriptionRepository.findById(prescription.getId()).orElseThrow();
        assertThat(reloaded.getBillStatus()).isEqualTo(BillStatus.PAID_BY_INSURANCE);
    }

    private Patient patient(String caseNumber) {
        Patient patient = new Patient();
        patient.setFirstName("Jordan");
        patient.setLastName("Reyes");
        patient.setDateOfBirth(LocalDate.of(1990, 5, 14));
        patient.setStateCaseNumber(caseNumber);
        return patientRepository.save(patient);
    }

    private Prescription prescription(Patient patient, long retailPriceCents) {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setNcpdpId("DEMO" + System.nanoTime());
        pharmacy.setName("Rhea Community Pharmacy (Demo)");
        pharmacy.setCity("Denver");
        pharmacy.setState("CO");
        pharmacy.setZip("80203");
        pharmacy = pharmacyRepository.save(pharmacy);

        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setPharmacy(pharmacy);
        prescription.setDrugName("Metformin 500mg");
        prescription.setNdcCode("00093-1048-01");
        prescription.setQuantity(60);
        prescription.setDaysSupply(30);
        prescription.setPrescriberNpi("1234567890");
        prescription.setRetailPriceCents(retailPriceCents);
        prescription.setStatus(PrescriptionStatus.DISPENSED);
        return prescriptionRepository.save(prescription);
    }

    private InsuranceApplication application(Patient patient, ApplicationStatus status) {
        InsuranceApplication application = new InsuranceApplication();
        application.setPatient(patient);
        application.setAiSummary("Best fit: Health First Colorado (Medicaid).");
        application.setStatus(status);
        return applicationRepository.save(application);
    }
}
