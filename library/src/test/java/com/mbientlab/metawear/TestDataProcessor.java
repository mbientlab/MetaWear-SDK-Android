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

import com.mbientlab.metawear.builder.filter.*;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.builder.function.Function2;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteElement;

import org.junit.Before;
import org.junit.Test;

import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 9/5/16.
 */
public class TestDataProcessor extends UnitTestBase {
    @Before
    public void setup() throws Exception {
        btlePlaform.boardInfo= MetaWearBoardInfo.CPRO;
        btlePlaform.firmware= "1.1.3";
        connectToBoard();
    }

    @Test
    public void createRmsLogger() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x00},
                {0x0b, 0x02, 0x09, 0x03, 0x00, 0x20}
        };

        mwBoard.getModule(Accelerometer.class).acceleration().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.map(Function1.RMS).log(null);
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {

                synchronized (TestDataProcessor.this) {
                    TestDataProcessor.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();

            assertArrayEquals(expected, btlePlaform.getCommands());
        }
    }

    @Test
    public void createFreefallDetector() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x01},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x20, 0x03, 0x05, 0x04},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x0d, 0x09, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x02, 0x00, 0x06, 0x01, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff},
                {0x09, 0x02, 0x09, 0x03, 0x02, 0x00, 0x06, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00}
        };

        mwBoard.getModule(Accelerometer.class).acceleration().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.map(Function1.RSS).average((byte) 4).filter(ThresholdOutput.BINARY, 0.5f)
                        .multicast()
                            .to().filter(Comparison.EQ, -1)
                            .to().filter(Comparison.EQ, 1)
                        .end();
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                synchronized (TestDataProcessor.this) {
                    TestDataProcessor.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();

            assertArrayEquals(expected, btlePlaform.getCommands());
        }
    }

    @Test
    public void createLedController() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x01, 0x01, (byte) 0xff, 0x00, 0x02, 0x13},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x60, 0x09, 0x0f, 0x04, 0x02, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x03, 0x0f},
                {0x0a, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, (byte) 0xff},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x01, 0x01},
                {0x0a, 0x03, 0x01},
                {0x0a, 0x02, 0x09, 0x03, 0x03, 0x02, 0x02, 0x01},
                {0x0a, 0x03, 0x01}
        };

        final Led led= mwBoard.getModule(Led.class);
        mwBoard.getModule(Switch.class).addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.count().map(Function2.MODULUS, 2)
                        .multicast()
                            .to().filter(Comparison.EQ, 1).react(new RouteElement.Action() {
                                @Override
                                public void execute(DataToken token) {
                                    led.editPattern(Led.Color.BLUE)
                                            .setHighIntensity((byte) 16).setLowIntensity((byte) 16)
                                            .setPulseDuration((short) 1000)
                                            .setHighTime((short) 500)
                                            .setRepeatCount(Led.PATTERN_REPEAT_INDEFINITELY)
                                            .commit();
                                    led.play();
                                }
                            })
                            .to().filter(Comparison.EQ, 0).react(new RouteElement.Action() {
                                @Override
                                public void execute(DataToken token) {
                                    led.stop(true);
                                }
                            })
                        .end();
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                synchronized (TestDataProcessor.this) {
                    TestDataProcessor.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();

            assertArrayEquals(expected, btlePlaform.getCommands());
        }
    }

    @Test
    public void createGpioFeedback() throws InterruptedException {
        byte[][] expected= {
                {0x09, 0x02, 0x05, (byte) 0xc6, 0x00, 0x20, 0x01, 0x02, 0x00, 0x00},
                {0x09, 0x02, 0x05, (byte) 0xc6, 0x00, 0x20, 0x09, 0x05, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x06, 0x01, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x02, 0x20, 0x02, 0x17},
                {0x09, 0x02, 0x09, 0x03, 0x03, 0x60, 0x06, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x06, 0x01, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x05, 0x20, 0x02, 0x17},
                {0x09, 0x02, 0x09, 0x03, 0x06, 0x60, 0x06, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x00, 0x09, 0x05, 0x09, 0x05, 0x04},
                {0x0a, 0x03, 0x01, 0x09, 0x05, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x00, 0x09, 0x04, 0x05},
                {0x0a, 0x03, 0x06, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x00, 0x09, 0x04, 0x05},
                {0x0a, 0x03, 0x03, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x09, 0x04, 0x05},
                {0x0a, 0x03, 0x06, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x04, 0x09, 0x04, 0x03},
                {0x0a, 0x03, 0x00, 0x01, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x05, 0x09, 0x04, 0x05},
                {0x0a, 0x03, 0x03, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x07, 0x09, 0x04, 0x03},
                {0x0a, 0x03, 0x00, 0x01, 0x00}
        };

        RouteCreator.createGpioFeedback(mwBoard).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                synchronized (TestDataProcessor.this) {
                    TestDataProcessor.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();

            /*
            // For TestDeserializeGpioFeedback
            btlePlaform.boardStateSuffix = "gpio_feedback";
            mwBoard.serialize();
             */
            assertArrayEquals(expected, btlePlaform.getCommands());
        }
    }
}
