package org.callistotech.rhea.dto;

public record CreatePharmacyRequest(
        String ncpdpId,
        String name,
        String address,
        String city,
        String state,
        String zip) {
}
