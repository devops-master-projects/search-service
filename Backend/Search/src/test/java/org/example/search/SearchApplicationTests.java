package org.example.search;

import org.example.search.config.ElasticsearchTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        exclude = {
                org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration.class,
                org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration.class
        }
)
@Import(ElasticsearchTestConfig.class)
@ActiveProfiles("test")
class SearchApplicationTests {
    @Test
    void contextLoads() {
    }
}
