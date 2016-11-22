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

import java.util.Arrays;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 9/2/16.
 */
public class TestGpioAnalog extends UnitTestBase {
    private Gpio gpio;

    @Before
    public void setup() throws Exception {
        btlePlaform.boardInfo= MetaWearBoardInfo.RPRO;
        connectToBoard();

        gpio= mwBoard.getModule(Gpio.class);
    }

    @Test
    public void read() {
        byte[][] expected= new byte[][] {
                {0x05, (byte) 0x86, 0x03},
                {0x05, (byte) 0x87, 0x02}
        };

        final Task<Route> absRef= gpio.getPin((byte) 3).analogAbsRef().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.stream(null);
            }
        });
        final Task<Route> adc= gpio.getPin((byte) 2).analogAdc().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.stream(null);
            }
        });

        List<Task<Route>> tasks= Arrays.asList(absRef, adc);
        Task.whenAll(tasks).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                gpio.getPin((byte) 3).analogAbsRef().read();
                gpio.getPin((byte) 2).analogAdc().read();
                return null;
            }
        });

        assertArrayEquals(expected, btlePlaform.getCommands());
    }

    @Test
    public void readSilent() {
        byte[][] expected= new byte[][] {
                {0x05, (byte) 0xc6, 0x03},
                {0x05, (byte) 0xc7, 0x02}
        };

        gpio.getPin((byte) 3).analogAbsRef().read();
        gpio.getPin((byte) 2).analogAdc().read();

        assertArrayEquals(expected, btlePlaform.getCommands());
    }

    @Test
    public void readEnhanced() {
        byte[][] expected= new byte[][] {
                {0x05, (byte) 0xc6, 0x03},
                {0x05, (byte) 0xc7, 0x02}
        };

        gpio.getPin((byte) 3).analogAbsRef().read((byte) 0xff, (byte) 0xff, (short) 0, (byte) 0xff);
        gpio.getPin((byte) 2).analogAdc().read((byte) 0xff, (byte) 0xff, (short) 0, (byte) 0xff);

        assertArrayEquals(expected, btlePlaform.getCommands());
    }

    private static final Subscriber ANALOG_SUBSCRIBER = new Subscriber() {
        private transient int i;

        @Override
        public void apply(Data data, Object ... env) {
            ((short[]) env[0])[i]= data.value(Short.class);
            i++;
        }
    };

    protected Task<Route> setupAbsRef() {
        return gpio.getPin((byte) 1).analogAbsRef().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.stream(ANALOG_SUBSCRIBER);
            }
        });
    }

    protected Task<Route> setupAdc() {
        return gpio.getPin((byte) 1).analogAdc().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.stream(ANALOG_SUBSCRIBER);
            }
        });
    }

    @Test
    public void receivedAnalogData() {
        final short[] actual= new short[2];
        short[] expected= new short[] {882, 2498};


        setupAbsRef().continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                task.getResult().setEnvironment(0, (Object) actual);
                return null;
            }
        });
        setupAdc().continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                task.getResult().setEnvironment(0, (Object) actual);
                return null;
            }
        });

        /*
        // for TestGpioAnalog
        btlePlaform.boardStateSuffix = "gpio_analog";
        mwBoard.serialize();
        */

        sendMockResponse(new byte[] {0x05, (byte) 0x87, 0x01, 0x72, 0x03});
        sendMockResponse(new byte[] {0x05, (byte) 0x86, 0x01, (byte) 0xc2, 0x09});

        assertArrayEquals(expected, actual);
    }
}
