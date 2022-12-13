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

import com.mbientlab.metawear.module.Gpio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import bolts.Capture;

/**
 * Created by etsai on 9/2/16.
 */
public class TestGpioEnhancedAnalog extends UnitTestBase {
    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        parameters.add(Arguments.of((byte) 3, true));
        parameters.add(Arguments.of((byte) 2, false));
        return parameters.stream();
    }

    private Gpio gpio;

    private byte mask;

    public void setup(boolean absRef) {
        try {
            junitPlatform.boardInfo = new MetaWearBoardInfo(Gpio.class);
            junitPlatform.addCustomModuleInfo(new byte[]{0x05, (byte) 0x80, 0x00, 0x02, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01});
            connectToBoard();

            gpio = mwBoard.getModule(Gpio.class);
            mask = (byte) (absRef ? 6 : 7);
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void read(byte pin, boolean absRef) {
        setup(absRef);
        byte[] expected= new byte[] {0x05, (byte) (0xc0 | mask), pin, (byte) 0xff, (byte) 0xff, 0x00, (byte) 0xff};

        (absRef ? gpio.pin(pin).analogAbsRef() : gpio.pin(pin).analogAdc()).read((byte) 0xff, (byte) 0xff, (short) 0, (byte) 0xff);

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void readEnhanced(byte pin, boolean absRef) {
        setup(absRef);
        byte[] expected= new byte[] {0x05, (byte) (0x80 | mask), pin, 0x01, 0x02, 0x02, 0x15};

        Gpio.Analog producer = absRef ? gpio.pin(pin).analogAbsRef() : gpio.pin(pin).analogAdc();
        producer.addRouteAsync(source -> source.stream(null));
        producer.read((byte) 1, (byte) 2, (short) 10, (byte) 0x15);

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void handleVirtualPinData(byte pin, boolean absRef) {
        setup(absRef);
        final Capture<Float> actualAbsRef = new Capture<>();
        final Capture<Short> actualAdc = new Capture<>();
        byte[][] responses= new byte[][] {
                {0x05, (byte) 0x86, 0x15, (byte) 0xd8, 0x02},
                {0x05, (byte) 0x87, 0x15, (byte) 0xda, 0x00}
        };

        float expectedAbsRef = 0.728f;
        short expectedAdc = 218;

        (absRef ? gpio.getVirtualPin((byte) 0x15).analogAbsRef() : gpio.getVirtualPin((byte) 0x15).analogAdc()).addRouteAsync(source -> source.stream((data, env) -> {
            if (absRef) {
                ((Capture<Float>) env[0]).set(data.value(Float.class));
            } else {
                ((Capture<Short>) env[0]).set(data.value(Short.class));
            }
        })).continueWith(task -> {
            task.getResult().setEnvironment(0, absRef ? actualAbsRef : actualAdc);
            return null;
        });

        sendMockResponse(responses[absRef ? 0 : 1]);

        if (absRef) {
            assertEquals(expectedAbsRef, actualAbsRef.get(), 0.001f);
        } else {
            assertEquals(expectedAdc, actualAdc.get().shortValue());
        }
    }

}
