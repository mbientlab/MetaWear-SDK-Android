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

import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.builder.function.Function2;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Accelerometer.AccelerationDataProducer;
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

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 9/15/16.
 */
@RunWith(Parameterized.class)
public class TestAccFeedback extends UnitTestBase {
    @Parameters
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
        junitPlatform.boardInfo= new MetaWearBoardInfo(accelClass);
        junitPlatform.firmware= "1.1.3";
        connectToBoard();

        accelerometer = mwBoard.getModule(Accelerometer.class);
    }

    @Test
    public void createYXFeedback() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, 0x22, 0x0a, 0x01, 0x01},
                {0x09, 0x02, 0x09, 0x03, 0x00, 0x20, 0x09, 0x15, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x0b, 0x02, 0x09, 0x03, (byte) 0x01, 0x20},
                {0x0a, 0x02, 0x03, 0x04, (byte) 0xff, 0x09, 0x05, 0x09, 0x05, 0x04},
                {0x0a, 0x03, 0x01, 0x09, 0x15, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x03, 0x01},
                {0x09, 0x07, 0x01, 0x01},
                {0x09, 0x07, 0x01, 0x00},
                {0x0b, 0x03, 0x00},
                {0x09, 0x06, 0x00},
                {0x09, 0x06, 0x01},
                {0x0a, 0x04, 0x00}
        };
        final AccelerationDataProducer acceleration = accelerometer.acceleration();

        acceleration.addRouteAsync(source ->
                source.split()
                    .index(1).delay((byte) 1).map(Function2.SUBTRACT, acceleration.xAxisName()).stream(null).log(null)
                .end()
        ).waitForCompletion();

        mwBoard.lookupRoute(0).remove();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }

    @Test
    public void xyAbsAdd() throws InterruptedException {
        final byte[][] expected = new byte[][] {
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, 0x20, 0x09, 0x15, 0x0a, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x03, 0x04, (byte) 0xff, 0x22, 0x09, 0x15, 0x0a, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x09, 0x07, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00},
                {0x0a, 0x02, 0x09, 0x03, 0x00, 0x09, 0x05, 0x09, 0x05, 0x04},
                {0x0a, 0x03, 0x02, 0x09, 0x07, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00}
        };
        final AccelerationDataProducer acceleration = accelerometer.acceleration();

        acceleration.addRouteAsync(source ->
                source.split()
                    .index(0).map(Function1.ABS_VALUE).name("x-abs")
                    .index(1).map(Function1.ABS_VALUE).map(Function2.ADD, "x-abs")
        ).waitForCompletion();

        assertArrayEquals(expected, junitPlatform.getCommands());
    }
}
