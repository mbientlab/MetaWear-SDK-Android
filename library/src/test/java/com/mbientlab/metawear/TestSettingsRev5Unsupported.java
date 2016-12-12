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

import java.util.Collection;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertNull;

/**
 * Created by etsai on 12/10/16.
 */
@RunWith(Parameterized.class)
public class TestSettingsRev5Unsupported extends UnitTestBase {
    @Parameters(name = "board: {0}")
    public static Collection<Object[]> data() {
        return UnitTestBase.allBoardsParams();
    }

    private Settings settings;

    @Parameter
    public MetaWearBoardInfo info;

    @Before
    public void setup() throws Exception {
        btlePlaform.addCustomModuleInfo(new byte[] {0x11, (byte) 0x80, 0x00, 0x05, 0x00});
        btlePlaform.boardInfo = info;
        connectToBoard();

        settings = mwBoard.getModule(Settings.class);
    }

    @Test
    public void powerStatusNull() {
        assertNull(settings.powerStatus());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void readPowerStatusFail() throws Exception {
        final Capture<Exception> actual= new Capture<>();

        settings.readPowerStatusAsync().continueWith(new Continuation<Byte, Void>() {
            @Override
            public Void then(Task<Byte> task) throws Exception {
                actual.set(task.getError());
                return null;
            }
        });

        throw actual.get();
    }

    @Test
    public void chargeStatusNull() {
        assertNull(settings.chargeStatus());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void readChargeStatusFail() throws Exception {
        final Capture<Exception> actual= new Capture<>();

        settings.readChargeStatusAsync().continueWith(new Continuation<Byte, Void>() {
            @Override
            public Void then(Task<Byte> task) throws Exception {
                actual.set(task.getError());
                return null;
            }
        });

        throw actual.get();
    }
}
