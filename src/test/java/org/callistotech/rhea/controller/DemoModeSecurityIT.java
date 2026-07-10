package org.callistotech.rhea.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms REQUIRE_AUTH=false (used by the local demo launch config) opens up /api/**
 * without a login, while the auth machinery itself (login page, user store) stays intact.
 */
@SpringBootTest(properties = "rhea.require-auth=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoModeSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiIsReachableWithoutLoginWhenAuthNotRequired() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk());
    }

    @Test
    void configEndpointReportsAuthDisabled() throws Exception {
        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk());
    }
}
