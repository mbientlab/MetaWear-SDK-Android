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

import static com.mbientlab.metawear.module.SensorFusionBosch.Mode.COMPASS;
import static com.mbientlab.metawear.module.SensorFusionBosch.Mode.IMU_PLUS;
import static com.mbientlab.metawear.module.SensorFusionBosch.Mode.M4G;
import static com.mbientlab.metawear.module.SensorFusionBosch.Mode.NDOF;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.SensorFusionBosch.AccRange;
import com.mbientlab.metawear.module.SensorFusionBosch.GyroRange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by etsai on 11/12/16.
 */
public class TestSensorFusionConfig extends UnitTestBase {
    private static final byte[] BMI160_ACC_RANGE_BITMASK= new byte[] { 0b0011, 0b0101, 0b1000, 0b1100 };
    private static final byte[][] CONFIG_MASKS;
    static {
        CONFIG_MASKS = new byte[][] {
                {0x10, 0x20, 0x30, 0x40},
                {0x11, 0x21, 0x31, 0x41},
                {0x12, 0x22, 0x32, 0x42},
                {0x13, 0x23, 0x33, 0x43}
        };
    }

    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        for(AccRange ar: AccRange.values()) {
            for(GyroRange gr: GyroRange.values()) {
                parameters.add(Arguments.of(ar, gr));
            }
        }
        return parameters.stream();
    }

    private SensorFusionBosch sensorFusion;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, Gyro.class, MagnetometerBmm150.class, SensorFusionBosch.class);
        connectToBoard();

        sensorFusion = mwBoard.getModule(SensorFusionBosch.class);
    }

    private byte[] gyroConfig(GyroRange gyroRange) {
        return new byte[] {0x13, 0x03, (byte) (0x20 | TestGyro160Config.ODR_BITMASK[Gyro.OutputDataRate.ODR_100_HZ.ordinal()]),
                TestGyro160Config.RANGE_BITMASK[Gyro.Range.values()[gyroRange.ordinal()].ordinal()]};
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureNdof(AccRange accRange, GyroRange gyroRange) {
        final byte[][] expected = new byte[][] {
                {0x19, 0x02, (byte) NDOF.ordinal(), CONFIG_MASKS[accRange.ordinal()][gyroRange.ordinal()]},
                {0x03, 0x03, 0x28, BMI160_ACC_RANGE_BITMASK[accRange.ordinal()]},
                gyroConfig(gyroRange),
                {0x15, 0x04, 0x04, 0x0e},
                {0x15, 0x03, 0x6}
        };

        sensorFusion.configure()
                .mode(NDOF)
                .accRange(accRange)
                .gyroRange(gyroRange)
                .commit();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureImuPlus(AccRange accRange, GyroRange gyroRange) {
        final byte[][] expected = new byte[][] {
                {0x19, 0x02, (byte) IMU_PLUS.ordinal(), CONFIG_MASKS[accRange.ordinal()][gyroRange.ordinal()]},
                {0x03, 0x03, 0x28, BMI160_ACC_RANGE_BITMASK[accRange.ordinal()]},
                gyroConfig(gyroRange)
        };

        sensorFusion.configure()
                .mode(IMU_PLUS)
                .accRange(accRange)
                .gyroRange(gyroRange)
                .commit();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureCompass(AccRange accRange, GyroRange gyroRange) {
        final byte[][] expected = new byte[][] {
                {0x19, 0x02, (byte) COMPASS.ordinal(), CONFIG_MASKS[accRange.ordinal()][gyroRange.ordinal()]},
                {0x03, 0x03, 0x26, BMI160_ACC_RANGE_BITMASK[accRange.ordinal()]},
                {0x15, 0x04, 0x04, 0x0e},
                {0x15, 0x03, 0x6}
        };

        sensorFusion.configure()
                .mode(COMPASS)
                .accRange(accRange)
                .gyroRange(gyroRange)
                .commit();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureM4g(AccRange accRange, GyroRange gyroRange) {
        final byte[][] expected = new byte[][] {
                {0x19, 0x02, (byte) M4G.ordinal(), CONFIG_MASKS[accRange.ordinal()][gyroRange.ordinal()]},
                {0x03, 0x03, 0x27, BMI160_ACC_RANGE_BITMASK[accRange.ordinal()]},
                {0x15, 0x04, 0x04, 0x0e},
                {0x15, 0x03, 0x6}
        };

        sensorFusion.configure()
                .mode(M4G)
                .accRange(accRange)
                .gyroRange(gyroRange)
                .commit();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }
}
