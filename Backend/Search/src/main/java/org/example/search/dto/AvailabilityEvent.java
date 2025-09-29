package org.example.search.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AvailabilityEvent {
    private String eventType;
    private String id;
    private String accommodationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal price;
    private String priceType;
    private String status;
}
