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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.data.Quaternion;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.SensorFusionBosch.CorrectedAcceleration;
import com.mbientlab.metawear.module.SensorFusionBosch.CorrectedAngularVelocity;
import com.mbientlab.metawear.module.SensorFusionBosch.CorrectedMagneticField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import bolts.Capture;

/**
 * Created by etsai on 11/12/16.
 */
public class TestSensorFusionData extends UnitTestBase {
    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        parameters.add(Arguments.of(
                        "correctedAcceleration",
                        new byte[] {0x19, 0x04, 0x20, 0x3e, 0x53, (byte) 0xc5, 0x0c, (byte) 0xfe, 0x79, 0x46, 0x0c, (byte) 0xfe, 0x79, (byte) 0xc6, 0x00},
                        new CorrectedAcceleration(Float.intBitsToFloat(0xc0585000), Float.intBitsToFloat(0x417ffe00), Float.intBitsToFloat(0xc17ffe00), (byte) 0x00)
        ));
        parameters.add(Arguments.of(
                        "correctedAngularVelocity",
                        new byte[] {0x19, 0x05, 0x7a, 0x56, (byte) 0x91, 0x42, (byte) 0xb4, 0x62, 0x60, (byte) 0xc2, 0x73, 0x34, 0x04, 0x44, 0x00},
                        new CorrectedAngularVelocity(Float.intBitsToFloat(0x4291567a), Float.intBitsToFloat(0xc26062b4), Float.intBitsToFloat(0x44043473), (byte) 0x00)
        ));
        parameters.add(Arguments.of(
                        "correctedMagneticField",
                        new byte[] {0x19, 0x06, 0x00, 0x00, 0x02, 0x42, (byte) 0xcd, (byte) 0xcc, 0x6c, (byte) 0xc1, (byte) 0x9a, (byte) 0x99, (byte) 0xed, 0x41, 0x03},
                        new CorrectedMagneticField(Float.intBitsToFloat(0x3808509c), Float.intBitsToFloat(0xb7784d84), Float.intBitsToFloat(0x37f92444), (byte) 0x03)
        ));
        parameters.add(Arguments.of(
                        "quaternion",
                        new byte[] {0x19, 0x07, 0x1b, (byte) 0x9b, 0x70, 0x3f, (byte) 0x8c, 0x5e, 0x4d, (byte) 0xbd, 0x07, 0x7f, 0x1d, (byte) 0xbe, 0x78, 0x02, (byte) 0x9a, (byte) 0xbe},
                        new Quaternion(Float.intBitsToFloat(0x3f709b1b), Float.intBitsToFloat(0xbd4d5e8c), Float.intBitsToFloat(0xbe1d7f07), Float.intBitsToFloat(0xbe9a0278))
        ));
        parameters.add(Arguments.of(
                        "eulerAngles",
                        new byte[] {0x19, 0x08, (byte) 0xb1, (byte) 0xf9, (byte) 0xc5, 0x41, 0x44, (byte) 0xb9, (byte) 0xf1, (byte) 0xc2, 0x1a, 0x2f, 0x04, (byte) 0xc2, (byte) 0xb1, (byte) 0xf9, (byte) 0xc5, 0x41},
                        new EulerAngles(Float.intBitsToFloat(0x41c5f9b1), Float.intBitsToFloat(0xc2f1b944), Float.intBitsToFloat(0xc2042f1a), Float.intBitsToFloat(0x41c5f9b1))
        ));
        parameters.add(Arguments.of(
                        "gravity",
                        new byte[] {0x19, 0x09, (byte) 0xee, 0x20, (byte) 0xd3, 0x3e, (byte) 0xb2, (byte) 0x93, 0x01, 0x41, 0x04, 0x59, (byte) 0xb0, (byte) 0xc0},
                        new Acceleration(Float.intBitsToFloat(0x3d2c3ba8), Float.intBitsToFloat(0x3f536925), Float.intBitsToFloat(0xbf0fdc15))
        ));
        parameters.add(Arguments.of(
                        "linearAcceleration",
                        new byte[] {0x19, 0x0a, 0x2f, (byte) 0xca, 0x39, 0x40, (byte) 0x86, (byte) 0xd4, 0x61, 0x41, (byte) 0x80, 0x4c, 0x6e, (byte) 0xc0},
                        new Acceleration(Float.intBitsToFloat(0x3e978ff1), Float.intBitsToFloat(0x3fb839e5), Float.intBitsToFloat(0xbec265d2))
                ));
        return parameters.stream();
    }

    private SensorFusionBosch sensorFusion;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(SensorFusionBosch.class);
        connectToBoard();

        sensorFusion = mwBoard.getModule(SensorFusionBosch.class);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void receivedData(String methodName, byte[] response, Object expected) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = SensorFusionBosch.class.getMethod(methodName);
        AsyncDataProducer producer = (AsyncDataProducer) m.invoke(sensorFusion);
        final Capture<Object> actual = new Capture<>();

        producer.addRouteAsync(source -> source.stream((data, env) -> ((Capture<Object>) env[0]).set(data.value(data.types()[0])))).continueWith(task -> {
            task.getResult().setEnvironment(0, actual);
            return null;
        });

        sendMockResponse(response);
        assertEquals(expected, actual.get());
    }
}
