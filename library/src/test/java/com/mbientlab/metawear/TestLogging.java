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

import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.Logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

/**
 * Created by etsai on 9/3/16.
 */
public class TestLogging extends UnitTestBase {
    private Logging logging;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(AccelerometerBmi160.class);
        connectToBoard();

        logging= mwBoard.getModule(Logging.class);
    }

    @Test
    public void startOverwrite() {
        byte[][] expected= new byte[][]{
                {0x0b, 0x0b, 0x01},
                {0x0b, 0x01, 0x01}
        };

        logging.start(true);
        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void startNoOverwrite() {
        byte[][] expected= new byte[][]{
                {0x0b, 0x0b, 0x00},
                {0x0b, 0x01, 0x01}
        };

        logging.start(false);
        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void stop() {
        byte[] expected= new byte[] {0x0b, 0x01, 0x00};

        logging.stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void clearEntries() {
        byte[] expected= new byte[] {0x0b, 0x09, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

        logging.clearEntries();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    public void timeoutHandler() throws Exception {
        final Exception[] actual= new Exception[1];

        junitPlatform.maxLoggers= 0;
        mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.log(null)).continueWith(task -> {
            actual[0]= task.getError();
            return null;
        }).waitForCompletion();

        assertInstanceOf(TimeoutException.class, actual[0]);
    }
}
