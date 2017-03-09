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
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.Settings;

import org.junit.Before;
import org.junit.Test;

import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 12/10/16.
 */

public class TestSettingsRev5 extends UnitTestBase {
    private Settings settings;

    @Before
    public void setup() throws Exception {
        junitPlatform.addCustomModuleInfo(new byte[] {0x11, (byte) 0x80, 0x00, 0x05, 0x03});
        connectToBoard();

        settings = mwBoard.getModule(Settings.class);
    }

    @Test
    public void readPowerStatus() {
        byte[] expected = new byte[] {0x11, (byte) 0x91};

        settings.readCurrentPowerStatusAsync();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void readPowerStatusData() {
        byte[] expected = new byte[] {0x1, 0x0};
        final byte[] actual = new byte[2];

        settings.readCurrentPowerStatusAsync().continueWith(new Continuation<Byte, Void>() {
            @Override
            public Void then(Task<Byte> task) throws Exception {
                actual[0] = task.getResult();
                return null;
            }
        });
        settings.readCurrentPowerStatusAsync().continueWith(new Continuation<Byte, Void>() {
            @Override
            public Void then(Task<Byte> task) throws Exception {
                actual[1] = task.getResult();
                return null;
            }
        });

        sendMockResponse(new byte[] {0x11, (byte) 0x91, 0x1});
        sendMockResponse(new byte[] {0x11, (byte) 0x91, 0x0});
        assertArrayEquals(expected, actual);
    }

    @Test
    public void powerStatus() {
        byte[] expected = new byte[] {0x11, 0x11, 0x01};

        settings.powerStatus().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(null);
            }
        });

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void powerStatusData() {
        final byte[] actual = new byte[2];
        byte[] expected = new byte[] {0x0, 0x1};

        settings.powerStatus().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    int i = 0;

                    @Override
                    public void apply(Data data, Object... env) {
                        actual[i] = data.value(Byte.class);
                        i++;
                    }
                });
            }
        });

        sendMockResponse(new byte[] {0x11, 0x11, 0x00});
        sendMockResponse(new byte[] {0x11, 0x11, 0x01});

        assertArrayEquals(expected, actual);
    }

    @Test
    public void readChargeStatus() {
        byte[] expected = new byte[] {0x11, (byte) 0x92};

        settings.readCurrentChargeStatusAsync();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void readChargeStatusData() {
        byte[] expected = new byte[] {0x1, 0x0};
        final byte[] actual = new byte[2];

        settings.readCurrentChargeStatusAsync().continueWith(new Continuation<Byte, Void>() {
            @Override
            public Void then(Task<Byte> task) throws Exception {
                actual[0] = task.getResult();
                return null;
            }
        });
        settings.readCurrentChargeStatusAsync().continueWith(new Continuation<Byte, Void>() {
            @Override
            public Void then(Task<Byte> task) throws Exception {
                actual[1] = task.getResult();
                return null;
            }
        });

        sendMockResponse(new byte[] {0x11, (byte) 0x92, 0x1});
        sendMockResponse(new byte[] {0x11, (byte) 0x92, 0x0});
        assertArrayEquals(expected, actual);
    }

    @Test
    public void chargeStatus() {
        byte[] expected = new byte[] {0x11, 0x12, 0x01};

        settings.chargeStatus().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(null);
            }
        });

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void chargeStatusData() {
        final byte[] actual = new byte[2];
        byte[] expected = new byte[] {0x0, 0x1};

        settings.chargeStatus().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    int i = 0;

                    @Override
                    public void apply(Data data, Object... env) {
                        actual[i] = data.value(Byte.class);
                        i++;
                    }
                });
            }
        });

        sendMockResponse(new byte[] {0x11, 0x12, 0x00});
        sendMockResponse(new byte[] {0x11, 0x12, 0x01});

        assertArrayEquals(expected, actual);
    }
}
