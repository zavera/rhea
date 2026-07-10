package org.callistotech.rhea.controller;

import org.callistotech.rhea.dto.SubmitAppealRequest;
import org.callistotech.rhea.model.AppealStatus;
import org.callistotech.rhea.model.PharmacyAppeal;
import org.callistotech.rhea.service.PharmacyAppealService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PharmacyAppealController {

    private final PharmacyAppealService appealService;

    public PharmacyAppealController(PharmacyAppealService appealService) {
        this.appealService = appealService;
    }

    @PostMapping("/api/pharmacy-appeals")
    @ResponseStatus(HttpStatus.CREATED)
    public PharmacyAppeal submit(@RequestBody SubmitAppealRequest request) {
        return appealService.submitAppeal(request.prescriptionId(), request.insuranceApplicationId());
    }

    @GetMapping("/api/pharmacy-appeals/{id}")
    public PharmacyAppeal get(@PathVariable Long id) {
        return appealService.get(id);
    }

    @PostMapping("/api/pharmacy-appeals/{id}/decision")
    public PharmacyAppeal advance(@PathVariable Long id, @RequestParam AppealStatus status) {
        return appealService.advance(id, status);
    }
}
