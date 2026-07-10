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

@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Patient patient;

    @ManyToOne(optional = false)
    private Pharmacy pharmacy;

    @Column(name = "drug_name", nullable = false)
    private String drugName;

    @Column(name = "ndc_code", nullable = false)
    private String ndcCode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "days_supply", nullable = false)
    private Integer daysSupply;

    @Column(name = "prescriber_npi", nullable = false)
    private String prescriberNpi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrescriptionStatus status = PrescriptionStatus.PENDING;

    @Column(name = "retail_price_cents", nullable = false)
    private Long retailPriceCents;

    /**
     * Patient is billed at retail price by default. Only flips to PAID_BY_INSURANCE once a
     * {@link PharmacyAppeal} against an approved {@link InsuranceApplication} is paid --
     * there is no automatic $0 dispense.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "bill_status", nullable = false)
    private BillStatus billStatus = BillStatus.PATIENT_OWES;

    @Column(name = "dispensed_at")
    private Instant dispensedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Pharmacy getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(Pharmacy pharmacy) {
        this.pharmacy = pharmacy;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getNdcCode() {
        return ndcCode;
    }

    public void setNdcCode(String ndcCode) {
        this.ndcCode = ndcCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getDaysSupply() {
        return daysSupply;
    }

    public void setDaysSupply(Integer daysSupply) {
        this.daysSupply = daysSupply;
    }

    public String getPrescriberNpi() {
        return prescriberNpi;
    }

    public void setPrescriberNpi(String prescriberNpi) {
        this.prescriberNpi = prescriberNpi;
    }

    public PrescriptionStatus getStatus() {
        return status;
    }

    public void setStatus(PrescriptionStatus status) {
        this.status = status;
    }

    public Long getRetailPriceCents() {
        return retailPriceCents;
    }

    public void setRetailPriceCents(Long retailPriceCents) {
        this.retailPriceCents = retailPriceCents;
    }

    public Instant getDispensedAt() {
        return dispensedAt;
    }

    public void setDispensedAt(Instant dispensedAt) {
        this.dispensedAt = dispensedAt;
    }

    public BillStatus getBillStatus() {
        return billStatus;
    }

    public void setBillStatus(BillStatus billStatus) {
        this.billStatus = billStatus;
    }
}
