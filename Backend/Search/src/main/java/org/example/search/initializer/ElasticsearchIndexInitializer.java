package org.example.search.initializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.search.model.AccommodationDocument;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void createIndexIfNotExists() {
        if (!elasticsearchOperations.indexOps(AccommodationDocument.class).exists()) {
            elasticsearchOperations.indexOps(AccommodationDocument.class).create();
            elasticsearchOperations.indexOps(AccommodationDocument.class).putMapping();
        }
    }
}
