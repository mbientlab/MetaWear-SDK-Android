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

import com.mbientlab.metawear.module.ColorTcs34725;
import com.mbientlab.metawear.module.ColorTcs34725.ColorAdc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bolts.Capture;

/**
 * Created by etsai on 10/2/16.
 */

public class TestColorTcs34725 extends UnitTestBase {
    private ColorTcs34725 colorTcs34725;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(ColorTcs34725.class);
        connectToBoard();

        colorTcs34725= mwBoard.getModule(ColorTcs34725.class);
    }

    @Test
    public void read() {
        byte[] expected= new byte[] {0x17, (byte) 0x81};

        colorTcs34725.adc().addRouteAsync(source -> source.stream(null));
        colorTcs34725.adc().read();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void readSilent() {
        byte[] expected= new byte[] {0x17, (byte) 0xc1};

        colorTcs34725.adc().read();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void interpretData() {
        ColorAdc expected= new ColorAdc(418, 123, 154, 124);
        final Capture<ColorAdc> actual= new Capture<>();

        colorTcs34725.adc().addRouteAsync(source -> source.stream((data, env) -> ((Capture<ColorAdc>) env[0]).set(data.value(ColorAdc.class))))
                .continueWith(task -> {
                    task.getResult().setEnvironment(0, actual);
                    return null;
                });

        sendMockResponse(new byte[] {0x17, (byte) 0x81, (byte) 0xa2, 0x01, 0x7b, 0x00, (byte) 0x9a, 0x00, 0x7c, 0x00});
        assertEquals(expected, actual.get());
    }

    @Test
    public void interpretSingleData() {
        int[] expected = new int[] {418, 123, 154, 124};
        final int[] actual= new int[4];

        colorTcs34725.adc().addRouteAsync(source -> source.split()
                .index(0).stream((data, env) -> ((int[]) env[0])[0] = data.value(Integer.class))
                .index(1).stream((data, env) -> ((int[]) env[0])[1] = data.value(Integer.class))
                .index(2).stream((data, env) -> ((int[]) env[0])[2] = data.value(Integer.class))
                .index(3).stream((data, env) -> ((int[]) env[0])[3] = data.value(Integer.class)))
        .continueWith(task -> {
            task.getResult().setEnvironment(0, (Object) actual);
            task.getResult().setEnvironment(1, (Object) actual);
            task.getResult().setEnvironment(2, (Object) actual);
            task.getResult().setEnvironment(3, (Object) actual);
            return null;
        });

        sendMockResponse(new byte[] {0x17, (byte) 0x81, (byte) 0xa2, 0x01, 0x7b, 0x00, (byte) 0x9a, 0x00, 0x7c, 0x00});
        assertArrayEquals(expected, actual);
    }
}
