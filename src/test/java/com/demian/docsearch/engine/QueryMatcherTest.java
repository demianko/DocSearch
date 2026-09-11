package com.demian.docsearch.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryMatcherTest {

    @Test
    void testMatchesRuleAndExcludes() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("python, java, -draft");

        assertThat(QueryMatcher.matches("Learning Python Guide.pdf", parsed.rules(), parsed.globalExcludes())).isTrue();
        assertThat(QueryMatcher.matches("Java 21 in Action.epub", parsed.rules(), parsed.globalExcludes())).isTrue();
        assertThat(QueryMatcher.matches("Learning Python Draft.pdf", parsed.rules(), parsed.globalExcludes())).isFalse();
        assertThat(QueryMatcher.matches("Ruby Programming.pdf", parsed.rules(), parsed.globalExcludes())).isFalse();
    }

    @Test
    void testEmptyRulesMatchesEverything() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("");
        assertThat(QueryMatcher.matches("AnyFile.txt", parsed.rules(), parsed.globalExcludes())).isTrue();
    }
}
