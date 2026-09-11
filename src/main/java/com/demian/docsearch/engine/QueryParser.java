package com.demian.docsearch.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

public class QueryParser {
    private static final Pattern NOT_PREFIX = Pattern.compile("^NOT\\s+", 2);
    private static final Pattern INLINE_NOT_SPLIT = Pattern.compile("\\s+NOT\\s+", 2);
    private static final Pattern EXCLUDE_DELIMITERS = Pattern.compile("\\s+(?:and\\s+not|&\\s*not|and|not|&)\\s+", 2);

    public static Pattern termToRegex(String termStr) {
        if (StringUtils.isBlank(termStr)) {
            return Pattern.compile(".*", 2);
        }
        String raw = StringUtils.trimToEmpty(termStr);
        raw = raw.replaceAll("\\s*\\*\\s*", "*").replaceAll("\\s*\\?\\s*", "?");
        String[] branches = StringUtils.split(raw, '|');
        ArrayList<String> branchRegexes = new ArrayList<String>();
        for (String branch : branches) {
            String b = StringUtils.trimToEmpty(branch);
            if (StringUtils.isEmpty(b)) continue;
            StringBuilder branchBuilder = new StringBuilder();
            int len = b.length();
            StringBuilder literal = new StringBuilder();
            for (int i = 0; i < len; ++i) {
                char c = b.charAt(i);
                if (c == '*' || c == '?') {
                    if (!literal.isEmpty()) {
                        branchBuilder.append(QueryParser.quoteWithSpaceWordSeparators(literal.toString()));
                        literal.setLength(0);
                    }
                    branchBuilder.append(c == '*' ? ".*" : ".");
                    continue;
                }
                literal.append(c);
            }
            if (!literal.isEmpty()) {
                branchBuilder.append(QueryParser.quoteWithSpaceWordSeparators(literal.toString()));
            }
            branchRegexes.add(branchBuilder.toString());
        }
        String fullRegex = branchRegexes.size() > 1
                ? "(?:" + StringUtils.join(branchRegexes, "|") + ")"
                : (CollectionUtils.isNotEmpty(branchRegexes) ? branchRegexes.getFirst() : ".*");
        try {
            return Pattern.compile(fullRegex, 2);
        }
        catch (PatternSyntaxException e) {
            return Pattern.compile(Pattern.quote(raw), 2);
        }
    }

    private static String quoteWithSpaceWordSeparators(String text) {
        if (StringUtils.isEmpty(text)) {
            return "";
        }
        boolean leadingSpace = text.startsWith(" ");
        boolean trailingSpace = text.length() > 1 && text.endsWith(" ");
        String trimmed = StringUtils.trimToEmpty(text);
        if (StringUtils.isEmpty(trimmed)) {
            return "[\\s._\\-+]+";
        }
        String[] tokens = StringUtils.split(trimmed);
        StringBuilder sb = new StringBuilder();
        if (leadingSpace) {
            sb.append("[\\s._\\-+]+");
        }
        for (int i = 0; i < tokens.length; ++i) {
            if (i > 0) {
                sb.append("[\\s._\\-+]+");
            }
            sb.append(Pattern.quote(tokens[i]));
        }
        if (trailingSpace) {
            sb.append("[\\s._\\-+]+");
        }
        return sb.toString();
    }

    public static List<Pattern> parseExcludeTerms(String rawExcludeStr) {
        if (StringUtils.isBlank(rawExcludeStr)) {
            return List.of();
        }
        String text = StringUtils.trimToEmpty(rawExcludeStr);
        text = text.replaceAll("\\s+-\\s*", "|SPLIT|");
        Matcher m = EXCLUDE_DELIMITERS.matcher(text);
        text = m.replaceAll("|SPLIT|");
        String[] parts = text.split("\\|SPLIT\\|");
        ArrayList<Pattern> regexes = new ArrayList<Pattern>();
        for (String part : parts) {
            String clean = StringUtils.trimToEmpty(part);
            if (StringUtils.isEmpty(clean) || !StringUtils.isNotEmpty(clean = clean.replaceAll("^(?i:NOT\\s+|-)", "").trim())) continue;
            regexes.add(QueryParser.termToRegex(clean));
        }
        return regexes;
    }

    public static ParsedQuery parse(String queryStr) {
        if (StringUtils.isBlank(queryStr)) {
            return new ParsedQuery(List.of(), List.of());
        }
        String[] rawClauses = StringUtils.split(queryStr, ',');
        ArrayList<SearchRule> rules = new ArrayList<SearchRule>();
        ArrayList<Pattern> globalExcludes = new ArrayList<Pattern>();
        for (String raw : rawClauses) {
            String clean;
            String clause = StringUtils.trimToEmpty(raw);
            if (StringUtils.isEmpty(clause)) continue;
            if (clause.startsWith("-")) {
                clean = StringUtils.trimToEmpty(clause.substring(1));
                if (!StringUtils.isNotEmpty(clean)) continue;
                globalExcludes.addAll(QueryParser.parseExcludeTerms(clean));
                continue;
            }
            if (NOT_PREFIX.matcher(clause).find()) {
                clean = StringUtils.trimToEmpty(NOT_PREFIX.matcher(clause).replaceFirst(""));
                if (!StringUtils.isNotEmpty(clean)) continue;
                globalExcludes.addAll(QueryParser.parseExcludeTerms(clean));
                continue;
            }
            String[] notParts = INLINE_NOT_SPLIT.split(clause, 2);
            if (notParts.length == 2) {
                String incStr = StringUtils.trimToEmpty(notParts[0]);
                String excStr = StringUtils.trimToEmpty(notParts[1]);
                Pattern incRx = StringUtils.isNotEmpty(incStr) ? QueryParser.termToRegex(incStr) : null;
                List<Pattern> excRxs = StringUtils.isNotEmpty(excStr) ? QueryParser.parseExcludeTerms(excStr) : List.of();
                rules.add(new SearchRule(incRx, excRxs));
                continue;
            }
            Pattern incRx = QueryParser.termToRegex(clause);
            rules.add(new SearchRule(incRx));
        }
        return new ParsedQuery(rules, globalExcludes);
    }

    public record ParsedQuery(List<SearchRule> rules, List<Pattern> globalExcludes) {
        public ParsedQuery {
            rules = Collections.unmodifiableList(ListUtils.emptyIfNull(rules));
            globalExcludes = Collections.unmodifiableList(ListUtils.emptyIfNull(globalExcludes));
        }
    }
}

