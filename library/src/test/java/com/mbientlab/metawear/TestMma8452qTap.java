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
import static com.mbientlab.metawear.data.Sign.NEGATIVE;
import static com.mbientlab.metawear.data.Sign.POSITIVE;
import static com.mbientlab.metawear.data.TapType.DOUBLE;
import static com.mbientlab.metawear.data.TapType.SINGLE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.android.gms.tasks.Task;
import com.mbientlab.metawear.data.CartesianAxis;
import com.mbientlab.metawear.data.Sign;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.AccelerometerMma8452q.Tap;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by etsai on 12/20/16.
 */

public class TestMma8452qTap extends UnitTestBase {
    private AccelerometerMma8452q mma8452qAcc;

    public Task<Void> setup() {
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerMma8452q.class);
        return connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            mma8452qAcc = mwBoard.getModule(AccelerometerMma8452q.class);
        });
    }

    @Test
    public void start() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x03, 0x0b, 0x01};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            mma8452qAcc.tap().start();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void stop() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x03, 0x0b, 0x00};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            mma8452qAcc.tap().stop();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void configureSingle() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x03, 0x0c, 0x50, 0x00, 0x1f, 0x1f, 0x1f, 0x18, 0x28, 0x3c};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            mma8452qAcc.tap().configure()
                    .enableSingleTap()
                    .axis(CartesianAxis.Z)
                    .commit();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void configureDouble() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x03, 0x0c, 0x60, 0x00, 0x1f, 0x1f, 0x1f, 0x18, 0x28, 0x3c};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            mma8452qAcc.tap().configure()
                    .enableDoubleTap()
                    .axis(CartesianAxis.Z)
                    .commit();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void handleResponse() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        final Tap[] expected = new Tap[] {
                new Tap(new boolean[] {false, false, true}, new Sign[] {POSITIVE, POSITIVE, NEGATIVE}, DOUBLE),
                new Tap(new boolean[] {false, false, true}, new Sign[] {POSITIVE, POSITIVE, POSITIVE}, DOUBLE),
                new Tap(new boolean[] {false, false, true}, new Sign[] {POSITIVE, POSITIVE, NEGATIVE}, SINGLE),
                new Tap(new boolean[] {false, false, true}, new Sign[] {POSITIVE, POSITIVE, POSITIVE}, SINGLE),
        };

        final byte[][] responses = new byte[][] {
                {0x03, 0x0d, (byte) 0xcc},
                {0x03, 0x0d, (byte) 0xc8},
                {0x03, 0x0d, (byte) 0xc4},
                {0x03, 0x0d, (byte) 0xc0}
        };

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {

            final Capture<Tap[]> actual = new Capture<>();

            actual.set(new Tap[4]);
            mma8452qAcc.tap().addRouteAsync(source -> source.stream(new Subscriber() {
                int i = 0;
                @Override
                public void apply(Data data, Object... env) {
                    actual.get()[i] = data.value(Tap.class);
                    i++;
                }
            })).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                for(byte[] it: responses) {
                    sendMockResponse(it);
                }

                assertArrayEquals(expected, actual.get());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
