package com.relayflow.api.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateJsonConfiguration {

    // Hibernate's default mapper registers feel-engine's Scala module, which reads arrays as Scala
    // lists.
    @Bean
    HibernatePropertiesCustomizer jsonFormatMapperCustomizer(ObjectMapper objectMapper) {
        return properties ->
                properties.put(
                        MappingSettings.JSON_FORMAT_MAPPER,
                        new JacksonJsonFormatMapper(objectMapper));
    }
}
