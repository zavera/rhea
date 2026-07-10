package org.callistotech.rhea.controller;

import org.callistotech.rhea.model.ClaimStatus;
import org.callistotech.rhea.model.ReimbursementClaim;
import org.callistotech.rhea.service.ReimbursementClaimService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClaimController {

    private final ReimbursementClaimService claimService;

    public ClaimController(ReimbursementClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping("/api/claims/{id}")
    public ReimbursementClaim get(@PathVariable Long id) {
        return claimService.get(id);
    }

    @PostMapping("/api/claims/{id}/advance")
    public ReimbursementClaim advance(@PathVariable Long id, @RequestParam ClaimStatus status) {
        return claimService.advance(id, status);
    }
}
