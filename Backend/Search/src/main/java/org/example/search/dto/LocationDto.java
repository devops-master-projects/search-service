package org.example.search.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@Getter
@Setter
public class LocationDto {
    private String country;
    private String city;
    private String address;
    private String postalCode;
}
