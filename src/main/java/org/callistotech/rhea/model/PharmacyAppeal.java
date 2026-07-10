package org.callistotech.rhea.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The pharmacy's petition to an approved {@link InsuranceApplication}'s program for
 * reimbursement of a prescription the patient was billed for at dispense time. Filed only
 * after the application reaches {@link ApplicationStatus#APPROVED}; when this appeal is
 * marked {@link AppealStatus#PAID}, the linked prescription's bill is cleared.
 */
@Entity
@Table(name = "pharmacy_appeals")
public class PharmacyAppeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Prescription prescription;

    @ManyToOne(optional = false)
    private InsuranceApplication insuranceApplication;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppealStatus status = AppealStatus.SUBMITTED;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "decided_at")
    private Instant decidedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public InsuranceApplication getInsuranceApplication() {
        return insuranceApplication;
    }

    public void setInsuranceApplication(InsuranceApplication insuranceApplication) {
        this.insuranceApplication = insuranceApplication;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public AppealStatus getStatus() {
        return status;
    }

    public void setStatus(AppealStatus status) {
        this.status = status;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }
}
