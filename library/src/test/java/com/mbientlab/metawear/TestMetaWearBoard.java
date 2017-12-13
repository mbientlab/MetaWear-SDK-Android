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

import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.impl.platform.BtleGattCharacteristic;
import com.mbientlab.metawear.impl.platform.DeviceInformationService;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Iterator;
import java.util.concurrent.TimeoutException;

import bolts.AggregateException;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by etsai on 9/18/16.
 */
@RunWith(Enclosed.class)
public class TestMetaWearBoard {
    public static class TestConnect extends UnitTestBase {
        @Test
        public void connectWithDiscovery() throws Exception {
            byte[][] expected = {
                    {0x01, (byte) (byte) 0x80}, {0x02, (byte) 0x80}, {0x03, (byte) 0x80}, {0x04, (byte) 0x80},
                    {0x05, (byte) 0x80}, {0x06, (byte) 0x80}, {0x07, (byte) 0x80}, {0x08, (byte) 0x80},
                    {0x09, (byte) 0x80}, {0x0a, (byte) 0x80}, {0x0b, (byte) 0x80}, {0x0c, (byte) 0x80},
                    {0x0d, (byte) 0x80}, {0x0f, (byte) 0x80}, {0x10, (byte) 0x80}, {0x11, (byte) 0x80},
                    {0x12, (byte) 0x80}, {0x13, (byte) 0x80}, {0x14, (byte) 0x80}, {0x15, (byte) 0x80},
                    {0x16, (byte) 0x80}, {0x17, (byte) 0x80}, {0x18, (byte) 0x80}, {0x19, (byte) 0x80},
                    {(byte) 0xfe, (byte) 0x80}, {0x0b, (byte) 0x84}
            };

            junitPlatform.firmware = "1.1.3";
            junitPlatform.delayModuleInfoResponse = true;

            connectToBoard();
            assertArrayEquals(expected, junitPlatform.getConnectCommands());
        }

        @Test
        public void connectNoDiscovery() throws Exception {
            byte[][] expected = {
                    {0x0b, (byte) 0x84}
            };

            junitPlatform.deserializeModuleInfo = true;
            junitPlatform.delayModuleInfoResponse = true;

            connectToBoard();
            assertArrayEquals(expected, junitPlatform.getConnectCommands());
        }

        @Test(expected = TimeoutException.class)
        public void serviceDiscoveryTimeout() throws Exception {
            junitPlatform.addCustomModuleInfo(new byte[]{0xf, (byte) 0xff});

            Task<Void> task = mwBoard.connectAsync();
            task.waitForCompletion();

            throw task.getError();
        }

        @Test()
        public void serviceDiscoveryTimeoutConn() throws InterruptedException {
            int expected = 1;

            junitPlatform.addCustomModuleInfo(new byte[]{0xf, (byte) 0xff});
            mwBoard.connectAsync().waitForCompletion();

            assertEquals(expected, junitPlatform.nDisconnects);
        }

        @Test()
        public void serviceDiscoveryRetry() throws InterruptedException {
            junitPlatform.addCustomModuleInfo(new byte[]{0xf, (byte) 0xff});
            mwBoard.connectAsync().waitForCompletion();

            byte[][] checkpoint1 = {
                    {0x01, (byte) (byte) 0x80}, {0x02, (byte) 0x80}, {0x03, (byte) 0x80}, {0x04, (byte) 0x80},
                    {0x05, (byte) 0x80}, {0x06, (byte) 0x80}, {0x07, (byte) 0x80}, {0x08, (byte) 0x80},
                    {0x09, (byte) 0x80}, {0x0a, (byte) 0x80}, {0x0b, (byte) 0x80}, {0x0c, (byte) 0x80},
                    {0x0d, (byte) 0x80}, {0x0f, (byte) 0x80}
            };
            assertArrayEquals(checkpoint1, junitPlatform.getConnectCommands());

            junitPlatform.removeCustomModuleInfo((byte) 0xf);
            junitPlatform.connectCmds.clear();
            mwBoard.connectAsync().waitForCompletion();

            byte[][] checkpoint2 = {
                {0x0f, (byte) 0x80}, {0x10, (byte) 0x80}, {0x11, (byte) 0x80},
                {0x12, (byte) 0x80}, {0x13, (byte) 0x80}, {0x14, (byte) 0x80}, {0x15, (byte) 0x80},
                {0x16, (byte) 0x80}, {0x17, (byte) 0x80}, {0x18, (byte) 0x80}, {0x19, (byte) 0x80},
                {(byte) 0xfe, (byte) 0x80}, {0x0b, (byte) 0x84}
            };
            assertArrayEquals(checkpoint2, junitPlatform.getConnectCommands());
        }

        @Test
        public void longFirmwareString() throws Exception {
            junitPlatform.firmware = "1.3.90";
            connectToBoard();

            Task<DeviceInformation> task = mwBoard.readDeviceInformationAsync();
            task.waitForCompletion();

            assertTrue(mwBoard.isConnected());
            assertEquals(junitPlatform.firmware, task.getResult().firmwareRevision);
        }
    }

    public static class TestMetaBoot extends UnitTestBase {
        @Test
        public void metabootServiceDiscovery() throws Exception {
            junitPlatform.enableMetaBootState = true;
            connectToBoard();

            byte[][] expected = new byte[][]{};
            assertArrayEquals(expected, junitPlatform.getConnectCommands());
        }

        @Test
        public void metabootReadGattChar() throws Exception {
            BtleGattCharacteristic[] expected = new BtleGattCharacteristic[]{
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
    }

    public static class TestDeviceInfo extends UnitTestBase {
        @Test
        public void readDeviceInfo() throws Exception {
            final DeviceInformation expected = new DeviceInformation("MbientLab Inc", "deadbeef", "003BF9", "1.2.3", "cafebabe");

            connectToBoard();

            Task<DeviceInformation> task = mwBoard.readDeviceInformationAsync();
            task.waitForCompletion();

            assertEquals(expected, task.getResult());
        }

        @Test(expected = TimeoutException.class)
        public void readDeviceInfoTimeout() throws Throwable {
            connectToBoard();

            junitPlatform.delayReadDevInfo = true;
            Task<DeviceInformation> task = mwBoard.readDeviceInformationAsync();
            task.waitForCompletion();

            AggregateException exception = (AggregateException) task.getError();
            assertEquals(2, exception.getInnerThrowables().size());
            throw exception.getInnerThrowables().get(1);
        }

        @Test
        public void unknownBoard() throws Exception {
            junitPlatform.boardInfo = new MetaWearBoardInfo("7", AccelerometerBmi160.class);
            connectToBoard();

            assertNull(mwBoard.getModelString());
        }

        final static JSONObject EXPECTED_MODULE_DUMP;
        static {
            EXPECTED_MODULE_DUMP = new JSONObject();

            try {
                EXPECTED_MODULE_DUMP.put("Switch", new JSONObject().put("implementation", 0).put("revision", 0));
                EXPECTED_MODULE_DUMP.put("Led", new JSONObject().put("implementation", 0).put("revision", 0));
                EXPECTED_MODULE_DUMP.put("Accelerometer", new JSONObject().put("implementation", 1).put("revision", 1));
                EXPECTED_MODULE_DUMP.put("Temperature", new JSONObject().put("implementation", 1).put("revision", 0).put("extra", "[0x00, 0x03, 0x01, 0x02]"));
                EXPECTED_MODULE_DUMP.put("Gpio", new JSONObject().put("implementation", 0).put("revision", 0).put("extra", "[0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01]"));
                EXPECTED_MODULE_DUMP.put("NeoPixel", new JSONObject().put("implementation", 0).put("revision", 0));
                EXPECTED_MODULE_DUMP.put("IBeacon", new JSONObject().put("implementation", 0).put("revision", 0));
                EXPECTED_MODULE_DUMP.put("Haptic", new JSONObject().put("implementation", 0).put("revision", 0));
                EXPECTED_MODULE_DUMP.put("DataProcessor", new JSONObject().put("implementation", 0).put("revision", 1).put("extra", "[0x1c]"));
                EXPECTED_MODULE_DUMP.put("Event", new JSONObject().put("implementation", 0).put("revision", 0).put("extra", "[0x1c]"));
                EXPECTED_MODULE_DUMP.put("Logging", new JSONObject().put("implementation", 0).put("revision", 2).put("extra", "[0x08, 0x80, 0x2d, 0x00, 0x00]"));
                EXPECTED_MODULE_DUMP.put("Timer", new JSONObject().put("implementation", 0).put("revision", 0).put("extra", "[0x08]"));
                EXPECTED_MODULE_DUMP.put("SerialPassthrough", new JSONObject().put("implementation", 0).put("revision", 1));
                EXPECTED_MODULE_DUMP.put("Macro", new JSONObject().put("implementation", 0).put("revision", 1).put("extra", "[0x08]"));
                EXPECTED_MODULE_DUMP.put("Conductance", new JSONObject());
                EXPECTED_MODULE_DUMP.put("Settings", new JSONObject().put("implementation", 0).put("revision", 5).put("extra", "[0x03]"));
                EXPECTED_MODULE_DUMP.put("Barometer", new JSONObject().put("implementation", 0).put("revision", 0));
                EXPECTED_MODULE_DUMP.put("Gyro", new JSONObject().put("implementation", 0).put("revision", 1));
                EXPECTED_MODULE_DUMP.put("AmbientLight", new JSONObject().put("implementation", 0).put("revision", 0));
                EXPECTED_MODULE_DUMP.put("Magnetometer", new JSONObject().put("implementation", 0).put("revision", 1));
                EXPECTED_MODULE_DUMP.put("Humidity", new JSONObject());
                EXPECTED_MODULE_DUMP.put("Color", new JSONObject());
                EXPECTED_MODULE_DUMP.put("Proximity", new JSONObject());
                EXPECTED_MODULE_DUMP.put("SensorFusion", new JSONObject().put("implementation", 0).put("revision", 0).put("extra", "[0x03, 0x00, 0x06, 0x00, 0x02, 0x00, 0x01, 0x00]"));
                EXPECTED_MODULE_DUMP.put("Debug", new JSONObject().put("implementation", 0).put("revision", 0));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Test
        public void moduleInfoDump() throws Exception {
            junitPlatform.boardInfo = MetaWearBoardInfo.MOTION_R;
            connectToBoard();

            junitPlatform.delayModuleInfoResponse = true;
            Task<JSONObject> task = mwBoard.dumpModuleInfo(null);
            task.waitForCompletion();

            assertEquals(EXPECTED_MODULE_DUMP.length(), task.getResult().length());
            Iterator<String> it = task.getResult().keys();
            while(it.hasNext()) {
                String current = it.next();
                if (!EXPECTED_MODULE_DUMP.has(current) || !task.getResult().has(current)) {
                    assertFalse(task.getResult().has(current));
                    assertFalse(EXPECTED_MODULE_DUMP.has(current));
                } else {
                    assertEquals(EXPECTED_MODULE_DUMP.get(current).toString(), task.getResult().get(current).toString());
                }
            }
        }

        @Test
        public void partialModuleInfoDump() throws Exception {
            byte[][] expected = {
                    {0x01, (byte) (byte) 0x80}, {0x02, (byte) 0x80}, {0x03, (byte) 0x80}, {0x04, (byte) 0x80},
                    {0x05, (byte) 0x80}, {0x06, (byte) 0x80}, {0x07, (byte) 0x80}, {0x08, (byte) 0x80},
                    {0x12, (byte) 0x80}, {0x13, (byte) 0x80}, {0x14, (byte) 0x80}, {0x15, (byte) 0x80},
                    {0x16, (byte) 0x80}, {0x17, (byte) 0x80}, {0x18, (byte) 0x80}, {0x19, (byte) 0x80},
                    {(byte) 0xfe, (byte) 0x80}
            };

            junitPlatform.boardInfo = MetaWearBoardInfo.MOTION_R;
            connectToBoard();

            JSONObject partialDump = new JSONObject();
            for(String it: new String[] {"DataProcessor", "Event", "Logging", "Timer", "SerialPassthrough", "Macro", "Conductance", "Settings"}) {
                partialDump.put(it, EXPECTED_MODULE_DUMP.optJSONObject(it));
            }

            junitPlatform.connectCmds.clear();
            junitPlatform.delayModuleInfoResponse = true;
            Task<JSONObject> task = mwBoard.dumpModuleInfo(partialDump);
            task.waitForCompletion();

            assertArrayEquals(expected, junitPlatform.getConnectCommands());
        }

        @Test(expected = TaskTimeoutException.class)
        public void moduleInfoDumpTimeout() throws Exception {
            junitPlatform.boardInfo = MetaWearBoardInfo.MOTION_R;
            connectToBoard();

            junitPlatform.maxModule = 0x8;
            junitPlatform.delayModuleInfoResponse = true;
            Task<JSONObject> task = mwBoard.dumpModuleInfo(null);
            task.waitForCompletion();

            TaskTimeoutException exception = (TaskTimeoutException) task.getError();
            JSONObject partial = (JSONObject) exception.partial, expectedPartial = new JSONObject();

            for(String it: new String[] {"Switch", "Led", "Accelerometer", "Temperature", "Gpio", "NeoPixel", "IBeacon", "Haptic"}) {
                expectedPartial.put(it, EXPECTED_MODULE_DUMP.optJSONObject(it));
            }

            assertEquals(expectedPartial.length(), partial.length());
            Iterator<String> it = partial.keys();
            String current;
            while(it.hasNext()) {
                current = it.next();
                if (!expectedPartial.has(current) || !partial.has(current)) {
                    assertFalse(current, partial.has(current));
                    assertFalse(expectedPartial.has(current));
                } else {
                    assertEquals(expectedPartial.get(current).toString(), partial.get(current).toString());
                }
            }

            throw exception;
        }
    }

    public static class TestRoute extends UnitTestBase {
        @Test
        public void createChain1() throws Exception {
            byte[][] expected = new byte[][]{
                    {0x09, 0x02, 0x19, 0x08, (byte) 0xff, (byte) 0b11100000, 8, 0b00010111, 20, 0, 0, 0},
                    {0x09, 0x03, 0x01},
                    {0x09, 0x07, 0x00, 0x01},
                    {0x03, 0x1c, 0x01}
            };

            junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, GyroBmi160.class, MagnetometerBmm150.class, SensorFusionBosch.class);
            connectToBoard();

            mwBoard.getModule(SensorFusionBosch.class).eulerAngles().addRouteAsync(source -> source.limit(20).stream(null))
                    .continueWithTask(task -> mwBoard.getModule(Accelerometer.class).packedAcceleration().addRouteAsync(source -> source.stream(null)))
                    .waitForCompletion();
            assertArrayEquals(expected, junitPlatform.getCommands());
        }

        @Test
        public void createChain2() throws Exception {
            byte[][] expected = new byte[][]{
                    {9, 2, 0x19, 0x08, (byte) 0xff, (byte) 0b11100000, 8, 0b00010111, 20, 0, 0, 0},
                    {0x09, 0x03, 0x01},
                    {0x09, 0x07, 0x00, 0x01},
                    {0x03, 0x1c, 0x01}
            };

            junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, GyroBmi160.class, MagnetometerBmm150.class, SensorFusionBosch.class);
            connectToBoard();

            mwBoard.getModule(SensorFusionBosch.class).eulerAngles().addRouteAsync(source -> source.limit(20).stream(null))
                    .continueWithTask(task -> mwBoard.getModule(Accelerometer.class).packedAcceleration().addRouteAsync(source -> source.stream(null)))
                    .waitForCompletion();
            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }
}
