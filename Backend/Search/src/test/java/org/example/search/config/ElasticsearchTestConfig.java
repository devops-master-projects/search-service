package org.example.search.config;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class ElasticsearchTestConfig {

    @Bean(destroyMethod = "stop")
    public ElasticsearchContainer elasticsearchContainer() {
        ElasticsearchContainer container = new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0")
        ).withEnv("discovery.type", "single-node");

        container.start();
        System.setProperty("spring.elasticsearch.uris", container.getHttpHostAddress());

        return container;
    }
}
