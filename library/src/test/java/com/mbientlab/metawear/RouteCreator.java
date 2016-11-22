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

import com.mbientlab.metawear.builder.RouteElement.Action;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.Passthrough;
import com.mbientlab.metawear.builder.function.Function2;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteElement;

import bolts.Task;

/**
 * Created by eric on 10/14/16.
 */

class RouteCreator {
    static Task<Route> createGpioFeedback(MetaWearBoard board) {
        final DataProcessor dataprocessor= board.getModule(DataProcessor.class);
        return board.getModule(Gpio.class).getPin((byte) 0).analogAbsRef().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.multicast()
                        .to()
                            .limit(Passthrough.COUNT, (short) 0).name("adc").react(new Action() {
                                @Override
                                public void execute(DataToken token) {
                                    dataprocessor.edit("lte_count", DataProcessor.Counter.class)
                                            .reset();
                                    dataprocessor.edit("gt_count", DataProcessor.Counter.class)
                                            .reset();
                                }
                            })
                        .to()
                            .map(Function2.SUBTRACT, "adc").multicast()
                            .to()
                                .filter(Comparison.GT, 0).react(new Action() {
                                    @Override
                                    public void execute(DataToken token) {
                                        dataprocessor.edit("lte_count", DataProcessor.Counter.class)
                                                .reset();
                                    }
                                })
                                .count().name("gt_count")
                                .filter(Comparison.EQ, 16).react(new Action() {
                                    @Override
                                    public void execute(DataToken token) {
                                        dataprocessor.edit("adc", DataProcessor.PassthroughLimiter.class)
                                                .set((short) 1);
                                    }
                                })
                            .to()
                                .filter(Comparison.LTE, 0).name("lte").react(new Action() {
                                    @Override
                                    public void execute(DataToken token) {
                                        dataprocessor.edit("gt_count", DataProcessor.Counter.class)
                                                .reset();
                                    }
                                })
                                .count().name("lte_count")
                                .filter(Comparison.EQ, 16).react(new Action() {
                                    @Override
                                    public void execute(DataToken data) {
                                        dataprocessor.edit("adc", DataProcessor.PassthroughLimiter.class)
                                                .set((short) 1);
                                    }
                                })
                            .end()
                        .end();
            }
        });
    }
}
