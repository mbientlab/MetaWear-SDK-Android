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
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.impl.platform.TimedTask;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi270;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Locale;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.ACCELEROMETER;

/**
 * Created by lkasso on 4/1/21.
 */
class AccelerometerBmi270Impl extends ModuleImplBase implements AccelerometerBmi270 {
    static String createUri(DataTypeBase dataType) {
        switch (dataType.eventConfig[1]) {
            case DATA_INTERRUPT:
                return dataType.attributes.length() > 2 ? "acceleration" : String.format(Locale.US, "acceleration[%d]", (dataType.attributes.offset >> 1));
            case PACKED_ACC_DATA:
                return "acceleration";
            default:
                return null;
        }
    }

    private static final long serialVersionUID = 0000000;
    final static byte IMPLEMENTATION= 0x4;
    protected static final byte POWER_MODE = 1,
            DATA_INTERRUPT_ENABLE = 2, DATA_CONFIG = 3, DATA_INTERRUPT = 4, PACKED_ACC_DATA= 0x5;
    protected final static String ACCEL_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBmi270Impl.ACCEL_PRODUCER",
            ACCEL_X_AXIS_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBmi270Impl.ACCEL_X_AXIS_PRODUCER",
            ACCEL_Y_AXIS_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBmi270Impl.ACCEL_Y_AXIS_PRODUCER",
            ACCEL_Z_AXIS_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBmi270Impl.ACCEL_Z_AXIS_PRODUCER",
            ACCEL_PACKED_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBmi270Impl.ACCEL_PACKED_PRODUCER";
    private final byte[] accDataConfig= new byte[] {(byte) 0xa8, 0x02};

    private transient AsyncDataProducer packedAcceleration, acceleration;
    private transient TimedTask<byte[]> pullConfigTask;

    private static class BoschAccCartesianFloatData extends FloatVectorData {
        private static final long serialVersionUID = 00000;

        public BoschAccCartesianFloatData() {
            this(DATA_INTERRUPT, (byte) 1);
        }

        public BoschAccCartesianFloatData(byte register, byte copies) {
            super(ACCELEROMETER, register, new DataAttributes(new byte[] {2, 2, 2}, copies, (byte) 0, true));
        }

        public BoschAccCartesianFloatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschAccCartesianFloatData(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase[] createSplits() {
            return new DataTypeBase[] {new BoschAccSFloatData((byte) 0), new BoschAccSFloatData((byte) 2), new BoschAccSFloatData((byte) 4)};
        }

        @Override
        protected float scale(MetaWearBoardPrivate mwPrivate) {
            return ((AccelerometerBmi270Impl) mwPrivate.getModules().get(Accelerometer.class)).getAccDataScale();
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, final Calendar timestamp, DataPrivate.ClassToObject mapper) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            short[] unscaled = new short[]{buffer.getShort(), buffer.getShort(), buffer.getShort()};
            final float scale= scale(mwPrivate);
            final Acceleration value= new Acceleration(unscaled[0] / scale, unscaled[1] / scale, unscaled[2] / scale);

            return new DataPrivate(timestamp, data, mapper) {
                @Override
                public float scale() {
                    return scale;
                }

                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {Acceleration.class, float[].class};
                }

                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz.equals(Acceleration.class)) {
                        return clazz.cast(value);
                    } else if (clazz.equals(float[].class)) {
                        return clazz.cast(new float[] {value.x(), value.y(), value.z()});
                    }
                    return super.value(clazz);
                }
            };
        }
    }
    private static class BoschAccSFloatData extends SFloatData {
        private static final long serialVersionUID = 0000000;

        BoschAccSFloatData(byte offset) {
            super(ACCELEROMETER, DATA_INTERRUPT, new DataAttributes(new byte[] {2}, (byte) 1, offset, true));
        }

        BoschAccSFloatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        protected float scale(MetaWearBoardPrivate mwPrivate) {
            return ((AccelerometerBmi270Impl) mwPrivate.getModules().get(Accelerometer.class)).getAccDataScale();
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschAccSFloatData(input, module, register, id, attributes);
        }
    }

    AccelerometerBmi270Impl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        DataTypeBase cfProducer = new BoschAccCartesianFloatData();

        this.mwPrivate= mwPrivate;
        this.mwPrivate.tagProducer(ACCEL_PRODUCER, cfProducer);
        this.mwPrivate.tagProducer(ACCEL_X_AXIS_PRODUCER, cfProducer.split[0]);
        this.mwPrivate.tagProducer(ACCEL_Y_AXIS_PRODUCER, cfProducer.split[1]);
        this.mwPrivate.tagProducer(ACCEL_Z_AXIS_PRODUCER, cfProducer.split[2]);
        this.mwPrivate.tagProducer(ACCEL_PACKED_PRODUCER, new BoschAccCartesianFloatData(PACKED_ACC_DATA, (byte) 3));
    }

    @Override
    protected void init() {
        pullConfigTask = new TimedTask<>();
        mwPrivate.addResponseHandler(new Pair<>(ACCELEROMETER.id, Util.setRead(DATA_CONFIG)), response -> pullConfigTask.setResult(response));
    }

    protected float getAccDataScale() {
        return AccRange.bitMaskToRange((byte) (accDataConfig[1] & 0x0f)).scale;
    }

    @Override
    public AccelerometerBmi270.ConfigEditor configure() {
        return new AccelerometerBmi270.ConfigEditor() {
            private OutputDataRate odr= OutputDataRate.ODR_100_HZ;
            private AccRange ar= AccRange.AR_8G;
            private FilterMode mode = FilterMode.NORMAL;

            @Override
            public AccelerometerBmi270.ConfigEditor odr(OutputDataRate odr) {
                this.odr= odr;
                return this;
            }

            @Override
            public AccelerometerBmi270.ConfigEditor range(AccRange ar) {
                this.ar= ar;
                return this;
            }

            @Override
            public AccelerometerBmi270.ConfigEditor filter(FilterMode mode) {
                this.mode = mode;
                return this;
            }

            @Override
            public AccelerometerBmi270.ConfigEditor odr(float odr) {
                return odr(OutputDataRate.values()[Util.closestIndex(OutputDataRate.frequencies(), odr)]);
            }

            @Override
            public AccelerometerBmi270.ConfigEditor range(float fsr) {
                return range(AccRange.values()[Util.closestIndex(AccRange.ranges(), fsr)]);
            }

            @Override
            public void commit() {
                accDataConfig[0]&= 0xf0;
                accDataConfig[0]|= odr.ordinal() + 1;

                accDataConfig[0]&= 0x0f;
                accDataConfig[0]|= 0xa0; //TODO

                accDataConfig[1]&= 0xf0;
                accDataConfig[1]|= ar.bitmask;

                mwPrivate.sendCommand(ACCELEROMETER, DATA_CONFIG, accDataConfig);
            }
        };
    }

    @Override
    public AsyncDataProducer packedAcceleration() {
        if (packedAcceleration == null) {
            packedAcceleration = new AsyncDataProducer() {
                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, ACCEL_PACKED_PRODUCER);
                }

                @Override
                public String name() {
                    return ACCEL_PACKED_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, DATA_INTERRUPT_ENABLE, 0x01, 0x00});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, DATA_INTERRUPT_ENABLE, 0x00, 0x01});
                }
            };
        }
        return packedAcceleration;
    }

    @Override
    public AccelerationDataProducer acceleration() {
        if (acceleration == null) {
            acceleration = new AccelerationDataProducer() {
                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, ACCEL_PRODUCER);
                }

                @Override
                public String name() {
                    return ACCEL_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, DATA_INTERRUPT_ENABLE, 0x01, 0x00});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, DATA_INTERRUPT_ENABLE, 0x00, 0x01});
                }

                @Override
                public String xAxisName() {
                    return ACCEL_X_AXIS_PRODUCER;
                }

                @Override
                public String yAxisName() {
                    return ACCEL_Y_AXIS_PRODUCER;
                }

                @Override
                public String zAxisName() {
                    return ACCEL_Z_AXIS_PRODUCER;
                }
            };
        }
        return (AccelerationDataProducer) acceleration;
    }

    @Override
    public float getOdr() {
        return OutputDataRate.values()[(byte) (accDataConfig[0] & ~0xf0) - 1].frequency;
    }

    @Override
    public float getRange() {
        return AccRange.bitMaskToRange((byte) (accDataConfig[1] & ~0x30)).range;
    }

    @Override
    public Task<Void> pullConfigAsync() {
        return pullConfigTask.execute("Did not receive BMI270 acc config within %dms", Constant.RESPONSE_TIMEOUT,
                () -> mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, Util.setRead(DATA_CONFIG)})
        ).onSuccessTask(task -> {
            System.arraycopy(task.getResult(), 2, accDataConfig, 0, accDataConfig.length);
            return Task.forResult(null);
        });
    }

    @Override
    public void start() {
        mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, POWER_MODE, 0x01});
    }

    @Override
    public void stop() {
        mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, POWER_MODE, 0x00});
    }

}
