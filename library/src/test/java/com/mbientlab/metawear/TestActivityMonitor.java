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
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.builder.RouteBuilder;

import org.junit.Before;
import org.junit.Test;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by etsai on 9/5/16.
 */
public class TestActivityMonitor extends UnitTestBase {
    private Route activityRoute, bufferStateRoute;

    @Before
    public void setup() throws Exception {
        junitPlatform.addCustomModuleInfo(new byte[] {0x09, (byte) 0x80, 0x00, 0x00, 0x1c});
        junitPlatform.boardInfo= new MetaWearBoardInfo(AccelerometerBmi160.class);
        connectToBoard();

        mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.map(Function1.RMS)
                        .accumulate()
                        .multicast()
                            .to().limit(1000).stream(new Subscriber() {
                                @Override
                                public void apply(Data data, Object ... env) {
                                    ((Capture<Float>) env[0]).set(data.value(Float.class));
                                }
                            })
                            .to().buffer().name("rms_accum")
                        .end();
            }
        }).continueWithTask(new Continuation<Route, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Route> task) throws Exception {
                activityRoute = task.getResult();
                return mwBoard.getModule(DataProcessor.class).state("rms_accum").addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object ... env) {
                                ((Capture<Float>) env[0]).set(data.value(Float.class));
                            }
                        });
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                bufferStateRoute= task.getResult();

                synchronized (TestActivityMonitor.this) {
                    TestActivityMonitor.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();
        }
    }

    @Test
    public void createRoute() {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x20, 0x02, 0x07},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x08, 0x03, (byte) 0xe8, 0x03, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x0f, 0x03},
                {0x09, 0x03, 0x01},
                {0x09, 0x07, 0x02, 0x01}
        };

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void handleData() {
        float expected= 33.771667f;
        Capture<Float> actual= new Capture<>();

        activityRoute.setEnvironment(0, actual);

        sendMockResponse(new byte[] {0x09, 0x03, 0x02, 0x63, 0x71, 0x08, 0x00});
        assertEquals(expected, actual.get(), 0.0001f);
    }

    @Test
    public void readBufferSilent() {
        byte[] expected= new byte[] {0x9, (byte) 0xc4, 3};

        bufferStateRoute.unsubscribe(0);
        mwBoard.getModule(DataProcessor.class).state("rms_accum").read();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void readBuffer() {
        byte[] expected= new byte[] {0x9, (byte) 0x84, 3};

        mwBoard.getModule(DataProcessor.class).state("rms_accum").read();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void bufferData() {
        float expected= 71.61182f;
        Capture<Float> actual= new Capture<>();

        bufferStateRoute.setEnvironment(0, actual);
        sendMockResponse(new byte[] {0x09, (byte) 0x84, 0x03, 0x28, (byte) 0xe7, 0x11, 0x00});
        assertEquals(expected, actual.get(), 0.0001f);
    }
}
