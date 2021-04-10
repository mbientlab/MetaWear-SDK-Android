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
import com.mbientlab.metawear.impl.platform.TimedTask;
import com.mbientlab.metawear.module.AccelerometerMma8452q;

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
class AccelerometerMma8452qImpl extends ModuleImplBase implements AccelerometerMma8452q {
    static String createUri(DataTypeBase dataType) {
        switch (dataType.eventConfig[1]) {
            case DATA_VALUE:
                return dataType.attributes.length() > 2 ? "acceleration" : String.format(Locale.US, "acceleration[%d]", (dataType.attributes.offset >> 1));
            case ORIENTATION_VALUE:
                return "orientation";
            case SHAKE_STATUS:
                return "mma8452q-shake";
            case PULSE_STATUS:
                return "mma8452q-tap";
            case MOVEMENT_VALUE:
                return "mma8452q-movement";
            case PACKED_ACC_DATA:
                return "acceleration";
            default:
                return null;
        }
    }

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
    private static final float[][][] OS_CUTOFF_FREQS = new float[][][]{
            {
                    {16f, 8f, 4f, 2f},
                    {16f, 8f, 4f, 2f},
                    {8f, 4f, 2f, 1f},
                    {4f, 2f, 1f, 0.5f},
                    {2f, 1f, 0.5f, 0.25f},
                    {2f, 1f, 0.5f, 0.25f},
                    {2f, 1f, 0.5f, 0.25f},
                    {2f, 1f, 0.5f, 0.25f}
            },
            {
                    {16f, 8f, 4f, 2f},
                    {16f, 8f, 4f, 2f},
                    {8f, 4f, 2f, 1f},
                    {4f, 2f, 1f, 0.5f},
                    {2f, 1f, 0.5f, 0.25f},
                    {0.5f, 0.25f, 0.125f, 0.063f},
                    {0.5f, 0.25f, 0.125f, 0.063f},
                    {0.5f, 0.25f, 0.125f, 0.063f}
            },
            {
                    {16f, 8f, 4f, 2f},
                    {16f, 8f, 4f, 2f},
                    {16f, 8f, 4f, 2f},
                    {16f, 8f, 4f, 2f},
                    {16f, 8f, 4f, 2f},
                    {16f, 8f, 4f, 2f},
                    {16f, 8f, 4f, 2f},
                    {16f, 8f, 4f, 2f}
            },
            {
                    {16f, 8f, 4f, 2f},
                    {8f, 4f, 2f, 1f},
                    {4f, 2f, 1f, 0.5f},
                    {2f, 1f, 0.5f, 0.25f},
                    {1f, 0.5f, 0.25f, 0.125f},
                    {0.25f, 0.125f, 0.063f, 0.031f},
                    {0.25f, 0.125f, 0.063f, 0.031f},
                    {0.25f, 0.125f, 0.063f, 0.031f}
            }
    },pulseTmltSteps= new float[][][] {
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
            this(DATA_VALUE, (byte) 1);
        }

        Mma8452QCartesianFloatData(byte register, byte copies) {
            super(ACCELEROMETER, register, new DataAttributes(new byte[] {2, 2, 2}, copies, (byte) 0, true));
        }
        Mma8452QCartesianFloatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        protected float scale(MetaWearBoardPrivate mwPrivate) {
            return 1000.f;
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QCartesianFloatData(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase[] createSplits() {
            return new DataTypeBase[] {new Mma8452QSFloatData((byte) 0), new Mma8452QSFloatData((byte) 2), new Mma8452QSFloatData((byte) 4)};
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, final Calendar timestamp, DataPrivate.ClassToObject mapper) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            final float scale= scale(mwPrivate);
            final Acceleration value= new Acceleration(buffer.getShort() / scale, buffer.getShort() / scale, buffer.getShort() / scale);

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
    private static class Mma8452QSFloatData extends MilliUnitsSFloatData {
        private static final long serialVersionUID = -8399682704460340788L;

        Mma8452QSFloatData(byte offset) {
            super(ACCELEROMETER, DATA_VALUE, new DataAttributes(new byte[] {2}, (byte) 1, offset, true));
        }

        Mma8452QSFloatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QSFloatData(input, module, register, id, attributes);
        }
    }
    private static class Mma8452QOrientationData extends DataTypeBase {
        private static final long serialVersionUID = 3678636934878581736L;

        Mma8452QOrientationData() {
            super(ACCELEROMETER, ORIENTATION_VALUE, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true));
        }

        Mma8452QOrientationData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QOrientationData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate mwPrivate, Number value) {
            return value;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper) {
            int offset = (data[0] & 0x06) >> 1;
            int index = 4 * (data[0] & 0x01) + ((offset == 2 || offset == 3) ? offset ^ 0x1 : offset);
            final SensorOrientation orientation = SensorOrientation.values()[index];

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
    private static class Mma8452QShakeData extends DataTypeBase {
        private static final long serialVersionUID = -1189504439231338252L;

        Mma8452QShakeData() {
            super(ACCELEROMETER, SHAKE_STATUS, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true));
        }

        Mma8452QShakeData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QShakeData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate mwPrivate, Number value) {
            return value;
        }

        private boolean exceedsThreshold(CartesianAxis axis, byte value) {
            byte mask= (byte) (2 << (2 * axis.ordinal()));
            return (value & mask) == mask;
        }
        private Sign direction(CartesianAxis axis, byte value) {
            byte mask= (byte) (1 << (2 * axis.ordinal()));
            return (value & mask) == mask ? Sign.NEGATIVE : Sign.POSITIVE;
        }
        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper) {
            final Movement castedData = new Movement(
                    new boolean[] {exceedsThreshold(CartesianAxis.X, data[0]), exceedsThreshold(CartesianAxis.Y, data[0]), exceedsThreshold(CartesianAxis.Z, data[0])},
                    new Sign[] {direction(CartesianAxis.X, data[0]), direction(CartesianAxis.Y, data[0]), direction(CartesianAxis.Z, data[0])}
            );

            return new DataPrivate(timestamp, data, mapper) {
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
    private static class Mma8452QTapData extends DataTypeBase {
        private static final long serialVersionUID = 1494924440373026139L;

        Mma8452QTapData() {
            super(ACCELEROMETER, PULSE_STATUS, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true));
        }

        Mma8452QTapData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QTapData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate mwPrivate, Number value) {
            return value;
        }

        private boolean active(CartesianAxis axis, byte value) {
            byte mask= (byte) (0x10 << axis.ordinal());
            return (value & mask) == mask;
        }

        private Sign polarity(CartesianAxis axis, byte value) {
            return Sign.values()[(value >> axis.ordinal()) & 0x1];
        }

        private TapType type(byte value) {
            return (value & 0x8) == 0x8 ? TapType.DOUBLE : TapType.SINGLE;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper) {
            final Tap castedData = new Tap(
                    new boolean[] {active(CartesianAxis.X, data[0]), active(CartesianAxis.Y, data[0]), active(CartesianAxis.Z, data[0])},
                    new Sign[] {polarity(CartesianAxis.X, data[0]), polarity(CartesianAxis.Y, data[0]), polarity(CartesianAxis.Z, data[0])},
                    type(data[0])
            );
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
    private static class Mma8452QMovementData extends DataTypeBase {
        private static final long serialVersionUID = 6933107216144068304L;

        Mma8452QMovementData() {
            super(ACCELEROMETER, MOVEMENT_VALUE, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true));
        }

        Mma8452QMovementData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new Mma8452QMovementData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate mwPrivate, Number value) {
            return value;
        }

        private boolean exceedsThreshold(CartesianAxis axis, byte value) {
            byte mask= (byte) (2 << (2 * axis.ordinal()));
            return (value & mask) == mask;
        }

        private Sign direction(CartesianAxis axis, byte value) {
            byte mask= (byte) (1 << (2 * axis.ordinal()));
            return (value & mask) == mask ? Sign.NEGATIVE : Sign.POSITIVE;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper) {
            final Movement castedData = new Movement(
                    new boolean[] {exceedsThreshold(CartesianAxis.X, data[0]), exceedsThreshold(CartesianAxis.Y, data[0]), exceedsThreshold(CartesianAxis.Z, data[0])},
                    new Sign[] {direction(CartesianAxis.X, data[0]), direction(CartesianAxis.Y, data[0]), direction(CartesianAxis.Z, data[0])}
            );

            return new DataPrivate(timestamp, data, mapper) {
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

    private transient AsyncDataProducer packedAcceleration, acceleration, orientation, shake, tap, freeFall, motion;
    private transient TimedTask<byte[]> pullConfigTask;

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

    @Override
    protected void init() {
        pullConfigTask = new TimedTask<>();

        mwPrivate.addResponseHandler(new Pair<>(ACCELEROMETER.id, Util.setRead(DATA_CONFIG)), response -> pullConfigTask.setResult(response));
    }

    private int pwMode() {
        return (dataSettings[3] >> 3) & 0x3;
    }

    private int odr() {
        return (dataSettings[2] & ~0xc7) >> 3;
    }

    private int pulseLpfEn() {
        return (dataSettings[1] & ~0x10) >> 4;
    }

    @Override
    public AccelerometerMma8452q.ConfigEditor configure() {
        return new AccelerometerMma8452q.ConfigEditor() {
            private OutputDataRate odr = OutputDataRate.ODR_100_HZ;
            private FullScaleRange fsr = FullScaleRange.FSR_2G;
            private Float hpfCutoff = null;
            private Oversampling osMode = Oversampling.NORMAL;
            private SleepModeRate sleepRate = null;
            private Oversampling sleepOsMode = null;
            private int aslpTimeout = 0;
            private boolean pulseLpfEn = false;

            @Override
            public AccelerometerMma8452q.ConfigEditor odr(OutputDataRate odr) {
                this.odr = odr;
                return this;
            }

            @Override
            public AccelerometerMma8452q.ConfigEditor range(FullScaleRange fsr) {
                this.fsr = fsr;
                return this;
            }

            @Override
            public ConfigEditor enableHighPassFilter(float cutoff) {
                hpfCutoff = cutoff;
                return this;
            }

            @Override
            public ConfigEditor enableTapLowPassFilter() {
                pulseLpfEn = true;
                return this;
            }

            @Override
            public ConfigEditor oversampling(Oversampling osMode) {
                this.osMode = osMode;
                return this;
            }

            @Override
            public ConfigEditor enableAutoSleep(SleepModeRate rate, int timeout, Oversampling osMode) {
                sleepRate = rate;
                aslpTimeout = timeout;
                sleepOsMode = osMode;
                return this;
            }

            @Override
            public ConfigEditor enableAutoSleep() {
                return enableAutoSleep(SleepModeRate.SMR_6_25_HZ, 20000, Oversampling.LOW_POWER);
            }

            @Override
            public AccelerometerMma8452q.ConfigEditor odr(float odr) {
                float[] frequencies = OutputDataRate.frequencies();
                int pos = Util.closestIndex(frequencies, odr);

                return odr(OutputDataRate.values()[pos]);
            }

            @Override
            public AccelerometerMma8452q.ConfigEditor range(float fsr) {
                float[] ranges = FullScaleRange.ranges();
                int pos = Util.closestIndex(ranges, fsr);

                return range(FullScaleRange.values()[pos]);
            }

            @Override
            public void commit() {
                Arrays.fill(dataSettings, (byte) 0);

                if (sleepRate != null && sleepOsMode != null) {
                    dataSettings[2] |= sleepRate.ordinal() << 6;
                    dataSettings[3] |= 0x4;
                    dataSettings[3] |= (sleepOsMode.ordinal() << 3);
                    dataSettings[4]= (byte)(aslpTimeout / sleepCountSteps[AccelerometerMma8452qImpl.this.odr()]);
                }

                if (pulseLpfEn) {
                    dataSettings[1] |= 0x10;
                }
                if (hpfCutoff != null) {
                    int hpfSel = Util.closestIndex(OS_CUTOFF_FREQS[odr.ordinal()][osMode.ordinal()], hpfCutoff);
                    dataSettings[1] |= hpfSel & 0x3;
                    dataSettings[0] |= 0x10;
                }

                dataSettings[3] |= osMode.ordinal();
                dataSettings[2] |= odr.ordinal() << 3;
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
    public Task<Void> pullConfigAsync() {
        return pullConfigTask.execute("Did not receive BMA255 acc config within %dms", Constant.RESPONSE_TIMEOUT,
                () -> mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, Util.setRead(DATA_CONFIG)})
        ).onSuccessTask(task -> {
            System.arraycopy(task.getResult(), 2, dataSettings, 0, dataSettings.length);
            return Task.forResult(null);
        });
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
                public Task<Route> addRouteAsync(RouteBuilder builder) {
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
                    public Task<Route> addRouteAsync(RouteBuilder builder) {
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
    public OrientationDataProducer orientation() {
        if (orientation == null) {
            orientation = new OrientationDataProducer() {
                private int delay = 100;

                @Override
                public OrientationConfigEditor configure() {
                    return new OrientationConfigEditor() {
                        private int newDelay;
                        @Override
                        public OrientationConfigEditor delay(int delay) {
                            newDelay = delay;
                            return this;
                        }

                        @Override
                        public void commit() {
                            delay = newDelay;
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
                    mwPrivate.sendCommand(ACCELEROMETER, ORIENTATION_CONFIG,
                            new byte[] {0x00, (byte) 0xc0, (byte) (delay / orientationSteps[pwMode()][odr()]), 0x44, (byte) 0x84});
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, ORIENTATION_ENABLE, 0x1});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, ORIENTATION_ENABLE, 0x0});
                    mwPrivate.sendCommand(ACCELEROMETER, ORIENTATION_CONFIG,
                            new byte[] {0x00, (byte) 0x80, 0x00, 0x44, (byte) 0x84});
                }
            };
        }
        return (OrientationDataProducer) orientation;
    }

    @Override
    public ShakeDataProducer shake() {
        if (shake == null) {
            shake = new ShakeDataProducer() {
                @Override
                public ShakeConfigEditor configure() {
                    return new ShakeConfigEditor() {
                        private CartesianAxis newAxis = CartesianAxis.X;
                        private float threshold = 0.5f;
                        private int duration = 50;

                        @Override
                        public ShakeConfigEditor axis(CartesianAxis axis) {
                            newAxis = axis;
                            return this;
                        }

                        @Override
                        public ShakeConfigEditor threshold(float threshold) {
                            this.threshold = threshold;
                            return this;
                        }

                        @Override
                        public ShakeConfigEditor duration(int duration) {
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
                public Task<Route> addRouteAsync(RouteBuilder builder) {
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
    public TapDataProducer tap() {
        if (tap == null) {
            tap = new TapDataProducer() {
                @Override
                public TapConfigEditor configure() {
                    return new TapConfigEditor() {
                        private int latency = 200, window = 300, interval = 60;
                        private final LinkedHashSet<TapType> types = new LinkedHashSet<>();
                        private float threshold = 2f;
                        private CartesianAxis axis = CartesianAxis.Z;

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
                        public TapConfigEditor latency(int latency) {
                            this.latency = latency;
                            return this;
                        }

                        @Override
                        public TapConfigEditor window(int window) {
                            this.window = window;
                            return this;
                        }

                        @Override
                        public TapConfigEditor axis(CartesianAxis axis) {
                            this.axis = axis;
                            return this;
                        }

                        @Override
                        public TapConfigEditor threshold(float threshold) {
                            this.threshold = threshold;
                            return this;
                        }

                        @Override
                        public TapConfigEditor interval(int interval) {
                            this.interval = interval;
                            return this;
                        }

                        @Override
                        public void commit() {
                            byte[] pulseSettings = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

                            int pwMode = pwMode(), odr = odr();
                            int lpfEn = pulseLpfEn();

                            pulseSettings[0] |= 0x40;
                            if (types.isEmpty()) {
                                types.add(TapType.SINGLE);
                                types.add(TapType.DOUBLE);
                            }
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
                            pulseSettings[5]= (byte) (interval / pulseTmltSteps[lpfEn][pwMode][odr]);
                            pulseSettings[6]= (byte) (latency / pulseLtcySteps[lpfEn][pwMode][odr]);
                            pulseSettings[7]= (byte) (window / pulseWindSteps[lpfEn][pwMode][odr]);

                            mwPrivate.sendCommand(ACCELEROMETER, PULSE_CONFIG, pulseSettings);
                        }
                    };
                }

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

    /**
     * Detectable movement types on the sensor
     * @author Eric Tsai
     */
    private enum MovementType {
        /** Magnitude of acceleration is below threshold */
        FREE_FALL,
        /** Acceleration exceeds a set threshold */
        MOTION
    }
    private class MovementDataProducerInner implements MovementDataProducer {
        private final MovementType type;

        MovementDataProducerInner(MovementType type) {
            this.type = type;
        }

        @Override
        public MovementConfigEditor configure() {
            return new MovementConfigEditor() {
                private float threshold = (type == MovementType.FREE_FALL ? 0.5f : 1.5f);
                private int duration= 100;
                private CartesianAxis[] axes= CartesianAxis.values();

                @Override
                public MovementConfigEditor axes(CartesianAxis... axes) {
                    this.axes = axes;
                    return this;
                }

                @Override
                public MovementConfigEditor threshold(float threshold) {
                    this.threshold = threshold;
                    return this;
                }

                @Override
                public MovementConfigEditor duration(int duration) {
                    this.duration = duration;
                    return this;
                }

                @Override
                public void commit() {
                    byte[] motionSettings = new byte[] {(byte) 0x80, 0x00, 0x00, 0x00 };
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
        public Task<Route> addRouteAsync(RouteBuilder builder) {
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
            mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOVEMENT_ENABLE, 0x0});
        }
    }

    @Override
    public MovementDataProducer freeFall() {
        if (freeFall == null) {
            freeFall = new MovementDataProducerInner(MovementType.FREE_FALL);
        }
        return (MovementDataProducer) freeFall;
    }

    @Override
    public MovementDataProducer motion() {
        if (motion == null) {
            motion = new MovementDataProducerInner(MovementType.MOTION);
        }
        return (MovementDataProducer) motion;
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
