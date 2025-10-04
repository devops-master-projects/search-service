package org.example.search.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.search.dto.AccommodationEvent;
import org.example.search.dto.AvailabilityDto;
import org.example.search.dto.AvailabilityEvent;
import org.example.search.model.AccommodationDocument;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccommodationEventConsumer {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "accommodation-events", groupId = "search-service")
    public void consumeAccommodationEvent(String message) {
        try {
            AccommodationEvent event = objectMapper.readValue(message, AccommodationEvent.class);
            System.out.println("Received event type: " + event.getEventType());

            if ("AccommodationCreated".equals(event.getEventType())) {
                // kreiranje novog dokumenta
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
                System.out.println("Indexed accommodation: " + doc.getId() + " [Created]");
            }
            else if ("AccommodationUpdated".equals(event.getEventType())) {
                Map<String, Object> updateFields = new HashMap<>();
                updateFields.put("name", event.getName());
                updateFields.put("description", event.getDescription());
                updateFields.put("minGuests", event.getMinGuests());
                updateFields.put("maxGuests", event.getMaxGuests());
                updateFields.put("autoConfirm", event.isAutoConfirm());
                updateFields.put("pricingMode", event.getPricingMode());
                updateFields.put("location", event.getLocation());
                updateFields.put("amenities", event.getAmenities());
                updateFields.put("photos", event.getPhotos());
                Map<String, Object> partialUpdate = objectMapper.convertValue(event, Map.class);
                partialUpdate.remove("id");

                Document document = Document.create();
                document.putAll(partialUpdate);

                UpdateQuery updateQuery = UpdateQuery.builder(event.getId())
                        .withDocument(document)
                        .withDocAsUpsert(true) // ako ne postoji, napravi
                        .build();


                elasticsearchOperations.update(updateQuery, elasticsearchOperations.getIndexCoordinatesFor(AccommodationDocument.class));
                System.out.println("Updated accommodation: " + event.getId());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @KafkaListener(topics = "availability-events", groupId = "search-service")
    public void consumeAvailabilityEvent(String message) {
        try {
            AvailabilityEvent event = objectMapper.readValue(
                    objectMapper.readTree(message).asText(),
                    AvailabilityEvent.class
            );
            AccommodationDocument doc = elasticsearchOperations.get(event.getAccommodationId(), AccommodationDocument.class);
            System.out.println(doc.getLocation());

            if (doc == null) {
                System.out.println("No accommodation found for availability " + event.getAccommodationId());
                return;
            }

            // Obradi event tip
            switch (event.getEventType()) {
                case "AvailabilityCreated":
                case "AvailabilityUpdated":
                case "AvailabilityStatusChanged":
                    AvailabilityDto dto = new AvailabilityDto(
                            event.getId(),
                            event.getStartDate(),
                            event.getEndDate(),
                            event.getPrice(),
                            event.getPriceType(),
                            event.getStatus()
                    );
                    // zameni ako postoji ili dodaj novi
                    doc.getAvailabilities().removeIf(a -> a.getId().equals(event.getId()));
                    doc.getAvailabilities().add(dto);
                    elasticsearchOperations.save(doc);
                    break;
                case "AvailabilityDeleted":
                    doc.getAvailabilities().removeIf(a -> a.getId().equals(event.getId()));
                    elasticsearchOperations.save(doc);
                    break;
            }

            System.out.println("Processed availability event: " + event.getEventType() + " for " + event.getAccommodationId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
