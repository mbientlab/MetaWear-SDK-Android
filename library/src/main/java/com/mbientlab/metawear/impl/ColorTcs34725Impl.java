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

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.module.ColorTcs34725;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Locale;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.COLOR_DETECTOR;
import static com.mbientlab.metawear.impl.Constant.Module.DATA_PROCESSOR;

/**
 * Created by etsai on 9/19/16.
 */
class ColorTcs34725Impl extends ModuleImplBase implements ColorTcs34725 {
    static String createUri(DataTypeBase dataType) {
        switch (Util.clearRead(dataType.eventConfig[1])) {
            case ADC:
                return dataType.attributes.length() > 2 ? "color" : String.format(Locale.US, "color[%d]", (dataType.attributes.offset >> 1));
            default:
                return null;
        }
    }

    private final static String ADC_PRODUCER= "com.mbientlab.metawear.impl.ColorTcs34725Impl.ADC_PRODUCER",
            ADC_CLEAR_PRODUCER= "com.mbientlab.metawear.impl.ColorTcs34725Impl.ADC_CLEAR_PRODUCER",
            ADC_RED_PRODUCER= "com.mbientlab.metawear.impl.ColorTcs34725Impl.ADC_RED_PRODUCER",
            ADC_GREEN_PRODUCER= "com.mbientlab.metawear.impl.ColorTcs34725Impl.ADC_GREEN_PRODUCER",
            ADC_BLUE_PRODUCER= "com.mbientlab.metawear.impl.ColorTcs34725Impl.ADC_BLUE_PRODUCER";
    private static final byte ADC = 1, MODE = 2;
    private static final long serialVersionUID = -6867360365437005527L;

    private static UintData createAdcUintDataProducer(byte offset) {
        return new UintData(COLOR_DETECTOR, Util.setSilentRead(ADC), new DataAttributes(new byte[] {2}, (byte) 1, offset, true));
    }
    static class ColorAdcData extends DataTypeBase {
        private static final long serialVersionUID = 3597945676319055134L;

        ColorAdcData() {
            super(COLOR_DETECTOR, Util.setSilentRead(ADC), new DataAttributes(new byte[] {2, 2, 2, 2}, (byte) 1, (byte) 0, false));
        }

        ColorAdcData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new ColorAdcData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate mwPrivate, Number value) {
            return value;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, final Calendar timestamp, DataPrivate.ClassToObject mapper) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            final ColorAdc wrapper= new ColorAdc(
                    buffer.getShort() & 0xffff,
                    buffer.getShort() & 0xffff,
                    buffer.getShort() & 0xffff,
                    buffer.getShort() & 0xffff
            );

            return new DataPrivate(timestamp, data, mapper) {
                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {ColorAdc.class};
                }

                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz == ColorAdc.class) {
                        return clazz.cast(wrapper);
                    }
                    return super.value(clazz);
                }
            };
        }

        @Override
        public DataTypeBase[] createSplits() {
            return new DataTypeBase[] {createAdcUintDataProducer((byte) 0), createAdcUintDataProducer((byte) 2),
                    createAdcUintDataProducer((byte) 4), createAdcUintDataProducer((byte) 6)};
        }

        @Override
        Pair<? extends DataTypeBase, ? extends DataTypeBase> dataProcessorTransform(DataProcessorConfig config, DataProcessorImpl dpModule) {
            switch(config.id) {
                case DataProcessorConfig.Combiner.ID: {
                    DataAttributes attributes= new DataAttributes(new byte[] {this.attributes.sizes[0]}, (byte) 1, (byte) 0, false);
                    return new Pair<>(new UintData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, attributes), null);
                }
            }

            return super.dataProcessorTransform(config, dpModule);
        }
    }

    private transient ColorAdcDataProducer adcProducer;

    ColorTcs34725Impl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        DataTypeBase adcProducer = new ColorAdcData();
        this.mwPrivate.tagProducer(ADC_PRODUCER, adcProducer);
        this.mwPrivate.tagProducer(ADC_CLEAR_PRODUCER, adcProducer.split[0]);
        this.mwPrivate.tagProducer(ADC_RED_PRODUCER, adcProducer.split[1]);
        this.mwPrivate.tagProducer(ADC_GREEN_PRODUCER, adcProducer.split[2]);
        this.mwPrivate.tagProducer(ADC_BLUE_PRODUCER, adcProducer.split[3]);
    }

    @Override
    public ConfigEditor configure() {
        return new ConfigEditor() {
            private byte aTime= (byte) 0xff;
            private Gain gain= Gain.TCS34725_1X;
            private byte illuminate= 0;

            @Override
            public ConfigEditor integrationTime(float time) {
                aTime= (byte) (256.f - time / 2.4f);
                return this;
            }

            @Override
            public ConfigEditor gain(Gain gain) {
                this.gain= gain;
                return this;
            }

            @Override
            public ConfigEditor enableIlluminatorLed() {
                illuminate= 1;
                return this;
            }

            @Override
            public void commit() {
                mwPrivate.sendCommand(new byte[] {COLOR_DETECTOR.id, MODE, aTime, (byte) gain.ordinal(), illuminate});
            }
        };
    }

    @Override
    public ColorAdcDataProducer adc() {
        if (adcProducer == null) {
            adcProducer = new ColorAdcDataProducer() {
                @Override
                public void read() {
                    mwPrivate.lookupProducer(ADC_PRODUCER).read(mwPrivate);
                }

                @Override
                public String clearName() {
                    return ADC_CLEAR_PRODUCER;
                }

                @Override
                public String redName() {
                    return ADC_RED_PRODUCER;
                }

                @Override
                public String greenName() {
                    return ADC_GREEN_PRODUCER;
                }

                @Override
                public String blueName() {
                    return ADC_BLUE_PRODUCER;
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, ADC_PRODUCER);
                }

                @Override
                public String name() {
                    return ADC_PRODUCER;
                }
            };
        }
        return adcProducer;
    }
}
