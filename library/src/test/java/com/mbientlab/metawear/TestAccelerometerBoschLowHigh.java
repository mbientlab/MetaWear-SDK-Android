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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.AccelerometerBosch.LowHighResponse;

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
public class TestAccelerometerBoschLowHigh extends UnitTestBase {
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
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureLow(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = accelClass.equals(AccelerometerBmi160.class) ?
                new byte[] {0x03, 0x07, 0x07, 0x40, (byte) 0x85, 0x0b, (byte) 0xc0} :
                new byte[] {0x03, 0x07, 0x09, 0x40, (byte) 0x85, 0x0f, (byte) 0xc0};

        boschAcc.configure()
                .range(16f)
                .commit();
        boschAcc.lowHigh().configure()
                .enableLowG()
                .lowThreshold(0.5f)
                .lowDuration(20)
                .lowGMode(AccelerometerBosch.LowGMode.SUM)
                .commit();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void startLow(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = new byte[] {0x03, 0x06, 0x08, 0x00};

        boschAcc.lowHigh().configure()
                .enableLowG()
                .commit();
        boschAcc.lowHigh().start();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void stopLow(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = new byte[] {0x03, 0x06, 0x00, 0x0f};

        boschAcc.lowHigh().stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void handleLowResponse(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        final LowHighResponse expected = new LowHighResponse(
                false,
                true,
                false,
                false,
                false,
                POSITIVE
        );
        final byte[] response = new byte[] {0x03, 0x08, 0x02};
        final Capture<LowHighResponse> actual = new Capture<>();

        boschAcc.lowHigh().addRouteAsync(source -> source.stream((data, env) -> actual.set(data.value(LowHighResponse.class))));
        sendMockResponse(response);

        assertEquals(expected, actual.get());
    }


    @ParameterizedTest
    @MethodSource("data")
    public void configureHigh(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = accelClass.equals(AccelerometerBmi160.class) ?
                new byte[] {0x03, 0x07, 0x07, 0x30, (byte) 0x81, 0x05, 0x20} :
                new byte[] {0x03, 0x07, 0x09, 0x30, (byte) 0x81, 0x06, 0x20};

        boschAcc.configure()
                .range(16f)
                .commit();
        boschAcc.lowHigh().configure()
                .enableHighGz()
                .highThreshold(2f)
                .highDuration(15)
                .commit();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void startHigh(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = new byte[] {0x03, 0x06, 0x04, 0x00};

        boschAcc.lowHigh().configure()
                .enableHighGz()
                .commit();
        boschAcc.lowHigh().start();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void stopHigh(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        byte[] expected = new byte[] {0x03, 0x06, 0x00, 0x0f};

        boschAcc.lowHigh().stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void handleHighResponse(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        final LowHighResponse[] expected = new LowHighResponse[] {
                new LowHighResponse(true, false, false, true, false, NEGATIVE),
                new LowHighResponse(true, false, false, true, false, POSITIVE),
                new LowHighResponse(true, false, false, false, true, NEGATIVE),
                new LowHighResponse(true, false, false, false, true, POSITIVE),
                new LowHighResponse(true, false, true, false, false, POSITIVE),
                new LowHighResponse(true, false, true, false, false, NEGATIVE),
        };
        final byte[][] responses = new byte[][] {
                {0x03, 0x08, 0x29},
                {0x03, 0x08, 0x09},
                {0x03, 0x08, 0x31},
                {0x03, 0x08, 0x11},
                {0x03, 0x08, 0x05},
                {0x03, 0x08, 0x25},
        };
        final Capture<LowHighResponse[]> actual = new Capture<>();

        actual.set(new LowHighResponse[6]);
        boschAcc.lowHigh().addRouteAsync(source -> source.stream(new Subscriber() {
            int i = 0;
            @Override
            public void apply(Data data, Object... env) {
                actual.get()[i] = data.value(LowHighResponse.class);
                i++;
            }
        }));
        for(byte[] it: responses) {
            sendMockResponse(it);
        }

        assertArrayEquals(expected, actual.get());
    }
}
