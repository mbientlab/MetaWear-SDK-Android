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

import static com.mbientlab.metawear.module.AccelerometerBmi160.StepDetectorMode.NORMAL;
import static com.mbientlab.metawear.module.AccelerometerBmi160.StepDetectorMode.ROBUST;
import static com.mbientlab.metawear.module.AccelerometerBmi160.StepDetectorMode.SENSITIVE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.mbientlab.metawear.module.AccelerometerBmi160;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by etsai on 11/14/16.
 */
public class TestBmi160StepDetectionConfig extends UnitTestBase {
    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        parameters.add(Arguments.of(NORMAL,
                new byte[] {0x03, 0x18, 0x15, 0x0b},
                new byte[] {0x03, 0x18, 0x15, 0x03}));
        parameters.add(Arguments.of(SENSITIVE,
                new byte[] {0x03, 0x18, 0x2d, 0x08},
                new byte[] {0x03, 0x18, 0x2d, 0x00}));

        parameters.add(Arguments.of(ROBUST,
                new byte[] {0x03, 0x18, 0x1d, 0x0f},
                new byte[] {0x03, 0x18, 0x1d, 0x07}));
        return parameters.stream();
    }

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class);
        connectToBoard();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureDetector(AccelerometerBmi160.StepDetectorMode mode, byte[] expectedCounter, byte[] expectedDetector) {
        mwBoard.getModule(AccelerometerBmi160.class).stepDetector().configure()
                .mode(mode)
                .commit();
        assertArrayEquals(expectedDetector, junitPlatform.getLastCommand());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configureCounter(AccelerometerBmi160.StepDetectorMode mode, byte[] expectedCounter, byte[] expectedDetector) {
        mwBoard.getModule(AccelerometerBmi160.class).stepCounter().configure()
                .mode(mode)
                .commit();
        assertArrayEquals(expectedCounter, junitPlatform.getLastCommand());
    }
}
