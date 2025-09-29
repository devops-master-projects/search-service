package org.example.search.integration;

import org.example.search.dto.AvailabilityDto;
import org.example.search.dto.LocationDto;
import org.example.search.dto.SearchRequest;
import org.example.search.dto.SearchResponse;
import org.example.search.model.AccommodationDocument;
import org.example.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(org.example.search.config.ElasticsearchTestConfig.class)
@ActiveProfiles("test")
class SearchServiceIntegrationTest {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private SearchService searchService;

    @BeforeEach
    void setupIndex() {
        var indexOps = elasticsearchOperations.indexOps(AccommodationDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.create();
        indexOps.putMapping();
        indexOps.refresh();

    }


    @Test
    void shouldReturnAccommodationFromElasticsearch() {
        AccommodationDocument doc = new AccommodationDocument();
        doc.setId("1");
        doc.setName("Integration Apt");
        doc.setPricingMode("PER_ACCOMMODATION");
        doc.setLocation(
                LocationDto.builder()
                        .country("France")
                        .city("Paris")
                        .address("123 Champs-Élysées")
                        .postalCode("75008")
                        .build()
        );

        doc.setAvailabilities(List.of(
                AvailabilityDto.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(2))
                        .status("AVAILABLE")
                        .price(new BigDecimal("100"))
                        .priceType("PER_PERSON")
                        .build()
        ));
        doc.setMinGuests(1);
        doc.setMaxGuests(4);
        elasticsearchOperations.save(doc);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();

        var allDocs = elasticsearchOperations.search(new CriteriaQuery(new Criteria()), AccommodationDocument.class);



        SearchRequest request = new SearchRequest("Paris", 2, LocalDate.now(), LocalDate.now().plusDays(2));

        List<SearchResponse> results = searchService.searchAccommodations(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Integration Apt");
        assertThat(results.get(0).getTotalPrice()).isEqualByComparingTo("300"); // 3 days * 100

        elasticsearchOperations.delete(doc);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();
    }
}
