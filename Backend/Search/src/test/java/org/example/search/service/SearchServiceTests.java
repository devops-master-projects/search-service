package org.example.search.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.example.search.dto.AvailabilityDto;
import org.example.search.dto.SearchRequest;
import org.example.search.model.AccommodationDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTests {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private SearchService searchService;

    @Test
    void searchAccommodations_noDates_setsUnitPrice_and_totalPriceZero() {
        // arrange
        AvailabilityDto availability = AvailabilityDto.builder()
                .id("a1")
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 10))
                .price(BigDecimal.valueOf(100))
                .priceType("PER_NIGHT")
                .status("AVAILABLE")
                .build();

        AccommodationDocument doc = AccommodationDocument.builder()
                .id("doc1")
                .name("Test Place")
                .description("Nice")
                .minGuests(1)
                .maxGuests(4)
                .pricingMode("PER_ACCOMMODATION")
                .availabilities(List.of(availability))
                .build();

        SearchHits<AccommodationDocument> hits = mock(SearchHits.class);
        SearchHit<AccommodationDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hits.stream()).thenReturn(Stream.of(hit));
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn(hits);

        SearchRequest request = new SearchRequest();
        request.setLocation("City");
        request.setGuests(2);

        // act
        var results = searchService.searchAccommodations(request);

        // assert
        assertNotNull(results);
        assertEquals(1, results.size());
        var r = results.getFirst();
        assertEquals(doc.getId(), r.getId());
        assertEquals(new BigDecimal("100"), r.getUnitPrice());
        assertEquals(BigDecimal.ZERO, r.getTotalPrice());
    }

    @Test
    void searchAccommodations_withDateRange_computesTotalPrice_perPerson() {
        AvailabilityDto availability = AvailabilityDto.builder()
                .id("a1")
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 3))
                .price(BigDecimal.valueOf(10))
                .priceType("PER_NIGHT")
                .status("AVAILABLE")
                .build();

        AccommodationDocument doc = AccommodationDocument.builder()
                .id("doc2")
                .name("Person Place")
                .description("For people")
                .minGuests(1)
                .maxGuests(4)
                .pricingMode("PER_PERSON")
                .availabilities(List.of(availability))
                .build();

        SearchHits<AccommodationDocument> hits = mock(SearchHits.class);
        SearchHit<AccommodationDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hits.stream()).thenReturn(Stream.of(hit));
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn(hits);

        SearchRequest request = new SearchRequest();
        request.setLocation("City");
        request.setGuests(2);
        request.setStartDate(LocalDate.of(2025, 10, 1));
        request.setEndDate(LocalDate.of(2025, 10, 3));

        // act
        var results = searchService.searchAccommodations(request);

        // assert
        assertNotNull(results);
        assertEquals(1, results.size());
        var r = results.getFirst();
        assertEquals(doc.getId(), r.getId());
        assertEquals(new BigDecimal("10"), r.getUnitPrice());
        // 3 days * 2 guests * 10 = 60
        assertEquals(new BigDecimal("60"), r.getTotalPrice());
    }

    @Test
    void findAll_returnsMappedDocuments_sortedById() {
        // arrange
        AccommodationDocument doc1 = AccommodationDocument.builder()
                .id("b")
                .name("B")
                .build();
        AccommodationDocument doc2 = AccommodationDocument.builder()
                .id("a")
                .name("A")
                .build();

        SearchHits<AccommodationDocument> hits = mock(SearchHits.class);
        SearchHit<AccommodationDocument> hit1 = mock(SearchHit.class);
        SearchHit<AccommodationDocument> hit2 = mock(SearchHit.class);
        when(hit1.getContent()).thenReturn(doc1);
        when(hit2.getContent()).thenReturn(doc2);
        when(hits.getSearchHits()).thenReturn(List.of(hit1, hit2));
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn(hits);

        // act
        var results = searchService.findAll();

        // assert: sorted by id -> a, b
        assertEquals(2, results.size());
        assertEquals("a", results.get(0).getId());
        assertEquals("b", results.get(1).getId());
    }

    @Test
    void search_byLocationAndGuests_returnsMatchingDocuments() {
        // arrange
        AccommodationDocument doc1 = AccommodationDocument.builder()
                .id("1")
                .name("One")
                .build();
        AccommodationDocument doc2 = AccommodationDocument.builder()
                .id("2")
                .name("Two")
                .build();

        @SuppressWarnings({ "rawtypes" })
        SearchHits hits = mock(SearchHits.class);
        @SuppressWarnings({ "rawtypes" })
        SearchHits mappedHits = mock(SearchHits.class);

        // when mapped, return a list of documents (raw type to avoid generic mismatch
        // in mocking)
        when(mappedHits.toList()).thenReturn(List.of(doc1, doc2));

        // ensure calling .map(...) on the original hits returns our mappedHits
        when(hits.map(org.mockito.ArgumentMatchers.any())).thenReturn(mappedHits);

        // cast the raw hits to the parameterized type for the mocked
        // elasticsearchOperations
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn((SearchHits<AccommodationDocument>) hits);

        // act
        var results = searchService.search("SomeCity", 2);

        // assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("1", results.get(0).getId());
        assertEquals("2", results.get(1).getId());
    }

    @Test
    void searchAccommodations_withDateRange_computesTotalPrice_perAccommodation() {
        // arrange: PER_ACCOMMODATION pricing, 3 days => total = price * days
        AvailabilityDto availability = AvailabilityDto.builder()
                .id("a1")
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 3))
                .price(BigDecimal.valueOf(20))
                .priceType("PER_NIGHT")
                .status("AVAILABLE")
                .build();

        AccommodationDocument doc = AccommodationDocument.builder()
                .id("doc3")
                .name("Accommodation 3")
                .pricingMode("PER_ACCOMMODATION")
                .minGuests(1)
                .maxGuests(4)
                .availabilities(List.of(availability))
                .build();

        SearchHits<AccommodationDocument> hits = mock(SearchHits.class);
        SearchHit<AccommodationDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hits.stream()).thenReturn(Stream.of(hit));
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn(hits);

        SearchRequest request = new SearchRequest();
        request.setStartDate(LocalDate.of(2025, 10, 1));
        request.setEndDate(LocalDate.of(2025, 10, 3));
        request.setGuests(2);

        // act
        var results = searchService.searchAccommodations(request);

        // assert
        assertNotNull(results);
        assertEquals(1, results.size());
        var r = results.getFirst();
        assertEquals(new BigDecimal("20"), r.getUnitPrice());
        // 3 days * 20 = 60
        assertEquals(new BigDecimal("60"), r.getTotalPrice());
    }

    @Test
    void searchAccommodations_withDateRange_missingCoverage_filteredOut() {
        // arrange: availability starts after cursor -> result should be filtered out
        AvailabilityDto availability = AvailabilityDto.builder()
                .id("a1")
                .startDate(LocalDate.of(2025, 10, 2))
                .endDate(LocalDate.of(2025, 10, 3))
                .price(BigDecimal.valueOf(10))
                .status("AVAILABLE")
                .build();

        AccommodationDocument doc = AccommodationDocument.builder()
                .id("doc4")
                .name("Gap Place")
                .pricingMode("PER_ACCOMMODATION")
                .minGuests(1)
                .maxGuests(4)
                .availabilities(List.of(availability))
                .build();

        SearchHits<AccommodationDocument> hits = mock(SearchHits.class);
        SearchHit<AccommodationDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hits.stream()).thenReturn(Stream.of(hit));
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn(hits);

        SearchRequest request = new SearchRequest();
        request.setStartDate(LocalDate.of(2025, 10, 1));
        request.setEndDate(LocalDate.of(2025, 10, 3));
        request.setGuests(1);

        // act
        var results = searchService.searchAccommodations(request);

        // assert: availability doesn't cover full range -> should be filtered out
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    void searchAccommodations_withMultipleContiguousSlots_sumsPrices() {
        // arrange: two contiguous slots Oct1-2 (2 days) price 10, Oct3-3 (1 day) price
        // 20 => total 40
        AvailabilityDto s1 = AvailabilityDto.builder()
                .id("s1")
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 2))
                .price(BigDecimal.valueOf(10))
                .status("AVAILABLE")
                .build();

        AvailabilityDto s2 = AvailabilityDto.builder()
                .id("s2")
                .startDate(LocalDate.of(2025, 10, 3))
                .endDate(LocalDate.of(2025, 10, 3))
                .price(BigDecimal.valueOf(20))
                .status("AVAILABLE")
                .build();

        AccommodationDocument doc = AccommodationDocument.builder()
                .id("doc5")
                .pricingMode("PER_ACCOMMODATION")
                .minGuests(1)
                .maxGuests(4)
                .availabilities(List.of(s1, s2))
                .build();

        SearchHits<AccommodationDocument> hits = mock(SearchHits.class);
        SearchHit<AccommodationDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hits.stream()).thenReturn(Stream.of(hit));
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn(hits);

        SearchRequest request = new SearchRequest();
        request.setStartDate(LocalDate.of(2025, 10, 1));
        request.setEndDate(LocalDate.of(2025, 10, 3));
        request.setGuests(2);

        // act
        var results = searchService.searchAccommodations(request);

        // assert
        assertNotNull(results);
        assertEquals(1, results.size());
        var r = results.getFirst();
        assertEquals(new BigDecimal("40"), r.getTotalPrice());
    }

    @Test
    void searchAccommodations_noDates_noAvailability_unitPriceZero() {
        // arrange: no availabilities -> unitPrice stays zero
        AccommodationDocument doc = AccommodationDocument.builder()
                .id("doc6")
                .pricingMode("PER_ACCOMMODATION")
                .minGuests(1)
                .maxGuests(4)
                .availabilities(List.of())
                .build();

        SearchHits<AccommodationDocument> hits = mock(SearchHits.class);
        SearchHit<AccommodationDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hits.stream()).thenReturn(Stream.of(hit));
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn(hits);

        SearchRequest request = new SearchRequest();
        request.setGuests(1);

        // act
        var results = searchService.searchAccommodations(request);

        // assert
        assertNotNull(results);
        assertEquals(1, results.size());
        var r = results.getFirst();
        assertEquals(BigDecimal.ZERO, r.getUnitPrice());
        assertEquals(BigDecimal.ZERO, r.getTotalPrice());
    }

    @Test
    void searchAccommodations_withNullLocationAndNullGuests_returnsResults() {
        // arrange: ensure branches for null location and null guests are executed
        AvailabilityDto availability = AvailabilityDto.builder()
                .id("a1")
                .startDate(LocalDate.of(2025, 11, 1))
                .endDate(LocalDate.of(2025, 11, 2))
                .price(BigDecimal.valueOf(30))
                .status("AVAILABLE")
                .build();

        AccommodationDocument doc = AccommodationDocument.builder()
                .id("doc7")
                .pricingMode("PER_ACCOMMODATION")
                .minGuests(1)
                .maxGuests(4)
                .availabilities(List.of(availability))
                .build();

        SearchHits<AccommodationDocument> hits = mock(SearchHits.class);
        SearchHit<AccommodationDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hits.stream()).thenReturn(Stream.of(hit));
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn(hits);

        SearchRequest request = new SearchRequest();
        request.setLocation(null);
        request.setGuests(null);

        // act
        var results = searchService.searchAccommodations(request);

        // assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void searchAccommodations_withBlankLocation_skipsLocationCriteria() {
        // arrange: blank location should be treated as not provided
        AvailabilityDto availability = AvailabilityDto.builder()
                .id("a2")
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 2))
                .price(BigDecimal.valueOf(40))
                .status("AVAILABLE")
                .build();

        AccommodationDocument doc = AccommodationDocument.builder()
                .id("doc8")
                .pricingMode("PER_ACCOMMODATION")
                .minGuests(1)
                .maxGuests(4)
                .availabilities(List.of(availability))
                .build();

        SearchHits<AccommodationDocument> hits = mock(SearchHits.class);
        SearchHit<AccommodationDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hits.stream()).thenReturn(Stream.of(hit));
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn(hits);

        SearchRequest request = new SearchRequest();
        request.setLocation("   ");
        request.setGuests(1);

        // act
        var results = searchService.searchAccommodations(request);

        // assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void searchAccommodations_dateRange_nonAvailableSlots_filteredOut() {
        // arrange: slot status not AVAILABLE -> validSlots empty -> filtered out for
        // date range
        AvailabilityDto availability = AvailabilityDto.builder()
                .id("a3")
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 3))
                .price(BigDecimal.valueOf(15))
                .status("BOOKED")
                .build();

        AccommodationDocument doc = AccommodationDocument.builder()
                .id("doc9")
                .pricingMode("PER_ACCOMMODATION")
                .minGuests(1)
                .maxGuests(4)
                .availabilities(List.of(availability))
                .build();

        SearchHits<AccommodationDocument> hits = mock(SearchHits.class);
        SearchHit<AccommodationDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hits.stream()).thenReturn(Stream.of(hit));
        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(AccommodationDocument.class))).thenReturn(hits);

        SearchRequest request = new SearchRequest();
        request.setStartDate(LocalDate.of(2025, 10, 1));
        request.setEndDate(LocalDate.of(2025, 10, 3));
        request.setGuests(2);

        // act
        var results = searchService.searchAccommodations(request);

        // assert: should be filtered out because validSlots is empty
        assertNotNull(results);
        assertEquals(0, results.size());
    }

}
