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
import com.mbientlab.metawear.module.AccelerometerBmi160;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by etsai on 11/14/16.
 */

public class TestBmi160StepCounterData extends UnitTestBase {
    private AccelerometerBmi160.StepCounterDataProducer counter;

    public Task<Void> setup() {
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class);
        return connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            counter = mwBoard.getModule(AccelerometerBmi160.class).stepCounter();
        });
    }

    @Test
    public void handleResponse() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        final Capture<Short> actual = new Capture<>();
        short expected = 43;

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            counter.addRouteAsync(source -> source.stream((data, env) -> ((Capture<Short>) env[0]).set(data.value(Short.class))))
                    .addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                        task.setEnvironment(0, actual);
                    }).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                        sendMockResponse(new byte[] {0x03, (byte) 0x9a, 0x2b, 0x00});
                        assertEquals(expected, actual.get().shortValue());
                        doneSignal.countDown();
                    });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void read() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x03, (byte) 0x9a};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            counter.addRouteAsync(source -> source.stream(null)).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                counter.read();
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                assertArrayEquals(expected, junitPlatform.getLastCommand());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void silentRead() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x03, (byte) 0xda};
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            counter.read();

            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
