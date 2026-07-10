package org.callistotech.rhea.repository;

import org.callistotech.rhea.model.ReimbursementClaim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementClaimRepository extends JpaRepository<ReimbursementClaim, Long> {

    void deleteByPrescription_Patient_Id(Long patientId);
}
