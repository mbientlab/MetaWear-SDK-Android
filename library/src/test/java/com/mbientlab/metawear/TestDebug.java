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

import com.mbientlab.metawear.module.Debug;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 10/12/16.
 */
@RunWith(Parameterized.class)
public class TestDebug extends UnitTestBase {
    @Parameters(name = "board: {0}")
    public static Collection<Object[]> allBoardsParams() {
        return UnitTestBase.allBoardsParams();
    }

    private Debug debug;

    @Parameter
    public MetaWearBoardInfo info;

    @Before
    public void setup() throws Exception {
        junitPlatform.boardInfo = info;
        connectToBoard();

        debug= mwBoard.getModule(Debug.class);
    }

    @Test
    public void reset() {
        byte[] expected= new byte[] {(byte) 0xfe, 0x01};

        debug.resetAsync();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void jumpToBootloader() {
        byte[] expected= new byte[] {(byte) 0xfe, 0x02};

        debug.jumpToBootloaderAsync();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void disconnect() {
        byte[] expected= new byte[] {(byte) 0xfe, 0x06};

        debug.disconnectAsync();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void resetAfterGc() {
        byte[] expected= new byte[] {(byte) 0xfe, 0x05};

        debug.resetAfterGc();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
