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

import com.mbientlab.metawear.module.ProximityTsl2671;
import com.mbientlab.metawear.module.ProximityTsl2671.ReceiverDiode;
import com.mbientlab.metawear.module.ProximityTsl2671.TransmitterDriveCurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by etsai on 10/2/16.
 */
public class TestProximityTsl2671Config extends UnitTestBase {
    private final float[] INTEGRATION_TIMES= new float[] {5.44f, 693.6f};
    private final byte[] INTEGRATION_BITMASKS= new byte[] {(byte) 0xfe, 0x1},
            CURRENT_BITMASKS = new byte[] {0x00, 0x01, 0x02, 0x03},
            DIODE_BITMASKS= new byte[] {0x01, 0x02, 0x03};

    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        for(int i= 0; i < 2; i++) {
            for(ReceiverDiode diode: ReceiverDiode.values()) {
                for(TransmitterDriveCurrent current: TransmitterDriveCurrent.values()) {
                    parameters.add(Arguments.of(i, diode, current));
                }
            }
        }
        return parameters.stream();
    }

    private ProximityTsl2671 proximity;

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo = new MetaWearBoardInfo(ProximityTsl2671.class);
        connectToBoard();

        proximity= mwBoard.getModule(ProximityTsl2671.class);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void configure(int intgrationIdx, ReceiverDiode diode, TransmitterDriveCurrent current) {
        byte[] expected= new byte[] {0x18, 0x02, INTEGRATION_BITMASKS[intgrationIdx], 0x20,
                (byte) ((CURRENT_BITMASKS[current.ordinal()] << 6) | (DIODE_BITMASKS[diode.ordinal()] << 4))};

        proximity.configure()
                .integrationTime(INTEGRATION_TIMES[intgrationIdx])
                .pulseCount((byte) 32)
                .receiverDiode(diode)
                .transmitterDriveCurrent(current)
                .commit();
        assertArrayEquals(expected, junitPlatform.getLastCommand());
    }
}
