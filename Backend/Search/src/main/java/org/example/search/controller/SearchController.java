package org.example.search.controller;

import lombok.RequiredArgsConstructor;
import org.example.search.dto.SearchRequest;
import org.example.search.dto.SearchResponse;
import org.example.search.model.AccommodationDocument;
import org.example.search.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam String location,
            @RequestParam Integer guests,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate == null && endDate == null) {
            List<AccommodationDocument> result = searchService.search(location, guests);
            return ResponseEntity.ok(result);
        }

        SearchRequest req = new SearchRequest(location, guests, startDate, endDate);
        List<SearchResponse> result = searchService.searchAccommodations(req);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/all")
    public List<AccommodationDocument> getAllAccommodations() {
        return searchService.findAll();
    }

}
