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

import com.mbientlab.metawear.module.Gpio;

import org.junit.Before;
import org.junit.Test;

import static com.mbientlab.metawear.module.Gpio.PullMode.*;
import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 9/2/16.
 */
public class TestGpioDigital extends UnitTestBase {
    private Gpio gpio;

    @Before
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(Gpio.class);
        connectToBoard();

        gpio= mwBoard.getModule(Gpio.class);
    }

    @Test
    public void setDigitalOut() {
        byte[] expected = new byte[] {0x05, 0x01, 0x00};
        gpio.pin((byte) 0).setOutput();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void clearDigitalOut() {
        byte[] expected = new byte[] {0x05, 0x02, 0x01};
        gpio.pin((byte) 1).clearOutput();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void setPullMode() {
        byte[][] expected= new byte[][]{
                {0x05, 0x05, 0x02},
                {0x05, 0x04, 0x03},
                {0x05, 0x03, 0x04}
        };
        gpio.pin((byte) 2).setPullMode(NO_PULL);
        gpio.pin((byte) 3).setPullMode(PULL_DOWN);
        gpio.pin((byte) 4).setPullMode(PULL_UP);

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void read() {
        byte[] expected= new byte[] {0x05, (byte) 0x88, 0x04};

        gpio.pin((byte) 4).digital().addRouteAsync(source -> source.stream(null));
        gpio.pin((byte) 4).digital().read();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void readSilent() {
        byte[] expected= new byte[] {0x05, (byte) 0xc8, 0x04};

        gpio.pin((byte) 4).digital().read();

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void receivedDigitalData() {
        final byte[] actual= new byte[2];
        byte[] expected= new byte[] {1, 0};

        gpio.pin((byte) 7).digital().addRouteAsync(source -> source.stream(new Subscriber() {
            private byte i= 0;

            @Override
            public void apply(Data data, Object ... env) {
                ((byte[]) env[0])[i]= data.value(Byte.class);
                i++;
            }
        })).continueWith(task -> {
            task.getResult().setEnvironment(0, (Object) actual);
            return null;
        });

        sendMockResponse(new byte[] {0x05, (byte) 0x88, 0x07, 0x01});
        sendMockResponse(new byte[] {0x05, (byte) 0x88, 0x07, 0x00});

        assertArrayEquals(expected, actual);
    }
}
