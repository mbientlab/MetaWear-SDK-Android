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

import com.mbientlab.metawear.module.BarometerBme280;
import com.mbientlab.metawear.module.BarometerBme280.StandbyTime;
import com.mbientlab.metawear.module.BarometerBosch;

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
 * Created by etsai on 10/2/16.
 */
@RunWith(Parameterized.class)
public class TestBarometerBme280Standby extends UnitTestBase {
    private final byte[] STANDBY_BITMASK= new byte[] {0x00, 0x20, 0x40, 0x60, (byte) 0x80, (byte) 0xa0, (byte) 0xc0, (byte) 0xe0};

    private BarometerBme280 baroBme280;

    @Parameters(name = "standby: {0}")
    public static Collection<Object[]> data() {
        float[] rawStandby= new float[] {0.25f, 60.125f, 127f, 225f, 376, 1234, 14.1421356f, 17.320508f};

        ArrayList<Object[]> parameters= new ArrayList<>();
        for(StandbyTime entry: StandbyTime.values()) {
            parameters.add(new Object[] {entry, rawStandby[entry.ordinal()]});
        }

        return parameters;
    }

    @Parameter
    public StandbyTime standby;

    @Parameter(value = 1)
    public float standbyLiteral;

    private byte[] expected;

    @Before
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(BarometerBme280.class);
        connectToBoard();

        baroBme280 = mwBoard.getModule(BarometerBme280.class);
        expected= new byte[] {0x12, 0x03, 0x2c, STANDBY_BITMASK[standby.ordinal()]};
    }

    @Test
    public void setStandbyTime() {
        baroBme280.configure()
                .standbyTime(standby)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void setStandbyTimeRaw() {
        baroBme280.configure()
                .standbyTime(standbyLiteral)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void setAll() {
        byte[] expected= new byte[] {0x12, 0x03, 0x30, (byte) (STANDBY_BITMASK[standby.ordinal()] | 0x10)};

        baroBme280.configure()
                .standbyTime(standby)
                .pressureOversampling(BarometerBosch.OversamplingMode.HIGH)
                .filterCoeff(BarometerBosch.FilterCoeff.AVG_16)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
