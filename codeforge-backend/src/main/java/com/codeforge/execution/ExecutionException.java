package com.codeforge.execution;

/** The sandbox could not be reached or did not answer in a usable way. */
public class ExecutionException extends RuntimeException {

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
