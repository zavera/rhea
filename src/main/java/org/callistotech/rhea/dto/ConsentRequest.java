package org.callistotech.rhea.dto;

public record ConsentRequest(
        String programName,
        String consentText,
        boolean agree) {
}
