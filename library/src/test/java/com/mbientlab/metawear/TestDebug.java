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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.android.gms.tasks.Task;
import com.mbientlab.metawear.module.Debug;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by etsai on 10/12/16.
 */

public class TestDebug extends UnitTestBase {
    private Debug debug;


    public Task<Void> setup() {
        junitPlatform.boardInfo = new MetaWearBoardInfo(Debug.class);
        return connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            debug = mwBoard.getModule(Debug.class);
        });
    }

    @Test
    public void reset() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {(byte) 0xfe, 0x01};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            debug.resetAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                assertArrayEquals(expected, junitPlatform.getLastCommand());
                doneSignal.countDown();
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void jumpToBootloader() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {(byte) 0xfe, 0x02};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            debug.jumpToBootloaderAsync();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void disconnect() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {(byte) 0xfe, 0x06};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            debug.disconnectAsync();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void resetAfterGc() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected = new byte[] {(byte) 0xfe, 0x05};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            debug.resetAfterGc();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void receivedTmpValue() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        int expected = 0xdeadbeef;

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            debug.readTmpValueAsync().continueWith(IMMEDIATE_EXECUTOR, task -> {
                sendMockResponse(new byte[] {(byte) 0xfe, (byte) 0x84, (byte) 0xef, (byte) 0xbe, (byte) 0xad, (byte) 0xde});
                assertEquals(expected, task.getResult().intValue());
                doneSignal.countDown();
                return null;
            });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void noPowersave() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertFalse(debug.enablePowersave());
            doneSignal.countDown();
        }).addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
            fail(exception);
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
