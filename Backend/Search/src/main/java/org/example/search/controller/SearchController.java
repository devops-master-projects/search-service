package org.example.search.controller;

import lombok.RequiredArgsConstructor;
import org.example.search.model.AccommodationDocument;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ElasticsearchOperations elasticsearchOperations;

    @GetMapping
    public List<AccommodationDocument> search(
            @RequestParam String location,
            @RequestParam Integer guests
    ) {
        Criteria criteria = new Criteria("location.city").is(location)
                .and(new Criteria("maxGuests").greaterThanEqual(guests));

        Query query = new CriteriaQuery(criteria);
        return elasticsearchOperations.search(query, AccommodationDocument.class)
                .map(hit -> hit.getContent())
                .toList();
    }
}
