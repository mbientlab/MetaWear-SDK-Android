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

import static com.mbientlab.metawear.Executors.IMMEDIATE_EXECUTOR;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.android.gms.tasks.Task;
import com.mbientlab.metawear.module.Timer;
import com.mbientlab.metawear.module.Timer.ScheduledTask;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by etsai on 9/18/16.
 */
public class TestTimer extends UnitTestBase {
    private ScheduledTask manager;

    protected Task<ScheduledTask> setupTimer() {
        return mwBoard.getModule(Timer.class).scheduleAsync(3141, (short) 59, true, () -> {
        });
    }

    public Task<Void> setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(Timer.class);
        return connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                    setupTimer().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                        manager = task;
                    });
                    junitPlatform.boardStateSuffix = "timer";
                    try {
                        mwBoard.serialize();
                    } catch (IOException e) {
                        fail(e);
                    }
                }
        );
    }

    @Test
    public void scheduleTasks() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[][] expected= new byte[][] {
                    {0x0c, 0x02, 0x45, 0x0c, 0x00, 0x00, 0x3B, 0x0, 0x0}
            };

            assertArrayEquals(expected, junitPlatform.getCommands());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void start() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[] expected= new byte[] {0x0c, 0x03, 0x0};

            manager.start();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void stop() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[] expected= new byte[] {0x0c, 0x04, 0x0};

            manager.stop();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void remove() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().continueWithTask(IMMEDIATE_EXECUTOR, ignored -> {


                return setupTimer().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                    manager = task;
                }).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored2 -> {

                    junitPlatform.boardStateSuffix = "timer";
                    try {
                        mwBoard.serialize();
                    } catch (IOException e) {
                        fail(e);
                    }

                    byte[][] expected= new byte[][] {
                            {0x0c, 0x05, 0x0}
                    };

                    manager.remove();
                    assertArrayEquals(expected, junitPlatform.getLastCommands(1));
                    doneSignal.countDown();
                });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void timeout() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().continueWithTask(IMMEDIATE_EXECUTOR, ignored -> {
            final Capture<Exception> actual = new Capture<>();

            junitPlatform.maxTimers = 0;
            return mwBoard.getModule(Timer.class).scheduleAsync(3000, false, () -> {
                System.out.println("test");
            }).continueWith(IMMEDIATE_EXECUTOR, task -> {
                actual.set(task.getException());
                assertInstanceOf(TimeoutException.class, actual.get());
                doneSignal.countDown();
                return null;
            });

        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
