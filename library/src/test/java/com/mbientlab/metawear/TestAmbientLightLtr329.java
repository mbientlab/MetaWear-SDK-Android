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
import com.mbientlab.metawear.module.AmbientLightLtr329;
import com.mbientlab.metawear.module.AmbientLightLtr329.Gain;
import com.mbientlab.metawear.module.AmbientLightLtr329.IntegrationTime;
import com.mbientlab.metawear.module.AmbientLightLtr329.MeasurementRate;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by etsai on 10/2/16.
 */

public class TestAmbientLightLtr329 extends UnitTestBase {
    private AmbientLightLtr329 alsLtr329;

    public Task<Void> setup() {
        junitPlatform.boardInfo = new MetaWearBoardInfo(AmbientLightLtr329.class);
        return connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            alsLtr329 = mwBoard.getModule(AmbientLightLtr329.class);
        });
    }

    @Test
    public void start() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x14, 0x01, 0x01};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            alsLtr329.illuminance().start();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void stop() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x14, 0x01, 0x00};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            alsLtr329.illuminance().stop();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void setGain() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[][] expected = new byte[][] {
                {0x14, 0x02, 0x00, 0x03},
                {0x14, 0x02, 0x04, 0x03},
                {0x14, 0x02, 0x08, 0x03},
                {0x14, 0x02, 0x0c, 0x03},
                {0x14, 0x02, 0x18, 0x03},
                {0x14, 0x02, 0x1c, 0x03}
        };

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            for(Gain it: Gain.values()) {
                alsLtr329.configure()
                        .gain(it)
                        .commit();
            }

            assertArrayEquals(expected, junitPlatform.getCommands());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void setIntegrationTime() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[][] expected= new byte[][] {
                {0x14, 0x02, 0x00, 0x0b},
                {0x14, 0x02, 0x00, 0x03},
                {0x14, 0x02, 0x00, 0x23},
                {0x14, 0x02, 0x00, 0x13},
                {0x14, 0x02, 0x00, 0x2b},
                {0x14, 0x02, 0x00, 0x33},
                {0x14, 0x02, 0x00, 0x3b},
                {0x14, 0x02, 0x00, 0x1b}
        };

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            for(IntegrationTime it: IntegrationTime.values()) {
                alsLtr329.configure()
                        .integrationTime(it)
                        .commit();
            }

            assertArrayEquals(expected, junitPlatform.getCommands());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void setMeasurementRate() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[][] expected= new byte[][] {
                {0x14, 0x02, 0x00, 0x00},
                {0x14, 0x02, 0x00, 0x01},
                {0x14, 0x02, 0x00, 0x02},
                {0x14, 0x02, 0x00, 0x03},
                {0x14, 0x02, 0x00, 0x04},
                {0x14, 0x02, 0x00, 0x05}
        };

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            for(MeasurementRate it: MeasurementRate.values()) {
                alsLtr329.configure()
                        .measurementRate(it)
                        .commit();
            }

            assertArrayEquals(expected, junitPlatform.getCommands());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void setAll() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {0x14, 0x02, 0x0c, 0x28};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            alsLtr329.configure()
                    .gain(Gain.LTR329_8X)
                    .integrationTime(IntegrationTime.LTR329_TIME_250MS)
                    .measurementRate(MeasurementRate.LTR329_RATE_50MS)
                    .commit();

            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void handleData() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        float expected = 11571.949f;
        final Capture<Float> actual = new Capture<>();

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            alsLtr329.illuminance().addRouteAsync(source -> source.stream((data, env) -> actual.set(data.value(Float.class))))
                    .addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                        alsLtr329.illuminance().start();
                        sendMockResponse(new byte[] {0x14, 0x03, (byte) 0xed, (byte) 0x92, (byte) 0xb0, 0x00});

                        assertEquals(expected, actual.get(), 0.001f);
                        doneSignal.countDown();
                    });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}