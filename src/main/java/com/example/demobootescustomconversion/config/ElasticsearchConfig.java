package com.example.demobootescustomconversion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ElasticsearchConfig {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    @Bean
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new LocalDateTimeToStringConverter());
        converters.add(new StringToLocalDateTimeConverter());
        return new ElasticsearchCustomConversions(converters);
    }

    /**
     * Entity to Elasticsearch Document
     */
    @WritingConverter
    static class LocalDateTimeToStringConverter implements Converter<LocalDateTime, String> {
        @Override
        public String convert(LocalDateTime source) {
            // Format with microsecond precision (6 digits)
            // DateTimeFormatter's SSSSSS pattern represents milliseconds, so we need to format manually
            int nanoOfSecond = source.getNano();
            int microseconds = nanoOfSecond / 1000; // Convert nanoseconds to microseconds

            String base = source.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            return String.format("%s.%06d", base, microseconds);
        }
    }

    /**
     * Elasticsearch Document to Entity
     */
    @ReadingConverter
    static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            return LocalDateTime.parse(source, FORMATTER);
        }
    }
}
