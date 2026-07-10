package org.callistotech.rhea.controller;

import org.callistotech.rhea.dto.DispenseRequest;
import org.callistotech.rhea.dto.DispenseResult;
import org.callistotech.rhea.service.DispenseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DispenseController {

    private final DispenseService dispenseService;

    public DispenseController(DispenseService dispenseService) {
        this.dispenseService = dispenseService;
    }

    @PostMapping("/api/prescriptions/dispense")
    @ResponseStatus(HttpStatus.CREATED)
    public DispenseResult dispense(@RequestBody DispenseRequest request) {
        return dispenseService.dispense(request);
    }
}
