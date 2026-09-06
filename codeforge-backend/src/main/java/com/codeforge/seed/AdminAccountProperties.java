package com.codeforge.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The bootstrap administrator, bound from {@code codeforge.seed.admin.*}.
 *
 * <p>A product where problems are written in the app needs at least one account
 * that can write them, and registration only ever creates ordinary users — so
 * without this there is no way to reach the authoring screens on a fresh
 * database at all.
 *
 * @param enabled false to skip the bootstrap entirely, which is what any
 *     deployment with real users should do once its administrator exists
 * @param username the account to create, or to promote if it is already there
 * @param password used only when the account is created; an existing account's
 *     password is never touched
 */
@ConfigurationProperties(prefix = "codeforge.seed.admin")
public record AdminAccountProperties(boolean enabled, String username, String email, String password) {}
