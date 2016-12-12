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
import com.mbientlab.metawear.data.SensorOrientation;
import com.mbientlab.metawear.module.AccelerometerMma8452q;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

import bolts.Task;

import static com.mbientlab.metawear.impl.ModuleId.ACCELEROMETER;

/**
 * Created by etsai on 9/1/16.
 */
class AccelerometerMma8452qImpl extends ModuleImplBase implements AccelerometerMma8452q {
    private static final long serialVersionUID = 8144499912703592184L;
    final static byte IMPLEMENTATION= 0x0;
    private final float MMA8452Q_G_PER_STEP= 0.063f;
    private static final byte PACKED_ACC_REVISION= 1;
    private static final byte GLOBAL_ENABLE = 1,
            DATA_ENABLE = 2, DATA_CONFIG = 3, DATA_VALUE = 4,
            MOVEMENT_ENABLE = 5, MOVEMENT_CONFIG = 6, MOVEMENT_VALUE = 7,
            ORIENTATION_ENABLE = 8, ORIENTATION_CONFIG = 9, ORIENTATION_VALUE = 0xa,
            PULSE_ENABLE = 0xb, PULSE_CONFIG = 0xc, PULSE_STATUS = 0xd,
            SHAKE_ENABLE = 0xe, SHAKE_CONFIG = 0xf, SHAKE_STATUS = 0x10,
            PACKED_ACC_DATA= 0x12;

    private static final float[][] motionCountSteps= new float[][] {
            {1.25f, 2.5f, 5, 10, 20, 20, 20, 20},
            {1.25f, 2.5f, 5, 10, 20, 80, 80, 80},
            {1.25f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f},
            {1.25f, 2.5f, 5, 10, 20, 80, 160, 160}
    }, transientSteps, orientationSteps;
    private static final float[][][] pulseTmltSteps= new float[][][] {
            {{0.625f, 0.625f, 1.25f, 2.5f, 5, 5, 5, 5},
                    {0.625f, 0.625f, 1.25f, 2.5f, 5, 20, 20, 20},
                    {0.625f, 0.625f, 0.625f, 0.625f, 0.625f, 0.625f, 0.625f, 0.625f},
                    {0.625f, 1.25f, 2.5f, 5, 10, 40, 40, 40}},
            {{1.25f, 2.5f, 5, 10, 20, 20, 20, 20},
                    {1.25f, 2.5f, 5, 10, 20, 80, 80, 80},
                    {1.25f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f},
                    {1.25f, 2.5f, 5, 10, 20, 80, 160, 160}}
    }, pulseLtcySteps, pulseWindSteps;
    private static final float[] sleepCountSteps= new float[] {320, 320, 320, 320, 320, 320, 320, 640};

    static {
        pulseLtcySteps= new float[2][4][8];
        for(int i= 0; i < pulseTmltSteps.length; i++) {
            for(int j= 0; j < pulseTmltSteps[i].length; j++) {
                for(int k= 0; k < pulseTmltSteps[i][j].length; k++) {
                    pulseLtcySteps[i][j][k]= pulseTmltSteps[i][j][k] * 2.f;
                }
            }
        }
        pulseWindSteps= pulseLtcySteps;
        transientSteps= motionCountSteps;
        orientationSteps= motionCountSteps;
    }

    private static class Mma8452QCartesianFloatData extends FloatVectorData {
        private static final long serialVersionUID = 8580438661319009866L;

        Mma8452QCartesianFloatData() {
            this(DATA_VALUE, (byte) 2);
        }

        Mma8452QCartesianFloatData(byte register, byte copies) {
            super(ACCELEROMETER, register, new DataAttributes(new byte[] {2, 2, 2}, copies, (byte) 0, true));
        }
        Mma8452QCartesianFloatData(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        protected float scale(MetaWearBoardPrivate owner) {
            return 1000.f;
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QCartesianFloatData(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase[] createSplits() {
            return new DataTypeBase[] {new Mma8452QSFloatData((byte) 0), new Mma8452QSFloatData((byte) 2), new Mma8452QSFloatData((byte) 4)};
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate owner, final byte[] data, final Calendar timestamp) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            short[] unscaled = new short[]{buffer.getShort(), buffer.getShort(), buffer.getShort()};
            float scale= scale(owner);
            final Acceleration value= new Acceleration(unscaled[0] / scale, unscaled[1] / scale, unscaled[2] / scale);

            return new DataPrivate(timestamp, data) {
                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {Acceleration.class};
                }

                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz == Acceleration.class) {
                        return clazz.cast(value);
                    }
                    return super.value(clazz);
                }
            };
        }
    }
    private static class Mma8452QSFloatData extends SFloatData {
        private static final long serialVersionUID = -8399682704460340788L;

        public Mma8452QSFloatData(byte offset) {
            super(ACCELEROMETER, DATA_VALUE, new DataAttributes(new byte[] {2}, (byte) 1, offset, true));
        }

        public Mma8452QSFloatData(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        protected float scale(MetaWearBoardPrivate owner) {
            return 1000.f;
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QSFloatData(input, module, register, id, attributes);
        }
    }
    private static class Mma8452QOrientationData extends DataTypeBase {
        private static final long serialVersionUID = 3678636934878581736L;

        Mma8452QOrientationData() {
            super(ACCELEROMETER, ORIENTATION_VALUE, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true));
        }

        Mma8452QOrientationData(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QOrientationData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate owner, Number input) {
            return input;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate board, byte[] data, Calendar timestamp) {
            final SensorOrientation orientation = SensorOrientation.values()[ 4 * (data[0] & 0x1) + ((data[0] >> 1) & 0x3)];

            return new DataPrivate(timestamp, data) {
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
    private static class Mma8452QShakeData extends DataTypeBase {
        private static final long serialVersionUID = -1189504439231338252L;

        Mma8452QShakeData() {
            super(ACCELEROMETER, SHAKE_STATUS, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true));
        }

        Mma8452QShakeData(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QShakeData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate owner, Number input) {
            return input;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate board, final byte[] data, Calendar timestamp) {
            final Shake castedData = new Shake() {
                @Override
                public boolean exceedsThreshold(CartesianAxis axis) {
                    byte mask= (byte) (2 << (2 * axis.ordinal()));
                    return (data[0] & mask) == mask;
                }

                @Override
                public Polarity polarity(CartesianAxis axis) {
                    byte mask= (byte) (1 << (2 * axis.ordinal()));
                    return (data[0] & mask) == mask ? Polarity.NEGATIVE : Polarity.POSITIVE;
                }

                @Override
                public String toString() {
                    boolean first= true;
                    StringBuilder builder= new StringBuilder();

                    builder.append("{");
                    for(CartesianAxis it: CartesianAxis.values()) {
                        builder.append(String.format("%s-Axis%s: {exceedsThreshold: %s, polarity: %s}",
                                (first ? "" : ", "), it.toString(), exceedsThreshold(it), polarity(it)));
                        first= false;
                    }
                    builder.append("}");
                    return builder.toString();
                }
            };

            return new DataPrivate(timestamp, data) {
                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz.equals(Shake.class)) {
                        return clazz.cast(castedData);
                    }
                    return super.value(clazz);
                }

                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {Shake.class};
                }
            };
        }
    }
    private static class Mma8452QTapData extends DataTypeBase {
        private static final long serialVersionUID = 1494924440373026139L;

        Mma8452QTapData() {
            super(ACCELEROMETER, PULSE_STATUS, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true));
        }

        Mma8452QTapData(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QTapData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate owner, Number input) {
            return input;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate board, final byte[] data, Calendar timestamp) {
            final Tap castedData = new Tap() {
                @Override
                public boolean active(CartesianAxis axis) {
                    byte mask= (byte) (0x10 << axis.ordinal());
                    return (data[0] & mask) == mask;
                }

                @Override
                public Polarity polarity(CartesianAxis axis) {
                    return Polarity.values()[(data[0] >> axis.ordinal()) & 0x1];
                }

                @Override
                public TapType type() {
                    return (data[0] & 0x8) == 0x8 ? TapType.DOUBLE : TapType.SINGLE;
                }

                @Override
                public String toString() {
                    boolean first= true;
                    StringBuilder builder= new StringBuilder();

                    builder.append("{");
                    for(CartesianAxis it: CartesianAxis.values()) {
                        builder.append(String.format("%s-Axis%s: {active: %s, polarity: %s, type: %s}",
                                (first ? "" : ", "), it.toString(), active(it), polarity(it), type()));
                        first= false;
                    }
                    builder.append("}");
                    return builder.toString();
                }
            };

            return new DataPrivate(timestamp, data) {
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
    private static class Mma8452QMovementData extends DataTypeBase {
        private static final long serialVersionUID = 6933107216144068304L;

        Mma8452QMovementData() {
            super(ACCELEROMETER, MOVEMENT_VALUE, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true));
        }

        Mma8452QMovementData(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QMovementData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate owner, Number input) {
            return input;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate board, final byte[] data, Calendar timestamp) {
            final Movement castedData = new Movement() {
                @Override
                public boolean crossedThreshold(CartesianAxis axis) {
                    byte mask= (byte) (2 << (2 * axis.ordinal()));
                    return (data[0] & mask) == mask;
                }

                @Override
                public Polarity polarity(CartesianAxis axis) {
                    byte mask= (byte) (1 << (2 * axis.ordinal()));
                    return (data[0] & mask) == mask ? Polarity.NEGATIVE : Polarity.POSITIVE;
                }

                @Override
                public String toString() {
                    boolean first= true;
                    StringBuilder builder= new StringBuilder();

                    builder.append("{");
                    for(CartesianAxis it: CartesianAxis.values()) {
                        builder.append(String.format("%s-Axis %s:{crossedThreshold: %s, polarity: %s}",
                                (first ? "" : ", "), it.toString(), crossedThreshold(it), polarity(it)));
                        first= false;
                    }
                    builder.append("}");
                    return builder.toString();
                }
            };

            return new DataPrivate(timestamp, data) {
                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz.equals(Movement.class)) {
                        return clazz.cast(castedData);
                    }
                    return super.value(clazz);
                }

                @Override
                public Class<?>[] types() {
                    return new Class<?>[] {Movement.class};
                }
            };
        }
    }

    private final static String ACCEL_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerMma8452qImpl.ACCEL_PRODUCER",
            ACCEL_X_AXIS_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerMma8452qImpl.ACCEL_X_AXIS_PRODUCER",
            ACCEL_Y_AXIS_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerMma8452qImpl.ACCEL_Y_AXIS_PRODUCER",
            ACCEL_Z_AXIS_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerMma8452qImpl.ACCEL_Z_AXIS_PRODUCER",
            ACCEL_PACKED_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerMma8452qImpl.ACCEL_PACKED_PRODUCER",
            ORIENTATION_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerMma8452qImpl.ORIENTATION_PRODUCER",
            SHAKE_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerMma8452qImpl.SHAKE_PRODUCER",
            TAP_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerMma8452qImpl.TAP_PRODUCER",
            MOVEMENT_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerMma8452qImpl.MOVEMENT_PRODUCER";

    private final byte[] dataSettings= new byte[] {0x00, 0x00, (byte) 0x18, 0x00, 0x00};

    private transient AsyncDataProducer packedAcceleration, acceleration, orientation, shake, tap, movement;
    private transient AutoSleep autosleep;

    AccelerometerMma8452qImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        DataTypeBase dataType = new Mma8452QCartesianFloatData();

        this.mwPrivate= mwPrivate;
        this.mwPrivate.tagProducer(ACCEL_PRODUCER, dataType);
        this.mwPrivate.tagProducer(ACCEL_X_AXIS_PRODUCER, dataType.split[0]);
        this.mwPrivate.tagProducer(ACCEL_Y_AXIS_PRODUCER, dataType.split[1]);
        this.mwPrivate.tagProducer(ACCEL_Z_AXIS_PRODUCER, dataType.split[2]);
        this.mwPrivate.tagProducer(ORIENTATION_PRODUCER, new Mma8452QOrientationData());
        this.mwPrivate.tagProducer(SHAKE_PRODUCER, new Mma8452QShakeData());
        this.mwPrivate.tagProducer(TAP_PRODUCER, new Mma8452QTapData());
        this.mwPrivate.tagProducer(MOVEMENT_PRODUCER, new Mma8452QMovementData());
        this.mwPrivate.tagProducer(ACCEL_PACKED_PRODUCER, new Mma8452QCartesianFloatData(PACKED_ACC_DATA, (byte) 3));
    }

    private int pwMode() {
        return (dataSettings[3] >> 3) & 0x3;
    }

    private int odr() {
        return (dataSettings[2] & ~0xc7) >> 3;
    }

    @Override
    public Mma8452qConfigEditor configure() {
        return new Mma8452qConfigEditor() {
            private OutputDataRate odr = OutputDataRate.ODR_100_HZ;
            private FullScaleRange fsr = FullScaleRange.FSR_2G;

            @Override
            public Mma8452qConfigEditor odr(OutputDataRate odr) {
                this.odr = odr;
                return this;
            }

            @Override
            public Mma8452qConfigEditor range(FullScaleRange fsr) {
                this.fsr = fsr;
                return this;
            }

            @Override
            public Mma8452qConfigEditor odr(float odr) {
                float[] frequencies = OutputDataRate.frequencies();
                int pos = Util.closestIndex(frequencies, odr);

                return odr(OutputDataRate.values()[pos]);
            }

            @Override
            public Mma8452qConfigEditor range(float fsr) {
                float[] ranges = FullScaleRange.ranges();
                int pos = Util.closestIndex(ranges, fsr);

                return range(FullScaleRange.values()[pos]);
            }

            @Override
            public void commit() {
                dataSettings[2] &= 0xc7;
                dataSettings[2] |= odr.ordinal() << 3;

                dataSettings[0] &= 0xfc;
                dataSettings[0] |= fsr.ordinal();

                mwPrivate.sendCommand(ACCELEROMETER, DATA_CONFIG, dataSettings);
            }
        };
    }

    @Override
    public float getOdr() {
        return OutputDataRate.values()[odr()].frequency;
    }

    @Override
    public float getRange() {
        return FullScaleRange.values()[dataSettings[0] & ~0xfc].range;
    }

    @Override
    public AccelerationDataProducer acceleration() {
        if (acceleration == null) {
            acceleration = new AccelerationDataProducer() {
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

                @Override
                public Task<Route> addRoute(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, ACCEL_PRODUCER);
                }

                @Override
                public String name() {
                    return ACCEL_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[]{ACCELEROMETER.id, DATA_ENABLE, 0x1});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[]{ACCELEROMETER.id, DATA_ENABLE, 0x0});
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
                    public Task<Route> addRoute(RouteBuilder builder) {
                        return mwPrivate.queueRouteBuilder(builder, ACCEL_PACKED_PRODUCER);
                    }

                    @Override
                    public String name() {
                        return ACCEL_PACKED_PRODUCER;
                    }

                    @Override
                    public void start() {
                        mwPrivate.sendCommand(new byte[]{ACCELEROMETER.id, DATA_ENABLE, 0x1});
                    }

                    @Override
                    public void stop() {
                        mwPrivate.sendCommand(new byte[]{ACCELEROMETER.id, DATA_ENABLE, 0x0});
                    }
                };
            }
            return packedAcceleration;
        }
        return null;
    }

    @Override
    public OrientationDataProducer orientationDetection() {
        if (orientation == null) {
            orientation = new OrientationDataProducer() {
                @Override
                public ConfigEditor configure() {
                    return new ConfigEditor() {
                        private int delay = 150;

                        @Override
                        public ConfigEditor delay(int delay) {
                            this.delay = delay;
                            return this;
                        }

                        @Override
                        public void commit() {
                            byte[] orientationSettings = new byte[] {0x00, (byte) 0x80, (byte) (delay / orientationSteps[pwMode()][odr()]), 0x44, (byte) 0x84};
                            mwPrivate.sendCommand(ACCELEROMETER, ORIENTATION_CONFIG, orientationSettings);
                        }
                    };
                }

                @Override
                public Task<Route> addRoute(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, ORIENTATION_PRODUCER);
                }

                @Override
                public String name() {
                    return ORIENTATION_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, ORIENTATION_ENABLE, 0x1});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, ORIENTATION_ENABLE, 0x0});
                }
            };
        }
        return (OrientationDataProducer) orientation;
    }

    @Override
    public ShakeDataProducer shakeDetection() {
        if (shake == null) {
            shake = new ShakeDataProducer() {
                @Override
                public ConfigEditor configure() {
                    return new ConfigEditor() {
                        private CartesianAxis newAxis = CartesianAxis.X;
                        private float threshold = 0.5f;
                        private int duration = 50;

                        @Override
                        public ConfigEditor axis(CartesianAxis axis) {
                            newAxis = axis;
                            return this;
                        }

                        @Override
                        public ConfigEditor threshold(float threshold) {
                            this.threshold = threshold;
                            return this;
                        }

                        @Override
                        public ConfigEditor duration(int duration) {
                            this.duration = duration;
                            return this;
                        }

                        @Override
                        public void commit() {
                            byte[] shakeSettings = new byte[] {
                                    (byte) ((2 << newAxis.ordinal()) | 0x10),
                                    0x00,
                                    (byte) (threshold / MMA8452Q_G_PER_STEP),
                                    (byte) (duration / transientSteps[pwMode()][odr()])
                            };
                            mwPrivate.sendCommand(ACCELEROMETER, SHAKE_CONFIG, shakeSettings);
                        }
                    };
                }

                @Override
                public Task<Route> addRoute(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, SHAKE_PRODUCER);
                }

                @Override
                public String name() {
                    return SHAKE_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, SHAKE_ENABLE, 0x1});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, SHAKE_ENABLE, 0x0});
                }
            };
        }
        return (ShakeDataProducer) shake;
    }

    @Override
    public TapDataProducer tapDetection() {
        if (tap == null) {
            tap = new TapDataProducer() {
                @Override
                public ConfigEditor configure() {
                    return new ConfigEditor() {
                        private int latency = 200, window = 300, duration = 60;
                        private TapType[] types;
                        private float threshold = 2f;
                        private boolean lpfEnable = false;
                        private CartesianAxis axis = CartesianAxis.Z;

                        @Override
                        public ConfigEditor latency(int latency) {
                            this.latency = latency;
                            return this;
                        }

                        @Override
                        public ConfigEditor window(int window) {
                            this.window = window;
                            return this;
                        }

                        @Override
                        public ConfigEditor lowPassFilter(boolean enable) {
                            this.lpfEnable = enable;
                            return this;
                        }

                        @Override
                        public ConfigEditor axis(CartesianAxis axis) {
                            this.axis = axis;
                            return this;
                        }

                        @Override
                        public ConfigEditor type(TapType... types) {
                            this.types = types;
                            return this;
                        }

                        @Override
                        public ConfigEditor threshold(float threshold) {
                            this.threshold = threshold;
                            return this;
                        }

                        @Override
                        public ConfigEditor duration(int duration) {
                            this.duration = duration;
                            return this;
                        }

                        @Override
                        public void commit() {
                            byte[] pulseSettings = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

                            int pwMode = pwMode(), odr = odr();
                            int lpfEn;
                            if (lpfEnable) {
                                dataSettings[1] |= 0x10;
                                lpfEn = 1;
                            } else {
                                dataSettings[1] &= ~0x10;
                                lpfEn = 0;
                            }

                            pulseSettings[0] |= 0x40;
                            for(TapType it: types) {
                                switch(it) {
                                    case SINGLE:
                                        pulseSettings[0] |= 1 << (2 * axis.ordinal());
                                        break;
                                    case DOUBLE:
                                        pulseSettings[0] |= 1 << (1 + 2 * axis.ordinal());
                                        break;
                                }
                            }

                            byte nSteps = (byte) (threshold / MMA8452Q_G_PER_STEP);
                            pulseSettings[2] |= nSteps;
                            pulseSettings[3] |= nSteps;
                            pulseSettings[4] |= nSteps;
                            pulseSettings[5]= (byte) (duration / pulseTmltSteps[lpfEn][pwMode][odr]);
                            pulseSettings[6]= (byte) (latency / pulseLtcySteps[lpfEn][pwMode][odr]);
                            pulseSettings[7]= (byte) (window / pulseWindSteps[lpfEn][pwMode][odr]);

                            mwPrivate.sendCommand(ACCELEROMETER, PULSE_CONFIG, pulseSettings);
                        }
                    };
                }

                @Override
                public Task<Route> addRoute(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, TAP_PRODUCER);
                }

                @Override
                public String name() {
                    return TAP_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, PULSE_ENABLE, 0x1});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, PULSE_ENABLE, 0x0});
                }
            };
        }
        return (TapDataProducer) tap;
    }

    @Override
    public MovementDataProducer movementDetection() {
        if (movement == null) {
            movement = new MovementDataProducer() {
                @Override
                public ConfigEditor configure(final MovementType type) {
                    return new ConfigEditor() {
                        private float threshold = (type == MovementType.FREE_FALL ? 0.5f : 1.5f);
                        private int duration= 100;
                        private CartesianAxis[] axes= CartesianAxis.values();

                        @Override
                        public ConfigEditor axes(CartesianAxis... axes) {
                            this.axes = axes;
                            return this;
                        }

                        @Override
                        public ConfigEditor threshold(float threshold) {
                            this.threshold = threshold;
                            return this;
                        }

                        @Override
                        public ConfigEditor duration(int duration) {
                            this.duration = duration;
                            return this;
                        }

                        @Override
                        public void commit() {
                            byte[] motionSettings = new byte[] { 0x00, 0x00, 0x00, 0x00 };
                            if (type == MovementType.MOTION) {
                                motionSettings[0] |= 0x40;
                            }

                            byte mask= 0;
                            for(CartesianAxis it: axes) {
                                mask |= 1 << (it.ordinal() + 3);
                            }
                            motionSettings[0] |= mask;
                            motionSettings[2]= (byte) (threshold / MMA8452Q_G_PER_STEP);
                            motionSettings[3]= (byte)(duration / transientSteps[pwMode()][odr()]);

                            mwPrivate.sendCommand(ACCELEROMETER, MOVEMENT_CONFIG, motionSettings);
                        }
                    };
                }

                @Override
                public Task<Route> addRoute(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, MOVEMENT_PRODUCER);
                }

                @Override
                public String name() {
                    return MOVEMENT_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOVEMENT_ENABLE, 0x1});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOVEMENT_ENABLE, 0x1});
                }
            };
        }
        return (MovementDataProducer) movement;
    }

    @Override
    public AutoSleep autosleep() {
        if (autosleep == null) {
            autosleep = new AutoSleep() {
                @Override
                public ConfigEditor configure() {
                    return new ConfigEditor() {
                        private SleepModeRate sleepRate = SleepModeRate.SMR_50_HZ;
                        private int timeout = 0;
                        private PowerMode powerMode = PowerMode.NORMAL;

                        @Override
                        public ConfigEditor dataRate(SleepModeRate rate) {
                            sleepRate = rate;
                            return this;
                        }

                        @Override
                        public ConfigEditor timeout(int timeout) {
                            this.timeout = timeout;
                            return this;
                        }

                        @Override
                        public ConfigEditor powerMode(PowerMode powerMode) {
                            this.powerMode = powerMode;
                            return this;
                        }

                        @Override
                        public void commit() {
                            dataSettings[2] |= sleepRate.ordinal() << 6;
                            dataSettings[3] &= ~(0x3 << 3);
                            dataSettings[3] |= (powerMode.ordinal() << 3);
                            dataSettings[4]= (byte)(timeout / sleepCountSteps[odr()]);
                        }
                    };
                }

                @Override
                public void enable() {
                    dataSettings[3] |= 0x4;
                }

                @Override
                public void disable() {
                    dataSettings[3] &= ~0x4;
                }
            };
        }
        return autosleep;
    }

    @Override
    public void start() {
        mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, GLOBAL_ENABLE, 0x1});
    }

    @Override
    public void stop() {
        mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, GLOBAL_ENABLE, 0x0});
    }
}
