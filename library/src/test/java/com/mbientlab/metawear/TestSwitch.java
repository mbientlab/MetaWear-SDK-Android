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

import com.google.android.gms.tasks.Task;
import com.mbientlab.metawear.module.Switch;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by etsai on 9/4/16.
 */
public class TestSwitch extends UnitTestBase {
    private Switch switchModule;

    public Task<Void> setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(Switch.class);
        return connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> switchModule = mwBoard.getModule(Switch.class));
    }

    @Test
    public void subscribe() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().continueWithTask(IMMEDIATE_EXECUTOR, ignored -> {
            return switchModule.state().addRouteAsync(source -> source.stream(null));
        }).continueWithTask(IMMEDIATE_EXECUTOR, ignored -> {
            byte[] expected= new byte[] {0x1, 0x1, 0x1};

            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
            return null;
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void unsubscribe() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[] expected= new byte[] {0x1, 0x1, 0x0};

            switchModule.state().addRouteAsync(source -> source.stream(null))
                    .addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                task.unsubscribe(0);
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                assertArrayEquals(expected, junitPlatform.getLastCommand());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void handlePressed() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {

            final long expected= 1;
            final long[] actual= new long[1];

            switchModule.state().addRouteAsync(source -> source.stream((data, env) -> ((long[]) env[0])[0]= data.value(Long.class)))
                    .addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                task.setEnvironment(0, (Object) actual);
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                        sendMockResponse(new byte[] {0x1, 0x1, 0x1});
                        assertEquals(expected, actual[0]);
                        doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void handleReleased() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            final long expected= 0;
            final long[] actual= new long[1];

            switchModule.state().addRouteAsync(source -> source.stream((data, env) -> ((long[]) env[0])[0]= data.value(Long.class))).continueWith(IMMEDIATE_EXECUTOR, task -> {
                task.getResult().setEnvironment(0, (Object) actual);
                return null;
            });

            sendMockResponse(new byte[] {0x1, 0x1, 0x0});
            assertEquals(expected, actual[0]);
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void readCurrentState() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[] expected = new byte[] {0x01, (byte) 0x81};

            switchModule.readCurrentStateAsync();

            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void readCurrentStateData() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x01, 0x00};
        final byte[] actual = new byte[2];

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {

            Task<Void> readTaskChain = switchModule.readCurrentStateAsync().continueWithTask(IMMEDIATE_EXECUTOR, task -> {
                actual[0] = task.getResult();
                return switchModule.readCurrentStateAsync();
            }).continueWith(IMMEDIATE_EXECUTOR, task -> {
                actual[1] = task.getResult();
                return null;
            });

            sendMockResponse(new byte[] {0x01, (byte) 0x81, 0x1});
            sendMockResponse(new byte[] {0x01, (byte) 0x81, 0x0});

            readTaskChain.addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored2 -> assertArrayEquals(expected, actual));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
