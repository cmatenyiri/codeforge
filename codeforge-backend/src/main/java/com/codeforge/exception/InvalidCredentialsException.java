package com.codeforge.exception;

/**
 * Wrong username or password.
 *
 * <p>Deliberately does not say which of the two was wrong, and is deliberately
 * not a field error: telling a caller that a username exists but the password
 * is wrong hands them a user-enumeration oracle.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
