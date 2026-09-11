package com.demian.docsearch.engine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class ExtensionFilter {
    private static final Pattern NOT_PREFIX = Pattern.compile("^NOT\\s+", 2);
    private final Set<String> includeExtensions;
    private final Set<String> excludeExtensions;

    public ExtensionFilter(Set<String> includeExts, Set<String> excludeExts) {
        this.includeExtensions = CollectionUtils.isNotEmpty(includeExts) ? new HashSet<String>(includeExts) : new HashSet();
        this.excludeExtensions = CollectionUtils.isNotEmpty(excludeExts) ? new HashSet<String>(excludeExts) : new HashSet();
    }

    public static ExtensionFilter convertFrom(String fileExtensions) {
        String[] rawExtensionItems;
        HashSet<String> includeExts = new HashSet<String>();
        HashSet<String> excludeExts = new HashSet<String>();
        if (StringUtils.isBlank(fileExtensions)) {
            return new ExtensionFilter(includeExts, excludeExts);
        }
        for (String eachExtension : rawExtensionItems = StringUtils.split(fileExtensions, ',')) {
            String clean = StringUtils.trimToEmpty(eachExtension);
            if (StringUtils.isEmpty(clean)) continue;
            boolean shouldExclude = false;
            if (clean.startsWith("-")) {
                shouldExclude = true;
                clean = StringUtils.trimToEmpty(clean.substring(1));
            } else if (NOT_PREFIX.matcher(clean).find()) {
                shouldExclude = true;
                clean = StringUtils.trimToEmpty(NOT_PREFIX.matcher(clean).replaceFirst(""));
            }
            clean = StringUtils.lowerCase(clean.replaceAll("^[*.]+", "").trim());
            if (StringUtils.isEmpty(clean)) continue;
            if (shouldExclude) {
                excludeExts.add(clean);
                continue;
            }
            includeExts.add(clean);
        }
        return new ExtensionFilter(includeExts, excludeExts);
    }

    public boolean matches(String fileExtension) {
        if (fileExtension == null) {
            return false;
        }
        String normExt = StringUtils.lowerCase(fileExtension.replaceAll("^\\.+", "").trim());
        if (CollectionUtils.isNotEmpty(this.excludeExtensions) && this.excludeExtensions.contains(normExt)) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(this.includeExtensions)) {
            return this.includeExtensions.contains(normExt);
        }
        return true;
    }

    public Set<String> getIncludeExts() {
        return Collections.unmodifiableSet(this.includeExtensions);
    }

    public Set<String> getExcludeExts() {
        return Collections.unmodifiableSet(this.excludeExtensions);
    }
}

