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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.module.Settings.BleAdvertisementConfig;
import com.mbientlab.metawear.module.Settings.BleConnectionParameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import bolts.Task;

/**
 * Created by etsai on 10/3/16.
 */
public class TestSettingsRev2 extends UnitTestBase {
    private Settings settings;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.addCustomModuleInfo(new byte[] { 0x11, (byte) 0x80, 0x00, 0x02 });
        junitPlatform.boardInfo = new MetaWearBoardInfo(Led.class);
        connectToBoard();

        settings = mwBoard.getModule(Settings.class);
    }

    @Test
    public void disconnectEvent() throws InterruptedException, IOException {
        byte[][] expected= new byte[][] {
            {0x0a, 0x02, 0x11, 0x0a, (byte) 0xff, 0x02, 0x03, 0x0f},
            {0x0a, 0x03, 0x02, 0x02, 0x1f, 0x00, 0x00, 0x00, 0x32, 0x00, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, 0x0a},
            {0x0a, 0x02, 0x11, 0x0a, (byte) 0xff, 0x02, 0x01, 0x01},
            {0x0a, 0x03, 0x01}
        };

        final Led led= mwBoard.getModule(Led.class);

        settings.onDisconnectAsync(() -> {
            led.editPattern(Led.Color.BLUE)
                    .highTime((short) 50)
                    .pulseDuration((short) 500)
                    .highIntensity((byte) 31)
                    .repeatCount((byte) 10)
                    .commit();
            led.play();
        }).waitForCompletion();

        // check observers properly serialize
        junitPlatform.boardStateSuffix = "dc_observer";
        mwBoard.serialize();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void readAdConfig() throws InterruptedException {
        junitPlatform.addCustomResponse(new byte[] {0x11, (byte) 0x81},
                new byte[] {0x11, (byte) 0x81, 0x4d, 0x65, 0x74, 0x61, 0x57, 0x65, 0x61, 0x72});
        junitPlatform.addCustomResponse(new byte[] {0x11, (byte) 0x82},
                new byte[] {0x11, (byte) 0x82, (byte) 0x9c, 0x02, 0x00, 0x00});
        junitPlatform.addCustomResponse(new byte[] {0x11, (byte) 0x83},
                new byte[] {0x11, (byte) 0x83, 0x00});
        junitPlatform.addCustomResponse(new byte[] {0x11, (byte) 0x87},
                new byte[] {0x11, (byte) 0x87, 0x19, (byte) 0xff, 0x6d, 0x62, 0x74, 0x68, 0x65, 0x20, 0x53, 0x63, 0x61, 0x72, 0x6c, 0x65, 0x74, 0x74, 0x20, 0x73});

        Task<BleAdvertisementConfig> task = settings.readBleAdConfigAsync();
        task.waitForCompletion();

        final BleAdvertisementConfig expected = new BleAdvertisementConfig("MetaWear", (short) 417, (byte) 0, (byte) 0,
                new byte[] {0x19, (byte) 0xff, 0x6d, 0x62, 0x74, 0x68, 0x65, 0x20, 0x53, 0x63, 0x61, 0x72, 0x6c, 0x65, 0x74, 0x74, 0x20, 0x73});
        assertEquals(expected, task.getResult());
    }

    @Test
    public void readConnParam() throws InterruptedException {
        junitPlatform.addCustomResponse(new byte[] {0x11, (byte) 0x89},
                new byte[] {0x11, (byte) 0x89, 0x06, 0x00, 0x09, 0x00, 0x00, 0x00, 0x58, 0x02});

        Task<BleConnectionParameters> task = settings.readBleConnParamsAsync();
        task.waitForCompletion();

        final BleConnectionParameters expected = new BleConnectionParameters(7.5f, 11.25f, (short) 0, (short) 6000);
        assertEquals(expected, task.getResult());
    }

    @Test
    public void batteryNull() {
        assertNull(settings.battery());
    }

    @Test
    public void powerStatusNull() {
        assertNull(settings.powerStatus());
    }

    @Test
    public void chargeStatusNull() {
        assertNull(settings.chargeStatus());
    }
}
