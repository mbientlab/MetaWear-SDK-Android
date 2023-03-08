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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.android.gms.tasks.Task;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.module.Settings.BatteryState;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by etsai on 10/3/16.
 */
public class TestSettingsRev3 extends UnitTestBase {
    private Settings settings;
// TODO: all tests flakey, race condition?
    public Task<Void> setup() throws Exception {
        junitPlatform.addCustomModuleInfo(new byte[] { 0x11, (byte) 0x80, 0x00, 0x03 });
        junitPlatform.boardInfo = new MetaWearBoardInfo(Haptic.class);
        return connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored ->
                settings = mwBoard.getModule(Settings.class));
    }

    @Test
    public void disconnectEvent() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[][] expected= new byte[][] {
                    {0x0a, 0x02, 0x11, 0x0a, (byte) 0xff, 0x08, 0x01, 0x04},
                    {0x0a, 0x03, (byte) 0xf8, (byte) 0xb8, 0x0b, 0x00}
            };
            final Haptic haptic = mwBoard.getModule(Haptic.class);

            settings.onDisconnectAsync(() -> haptic.startMotor(100f, (short) 3000))
                .addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                    assertArrayEquals(expected, junitPlatform.getCommands());
                    doneSignal.countDown();
                });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void readBattery() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[] expected= new byte[] {0x11, (byte) 0xcc};

            settings.battery().read();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void interpretBatteryData() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            BatteryState expected = new BatteryState((byte) 99,  Float.intBitsToFloat(0x4084bc6a));
            final Capture<BatteryState> actual = new Capture<>();

            settings.battery().addRouteAsync(source ->
                    source.stream((data, env) -> ((Capture<BatteryState>) env[0]).set(data.value(BatteryState.class)))
            ).continueWith(IMMEDIATE_EXECUTOR, task -> {
                task.getResult().setEnvironment(0, actual);
                return null;
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored2 -> {

                sendMockResponse(new byte[] {0x11, (byte) 0x8c, 0x63, 0x34, 0x10});

                assertEquals(expected, actual.get());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void interpretComponentBatteryData() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            final Capture<Float> actualVoltage = new Capture<>();
            final Capture<Byte> actualCharge = new Capture<>();

            short expectedCharge =  99;
            float expectedVoltage = Float.intBitsToFloat(0x4084bc6a);

            settings.battery().addRouteAsync(source -> source.split()
                    .index(0).stream((Subscriber) (data, env) -> ((Capture<Byte>) env[0]).set(data.value(Byte.class)))
                    .index(1).stream((Subscriber) (data, env) -> ((Capture<Float>) env[0]).set(data.value(Float.class)))
            ).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                task.setEnvironment(0, actualCharge);
                task.setEnvironment(1, actualVoltage);
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                sendMockResponse(new byte[] {0x11, (byte) 0x8c, 0x63, 0x34, 0x10});

                assertEquals(expectedCharge, actualCharge.get().byteValue());
                assertEquals(expectedVoltage, actualVoltage.get(), 0.001f);
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void powerStatusNull() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNull(settings.powerStatus());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void chargeStatusNull() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNull(settings.chargeStatus());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
