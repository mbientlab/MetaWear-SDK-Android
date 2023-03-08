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
import com.mbientlab.metawear.module.ProximityTsl2671;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by etsai on 10/2/16.
 */

public class TestProximityTsl2671 extends UnitTestBase {
    private ProximityTsl2671 proximity;

    public Task<Void> setup() {
        junitPlatform.boardInfo = new MetaWearBoardInfo(ProximityTsl2671.class);
        return connectToBoardNew().continueWithTask(IMMEDIATE_EXECUTOR, ignored -> {
            proximity = mwBoard.getModule(ProximityTsl2671.class);
            return ignored;
        });
    }

    @Test
    public void read() throws InterruptedException {
        byte[] expected= new byte[] {0x18, (byte) 0x81};
        CountDownLatch doneSignal = new CountDownLatch(1);

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                    proximity.adc().addRouteAsync(source -> source.stream(null));
                    proximity.adc().read();
                    assertArrayEquals(expected, junitPlatform.getLastCommand());
                    doneSignal.countDown();
                });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void readSilent() throws InterruptedException {
        byte[] expected= new byte[] {0x18, (byte) 0xc1};
        CountDownLatch doneSignal = new CountDownLatch(1);

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            proximity.adc().read();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void interpretData() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            short expected = 1522;
            final Capture<Short> actual = new Capture<>();

            proximity.adc().addRouteAsync(source -> source.stream((data, env)
                    -> ((Capture<Short>) env[0]).set(data.value(Short.class))))
                    .addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                        task.setEnvironment(0, actual);
            }).continueWith(IMMEDIATE_EXECUTOR, task -> {
                sendMockResponse(new byte[] { 0x18, (byte) 0x81, (byte) 0xf2, 0x05 });

                assertEquals(expected, actual.get().shortValue());
                doneSignal.countDown();
                return null;
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
