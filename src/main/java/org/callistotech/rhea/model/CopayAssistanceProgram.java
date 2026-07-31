package org.callistotech.rhea.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "copay_assistance_programs")
public class CopayAssistanceProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "program_type", nullable = false)
    private String programType;

    private String description;

    @Column(name = "eligibility_summary")
    private String eligibilitySummary;

    @Column(name = "official_url")
    private String officialUrl;

    @Column(name = "federal_program_safe", nullable = false)
    private boolean federalProgramSafe;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProgramType() {
        return programType;
    }

    public void setProgramType(String programType) {
        this.programType = programType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEligibilitySummary() {
        return eligibilitySummary;
    }

    public void setEligibilitySummary(String eligibilitySummary) {
        this.eligibilitySummary = eligibilitySummary;
    }

    public String getOfficialUrl() {
        return officialUrl;
    }

    public void setOfficialUrl(String officialUrl) {
        this.officialUrl = officialUrl;
    }

    public boolean isFederalProgramSafe() {
        return federalProgramSafe;
    }

    public void setFederalProgramSafe(boolean federalProgramSafe) {
        this.federalProgramSafe = federalProgramSafe;
    }
}
