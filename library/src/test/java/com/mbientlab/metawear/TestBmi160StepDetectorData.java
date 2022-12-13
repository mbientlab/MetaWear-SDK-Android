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
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mbientlab.metawear.module.AccelerometerBmi160;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bolts.Capture;

/**
 * Created by etsai on 11/14/16.
 */

public class TestBmi160StepDetectorData extends UnitTestBase {
    private AccelerometerBmi160.StepDetectorDataProducer detector;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class);
        connectToBoard();

        detector = mwBoard.getModule(AccelerometerBmi160.class).stepDetector();
    }

    @Test
    public void subscribe() {
        byte[] expected = new byte[] {0x3, 0x19, 0x1};

        detector.addRouteAsync(source -> source.stream(null));

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void unsubscribe() {
        byte[] expected = new byte[] {0x3, 0x19, 0x0};

        detector.addRouteAsync(source -> source.stream(null)).continueWith(task -> {
            task.getResult().unsubscribe(0);
            return null;
        });

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void enable() {
        byte[] expected= new byte[] {0x03, 0x17, 0x01, 0x00};
        detector.start();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void disable() {
        byte[] expected= new byte[] {0x03, 0x17, 0x00, 0x01};
        detector.stop();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void handleResponse() {
        final Capture<Byte> actual = new Capture<>();
        byte expected = 1;

        detector.addRouteAsync(source -> source.stream((data, env) -> ((Capture<Byte>) env[0]).set(data.value(Byte.class))))
                .continueWith(task -> {
                    task.getResult().setEnvironment(0, actual);
                    return null;
                });

        sendMockResponse(new byte[] {0x03, 0x19, 0x01});
        assertEquals(expected, actual.get().byteValue());
    }
}
