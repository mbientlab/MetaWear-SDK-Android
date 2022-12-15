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

import com.mbientlab.metawear.module.BarometerBmp280;
import com.mbientlab.metawear.module.BarometerBmp280.StandbyTime;
import com.mbientlab.metawear.module.BarometerBosch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by etsai on 10/2/16.
 */
public class TestBarometerBmp280Standby extends UnitTestBase {
    private final byte[] STANDBY_BITMASK= new byte[] {0x00, 0x20, 0x40, 0x60, (byte) 0x80, (byte) 0xa0, (byte) 0xc0, (byte) 0xe0};

    private BarometerBmp280 baroBmp280;

    private static Stream<Arguments> data() {
        float[] rawStandby= new float[] {0.25f, 60.125f, 127f, 225f, 376, 1234, 2718.2818f, 3141.592653f};
        List<Arguments> params = new LinkedList<>();
        for(StandbyTime entry: StandbyTime.values()) {
            params.add(Arguments.of(entry, rawStandby[entry.ordinal()]));
        }
        return params.stream();
    }

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(BarometerBmp280.class);
        connectToBoard();

        baroBmp280= mwBoard.getModule(BarometerBmp280.class);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void setStandbyTime(StandbyTime standby, float standbyLiteral) {
        byte[] expected= new byte[] {0x12, 0x03, 0x2c, STANDBY_BITMASK[standby.ordinal()]};

        baroBmp280.configure()
                .standbyTime(standby)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void setStandbyTimeRaw(StandbyTime standby, float standbyLiteral) {
        byte[] expected= new byte[] {0x12, 0x03, 0x2c, STANDBY_BITMASK[standby.ordinal()]};

        baroBmp280.configure()
                .standbyTime(standbyLiteral)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void setAll(StandbyTime standby, float standbyLiteral) {
        byte[] expected= new byte[] {0x12, 0x03, 0x30, (byte) (STANDBY_BITMASK[standby.ordinal()] | 0x10)};

        baroBmp280.configure()
                .standbyTime(standby)
                .pressureOversampling(BarometerBosch.OversamplingMode.HIGH)
                .filterCoeff(BarometerBosch.FilterCoeff.AVG_16)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
