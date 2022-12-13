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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by eric on 12/7/17.
 */

public class TestSensorFusionConfigFiltering extends UnitTestBase {
    private static final byte[] BMI160_FILTER_MASK = new byte[] { 0b00000000, 0b00010000, 0b00100000 };

    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        for(AccelerometerBmi160.FilterMode accF: AccelerometerBmi160.FilterMode.values()) {
            for(Gyro.FilterMode gyrF: Gyro.FilterMode.values()) {
                parameters.add(Arguments.of(accF, gyrF));
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

    @ParameterizedTest
    @MethodSource("data")
    public void configureNdof(AccelerometerBmi160.FilterMode accFilter, Gyro.FilterMode gyroFilter) {
        final byte[][] expected = new byte[][] {
                {0x19, 0x02, (byte) NDOF.ordinal(), 0x13},
                {0x03, 0x03, (byte) (0x8 | BMI160_FILTER_MASK[accFilter.ordinal()]), 0xc},
                {0x13, 0x03, (byte) (0x8 | BMI160_FILTER_MASK[gyroFilter.ordinal()]), 0x0},
                {0x15, 0x04, 0x04, 0x0e},
                {0x15, 0x03, 0x6}
        };

        sensorFusion.configure()
                .mode(NDOF)
                .accRange(SensorFusionBosch.AccRange.AR_16G)
                .accExtra(accFilter)
                .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                .gyroExtra(gyroFilter)
                .commit();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureImuPlus(AccelerometerBmi160.FilterMode accFilter, Gyro.FilterMode gyroFilter) {
        final byte[][] expected = new byte[][] {
                {0x19, 0x02, (byte) IMU_PLUS.ordinal(), 0x13},
                {0x03, 0x03, (byte) (0x8 | BMI160_FILTER_MASK[accFilter.ordinal()]), 0xc},
                {0x13, 0x03, (byte) (0x8 | BMI160_FILTER_MASK[gyroFilter.ordinal()]), 0x0},
        };

        sensorFusion.configure()
                .mode(IMU_PLUS)
                .accRange(SensorFusionBosch.AccRange.AR_16G)
                .accExtra(accFilter)
                .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                .gyroExtra(gyroFilter)
                .commit();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureCompass(AccelerometerBmi160.FilterMode accFilter, Gyro.FilterMode gyroFilter) {
        final byte[][] expected = new byte[][] {
                {0x19, 0x02, (byte) COMPASS.ordinal(), 0x13},
                {0x03, 0x03, (byte) (0x6 | BMI160_FILTER_MASK[accFilter.ordinal()]), 0xc},
                {0x15, 0x04, 0x04, 0x0e},
                {0x15, 0x03, 0x6}
        };

        sensorFusion.configure()
                .mode(COMPASS)
                .accRange(SensorFusionBosch.AccRange.AR_16G)
                .accExtra(accFilter)
                .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                .gyroExtra(gyroFilter)
                .commit();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureM4g(AccelerometerBmi160.FilterMode accFilter, Gyro.FilterMode gyroFilter) {
        final byte[][] expected = new byte[][] {
                {0x19, 0x02, (byte) M4G.ordinal(), 0x13},
                {0x03, 0x03, (byte) (0x7 | BMI160_FILTER_MASK[accFilter.ordinal()]), 0xc},
                {0x15, 0x04, 0x04, 0x0e},
                {0x15, 0x03, 0x6}
        };

        sensorFusion.configure()
                .mode(M4G)
                .accRange(SensorFusionBosch.AccRange.AR_16G)
                .accExtra(accFilter)
                .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                .gyroExtra(gyroFilter)
                .commit();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }
}
