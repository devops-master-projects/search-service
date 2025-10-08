package org.example.search.service;


import org.example.search.dto.AvailabilityDto;
import org.example.search.dto.SearchRequest;
import org.example.search.dto.SearchResponse;
import org.example.search.model.AccommodationDocument;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;


    public List<AccommodationDocument> search(String location, Integer guests) {
        Criteria criteria = new Criteria("location.city").is(location)
                .and(new Criteria("maxGuests").greaterThanEqual(guests));

        Query query = new CriteriaQuery(criteria);
        return elasticsearchOperations.search(query, AccommodationDocument.class)
                .map(SearchHit::getContent)
                .toList();
    }
    public List<SearchResponse> searchAccommodations(SearchRequest request) {
        Criteria criteria = new Criteria();
        System.out.println(">>> Location from request: " + request.getLocation());
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            criteria = criteria.and(new Criteria("location.city").is(request.getLocation()));
            System.out.println(">>> Criteria after location: " + criteria);
        }

        if (request.getGuests() != null && request.getGuests() > 0) {
            criteria = criteria.and(new Criteria("minGuests").lessThanEqual(request.getGuests()));
            criteria = criteria.and(new Criteria("maxGuests").greaterThanEqual(request.getGuests()));
        }

        criteria = criteria.and(new Criteria("availabilities.status").is("AVAILABLE"));

        Query query = new CriteriaQuery(criteria);
        SearchHits<AccommodationDocument> hits =
                elasticsearchOperations.search(query, AccommodationDocument.class);

        return hits.stream()
                .map(hit -> {
                    AccommodationDocument doc = hit.getContent();

                    BigDecimal totalPrice = BigDecimal.ZERO;
                    BigDecimal unitPrice = BigDecimal.ZERO;

                    if (request.getStartDate() != null && request.getEndDate() != null) {
                        LocalDate start = request.getStartDate();
                        LocalDate end = request.getEndDate();

                        List<AvailabilityDto> validSlots = doc.getAvailabilities().stream()
                                .filter(a -> a.getStatus().equals("AVAILABLE") &&
                                        !a.getEndDate().isBefore(start) &&
                                        !a.getStartDate().isAfter(end))
                                .sorted(Comparator.comparing(AvailabilityDto::getStartDate))
                                .toList();

                        LocalDate cursor = start;

                        for (AvailabilityDto slot : validSlots) {
                            if (slot.getStartDate().isAfter(cursor)) {
                                return null;
                            }

                            LocalDate overlapStart = cursor.isAfter(slot.getStartDate()) ? cursor : slot.getStartDate();
                            LocalDate overlapEnd = end.isBefore(slot.getEndDate()) ? end : slot.getEndDate();

                            long days = ChronoUnit.DAYS.between(overlapStart, overlapEnd.plusDays(1));
                            if (days > 0) {
                                unitPrice = slot.getPrice();

                                if ("PER_PERSON".equalsIgnoreCase(doc.getPricingMode())) {
                                    totalPrice = totalPrice.add(slot.getPrice()
                                            .multiply(BigDecimal.valueOf(days * request.getGuests())));
                                } else { // PER_ACCOMMODATION
                                    totalPrice = totalPrice.add(slot.getPrice()
                                            .multiply(BigDecimal.valueOf(days)));
                                }

                                cursor = overlapEnd.plusDays(1);
                                if (cursor.isAfter(end)) break;
                            }
                        }
                        if (cursor.isBefore(end.plusDays(1))) {
                            return null;
                        }

                    } else {
                        AvailabilityDto availability = doc.getAvailabilities().stream()
                                .filter(a -> a.getStatus().equals("AVAILABLE"))
                                .findFirst()
                                .orElse(null);

                        if (availability != null) {
                            unitPrice = availability.getPrice();
                        }
                    }

                    return SearchResponse.builder()
                            .id(doc.getId())
                            .name(doc.getName())
                            .description(doc.getDescription())
                            .location(doc.getLocation())
                            .photos(doc.getPhotos())
                            .amenities(doc.getAmenities())
                            .minGuests(doc.getMinGuests())
                            .maxGuests(doc.getMaxGuests())
                            .totalPrice(totalPrice)
                            .unitPrice(unitPrice)
                            .pricingMode(doc.getPricingMode())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    public List<AccommodationDocument> findAll() {
        CriteriaQuery query = new CriteriaQuery(new Criteria());

        SearchHits<AccommodationDocument> hits =
                elasticsearchOperations.search(query, AccommodationDocument.class);

        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .sorted(Comparator.comparing(AccommodationDocument::getId))
                .toList();
    }

}
