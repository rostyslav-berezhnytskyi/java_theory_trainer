package com.els.javatheorytrainer.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utility class for generating simple URL-friendly slugs.
 *
 * Example:
 * "Java Core" -> "java-core"
 * "JVM, Memory" -> "jvm-memory"
 */
public final class SlugUtils {

    private SlugUtils() {
    }

    public static String toSlug(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);

        String slug = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-")
                .replaceAll("^-+|-+$", "");

        return slug.isBlank() ? "item" : slug;
    }
}
