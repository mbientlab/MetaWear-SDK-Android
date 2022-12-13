///*
// * Copyright 2014-2015 MbientLab Inc. All rights reserved.
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
//
//package com.mbientlab.metawear;
//
//import static com.mbientlab.metawear.module.SensorFusionBosch.Mode.COMPASS;
//import static com.mbientlab.metawear.module.SensorFusionBosch.Mode.IMU_PLUS;
//import static com.mbientlab.metawear.module.SensorFusionBosch.Mode.M4G;
//import static com.mbientlab.metawear.module.SensorFusionBosch.Mode.NDOF;
//
//import com.mbientlab.metawear.module.AccelerometerBmi160;
//import com.mbientlab.metawear.module.Gyro;
//import com.mbientlab.metawear.module.MagnetometerBmm150;
//import com.mbientlab.metawear.module.SensorFusionBosch;
//
//import org.junit.jupiter.params.provider.Arguments;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.Arrays;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.stream.Stream;
//
///**
// * Created by etsai on 11/12/16.
// */
//public class TestSensorFusionControl extends UnitTestBase {
//    private static Stream<Arguments> data() {
//        String[] functionNames = new String[] {
//                "correctedAcceleration",
//                "correctedAngularVelocity",
//                "correctedMagneticField",
//                "quaternion",
//                "eulerAngles",
//                "gravity",
//                "linearAcceleration"
//        };
//
//        Object[][] testBases = new Object[][]{
//                {
//                        NDOF,
//                        new byte[][] {
//                                {0x19, 0x02, 0x01, 0x13},
//                                {0x03, 0x03, 0x28, 0x0c},
//                                {0x13, 0x03, 0x28, 0x00},
//                                {0x15, 0x04, 0x04, 0x0e},
//                                {0x15, 0x03, 0x6},
//                                {0x03, 0x02, 0x01, 0x00},
//                                {0x13, 0x02, 0x01, 0x00},
//                                {0x15, 0x02, 0x01, 0x00},
//                                {0x03, 0x01, 0x01},
//                                {0x13, 0x01, 0x01},
//                                {0x15, 0x01, 0x01},
//                                {0x19, 0x03, 0x00, 0x00},
//                                {0x19, 0x01, 0x01},
//                                {0x19, 0x01, 0x00},
//                                {0x19, 0x03, 0x00, 0x7f},
//                                {0x03, 0x01, 0x00},
//                                {0x13, 0x01, 0x00},
//                                {0x15, 0x01, 0x00},
//                                {0x03, 0x02, 0x00, 0x01},
//                                {0x13, 0x02, 0x00, 0x01},
//                                {0x15, 0x02, 0x00, 0x01}
//                        },
//                        11
//                },
//                {
//                        IMU_PLUS,
//                        new byte[][] {
//                                {0x19, 0x02, 0x02, 0x13},
//                                {0x03, 0x03, 0x28, 0x0c},
//                                {0x13, 0x03, 0x28, 0x00},
//                                {0x03, 0x02, 0x01, 0x00},
//                                {0x13, 0x02, 0x01, 0x00},
//                                {0x03, 0x01, 0x01},
//                                {0x13, 0x01, 0x01},
//                                {0x19, 0x03, 0x00, 0x00},
//                                {0x19, 0x01, 0x01},
//                                {0x19, 0x01, 0x00},
//                                {0x19, 0x03, 0x00, 0x7f},
//                                {0x03, 0x01, 0x00},
//                                {0x13, 0x01, 0x00},
//                                {0x03, 0x02, 0x00, 0x01},
//                                {0x13, 0x02, 0x00, 0x01}
//                        },
//                        7
//                },
//                {
//                        COMPASS,
//                        new byte[][] {
//                                {0x19, 0x02, 0x03, 0x13},
//                                {0x03, 0x03, 0x26, 0x0c},
//                                {0x15, 0x04, 0x04, 0x0e},
//                                {0x15, 0x03, 0x6},
//                                {0x03, 0x02, 0x01, 0x00},
//                                {0x15, 0x02, 0x01, 0x00},
//                                {0x03, 0x01, 0x01},
//                                {0x15, 0x01, 0x01},
//                                {0x19, 0x03, 0x00, 0x00},
//                                {0x19, 0x01, 0x01},
//                                {0x19, 0x01, 0x00},
//                                {0x19, 0x03, 0x00, 0x7f},
//                                {0x03, 0x01, 0x00},
//                                {0x15, 0x01, 0x00},
//                                {0x03, 0x02, 0x00, 0x01},
//                                {0x15, 0x02, 0x00, 0x01}
//                        },
//                        8
//                },
//                {
//                        M4G,
//                        new byte[][] {
//                                {0x19, 0x02, 0x04, 0x13},
//                                {0x03, 0x03, 0x27, 0x0c},
//                                {0x15, 0x04, 0x04, 0x0e},
//                                {0x15, 0x03, 0x6},
//                                {0x03, 0x02, 0x01, 0x00},
//                                {0x15, 0x02, 0x01, 0x00},
//                                {0x03, 0x01, 0x01},
//                                {0x15, 0x01, 0x01},
//                                {0x19, 0x03, 0x00, 0x00},
//                                {0x19, 0x01, 0x01},
//                                {0x19, 0x01, 0x00},
//                                {0x19, 0x03, 0x00, 0x7f},
//                                {0x03, 0x01, 0x00},
//                                {0x15, 0x01, 0x00},
//                                {0x03, 0x02, 0x00, 0x01},
//                                {0x15, 0x02, 0x00, 0x01}
//                        },
//                        8
//                }
//        };
//
//
//        List<Arguments> params = new LinkedList<>();
//        for(int i= 0; i < 7; i++) {
//            for(Object[] base: testBases) {
//                Object[] config = new Object[base.length];
//                config[0] = base[0];
//                config[2] = functionNames[i];
//
//                byte[][] copy = new byte[((byte[][]) base[1]).length][];
//                for(int j = 0; j < copy.length; j++) {
//                    copy[j] = Arrays.copyOf(((byte[][]) base[1])[j], ((byte[][]) base[1])[j].length);
//                }
//                config[1] = copy;
//
//                ((byte[][]) config[1])[(int) base[2]][2] |= (0x1 << i);
//                params.add(Arguments.of(config));
//            }
//        }
//        return params.stream();
//    }
//
//    @Parameter
//    public SensorFusionBosch.Mode opMode;
//
//    @Parameter(value = 1)
//    public byte[][] expected;
//
//    @Parameter(value = 2)
//    public String fnName;
//
//    private SensorFusionBosch sensorFusion;
//
//    @Before
//    public void setup() throws Exception {
//        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, Gyro.class, MagnetometerBmm150.class, SensorFusionBosch.class);
//        connectToBoard();
//
//        sensorFusion = mwBoard.getModule(SensorFusionBosch.class);
//    }
//
//    @Test
//    public void startAndStop() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        Method m = SensorFusionBosch.class.getMethod(fnName);
//        AsyncDataProducer producer = (AsyncDataProducer) m.invoke(sensorFusion);
//
//        sensorFusion.configure()
//                .mode(opMode)
//                .commit();
//        producer.start();
//        sensorFusion.start();
//        sensorFusion.stop();
//        producer.stop();
//
//        assertArrayEquals(expected, junitPlatform.getCommands());
//    }
//}
