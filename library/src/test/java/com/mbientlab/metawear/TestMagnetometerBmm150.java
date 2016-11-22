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

import com.mbientlab.metawear.datatype.CartesianFloat;
import com.mbientlab.metawear.module.MagnetometerBmm150;
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

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by etsai on 10/6/16.
 */
@RunWith(Parameterized.class)
public class TestMagnetometerBmm150 extends UnitTestBase {
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { MetaWearBoardInfo.CPRO },
                { MetaWearBoardInfo.MOTIOON_R }
        });
    }

    @Parameter
    public MetaWearBoardInfo boardInfo;

    private MagnetometerBmm150 mag;

    @Before
    public void setup() throws Exception {
        btlePlaform.boardInfo= boardInfo;
        connectToBoard();

        mag= mwBoard.getModule(MagnetometerBmm150.class);
    }

    @Test
    public void enableBFieldSampling() {
        byte[] expected= new byte[] {0x15, 0x02, 0x01, 0x00};

        mag.magneticField().start();
        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }

    @Test
    public void disableBFieldSampling() {
        byte[] expected= new byte[] {0x15, 0x02, 0x00, 0x01};

        mag.magneticField().stop();
        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }

    @Test
    public void globalStart() {
        byte[] expected= new byte[] {0x15, 0x01, 0x01};

        mag.start();
        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }

    @Test
    public void globalStop() {
        byte[] expected= new byte[] {0x15, 0x01, 0x00};

        mag.stop();
        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }

    @Test
    public void bFieldSubscribe() {
        byte[] expected= new byte[] {0x15, 0x05, 0x01};
        mag.magneticField().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.stream(null);
            }
        });

        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }

    @Test
    public void bFieldUnsubscribe() {
        byte[] expected= new byte[] {0x15, 0x05, 0x00};
        mag.magneticField().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.stream(null);
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                task.getResult().unsubscribe(0);
                return null;
            }
        });

        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }

    @Test
    public void bFieldData() {
        CartesianFloat expected= new CartesianFloat(Float.intBitsToFloat(0xc37b2000), Float.intBitsToFloat(0x43253000), Float.intBitsToFloat(0x428ea000));
        final Capture<CartesianFloat> actual= new Capture<>();

        mag.magneticField().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object ... env) {
                        ((Capture<CartesianFloat>) env[0]).set(data.value(CartesianFloat.class));
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                task.getResult().setEnvironment(0, actual);
                return null;
            }
        });
        sendMockResponse(new byte[] {0x15, 0x05, 0x4e, (byte) 0xf0, 0x53, 0x0a, 0x75, 0x04});

        assertEquals(expected, actual.get());
    }

    @Test
    public void bFieldComponentData() {
        float[] expected = new float[] {-251.1250f, 165.1875f, 71.3125f};
        final float[] actual= new float[3];

        mag.magneticField().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.split()
                        .index(0).stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                ((float[]) env[0])[0] = data.value(Float.class);
                            }
                        })
                        .index(1).stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                ((float[]) env[0])[1] = data.value(Float.class);
                            }
                        })
                        .index(2).stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                ((float[]) env[0])[2] = data.value(Float.class);
                            }
                        });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                task.getResult().setEnvironment(0, (Object) actual);
                task.getResult().setEnvironment(1, (Object) actual);
                task.getResult().setEnvironment(2, (Object) actual);
                return null;
            }
        });
        sendMockResponse(new byte[] {0x15, 0x05, 0x4e, (byte) 0xf0, 0x53, 0x0a, 0x75, 0x04});

        assertArrayEquals(expected, actual, 0.001f);
    }
}
