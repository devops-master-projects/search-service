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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        SearchRequest request = new SearchRequest("Paris", 2, LocalDate.now(), LocalDate.now().plusDays(2));

        List<SearchResponse> results = searchService.searchAccommodations(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Integration Apt");
        assertThat(results.get(0).getTotalPrice()).isEqualByComparingTo("300"); // 3 days * 100

        elasticsearchOperations.delete(doc);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();
    }

    @Test
    void shouldCalculateTotalPricePerPerson() {
        AccommodationDocument doc = baseDocument("1", "Paris", "PER_PERSON", 1, 4);
        doc.setAvailabilities(List.of(
                AvailabilityDto.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(2)) // 3 dana
                        .status("AVAILABLE")
                        .price(new BigDecimal("100"))
                        .priceType("PER_PERSON")
                        .build()
        ));
        elasticsearchOperations.save(doc);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();

        SearchRequest request = new SearchRequest("Paris", 2, LocalDate.now(), LocalDate.now().plusDays(2));
        List<SearchResponse> results = searchService.searchAccommodations(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTotalPrice()).isEqualByComparingTo("600"); // 3 dana × 100 × 2 gosta
    }

    @Test
    void shouldCalculateTotalPricePerNight() {
        AccommodationDocument doc = baseDocument("2", "Paris", "PER_NIGHT", 1, 4);
        doc.setAvailabilities(List.of(
                AvailabilityDto.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(3)) // 4 dana
                        .status("AVAILABLE")
                        .price(new BigDecimal("200"))
                        .priceType("PER_NIGHT")
                        .build()
        ));
        elasticsearchOperations.save(doc);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();

        SearchRequest request = new SearchRequest("Paris", 2, LocalDate.now(), LocalDate.now().plusDays(3));
        List<SearchResponse> results = searchService.searchAccommodations(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTotalPrice()).isEqualByComparingTo("800"); // 4 noći × 200
    }

    @Test
    void shouldReturnEmptyWhenNoAvailability() {
        AccommodationDocument doc = baseDocument("3", "Paris", "PER_NIGHT", 1, 4);
        doc.setAvailabilities(List.of()); // prazno
        elasticsearchOperations.save(doc);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();

        SearchRequest request = new SearchRequest("Paris", 2, LocalDate.now(), LocalDate.now().plusDays(2));
        List<SearchResponse> results = searchService.searchAccommodations(request);

        assertThat(results).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenGuestsExceedMax() {
        AccommodationDocument doc = baseDocument("4", "Paris", "PER_NIGHT", 1, 2); // max 2
        doc.setAvailabilities(List.of(
                AvailabilityDto.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(2))
                        .status("AVAILABLE")
                        .price(new BigDecimal("100"))
                        .priceType("PER_NIGHT")
                        .build()
        ));
        elasticsearchOperations.save(doc);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();

        SearchRequest request = new SearchRequest("Paris", 5, LocalDate.now(), LocalDate.now().plusDays(2)); // 5 > maxGuests
        List<SearchResponse> results = searchService.searchAccommodations(request);

        assertThat(results).isEmpty();
    }

    @Test
    void shouldReturnUnitPriceWhenNoDatesProvided() {
        AccommodationDocument doc = baseDocument("5", "Paris", "PER_NIGHT", 1, 4);
        doc.setAvailabilities(List.of(
                AvailabilityDto.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(5))
                        .status("AVAILABLE")
                        .price(new BigDecimal("120"))
                        .priceType("PER_NIGHT")
                        .build()
        ));
        elasticsearchOperations.save(doc);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();

        SearchRequest request = new SearchRequest("Paris", 2, null, null); // nema datuma
        List<SearchResponse> results = searchService.searchAccommodations(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUnitPrice()).isEqualByComparingTo("120");
        assertThat(results.get(0).getTotalPrice()).isEqualByComparingTo("0"); // jer nema datuma
    }

    @Test
    void shouldReturnAllDocumentsSortedById() {
        // given
        AccommodationDocument doc1 = baseDocument("1", "Paris");
        AccommodationDocument doc2 = baseDocument("2", "London");

        elasticsearchOperations.save(doc1);
        elasticsearchOperations.save(doc2);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();

        // when
        List<AccommodationDocument> results = searchService.findAll();

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo("1");
        assertThat(results.get(1).getId()).isEqualTo("2");
    }

    @Test
    void shouldReturnEmptyListWhenNoDocumentsExist() {
        // when
        List<AccommodationDocument> results = searchService.findAll();
        // then
        assertThat(results).isEmpty();
    }

    @Test
    void shouldReturnSortedById() {
        AccommodationDocument docA = baseDocument("5", "Berlin");
        AccommodationDocument docB = baseDocument("2", "Rome");
        AccommodationDocument docC = baseDocument("3", "Paris");

        elasticsearchOperations.save(docA);
        elasticsearchOperations.save(docB);
        elasticsearchOperations.save(docC);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();

        List<AccommodationDocument> results = searchService.findAll();

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo("2");
        assertThat(results.get(1).getId()).isEqualTo("3");
        assertThat(results.get(2).getId()).isEqualTo("5");
    }

    @Test
    void shouldHandleDocumentWithMinimalFields() {
        AccommodationDocument doc = new AccommodationDocument();
        doc.setId("10");
        doc.setName("Minimal Apt");
        doc.setPricingMode("PER_NIGHT");
        doc.setMinGuests(1);
        doc.setMaxGuests(2);

        elasticsearchOperations.save(doc);
        elasticsearchOperations.indexOps(AccommodationDocument.class).refresh();

        List<AccommodationDocument> results = searchService.findAll();

        assertThat(results).extracting(AccommodationDocument::getName)
                .contains("Minimal Apt");
    }


    private AccommodationDocument baseDocument(String id, String city) {
        AccommodationDocument doc = new AccommodationDocument();
        doc.setId(id);
        doc.setName("Apt " + id);
        doc.setPricingMode("PER_NIGHT");
        doc.setMinGuests(1);
        doc.setMaxGuests(4);
        doc.setLocation(
                LocationDto.builder()
                        .country("TestCountry")
                        .city(city)
                        .address("Some street")
                        .postalCode("00000")
                        .build()
        );
        doc.setAvailabilities(List.of(
                AvailabilityDto.builder()
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(2))
                        .status("AVAILABLE")
                        .price(new BigDecimal("100"))
                        .priceType("PER_NIGHT")
                        .build()
        ));
        return doc;
    }

    private AccommodationDocument baseDocument(String id, String city, String pricingMode, int minGuests, int maxGuests) {
        AccommodationDocument doc = new AccommodationDocument();
        doc.setId(id);
        doc.setName("Test Apt " + id);
        doc.setPricingMode(pricingMode);
        doc.setMinGuests(minGuests);
        doc.setMaxGuests(maxGuests);
        doc.setLocation(
                LocationDto.builder()
                        .country("France")
                        .city(city)
                        .address("123 Champs-Élysées")
                        .postalCode("75008")
                        .build()
        );
        return doc;
    }



}
