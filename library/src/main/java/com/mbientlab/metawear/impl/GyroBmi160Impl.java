package com.mbientlab.metawear.impl;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.GyroBmi160;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Locale;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.GYRO;

class GyroBmi160Impl extends GyroImpl implements GyroBmi160 {
    static final byte IMPLEMENTATION= 0;
    static String createUri(DataTypeBase dataType) {
        switch (Util.clearRead(dataType.eventConfig[1])) {
            case DATA:
                return dataType.attributes.length() > 2 ? "angular-velocity" : String.format(Locale.US, "angular-velocity[%d]", (dataType.attributes.offset >> 1));
            case PACKED_DATA:
                return "angular-velocity";
        }
        return GyroImpl.createUri(dataType);
    }

    private static final byte PACKED_ROT_REVISION= 1;
    private final static byte DATA_INTERRUPT_ENABLE = 2, DATA = 5, PACKED_DATA= 0x7;
    private final static String ROT_PRODUCER= "com.mbientlab.metawear.impl.GyroBmi160Impl.ROT_PRODUCER",
            ROT_X_AXIS_PRODUCER= "com.mbientlab.metawear.impl.GyroBmi160Impl.ROT_X_AXIS_PRODUCER",
            ROT_Y_AXIS_PRODUCER= "com.mbientlab.metawear.impl.GyroBmi160Impl.ROT_Y_AXIS_PRODUCER",
            ROT_Z_AXIS_PRODUCER= "com.mbientlab.metawear.impl.GyroBmi160Impl.ROT_Z_AXIS_PRODUCER",
            ROT_PACKED_PRODUCER= "com.mbientlab.metawear.impl.GyroBmi160Impl.ROT_PACKED_PRODUCER";
    private static final long serialVersionUID = 4400485740135675260L;

    private transient AsyncDataProducer rotationalSpeed, packedRotationalSpeed;

    private static class BoschGyrCartesianFloatData extends FloatVectorData {
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
            return new GyroBmi160Impl.BoschGyrCartesianFloatData(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase[] createSplits() {
            return new DataTypeBase[] {new GyroBmi160Impl.BoschGyrSFloatData((byte) 0), new GyroBmi160Impl.BoschGyrSFloatData((byte) 2), new GyroBmi160Impl.BoschGyrSFloatData((byte) 4)};
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
            return new GyroBmi160Impl.BoschGyrSFloatData(input, module, register, id, attributes);
        }
    }

    GyroBmi160Impl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        DataTypeBase dataType = new BoschGyrCartesianFloatData();
        mwPrivate.tagProducer(ROT_PRODUCER, dataType);
        mwPrivate.tagProducer(ROT_X_AXIS_PRODUCER, dataType.split[0]);
        mwPrivate.tagProducer(ROT_Y_AXIS_PRODUCER, dataType.split[1]);
        mwPrivate.tagProducer(ROT_Z_AXIS_PRODUCER, dataType.split[2]);
        mwPrivate.tagProducer(ROT_PACKED_PRODUCER, new BoschGyrCartesianFloatData(PACKED_DATA, (byte) 3));
    }

    @Override
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
    }

    @Override
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
    }
}
