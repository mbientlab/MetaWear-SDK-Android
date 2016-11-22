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
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteElement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by etsai on 9/2/16.
 */
@RunWith(Parameterized.class)
public class TestGpioEnhancedAnalog extends UnitTestBase {
    @Parameters(name = "mode: {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {(byte) 3, true},
                {(byte) 2, false}
        });
    }

    private Gpio gpio;

    @Parameter
    public byte pin;

    @Parameter(value = 1)
    public boolean absRef;

    private byte mask;

    @Before
    public void setup() throws Exception {
        btlePlaform.boardInfo= MetaWearBoardInfo.RPRO;
        btlePlaform.addCustomModuleInfo(new byte[] {0x05, (byte) 0x80, 0x00, 0x02, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01});
        connectToBoard();

        gpio= mwBoard.getModule(Gpio.class);
        mask= (byte) (absRef ? 6 : 7);
    }

    @Test
    public void read() {
        byte[] expected= new byte[] {0x05, (byte) (0xc0 | mask), pin, (byte) 0xff, (byte) 0xff, 0x00, (byte) 0xff};

        (absRef ? gpio.getPin(pin).analogAbsRef() : gpio.getPin(pin).analogAdc()).read((byte) 0xff, (byte) 0xff, (short) 0, (byte) 0xff);

        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }

    @Test
    public void readEnhanced() {
        byte[] expected= new byte[] {0x05, (byte) (0x80 | mask), pin, 0x01, 0x02, 0x02, 0x15};

        (absRef ? gpio.getPin(pin).analogAbsRef() : gpio.getPin(pin).analogAdc()).addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.stream(null);
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                (absRef ? gpio.getPin(pin).analogAbsRef() : gpio.getPin(pin).analogAdc()).read((byte) 1, (byte) 2, (short) 10, (byte) 0x15);
                return null;
            }
        });

        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }

    @Test
    public void handlePinVirtualData() {
        final short[] actual= new short[1];
        byte[][] responses= new byte[][] {
                {0x05, (byte) 0x86, 0x15, (byte) 0xd8, 0x02},
                {0x05, (byte) 0x87, 0x15, (byte) 0xda, 0x00}
        };
        short[] expected= new short[] {728, 218};

        (absRef ? gpio.createVirtualPin((byte) 0x15).analogAbsRef() : gpio.createVirtualPin((byte) 0x15).analogAdc()).addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object ... env) {
                        ((short[]) env[0])[0]= data.value(Short.class);
                    }
                });
            }
        }).continueWith(new Continuation<Route, Object>() {
            @Override
            public Object then(Task<Route> task) throws Exception {
                task.getResult().setEnvironment(0, actual);
                return null;
            }
        });

        sendMockResponse(responses[absRef ? 0 : 1]);

        assertEquals(expected[absRef ? 0 : 1], actual[0]);
    }
}
