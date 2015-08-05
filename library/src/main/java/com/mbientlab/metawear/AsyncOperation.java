/*
 * Copyright 2014-2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights granted under the terms of a software
 * license agreement between the user who downloaded the software, his/her employer (which must be your
 * employer) and MbientLab Inc, (the "License").  You may not use this Software unless you agree to abide by the
 * terms of the License which can be found at www.mbientlab.com/terms.  The License limits your use, and you
 * acknowledge, that the Software may be modified, copied, and distributed when used in conjunction with an
 * MbientLab Inc, product.  Other than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this Software and/or its documentation for any
 * purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT WARRANTY
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL MBIENTLAB OR ITS LICENSORS BE LIABLE OR
 * OBLIGATED UNDER CONTRACT, NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software, contact MbientLab via email:
 * hello@mbientlab.com.
 */

package com.mbientlab.metawear;

import java.util.concurrent.ExecutionException;

/**
 * Wrapper class around a background operation that will asynchronously notify the user when it is done
 * @author Eric Tsai
 */
public interface AsyncOperation<T> {
    /**
     * Processes the result of the background operation that created this object.  In most cases, the
     * two member functions are mutually exclusive, that is, either one or the other will be called.  In
     * the case that the {@link #success(Object)} function throws an exception, the {@link #failure(Throwable)}
     * function will also be called
     * @param <U> The task's result type
     */
    abstract class CompletionHandler<U> {
        /**
         * Called if the background operation successfully completed and reported a result
         * @param result Result of the task
         */
        public void success(U result) { }

        /**
         * Called if the background operation reported an error
         * @param error Error thrown by the task
         */
        public void failure(Throwable error) { }
    }

    /**
     * Queues a handler to process the result of the background operation.  If the calling object already
     * has the result ready, the handler will be immediately executed and discarded.  Otherwise, the handler
     * will be stored and executed asynchronously when the result is ready.
     * @param handler Handler to process the task's result
     */
    void onComplete(CompletionHandler<T> handler);

    /**
     * Checks the status of the result
     * @return True if the result is available, false otherwise
     */
    boolean isComplete();

    /**
     * Retrieves the result of the operation if available
     * @return Result of the task
     * @throws ExecutionException If the underlying task threw an exception
     * @throws InterruptedException If the result is not ready
     */
    T result() throws ExecutionException, InterruptedException;
}
