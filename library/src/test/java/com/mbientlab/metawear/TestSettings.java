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

import com.mbientlab.metawear.module.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 10/3/16.
 */
@RunWith(Parameterized.class)
public class TestSettings extends UnitTestBase {
    @Parameters(name = "revision: {0}")
    public static Collection<Object[]> data() {
        ArrayList<Object[]> parameters= new ArrayList<>();
        for(byte i= 1; i <= 6; i++) {
            parameters.add(new Object[] { i });
        }

        return parameters;
    }

    private Settings settings;

    @Parameter
    public byte revision;

    @Before
    public void setup() throws Exception {
        junitPlatform.addCustomModuleInfo(new byte[] {0x11, (byte) 0x80, 0x00, revision});
        connectToBoard();

        settings = mwBoard.getModule(Settings.class);
    }

    @Test
    public void setName() {
        byte[] expected= new byte[] {0x11, 0x01, 0x41, 0x6e, 0x74, 0x69, 0x57, 0x61, 0x72, 0x65};

        settings.editBleAdConfig()
                .deviceName("AntiWare")
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void setTxPower() {
        byte[] expected= new byte[] {0x11, 0x03, (byte) 0xec};

        settings.editBleAdConfig()
                .txPower((byte) -20)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void setScanResponse() {
        byte[][] expected= new byte[][] {
                {0x11, 0x08, 0x03, 0x03, (byte) 0xd8, (byte) 0xfe, 0x10, 0x16, (byte) 0xd8, (byte) 0xfe, 0x00, 0x12, 0x00, 0x6d, 0x62},
                {0x11, 0x07, 0x69, 0x65, 0x6e, 0x74, 0x6c, 0x61, 0x62, 0x00}
        };

        settings.editBleAdConfig()
                .scanResponse(new byte[] {0x03, 0x03, (byte) 0xD8, (byte) 0xfe, 0x10, 0x16, (byte) 0xd8, (byte) 0xfe, 0x00, 0x12, 0x00, 0x6d, 0x62, 0x69, 0x65, 0x6e, 0x74, 0x6c, 0x61, 0x62, 0x00})
                .commit();
        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void setAdParameters() {
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

    @Test
    public void startAdvertising() {
        byte[] expected= new byte[] {0x11, 0x5};

        settings.startBleAdvertising();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
