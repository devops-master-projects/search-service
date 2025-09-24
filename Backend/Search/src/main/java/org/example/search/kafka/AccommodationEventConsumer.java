package org.example.search.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.search.dto.AccommodationEvent;
import org.example.search.model.AccommodationDocument;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Service
@RequiredArgsConstructor
public class AccommodationEventConsumer {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "accommodation-events", groupId = "search-service")
    public void consumeAccommodationEvent(String message) {
        try {
            AccommodationEvent event = objectMapper.readValue(message, AccommodationEvent.class);

            AccommodationDocument doc = AccommodationDocument.builder()
                    .id(event.getId())
                    .name(event.getName())
                    .description(event.getDescription())
                    .minGuests(event.getMinGuests())
                    .maxGuests(event.getMaxGuests())
                    .autoConfirm(event.isAutoConfirm())
                    .pricingMode(event.getPricingMode())
                    .location(event.getLocation())
                    .amenities(event.getAmenities())
                    .photos(event.getPhotos())
                    .build();

            elasticsearchOperations.save(doc);
            System.out.println("Indexed accommodation: " + doc.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
