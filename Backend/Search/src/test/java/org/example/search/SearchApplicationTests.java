package org.example.search;

import org.example.search.config.ElasticsearchTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootTest
@EnableAutoConfiguration(
        exclude = {
                org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration.class,
                org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration.class
        }
)
@Import(org.example.search.config.ElasticsearchTestConfig.class)
@ActiveProfiles("test")
class SearchApplicationTests {

    @Test
    void contextLoads() {
    }
}