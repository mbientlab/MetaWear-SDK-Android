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

import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.Passthrough;
import com.mbientlab.metawear.module.AccelerometerBosch;

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

/**
 * Created by etsai on 1/13/17.
 */
@RunWith(Parameterized.class)
public class TestAccelerometerBoschFlatRev2 extends UnitTestBase {
    private static final boolean[] EXPECTED = new boolean[] {
            true, false
    };

    @Parameters(name = "board: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { MetaWearBoardInfo.MOTION_R },
                { MetaWearBoardInfo.DETECTOR }
        });
    }

    @Parameter
    public MetaWearBoardInfo info;

    private AccelerometerBosch acc;

    @Before
    public void setup() throws Exception {
        byte[] moduleInfo = new byte[info.moduleResponses.get((byte) 3).length];
        System.arraycopy(info.moduleResponses.get((byte) 3), 0, moduleInfo, 0, moduleInfo.length);
        moduleInfo[3] = 0x2;

        junitPlatform.boardInfo = info;
        junitPlatform.addCustomModuleInfo(moduleInfo);

        connectToBoard();

        acc = mwBoard.getModule(AccelerometerBosch.class);
    }

    @Test
    public void response() {
        final byte[][] responses = new byte[][] {
                {0x03, 0x14, 0x07},
                {0x03, 0x14, 0x03}
        };
        final Capture<boolean[]> actual = new Capture<>();

        actual.set(new boolean[2]);
        acc.flat().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    int i = 0;
                    @Override
                    public void apply(Data data, Object... env) {
                        actual.get()[i] = data.value(Boolean.class);
                        i++;
                    }
                });
            }
        });
        for(byte[] it: responses) {
            sendMockResponse(it);
        }

        assertArrayEquals(EXPECTED, actual.get());
    }

    @Test
    public void dataProcessorResponse() throws InterruptedException {
        final byte[][] responses = new byte[][] {
                {0x09, 0x03, 0x00, 0x07},
                {0x09, 0x03, 0x00, 0x03}
        };
        final Capture<boolean[]> actual = new Capture<>();

        actual.set(new boolean[2]);
        acc.flat().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.limit(Passthrough.ALL, (short) 0).stream(new Subscriber() {
                    int i = 0;
                    @Override
                    public void apply(Data data, Object... env) {
                        actual.get()[i] = data.value(Boolean.class);
                        i++;
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {

                synchronized (TestAccelerometerBoschFlatRev2.this) {
                    TestAccelerometerBoschFlatRev2.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();

            for(byte[] it: responses) {
                sendMockResponse(it);
            }

            assertArrayEquals(EXPECTED, actual.get());
        }
    }

    @Test
    public void logResponse() throws InterruptedException {
        final byte[] responses = new byte[] {
                0x0b, 0x07,
                (byte) 0xe0, (byte) 0x95, (byte) 0x99, (byte) 0x88, 0x00, 0x07, 0x00, 0x00, 0x00,
                (byte) 0xe0, (byte) 0x81, (byte) 0xa3, (byte) 0x88, 0x00, 0x03, 0x00, 0x00, 0x00
        };
        final Capture<boolean[]> actual = new Capture<>();

        actual.set(new boolean[2]);
        acc.flat().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.log(new Subscriber() {
                    int i = 0;
                    @Override
                    public void apply(Data data, Object... env) {
                        actual.get()[i] = data.value(Boolean.class);
                        i++;
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {

                synchronized (TestAccelerometerBoschFlatRev2.this) {
                    TestAccelerometerBoschFlatRev2.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();

            sendMockResponse(responses);

            assertArrayEquals(EXPECTED, actual.get());
        }
    }
}
