package com.codeforge.domain;

/**
 * The languages a solution can be written in.
 *
 * <p>Adding one takes a {@code LanguageSupport} component, a Judge0 id under
 * {@code codeforge.execution.judge0.language-ids}, and one step outside the
 * code: {@code submissions.language} and {@code editorial_solutions.language}
 * are MySQL {@code ENUM} columns, and {@code ddl-auto: update} never alters an
 * existing column. On a database that already exists, both need an
 * {@code ALTER TABLE ... MODIFY language ENUM(...) NOT NULL} listing every
 * constant alphabetically, the order Hibernate creates them in — otherwise every
 * submission in the new language fails to insert.
 */
public enum Language {
    JAVA,
    PYTHON,
    JAVASCRIPT,
    TYPESCRIPT,
    CPP
}
