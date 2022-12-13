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

import static com.mbientlab.metawear.MetaWearBoardInfo.MODULE_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.mbientlab.metawear.builder.filter.Passthrough;
import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.MagnetometerBmm150;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import bolts.Capture;

/**
 * Created by etsai on 1/13/17.
 */
public class TestAccelerometerBoschFlatRev2 extends UnitTestBase {
    private static final boolean[] EXPECTED = new boolean[] {
            true, false
    };

    private static Stream<Arguments> data() {
        List<Arguments> params = new LinkedList<>();
        for(MagnetometerBmm150.Preset preset: MagnetometerBmm150.Preset.values()) {
            params.add(Arguments.of(AccelerometerBma255.class));
            params.add(Arguments.of(AccelerometerBmi160.class));
        }
        return params.stream();
    }

    private AccelerometerBosch acc;

    public void setup(Class<? extends AccelerometerBosch> accelClass) {
        try {
            byte[] original = MODULE_RESPONSE.get(accelClass);
            byte[] moduleInfo = new byte[original.length];
            System.arraycopy(original, 0, moduleInfo, 0, moduleInfo.length);
            moduleInfo[3] = 0x2;

            junitPlatform.addCustomModuleInfo(moduleInfo);
            connectToBoard();

            acc = mwBoard.getModule(AccelerometerBosch.class);
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void response(Class<? extends AccelerometerBosch> accelClass) {
        setup(accelClass);
        final byte[][] responses = new byte[][] {
                {0x03, 0x14, 0x07},
                {0x03, 0x14, 0x03}
        };
        final Capture<boolean[]> actual = new Capture<>();

        actual.set(new boolean[2]);
        acc.flat().addRouteAsync(source -> source.stream(new Subscriber() {
            int i = 0;
            @Override
            public void apply(Data data, Object... env) {
                actual.get()[i] = data.value(Boolean.class);
                i++;
            }
        }));
        for(byte[] it: responses) {
            sendMockResponse(it);
        }

        assertArrayEquals(EXPECTED, actual.get());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void dataProcessorResponse(Class<? extends AccelerometerBosch> accelClass) throws InterruptedException {
        setup(accelClass);
        final byte[][] responses = new byte[][] {
                {0x09, 0x03, 0x00, 0x07},
                {0x09, 0x03, 0x00, 0x03}
        };
        final Capture<boolean[]> actual = new Capture<>();

        actual.set(new boolean[2]);
        acc.flat().addRouteAsync(source -> source.limit(Passthrough.ALL, (short) 0).stream(new Subscriber() {
            int i = 0;
            @Override
            public void apply(Data data, Object... env) {
                actual.get()[i] = data.value(Boolean.class);
                i++;
            }
        })).waitForCompletion();

        for(byte[] it: responses) {
            sendMockResponse(it);
        }

        assertArrayEquals(EXPECTED, actual.get());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void logResponse(Class<? extends AccelerometerBosch> accelClass) throws InterruptedException {
        setup(accelClass);
        final byte[] responses = new byte[] {
                0x0b, 0x07,
                (byte) 0xe0, (byte) 0x95, (byte) 0x99, (byte) 0x88, 0x00, 0x07, 0x00, 0x00, 0x00,
                (byte) 0xe0, (byte) 0x81, (byte) 0xa3, (byte) 0x88, 0x00, 0x03, 0x00, 0x00, 0x00
        };
        final Capture<boolean[]> actual = new Capture<>();

        actual.set(new boolean[2]);
        acc.flat().addRouteAsync(source -> source.log(new Subscriber() {
            int i = 0;
            @Override
            public void apply(Data data, Object... env) {
                actual.get()[i] = data.value(Boolean.class);
                i++;
            }
        })).waitForCompletion();

        sendMockResponse(responses);

        assertArrayEquals(EXPECTED, actual.get());
    }
}
