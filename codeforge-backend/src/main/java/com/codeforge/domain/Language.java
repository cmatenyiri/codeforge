package com.codeforge.domain;

/**
 * The languages a solution can be written in.
 *
 * <p>Adding one takes a {@code LanguageSupport} component, a Judge0 id under
 * {@code codeforge.execution.judge0.language-ids}, and a Liquibase changeset:
 * {@code submissions.language} and {@code editorial_solutions.language} are
 * MySQL {@code ENUM} columns, and both need an
 * {@code ALTER TABLE ... MODIFY language ENUM(...) NOT NULL} listing every
 * constant alphabetically — otherwise every submission in the new language
 * fails to insert.
 */
public enum Language {
    JAVA,
    PYTHON,
    JAVASCRIPT,
    TYPESCRIPT,
    CPP
}
