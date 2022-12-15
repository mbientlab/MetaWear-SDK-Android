package com.mbientlab.metawear;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBmi270;
import com.mbientlab.metawear.module.AccelerometerMma8452q;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by eric on 5/1/17.
 */
public class TestLogAccelerometer extends UnitTestBase {
    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        parameters.add(Arguments.of(AccelerometerBmi160.class));
        parameters.add(Arguments.of(AccelerometerBmi270.class));
        parameters.add(Arguments.of(AccelerometerMma8452q.class));
        parameters.add(Arguments.of(AccelerometerBma255.class));
        return parameters.stream();
    }

    private Accelerometer accelerometer;

    public void setup(Class<? extends Accelerometer> accelClass) {
        try {
            junitPlatform.boardInfo = new MetaWearBoardInfo(accelClass);
            connectToBoard();

            accelerometer = mwBoard.getModule(Accelerometer.class);
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void setupAndRemove(Class<? extends Accelerometer> accelClass) throws InterruptedException {
        setup(accelClass);
        byte[][] expected= new byte[][]{
                {0x0b, 0x02, 0x03, 0x04, (byte) 0xff, 0x60},
                {0x0b, 0x02, 0x03, 0x04, (byte) 0xff, 0x24},
                {0x0b, 0x03, 0x00},
                {0x0b, 0x03, 0x01}
        };

        accelerometer.acceleration().addRouteAsync(source -> source.log(null)).continueWith(task -> {
            task.getResult().remove();
            return null;
        }).waitForCompletion();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

}
