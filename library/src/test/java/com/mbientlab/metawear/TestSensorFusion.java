///*
// * Copyright 2014-2018 MbientLab Inc. All rights reserved.
// *
// * IMPORTANT: Your use of this Software is limited to those specific rights granted under the terms of a software
// * license agreement between the user who downloaded the software, his/her employer (which must be your
// * employer) and MbientLab Inc, (the "License").  You may not use this Software unless you agree to abide by the
// * terms of the License which can be found at www.mbientlab.com/terms.  The License limits your use, and you
// * acknowledge, that the Software may be modified, copied, and distributed when used in conjunction with an
// * MbientLab Inc, product.  Other than for the foregoing purpose, you may not use, reproduce, copy, prepare
// * derivative works of, modify, distribute, perform, display or sell this Software and/or its documentation for any
// * purpose.
// *
// * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT WARRANTY
// * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
// * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL MBIENTLAB OR ITS LICENSORS BE LIABLE OR
// * OBLIGATED UNDER CONTRACT, NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
// * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT,
// * PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
// * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
// *
// * Should you have any questions regarding your right to use this Software, contact MbientLab via email:
// * hello@mbientlab.com.
// */
//package com.mbientlab.metawear;
//
//import static com.mbientlab.metawear.module.SensorFusionBosch.CalibrationAccuracy.LOW_ACCURACY;
//import static com.mbientlab.metawear.module.SensorFusionBosch.CalibrationAccuracy.MEDIUM_ACCURACY;
//import static com.mbientlab.metawear.module.SensorFusionBosch.CalibrationAccuracy.UNRELIABLE;
//import static org.junit.jupiter.api.Assertions.assertArrayEquals;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//import com.mbientlab.metawear.module.AccelerometerBmi160;
//import com.mbientlab.metawear.module.Gyro;
//import com.mbientlab.metawear.module.MagnetometerBmm150;
//import com.mbientlab.metawear.module.SensorFusionBosch;
//import com.mbientlab.metawear.module.SensorFusionBosch.CalibrationData;
//import com.mbientlab.metawear.module.SensorFusionBosch.CalibrationState;
//import com.mbientlab.metawear.module.SensorFusionBosch.Mode;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.provider.Arguments;
//
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.stream.Stream;
//
//import bolts.CancellationTokenSource;
//import bolts.Task;
//
//public class TestSensorFusion {
//    static class TestBase extends UnitTestBase {
//        protected SensorFusionBosch sensorFusion;
//
//        @BeforeEach
//        public void setup() throws Exception {
//            junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, Gyro.class, MagnetometerBmm150.class, SensorFusionBosch.class);
//            junitPlatform.firmware = "1.2.5";
//            connectToBoard();
//
//            sensorFusion = mwBoard.getModule(SensorFusionBosch.class);
//        }
//    }
//
//    public static class TestRev3 extends TestBase {
//        @BeforeEach
//        @Override
//        public void setup() throws Exception {
//            junitPlatform.addCustomModuleInfo(new byte[] {0x19, (byte) 0x80, 0x00, 0x03, 0x03, 0x00, 0x06, 0x00, 0x02, 0x00, 0x01, 0x00});
//            super.setup();
//        }
//
//        @Test
//        public void resetOrientation() {
//            byte[] expected= new byte[] {0x19, 0x0f, 0x01};
//
//            sensorFusion.resetOrientation();
//            assertArrayEquals(expected, junitPlatform.getLastCommand());
//        }
//    }
//
//    public static class TestRev1 extends TestBase {
//        @BeforeEach
//        @Override
//        public void setup() throws Exception {
//            junitPlatform.addCustomModuleInfo(new byte[] {0x19, (byte) 0x80, 0x00, 0x01, 0x03, 0x00, 0x06, 0x00, 0x02, 0x00, 0x01, 0x00});
//            super.setup();
//        }
//
//        @Test
//        public void readCalibrationState() throws Exception {
//            byte[][] expectedCmds= new byte[][] {
//                    {0x19, (byte) 0x8b},
//            };
//            CalibrationState expectedState = new CalibrationState(UNRELIABLE, LOW_ACCURACY, MEDIUM_ACCURACY);
//
//            junitPlatform.addCustomResponse(new byte[] { 0x19, (byte) 0x8b }, new byte[] { 0x19, (byte) 0x8b, 0x00, 0x01, 0x02 });
//
//            Task<CalibrationState> readTask = sensorFusion.readCalibrationStateAsync();
//            readTask.waitForCompletion();
//            if (readTask.isFaulted()) {
//                throw readTask.getError();
//            }
//
//            assertArrayEquals(expectedCmds, junitPlatform.getCommands());
//            assertEquals(expectedState, readTask.getResult());
//        }
//    }
//
//    public static class TestRev2 extends TestBase {
//        private static final byte[] ACC = new byte[] {
//                0x19, (byte) 0x0c, (byte) 0xf6, (byte) 0xff, 0x00, 0x00, 0x0a, 0x00, (byte) 0xe8, 0x03, 0x03, 0x00
//        }, GYRO = new byte[] {
//                0x19, (byte) 0x0d, 0x19, 0x0d, 0x04, 0x00, 0x08, 0x00, 0x01, 0x00, 0x00, 0x00, 0x03, 0x00
//        }, MAG = new byte[] {
//                0x19, (byte) 0x0e, 0x19, 0x0e, 0x66, 0x00, 0x17, (byte) 0xfd, (byte) 0x8a, (byte) 0xfc, 0x7f, 0x03, 0x01, 0x00
//        };
//
//
//        @Parameters(name = "{0}")
//        private static Stream<Arguments> data() {
//            List<Arguments> params = new LinkedList<>();
//            params.add(Arguments.of(preset, (byte) 1));
//
//            byte[] ACC_READ = Arrays.copyOfRange(ACC, 0, 2);
//            byte[] GYRO_READ = Arrays.copyOfRange(GYRO, 0, 2);
//            byte[] MAG_READ = Arrays.copyOfRange(MAG, 0, 2);
//
//            ACC_READ[1] |= 0x80;
//            GYRO_READ[1] |= 0x80;
//            MAG_READ[1] |= 0x80;
//
//            params.add(Arguments.of(Mode.NDOF, new byte[][] {
//                            { 0x19, (byte) 0x8b},
//                            ACC_READ,
//                            GYRO_READ,
//                            MAG_READ
//                    }, 5, new byte[][] {
//                            ACC,
//                            GYRO,
//                            MAG
//                    }, new SensorFusionBosch.CalibrationData(
//                            Arrays.copyOfRange(ACC, 2, ACC.length),
//                            Arrays.copyOfRange(GYRO, 2, GYRO.length),
//                            Arrays.copyOfRange(MAG, 2, MAG.length)
//                    )},
//                    { Mode.IMU_PLUS, new byte[][] {
//                            { 0x19, (byte) 0x8b},
//                            ACC_READ,
//                            GYRO_READ
//                    }, 3, new byte[][] {
//                            ACC,
//                            GYRO
//                    }, new SensorFusionBosch.CalibrationData(
//                            Arrays.copyOfRange(ACC, 2, ACC.length),
//                            Arrays.copyOfRange(GYRO, 2, GYRO.length),
//                            null
//                    )},
//                    { Mode.COMPASS, new byte[][] {
//                            { 0x19, (byte) 0x8b},
//                            ACC_READ,
//                            MAG_READ,
//                    }, 4, new byte[][] {
//                            ACC,
//                            MAG
//                    }, new SensorFusionBosch.CalibrationData(
//                            Arrays.copyOfRange(ACC, 2, ACC.length),
//                            null,
//                            Arrays.copyOfRange(MAG, 2, MAG.length)
//                    )},
//                    { Mode.M4G, new byte[][] {
//                            { 0x19, (byte) 0x8b},
//                            ACC_READ,
//                            MAG_READ,
//                    }, 4, new byte[][] {
//                            ACC,
//                            MAG
//                    }, new SensorFusionBosch.CalibrationData(
//                            Arrays.copyOfRange(ACC, 2, ACC.length),
//                            null,
//                            Arrays.copyOfRange(MAG, 2, MAG.length)
//                    )}
//            });
//            return params.stream();
//        }
//        public static Collection<Object[]> data() {
//        }
//
//        @Parameter
//        public Mode mode;
//
//        @Parameter(value = 1)
//        public byte[][] expectedCalibrateCmds;
//
//        @Parameter(value = 2)
//        public int calibCmdStart;
//
//        @Parameter(value = 3)
//        public byte[][] expectedWriteCalibrateCmds;
//
//        @Parameter(value = 4)
//        public CalibrationData expectedData;
//
//        @Before
//        public void setup() throws Exception {
//            byte[][] copy = new byte[][] {
//                    Arrays.copyOf(ACC, ACC.length),
//                    Arrays.copyOf(GYRO, GYRO.length),
//                    Arrays.copyOf(MAG, MAG.length)
//            };
//
//            copy[0][1] |= 0x80;
//            copy[1][1] |= 0x80;
//            copy[2][1] |= 0x80;
//
//            junitPlatform.addCustomResponse(Arrays.copyOfRange(copy[0], 0, 2), copy[0]);
//            junitPlatform.addCustomResponse(Arrays.copyOfRange(copy[1], 0, 2), copy[1]);
//            junitPlatform.addCustomResponse(Arrays.copyOfRange(copy[2], 0, 2), copy[2]);
//
//            junitPlatform.addCustomModuleInfo(new byte[] {0x19, (byte) 0x80, 0x00, 0x02, 0x03, 0x00, 0x06, 0x00, 0x02, 0x00, 0x01, 0x00});
//            super.setup();
//
//            sensorFusion.configure()
//                    .mode(mode)
//                    .commit();
//        }
//
//        @Test
//        public void calibrate() throws Exception {
//            junitPlatform.addCustomResponse(new byte[] { 0x19, (byte) 0x8b}, new byte[] { 0x19, (byte) 0x8b, 0x03, 0x03, 0x03 });
//
//            CancellationTokenSource cts = new CancellationTokenSource();
//            Task<SensorFusionBosch.CalibrationData> calibrateTask = sensorFusion.calibrate(cts.getToken());
//
//            calibrateTask.waitForCompletion();
//            if (calibrateTask.isFaulted()) {
//                throw calibrateTask.getError();
//            }
//
//            assertArrayEquals(expectedCalibrateCmds, junitPlatform.getCommands(calibCmdStart));
//            assertEquals(expectedData, calibrateTask.getResult());
//        }
//
//        @Test
//        public void writeCalibrationData() {
//            CalibrationData data = new SensorFusionBosch.CalibrationData(
//                    Arrays.copyOfRange(ACC, 2, ACC.length),
//                    mode == Mode.NDOF || mode == Mode.IMU_PLUS ? Arrays.copyOfRange(GYRO, 2, GYRO.length) : new byte[] { },
//                    mode != Mode.IMU_PLUS ? Arrays.copyOfRange(MAG, 2, MAG.length) : new byte[] { }
//            );
//
//            sensorFusion.writeCalibrationData(data);
//            assertArrayEquals(expectedWriteCalibrateCmds, junitPlatform.getCommands(calibCmdStart));
//        }
//    }
//}
