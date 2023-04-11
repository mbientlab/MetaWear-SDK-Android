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
import static org.junit.jupiter.api.Assertions.fail;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.MagnetometerBmm150.Preset;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by etsai on 10/6/16.
 */
public class TestMagnetometerBmm150Config extends UnitTestBase {
    static final byte SLEEP_REV = 2;
    private static final byte[] XY_BITMASK= new byte[] { 0x01, 0x04, 0x07, 0x17 },
            Z_BITMASK= new byte[] { 0x02, 0x0e, 0x1a, 0x52 },
            ODR_BITMASK= new byte[] { 0, 0, 0, 5};

    private static Stream<Arguments> data() {
        List<Arguments> params = new LinkedList<>();
        for(Preset preset: Preset.values()) {
            params.add(Arguments.of(preset, (byte) 1));
            params.add(Arguments.of(preset, SLEEP_REV));
        }
        return params.stream();
    }

    private MagnetometerBmm150 mag;

    public Task<Void> setup(byte revision) {
        try {
            junitPlatform.addCustomModuleInfo(new byte[]{0x15, (byte) 0x80, 0x00, revision});
            return connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mag = mwBoard.getModule(MagnetometerBmm150.class);
            });
        } catch (Exception e) {
            fail(e);
            return Tasks.forException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configure(Preset preset, byte revision) throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup(revision).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            byte[][] expected= revision == SLEEP_REV ? new byte[][] {
                    { 0x15, 0x01, 0x00},
                    { 0x15, 0x04, XY_BITMASK[preset.ordinal()], Z_BITMASK[preset.ordinal()] },
                    { 0x15, 0x03, ODR_BITMASK[preset.ordinal()] }
            } : new byte[][] {
                    { 0x15, 0x04, XY_BITMASK[preset.ordinal()], Z_BITMASK[preset.ordinal()] },
                    { 0x15, 0x03, ODR_BITMASK[preset.ordinal()] }
            };
            mag.usePreset(preset);

            assertArrayEquals(expected, junitPlatform.getCommands());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
