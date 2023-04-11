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
import com.mbientlab.metawear.module.Led;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


/**
 * Created by etsai on 8/31/16.
 */
public class TestLedPattern extends UnitTestBase {
    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        parameters.add(Arguments.of(
                        false,
                        (short) 5000,
                        (byte) 10,
                        new byte[] {0x02, 0x03, 0x00, 0x02, 0x1f, 0x00, 0x00, 0x00, 0x32, 0x00, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, 0x0a},
                        Led.Color.GREEN,
                        Led.PatternPreset.BLINK
        ));
        parameters.add(Arguments.of(
                        true,
                        (short) 5000,
                        (byte) 10,
                        new byte[] {0x02, 0x03, 0x00, 0x02, 0x1f, 0x00, 0x00, 0x00, 0x32, 0x00, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, 0x0a},
                        Led.Color.GREEN,
                        Led.PatternPreset.BLINK
        ));
        parameters.add(Arguments.of(
                        false,
                        (short) 10000,
                        (byte) 20,
                        new byte[] {0x02, 0x03, 0x01, 0x02, 0x1f, 0x1f, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xE8, 0x03, 0x00, 0x00, 0x14},
                        Led.Color.RED,
                        Led.PatternPreset.SOLID
        ));
        parameters.add(Arguments.of(
                        true,
                        (short) 10000,
                        (byte) 20,
                        new byte[] {0x02, 0x03, 0x01, 0x02, 0x1f, 0x1f, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xE8, 0x03, 0x00, 0x00, 0x14},
                        Led.Color.RED,
                        Led.PatternPreset.SOLID
        ));
        parameters.add(Arguments.of(
                        false,
                        (short) 12345,
                        (byte) 40,
                        new byte[] {0x02, 0x03, 0x02, 0x02, 0x1f, 0x00, (byte) 0xd5, 0x02, (byte) 0xf4, 0x01, (byte) 0xd5, 0x02, (byte) 0xd0, 0x07, 0x00, 0x00, 0x28},
                        Led.Color.BLUE,
                        Led.PatternPreset.PULSE
        ));
        parameters.add(Arguments.of(
                        true,
                        (short) 12345,
                        (byte) 40,
                        new byte[] {0x02, 0x03, 0x02, 0x02, 0x1f, 0x00, (byte) 0xd5, 0x02, (byte) 0xf4, 0x01, (byte) 0xd5, 0x02, (byte) 0xd0, 0x07, 0x00, 0x00, 0x28},
                        Led.Color.BLUE,
                        Led.PatternPreset.PULSE
        ));
        return parameters.stream();
    }



    private byte[] expected;

    public Task<Void> setup(boolean delaySupported, short delay, byte[] expectedBase) {
        try {
            junitPlatform.boardInfo = new MetaWearBoardInfo(Led.class);

            expected = new byte[expectedBase.length];
            System.arraycopy(expectedBase, 0, expected, 0, expectedBase.length);

            if (delaySupported) {
                junitPlatform.addCustomModuleInfo(new byte[]{0x02, (byte) 0x80, 0x00, 0x01, 0x03, 0x00});
                expected[15] = (byte) ((delay >> 8) & 0xff);
                expected[14] = (byte) (delay & 0xff);
            }

            return connectToBoard();
        } catch (Exception e) {
            fail(e);
            return Tasks.forException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void writePresetPattern(boolean delaySupported, short delay, byte repeat,
                                   byte[] expectedBase, Led.Color color, Led.PatternPreset pattern) throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        setup(delaySupported, delay, expectedBase).addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            mwBoard.getModule(Led.class).editPattern(color, pattern)
                    .delay(delay)
                    .repeatCount(repeat)
                    .commit();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
