package com.codeforge.domain;

import java.util.Locale;

/**
 * The one way a human-readable name becomes a URL segment.
 *
 * <p>Shared rather than reimplemented per caller so that a problem seeded from
 * the catalogue, one written in the app and a tag created alongside it all land
 * on the same slug for the same name — which is what makes "does this already
 * exist?" a question the database can answer.
 */
public final class Slugs {

    private Slugs() {}

    /** Lowercases, and collapses every run of non-alphanumerics into a single hyphen. */
    public static String slugify(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
