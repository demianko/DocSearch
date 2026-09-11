package com.demian.docsearch.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionFilterTest {

    @Test
    void testInclusionsOnly() {
        ExtensionFilter filter = ExtensionFilter.convertFrom("pdf, epub");
        assertThat(filter.matches("pdf")).isTrue();
        assertThat(filter.matches("EPUB")).isTrue();
        assertThat(filter.matches("mobi")).isFalse();
    }

    @Test
    void testExclusionsOnly() {
        ExtensionFilter filter = ExtensionFilter.convertFrom("-tmp, -log");
        assertThat(filter.matches("pdf")).isTrue();
        assertThat(filter.matches("tmp")).isFalse();
        assertThat(filter.matches("LOG")).isFalse();
    }

    @Test
    void testMixedInclusionsAndExclusions() {
        ExtensionFilter filter = ExtensionFilter.convertFrom("pdf, epub, NOT txt");
        assertThat(filter.matches("pdf")).isTrue();
        assertThat(filter.matches("epub")).isTrue();
        assertThat(filter.matches("txt")).isFalse();
        assertThat(filter.matches("docx")).isFalse();
    }

    @Test
    void testBlankQueryMatchesAll() {
        ExtensionFilter filter = ExtensionFilter.convertFrom("");
        assertThat(filter.matches("pdf")).isTrue();
        assertThat(filter.matches("exe")).isTrue();
    }
}
