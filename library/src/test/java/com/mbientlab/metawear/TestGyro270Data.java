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

import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.Gyro.Range;
import com.mbientlab.metawear.module.GyroBmi270;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bolts.Capture;
/**
 * Created by lkasso on 04/01/21.
 */

public class TestGyro270Data extends UnitTestBase {
    private Gyro gyro;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(GyroBmi270.class);
        connectToBoard();

        gyro = mwBoard.getModule(GyroBmi270.class);
    }

    @Test
    public void start() {
        byte[] expected = new byte[] {0x13, 0x01, 0x01};

        gyro.start();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void stop() {
        byte[] expected = new byte[] {0x13, 0x01, 0x00};

        gyro.stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void enable() {
        byte[] expected = new byte[] {0x13, 0x02, 0x00, 0x01};

        gyro.angularVelocity().stop();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void disable() {
        byte[] expected = new byte[] {0x13, 0x02, 0x01, 0x00};

        gyro.angularVelocity().start();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void interpretData() {
        AngularVelocity expected= new AngularVelocity(Float.intBitsToFloat(0x4383344b), Float.intBitsToFloat(0x43f9bf9c), Float.intBitsToFloat(0xc3f9c190));
        final Capture<AngularVelocity> actual= new Capture<>();

        gyro.configure()
                .range(Range.FSR_500)
                .commit();
        gyro.angularVelocity().addRouteAsync(source ->
                source.stream((data, env) -> ((Capture<AngularVelocity>) env[0]).set(data.value(AngularVelocity.class))))
                .continueWith(task -> {
                    task.getResult().setEnvironment(0, actual);
                    return null;
                });
        sendMockResponse(new byte[] {0x13, 0x04, 0x3e, 0x43, (byte) 0xff, 0x7f, 0x00, (byte) 0x80});

        assertEquals(expected, actual.get());
    }

    @Test
    public void interpretComponentData() {
        float[] expected = new float[] {262.409f, 499.497f, -499.512f};
        final float[] actual= new float[3];

        gyro.configure()
                .range(Range.FSR_500)
                .commit();
        gyro.angularVelocity().addRouteAsync(source -> source.split()
                .index(0).stream((data, env) -> ((float[]) env[0])[0] = data.value(Float.class))
                .index(1).stream((data, env) -> ((float[]) env[0])[1] = data.value(Float.class))
                .index(2).stream((data, env) -> ((float[]) env[0])[2] = data.value(Float.class)))
                .continueWith(task -> {
                    task.getResult().setEnvironment(0, (Object) actual);
                    task.getResult().setEnvironment(1, (Object) actual);
                    task.getResult().setEnvironment(2, (Object) actual);
                    return null;
                });
        sendMockResponse(new byte[] {0x13, 0x04, 0x3e, 0x43, (byte) 0xff, 0x7f, 0x00, (byte) 0x80});

        assertArrayEquals(expected, actual, 0.001f);
    }

    @Test
    public void subscribe() {
        byte[] expected= new byte[] { 0x13, 0x04, 0x01 };
        gyro.angularVelocity().addRouteAsync(source -> source.stream(null));

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }

    @Test
    public void unsubscribe() {
        byte[] expected= new byte[] { 0x13, 0x04, 0x00 };
        gyro.angularVelocity().addRouteAsync(source -> source.stream(null)).continueWith(task -> {
            task.getResult().unsubscribe(0);
            return null;
        });

        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
