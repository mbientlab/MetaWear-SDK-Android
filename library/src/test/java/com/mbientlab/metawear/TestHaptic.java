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

import com.mbientlab.metawear.module.Haptic;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;

import static com.mbientlab.metawear.MetaWearBoardInfo.*;
import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 10/2/16.
 */
@RunWith(Parameterized.class)
public class TestHaptic extends UnitTestBase {
    @Parameters(name = "board: {0}")
    public static Collection<Object[]> data() {
        ArrayList<Object[]> parameters= new ArrayList<>();
        for(MetaWearBoardInfo info: new MetaWearBoardInfo[] {CPRO, DETECTOR, ENVIRONMENT, RPRO, R, RG, MOTIOON_R}) {
            parameters.add(new Object[] {info});
        }

        return parameters;
    }

    private Haptic haptic;

    @Parameter
    public MetaWearBoardInfo info;

    @Before
    public void setup() throws Exception {
        btlePlaform.boardInfo= info;
        connectToBoard();

        haptic= mwBoard.getModule(Haptic.class);
    }

    @Test
    public void startMotor() {
        byte[] expected= new byte[] {0x08, 0x01, (byte) 0xf8, (byte) 0x88, 0x13, 0x00};

        haptic.startMotor(100f, (short) 5000);
        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }

    @Test
    public void startBuzzer() {
        byte[] expected= new byte[] {0x08, 0x01, 0x7f, 0x4c, 0x1d, 0x01};

        haptic.startBuzzer((short) 7500);
        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }
}
