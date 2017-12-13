package com.mbientlab.metawear;

import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;

import static com.mbientlab.metawear.module.SensorFusionBosch.Mode.*;
import static org.junit.Assert.assertArrayEquals;

/**
 * Created by eric on 12/7/17.
 */

@RunWith(Parameterized.class)
public class TestSensorFusionConfigFiltering extends UnitTestBase {
    private static final byte[] BMI160_FILTER_MASK = new byte[] { 0b00000000, 0b00010000, 0b00100000 };

    @Parameters(name = "{0}, {1}")
    public static Collection<Object[]> data() {
        ArrayList<Object[]> tests = new ArrayList<>();

        for(AccelerometerBmi160.FilterMode accF: AccelerometerBmi160.FilterMode.values()) {
            for(GyroBmi160.FilterMode gyrF: GyroBmi160.FilterMode.values()) {
                tests.add(new Object[]{accF, gyrF});
            }
        }

        return tests;
    }

    @Parameter
    public AccelerometerBmi160.FilterMode accFilter;

    @Parameter(value = 1)
    public GyroBmi160.FilterMode gyroFilter;

    private SensorFusionBosch sensorFusion;

    @Before
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, GyroBmi160.class, MagnetometerBmm150.class, SensorFusionBosch.class);
        connectToBoard();

        sensorFusion = mwBoard.getModule(SensorFusionBosch.class);
    }

    @Test
    public void configureNdof() {
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

    @Test
    public void configureImuPlus() {
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

    @Test
    public void configureCompass() {
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

    @Test
    public void configureM4g() {
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
