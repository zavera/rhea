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
 * An AI-drafted application pairing a patient with a Colorado public program, gated by
 * explicit patient consent before it is ever submitted. Lifecycle:
 * DRAFTED (AI has proposed a program) -&gt; SUBMITTED (patient consented) or DECLINED
 * (patient did not consent, nothing is filed) -&gt; APPROVED or DENIED (state decision,
 * recorded manually by pharmacy staff since there is no live state API to poll).
 */
@Entity
@Table(name = "insurance_applications")
public class InsuranceApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Patient patient;

    @Column(name = "program_name")
    private String programName;

    @Column(name = "ai_summary", nullable = false, length = 4000)
    private String aiSummary;

    @Column(name = "source_urls", length = 2000)
    private String sourceUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.DRAFTED;

    @Column(name = "consented_at")
    private Instant consentedAt;

    @Column(name = "consent_text", length = 1000)
    private String consentText;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

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

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getSourceUrls() {
        return sourceUrls;
    }

    public void setSourceUrls(String sourceUrls) {
        this.sourceUrls = sourceUrls;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public Instant getConsentedAt() {
        return consentedAt;
    }

    public void setConsentedAt(Instant consentedAt) {
        this.consentedAt = consentedAt;
    }

    public String getConsentText() {
        return consentText;
    }

    public void setConsentText(String consentText) {
        this.consentText = consentText;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
