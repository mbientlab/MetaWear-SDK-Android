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

import com.google.android.gms.tasks.Task;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.Gyro.OutputDataRate;
import com.mbientlab.metawear.module.Gyro.Range;
import com.mbientlab.metawear.module.GyroBmi270;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by lkasso on 04/01/21.
 */
public class TestGyro270Config extends UnitTestBase {
    static final byte[] ODR_BITMASK= new byte[] { 0b0110, 0b0111, 0b1000, 0b1001, 0b1010, 0b1011, 0b1100, 0b1101 },
            RANGE_BITMASK= new byte[] { 0b000, 0b001, 0b010, 0b011, 0b100 };

    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        for(OutputDataRate odr: OutputDataRate.values()) {
            for(Range fsr: Range.values()) {
                parameters.add(Arguments.of(odr, fsr));
            }
        }
        return parameters.stream();
    }

    private Gyro gyro;

    public Task<Void> setup() {
        junitPlatform.boardInfo = new MetaWearBoardInfo(GyroBmi270.class);
        return connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            gyro = mwBoard.getModule(GyroBmi270.class);
        });
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configure(OutputDataRate odr, Range fsr) throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        byte[] expected= new byte[] {0x13, 0x03, (byte) (0x20 | ODR_BITMASK[odr.ordinal()]), RANGE_BITMASK[fsr.ordinal()]};

        setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            gyro.configure()
                    .odr(odr)
                    .range(fsr)
                    .commit();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}