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

package com.mbientlab.metawear.impl;

import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.module.HumidityBme280;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.HUMIDITY;

/**
 * Created by etsai on 9/19/16.
 */
class HumidityBme280Impl extends ModuleImplBase implements HumidityBme280 {
    static String createUri(DataTypeBase dataType) {
        switch (Util.clearRead(dataType.eventConfig[1])) {
            case VALUE:
                return "relative-humidity";
            default:
                return null;
        }
    }

    private final static String PRODUCER= "com.mbientlab.metawear.impl.HumidityBme280Impl.PRODUCER";
    private final static byte VALUE = 1, MODE = 2;
    private static final long serialVersionUID = 1478927889851422678L;

    private static class HumidityBme280SFloatData extends UFloatData {
        private static final long serialVersionUID = -2742030048950836121L;

        HumidityBme280SFloatData() {
            super(HUMIDITY, Util.setSilentRead(VALUE), new DataAttributes(new byte[] {4}, (byte) 1, (byte) 0, false));
        }

        HumidityBme280SFloatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new HumidityBme280SFloatData(input, module, register, id, attributes);
        }

        @Override
        protected float scale(MetaWearBoardPrivate mwPrivate) {
            return 1024.f;
        }
    }

    private transient ForcedDataProducer humidityProducer;

    HumidityBme280Impl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        mwPrivate.tagProducer(PRODUCER, new HumidityBme280SFloatData());
    }

    @Override
    public void setOversampling(OversamplingMode mode) {
        mwPrivate.sendCommand(new byte[] {HUMIDITY.id, MODE, (byte) (mode.ordinal() + 1)});
    }

    @Override
    public ForcedDataProducer value() {
        if (humidityProducer == null) {
            humidityProducer = new ForcedDataProducer() {
                @Override
                public void read() {
                    mwPrivate.lookupProducer(PRODUCER).read(mwPrivate);
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, PRODUCER);
                }

                @Override
                public String name() {
                    return PRODUCER;
                }
            };
        }
        return humidityProducer;
    }
}
