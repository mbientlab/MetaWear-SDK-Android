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

import com.mbientlab.metawear.module.Settings;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by etsai on 10/3/16.
 */
public class TestSettings extends UnitTestBase {
    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        for(byte i= 1; i <= 6; i++) {
            parameters.add(Arguments.of(i));
        }
        return parameters.stream();
    }

    private Settings settings;

    public void setup(byte revision) {
        try {
            junitPlatform.addCustomModuleInfo(new byte[]{0x11, (byte) 0x80, 0x00, revision});
            connectToBoard();

            settings = mwBoard.getModule(Settings.class);
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void setName(byte revision) {
        setup(revision);
        byte[] expected= new byte[] {0x11, 0x01, 0x41, 0x6e, 0x74, 0x69, 0x57, 0x61, 0x72, 0x65};

        settings.editBleAdConfig()
                .deviceName("AntiWare")
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void setTxPower(byte revision) {
        setup(revision);
        byte[] expected= new byte[] {0x11, 0x03, (byte) 0xec};

        settings.editBleAdConfig()
                .txPower((byte) -20)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void setScanResponse(byte revision) {
        setup(revision);
        byte[][] expected= new byte[][] {
                {0x11, 0x08, 0x03, 0x03, (byte) 0xd8, (byte) 0xfe, 0x10, 0x16, (byte) 0xd8, (byte) 0xfe, 0x00, 0x12, 0x00, 0x6d, 0x62},
                {0x11, 0x07, 0x69, 0x65, 0x6e, 0x74, 0x6c, 0x61, 0x62, 0x00}
        };

        settings.editBleAdConfig()
                .scanResponse(new byte[] {0x03, 0x03, (byte) 0xD8, (byte) 0xfe, 0x10, 0x16, (byte) 0xd8, (byte) 0xfe, 0x00, 0x12, 0x00, 0x6d, 0x62, 0x69, 0x65, 0x6e, 0x74, 0x6c, 0x61, 0x62, 0x00})
                .commit();
        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void setAdParameters(byte revision) {
        setup(revision);
        byte[] expected;

        if (revision >= 6) {
            expected = new byte[]{0x11, 0x02, (byte) 0x9b, 0x02, (byte) 0xb4, 0x00};
        } else if (revision >= 1) {
            expected = new byte[]{0x11, 0x02, (byte) 0x9b, 0x02, (byte) 0xb4};
        } else {
            expected = new byte[]{0x11, 0x02, (byte) 0xa1, 0x01, (byte) 0xb4};
        }

        settings.editBleAdConfig()
                .interval((short) 417)
                .timeout((byte) 180)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void startAdvertising(byte revision) {
        setup(revision);
        byte[] expected= new byte[] {0x11, 0x5};

        settings.startBleAdvertising();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
