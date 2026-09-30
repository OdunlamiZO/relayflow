package com.relayflow.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class HibernateJsonConfigurationTest {

    @Test
    void jsonColumnsReadArraysAsJavaLists() {
        Map<String, Object> properties = new HashMap<>();
        new HibernateJsonConfiguration()
                .jsonFormatMapperCustomizer(new Jackson2ObjectMapperBuilder().build())
                .customize(properties);

        JacksonJsonFormatMapper formatMapper =
                (JacksonJsonFormatMapper) properties.get(MappingSettings.JSON_FORMAT_MAPPER);
        Map<?, ?> graph =
                formatMapper.fromString(
                        "{\"nodes\":[{\"id\":\"trigger-1\"}],\"edges\":[]}", Map.class);

        assertThat(graph.get("nodes")).isInstanceOf(List.class);
        assertThat(graph.get("edges")).isInstanceOf(List.class);
    }
}
