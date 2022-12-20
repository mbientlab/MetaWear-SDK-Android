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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.mbientlab.metawear.module.Timer;
import com.mbientlab.metawear.module.Timer.ScheduledTask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import bolts.Capture;
import bolts.Task;

/**
 * Created by etsai on 9/18/16.
 */
public class TestTimer extends UnitTestBase {
    private boolean wait;
    private ScheduledTask manager;

    protected Task<ScheduledTask> setupTimer() {
        wait = true;
        return mwBoard.getModule(Timer.class).scheduleAsync(3141, (short) 59, true, () -> {
        });
    }

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(Timer.class);
        connectToBoard();

        setupTimer().continueWith(task -> {
            manager= task.getResult();
            return null;
        }).waitForCompletion();

        // For TestDeserializeTimer
        junitPlatform.boardStateSuffix = "timer";
        mwBoard.serialize();
    }

    @Test
    public void scheduleTasks() {
        byte[][] expected= new byte[][] {
                {0x0c, 0x02, 0x45, 0x0c, 0x00, 0x00, 0x3B, 0x0, 0x0}
        };

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void start() {
        byte[] expected= new byte[] {0x0c, 0x03, 0x0};

        manager.start();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void stop() {
        byte[] expected= new byte[] {0x0c, 0x04, 0x0};

        manager.stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void remove() {
        byte[][] expected= new byte[][] {
                {0x0c, 0x05, 0x0}
        };

        manager.remove();
        assertArrayEquals(expected, junitPlatform.getLastCommands(1));
    }

    @Test
    public void timeout() throws Exception {
        final Capture<Exception> actual= new Capture<>();

        junitPlatform.maxTimers= 0;
        mwBoard.getModule(Timer.class).scheduleAsync(26535, false, () -> {

        }).continueWith(task -> {
            actual.set(task.getError());
            return null;
        }).waitForCompletion();

        assertInstanceOf(TimeoutException.class, actual.get());
    }
}
