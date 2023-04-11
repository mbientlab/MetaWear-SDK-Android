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
import com.mbientlab.metawear.module.AccelerometerBmi160.SignificantMotionDataProducer;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by etsai on 12/17/16.
 */

public class TestBmi160SignificantMotion extends UnitTestBase {
    private AccelerometerBmi160 bmi160Acc;

    public Task<Void> setup() {
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class);
        return connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            bmi160Acc = mwBoard.getModule(AccelerometerBmi160.class);
        });
    }

    @Test
    public void startSignificantMotion() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x03, 0x09, 0x07, 0x00};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            bmi160Acc.motion(SignificantMotionDataProducer.class).start();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void stopSignificantMotion() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x03, 0x09, 0x00, 0x07};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            bmi160Acc.motion(SignificantMotionDataProducer.class).stop();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void configureSignificantMotion() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x03, 0x0a, 0x00, 0x14, 0x14, 0x36};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            bmi160Acc.motion(SignificantMotionDataProducer.class).configure()
                    .proofTime(AccelerometerBmi160.ProofTime.PT_1_S)
                    .skipTime(AccelerometerBmi160.SkipTime.ST_1_5_S)
                    .commit();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void significantMotionData() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        final byte expected = 0x1;
        final Capture<Byte> actual = new Capture<>();

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            bmi160Acc.motion(SignificantMotionDataProducer.class).addRouteAsync(source -> source.stream((data, env) -> actual.set(data.bytes()[0])))
                    .addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                        sendMockResponse(new byte[] {0x03, 0x0b, 0x01});

                        assertEquals(expected, actual.get().byteValue());
                        doneSignal.countDown();
                    });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
