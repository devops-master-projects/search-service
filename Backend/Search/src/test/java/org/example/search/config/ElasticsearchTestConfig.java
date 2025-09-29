package org.example.search.config;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@TestConfiguration
public class ElasticsearchTestConfig {

    private static final ElasticsearchContainer container =
            new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0"))
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("xpack.security.enrollment.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m");

    static {
        container.setWaitStrategy(
                Wait.forHttp("/")
                        .forPort(9200)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofSeconds(90))
        );
        container.start();
        System.out.println(">>> Elasticsearch Testcontainer running at: " + container.getHttpHostAddress());
    }


    @DynamicPropertySource
    static void registerElasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", container::getHttpHostAddress);
        registry.add("spring.data.elasticsearch.client.reactive.endpoints", container::getHttpHostAddress);
    }

}
