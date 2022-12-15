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

import com.mbientlab.metawear.data.Quaternion;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.SensorFusionBosch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bolts.Capture;

/**
 * Created by etsai on 12/5/16.
 */

public class TestMetaMotionDataProcessor extends UnitTestBase {
    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(DataProcessor.class, SensorFusionBosch.class);
        connectToBoard();
    }

    @Test
    public void testQuaternionLimiter() throws InterruptedException {
        byte[][] expected = new byte[][] {
                {0x09, 0x02, 0x19, 0x07, (byte) 0xff, (byte) 0xe0, 0x08, 0x17, 0x14, 0x00, 0x00, 0x00},
                {0x0b, 0x02, 0x09, 0x03, 0x00, 0x60},
                {0x0b, 0x02, 0x09, 0x03, 0x00, 0x64},
                {0x0b, 0x02, 0x09, 0x03, 0x00, 0x68},
                {0x0b, 0x02, 0x09, 0x03, 0x00, 0x6c}
        };

        mwBoard.getModule(SensorFusionBosch.class).quaternion().addRouteAsync(source -> source.limit(20).log(null)).waitForCompletion();
        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void testQuaternionLimiterData() throws InterruptedException {
        final Quaternion expected = new Quaternion(Float.intBitsToFloat(0x3f6e62a4), Float.intBitsToFloat( 0x3eba7b01),
                Float.intBitsToFloat(0x3c756866), Float.intBitsToFloat(0x00000000));
        final Capture<Quaternion> actual = new Capture<>();

        mwBoard.getModule(SensorFusionBosch.class).quaternion().addRouteAsync(source ->
                source.limit(20).log((data, env) -> actual.set(data.value(Quaternion.class)))
        ).waitForCompletion();

        sendMockResponse(new byte[] {0x0b, 0x07, 0x60, 0x78, 0x70, 0x05, 0x00, (byte) 0xa4, 0x62, 0x6e, 0x3f,
                0x61, 0x78, 0x70, 0x05, 0x00, 0x01, 0x7b, (byte) 0xba, 0x3e});
        sendMockResponse(new byte[] {0x0b, 0x07, 0x62, 0x78, 0x70, 0x05, 0x00, 0x66, 0x68, 0x75, 0x3c,
                0x63, 0x78, 0x70, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00});
        assertEquals(expected, actual.get());
    }
}