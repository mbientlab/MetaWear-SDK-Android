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

import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.IBeacon;
import com.mbientlab.metawear.module.IBeacon.Configuration;
import com.mbientlab.metawear.module.Switch;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import bolts.Task;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by etsai on 10/18/16.
 */
public class TestIBeacon extends UnitTestBase {
    private IBeacon ibeacon;

    @Before
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(Switch.class, IBeacon.class, AccelerometerBmi160.class);
        connectToBoard();

        ibeacon= mwBoard.getModule(IBeacon.class);
    }

    @Test
    public void setMajorFeedback() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x01, 0x01, (byte) 0xff, 0x00, 0x02, 0x13},
                {0x0a, 0x02, 0x09, 0x03, 0x00, 0x07, 0x03, 0x02, 0x09, 0x00},
                {0x0a, 0x03, 0x00, 0x00},
                {0x07, 0x01, 0x01}
        };

        Switch mwSwitch= mwBoard.getModule(Switch.class);
        mwSwitch.state().addRouteAsync(source -> source.count().react(token -> ibeacon.configure()
                .major(token)
                .commit())
        ).waitForCompletion();
        ibeacon.enable();


        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void setSlicedFeedback() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x0a, 0x02, 0x03, 0x04, (byte) 0xff, 0x07, 0x03, 0x02, 0x09, 0x00},
                {0x0a, 0x03, 0x00, 0x00},
                {0x0a, 0x02, 0x03, 0x04, (byte) 0xff, 0x07, 0x04, 0x02, 0x45, 0x00},
                {0x0a, 0x03, 0x00, 0x00},
                {0x07, 0x01, 0x01}
        };

        Accelerometer accelerometer = mwBoard.getModule(Accelerometer.class);
        accelerometer.acceleration().addRouteAsync(source -> source.react(token ->
            ibeacon.configure()
                .major(token.slice((byte) 0, (byte) 4))
                .minor(token.slice((byte) 4, (byte) 2))
                .commit()
        )).waitForCompletion();
        ibeacon.enable();


        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void sliceOutOfBounds() throws Exception {
        Accelerometer accelerometer = mwBoard.getModule(Accelerometer.class);

        Task<Route> task = accelerometer.acceleration().addRouteAsync(source -> source.react(token ->
            ibeacon.configure()
                    .major(token.slice((byte) -2, (byte) 2))
                    .commit()
        ));
        task.waitForCompletion();


        throw task.getError();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void sliceOutOfBounds2() throws Exception {
        Accelerometer accelerometer = mwBoard.getModule(Accelerometer.class);

        Task<Route> task = accelerometer.acceleration().addRouteAsync(source -> source.react(token ->
            ibeacon.configure()
                    .major(token.slice((byte) 0, (byte) 7))
                    .commit()
        ));
        task.waitForCompletion();


        throw task.getError();
    }

    @Test
    public void readConfig() throws InterruptedException {
        junitPlatform.addCustomResponse(new byte[] {0x07, (byte) 0x82},
                new byte[] {0x07, (byte) 0x82, 0x5a, (byte) 0xe7, (byte) 0xba, (byte) 0xfb, 0x4c, 0x46, (byte) 0xdd, (byte) 0xd9, (byte) 0x95, (byte) 0x91, (byte) 0xcb, (byte) 0x85, 0x00, (byte) 0x90, 0x6a, 0x32});
        junitPlatform.addCustomResponse(new byte[] {0x07, (byte) 0x83},
                new byte[] {0x07, (byte) 0x83, 0x45, 0x0c});
        junitPlatform.addCustomResponse(new byte[] {0x07, (byte) 0x84},
                new byte[] {0x07, (byte) 0x84, (byte) 0x81, (byte) 0xe7});
        junitPlatform.addCustomResponse(new byte[] {0x07, (byte) 0x85},
                new byte[] {0x07, (byte) 0x85, (byte) 0xc9});
        junitPlatform.addCustomResponse(new byte[] {0x07, (byte) 0x86},
                new byte[] {0x07, (byte) 0x86, 0x00});
        junitPlatform.addCustomResponse(new byte[] {0x07, (byte) 0x87},
                new byte[] {0x07, (byte) 0x87, 0x64, 0x00});

        Task<Configuration> task = ibeacon.readConfigAsync();
        task.waitForCompletion();

        final Configuration expected = new Configuration(UUID.fromString("326a9000-85cb-9195-d9dd-464cfbbae75a"),
                (short) 3141, (short) 59265, (short)100, (byte) -55, (byte)0);
        assertEquals(expected, task.getResult());
    }
}
