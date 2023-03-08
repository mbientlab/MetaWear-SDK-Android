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
import com.mbientlab.metawear.module.Settings;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by etsai on 10/3/16.
 */
public class TestSettingsRev1 extends UnitTestBase {
    private Settings settings;

    public Task<Void> setup() throws Exception {
        junitPlatform.addCustomModuleInfo(new byte[] { 0x11, (byte) 0x80, 0x00, 0x01 });
        return connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored ->
                settings = mwBoard.getModule(Settings.class));
    }

    @Test
    public void setConnParams() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[] expected= new byte[] {0x11, 0x09, 0x58, 0x02, 0x20, 0x03, (byte) 0x80, 0x00, 0x66, 0x06};

            settings.editBleConnParams()
                    .minConnectionInterval(750f)
                    .maxConnectionInterval(1000f)
                    .slaveLatency((short) 128)
                    .supervisorTimeout((short) 16384)
                    .commit();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void disconnectEvent() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            final Capture<Exception> actual = new Capture<>();

            settings.onDisconnectAsync(null).continueWith(IMMEDIATE_EXECUTOR, task -> {
                actual.set(task.getException());
                return null;
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                assertInstanceOf(UnsupportedOperationException.class, actual.get());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void batteryNull() throws Exception {
        setup().addOnSuccessListener(ignored ->
                assertNull(settings.battery()));
    }

    @Test
    public void powerStatusNull() throws Exception {
        setup().addOnSuccessListener(ignored ->
            assertNull(settings.powerStatus()));
    }

    @Test
    public void chargeStatusNull() throws Exception {
        setup().addOnSuccessListener(ignored ->
            assertNull(settings.chargeStatus()));
    }
}
