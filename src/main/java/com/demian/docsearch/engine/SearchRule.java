package com.demian.docsearch.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

public class SearchRule {
    private final Pattern includeRegex;
    private final List<Pattern> excludeRegexes;

    public SearchRule(Pattern includeRegex, List<Pattern> excludeRegexes) {
        this.includeRegex = includeRegex;
        this.excludeRegexes = new ArrayList<Pattern>(ListUtils.emptyIfNull(excludeRegexes));
    }

    public SearchRule(Pattern includeRegex, Pattern excludeRegex) {
        this.includeRegex = includeRegex;
        this.excludeRegexes = new ArrayList<Pattern>();
        if (excludeRegex != null) {
            this.excludeRegexes.add(excludeRegex);
        }
    }

    public SearchRule(Pattern includeRegex) {
        this(includeRegex, (List<Pattern>)null);
    }

    public Pattern getIncludeRegex() {
        return this.includeRegex;
    }

    public List<Pattern> getExcludeRegexes() {
        return Collections.unmodifiableList(this.excludeRegexes);
    }

    public Pattern getExcludeRegex() {
        return CollectionUtils.isEmpty(this.excludeRegexes) ? null : this.excludeRegexes.getFirst();
    }

    public boolean matches(String text) {
        if (text == null) {
            return false;
        }
        if (this.includeRegex != null && !this.includeRegex.matcher(text).find()) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(this.excludeRegexes)) {
            for (Pattern exc : this.excludeRegexes) {
                if (!exc.matcher(text).find()) continue;
                return false;
            }
        }
        return true;
    }
}

