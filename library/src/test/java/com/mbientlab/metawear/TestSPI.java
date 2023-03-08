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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.android.gms.tasks.Task;
import com.mbientlab.metawear.module.SerialPassthrough;
import com.mbientlab.metawear.module.SerialPassthrough.SpiParameterBuilder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Created by etsai on 10/6/16.
 */

public class TestSPI extends UnitTestBase {
    private static <T> SpiParameterBuilder<T> setParameters(SpiParameterBuilder<T> builder) {
        return builder.data(new byte[] {(byte) 0xda})
                .mode((byte) 3)
                .frequency(SerialPassthrough.SpiFrequency.FREQ_8_MHZ)
                .slaveSelectPin((byte) 10)
                .clockPin((byte) 0)
                .mosiPin((byte) 11)
                .misoPin((byte) 7)
                .useNativePins();
    }

    private SerialPassthrough.SPI spi;

    public Task<Void> setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(SerialPassthrough.class);
        return connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored ->
                spi = mwBoard.getModule(SerialPassthrough.class).spi((byte) 5, (byte) 0xe));
    }

    @Test
    public void readBmi160() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[] expected= new byte[] {0x0d, (byte) 0xc2, 0x0a, 0x00, 0x0b, 0x07, 0x76, (byte) 0xe4, (byte) 0xda};

            setParameters(spi.read()).commit();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    protected Task<Route> setupSpiStream() {
        return spi.addRouteAsync(source -> source.stream((data, env) -> ((Capture<byte[]>) env[0]).set(data.value(byte[].class))));
    }

    @Test
    public void bmi160Data() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected= new byte[] {0x07, 0x30, (byte) 0x81, 0x0b, (byte) 0xc0};
        final Capture<byte[]> actual= new Capture<>();

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, setup ->
                setupSpiStream().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                task.setEnvironment(0, actual);
            }).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                  sendMockResponse(new byte[] {0x0d, (byte) 0x82, 0x0e, 0x07, 0x30, (byte) 0x81, 0x0b, (byte) 0xc0});

                  // For TestDeserializeSPI
                  junitPlatform.boardStateSuffix = "spi_stream";
                  try {
                      mwBoard.serialize();
                  } catch (IOException e) {
                      fail(e);
                  }

                  assertArrayEquals(expected, actual.get());
                  doneSignal.countDown();
        }));
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void directReadBmi160() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[] expected= new byte[] {0x0d, (byte) 0x82, 0x0a, 0x00, 0x0b, 0x07, 0x76, (byte) 0xf4, (byte) 0xda};

            setParameters(mwBoard.getModule(SerialPassthrough.class).readSpiAsync((byte) 5)).commit();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void directReadBmi160Data() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[] expected= new byte[] {0x07, 0x30, (byte) 0x81, 0x0b, (byte) 0xc0};
            final Capture<byte[]> actual= new Capture<>();

            setParameters(mwBoard.getModule(SerialPassthrough.class).readSpiAsync((byte) 5)).commit()
                    .addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                        actual.set(task);
                    }).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                        sendMockResponse(new byte[] {0x0d, (byte) 0x82, (byte) 0x0f, 0x07, 0x30, (byte) 0x81, 0x0b, (byte) 0xc0});
                        assertArrayEquals(expected, actual.get());
                        doneSignal.countDown();
                    });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void directReadBmi160Timeout() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            final Capture<Exception> actual = new Capture<>();

            setParameters(mwBoard.getModule(SerialPassthrough.class).readSpiAsync((byte) 5)).commit()
                    .addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                        actual.set(exception);
                    }).addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                        assertInstanceOf(TimeoutException.class, actual.get());
                        doneSignal.countDown();
                    });
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
