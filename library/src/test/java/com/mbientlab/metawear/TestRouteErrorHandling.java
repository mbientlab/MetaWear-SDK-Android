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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.android.gms.tasks.Task;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.builder.function.Function2;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Timer;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by etsai on 10/14/16.
 */

public class TestRouteErrorHandling extends UnitTestBase {
    public Task<Void> setup() {
        junitPlatform.firmware= "1.1.3";
        junitPlatform.boardInfo = new MetaWearBoardInfo(Switch.class, Led.class, AccelerometerBmi160.class,
                Temperature.class, Haptic.class, Timer.class);
        junitPlatform.addCustomModuleInfo(new byte[] { 0x11, (byte) 0x80, 0x00, 0x03 });
        junitPlatform.addCustomModuleInfo(new byte[] { 0x05, (byte) 0x80, 0x00, 0x00, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01 });
        return connectToBoard();
    }

    @Test
    public void splitIndexOob() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.split()
                    .index(3)).addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                assertInstanceOf(IllegalRouteOperationException.class, exception);
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void duplicateKey1() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            final Accelerometer.AccelerationDataProducer accData = mwBoard.getModule(Accelerometer.class).acceleration();
            accData.addRouteAsync(source -> source.map(Function1.RMS).name(accData.name())).addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                assertInstanceOf(IllegalRouteOperationException.class, exception);
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void duplicateKey2() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            final Temperature.Sensor sensor1 = mwBoard.getModule(Temperature.class).sensors()[0],
                    sensor2 = mwBoard.getModule(Temperature.class).sensors()[1];

            sensor1.addRouteAsync(source ->
                    source.map(Function2.SUBTRACT, 273.15).name("duplicate_key")
            ).continueWithTask(IMMEDIATE_EXECUTOR, task -> sensor2.addRouteAsync(source ->
                    source.map(Function2.MULTIPLY, 1.8f)
                            .map(Function2.ADD, 32f).name("duplicate_key"))
            ).addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                assertInstanceOf(IllegalRouteOperationException.class, exception);
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void duplicateKey3() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            final Temperature.Sensor sensor1 = mwBoard.getModule(Temperature.class).sensors()[0],
                    sensor2 = mwBoard.getModule(Temperature.class).sensors()[1];

            sensor1.addRouteAsync(source ->
                    source.map(Function2.SUBTRACT, 273.15).name("duplicate_key")
            ).continueWithTask(IMMEDIATE_EXECUTOR, task -> sensor2.addRouteAsync(source ->
                    source.map(Function2.MULTIPLY, 1.8f).name("new_key")
                            .map(Function2.ADD, 32f).name("duplicate_key"))
            ).continueWithTask(IMMEDIATE_EXECUTOR, task -> sensor2.addRouteAsync(source ->
                    source.map(Function2.MULTIPLY, 1.8f).name("new_key")
                            .map(Function2.ADD, 32f))
            ).continueWithTask(IMMEDIATE_EXECUTOR, task -> {
                assertNull(task.getException());
                doneSignal.countDown();
                return task;
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void routeProcessorRemoval() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[][] expected = {
                    {0x09, 0x02, 0x01, 0x01, (byte) 0xff, 0x0, 0x02, 0x13},
                    {0x09, 0x02, 0x09, 0x03, 0x00, 0x60, 0x09, 0x0f, 0x04, 0x02, 0x0, 0x0, 0x0, 0x0},
                    {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00},
                    {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                    {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x03, 0x0f},
                    {0x0a, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x0, 0x0, (byte) 0xff},
                    {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x01, 0x01},
                    {0x0a, 0x03, 0x01},
                    {0x0a, 0x02, 0x09, 0x03, 0x03, 0x02, 0x02, 0x01},
                    {0x0a, 0x03, 0x01}
            };

            junitPlatform.maxProcessors= 5;
            RouteCreator.createLedController(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, actual -> {
                assertArrayEquals(expected, junitPlatform.getCommands());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void actualLoggingRemoval() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[][] expected = {
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x20, 0x02, 0x07},
                {0x0b, 0x02, 0x09, 0x03, 0x00, 0x20},
                {0x0b, 0x02, 0x09, 0x03, 0x01, 0x60},
                {0x0b, 0x02, 0x03, 0x04, (byte) 0xff, 0x60},
                {0x0b, 0x02, 0x03, 0x04, (byte) 0xff, 0x24},
                {0x0b, 0x03, 0x00},
                {0x0b, 0x03, 0x01},
                {0x0b, 0x03, 0x02},
                {0x09, 0x06, 0x00},
                {0x09, 0x06, 0x01},
        };

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            junitPlatform.maxLoggers = 3;
            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.multicast()
                    .to()
                    .map(Function1.RMS).log(null)
                    .accumulate().log(null)
                    .to()
                    .log(null)
                    .end()).addOnSuccessListener(IMMEDIATE_EXECUTOR, actual -> {
                assertArrayEquals(expected, junitPlatform.getCommands());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void routeEventRemoval() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[][] expected = new byte[][] {
                {0x09, 0x02, 0x01, 0x01, (byte) 0xff, 0x00, 0x02, 0x13},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x60, 0x09, 0x0f, 0x04, 0x02, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x03, 0x0f},
                {0x0a, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, (byte) 0xff},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x01, 0x01},
                {0x0a, 0x03, 0x01},
                {0x0a, 0x04, 0x00},
                {0x09, 0x06, 0x00},
                {0x09, 0x06, 0x01},
                {0x09, 0x06, 0x02},
                {0x09, 0x06, 0x03}
        };
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            junitPlatform.maxEvents = 1;
            final Led led = mwBoard.getModule(Led.class);
            mwBoard.getModule(Switch.class).state().addRouteAsync(source -> source.count().map(Function2.MODULUS, 2)
                    .multicast()
                    .to().filter(Comparison.EQ, 1).react(token -> {
                        led.editPattern(Led.Color.BLUE)
                                .highIntensity((byte) 16).lowIntensity((byte) 16)
                                .pulseDuration((short) 1000)
                                .highTime((short) 500)
                                .repeatCount(Led.PATTERN_REPEAT_INDEFINITELY)
                                .commit();
                        led.play();
                    })
                    .to().filter(Comparison.EQ, 0).react(token -> led.stop(true))
                    .end()).addOnSuccessListener(IMMEDIATE_EXECUTOR, actual -> {
                assertArrayEquals(expected, junitPlatform.getCommands());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void timerEventRemoval() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[][] expected = new byte[][] {
                {0x0c, 0x02, 0x45, 0x0c, 0x00, 0x00, 0x3B, 0x0, 0x0}
        };
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            junitPlatform.maxEvents = 1;
            mwBoard.getModule(Timer.class).scheduleAsync(3141, (short) 59, true, () -> {
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, actual -> {
                assertArrayEquals(expected, junitPlatform.getCommands());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void eventRemoval() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[][] expected= new byte[][] {
                {0x0a, 0x02, 0x011, 0x0a, (byte) 0xff, 0x02, 0x03, 0x0f},
                {0x0a, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, (byte) 0xff},
                {0x0a, 0x02, 0x011, 0x0a, (byte) 0x0ff, 0x02, 0x01, 0x01},
                {0x0a, 0x03, 0x01},
                {0x0a, 0x02, 0x11, 0x0a, (byte) 0xff, 0x08, 0x01, 0x04},
                {0x0a, 0x03, (byte) 0xf8, (byte) 0xb8, 0x0b, 0x00},
                {0x0a, 0x04, 0x00},
                {0x0a, 0x04, 0x01}
        };
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            final Led led = mwBoard.getModule(Led.class);
            final Haptic haptic = mwBoard.getModule(Haptic.class);

            junitPlatform.maxEvents = 2;
            mwBoard.getModule(Settings.class).onDisconnectAsync(() -> {
                led.editPattern(Led.Color.BLUE)
                        .highIntensity((byte) 16).lowIntensity((byte) 16)
                        .pulseDuration((short) 1000)
                        .highTime((short) 500)
                        .repeatCount(Led.PATTERN_REPEAT_INDEFINITELY)
                        .commit();
                led.play();
                haptic.startMotor(100f, (short) 3000);
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, actual -> {
                assertArrayEquals(expected, junitPlatform.getCommands());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
