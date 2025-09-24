package org.example.search.model;

import lombok.*;
import org.example.search.dto.LocationDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(indexName = "accommodations")
public class AccommodationDocument {

    @Id
    private String id;
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
