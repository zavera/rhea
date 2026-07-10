package org.callistotech.rhea.service;

import org.callistotech.rhea.model.ClaimStatus;
import org.callistotech.rhea.model.Prescription;
import org.callistotech.rhea.model.ReimbursementClaim;
import org.callistotech.rhea.model.UnemploymentVerification;
import org.callistotech.rhea.repository.ReimbursementClaimRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Tracks the pharmacy's reimbursement petition to the state program that
 * backs a $0 dispense. Submission here is a recordkeeping step (SUBMITTED);
 * {@link #advance} simulates the state program's adjudication for demo
 * purposes until a real payer integration exists.
 */
@Service
public class ReimbursementClaimService {

    private final ReimbursementClaimRepository claimRepository;

    public ReimbursementClaimService(ReimbursementClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public ReimbursementClaim submitClaim(Prescription prescription, UnemploymentVerification verification,
                                           String stateProgram) {
        ReimbursementClaim claim = new ReimbursementClaim();
        claim.setPrescription(prescription);
        claim.setVerification(verification);
        claim.setStateProgram(stateProgram);
        claim.setClaimAmountCents(prescription.getRetailPriceCents());
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setExternalReference("RHEA-CLAIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return claimRepository.save(claim);
    }

    public ReimbursementClaim get(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Claim not found: " + id));
    }

    public ReimbursementClaim advance(Long id, ClaimStatus status) {
        ReimbursementClaim claim = get(id);
        claim.setStatus(status);
        claim.setDecidedAt(Instant.now());
        return claimRepository.save(claim);
    }
}
