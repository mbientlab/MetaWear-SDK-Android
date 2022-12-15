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

import com.mbientlab.metawear.data.MagneticField;
import com.mbientlab.metawear.module.MagnetometerBmm150;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

/**
 * Created by etsai on 11/17/16.
 */
public class TestMagnetometerBmm150PackedData extends UnitTestBase {

    private MagnetometerBmm150 mag;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(MagnetometerBmm150.class);
        connectToBoard();

        mag= mwBoard.getModule(MagnetometerBmm150.class);
        mag.packedMagneticField().addRouteAsync(source -> source.stream((data, env) -> ((ArrayList<MagneticField>) env[0]).add(data.value(MagneticField.class))));
    }

    @Test
    public void interpretPackedData() {
        byte[] response= new byte[] {0x15, 0x09, (byte) 0xb6, 0x0c, 0x72, (byte) 0xf7, (byte) 0x89, (byte) 0xee, (byte) 0xb6,
                0x0b, 0x5a, (byte) 0xf8, 0x32, (byte) 0xee, (byte) 0xe6, 0x0a, (byte) 0xa2, (byte) 0xf7, 0x25, (byte) 0xef};
        MagneticField[] expected = new MagneticField[] {
                new MagneticField(Float.intBitsToFloat(0x39554110), Float.intBitsToFloat(0xb90f861a), Float.intBitsToFloat(0xb9928177)),
                new MagneticField(Float.intBitsToFloat(0x39447a18), Float.intBitsToFloat(0xb90051ca), Float.intBitsToFloat(0xb9955b46)),
                new MagneticField(Float.intBitsToFloat(0x3936d86f), Float.intBitsToFloat(0xb90c60cc), Float.intBitsToFloat(0xb98d64d8))
        };

        final ArrayList<MagneticField> received = new ArrayList<>();
        mwBoard.lookupRoute(0).setEnvironment(0, received);

        sendMockResponse(response);
        MagneticField[] actual = new MagneticField[3];
        received.toArray(actual);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void subscribe() {
        byte[] expected = new byte[] {0x15, 0x09, 0x01};
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void unsubscribe() {
        byte[] expected = new byte[] {0x15, 0x09, 0x00};
        mwBoard.lookupRoute(0).unsubscribe(0);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void enable() {
        byte[] expected= new byte[] {0x15, 0x02, 0x01, 0x00};

        mag.packedMagneticField().start();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void disable() {
        byte[] expected= new byte[] {0x15, 0x02, 0x00, 0x01};

        mag.packedMagneticField().stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
