/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.impl;

import com.mbientlab.metawear.AsyncResult;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by etsai on 6/17/2015.
 */
public class AsyncResultImpl<T> implements AsyncResult<T> {
    private final ConcurrentLinkedQueue<CompletionHandler<T>> handlers= new ConcurrentLinkedQueue<>();
    private final AtomicBoolean completed= new AtomicBoolean(false);
    private T result;
    private Throwable exception;

    @Override
    public void onComplete(CompletionHandler<T> handler) {
        handlers.add(handler);

        if (completed.get()) {
            executeHandlers();
        }
    }

    public void setResult(T result, Throwable exception) {
        if (!completed.get()) {
            this.result= result;
            this.exception= exception;

            completed.set(true);
            executeHandlers();
        }
    }

    private void executeHandlers() {
        while(!handlers.isEmpty()) {
            CompletionHandler<T> next= handlers.poll();

            if (next != null) {
                if (exception != null) {
                    next.failure(exception);
                } else {
                    next.success(result);
                }
            }
        }
    }

    @Override
    public boolean isCompleted() {
        return completed.get();
    }

    @Override
    public T result() throws ExecutionException, InterruptedException {
        if (!completed.get()) {
            throw new InterruptedException("Task not yet completed");
        }

        if (exception != null) {
            throw new ExecutionException("Received exception when executing task", exception);
        }
        return result;
    }
}
