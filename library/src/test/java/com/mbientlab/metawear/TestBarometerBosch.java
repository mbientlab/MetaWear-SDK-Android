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

import static com.mbientlab.metawear.Executors.IMMEDIATE_EXECUTOR;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mbientlab.metawear.module.BarometerBme280;
import com.mbientlab.metawear.module.BarometerBmp280;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by etsai on 10/2/16.
 */
public class TestBarometerBosch extends TestBarometerBoschBase {
    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        parameters.add(Arguments.of(BarometerBme280.class));
        parameters.add(Arguments.of(BarometerBmp280.class));
        return parameters.stream();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void startNoAltitude(Class<? extends MetaWearBoard.Module> moduleClass) throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup(moduleClass).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            byte[] expected= new byte[] {0x12, 0x04, 0x01, 0x00};

            baroBosch.start();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }


    @ParameterizedTest
    @MethodSource("data")
    public void startWithAltitude(Class<? extends MetaWearBoard.Module> moduleClass) throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup(moduleClass).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            byte[] expected= new byte[] {0x12, 0x04, 0x01, 0x01};

            baroBosch.altitude().start();
            baroBosch.start();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }


    @ParameterizedTest
    @MethodSource("data")
    public void stop(Class<? extends MetaWearBoard.Module> moduleClass) throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup(moduleClass).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
            byte[] expected = new byte[] {0x12, 0x04, 0x00, 0x00};

            baroBosch.stop();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
