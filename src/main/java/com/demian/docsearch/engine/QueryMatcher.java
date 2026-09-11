package com.demian.docsearch.engine;

import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.collections4.CollectionUtils;

public class QueryMatcher {
    public static boolean matches(String text, List<SearchRule> rules, List<Pattern> globalExcludes) {
        if (text == null) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(globalExcludes)) {
            for (Pattern ex : globalExcludes) {
                if (!ex.matcher(text).find()) continue;
                return false;
            }
        }
        if (CollectionUtils.isEmpty(rules)) {
            return true;
        }
        for (SearchRule rule : rules) {
            if (!rule.matches(text)) continue;
            return true;
        }
        return false;
    }
}

