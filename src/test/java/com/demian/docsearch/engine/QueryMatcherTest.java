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

    @Test
    void testMatchesAnyWithCandidateStrings() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("best");

        // "best" is in filename
        assertThat(QueryMatcher.matchesAny(
                java.util.List.of("best_guide.pdf", "C:\\docs", "C:\\docs\\best_guide.pdf"),
                parsed.rules(), parsed.globalExcludes())).isTrue();

        // "best" is only in directory path
        assertThat(QueryMatcher.matchesAny(
                java.util.List.of("guide.pdf", "C:\\docs\\best", "C:\\docs\\best\\guide.pdf"),
                parsed.rules(), parsed.globalExcludes())).isTrue();

        // "best" is not present
        assertThat(QueryMatcher.matchesAny(
                java.util.List.of("guide.pdf", "C:\\docs\\other", "C:\\docs\\other\\guide.pdf"),
                parsed.rules(), parsed.globalExcludes())).isFalse();
    }

    @Test
    void testMatchesAnyWithExclusionInDirectory() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("best NOT draft");

        // "best" in name, but "draft" in directory -> must be excluded
        assertThat(QueryMatcher.matchesAny(
                java.util.List.of("best_guide.pdf", "C:\\draft", "C:\\draft\\best_guide.pdf"),
                parsed.rules(), parsed.globalExcludes())).isFalse();

        // "best" in directory, "draft" in name -> must be excluded
        assertThat(QueryMatcher.matchesAny(
                java.util.List.of("draft_guide.pdf", "C:\\best", "C:\\best\\draft_guide.pdf"),
                parsed.rules(), parsed.globalExcludes())).isFalse();

        // "best" in directory, no draft -> included
        assertThat(QueryMatcher.matchesAny(
                java.util.List.of("guide.pdf", "C:\\best", "C:\\best\\guide.pdf"),
                parsed.rules(), parsed.globalExcludes())).isTrue();
    }

    @Test
    void testMatchesQuotedQuery() {
        QueryParser.ParsedQuery parsed = QueryParser.parse("\"best\"");
        assertThat(QueryMatcher.matchesAny(
                java.util.List.of("guide.pdf", "C:\\best", "C:\\best\\guide.pdf"),
                parsed.rules(), parsed.globalExcludes())).isTrue();
    }

    @Test
    void testMatchesUnclosedQuotes() {
        QueryParser.ParsedQuery parsedLeading = QueryParser.parse("\"good");
        assertThat(QueryMatcher.matchesAny(
                java.util.List.of("video-good.mp4", "C:\\videos", "C:\\videos\\video-good.mp4"),
                parsedLeading.rules(), parsedLeading.globalExcludes())).isTrue();

        QueryParser.ParsedQuery parsedTrailing = QueryParser.parse("good\"");
        assertThat(QueryMatcher.matchesAny(
                java.util.List.of("video-good.mp4", "C:\\videos", "C:\\videos\\video-good.mp4"),
                parsedTrailing.rules(), parsedTrailing.globalExcludes())).isTrue();
    }
}
