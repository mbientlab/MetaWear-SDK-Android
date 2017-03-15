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

import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.*;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.builder.function.Function2;
import com.mbientlab.metawear.builder.predicate.PulseOutput;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Temperature;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 9/5/16.
 */
public class TestDataProcessor extends UnitTestBase {
    @Before
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(Switch.class, Led.class, AccelerometerBmi160.class, Gpio.class, Temperature.class);
        junitPlatform.firmware= "1.2.5";
        connectToBoard();
    }

    @Test
    public void setAccSum() throws InterruptedException {
        byte[] expected = new byte[] {0x09, 0x04, 0x01, 0x00, 0x00, 0x71, 0x02};

        mwBoard.getModule(Accelerometer.class).configure()
                .range(16f)
                .odr(100f)
                .commit();
        mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.map(Function1.RMS).accumulate().name("rms_acc");
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

            mwBoard.getModule(DataProcessor.class).edit("rms_acc", DataProcessor.AccumulatorEditor.class).set(20000f);
            assertArrayEquals(expected, junitPlatform.getLastCommand());
        }
    }

    @Test
    public void createRmsLogger() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x00},
                {0x0b, 0x02, 0x09, 0x03, 0x00, 0x20}
        };

        mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
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

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    @Test
    public void createFreefallDetector() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x01},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x20, 0x03, 0x05, 0x04},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x0d, 0x09, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x02, 0x00, 0x06, 0b00000001, (byte) 0xff},
                {0x09, 0x02, 0x09, 0x03, 0x02, 0x00, 0x06, 0b00000001, (byte) 0x01}
        };


        mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
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

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    @Test
    public void createLedController() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x01, 0x01, (byte) 0xff, 0x00, 0x02, 0x13},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x60, 0x09, 0x0f, 0x04, 0x02, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0b00000110, 0x01, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0b00000110, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x03, 0x0f},
                {0x0a, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, (byte) 0xff},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x01, 0x01},
                {0x0a, 0x03, 0x01},
                {0x0a, 0x02, 0x09, 0x03, 0x03, 0x02, 0x02, 0x01},
                {0x0a, 0x03, 0x01}
        };

        RouteCreator.createLedController(mwBoard).continueWith(new Continuation<Route, Void>() {
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

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    @Test
    public void comparatorFeedback() throws InterruptedException {
        byte[][] expected = {
                {0x09, 0x02, 0x05, (byte) 0xc7, 0x00, 0x20, 0x06, 0x22, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x00, 0x09, 0x05, 0x05, 0x05, 0x03},
                {0x0a, 0x03, 0x00, 0x06, 0x22, 0x00, 0x00}
        };

        final Gpio gpio = mwBoard.getModule(Gpio.class);
        gpio.pin((byte) 0).analogAdc().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.filter(Comparison.GT, "reference").name("reference");
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

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    @Test
    public void createGpioFeedback() throws InterruptedException, IOException {
        byte[][] expected= {
                {0x09, 0x02, 0x05, (byte) 0xc6, 0x00, 0x20, 0x01, 0x02, 0x00, 0x00},
                {0x09, 0x02, 0x05, (byte) 0xc6, 0x00, 0x20, 0x09, 0x05, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x06, 0b00100011, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x02, 0x20, 0x02, 0x17},
                {0x09, 0x02, 0x09, 0x03, 0x03, 0x60, 0x06, 0b00000110, 0x10, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x06, 0b00011011, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x05, 0x20, 0x02, 0x17},
                {0x09, 0x02, 0x09, 0x03, 0x06, 0x60, 0x06, 0b00000110, 0x10, 0x00, 0x00, 0x00},
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

            // For TestDeserializeGpioFeedback
            junitPlatform.boardStateSuffix = "gpio_feedback";
            mwBoard.serialize();
            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    @Test
    public void adcPulse() throws InterruptedException {
        byte[][] expected = new byte[][] {
                {0x09, 0x02, 0x05, (byte) 0xc7, 0x00, 0x20, 0x0b, 0x01, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x10, 0x00},
                {0x09, 0x03, 0x01},
                {0x09, 0x07, 0x00, 0x01}
        };

        Gpio.Pin pin = mwBoard.getModule(Gpio.class).pin((byte) 0);
        pin.analogAdc().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.find(PulseOutput.AREA, 512, (short) 16).stream(null);
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

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    @Test
    public void accRightShift() throws InterruptedException {
        byte[][] expected = new byte[][] {
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x09, 0x14, 0x08, 0x08, 0x00, 0x00, 0x00, 0x02},
                {0x0b, 0x02, 0x09, 0x03, 0x00, 0x40}
        };
        
        mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.map(Function2.RIGHT_SHIFT, 8).log(null);
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

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    @Test
    public void accRightShiftData() throws InterruptedException {
        float[] expected = new float[] {1.969f, 0.812f, 0.984f};
        final float[] actual = new float[3];

        mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.map(Function2.RIGHT_SHIFT, 8).stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        byte[] bytes = data.bytes();
                        for(int i = 0; i < bytes.length; i++) {
                            actual[i] = (bytes[i] << 8) / data.scale();
                        }
                    }
                });
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

            sendMockResponse(new byte[] {0x09, 0x03, 0x00, 126, 52, 63});
            assertArrayEquals(expected, actual, 0.001f);
        }
    }

    @Test
    public void tempConverter() throws InterruptedException {
        byte[][] expected = new byte[][] {
                {0x09, 0x02, 0x04, (byte) 0x81, 0x00, 0x20, 0x09, 0x17, 0x02, 0x12, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x60, 0x09, 0x1f, 0x03, 0x0a, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x09, 0x1f, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x04, (byte) 0x81, 0x00, 0x20, 0x09, 0x17, 0x01, (byte) 0x89, 0x08, 0x00, 0x00, 0x00},
                {0x09, 0x03, 0x01},
                {0x09, 0x07, 0x02, 0x01},
                {0x09, 0x03, 0x01},
                {0x09, 0x07, 0x03, 0x01},
                {0x04, (byte) 0x81, 0x00}
        };


        final Temperature.Sensor thermometer = mwBoard.getModule(Temperature.class).findSensors(Temperature.SensorType.NRF_SOC)[0];
        thermometer.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.multicast()
                        .to().stream(null)
                        .to()
                            .map(Function2.MULTIPLY, 18)
                            .map(Function2.DIVIDE, 10)
                            .map(Function2.ADD, 32)
                            .stream(null)
                        .to()
                            .map(Function2.ADD, 273.15f)
                            .stream(null);
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                thermometer.read();
                synchronized (TestDataProcessor.this) {
                    TestDataProcessor.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }
}
