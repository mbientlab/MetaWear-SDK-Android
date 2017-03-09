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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 10/2/16.
 */
public class TestNeoPixelInit extends TestNeoPixelBase {
    @Test
    public void initSlow() {
        byte[] expected= new byte[] {0x06, 0x01, 0x01, 0x03, 0x00, 0x1e};

        neoPixel.initializeStrand((byte) 1, NeoPixel.ColorOrdering.MW_WS2811_GBR, NeoPixel.StrandSpeed.SLOW, (byte) 0, (byte) 30);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void initFast() {
        byte[] expected= new byte[] {0x06, 0x01, 0x02, 0x05, 0x01, 0x3c};

        neoPixel.initializeStrand((byte) 2, NeoPixel.ColorOrdering.MW_WS2811_RBG, NeoPixel.StrandSpeed.FAST, (byte) 1, (byte) 60);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void freeStrand() {
        byte[] expected= new byte[] {0x06, 0x06, 0x02};

        NeoPixel.Strand strand= neoPixel.initializeStrand((byte) 2, NeoPixel.ColorOrdering.MW_WS2811_RBG, NeoPixel.StrandSpeed.FAST, (byte) 1, (byte) 60);
        strand.free();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
