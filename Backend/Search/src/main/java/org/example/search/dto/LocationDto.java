package org.example.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LocationDto {
    private String country;
    private String city;
    private String address;
    private String postalCode;
}
