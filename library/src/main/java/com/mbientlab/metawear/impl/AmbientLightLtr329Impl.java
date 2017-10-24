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
import com.mbientlab.metawear.module.AmbientLightLtr329;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.AMBIENT_LIGHT;

/**
 * Created by etsai on 9/20/16.
 */
class AmbientLightLtr329Impl extends ModuleImplBase implements AmbientLightLtr329 {
    static String createUri(DataTypeBase dataType) {
        switch (dataType.eventConfig[1]) {
            case OUTPUT:
                return "illuminance";
            default:
                return null;
        }
    }

    private final static String ILLUMINANCE_PRODUCER= "com.mbientlab.metawear.impl.AmbientLightLtr329Impl.ILLUMINANCE_PRODUCER";
    private static final byte ENABLE = 1, CONFIG = 2, OUTPUT = 3;
    private static final long serialVersionUID = 8287988596635899285L;

    private transient AsyncDataProducer illuminanceProducer;

    AmbientLightLtr329Impl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
        mwPrivate.tagProducer(ILLUMINANCE_PRODUCER, new MilliUnitsUFloatData(AMBIENT_LIGHT, OUTPUT, new DataAttributes(new byte[] {4}, (byte) 1, (byte) 0, false)));
    }

    @Override
    public ConfigEditor configure() {
        return new ConfigEditor() {
            private Gain ltr329Gain= Gain.LTR329_1X;
            private IntegrationTime ltr329Time= IntegrationTime.LTR329_TIME_100MS;
            private MeasurementRate ltr329Rate= MeasurementRate.LTR329_RATE_500MS;

            @Override
            public ConfigEditor gain(Gain sensorGain) {
                ltr329Gain= sensorGain;
                return this;
            }

            @Override
            public ConfigEditor integrationTime(IntegrationTime time) {
                ltr329Time= time;
                return this;
            }

            @Override
            public ConfigEditor measurementRate(MeasurementRate rate) {
                ltr329Rate= rate;
                return this;
            }

            @Override
            public void commit() {
                byte alsContr= (byte) (ltr329Gain.bitmask << 2);
                byte alsMeasRate= (byte) ((ltr329Time.bitmask << 3) | ltr329Rate.ordinal());

                mwPrivate.sendCommand(new byte[] {AMBIENT_LIGHT.id, CONFIG, alsContr, alsMeasRate});
            }
        };
    }

    @Override
    public AsyncDataProducer illuminance() {
        if (illuminanceProducer == null) {
            illuminanceProducer = new AsyncDataProducer() {
                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, ILLUMINANCE_PRODUCER);
                }

                @Override
                public String name() {
                    return ILLUMINANCE_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {AMBIENT_LIGHT.id, ENABLE, 0x1});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {AMBIENT_LIGHT.id, ENABLE, 0x0});
                }
            };
        }
        return illuminanceProducer;
    }
}
