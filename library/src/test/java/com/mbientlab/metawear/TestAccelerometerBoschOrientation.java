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
import static org.junit.jupiter.api.Assertions.fail;

import com.mbientlab.metawear.data.SensorOrientation;
import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBosch;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import bolts.Capture;

/**
 * Created by etsai on 12/18/16.
 */
public class TestAccelerometerBoschOrientation extends UnitTestBase {

    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        parameters.add(Arguments.of(AccelerometerBma255.class));
        parameters.add(Arguments.of(AccelerometerBmi160.class));
        return parameters.stream();
    }

    private AccelerometerBosch boschAcc;

    public void setup(Class<? extends AccelerometerBosch> moduleClass) {
        try {
            junitPlatform.boardInfo = new MetaWearBoardInfo(moduleClass);
            connectToBoard();

            boschAcc = mwBoard.getModule(AccelerometerBosch.class);
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void startOrientation(Class<? extends AccelerometerBosch> moduleClass) {
        setup(moduleClass);
        byte[] expected = new byte[] {0x03, 0x0f, 0x01, 0x00};

        boschAcc.orientation().start();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void stopOrientation(Class<? extends AccelerometerBosch> moduleClass) {
        setup(moduleClass);
        byte[] expected = new byte[] {0x03, 0x0f, 0x00, 0x01};

        boschAcc.orientation().stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void handleOrientationResponse(Class<? extends AccelerometerBosch> moduleClass) {
        setup(moduleClass);
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
        final byte[][] responses = new byte[][]{
                {0x03, 0x11, 0x07},
                {0x03, 0x11, 0x01},
                {0x03, 0x11, 0x03},
                {0x03, 0x11, 0x05},
                {0x03, 0x11, 0x0f},
                {0x03, 0x11, 0x0d},
                {0x03, 0x11, 0x09},
                {0x03, 0x11, 0x0b}
        };
        final Capture<SensorOrientation[]> actual = new Capture<>();

        actual.set(new SensorOrientation[8]);
        boschAcc.orientation().addRouteAsync(source -> source.stream(new Subscriber() {
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
