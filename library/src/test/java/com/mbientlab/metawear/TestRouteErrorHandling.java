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

import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.builder.function.Function2;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Timer;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteElement;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by etsai on 10/14/16.
 */

public class TestRouteErrorHandling extends UnitTestBase {
    private final AtomicBoolean needToWait= new AtomicBoolean(true);
    private final Capture<Exception> actual= new Capture<>();
    private final Continuation<Route, Void> errorHandler= new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            actual.set(task.getError());
            needToWait.set(false);

            synchronized (TestRouteErrorHandling.this) {
                TestRouteErrorHandling.this.notifyAll();
            }
            return null;
        }
    };

    @Before
    public void setup() throws Exception {
        btlePlaform.firmware= "1.1.3";
        btlePlaform.addCustomModuleInfo(new byte[] { 0x11, (byte) 0x80, 0x00, 0x03 });
        connectToBoard();
    }

    @Test(expected = IllegalRouteOperationException.class)
    public void emptyEnd() throws Exception {
        mwBoard.getModule(Gpio.class).getPin((byte) 0).analogAdc().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.end();
            }
        }).continueWith(errorHandler);

        throw actual.get();
    }

    @Test(expected = NullPointerException.class)
    public void endNoMulticast() throws Exception {
        mwBoard.getModule(Gpio.class).getPin((byte) 0).analogAdc().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.multicast()
                        .to()
                        .end()
                        .end();
            }
        }).continueWith(errorHandler);

        throw actual.get();
    }

    @Test(expected = IllegalRouteOperationException.class)
    public void splitIndexOob() throws Exception {
        mwBoard.getModule(Accelerometer.class).acceleration().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.split()
                        .index(3);
            }
        }).continueWith(errorHandler);

        throw actual.get();
    }

    @Test(expected = IllegalRouteOperationException.class)
    public void duplicateKey1() throws Exception {
        final Accelerometer.AccelerationDataProducer accData= mwBoard.getModule(Accelerometer.class).acceleration();
        accData.addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.map(Function1.RMS).name(accData.name());
            }
        }).continueWith(errorHandler);

        if (needToWait.get()) {
            synchronized (this) {
                this.wait();
            }
        }
        throw actual.get();
    }

    @Test(expected = IllegalRouteOperationException.class)
    public void duplicateKey2() throws Exception {
        final Temperature.Source source1= mwBoard.getModule(Temperature.class).sources()[0],
                source2= mwBoard.getModule(Temperature.class).sources()[1];

        source1.addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.map(Function2.SUBTRACT, 273.15).name("duplicate_key");
            }
        }).continueWithTask(new Continuation<Route, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Route> task) throws Exception {
                return source2.addRoute(new RouteBuilder() {
                    @Override
                    public void configure(RouteElement source) {
                        source.map(Function2.MULTIPLY, 1.8f)
                                .map(Function2.ADD, 32f)
                                .name("duplicate_key");
                    }
                });
            }
        }).continueWith(errorHandler);

        if (needToWait.get()) {
            synchronized (this) {
                this.wait();
            }
        }
        throw actual.get();
    }

    @Test
    public void duplicateKey3() throws InterruptedException {
        final Temperature.Source source1= mwBoard.getModule(Temperature.class).sources()[0],
                source2= mwBoard.getModule(Temperature.class).sources()[1];

        source1.addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.map(Function2.SUBTRACT, 273.15).name("duplicate_key");
            }
        }).continueWithTask(new Continuation<Route, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Route> task) throws Exception {
                return source2.addRoute(new RouteBuilder() {
                    @Override
                    public void configure(RouteElement source) {
                        source.map(Function2.MULTIPLY, 1.8f).name("new_key")
                                .map(Function2.ADD, 32f).name("duplicate_key");
                    }
                });
            }
        }).continueWithTask(new Continuation<Route, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Route> task) throws Exception {
                return source2.addRoute(new RouteBuilder() {
                    @Override
                    public void configure(RouteElement source) {
                        source.map(Function2.MULTIPLY, 1.8f).name("new_key")
                                .map(Function2.ADD, 32f);
                    }
                });
            }
        }).continueWith(errorHandler);

        if (needToWait.get()) {
            synchronized (this) {
                this.wait();
            }
        }
        assertNull(actual.get());
    }

    @Test
    public void routeProcessorRemoval() throws InterruptedException {
        byte[][] expected= {
                {0x09, 0x02, 0x05, (byte) 0xc6, 0x00, 0x20, 0x01, 0x02, 0x00, 0x00},
                {0x09, 0x02, 0x05, (byte) 0xc6, 0x00, 0x20, 0x09, 0x05, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x06, 0x01, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x02, 0x20, 0x02, 0x17},
                {0x09, 0x02, 0x09, 0x03, 0x03, 0x60, 0x06, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x06, 0x01, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x06, 0x00},
                {0x09, 0x06, 0x01},
                {0x09, 0x06, 0x02},
                {0x09, 0x06, 0x03},
                {0x09, 0x06, 0x04}
        };

        btlePlaform.maxProcessors= 5;
        RouteCreator.createGpioFeedback(mwBoard).continueWith(errorHandler);

        if (needToWait.get()) {
            synchronized (this) {
                this.wait();
            }
        }

        assertArrayEquals(expected, btlePlaform.getCommands());
    }

    @Test
    public void routeLoggingRemoval() throws InterruptedException {
        byte[][] expected= {
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x20, 0x02, 0x07},
                {0x0b, 0x02, 0x09, 0x03, 0x00, 0x20},
                {0x0b, 0x02, 0x09, 0x03, 0x01, 0x60},
                {0x0b, 0x02, 0x03, 0x04, (byte) 0xff, 0x60},
                {0x0b, 0x02, 0x03, 0x04, (byte) 0xff, 0x24},
                {0x0b, 0x03, 0x00},
                {0x0b, 0x03, 0x01},
                {0x0b, 0x03, 0x02},
                {0x09, 0x06, 0x00},
                {0x09, 0x06, 0x01},
        };

        btlePlaform.maxLoggers= 3;
        mwBoard.getModule(Accelerometer.class).acceleration().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.multicast()
                        .to()
                            .map(Function1.RMS).log(null)
                            .accumulate().log(null)
                        .to()
                            .log(null)
                        .end();
            }
        }).continueWith(errorHandler);

        if (needToWait.get()) {
            synchronized (this) {
                this.wait();
            }
        }

        assertArrayEquals(expected, btlePlaform.getCommands());
    }

    @Test
    public void routeEventRemoval() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x01, 0x01, (byte) 0xff, 0x00, 0x02, 0x13},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x60, 0x09, 0x0f, 0x04, 0x02, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x03, 0x0f},
                {0x0a, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, (byte) 0xff},
                {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x01, 0x01},
                {0x0a, 0x03, 0x01},
                {0x0a, 0x04, 0x00},
                {0x09, 0x06, 0x00},
                {0x09, 0x06, 0x01},
                {0x09, 0x06, 0x02},
                {0x09, 0x06, 0x03}
        };

        btlePlaform.maxEvents= 1;
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
                                            .highIntensity((byte) 16).lowIntensity((byte) 16)
                                            .pulseDuration((short) 1000)
                                            .highTime((short) 500)
                                            .repeatCount(Led.PATTERN_REPEAT_INDEFINITELY)
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
        }).continueWith(errorHandler);

        if (needToWait.get()) {
            synchronized (this) {
                this.wait();
            }
        }

        assertArrayEquals(expected, btlePlaform.getCommands());
    }

    @Test
    public void timerEventRemoval() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x0c, 0x02, 0x45, 0x0c, 0x00, 0x00, 0x3B, 0x0, 0x0},
                {0x0a, 0x02, 0x0c, 0x06, 0x00, 0x05, (byte) 0xc6, 0x01},
                {0x0a, 0x03, 0x00},
                {0x0a, 0x02, 0x0c, 0x06, 0x00, 0x05, (byte) 0xc7, 0x01},
                {0x0a, 0x03, 0x00},
                {0x0a, 0x04, 0x00}
        };

        btlePlaform.maxEvents= 1;
        mwBoard.getModule(Timer.class).schedule(3141, (short) 59, true, new CodeBlock() {
            @Override
            public void program() {
                mwBoard.getModule(Gpio.class).getPin((byte) 0).analogAbsRef().read();
                mwBoard.getModule(Gpio.class).getPin((byte) 0).analogAdc().read();
            }
        }).continueWith(new Continuation<Timer.ScheduledTask, Void>() {
            @Override
            public Void then(Task<Timer.ScheduledTask> task) throws Exception {
                needToWait.set(false);

                synchronized (TestRouteErrorHandling.this) {
                    TestRouteErrorHandling.this.notifyAll();
                }
                return null;
            }
        });

        if (needToWait.get()) {
            synchronized (this) {
                this.wait();
            }
        }

        assertArrayEquals(expected, btlePlaform.getCommands());
    }

    @Test
    public void eventRemoval() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x0a, 0x02, 0x011, 0x0a, (byte) 0xff, 0x02, 0x03, 0x0f},
                {0x0a, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, (byte) 0xff},
                {0x0a, 0x02, 0x011, 0x0a, (byte) 0x0ff, 0x02, 0x01, 0x01},
                {0x0a, 0x03, 0x01},
                {0x0a, 0x02, 0x11, 0x0a, (byte) 0xff, 0x08, 0x01, 0x04},
                {0x0a, 0x03, (byte) 0xf8, (byte) 0xb8, 0x0b, 0x00},
                {0x0a, 0x04, 0x00},
                {0x0a, 0x04, 0x01}
        };

        final Led led= mwBoard.getModule(Led.class);
        final Haptic haptic= mwBoard.getModule(Haptic.class);

        btlePlaform.maxEvents= 2;
        mwBoard.getModule(Settings.class).onDisconnect(new CodeBlock() {
            @Override
            public void program() {
                led.editPattern(Led.Color.BLUE)
                        .highIntensity((byte) 16).lowIntensity((byte) 16)
                        .pulseDuration((short) 1000)
                        .highTime((short) 500)
                        .repeatCount(Led.PATTERN_REPEAT_INDEFINITELY)
                        .commit();
                led.play();
                haptic.startMotor(100f, (short) 3000);
            }
        }).continueWith(new Continuation<Observer, Void>() {
            @Override
            public Void then(Task<Observer> task) throws Exception {
                needToWait.set(false);

                synchronized (TestRouteErrorHandling.this) {
                    TestRouteErrorHandling.this.notifyAll();
                }
                return null;
            }
        });

        if (needToWait.get()) {
            synchronized (this) {
                this.wait();
            }
        }

        assertArrayEquals(expected, btlePlaform.getCommands());
    }
}
