package com.codeforge.domain;

/**
 * Application roles. Stored on the user and copied into the JWT so that
 * authorization never needs a database round trip.
 */
public enum Role {

    /** Solves problems and takes interviews. */
    USER,

    /** Everything a USER can do, plus authoring and archiving problems. */
    ADMIN;

    /** Spring Security expects authorities to carry the {@code ROLE_} prefix. */
    public String authority() {
        return "ROLE_" + name();
    }
}
