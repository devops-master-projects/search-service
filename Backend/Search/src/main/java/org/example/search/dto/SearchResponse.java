package org.example.search.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class SearchResponse {
    private String id;
    private String name;
    private String description;
    private LocationDto location;
    private List<String> photos;
    private List<String> amenities;
    private int minGuests;
    private int maxGuests;

    private BigDecimal totalPrice;
    private BigDecimal unitPrice;
    private String pricingMode; // "PER_PERSON" ili "PER_ACCOMMODATION"
}
