package org.example.search.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.search.dto.AccommodationEvent;
import org.example.search.dto.AvailabilityDto;
import org.example.search.dto.AvailabilityEvent;
import org.example.search.model.AccommodationDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class AccommodationEventConsumerTests {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private ObjectMapper objectMapper;

    private AccommodationEventConsumer consumer;

    @Captor
    private ArgumentCaptor<AccommodationDocument> docCaptor;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        // real mapper with JavaTime support for LocalDate
        objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new AccommodationEventConsumer(elasticsearchOperations, objectMapper);
    }

    @Test
    void consumeAccommodationCreated_indexesNewDocument() throws Exception {
        AccommodationEvent event = new AccommodationEvent();
        event.setEventType("AccommodationCreated");
        event.setId("acc1");
        event.setName("Name");
        event.setDescription("Desc");
        event.setMinGuests(1);
        event.setMaxGuests(4);

        String json = objectMapper.writeValueAsString(event);
        consumer.consumeAccommodationEvent(json);

        verify(elasticsearchOperations, times(1)).save(docCaptor.capture());
        AccommodationDocument saved = docCaptor.getValue();
        assertEquals("acc1", saved.getId());
        assertEquals("Name", saved.getName());
    }

    @Test
    void consumeAccommodationUpdated_callsUpdate() throws Exception {
        AccommodationEvent event = new AccommodationEvent();
        event.setEventType("AccommodationUpdated");
        event.setId("u1");
        event.setName("NewName");

        String json = objectMapper.writeValueAsString(event);

        consumer.consumeAccommodationEvent(json);

        verify(elasticsearchOperations, times(1)).update(any(UpdateQuery.class), any());
    }

    @Test
    void consumeAvailabilityCreated_addsAvailabilityAndSaves() throws Exception {
        AvailabilityEvent event = new AvailabilityEvent();
        event.setEventType("AvailabilityCreated");
        event.setId("av1");
        event.setAccommodationId("acc1");
        event.setStartDate(LocalDate.of(2025, 10, 1));
        event.setEndDate(LocalDate.of(2025, 10, 3));
        event.setPrice(BigDecimal.valueOf(10));
        event.setPriceType("PER_NIGHT");
        event.setStatus("AVAILABLE");

        String inner = objectMapper.writeValueAsString(event);
        String wrapper = objectMapper.writeValueAsString(inner); // JSON string containing the inner JSON

        AccommodationDocument existing = AccommodationDocument.builder()
                .id("acc1")
                .availabilities(new java.util.ArrayList<>())
                .build();

        when(elasticsearchOperations.get("acc1", AccommodationDocument.class)).thenReturn(existing);

        consumer.consumeAvailabilityEvent(wrapper);

        verify(elasticsearchOperations, times(1)).save(any(AccommodationDocument.class));
    }

    @Test
    void consumeAvailabilityDeleted_removesAvailabilityAndSaves() throws Exception {
        AvailabilityEvent event = new AvailabilityEvent();
        event.setEventType("AvailabilityDeleted");
        event.setId("av1");
        event.setAccommodationId("acc1");

        String inner = objectMapper.writeValueAsString(event);
        String wrapper = objectMapper.writeValueAsString(inner);

        AvailabilityDto dto = new AvailabilityDto("av1", LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 3),
                BigDecimal.TEN, "PER_NIGHT", "AVAILABLE");
        AccommodationDocument existing = AccommodationDocument.builder()
                .id("acc1")
                .availabilities(new java.util.ArrayList<>())
                .build();
        existing.getAvailabilities().add(dto);

        when(elasticsearchOperations.get("acc1", AccommodationDocument.class)).thenReturn(existing);

        consumer.consumeAvailabilityEvent(wrapper);

        verify(elasticsearchOperations, times(1)).save(any(AccommodationDocument.class));
        assertTrue(existing.getAvailabilities().isEmpty());
    }

}
