package org.example.search;

import org.example.search.config.ElasticsearchTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(ElasticsearchTestConfig.class)
@ActiveProfiles("test")
class SearchApplicationTests {
    @Test
    void contextLoads() {
    }
}
