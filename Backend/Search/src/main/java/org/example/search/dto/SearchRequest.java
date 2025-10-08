package org.example.search.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SearchRequest {
    private String location;
    private Integer guests;
    private LocalDate startDate;
    private LocalDate endDate;
}
