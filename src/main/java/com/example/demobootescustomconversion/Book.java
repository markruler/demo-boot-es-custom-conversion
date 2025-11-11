package com.example.demobootescustomconversion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "books")
public class Book {
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String author;

    @Field(type = FieldType.Integer)
    private Integer publicationYear;

    @Field(type = FieldType.Double)
    private Double price;

    /**
     * @see ElasticsearchConfig#FORMATTER
     * @see ElasticsearchConfig#LocalDateTimeToStringConverter
     * @see ElasticsearchConfig#StringToLocalDateTimeConverter
     */
    @Field(type = FieldType.Keyword)
    private LocalDateTime date;

    @Field(type = FieldType.Date_Nanos, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSSSS"})
    private String dateNanos;
}

