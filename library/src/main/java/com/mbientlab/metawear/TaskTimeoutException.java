package com.mbientlab.metawear;

import java.util.concurrent.TimeoutException;

/**
 * Variant of the {@link TimeoutException} class that contains a partial result of the task
 * @author Eric Tsai
 */
public class TaskTimeoutException extends TimeoutException {
    private static final long serialVersionUID = -1265719967367246950L;

    /** Partial result of the task */
    public final Object partial;

    /**
     * Creates an exception with the given message and partial result
     * @param message   Message to accompany the exception
     * @param partial   Partial result of the task
     */
    public TaskTimeoutException(String message, Object partial) {
        super(message);

        this.partial = partial;
    }
    /**
     * Creates an exception with the given reason and partial result
     * @param cause     Reason for throwing this exception
     * @param partial   Partial result of the task
     */
    public TaskTimeoutException(Exception cause, Object partial) {
        initCause(cause);
        this.partial = partial;
    }
}
