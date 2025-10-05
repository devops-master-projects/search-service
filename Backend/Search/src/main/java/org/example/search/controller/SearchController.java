package org.example.search.controller;

import lombok.RequiredArgsConstructor;
import org.example.search.dto.SearchRequest;
import org.example.search.dto.SearchResponse;
import org.example.search.model.AccommodationDocument;
import org.example.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public List<AccommodationDocument> search(
            @RequestParam String location,
            @RequestParam Integer guests
    ) {
        return searchService.search(location, guests);
    }

    @PostMapping
    public ResponseEntity<List<SearchResponse>> search(@RequestBody SearchRequest request) {
        return ResponseEntity.ok(searchService.searchAccommodations(request));
    }

    @GetMapping("/all")
    public List<AccommodationDocument> getAllAccommodations() {
        return searchService.findAll();
    }

}
