package org.callistotech.rhea.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ConfigController {

    private final boolean requireAuth;

    public ConfigController(@Value("${rhea.require-auth:true}") boolean requireAuth) {
        this.requireAuth = requireAuth;
    }

    @GetMapping("/api/config")
    public Map<String, Boolean> config() {
        return Map.of("requireAuth", requireAuth);
    }
}
