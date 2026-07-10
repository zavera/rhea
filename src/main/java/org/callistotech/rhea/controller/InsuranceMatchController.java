package org.callistotech.rhea.controller;

import org.callistotech.rhea.dto.InsuranceMatchRequest;
import org.callistotech.rhea.model.InsuranceMatchRecommendation;
import org.callistotech.rhea.model.InsuranceProgram;
import org.callistotech.rhea.repository.InsuranceProgramRepository;
import org.callistotech.rhea.service.InsuranceMatchAgentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class InsuranceMatchController {

    private final InsuranceMatchAgentService insuranceMatchAgentService;
    private final InsuranceProgramRepository insuranceProgramRepository;

    public InsuranceMatchController(InsuranceMatchAgentService insuranceMatchAgentService,
                                     InsuranceProgramRepository insuranceProgramRepository) {
        this.insuranceMatchAgentService = insuranceMatchAgentService;
        this.insuranceProgramRepository = insuranceProgramRepository;
    }

    @PostMapping("/api/insurance-matches")
    @ResponseStatus(HttpStatus.CREATED)
    public InsuranceMatchRecommendation match(@RequestBody InsuranceMatchRequest request) {
        return insuranceMatchAgentService.findMatch(request.patientId());
    }

    @GetMapping("/api/insurance-programs")
    public List<InsuranceProgram> knownPrograms() {
        return insuranceProgramRepository.findAll();
    }
}
