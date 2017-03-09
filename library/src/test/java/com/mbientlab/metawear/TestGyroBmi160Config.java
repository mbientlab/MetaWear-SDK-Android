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

import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.GyroBmi160.Range;
import com.mbientlab.metawear.module.GyroBmi160.OutputDataRate;

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
public class TestGyroBmi160Config extends UnitTestBase {
    static final byte[] ODR_BITMASK= new byte[] { 0b0110, 0b0111, 0b1000, 0b1001, 0b1010, 0b1011, 0b1100, 0b1101 },
            RANGE_BITMASK= new byte[] { 0b000, 0b001, 0b010, 0b011, 0b100 };

    @Parameters(name = "odr: {0}, range: {1}")
    public static Collection<Object[]> data() {
        ArrayList<Object[]> parameters = new ArrayList<>();
        for(OutputDataRate odr: OutputDataRate.values()) {
            for(Range fsr: Range.values()) {
                parameters.add(new Object[] { odr, fsr });
            }
        }

        return parameters;
    }

    @Parameter
    public OutputDataRate odr;

    @Parameter(value = 1)
    public Range fsr;

    private GyroBmi160 gyroBmi160;

    @Before
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(GyroBmi160.class);
        connectToBoard();

        gyroBmi160= mwBoard.getModule(GyroBmi160.class);
    }

    @Test
    public void configure() {
        byte[] expected= new byte[] {0x13, 0x03, (byte) (0x20 | ODR_BITMASK[odr.ordinal()]), RANGE_BITMASK[fsr.ordinal()]};

        gyroBmi160.configure()
                .odr(odr)
                .range(fsr)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
