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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.mbientlab.metawear.module.SerialPassthrough;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import bolts.Capture;
import bolts.Task;

/**
 * Created by etsai on 10/6/16.
 */

public class TestI2C extends UnitTestBase {
    private SerialPassthrough.I2C i2c;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(SerialPassthrough.class);
        connectToBoard();

        i2c = mwBoard.getModule(SerialPassthrough.class).i2c((byte) 1, (byte) 0xa);
    }

    @Test
    public void readWhoAmI() {
        byte[] expected= new byte[] {0x0d, (byte) 0xc1, 0x1c, 0x0d, 0x0a, 0x01};

        i2c.read((byte) 0x1c, (byte) 0xd);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    protected Task<Route> setupI2cRoute() {
        return i2c.addRouteAsync(source ->
                source.stream((data, env) -> ((Capture<byte[]>) env[0]).set(data.value(byte[].class)))
        );
    }

    @Test
    public void whoAmIData() throws Exception {
        byte[] expected= new byte[] {0x2a};
        final Capture<byte[]> actual= new Capture<>();

        Task<Route> routeTask = setupI2cRoute().onSuccess(task -> {
            task.getResult().setEnvironment(0, actual);
            return null;
        });
        routeTask.waitForCompletion();
        if (routeTask.isFaulted()) {
            throw routeTask.getError();
        }

        sendMockResponse(new byte[] {0x0d, (byte) 0x81, 0x0a, 0x2a});

        // For TestDeserializedI2C
        junitPlatform.boardStateSuffix = "i2c_stream";
        mwBoard.serialize();

        assertArrayEquals(expected, actual.get());
    }

    @Test
    public void directReadWhoAmI() {
        byte[] expected= new byte[] {0x0d, (byte) 0x81, 0x1c, 0x0d, (byte) 0xff, 0x01};

        mwBoard.getModule(SerialPassthrough.class).readI2cAsync((byte) 0x1c, (byte) 0x0d, (byte) 1);
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void directReadWhoAmIData() {
        byte[] expected= new byte[] {0x2a};
        final Capture<byte[]> actual= new Capture<>();

        mwBoard.getModule(SerialPassthrough.class).readI2cAsync((byte) 0x1c, (byte) 0x0d, (byte) 1)
                .continueWith(task -> {
                    actual.set(task.getResult());
                    return null;
                });

        sendMockResponse(new byte[] {0x0d, (byte) 0x81, (byte) 0xff, 0x2a});
        assertArrayEquals(expected, actual.get());
    }

    @Test
    public void directReadWhoAmITimeout() throws Exception {
        final Capture<Exception> actual= new Capture<>();

        mwBoard.getModule(SerialPassthrough.class).readI2cAsync((byte) 0x1c, (byte) 0x0d, (byte) 1)
                .continueWith(task -> {
                    actual.set(task.getError());
                    return null;
                }).waitForCompletion();

        assertInstanceOf(TimeoutException.class, actual.get());
    }
}
