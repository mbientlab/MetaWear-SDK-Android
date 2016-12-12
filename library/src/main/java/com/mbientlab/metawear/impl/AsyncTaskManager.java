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

package com.mbientlab.metawear.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * Created by etsai on 12/10/16.
 */

class AsyncTaskManager<T> {
    private ScheduledFuture<?> timeoutFuture;
    private final Queue<Tuple3<TaskCompletionSource<T>, Runnable, Long>> taskSources;
    private final String timeoutMessage;
    private final Runnable taskTimeoutAction = new Runnable() {
        @Override
        public void run() {
            taskSources.poll().first.setError(new TimeoutException(timeoutMessage));
            execute(true);
        }
    };
    private final MetaWearBoardPrivate owner;

    public AsyncTaskManager(MetaWearBoardPrivate owner, String timeoutMessage) {
        this.owner = owner;
        this.timeoutMessage = timeoutMessage;
        this.taskSources = new ConcurrentLinkedQueue<>();
    }

    public Task<T> queueTask(long timeout, Runnable task) {
        TaskCompletionSource<T> source= new TaskCompletionSource<>();
        taskSources.add(new Tuple3<>(source, task, timeout));
        execute(false);
        return source.getTask();
    }

    public void cancelTimeout() {
        timeoutFuture.cancel(false);
    }

    public void setResult(T result) {
        taskSources.poll().first.setResult(result);
        execute(true);
    }

    public void setError(Exception error) {
        taskSources.poll().first.setError(error);
        execute(true);
    }

    private void execute(boolean force) {
        if (!taskSources.isEmpty() && (force || taskSources.size() == 1)) {
            taskSources.peek().second.run();
            timeoutFuture = owner.scheduleTask(taskTimeoutAction, taskSources.peek().third);
        }
    }
}
