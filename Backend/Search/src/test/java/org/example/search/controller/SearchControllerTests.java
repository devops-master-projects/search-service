package org.example.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.search.dto.SearchRequest;
import org.example.search.dto.SearchResponse;
import org.example.search.model.AccommodationDocument;
import org.example.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SearchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SearchService searchService;

    @Test
    void getSearchEndpoint_returnsDocuments() throws Exception {
        AccommodationDocument doc = AccommodationDocument.builder()
                .id("1")
                .name("Place One")
                .description("desc")
                .build();

        Mockito.when(searchService.search(eq("City"), eq(2))).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/search")
                        .param("location", "City")
                        .param("guests", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("1")))
                .andExpect(jsonPath("$[0].name", is("Place One")));
    }

    @Test
    void postSearchEndpoint_returnsSearchResponses() throws Exception {
        SearchRequest req = new SearchRequest();
        req.setLocation("City");
        req.setGuests(2);

        SearchResponse resp = SearchResponse.builder()
                .id("r1")
                .name("Res1")
                .unitPrice(BigDecimal.valueOf(50))
                .totalPrice(BigDecimal.valueOf(100))
                .build();

        Mockito.when(searchService.searchAccommodations(any(SearchRequest.class))).thenReturn(List.of(resp));

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("r1")))
                .andExpect(jsonPath("$[0].unitPrice", is(50)));
    }

    @Test
    void getAllEndpoint_returnsAllAccommodations() throws Exception {
        AccommodationDocument a = AccommodationDocument.builder()
                .id("a1")
                .name("A1")
                .build();

        Mockito.when(searchService.findAll()).thenReturn(List.of(a));

        mockMvc.perform(get("/api/search/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("a1")));
    }

}
