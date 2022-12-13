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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mbientlab.metawear.module.Settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bolts.Capture;

/**
 * Created by etsai on 12/10/16.
 */
public class TestSettingsRev5Unsupported extends UnitTestBase {
    private Settings settings;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.addCustomModuleInfo(new byte[] {0x11, (byte) 0x80, 0x00, 0x05, 0x00});
        connectToBoard();

        settings = mwBoard.getModule(Settings.class);
    }

    @Test
    public void powerStatusNull() {
        assertNull(settings.powerStatus());
    }

    @Test
    public void chargeStatusNull() {
        assertNull(settings.chargeStatus());
    }

    @Test
    public void readPowerStatusError() {
        final Capture<Exception> result = new Capture<>();

        settings.readCurrentPowerStatusAsync().continueWith(task -> {
            result.set(task.getError());
            return null;
        });

        assertInstanceOf(UnsupportedOperationException.class, result.get());
    }

    @Test
    public void readChargeStatusError() {
        final Capture<Exception> result = new Capture<>();

        settings.readCurrentChargeStatusAsync().continueWith(task -> {
            result.set(task.getError());
            return null;
        });

        assertInstanceOf(UnsupportedOperationException.class, result.get());
    }
}
