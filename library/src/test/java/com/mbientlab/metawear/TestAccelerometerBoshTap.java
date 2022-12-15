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
import static org.junit.jupiter.api.Assertions.fail;

import com.mbientlab.metawear.data.TapType;
import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.AccelerometerBosch.DoubleTapWindow;
import com.mbientlab.metawear.module.AccelerometerBosch.Tap;
import com.mbientlab.metawear.module.AccelerometerBosch.TapQuietTime;
import com.mbientlab.metawear.module.AccelerometerBosch.TapShockTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import bolts.Capture;

/**
 * Created by etsai on 12/19/16.
 */
public class TestAccelerometerBoshTap extends UnitTestBase {

    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        parameters.add(Arguments.of(AccelerometerBma255.class));
        parameters.add(Arguments.of(AccelerometerBmi160.class));
        return parameters.stream();
    }

    private AccelerometerBosch boschAcc;

    public void setup(Class<? extends AccelerometerBosch> accelClass) {
        try {
            junitPlatform.boardInfo = new MetaWearBoardInfo(accelClass);
            connectToBoard();

            boschAcc = mwBoard.getModule(AccelerometerBosch.class);
        } catch(Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureSingle(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = new byte[] {0x03, 0x0d, 0x04, 0x04};

        boschAcc.configure()
                .range(16f)
                .commit();
        boschAcc.tap().configure()
                .threshold(2f)
                .shockTime(TapShockTime.TST_50_MS)
                .commit();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void startSingle(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = new byte[] {0x03, 0x0c, 0x02, 0x00};

        boschAcc.tap().configure()
                .enableSingleTap()
                .commit();
        boschAcc.tap().start();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void stopSingle(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = new byte[] {0x03, 0x0c, 0x00, 0x03};

        boschAcc.tap().stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void handleSingleResponse(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        final Tap[] expected = new Tap[]{
                new Tap(TapType.SINGLE, POSITIVE),
                new Tap(TapType.SINGLE, NEGATIVE)
        };
        final byte[][] responses = new byte[][] {
                {0x03, 0x0e, 0x12},
                {0x03, 0x0e, 0x32}
        };
        final Capture<Tap[]> actual = new Capture<>();

        actual.set(new Tap[2]);
        boschAcc.tap().addRouteAsync(source -> source.stream(new Subscriber() {
            int i = 0;
            @Override
            public void apply(Data data, Object... env) {
                actual.get()[i] = data.value(Tap.class);
                i++;
            }
        }));
        for(byte[] it: responses) {
            sendMockResponse(it);
        }

        assertArrayEquals(expected, actual.get());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureDouble(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = new byte[] {0x03, 0x0d, (byte) 0xc0, 0x04};

        boschAcc.configure()
                .range(8f)
                .commit();
        boschAcc.tap().configure()
                .threshold(1f)
                .doubleTapWindow(DoubleTapWindow.DTW_50_MS)
                .quietTime(TapQuietTime.TQT_20_MS)
                .shockTime(TapShockTime.TST_75_MS)
                .commit();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void startDouble(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = new byte[] {0x03, 0x0c, 0x01, 0x00};

        boschAcc.tap().configure()
                .enableDoubleTap()
                .commit();
        boschAcc.tap().start();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void stoDouble(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = new byte[] {0x03, 0x0c, 0x00, 0x03};

        boschAcc.tap().stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void handleDoubleResponse(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        final Tap[] expected = new Tap[]{
                new Tap(TapType.DOUBLE, POSITIVE),
                new Tap(TapType.DOUBLE, NEGATIVE)
        };
        final byte[][] responses = new byte[][] {
                {0x03, 0x0e, 0x11},
                {0x03, 0x0e, 0x31}
        };
        final Capture<Tap[]> actual = new Capture<>();

        actual.set(new Tap[2]);
        boschAcc.tap().addRouteAsync(source -> source.stream(new Subscriber() {
            int i = 0;
            @Override
            public void apply(Data data, Object... env) {
                actual.get()[i] = data.value(Tap.class);
                i++;
            }
        }));
        for(byte[] it: responses) {
            sendMockResponse(it);
        }

        assertArrayEquals(expected, actual.get());
    }
}
