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

import com.mbientlab.metawear.module.NeoPixel;
import com.mbientlab.metawear.module.NeoPixel.Strand;
import com.mbientlab.metawear.module.NeoPixel.Strand.RotationDirection;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 10/2/16.
 */
public class TestNeoPixelStrand extends TestNeoPixelBase {
    private Strand strand;

    @Before
    public void setup() throws Exception {
        super.setup();

        strand= neoPixel.initializeStrand((byte) 2, NeoPixel.ColorOrdering.MW_WS2811_RBG, NeoPixel.StrandSpeed.FAST, (byte) 1, (byte) 60);
    }

    @Test
    public void rotate() {
        byte[] expected= new byte[] {0x06, 0x05, 0x02, 0x01, 0x4b, (byte) 0xE8, 0x03};

        strand.rotate(RotationDirection.AWAY, (byte) 75, (short) 1000);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void rotateIndefinitely() {
        byte[] expected= new byte[] {0x06, 0x05, 0x02, 0x00, (byte) 0xff, (byte) 0xfa, 0x00};

        strand.rotate(RotationDirection.TOWARDS, (short) 250);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void stopRotate() {
        byte[] expected= new byte[] {0x06, 0x05, 0x02, 0x00, 0x00, 0x00, 0x00};

        strand.stopRotation();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void turnOff() {
        byte[] expected= new byte[] {0x06, 0x03, 0x02, 0x0a, 0x2d};

        strand.clear((byte) 10, (byte) 45);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void setColor() {
        byte[] expected= new byte[] {0x06, 0x04, 0x02, 0x18, (byte) 0xd5, 0x55, 0x6b};

        strand.setRgb((byte) 24, (byte) 213, (byte) 85, (byte) 107);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void hole() {
        byte[] expected= new byte[] {0x06, 0x02, 0x02, 0x01};

        strand.hold();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void release() {
        byte[] expected= new byte[] {0x06, 0x02, 0x02, 0x00};

        strand.release();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
