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

import static com.mbientlab.metawear.MetaWearBoardInfo.C;
import static com.mbientlab.metawear.MetaWearBoardInfo.CPRO;
import static com.mbientlab.metawear.MetaWearBoardInfo.DETECTOR;
import static com.mbientlab.metawear.MetaWearBoardInfo.ENVIRONMENT;
import static com.mbientlab.metawear.MetaWearBoardInfo.MOTION_R;
import static com.mbientlab.metawear.MetaWearBoardInfo.MOTION_RL;
import static com.mbientlab.metawear.MetaWearBoardInfo.MOTION_S;
import static com.mbientlab.metawear.MetaWearBoardInfo.R;
import static com.mbientlab.metawear.MetaWearBoardInfo.RG;
import static com.mbientlab.metawear.MetaWearBoardInfo.RPRO;
import static com.mbientlab.metawear.MetaWearBoardInfo.TRACKER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by etsai on 12/21/16.
 * Updated by lkasso
 */
public class TestModel extends UnitTestBase {
    private static Stream<Arguments> data() {
        List<Arguments> parameters = new LinkedList<>();
        for(MetaWearBoardInfo info: new MetaWearBoardInfo[] {C, CPRO, DETECTOR, ENVIRONMENT, RPRO, R, RG, MOTION_R, MOTION_RL, MOTION_S, TRACKER}) {
            parameters.add(Arguments.of(info));
        }
        return parameters.stream();
    }

    public void setup(MetaWearBoardInfo info) {
        try {
            junitPlatform.boardInfo = info;
            connectToBoard();
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void checkModel(MetaWearBoardInfo info) {
        setup(info);
        assertEquals(info.model, mwBoard.getModel());
    }
}
