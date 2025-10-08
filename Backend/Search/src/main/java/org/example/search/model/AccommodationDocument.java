package org.example.search.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.search.dto.AvailabilityDto;
import org.example.search.dto.LocationDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(indexName = "accommodations_v2")
public class AccommodationDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Integer)
    private Integer minGuests;

    @Field(type = FieldType.Integer)
    private Integer maxGuests;

    @Field(type = FieldType.Boolean)
    private boolean autoConfirm;

    @Field(type = FieldType.Keyword)
    private String pricingMode;

    @Field(type = FieldType.Object)
    private LocationDto location;

    @Field(type = FieldType.Keyword)
    private List<String> amenities;

    @Field(type = FieldType.Keyword)
    private List<String> photos;

    @Field(type = FieldType.Nested)
    @Builder.Default
    private List<AvailabilityDto> availabilities = new ArrayList<>();
}
