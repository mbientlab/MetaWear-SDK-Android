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
import com.mbientlab.metawear.data.CartesianAxis;
import com.mbientlab.metawear.data.Sign;
import com.mbientlab.metawear.data.SensorOrientation;
import com.mbientlab.metawear.data.TapType;
import com.mbientlab.metawear.module.AccelerometerBosch;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Locale;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.ACCELEROMETER;

/**
 * Created by etsai on 9/1/16.
 */
abstract class AccelerometerBoschImpl extends ModuleImplBase implements AccelerometerBosch {
    static String createUri(DataTypeBase dataType) {
        switch (dataType.eventConfig[1]) {
            case DATA_INTERRUPT:
                return dataType.attributes.length() > 2 ? "acceleration" : String.format(Locale.US, "acceleration[%d]", (dataType.attributes.offset >> 1));
            case ORIENT_INTERRUPT:
                return "orientation";
            case FLAT_INTERRUPT:
                return "bosch-flat";
            case LOW_HIGH_G_INTERRUPT:
                return "bosch-low-high";
            case MOTION_INTERRUPT:
                return "bosch-motion";
            case TAP_INTERRUPT:
                return "bosch-tap";
            case PACKED_ACC_DATA:
                return "acceleration";
            default:
                return null;
        }
    }

    private static final long serialVersionUID = -5265441447807910938L;

    private static final byte PACKED_ACC_REVISION = 0x1, FLAT_REVISION = 0x2;
    private final float LOW_THRESHOLD_STEP= 0.00781f, LOW_HYSTERESIS_STEP= 0.125f;
    private static final float[] BOSCH_HIGH_THRESHOLD_STEPS= {0.00781f, 0.01563f, 0.03125f, 0.0625f},
            BOSCH_HIGH_HYSTERESIS_STEPS= {0.125f, 0.250f, 0.5f, 1f},
            BOSCH_ANY_MOTION_THS_STEPS= {0.00391f, 0.00781f, 0.01563f, 0.03125f};
    protected static final float[] BOSCH_NO_MOTION_THS_STEPS= BOSCH_ANY_MOTION_THS_STEPS;
    private static final float[] BOSCH_TAP_THS_STEPS= {0.0625f, 0.125f, 0.250f, 0.5f};
    protected static final byte POWER_MODE = 1,
            DATA_INTERRUPT_ENABLE = 2, DATA_CONFIG = 3, DATA_INTERRUPT = 4, DATA_INTERRUPT_CONFIG = 5,
            ORIENT_INTERRUPT_ENABLE = 0xf, ORIENT_CONFIG = 0x10, ORIENT_INTERRUPT = 0x11,
            LOW_HIGH_G_INTERRUPT_ENABLE = 0x6, LOW_HIGH_G_CONFIG = 0x7, LOW_HIGH_G_INTERRUPT = 0x8,
            MOTION_INTERRUPT_ENABLE = 0x9, MOTION_CONFIG = 0xa, MOTION_INTERRUPT = 0xb,
            TAP_INTERRUPT_ENABLE = 0xc, TAP_CONFIG = 0xd, TAP_INTERRUPT = 0xe,
            FLAT_INTERRUPT_ENABLE = 0x12, FLAT_CONFIG = 0x13, FLAT_INTERRUPT = 0x14,
            PACKED_ACC_DATA= 0x1c;
    protected final static String ACCEL_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBoschImpl.ACCEL_PRODUCER",
            ACCEL_X_AXIS_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBoschImpl.ACCEL_X_AXIS_PRODUCER",
            ACCEL_Y_AXIS_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBoschImpl.ACCEL_Y_AXIS_PRODUCER",
            ACCEL_Z_AXIS_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBoschImpl.ACCEL_Z_AXIS_PRODUCER",
            ACCEL_PACKED_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBoschImpl.ACCEL_PACKED_PRODUCER",
            LOW_HIGH_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBoschImpl.LOW_HIGH_PRODUCER",
            ORIENTATION_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBoschImpl.ORIENTATION_PRODUCER",
            FLAT_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBoschImpl.FLAT_PRODUCER",
            MOTION_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBoschImpl.MOTION_PRODUCER",
            TAP_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBoschImpl.TAP_PRODUCER";
    private static final float ORIENT_HYS_G_PER_STEP= 0.0625f, THETA_STEP= (float) (44.8/63.f);

    private static class BoschAccCartesianFloatData extends FloatVectorData {
        private static final long serialVersionUID = -758164941443260674L;

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
            return ((AccelerometerBoschImpl) mwPrivate.getModules().get(AccelerometerBosch.class)).getAccDataScale();
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
        private static final long serialVersionUID = 7931910535343574126L;

        BoschAccSFloatData(byte offset) {
            super(ACCELEROMETER, DATA_INTERRUPT, new DataAttributes(new byte[] {2}, (byte) 1, offset, true));
        }

        BoschAccSFloatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        protected float scale(MetaWearBoardPrivate mwPrivate) {
            return ((AccelerometerBoschImpl) mwPrivate.getModules().get(AccelerometerBosch.class)).getAccDataScale();
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschAccSFloatData(input, module, register, id, attributes);
        }
    }
    private static class BoschFlatData extends DataTypeBase {
        private static final long serialVersionUID = 7754881668078978202L;

        BoschFlatData() {
            super(ACCELEROMETER, FLAT_INTERRUPT, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false));
        }

        BoschFlatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschFlatData(input, module, register, id, attributes);
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper) {
            int mask = mwPrivate.lookupModuleInfo(ACCELEROMETER).revision >= FLAT_REVISION ? 0x4 : 0x2;
            final boolean isFlat = (data[0] & mask) == mask;

            return new DataPrivate(timestamp, data, mapper) {
                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz.equals(Boolean.class)) {
                        return clazz.cast(isFlat);
                    }
                    return super.value(clazz);
                }

                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {Boolean.class};
                }
            };
        }
    }
    private static class BoschOrientationData extends DataTypeBase {
        private static final long serialVersionUID = -3067060296134186104L;

        BoschOrientationData() {
            super(ACCELEROMETER, ORIENT_INTERRUPT, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false));
        }

        BoschOrientationData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschOrientationData(input, module, register, id, attributes);
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper) {
            final SensorOrientation orientation = SensorOrientation.values()[((data[0] & 0x6) >> 1) + 4 * ((data[0] & 0x8) >> 3)];

            return new DataPrivate(timestamp, data, mapper) {
                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz.equals(SensorOrientation.class)) {
                        return clazz.cast(orientation);
                    }
                    return super.value(clazz);
                }

                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {SensorOrientation.class};
                }
            };
        }
    }
    private static class BoschLowHighData extends DataTypeBase {
        private static final long serialVersionUID = 2893724840326544116L;

        BoschLowHighData() {
            super(ACCELEROMETER, LOW_HIGH_G_INTERRUPT, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false));
        }

        BoschLowHighData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschLowHighData(input, module, register, id, attributes);
        }

        private boolean highG(CartesianAxis axis, byte value) {
            byte mask= (byte) (0x1 << axis.ordinal());
            return (value & mask) == mask;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper) {
            final byte highFirst = (byte) ((data[0] & 0x1c) >> 2);
            final LowHighResponse castedData = new LowHighResponse(
                    (data[0] & 0x1) == 0x1,
                    (data[0] & 0x2) == 0x2,
                    highG(CartesianAxis.X, highFirst),
                    highG(CartesianAxis.Y, highFirst),
                    highG(CartesianAxis.Z, highFirst),
                    (data[0] & 0x20) == 0x20 ? Sign.NEGATIVE : Sign.POSITIVE);

            return new DataPrivate(timestamp, data, mapper) {
                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz.equals(LowHighResponse.class)) {
                        return clazz.cast(castedData);
                    }
                    return super.value(clazz);
                }

                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {LowHighResponse.class};
                }
            };
        }
    }
    private static class BoschMotionData extends DataTypeBase {
        private static final long serialVersionUID = 5170523637959567052L;

        BoschMotionData() {
            super(ACCELEROMETER, MOTION_INTERRUPT, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false));
        }

        BoschMotionData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschMotionData(input, module, register, id, attributes);
        }

        private boolean detected(CartesianAxis axis, byte value) {
            byte mask= (byte) (0x1 << (axis.ordinal() + 3));
            return (value & mask) == mask;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper) {
            final AnyMotion castedData = new AnyMotion(
                    (data[0] & 0x40) == 0x40 ? Sign.NEGATIVE : Sign.POSITIVE,
                    detected(CartesianAxis.X, data[0]),
                    detected(CartesianAxis.Y, data[0]),
                    detected(CartesianAxis.Z, data[0])
            );

            return new DataPrivate(timestamp, data, mapper) {
                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz.equals(AnyMotion.class)) {
                        return clazz.cast(castedData);
                    }
                    return super.value(clazz);
                }

                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {AnyMotion.class};
                }
            };
        }
    }
    private static class BoschTapData extends DataTypeBase {
        private static final long serialVersionUID = 7541750644119353713L;

        BoschTapData() {
            super(ACCELEROMETER, TAP_INTERRUPT, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false));
        }

        BoschTapData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BoschTapData(input, module, register, id, attributes);
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper) {
            TapType type = null;
            if ((data[0] & 0x1) == 0x1) {
                type = TapType.DOUBLE;
            } else if ((data[0] & 0x2) == 0x2) {
                type = TapType.SINGLE;
            }

            final Tap castedData = new Tap(type, (data[0] & 0x20) == 0x20 ? Sign.NEGATIVE : Sign.POSITIVE);
            return new DataPrivate(timestamp, data, mapper) {
                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz.equals(Tap.class)) {
                        return clazz.cast(castedData);
                    }
                    return super.value(clazz);
                }

                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {Tap.class};
                }
            };
        }
    }

    abstract class BoschFlatDataProducer implements FlatDataProducer {
        @Override
        public Task<Route> addRouteAsync(RouteBuilder builder) {
            return mwPrivate.queueRouteBuilder(builder, FLAT_PRODUCER);
        }

        @Override
        public String name() {
            return FLAT_PRODUCER;
        }

        @Override
        public void start() {
            mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, FLAT_INTERRUPT_ENABLE, (byte) 1, (byte) 0});
        }

        @Override
        public void stop() {
            mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, FLAT_INTERRUPT_ENABLE, (byte) 0, (byte) 1});
        }
    }
    class LowHighDataProducerInner implements LowHighDataProducer {
        private final byte[] initialConfig;
        private final float durationStep;
        private byte lowHighEnableMask = 0;

        LowHighDataProducerInner(byte[] initialConfig, float durationStep) {
            this.initialConfig = initialConfig;
            this.durationStep = durationStep;
        }

        @Override
        public LowHighConfigEditor configure() {
            final byte[] lowHighConfig = Arrays.copyOf(initialConfig, initialConfig.length);
            lowHighEnableMask = 0;
            return new LowHighConfigEditor() {
                private LowGMode lowGMode= null;
                private Integer lowDuration= null, highDuration= null;
                private Float lowThreshold= null, lowHysteresis= null, newHighThreshold= null, newHighHysteresis= null;

                @Override
                public LowHighConfigEditor enableLowG() {
                    lowHighEnableMask |= 0x8;
                    return this;
                }

                @Override
                public LowHighConfigEditor enableHighGx() {
                    lowHighEnableMask |= 0x1;
                    return this;
                }

                @Override
                public LowHighConfigEditor enableHighGy() {
                    lowHighEnableMask |= 0x2;
                    return this;
                }

                @Override
                public LowHighConfigEditor enableHighGz() {
                    lowHighEnableMask |= 0x4;
                    return this;
                }

                @Override
                public LowHighConfigEditor lowDuration(int duration) {
                    lowDuration= duration;
                    return this;
                }

                @Override
                public LowHighConfigEditor lowThreshold(float threshold) {
                    lowThreshold= threshold;
                    return this;
                }

                @Override
                public LowHighConfigEditor lowHysteresis(float hysteresis) {
                    lowHysteresis= hysteresis;
                    return this;
                }

                @Override
                public LowHighConfigEditor lowGMode(LowGMode mode) {
                    lowGMode= mode;
                    return this;
                }

                @Override
                public LowHighConfigEditor highDuration(int duration) {
                    highDuration= duration;
                    return this;
                }

                @Override
                public LowHighConfigEditor highThreshold(float threshold) {
                    newHighThreshold= threshold;
                    return this;
                }

                @Override
                public LowHighConfigEditor highHysteresis(float hysteresis) {
                    newHighHysteresis= hysteresis;
                    return this;
                }

                @Override
                public void commit() {
                    if (lowDuration != null) {
                        lowHighConfig[0]= (byte) ((lowDuration / durationStep) - 1);
                    }
                    if (lowThreshold != null) {
                        lowHighConfig[1]= (byte) (lowThreshold / LOW_THRESHOLD_STEP);
                    }
                    if (newHighHysteresis != null) {
                        lowHighConfig[2]|= ((int) (newHighHysteresis / BOSCH_HIGH_HYSTERESIS_STEPS[getSelectedAccRange()]) & 0x3) << 6;
                    }
                    if (lowGMode != null) {
                        lowHighConfig[2]&= 0xfb;
                        lowHighConfig[2]|= (lowGMode.ordinal() << 2);
                    }
                    if (lowHysteresis != null) {
                        lowHighConfig[2]&= 0xfc;
                        lowHighConfig[2]|= ((byte) (lowHysteresis / LOW_HYSTERESIS_STEP) & 0x3);
                    }
                    if (highDuration != null) {
                        lowHighConfig[3]= (byte) ((highDuration / durationStep) - 1);
                    }
                    if (newHighThreshold != null) {
                        lowHighConfig[4]= (byte) (newHighThreshold / BOSCH_HIGH_THRESHOLD_STEPS[getSelectedAccRange()]);
                    }

                    mwPrivate.sendCommand(ACCELEROMETER, LOW_HIGH_G_CONFIG, lowHighConfig);
                }
            };
        }

        @Override
        public Task<Route> addRouteAsync(RouteBuilder builder) {
            return mwPrivate.queueRouteBuilder(builder, LOW_HIGH_PRODUCER);
        }

        @Override
        public String name() {
            return LOW_HIGH_PRODUCER;
        }

        @Override
        public void start() {
            mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, LOW_HIGH_G_INTERRUPT_ENABLE, lowHighEnableMask, (byte) 0x0});
        }

        @Override
        public void stop() {
            mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, LOW_HIGH_G_INTERRUPT_ENABLE, (byte) 0, 0xf});
        }
    }
    class AnyMotionConfigEditorInner implements AnyMotionConfigEditor {
        private Integer count= null;
        private Float threshold= null;
        private final byte[] motionConfig;

        AnyMotionConfigEditorInner(byte[] initialConfig) {
            motionConfig = initialConfig;
        }

        @Override
        public AnyMotionConfigEditor count(int count) {
            this.count= count;
            return this;
        }

        @Override
        public AnyMotionConfigEditor threshold(float threshold) {
            this.threshold= threshold;
            return this;
        }

        @Override
        public void commit() {
            if (count != null) {
                motionConfig[0]&= 0xfc;
                motionConfig[0]|= (count - 1) & 0x3;
            }

            if (threshold != null) {
                motionConfig[1]= (byte) (threshold / BOSCH_ANY_MOTION_THS_STEPS[getSelectedAccRange()]);
            }

            mwPrivate.sendCommand(ACCELEROMETER, MOTION_CONFIG, motionConfig);
        }
    }
    class SlowMotionConfigEditorInner implements SlowMotionConfigEditor {
        private Byte count= null;
        private Float threshold= null;
        private final byte[] motionConfig;

        SlowMotionConfigEditorInner(byte[] initialConfig) {
            motionConfig = initialConfig;
        }

        @Override
        public SlowMotionConfigEditor count(byte count) {
            this.count= count;
            return this;
        }

        @Override
        public SlowMotionConfigEditor threshold(float threshold) {
            this.threshold= threshold;
            return this;
        }

        @Override
        public void commit() {
            if (count != null) {
                motionConfig[0]&= 0x3;
                motionConfig[0]|= (count - 1) << 2;
            }
            if (threshold != null) {
                motionConfig[2]= (byte) (threshold / BOSCH_NO_MOTION_THS_STEPS[getSelectedAccRange()]);
            }

            mwPrivate.sendCommand(ACCELEROMETER, MOTION_CONFIG, motionConfig);
        }
    }

    private transient byte tapEnableMask;
    private transient AsyncDataProducer packedAcceleration, acceleration, orientation, tap;

    AccelerometerBoschImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        DataTypeBase cfProducer = new BoschAccCartesianFloatData();

        this.mwPrivate= mwPrivate;
        this.mwPrivate.tagProducer(ACCEL_PRODUCER, cfProducer);
        this.mwPrivate.tagProducer(ACCEL_X_AXIS_PRODUCER, cfProducer.split[0]);
        this.mwPrivate.tagProducer(ACCEL_Y_AXIS_PRODUCER, cfProducer.split[1]);
        this.mwPrivate.tagProducer(ACCEL_Z_AXIS_PRODUCER, cfProducer.split[2]);
        this.mwPrivate.tagProducer(FLAT_PRODUCER, new BoschFlatData());
        this.mwPrivate.tagProducer(ORIENTATION_PRODUCER, new BoschOrientationData());
        this.mwPrivate.tagProducer(LOW_HIGH_PRODUCER, new BoschLowHighData());
        this.mwPrivate.tagProducer(MOTION_PRODUCER, new BoschMotionData());
        this.mwPrivate.tagProducer(TAP_PRODUCER, new BoschTapData());
        this.mwPrivate.tagProducer(ACCEL_PACKED_PRODUCER, new BoschAccCartesianFloatData(PACKED_ACC_DATA, (byte) 3));
    }

    protected abstract float getAccDataScale();
    protected abstract int getSelectedAccRange();
    protected abstract int getMaxOrientHys();
    void writeFlatConfig(int holdTime, float theta) {
        byte[] flatConfig = new byte[] {0x08, 0x11};

        flatConfig[0]|= ((int) (theta / THETA_STEP) & 0x3f);
        flatConfig[1]|= (holdTime << 4);

        mwPrivate.sendCommand(ACCELEROMETER, FLAT_CONFIG, flatConfig);
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
    public AsyncDataProducer packedAcceleration() {
        if (mwPrivate.lookupModuleInfo(ACCELEROMETER).revision >= PACKED_ACC_REVISION) {
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
        return null;
    }

    @Override
    public void start() {
        mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, POWER_MODE, 0x01});
    }

    @Override
    public void stop() {
        mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, POWER_MODE, 0x00});
    }

    @Override
    public OrientationDataProducer orientation() {
        if (orientation == null) {
            orientation = new OrientationDataProducer() {
                @Override
                public OrientationConfigEditor configure() {
                    return new OrientationConfigEditor() {
                        private Float hysteresis = null;
                        private OrientationMode mode = OrientationMode.SYMMETRICAL;

                        @Override
                        public OrientationConfigEditor hysteresis(float hysteresis) {
                            this.hysteresis = hysteresis;
                            return this;
                        }

                        @Override
                        public OrientationConfigEditor mode(OrientationMode mode) {
                            this.mode = mode;
                            return this;
                        }

                        @Override
                        public void commit() {
                            byte[] orientationConfig = new byte[] {0x18, 0x48};

                            if (hysteresis != null) {
                                orientationConfig[0]|= (byte) Math.min(getMaxOrientHys(), (byte) (hysteresis / ORIENT_HYS_G_PER_STEP));
                            }

                            orientationConfig[0]&= 0xfc;
                            orientationConfig[0]|= mode.ordinal();

                            mwPrivate.sendCommand(ACCELEROMETER, ORIENT_CONFIG, orientationConfig);
                        }
                    };
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, ORIENTATION_PRODUCER);
                }

                @Override
                public String name() {
                    return ORIENTATION_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, ORIENT_INTERRUPT_ENABLE, (byte) 0x1, (byte) 0x0});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, ORIENT_INTERRUPT_ENABLE, (byte) 0x0, (byte) 0x1});
                }
            };
        }
        return (OrientationDataProducer) orientation;
    }

    @Override
    public TapDataProducer tap() {
        if (tap == null) {
            tap = new TapDataProducer() {
                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, TAP_PRODUCER);
                }

                @Override
                public String name() {
                    return TAP_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, TAP_INTERRUPT_ENABLE, tapEnableMask, (byte) 0});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, TAP_INTERRUPT_ENABLE, (byte) 0, (byte) 0x3});
                }

                @Override
                public TapConfigEditor configure() {
                    return new TapConfigEditor() {
                        private TapQuietTime newTapTime= null;
                        private TapShockTime newShockTime= null;
                        private DoubleTapWindow newWindow= null;
                        private Float newThs= null;
                        private final LinkedHashSet<TapType> types = new LinkedHashSet<>();

                        @Override
                        public TapConfigEditor enableDoubleTap() {
                            types.add(TapType.DOUBLE);
                            return this;
                        }

                        @Override
                        public TapConfigEditor enableSingleTap() {
                            types.add(TapType.SINGLE);
                            return this;
                        }

                        @Override
                        public TapConfigEditor quietTime(TapQuietTime time) {
                            newTapTime= time;
                            return this;
                        }

                        @Override
                        public TapConfigEditor shockTime(TapShockTime time) {
                            newShockTime= time;
                            return this;
                        }

                        @Override
                        public TapConfigEditor doubleTapWindow(DoubleTapWindow window) {
                            newWindow= window;
                            return this;
                        }

                        @Override
                        public TapConfigEditor threshold(float threshold) {
                            newThs= threshold;
                            return this;
                        }

                        @Override
                        public void commit() {
                            byte[] tapConfig = new byte[] {0x04, 0x0a};

                            if (newTapTime != null) {
                                tapConfig[0]|= newTapTime.ordinal() << 7;
                            }

                            if (newShockTime != null) {
                                tapConfig[0]|= newShockTime.ordinal() << 6;
                            }

                            if (newWindow != null) {
                                tapConfig[0]&= 0xf8;
                                tapConfig[0]|= newWindow.ordinal();
                            }

                            if (newThs != null) {
                                tapConfig[1]&= 0xe0;
                                tapConfig[1]|= (byte) Math.min(15, newThs / BOSCH_TAP_THS_STEPS[getSelectedAccRange()]);
                            }

                            tapEnableMask= 0;
                            if (types.isEmpty()) {
                                types.add(TapType.SINGLE);
                                types.add(TapType.DOUBLE);
                            }
                            for(TapType it: types) {
                                switch (it) {
                                    case SINGLE:
                                        tapEnableMask |= 0x2;
                                        break;
                                    case DOUBLE:
                                        tapEnableMask |= 0x1;
                                        break;
                                }
                            }

                            mwPrivate.sendCommand(ACCELEROMETER, TAP_CONFIG, tapConfig);
                        }
                    };
                }
            };
        }
        return (TapDataProducer) tap;
    }
}
