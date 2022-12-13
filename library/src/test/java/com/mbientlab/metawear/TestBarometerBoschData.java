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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.mbientlab.metawear.module.BarometerBme280;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import bolts.Capture;

/**
 * Created by etsai on 10/2/16.
 */
public class TestBarometerBoschData extends TestBarometerBoschBase {
    private static final byte PRESSURE = 0x1, ALTITUDE = 0x2;

    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        parameters.add(Arguments.of(BarometerBme280.class, PRESSURE));
        parameters.add(Arguments.of(BarometerBme280.class, ALTITUDE));
        parameters.add(Arguments.of(BarometerBme280.class, PRESSURE));
        parameters.add(Arguments.of(BarometerBme280.class, ALTITUDE));
        return parameters.stream();
    }

    private Route dataRoute;
    private final Capture<Float> actualData= new Capture<>();

    public void setup(Class<? extends MetaWearBoard.Module> moduleClass, byte dataType) {
        try {
            super.setup(moduleClass);

            AsyncDataProducer producer = dataType == PRESSURE ? baroBosch.pressure() : baroBosch.altitude();
            producer.addRouteAsync(source -> source.stream((data, env) -> ((Capture<Float>) env[0]).set(data.value(Float.class))))
                    .continueWith(task -> {
                        dataRoute = task.getResult();
                        dataRoute.setEnvironment(0, actualData);
                        return null;
                    });
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void subscribe(Class<? extends MetaWearBoard.Module> moduleClass, byte dataType) {
        setup(moduleClass, dataType);
        byte[] expected= new byte[] {0x12, dataType, 0x1};

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void unsubscribe(Class<? extends MetaWearBoard.Module> moduleClass, byte dataType) {
        setup(moduleClass, dataType);
        byte[] expected= new byte[] {0x12, dataType, 0x0};

        dataRoute.unsubscribe(0);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void interpretData(Class<? extends MetaWearBoard.Module> moduleClass, byte dataType) {
        setup(moduleClass, dataType);
        float expected= dataType == PRESSURE ? 101173.828125f : -480.8828125f;
        byte[] response= dataType == PRESSURE ?
                new byte[] {0x12, 0x01, (byte) 0xd3, 0x35, (byte) 0x8b, 0x01} :
                new byte[] {0x12, 0x02, 0x1e, 0x1f, (byte) 0xfe, (byte) 0xff};

        sendMockResponse(response);
        assertEquals(expected, actualData.get(), 0.00000001);
    }
}
