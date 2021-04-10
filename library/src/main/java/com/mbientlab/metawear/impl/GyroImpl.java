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

import com.mbientlab.metawear.impl.platform.TimedTask;
import com.mbientlab.metawear.module.Gyro;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.GYRO;

/**
 * Created by etsai on 9/20/16.
 */
abstract class GyroImpl extends ModuleImplBase implements Gyro {
    static String createUri(DataTypeBase dataType) {
        switch (dataType.eventConfig[1]) {
            //case DATA:
            //    return dataType.attributes.length() > 2 ? "angular-velocity" : String.format(Locale.US, "angular-velocity[%d]", (dataType.attributes.offset >> 1));
            //case PACKED_DATA:
            //    return "angular-velocity";
            default:
                return null;
        }
    }

    //private static final byte PACKED_ROT_REVISION= 1;
    private final static byte POWER_MODE = 1, DATA_INTERRUPT_ENABLE = 2, CONFIG = 3; //, DATA = 5, PACKED_DATA= 0x7;
    //private final static String ROT_PRODUCER= "com.mbientlab.metawear.impl.GyroImpl.ROT_PRODUCER",
            //ROT_X_AXIS_PRODUCER= "com.mbientlab.metawear.impl.GyroImpl.ROT_X_AXIS_PRODUCER",
            //ROT_Y_AXIS_PRODUCER= "com.mbientlab.metawear.impl.GyroImpl.ROT_Y_AXIS_PRODUCER",
            //ROT_Z_AXIS_PRODUCER= "com.mbientlab.metawear.impl.GyroImpl.ROT_Z_AXIS_PRODUCER";//,
            //ROT_PACKED_PRODUCER= "com.mbientlab.metawear.impl.GyroImpl.ROT_PACKED_PRODUCER";
    private static final long serialVersionUID = 4400485740135675260L;

    /*private static class BoschGyrCartesianFloatData extends FloatVectorData {
        private static final long serialVersionUID = -3634187442404058486L;

        BoschGyrCartesianFloatData() {
            this(DATA, (byte) 1);
        }

        BoschGyrCartesianFloatData(byte register, byte copies) {
            super(GYRO, register, new DataAttributes(new byte[] {2, 2, 2}, copies, (byte) 0, true));
        }

        BoschGyrCartesianFloatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschGyrCartesianFloatData(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase[] createSplits() {
            return new DataTypeBase[] {new BoschGyrSFloatData((byte) 0), new BoschGyrSFloatData((byte) 2), new BoschGyrSFloatData((byte) 4)};
        }

        @Override
        protected float scale(MetaWearBoardPrivate mwPrivate) {
            return ((GyroImpl) mwPrivate.getModules().get(Gyro.class)).getGyrDataScale();
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, final Calendar timestamp, DataPrivate.ClassToObject mapper) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            short[] unscaled = new short[]{buffer.getShort(), buffer.getShort(), buffer.getShort()};
            final float scale= scale(mwPrivate);
            final AngularVelocity value= new AngularVelocity(unscaled[0] / scale, unscaled[1] / scale, unscaled[2] / scale);

            return new DataPrivate(timestamp, data, mapper) {
                @Override
                public float scale() {
                    return scale;
                }

                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {AngularVelocity.class, float[].class};
                }

                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz.equals(AngularVelocity.class)) {
                        return clazz.cast(value);
                    } else if (clazz.equals(float[].class)) {
                        return clazz.cast(new float[] {value.x(), value.y(), value.z()});
                    }
                    return super.value(clazz);
                }
            };
        }
    }
    private static class BoschGyrSFloatData extends SFloatData {
        private static final long serialVersionUID = -39028787047513082L;

        BoschGyrSFloatData(byte offset) {
            super(GYRO, DATA, new DataAttributes(new byte[] {2}, (byte) 1, offset, true));
        }

        BoschGyrSFloatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        protected float scale(MetaWearBoardPrivate mwPrivate) {
            return ((GyroImpl) mwPrivate.getModules().get(Gyro.class)).getGyrDataScale();
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschGyrSFloatData(input, module, register, id, attributes);
        }
    }*/

    ///< ACC_CONF, ACC_RANGE
    private final byte[] gyrDataConfig= new byte[] {(byte) (0x20 | OutputDataRate.ODR_100_HZ.bitmask), Range.FSR_2000.bitmask};
    //private transient AsyncDataProducer rotationalSpeed, packedRotationalSpeed;
    private transient TimedTask<byte[]> pullConfigTask;

    GyroImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        //DataTypeBase dataType = new BoschGyrCartesianFloatData();
        //mwPrivate.tagProducer(ROT_PRODUCER, dataType);
        //mwPrivate.tagProducer(ROT_X_AXIS_PRODUCER, dataType.split[0]);
        //mwPrivate.tagProducer(ROT_Y_AXIS_PRODUCER, dataType.split[1]);
        //mwPrivate.tagProducer(ROT_Z_AXIS_PRODUCER, dataType.split[2]);
        //mwPrivate.tagProducer(ROT_PACKED_PRODUCER, new BoschGyrCartesianFloatData(PACKED_DATA, (byte) 3));
    }

    @Override
    protected void init() {
        pullConfigTask = new TimedTask<>();

        mwPrivate.addResponseHandler(new Pair<>(GYRO.id, Util.setRead(CONFIG)), response -> pullConfigTask.setResult(response));
    }

    public float getGyrDataScale() {
        return Range.bitMaskToRange((byte) (gyrDataConfig[1] & 0x07)).scale;
    }

    @Override
    public ConfigEditor configure() {
        return new ConfigEditor() {
            private Range newRange= null;
            private OutputDataRate newOdr= null;
            private FilterMode mode = null;

            @Override
            public ConfigEditor range(Range range) {
                newRange= range;
                return this;
            }

            @Override
            public ConfigEditor odr(OutputDataRate odr) {
                newOdr= odr;
                return this;
            }

            @Override
            public ConfigEditor filter(FilterMode mode) {
                this.mode = mode;
                return this;
            }

            @Override
            public void commit() {
                if (newRange != null) {
                    gyrDataConfig[1] &= 0xf8;
                    gyrDataConfig[1] |= newRange.bitmask;
                }

                if (newOdr != null) {
                    gyrDataConfig[0] &= 0xf0;
                    gyrDataConfig[0] |= newOdr.bitmask;
                }

                if (mode != null) {
                    gyrDataConfig[0] &= 0xcf;
                    gyrDataConfig[0] |= (mode.ordinal() << 4);
                }

                mwPrivate.sendCommand(GYRO, CONFIG, gyrDataConfig);
            }
        };
    }

    @Override
    public Task<Void> pullConfigAsync() {
        return pullConfigTask.execute("Did not receive gyro config within %dms", Constant.RESPONSE_TIMEOUT,
                () -> mwPrivate.sendCommand(new byte[] {GYRO.id, Util.setRead(CONFIG)})
        ).onSuccessTask(task -> {
            System.arraycopy(task.getResult(), 2, gyrDataConfig, 0, gyrDataConfig.length);
            return Task.forResult(null);
        });
    }

    /*@Override
    public AngularVelocityDataProducer angularVelocity() {
        if (rotationalSpeed == null) {
            rotationalSpeed = new AngularVelocityDataProducer() {
                @Override
                public String xAxisName() {
                    return ROT_X_AXIS_PRODUCER;
                }

                @Override
                public String yAxisName() {
                    return ROT_Y_AXIS_PRODUCER;
                }

                @Override
                public String zAxisName() {
                    return ROT_Z_AXIS_PRODUCER;
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, ROT_PRODUCER);
                }

                @Override
                public String name() {
                    return ROT_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {GYRO.id, DATA_INTERRUPT_ENABLE, 1, 0});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {GYRO.id, DATA_INTERRUPT_ENABLE, 0, 1});
                }
            };
        }
        return (AngularVelocityDataProducer) rotationalSpeed;
    }*/

    /*@Override
    public AsyncDataProducer packedAngularVelocity() {
        if (mwPrivate.lookupModuleInfo(GYRO).revision >= PACKED_ROT_REVISION) {
            if (packedRotationalSpeed == null) {
                packedRotationalSpeed = new AsyncDataProducer() {
                    @Override
                    public Task<Route> addRouteAsync(RouteBuilder builder) {
                        return mwPrivate.queueRouteBuilder(builder, ROT_PACKED_PRODUCER);
                    }

                    @Override
                    public String name() {
                        return ROT_PACKED_PRODUCER;
                    }

                    @Override
                    public void start() {
                        mwPrivate.sendCommand(new byte[]{GYRO.id, DATA_INTERRUPT_ENABLE, 1, 0});
                    }

                    @Override
                    public void stop() {
                        mwPrivate.sendCommand(new byte[]{GYRO.id, DATA_INTERRUPT_ENABLE, 0, 1});
                    }
                };
            }
            return packedRotationalSpeed;
        }
        return null;
    }*/

    @Override
    public void start() {
        mwPrivate.sendCommand(new byte[] {GYRO.id, POWER_MODE, 1});
    }

    @Override
    public void stop() {
        mwPrivate.sendCommand(new byte[] {GYRO.id, POWER_MODE, 0});
    }


}