package com.demian.docsearch.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryParserTest {

    @Test
    void testBasicQuery() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("machine learning");
        assertThat(parsed.rules()).hasSize(1);
        assertThat(parsed.rules().getFirst().matches("machine learning in action")).isTrue();
        assertThat(parsed.globalExcludes()).isEmpty();
    }

    @Test
    void testCommaOrSplitting() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("python, java");
        assertThat(parsed.rules()).hasSize(2);
        assertThat(parsed.rules().get(0).matches("learning python")).isTrue();
        assertThat(parsed.rules().get(1).matches("learning java")).isTrue();
    }

    @Test
    void testPipeAlternativeSplitting() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("intro | beginner");
        assertThat(parsed.rules()).hasSize(1);
        assertThat(parsed.rules().getFirst().matches("intro to programming")).isTrue();
        assertThat(parsed.rules().getFirst().matches("beginner guide")).isTrue();
        assertThat(parsed.rules().getFirst().matches("advanced topics")).isFalse();
    }

    @Test
    void testInlineExcludes() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("python NOT draft");
        assertThat(parsed.rules()).hasSize(1);
        assertThat(parsed.rules().getFirst().matches("learning python")).isTrue();
        assertThat(parsed.rules().getFirst().matches("learning python draft")).isFalse();
    }

    @Test
    void testGlobalExcludes() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("-draft, NOT old");
        assertThat(parsed.globalExcludes()).hasSize(2);
    }

    @Test
    void testEmptyQuery() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("   ");
        assertThat(parsed.rules()).isEmpty();
        assertThat(parsed.globalExcludes()).isEmpty();
    }
}
