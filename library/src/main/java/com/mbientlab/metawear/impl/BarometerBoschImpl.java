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

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.module.BarometerBosch;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.BAROMETER;

/**
 * Created by etsai on 9/20/16.
 */
abstract class BarometerBoschImpl extends ModuleImplBase implements BarometerBosch{
    static String createUri(DataTypeBase dataType) {
        switch (dataType.eventConfig[1]) {
            case PRESSURE:
                return "pressure";
            case ALTITUDE:
                return "altitude";
            default:
                return null;
        }
    }

    private final static String PRESSURE_PRODUCER= "com.mbientlab.metawear.impl.BarometerBoschImpl.PRESSURE_PRODUCER",
            ALTITUDE_PRODUCER= "com.mbientlab.metawear.impl.BarometerBoschImpl.ALTITUDE_PRODUCER";
    private static final byte PRESSURE = 1, ALTITUDE = 2, CYCLIC = 4;
    static final byte CONFIG = 3;
    private static final long serialVersionUID = 8553278769965522159L;

    private static class BoschPressureUFloatData extends UFloatData {
        private static final long serialVersionUID = 5645582168037917626L;

        BoschPressureUFloatData() {
            super(BAROMETER, PRESSURE, new DataAttributes(new byte[] {4}, (byte) 1, (byte) 0, false));
        }

        BoschPressureUFloatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschPressureUFloatData(input, module, register, id, attributes);
        }

        @Override
        protected float scale(MetaWearBoardPrivate mwPrivate) {
            return 256.f;
        }
    }
    private static class BoschAltitudeSFloatData extends SFloatData {
        private static final long serialVersionUID = -7561816282096806876L;

        BoschAltitudeSFloatData() {
            super(BAROMETER, ALTITUDE, new DataAttributes(new byte[] {4}, (byte) 1, (byte) 0, true));
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschPressureUFloatData(input, module, register, id, attributes);
        }

        @Override
        protected float scale(MetaWearBoardPrivate mwPrivate) {
            return 256.f;
        }
    }

    private byte enableAltitude= 0;

    BarometerBoschImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        mwPrivate.tagProducer(PRESSURE_PRODUCER, new BoschPressureUFloatData());
        mwPrivate.tagProducer(ALTITUDE_PRODUCER, new BoschAltitudeSFloatData());
    }

    @Override
    public AsyncDataProducer pressure() {
        return new AsyncDataProducer() {
            @Override
            public Task<Route> addRouteAsync(RouteBuilder builder) {
                return mwPrivate.queueRouteBuilder(builder, PRESSURE_PRODUCER);
            }

            @Override
            public String name() {
                return PRESSURE_PRODUCER;
            }

            @Override
            public void start() {

            }

            @Override
            public void stop() {

            }
        };
    }

    @Override
    public AsyncDataProducer altitude() {
        return new AsyncDataProducer() {
            @Override
            public Task<Route> addRouteAsync(RouteBuilder builder) {
                return mwPrivate.queueRouteBuilder(builder, ALTITUDE_PRODUCER);
            }

            @Override
            public String name() {
                return ALTITUDE_PRODUCER;
            }

            @Override
            public void start() {
                enableAltitude= 1;
            }

            @Override
            public void stop() {
                enableAltitude= 0;
            }
        };
    }

    @Override
    public void start() {
        mwPrivate.sendCommand(new byte[] {BAROMETER.id, CYCLIC, 1, enableAltitude});
    }

    @Override
    public void stop() {
        mwPrivate.sendCommand(new byte[] {BAROMETER.id, CYCLIC, 0, 0});
    }
}
