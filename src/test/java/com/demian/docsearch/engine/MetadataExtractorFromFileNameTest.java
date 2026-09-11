package com.demian.docsearch.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataExtractorFromFileNameTest {

    @Test
    void testExtractYear() {
        assertThat(MetadataExtractorFromFileName.extractYear("The.Godfather.Part.III.1990.1080p.mp4")).isEqualTo(1990);
        assertThat(MetadataExtractorFromFileName.extractYear("Spring Boot in Action 2024.pdf")).isEqualTo(2024);
        assertThat(MetadataExtractorFromFileName.extractYear("Classic_Track.mp3")).isEqualTo(0);
    }

    @Test
    void testExtractPublisher() {
        assertThat(MetadataExtractorFromFileName.extractPublisher("Manning-Spring in Action.pdf")).isEqualTo("manning");
        assertThat(MetadataExtractorFromFileName.extractPublisher("O'Reilly - Learning Python.epub")).isEqualTo("o'reilly");
        assertThat(MetadataExtractorFromFileName.extractPublisher("some_random_doc.pdf")).isEqualTo("unknown");
    }
}
