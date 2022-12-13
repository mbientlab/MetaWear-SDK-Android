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
import static org.junit.jupiter.api.Assertions.fail;

import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Temperature.Sensor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import bolts.Capture;

/**
 * Created by etsai on 10/3/16.
 */
public class TestTemperatureMwrProData extends UnitTestBase {
    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        for(int i= 0; i < 4; i++) {
            parameters.add(Arguments.of(i));
        }
        return parameters.stream();
    }

    private Sensor currentSrc;

    public void setup(int sourceIdx) {
        try {
            junitPlatform.boardInfo= new MetaWearBoardInfo(Temperature.class);
            connectToBoard();

            currentSrc= mwBoard.getModule(Temperature.class).sensors()[sourceIdx];
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void read(int sourceIdx) {
        setup(sourceIdx);

        byte[] expected= new byte[] {0x4, (byte) 0x81, (byte) sourceIdx};

        currentSrc.addRouteAsync(source -> source.stream(null));
        currentSrc.read();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void readSilent(int sourceIdx) {
        setup(sourceIdx);

        byte[] expected= new byte[] {0x4, (byte) 0xc1, (byte) sourceIdx};

        currentSrc.read();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    private static final byte[][] RESPONSES = new byte[][] {
            {0x04, (byte) 0x81, 0x00, (byte) 0xfe, 0x00},
            {0x04, (byte) 0x81, 0x01, (byte) 0xa8, 0x00},
            {0x04, (byte) 0x81, 0x02, (byte) 0xac, (byte) 0xff},
            {0x04, (byte) 0x81, 0x03, 0x00, 0x00}
    };
    private static final float[] EXPECTED = new float[] { 31.75f, 21f, -10.5f, 0f};

    @ParameterizedTest
    @MethodSource("data")
    public void interpretData(int sourceIdx) {
        setup(sourceIdx);

        final Capture<Float> actual= new Capture<>();

        currentSrc.addRouteAsync(source -> source.stream((data, env) -> ((Capture<Float>) env[0]).set(data.value(Float.class)))).continueWith(task -> {
            task.getResult().setEnvironment(0, actual);
            return null;
        });
        sendMockResponse(RESPONSES[sourceIdx]);

        assertEquals(EXPECTED[sourceIdx], actual.get(), 0.00000001f);
    }
}
