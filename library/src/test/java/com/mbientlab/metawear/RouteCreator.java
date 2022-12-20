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

import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.function.Function2;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Switch;

import bolts.Task;

/**
 * Created by eric on 10/14/16.
 */

class RouteCreator {
    static Task<Route> createLedController(MetaWearBoard board) {
        final Led led= board.getModule(Led.class);
        return board.getModule(Switch.class).state().addRouteAsync(source -> source.count().map(Function2.MODULUS, 2)
                .multicast()
                .to().filter(Comparison.EQ, 1).react(token -> {
                            led.editPattern(Led.Color.BLUE)
                                    .highIntensity((byte) 16).lowIntensity((byte) 16)
                                    .pulseDuration((short) 1000)
                                    .highTime((short) 500)
                                    .repeatCount(Led.PATTERN_REPEAT_INDEFINITELY)
                                    .commit();
                            led.play();
                        })
                .to().filter(Comparison.EQ, 0).react(token -> led.stop(true))
                .end());
    }
}
