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

import static com.mbientlab.metawear.data.Sign.NEGATIVE;
import static com.mbientlab.metawear.data.Sign.POSITIVE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.mbientlab.metawear.data.CartesianAxis;
import com.mbientlab.metawear.data.Sign;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.AccelerometerMma8452q.Movement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bolts.Capture;

/**
 * Created by etsai on 12/20/16.
 */

public class TestMma8452qMovement extends UnitTestBase {
    private AccelerometerMma8452q mma8452qAcc;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerMma8452q.class);
        connectToBoard();

        mma8452qAcc = mwBoard.getModule(AccelerometerMma8452q.class);
    }

    @Test
    public void start() {
        byte[] expected = new byte[] {0x03, 0x05, 0x01};

        mma8452qAcc.freeFall().start();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void stop() {
        byte[] expected = new byte[] {0x03, 0x05, 0x00};

        mma8452qAcc.freeFall().stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void configureFreefall() {
        byte[] expected = new byte[] {0x03, 0x06, (byte) 0xb8, 0x00, 0x07, 0x0a};

        mma8452qAcc.freeFall().configure()
                .threshold(0.5f)
                .axes(CartesianAxis.values())
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void handleFreefall() {
        final byte[][] responses = new byte[][]{
                {0x03, 0x07, (byte) 0x94},
                {0x03, 0x07, (byte) 0x80},
                {0x03, 0x07, (byte) 0x91}
        };
        final Movement[] expected = new Movement[] {
                new Movement(new boolean[] {false, false, false}, new Sign[] {POSITIVE, NEGATIVE, NEGATIVE}),
                new Movement(new boolean[] {false, false, false}, new Sign[] {POSITIVE, POSITIVE, POSITIVE}),
                new Movement(new boolean[] {false, false, false}, new Sign[] {NEGATIVE, POSITIVE, NEGATIVE}),
        };
        final Capture<Movement[]> actual = new Capture<>();

        actual.set(new Movement[3]);
        mma8452qAcc.freeFall().addRouteAsync(source -> source.stream(new Subscriber() {
            int i = 0;
            @Override
            public void apply(Data data, Object... env) {
                actual.get()[i] = data.value(Movement.class);
                i++;
            }
        }));
        for(byte[] it: responses) {
            sendMockResponse(it);
        }

        assertArrayEquals(expected, actual.get());
    }

    @Test
    public void configureMotion() {
        byte[] expected = new byte[] {0x03, 0x06, (byte) 0xf8, 0x00, 0x1f, 0x0a};

        mma8452qAcc.motion().configure()
                .threshold(2f)
                .axes(CartesianAxis.values())
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void handleMotion() {
        final byte[][] responses = new byte[][] {
                {0x03, 0x07, (byte) 0xa0},
                {0x03, 0x07, (byte) 0x99},
                {0x03, 0x07, (byte) 0x87}
        };
        final Movement[] expected = new Movement[] {
                new Movement(new boolean[] {false, false, true}, new Sign[] {POSITIVE, POSITIVE, POSITIVE}),
                new Movement(new boolean[] {false, true, false}, new Sign[] {NEGATIVE, POSITIVE, NEGATIVE}),
                new Movement(new boolean[] {true, false, false}, new Sign[] {NEGATIVE, NEGATIVE, POSITIVE}),
        };
        final Capture<Movement[]> actual = new Capture<>();

        actual.set(new Movement[3]);
        mma8452qAcc.motion().addRouteAsync(source -> source.stream(new Subscriber() {
            int i = 0;
            @Override
            public void apply(Data data, Object... env) {
                actual.get()[i] = data.value(Movement.class);
                i++;
            }
        }));
        for(byte[] it: responses) {
            sendMockResponse(it);
        }

        assertArrayEquals(expected, actual.get());
    }
}
