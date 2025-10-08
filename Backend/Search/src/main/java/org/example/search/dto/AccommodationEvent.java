package org.example.search.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AccommodationEvent {
    private String eventType;
    private String id;
    private String hostId;
    private String name;
    private String description;
    private Integer minGuests;
    private Integer maxGuests;
    private boolean autoConfirm;
    private String pricingMode;
    private LocationDto location;
    private List<String> amenities;
    private List<String> photos;
}
