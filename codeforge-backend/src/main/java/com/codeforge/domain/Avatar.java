package com.codeforge.domain;

/**
 * The fixed catalogue of profile avatars a user may pick from.
 *
 * <p>Only the choice is stored; the artwork itself is drawn by the frontend from
 * this identifier. That keeps avatars free of any upload, storage or moderation
 * concern, and means restyling them ships without a data migration.
 *
 * <p>Every account has one: it is chosen during registration and can be changed
 * from the profile page, so there is no "no avatar yet" state to design around.
 */
public enum Avatar {
    FORGE,
    CIRCUIT,
    NEBULA,
    PRISM,
    VERTEX,
    CIPHER,
    LATTICE,
    PULSE,
    ORBIT,
    GLITCH
}
