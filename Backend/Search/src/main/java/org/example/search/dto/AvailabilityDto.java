package org.example.search.dto;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AvailabilityDto {
    private String id;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd")
    private LocalDate startDate;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd")
    private LocalDate endDate;
    private BigDecimal price;
    private String priceType;
    private String status;
}
