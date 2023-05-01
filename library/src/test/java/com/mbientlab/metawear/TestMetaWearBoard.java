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

import static com.mbientlab.metawear.Executors.IMMEDIATE_EXECUTOR;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.mbientlab.metawear.impl.platform.BtleGattCharacteristic;
import com.mbientlab.metawear.impl.platform.DeviceInformationService;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * Created by etsai on 9/18/16.
 */
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

            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                assertArrayEquals(expected, junitPlatform.getConnectCommands());
                doneSignal.countDown();
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void connectNoDiscovery() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            byte[][] expected = {
                    {0x01, (byte) (byte) 0x80}, {0x02, (byte) 0x80}, {0x03, (byte) 0x80}, {0x04, (byte) 0x80},
                    {0x05, (byte) 0x80}, {0x06, (byte) 0x80}, {0x07, (byte) 0x80}, {0x08, (byte) 0x80},
                    {0x09, (byte) 0x80}, {0x0a, (byte) 0x80}, {0x0b, (byte) 0x80}, {0x0c, (byte) 0x80},
                    {0x0d, (byte) 0x80}, {0x0f, (byte) 0x80}, {0x10, (byte) 0x80}, {0x11, (byte) 0x80},
                    {0x12, (byte) 0x80}, {0x13, (byte) 0x80}, {0x14, (byte) 0x80}, {0x15, (byte) 0x80},
                    {0x16, (byte) 0x80}, {0x17, (byte) 0x80}, {0x18, (byte) 0x80}, {0x19, (byte) 0x80},
                    {(byte) 0xfe, (byte) 0x80}, {0x0b, (byte) 0x84}
            };

            junitPlatform.deserializeModuleInfo = true;
            junitPlatform.delayModuleInfoResponse = true;

            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                assertArrayEquals(expected, junitPlatform.getConnectCommands());
                doneSignal.countDown();
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void serviceDiscoveryTimeout() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            junitPlatform.addCustomModuleInfo(new byte[]{0xf, (byte) 0xff});

            mwBoard.connectWithRetryAsync(3).addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                assertInstanceOf(TimeoutException.class, exception);
                doneSignal.countDown();
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test()
        public void serviceDiscoveryTimeoutConn() throws InterruptedException {
            int expected = 3;

            junitPlatform.addCustomModuleInfo(new byte[]{0xf, (byte) 0xff});
            mwBoard.connectWithRetryAsync(3).addOnSuccessListener(ignored -> {
                assertEquals(expected, junitPlatform.nConnects);
            });
        }

        @Test()
        public void serviceDiscoveryRetry() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            junitPlatform.addCustomModuleInfo(new byte[]{0xf, (byte) 0xff});
            mwBoard.connectAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                byte[][] checkpoint1 = {
                        {0x01, (byte) (byte) 0x80}, {0x02, (byte) 0x80}, {0x03, (byte) 0x80}, {0x04, (byte) 0x80},
                        {0x08, (byte) 0x80},
                        {0x09, (byte) 0x80}, {0x0a, (byte) 0x80}, {0x0b, (byte) 0x80}, {0x0c, (byte) 0x80},
                        {0x0d, (byte) 0x80}, {0x0f, (byte) 0x80}
                };
                assertArrayEquals(checkpoint1, junitPlatform.getConnectCommands());

                junitPlatform.removeCustomModuleInfo((byte) 0xf);
                junitPlatform.connectCmds.clear();
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored2 -> {
                mwBoard.connectAsync();
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored3 -> {
                byte[][] checkpoint2 = {
                        {0x0f, (byte) 0x80}, {0x11, (byte) 0x80},
                        {0x12, (byte) 0x80}, {0x13, (byte) 0x80}, {0x14, (byte) 0x80}, {0x15, (byte) 0x80},
                        {0x16, (byte) 0x80}, {0x17, (byte) 0x80}, {0x18, (byte) 0x80}, {0x19, (byte) 0x80},
                        {(byte) 0xfe, (byte) 0x80}, {0x0b, (byte) 0x84}
                };
                assertArrayEquals(checkpoint2, junitPlatform.getConnectCommands());
                doneSignal.countDown();
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void longFirmwareString() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            junitPlatform.firmware = "1.3.90";
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.readDeviceInformationAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                    assertTrue(mwBoard.isConnected());
                    assertEquals(junitPlatform.firmware, task.firmwareRevision);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }

    public static class TestMetaBoot extends UnitTestBase {
        @Test
        public void metabootServiceDiscovery() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            junitPlatform.enableMetaBootState = true;
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                byte[][] expected = new byte[][]{};
                assertArrayEquals(expected, junitPlatform.getConnectCommands());
                doneSignal.countDown();
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void metabootReadGattChar() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            BtleGattCharacteristic[] expected = new BtleGattCharacteristic[]{
                    DeviceInformationService.FIRMWARE_REVISION,
                    DeviceInformationService.MODEL_NUMBER,
                    DeviceInformationService.HARDWARE_REVISION
            };

            junitPlatform.enableMetaBootState = true;
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                assertArrayEquals(expected, junitPlatform.getGattCharReadHistory());
                doneSignal.countDown();
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void metabootGetModule() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            junitPlatform.boardInfo = new MetaWearBoardInfo(Accelerometer.class);
            junitPlatform.enableMetaBootState = true;
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                assertThrows(com.mbientlab.metawear.UnsupportedModuleException.class, () -> mwBoard.getModuleOrThrow(Accelerometer.class));
                doneSignal.countDown();
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }

    public static class TestDeviceInfo extends UnitTestBase {
        @Test
        public void readDeviceInfo() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            final DeviceInformation expected = new DeviceInformation("MbientLab Inc", "deadbeef", "003BF9", "1.2.3", "cafebabe");

            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.readDeviceInformationAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                    assertEquals(expected, task);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void readDeviceInfoTimeout() throws Throwable {
            CountDownLatch doneSignal = new CountDownLatch(1);
            junitPlatform.delayReadDevInfo = true;

            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.readDeviceInformationAsync().addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                    assertInstanceOf(TimeoutException.class, exception);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void unknownBoard() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            junitPlatform.boardInfo = new MetaWearBoardInfo("7", AccelerometerBmi160.class);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                assertNull(mwBoard.getModelString());
                doneSignal.countDown();
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        final static JSONObject EXPECTED_MODULE_DUMP;
        static {
            EXPECTED_MODULE_DUMP = new JSONObject();

            try {
                EXPECTED_MODULE_DUMP.put("Switch", new JSONObject().put("implementation", 0).put("revision", 0));
                EXPECTED_MODULE_DUMP.put("Led", new JSONObject().put("implementation", 0).put("revision", 0));
                EXPECTED_MODULE_DUMP.put("Accelerometer", new JSONObject().put("implementation", 1).put("revision", 1));
                EXPECTED_MODULE_DUMP.put("Temperature", new JSONObject().put("implementation", 1).put("revision", 0).put("extra", "[0x00, 0x03, 0x01, 0x02]"));
                EXPECTED_MODULE_DUMP.put("Haptic", new JSONObject().put("implementation", 0).put("revision", 0));
                EXPECTED_MODULE_DUMP.put("DataProcessor", new JSONObject().put("implementation", 0).put("revision", 1).put("extra", "[0x1c]"));
                EXPECTED_MODULE_DUMP.put("Event", new JSONObject().put("implementation", 0).put("revision", 0).put("extra", "[0x1c]"));
                EXPECTED_MODULE_DUMP.put("Logging", new JSONObject().put("implementation", 0).put("revision", 2).put("extra", "[0x08, 0x80, 0x2d, 0x00, 0x00]"));
                EXPECTED_MODULE_DUMP.put("Timer", new JSONObject().put("implementation", 0).put("revision", 0).put("extra", "[0x08]"));
                EXPECTED_MODULE_DUMP.put("SerialPassthrough", new JSONObject().put("implementation", 0).put("revision", 1));
                EXPECTED_MODULE_DUMP.put("Macro", new JSONObject().put("implementation", 0).put("revision", 1).put("extra", "[0x08]"));
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
            CountDownLatch doneSignal = new CountDownLatch(1);
            junitPlatform.boardInfo = MetaWearBoardInfo.MOTION_R;
            junitPlatform.delayModuleInfoResponse = true;

            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.dumpModuleInfo(null).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                    assertEquals(EXPECTED_MODULE_DUMP.length(), task.length());
                    Iterator<String> it = task.keys();
                    while(it.hasNext()) {
                        String current = it.next();
                        if (!EXPECTED_MODULE_DUMP.has(current) || !task.has(current)) {
                            assertFalse(task.has(current));
                            assertFalse(EXPECTED_MODULE_DUMP.has(current));
                        } else {
                            try {
                                JSONAssert.assertEquals(EXPECTED_MODULE_DUMP.get(current).toString(), task.get(current).toString(), false);
                            } catch (JSONException e) {
                                fail(e);
                            }
                        }
                    }
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void partialModuleInfoDump() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            byte[][] expected = {
                    {0x01, (byte) (byte) 0x80}, {0x02, (byte) 0x80}, {0x03, (byte) 0x80}, {0x04, (byte) 0x80},
                    {0x08, (byte) 0x80},
                    {0x12, (byte) 0x80}, {0x13, (byte) 0x80}, {0x14, (byte) 0x80}, {0x15, (byte) 0x80},
                    {0x16, (byte) 0x80}, {0x17, (byte) 0x80}, {0x18, (byte) 0x80}, {0x19, (byte) 0x80},
                    {(byte) 0xfe, (byte) 0x80}
            };

            junitPlatform.boardInfo = MetaWearBoardInfo.MOTION_R;
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                JSONObject partialDump = new JSONObject();
                for(String it: new String[] {"DataProcessor", "Event", "Logging", "Timer", "SerialPassthrough", "Macro", "Conductance", "Settings"}) {
                    try {
                        partialDump.put(it, EXPECTED_MODULE_DUMP.optJSONObject(it));
                    } catch (JSONException e) {
                        fail(e);
                    }
                }

                junitPlatform.connectCmds.clear();
                junitPlatform.delayModuleInfoResponse = true;

                mwBoard.dumpModuleInfo(partialDump).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored2 -> {
                    assertArrayEquals(expected, junitPlatform.getConnectCommands());
                    doneSignal.countDown();
                });
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {

            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void moduleInfoDumpTimeout() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            junitPlatform.boardInfo = MetaWearBoardInfo.MOTION_R;
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                junitPlatform.maxModule = 0x8;
                junitPlatform.delayModuleInfoResponse = true;
                mwBoard.dumpModuleInfo(null).addOnFailureListener(IMMEDIATE_EXECUTOR, task -> {
                    TaskTimeoutException exception = (TaskTimeoutException) task;
                    JSONObject partial = (JSONObject) exception.partial, expectedPartial = new JSONObject();

                    for(String it: new String[] {"Switch", "Led", "Accelerometer", "Temperature", "Gpio", "Haptic"}) {
                        try {
                            expectedPartial.put(it, EXPECTED_MODULE_DUMP.optJSONObject(it));
                        } catch (JSONException e) {
                            fail(e);
                        }
                    }

                    assertEquals(expectedPartial.length(), partial.length());
                    Iterator<String> it = partial.keys();
                    String current;
                    while(it.hasNext()) {
                        current = it.next();
                        if (!expectedPartial.has(current) || !partial.has(current)) {
                            assertFalse(partial.has(current), current);
                            assertFalse(expectedPartial.has(current));
                        } else {
                            try {
                                JSONAssert.assertEquals(expectedPartial.getJSONObject(current).toString(), partial.getJSONObject(current).toString(), false);
                            } catch (JSONException e) {
                                fail(e);
                            }
                        }
                    }

                    assertInstanceOf(TaskTimeoutException.class, exception);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }

    public static class TestRoute extends UnitTestBase {
        @Test
        public void createChain1() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            byte[][] expected = new byte[][]{
                    {0x09, 0x02, 0x19, 0x08, (byte) 0xff, (byte) 0b11100000, 8, 0b00010111, 20, 0, 0, 0},
                    {0x09, 0x03, 0x01},
                    {0x09, 0x07, 0x00, 0x01},
                    {0x03, 0x1c, 0x01}
            };

            junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, Gyro.class, MagnetometerBmm150.class, SensorFusionBosch.class);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.getModule(SensorFusionBosch.class).eulerAngles().addRouteAsync(source -> source.limit(20).stream(null))
                        .continueWithTask(IMMEDIATE_EXECUTOR, task -> mwBoard.getModule(Accelerometer.class).packedAcceleration().addRouteAsync(source -> source.stream(null)))
                        .addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                            assertArrayEquals(expected, junitPlatform.getCommands());
                            doneSignal.countDown();
                        });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void createChain2() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            byte[][] expected = new byte[][]{
                    {9, 2, 0x19, 0x08, (byte) 0xff, (byte) 0b11100000, 8, 0b00010111, 20, 0, 0, 0},
                    {0x09, 0x03, 0x01},
                    {0x09, 0x07, 0x00, 0x01},
                    {0x03, 0x1c, 0x01}
            };

            junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, Gyro.class, MagnetometerBmm150.class, SensorFusionBosch.class);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.getModule(SensorFusionBosch.class).eulerAngles().addRouteAsync(source -> source.limit(20).stream(null))
                        .continueWithTask(IMMEDIATE_EXECUTOR, task -> mwBoard.getModule(Accelerometer.class).packedAcceleration().addRouteAsync(source -> source.stream(null)))
                        .addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                            assertArrayEquals(expected, junitPlatform.getCommands());
                            doneSignal.countDown();
                        });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }

    private static String[] fileToNames(List<File> files) {
        String[] names = new String[files.size()];
        int i = 0;
        for(File f: files) {
            names[i] = f.getName();
            i++;
        }

        return names;
    }

    public static class TestFirmwareRetrieval extends UnitTestBase {
        private static Stream<Arguments> data() {
            List<Arguments> parameters = new LinkedList<>();
            List<String> arg1 = new ArrayList<>();
            arg1.add("1.2.5");
            arg1.add("1.4.0");
            arg1.add("1.5.0");
            parameters.add(Arguments.of(false, arg1));
            List<String> arg2= new ArrayList<>();
            arg2.add("0.2.1");
            arg2.add("0.3.1");
            arg2.add("0.3.1");
            parameters.add(Arguments.of(true, arg2));
            return parameters.stream();
        }

        public void setup(boolean enableMetaboot) {
            try {
                junitPlatform.enableMetaBootState = enableMetaboot;
                junitPlatform.boardInfo = MetaWearBoardInfo.MOTION_R;
            } catch (Exception e) {
                fail(e);
            }
        }

        @ParameterizedTest
        @MethodSource("data")
        public void updateFromOldBootLoader(boolean enableMetaboot, List<String> firmwareRevisions) throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            setup(enableMetaboot);
            junitPlatform.firmware = firmwareRevisions.get(0);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.downloadFirmwareUpdateFilesAsync("1.4.0").addOnSuccessListener(IMMEDIATE_EXECUTOR, filesTask -> {

                    // Firmware prior to v1.4.0 will need to upload 3 files with the Nordic DFU lib
                    final String[] expected = new String[] {
                            "0.1_5_bootloader_0.2.2_bl.zip",
                            "0.1_5_bootloader_0.3.1_sd_bl.zip",
                            "0.1_5_vanilla_1.4.0_firmware.zip",
                    };
                    assertArrayEquals(expected, fileToNames(filesTask));
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @ParameterizedTest
        @MethodSource("data")
        public void updateWithOldDfuFn(boolean enableMetaboot, List<String> firmwareRevisions) throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            setup(enableMetaboot);
            junitPlatform.firmware = firmwareRevisions.get(0);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                //  Updating from older firmware to v1.4.0 requires 3 files, old dfu function only returns 1
                mwBoard.downloadFirmwareAsync("1.4.0").addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                    assertInstanceOf(UnsupportedOperationException.class, exception);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @ParameterizedTest
        @MethodSource("data")
        public void updateFromNewBootLoader(boolean enableMetaboot, List<String> firmwareRevisions) throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            setup(enableMetaboot);
            junitPlatform.firmware = firmwareRevisions.get(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.downloadFirmwareUpdateFilesAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, filesTask -> {
                    // Firmware v1.4.0+ can just upload the next firmware image
                    final String[] expected = new String[] {
                            "0.1_5_vanilla_1.5.0_firmware.zip",
                    };
                    assertArrayEquals(expected, fileToNames(filesTask));
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @ParameterizedTest
        @MethodSource("data")
        public void downgradeToOldBootloader(boolean enableMetaboot, List<String> firmwareRevisions) throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            setup(enableMetaboot);
            junitPlatform.firmware = firmwareRevisions.get(2);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                // Firmware v1.4.0+ cannot downgrade below v1.4.0
                mwBoard.downloadFirmwareUpdateFilesAsync("1.2.5").addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                    assertInstanceOf(IllegalFirmwareFile.class, exception);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }

    public static class TestFirmwareCheck extends UnitTestBase {
        private static Stream<Arguments> data() {
            List<Arguments> params = new LinkedList<>();
            params.add(Arguments.of("1.5.0", "1.2.5"));
            params.add(Arguments.of(null, "1.5.0"));
            return params.stream();
        }

        public Task<Void> setup(String firmwareRevision) {
            try {
                junitPlatform.boardInfo = MetaWearBoardInfo.MOTION_R;
                junitPlatform.firmware = firmwareRevision;
                return connectToBoard();
            } catch (Exception e) {
                fail(e);
                return Tasks.forException(e);
            }
        }

        @ParameterizedTest
        @MethodSource("data")
        public void versionString(String expected, String firmwareRevision) throws Exception {
            setup(firmwareRevision).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.findLatestAvailableFirmwareAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, stringTask -> {
                    assertEquals(expected, stringTask);
                });
            });
        }

        @ParameterizedTest
        @MethodSource("data")
        public void versionBoolean(String expected, String firmwareRevision) throws Exception {
            setup(firmwareRevision).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.checkForFirmwareUpdateAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, boolTask -> {
                    assertEquals(expected != null, boolTask);
                });
            });
        }
    }

    public static class TestApiCheckFail extends UnitTestBase {
        public TestApiCheckFail() {
            super("3.4.7");
        }

        @Test
        public void updateToLatest() throws Exception {
            junitPlatform.boardInfo = MetaWearBoardInfo.MOTION_R;
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.downloadFirmwareUpdateFilesAsync().addOnFailureListener(exception -> {
                    assertInstanceOf(UnsupportedOperationException.class, exception);
                });
            });
        }
    }

    public static class TestFirmwareBuildCheck extends UnitTestBase {
        private static Stream<Arguments> data() {
            List<Arguments> params = new LinkedList<>();
            params.add(Arguments.of((byte) 0x80, "128"));
            params.add(Arguments.of((byte) 0x00, "vanilla"));
            return params.stream();
        }

        @BeforeEach
        public void setup() {
            junitPlatform.boardInfo = MetaWearBoardInfo.MOTION_R;
            junitPlatform.firmware = "1.4.2";
        }

        @ParameterizedTest
        @MethodSource("data")
        public void checkFilename(byte revision, String buildName) throws Exception {
            junitPlatform.addCustomModuleInfo(new byte[] {0x11, (byte) 0x80, 0x00, 0x08, 0x03, revision});
            connectToBoard().addOnSuccessListener(ignored -> {
                mwBoard.downloadFirmwareUpdateFilesAsync().addOnSuccessListener(filesTask -> {
                    // Firmware v1.4.0+ can just upload the next firmware image
                    final String[] expected = new String[] {
                            String.format(Locale.US, "0.1_5_%s_1.5.0_firmware.zip", buildName)
                    };
                    assertArrayEquals(expected, fileToNames(filesTask));
                });
            });
        }
    }
}
