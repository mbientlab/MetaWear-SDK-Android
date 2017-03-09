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
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.impl.platform.BtleGattCharacteristic;
import com.mbientlab.metawear.impl.platform.DeviceInformationService;

import org.junit.Test;

import java.util.concurrent.TimeoutException;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by etsai on 9/18/16.
 */
public class TestMetaWearBoard extends UnitTestBase {
    @Test
    public void connectWithDiscovery() throws Exception {
        byte[][] expected= {
                {0x01, (byte) (byte) 0x80}, {0x02, (byte) 0x80}, {0x03, (byte) 0x80}, {0x04, (byte) 0x80},
                {0x05, (byte) 0x80}, {0x06, (byte) 0x80}, {0x07, (byte) 0x80}, {0x08, (byte) 0x80},
                {0x09, (byte) 0x80}, {0x0a, (byte) 0x80}, {0x0b, (byte) 0x80}, {0x0c, (byte) 0x80},
                {0x0d, (byte) 0x80}, {0x0f, (byte) 0x80}, {0x10, (byte) 0x80}, {0x11, (byte) 0x80},
                {0x12, (byte) 0x80}, {0x13, (byte) 0x80}, {0x14, (byte) 0x80}, {0x15, (byte) 0x80},
                {0x16, (byte) 0x80}, {0x17, (byte) 0x80}, {0x18, (byte) 0x80}, {0x19, (byte) 0x80},
                {(byte) 0xfe, (byte) 0x80}, {0x0b, (byte) 0x84}
        };

        junitPlatform.firmware= "1.1.3";
        junitPlatform.delayModuleInfoResponse= true;

        connectToBoard();
        assertArrayEquals(expected, junitPlatform.getConnectCommands());
    }

    @Test
    public void connectNoDiscovery() throws Exception {
        byte[][] expected= {
                {0x0b, (byte) 0x84}
        };

        junitPlatform.deserializeModuleInfo= true;
        junitPlatform.delayModuleInfoResponse= true;

        connectToBoard();
        assertArrayEquals(expected, junitPlatform.getConnectCommands());
    }

    @Test(expected = TimeoutException.class)
    public void serviceDiscoveryTimeout() throws Exception {
        final Capture<Exception> actual = new Capture<>();

        junitPlatform.addCustomModuleInfo(new byte[] { 0xf, (byte) 0xff});
        mwBoard.connectAsync()
                .continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(Task<Void> task) throws Exception {
                        actual.set(task.getError());

                        synchronized (TestMetaWearBoard.this) {
                            TestMetaWearBoard.this.notifyAll();
                        }
                        return null;
                    }
                });

        synchronized (this) {
            this.wait();
            throw actual.get();
        }
    }

    @Test()
    public void serviceDiscoveryTimeoutConn() throws InterruptedException {
        int expected = 1;

        junitPlatform.addCustomModuleInfo(new byte[] { 0xf, (byte) 0xff});
        mwBoard.connectAsync()
                .continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(Task<Void> task) throws Exception {
                        synchronized (TestMetaWearBoard.this) {
                            TestMetaWearBoard.this.notifyAll();
                        }
                        return null;
                    }
                });

        synchronized (this) {
            this.wait();
            assertEquals(expected, junitPlatform.nDisconnects);
        }
    }

    @Test
    public void metabootServiceDiscovery() throws Exception {
        junitPlatform.enableMetaBootState = true;
        connectToBoard();

        byte[][] expected= new byte[][] {};
        assertArrayEquals(expected, junitPlatform.getConnectCommands());
    }

    @Test
    public void metabootReadGattChar() throws Exception {
        BtleGattCharacteristic[] expected = new BtleGattCharacteristic[] {
                DeviceInformationService.FIRMWARE_REVISION,
                DeviceInformationService.MODEL_NUMBER,
                DeviceInformationService.HARDWARE_REVISION
        };

        junitPlatform.enableMetaBootState = true;
        connectToBoard();

       assertArrayEquals(expected, junitPlatform.getGattCharReadHistory());
    }

    @Test(expected = UnsupportedModuleException.class)
    public void metabootGetModule() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(Accelerometer.class);
        junitPlatform.enableMetaBootState = true;
        connectToBoard();

        mwBoard.getModuleOrThrow(Accelerometer.class);
    }

    @Test
    public void readDeviceInfo() throws Exception {
        final DeviceInformation expected = new DeviceInformation("MbientLab Inc", "deadbeef", "003BF9", "1.2.3", "cafebabe");
        final Capture<DeviceInformation> actual = new Capture<>();

        connectToBoard();

        mwBoard.readDeviceInformationAsync().continueWith(new Continuation<DeviceInformation, Void>() {
            @Override
            public Void then(Task<DeviceInformation> task) throws Exception {
                actual.set(task.getResult());
                synchronized (TestMetaWearBoard.this) {
                    TestMetaWearBoard.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();
            assertEquals(expected, actual.get());
        }
    }

    @Test(expected = TimeoutException.class)
    public void readDeviceInfoTimeout() throws Exception {
        final Capture<Exception> actual = new Capture<>();

        connectToBoard();
        junitPlatform.delayReadDevInfo = true;
        mwBoard.readDeviceInformationAsync().continueWith(new Continuation<DeviceInformation, Void>() {
            @Override
            public Void then(Task<DeviceInformation> task) throws Exception {
                actual.set(task.getError());

                synchronized (TestMetaWearBoard.this) {
                    TestMetaWearBoard.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();
            throw actual.get();
        }
    }

    @Test
    public void routeChaining() throws Exception {
        byte[][] expected = new byte[][] {
                {0x09, 0x02, 0x19, 0x08, (byte) 0xff, (byte) 0b11100000, 8, 0b00010111, 20, 0, 0, 0},
                {0x09, 0x03, 0x01},
                {0x09, 0x07, 0x00, 0x01},
                {0x03, 0x1c, 0x01}
        };

        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, GyroBmi160.class, MagnetometerBmm150.class, SensorFusionBosch.class);
        connectToBoard();

        mwBoard.getModule(SensorFusionBosch.class).eulerAngles().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.limit(20).stream(null);
            }
        });
        mwBoard.getModule(Accelerometer.class).packedAcceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(null);
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                synchronized (TestMetaWearBoard.this) {
                    TestMetaWearBoard.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            wait();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    @Test
    public void routeChainingTask() throws Exception {
        byte[][] expected = new byte[][] {
                {9, 2, 0x19, 0x08, (byte) 0xff, (byte) 0b11100000, 8, 0b00010111, 20, 0, 0, 0},
                {0x09, 0x03, 0x01},
                {0x09, 0x07, 0x00, 0x01},
                {0x03, 0x1c, 0x01}
        };

        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, GyroBmi160.class, MagnetometerBmm150.class, SensorFusionBosch.class);
        connectToBoard();

        mwBoard.getModule(SensorFusionBosch.class).eulerAngles().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.limit(20).stream(null);
            }
        }).continueWithTask(new Continuation<Route, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Route> task) throws Exception {
                return mwBoard.getModule(Accelerometer.class).packedAcceleration().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(null);
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                synchronized (TestMetaWearBoard.this) {
                    TestMetaWearBoard.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            wait();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }
}
