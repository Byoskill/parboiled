package org.parboiled.common;

public class ParboiledException extends RuntimeException {
    public ParboiledException(final String message, final Throwable t) {
        super(message, t);
    }

    public ParboiledException(final String message) {
        super(message);
    }
}
