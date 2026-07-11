package org.callistotech.rhea.dto;

public record RegisterRequest(
        String name,
        String email,
        String password) {
}
