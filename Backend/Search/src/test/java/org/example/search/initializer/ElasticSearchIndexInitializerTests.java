package org.example.search.initializer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.search.model.AccommodationDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

public class ElasticSearchIndexInitializerTests {
    
    private ElasticsearchOperations esOps;
    private IndexOperations indexOps;
    private ElasticsearchIndexInitializer initializer;

    @BeforeEach
    void setUp() {
        esOps = mock(ElasticsearchOperations.class);
        indexOps = mock(IndexOperations.class);
        when(esOps.indexOps(AccommodationDocument.class)).thenReturn(indexOps);

        initializer = new ElasticsearchIndexInitializer(esOps);
    }

    @Test
    void createIndexIfNotExists_createsWhenMissing() {
        when(indexOps.exists()).thenReturn(false);

        initializer.createIndexIfNotExists();

        verify(indexOps, times(1)).create();
        verify(indexOps, times(1)).putMapping();
    }

    @Test
    void createIndexIfNotExists_noopWhenExists() {
        when(indexOps.exists()).thenReturn(true);

        initializer.createIndexIfNotExists();

        verify(indexOps, never()).create();
        verify(indexOps, never()).putMapping();
    }
}
