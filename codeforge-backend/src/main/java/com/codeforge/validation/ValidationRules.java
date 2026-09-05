package com.codeforge.validation;

import com.codeforge.domain.Avatar;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Field-shape constants and primitive checks shared by the validators.
 *
 * <p>These are plain static helpers rather than Bean Validation annotations:
 * uniqueness and cross-field rules need repository access anyway, and splitting
 * "shape" rules into annotations and "real" rules into code would mean two
 * places to look and two error formats to merge.
 */
public final class ValidationRules {

    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 32;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 72; // BCrypt truncates beyond 72 bytes.
    public static final int EMAIL_MAX_LENGTH = 255;

    /** Letters, digits, underscore and hyphen; must start with a letter or digit. */
    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]*$");

    /**
     * Intentionally permissive. Anything stricter rejects valid addresses; the
     * only authoritative check is sending mail, which this product never does.
     */
    public static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");

    private ValidationRules() {}

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Resolves an avatar name against the catalogue, or empty if it is absent or unknown. */
    public static Optional<Avatar> parseAvatar(String value) {
        String name = trimToNull(value);
        if (name == null) {
            return Optional.empty();
        }
        return Arrays.stream(Avatar.values())
                .filter(candidate -> candidate.name().equals(name))
                .findFirst();
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
