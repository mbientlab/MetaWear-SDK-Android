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

import com.mbientlab.metawear.module.BarometerBme280;
import com.mbientlab.metawear.module.BarometerBmp280;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;

import bolts.Capture;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by etsai on 10/2/16.
 */
@RunWith(Parameterized.class)
public class TestBarometerBoschData extends TestBarometerBoschBase {
    private static final byte PRESSURE = 0x1, ALTITUDE = 0x2;

    @Parameters(name = "board: {0}, type: {1}")
    public static Collection<Object[]> data() {
        ArrayList<Object[]> parameters= new ArrayList<>();
        parameters.add(new Object[] {BarometerBme280.class, PRESSURE});
        parameters.add(new Object[] {BarometerBme280.class, ALTITUDE});
        parameters.add(new Object[] {BarometerBmp280.class, PRESSURE});
        parameters.add(new Object[] {BarometerBmp280.class, ALTITUDE});
        return parameters;
    }

    @Parameter(value = 1)
    public byte dataType;

    private Route dataRoute;
    private final Capture<Float> actualData= new Capture<>();

    @Before
    public void setup() throws Exception {
        super.setup();

        AsyncDataProducer producer = dataType == PRESSURE ? baroBosch.pressure() : baroBosch.altitude();
        producer.addRouteAsync(source -> source.stream((data, env) -> ((Capture<Float>) env[0]).set(data.value(Float.class))))
                .continueWith(task -> {
                    dataRoute = task.getResult();
                    dataRoute.setEnvironment(0, actualData);
                    return null;
                });
    }

    @Test
    public void subscribe() {
        byte[] expected= new byte[] {0x12, dataType, 0x1};

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void unsubscribe() {
        byte[] expected= new byte[] {0x12, dataType, 0x0};

        dataRoute.unsubscribe(0);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void interpretData() {
        float expected= dataType == PRESSURE ? 101173.828125f : -480.8828125f;
        byte[] response= dataType == PRESSURE ?
                new byte[] {0x12, 0x01, (byte) 0xd3, 0x35, (byte) 0x8b, 0x01} :
                new byte[] {0x12, 0x02, 0x1e, 0x1f, (byte) 0xfe, (byte) 0xff};

        sendMockResponse(response);
        assertEquals(expected, actualData.get(), 0.00000001);
    }
}
