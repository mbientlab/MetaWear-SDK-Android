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

import com.mbientlab.metawear.data.SensorOrientation;
import com.mbientlab.metawear.module.AccelerometerMma8452q;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bolts.Capture;

/**
 * Created by etsai on 12/20/16.
 */

public class TestMma8452qOrientation extends UnitTestBase {
    private AccelerometerMma8452q mma8452qAcc;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerMma8452q.class);
        connectToBoard();

        mma8452qAcc = mwBoard.getModule(AccelerometerMma8452q.class);
    }

    @Test
    public void startOrientation() {
        byte[][] expected = new byte[][] {
                {0x03, 0x09, 0x00, (byte) 0xc0, 0x0a, 0x44, (byte) 0x84},
                {0x03, 0x08, 0x01}
        };

        mma8452qAcc.orientation().start();
        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void stopOrientation() {
        byte[][] expected = new byte[][] {
                {0x03, 0x08, 0x00},
                {0x03, 0x09, 0x00, (byte) 0x80, 0x00, 0x44, (byte) 0x84}
        };

        mma8452qAcc.orientation().stop();
        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void handleResponse() {
        final SensorOrientation[] expected = new SensorOrientation[] {
                SensorOrientation.FACE_UP_LANDSCAPE_RIGHT,
                SensorOrientation.FACE_UP_PORTRAIT_UPRIGHT,
                SensorOrientation.FACE_UP_PORTRAIT_UPSIDE_DOWN,
                SensorOrientation.FACE_UP_LANDSCAPE_LEFT,
                SensorOrientation.FACE_DOWN_LANDSCAPE_RIGHT,
                SensorOrientation.FACE_DOWN_LANDSCAPE_LEFT,
                SensorOrientation.FACE_DOWN_PORTRAIT_UPRIGHT,
                SensorOrientation.FACE_DOWN_PORTRAIT_UPSIDE_DOWN
        };
        final byte[][] responses = new byte[][] {
                {0x03, 0x0a, (byte) 0x84},
                {0x03, 0x0a, (byte) 0x80},
                {0x03, 0x0a, (byte) 0x82},
                {0x03, 0x0a, (byte) 0x86},
                {0x03, 0x0a, (byte) 0x85},
                {0x03, 0x0a, (byte) 0x87},
                {0x03, 0x0a, (byte) 0x81},
                {0x03, 0x0a, (byte) 0x83}
        };
        final Capture<SensorOrientation[]> actual = new Capture<>();

        actual.set(new SensorOrientation[8]);
        mma8452qAcc.orientation().addRouteAsync(source -> source.stream(new Subscriber() {
            int i = 0;
            @Override
            public void apply(Data data, Object... env) {
                actual.get()[i] = data.value(SensorOrientation.class);
                i++;
            }
        }));
        for(byte[] it: responses) {
            sendMockResponse(it);
        }

        assertArrayEquals(expected, actual.get());
    }
}
