package com.mbientlab.metawear;

import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerMma8452q;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by eric on 5/1/17.
 */
@RunWith(Parameterized.class)
public class TestLogAccelerometer extends UnitTestBase {
    @Parameters(name = "channel: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { AccelerometerBmi160.class },
                { AccelerometerMma8452q.class },
                { AccelerometerBma255.class },
        });
    }

    @Parameter
    public Class<? extends Accelerometer> accelClass;

    private Accelerometer accelerometer;

    @Before
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(accelClass);
        connectToBoard();

        accelerometer = mwBoard.getModule(Accelerometer.class);
    }

    @Test
    public void setupAndRemove() throws InterruptedException {
        byte[][] expected= new byte[][]{
                {0x0b, 0x02, 0x03, 0x04, (byte) 0xff, 0x60},
                {0x0b, 0x02, 0x03, 0x04, (byte) 0xff, 0x24},
                {0x0b, 0x03, 0x00},
                {0x0b, 0x03, 0x01}
        };

        accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.log(null);
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                task.getResult().remove();

                synchronized (TestLogAccelerometer.this) {
                    TestLogAccelerometer.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();
            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

}
