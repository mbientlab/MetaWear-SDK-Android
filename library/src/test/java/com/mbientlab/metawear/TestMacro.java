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
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Macro;
import com.mbientlab.metawear.module.Switch;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by etsai on 11/30/16.
 */

public class TestMacro extends UnitTestBase {
    private Macro macro;

    public Task<Void> setup() {
        junitPlatform.firmware = "1.2.3";
        junitPlatform.addCustomModuleInfo(new byte[] {0x09, (byte) 0x80, 0x00, 0x01, 0x1c});
        junitPlatform.boardInfo= new MetaWearBoardInfo(Switch.class, Led.class, AccelerometerBmi160.class, Macro.class);
        return connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            macro = mwBoard.getModule(Macro.class);
        });
    }

    @Test
    public void ledOnBoot() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[][] expected = new byte[][] {
                {0x02, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, 0x05},
                {0x02, 0x01, 0x01},
                {0x0f, 0x02, 0x01},
                {0x0f, 0x03, 0x02, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, 0x05},
                {0x0f, 0x03, 0x02, 0x01, 0x01},
                {0x0f, 0x04}
        };

        setup().continueWithTask(IMMEDIATE_EXECUTOR, ignored -> {
            final Led led = mwBoard.getModule(Led.class);
            macro.startRecord();
            led.editPattern(Led.Color.BLUE)
                    .riseTime((short) 0).pulseDuration((short) 1000)
                    .repeatCount((byte) 5).highTime((short) 500)
                    .highIntensity((byte) 16).lowIntensity((byte) 16)
                    .commit();
            led.play();
            return macro.endRecordAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                assertArrayEquals(expected, junitPlatform.getCommands());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void freefallOnBoot() throws InterruptedException {
        byte[][] expected = new byte[][] {
                {0x03, 0x03, 0x28, 0x0c},
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x01},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x20, 0x03, 0x05, 0x10},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x0d, 0x09, 0x66, 0x02, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x02, 0x00, 0x06, 0x01, (byte) 0xff},
                {0x09, 0x02, 0x09, 0x03, 0x02, 0x00, 0x06, 0x01, 0x01},
                {0x09, 0x03, 0x01},
                {0x09, 0x07, 0x03, 0x01},
                {0x09, 0x03, 0x01},
                {0x09, 0x07, 0x04, 0x01},
                {0x03, 0x02, 0x01, 0x00},
                {0x03, 0x01, 0x01},
                {0x0f, 0x02, 0x01},
                {0x0f, 0x03, 0x03, 0x03, 0x28, 0x0c},
                {0x0f, 0x03, 0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x01},
                {0x0f, 0x03, 0x09, 0x02, 0x09, 0x03, 0x00, 0x20, 0x03, 0x05, 0x10},
                {0x0f, 0x03, 0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x0d, 0x09, 0x66, 0x02, 0x00, 0x00, 0x00, 0x00},
                {0x0f, 0x03, 0x09, 0x02, 0x09, 0x03, 0x02, 0x00, 0x06, 0x01, (byte) 0xff},
                {0x0f, 0x03, 0x09, 0x02, 0x09, 0x03, 0x02, 0x00, 0x06, 0x01, 0x01},
                {0x0f, 0x03, 0x09, 0x03, 0x01},
                {0x0f, 0x03, 0x09, 0x07, 0x03, 0x01},
                {0x0f, 0x03, 0x09, 0x03, 0x01},
                {0x0f, 0x03, 0x09, 0x07, 0x04, 0x01},
                {0x0f, 0x03, 0x03, 0x02, 0x01, 0x00},
                {0x0f, 0x03, 0x03, 0x01, 0x01},
                {0x0f, 0x04}
        };
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            final Accelerometer accelerometer = mwBoard.getModule(Accelerometer.class);

            macro.startRecord();
            accelerometer.configure()
                    .range(16f)
                    .commit();
            accelerometer.acceleration().addRouteAsync(source -> source.map(Function1.RSS).lowpass((byte) 16).filter(ThresholdOutput.BINARY, 0.3f)
                            .multicast()
                            .to().filter(Comparison.EQ, -1).stream(null)
                            .to().filter(Comparison.EQ, 1).stream(null)
                            .end())
                    .continueWithTask(IMMEDIATE_EXECUTOR, task -> {
                        accelerometer.acceleration().start();
                        accelerometer.start();
                        return macro.endRecordAsync();

                    }).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored2 -> {
                        assertArrayEquals(expected, junitPlatform.getCommands());
                        doneSignal.countDown();
                    });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void ledSwitch() throws InterruptedException {
        byte[][] expected = new byte[][] {
                {0x09, 0x02, 0x01, 0x01, (byte) 0xff, 0x00, 0x02, 0x13},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x60, 0x09, 0x0f, 0x04, 0x02, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x06, 0x01, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x06, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x03, 0x0f},
                {0x0a, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, (byte) 0xff},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x01, 0x01},
                {0x0a, 0x03, 0x01},
                {0x0a, 0x02, 0x09, 0x03, 0x03, 0x02, 0x02, 0x01},
                {0x0a, 0x03, 0x01},
                {0x0f, 0x02, 0x00},
                {0x0f, 0x03, 0x09, 0x02, 0x01, 0x01, (byte) 0xff, 0x00, 0x02, 0x13},
                {0x0f, 0x03, 0x09, 0x02, 0x09, 0x03, 0x00, 0x60, 0x09, 0x0f, 0x04, 0x02, 0x00, 0x00, 0x00, 0x00},
                {0x0f, 0x03, 0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x06, 0x01, 0x00, 0x00, 0x00},
                {0x0f, 0x03, 0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x06, 0x00, 0x00, 0x00, 0x00},
                {0x0f, 0x03, 0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x03, 0x0f},
                {0x0f, 0x03, 0x0a, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, (byte) 0xff},
                {0x0f, 0x03, 0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x01, 0x01},
                {0x0f, 0x03, 0x0a, 0x03, 0x01},
                {0x0f, 0x03, 0x0a, 0x02, 0x09, 0x03, 0x03, 0x02, 0x02, 0x01},
                {0x0f, 0x03, 0x0a, 0x03, 0x01},
                {0x0f, 0x04}
        };

        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            macro.startRecord(false);
            RouteCreator.createLedController(mwBoard)
                .continueWithTask(IMMEDIATE_EXECUTOR, task -> {
                    return macro.endRecordAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored2 -> {
                        assertArrayEquals(expected, junitPlatform.getCommands());
                        doneSignal.countDown();
                    });
                });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
