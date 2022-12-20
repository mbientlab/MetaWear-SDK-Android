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

import static com.mbientlab.metawear.impl.Constant.Module.DATA_PROCESSOR;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.DataToken;
import com.mbientlab.metawear.builder.filter.ComparisonOutput;
import com.mbientlab.metawear.impl.Constant.Module;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBmi270;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.GyroBmi270;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Created by etsai on 9/4/16.
 */
abstract class DataTypeBase implements Serializable, DataToken {
    static String createUri(DataTypeBase dataType, MetaWearBoardPrivate mwPrivate) {
        String uri = null;
        switch(Module.lookupEnum(dataType.eventConfig[0])) {
            case SWITCH:
                uri = SwitchImpl.createUri(dataType);
                break;
            case ACCELEROMETER:
                Object accModule = mwPrivate.getModules().get(Accelerometer.class);
                if (accModule instanceof AccelerometerMma8452q) {
                    uri = AccelerometerMma8452qImpl.createUri(dataType);
                } else if (accModule instanceof AccelerometerBmi270) {
                    uri = AccelerometerBmi270Impl.createUri(dataType);
                } else if (accModule instanceof AccelerometerBmi160) {
                    uri = AccelerometerBmi160Impl.createUri(dataType);
                } else if (accModule instanceof AccelerometerBma255) {
                    uri = AccelerometerBoschImpl.createUri(dataType);
                }
                break;
            case TEMPERATURE:
                uri = TemperatureImpl.createUri(dataType);
                break;
            case DATA_PROCESSOR:
                uri = DataProcessorImpl.createUri(dataType, (DataProcessorImpl) mwPrivate.getModules().get(DataProcessor.class), mwPrivate.getFirmwareVersion(),
                        mwPrivate.lookupModuleInfo(DATA_PROCESSOR).revision);
                break;
            case SERIAL_PASSTHROUGH:
                uri = SerialPassthroughImpl.createUri(dataType);
                break;
            case SETTINGS:
                uri = SettingsImpl.createUri(dataType);
                break;
            case BAROMETER:
                uri = BarometerBoschImpl.createUri(dataType);
                break;
            case GYRO:
                Object gyroModule = mwPrivate.getModules().get(Gyro.class);
                if (gyroModule instanceof GyroBmi270) {
                    uri = GyroBmi270Impl.createUri(dataType);
                } else if (gyroModule instanceof GyroBmi160) {
                    uri = GyroBmi160Impl.createUri(dataType);
                }
                break;
                //uri = GyroImpl.createUri(dataType);
                //break;
            case AMBIENT_LIGHT:
                uri = AmbientLightLtr329Impl.createUri(dataType);
                break;
            case MAGNETOMETER:
                uri = MagnetometerBmm150Impl.createUri(dataType);
                break;
            case HUMIDITY:
                uri = HumidityBme280Impl.createUri(dataType);
                break;
            case COLOR_DETECTOR:
                uri = ColorTcs34725Impl.createUri(dataType);
                break;
            case PROXIMITY:
                uri = ProximityTsl2671Impl.createUri(dataType);
                break;
            case SENSOR_FUSION:
                uri = SensorFusionBoschImpl.createUri(dataType);
                break;
            default:
                uri = null;
                break;
        }

        if (uri == null) {
            throw new IllegalStateException("Cannot create uri for data type: " + Util.arrayToHexString(dataType.eventConfig));
        }
        return uri;
    }

    static final byte NO_DATA_ID= (byte) 0xff;
    private static final long serialVersionUID = 1389028730582422419L;

    public final byte[] eventConfig;
    public final DataAttributes attributes;
    public final DataTypeBase input;
    public final DataTypeBase[] split;

    DataTypeBase(byte[] config, byte offset, byte length) {
        eventConfig = config;
        input = null;
        split = null;
        attributes = new DataAttributes(new byte[] { length }, (byte) 1, offset, false);
    }

    DataTypeBase(Constant.Module module, byte register, byte id, DataAttributes attributes) {
        this(null, module, register, id, attributes);
    }

    DataTypeBase(Constant.Module module, byte register, DataAttributes attributes) {
        this(null, module, register, attributes);
    }

    DataTypeBase(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
        this.eventConfig = new byte[] {module.id, register, id};
        this.attributes= attributes;
        this.input= input;
        this.split = createSplits();
    }

    DataTypeBase(DataTypeBase input, Constant.Module module, byte register, DataAttributes attributes) {
        this(input, module, register, NO_DATA_ID, attributes);
    }

    Tuple3<Byte, Byte, Byte> eventConfigAsTuple() {
        return new Tuple3<>(eventConfig[0], eventConfig[1], eventConfig[2]);
    }

    public void read(MetaWearBoardPrivate mwPrivate) {
        if (eventConfig[2] == NO_DATA_ID) {
            mwPrivate.sendCommand(new byte[] {eventConfig[0], eventConfig[1]});
        } else {
            mwPrivate.sendCommand(eventConfig);
        }
    }

    public void read(MetaWearBoardPrivate mwPrivate, byte[] parameters) {
        byte[] command= new byte[eventConfig.length + parameters.length];
        System.arraycopy(eventConfig, 0, command, 0, eventConfig.length);
        System.arraycopy(parameters, 0, command, eventConfig.length, parameters.length);

        mwPrivate.sendCommand(command);
    }

    void markSilent() {
        if ((eventConfig[1] & 0x80) == 0x80) {
            eventConfig[1] |= 0x40;
        }
    }

    void markLive() {
        if ((eventConfig[1] & 0x80) == 0x80) {
            eventConfig[1] &= ~0x40;
        }
    }

    protected float scale(MetaWearBoardPrivate mwPrivate) {
        return (input == null) ? 1.f : input.scale(mwPrivate);
    }
    public abstract DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes);
    public DataTypeBase dataProcessorCopy(DataTypeBase input, DataAttributes attributes) {
        return copy(input, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, NO_DATA_ID, attributes);
    }
    public DataTypeBase dataProcessorStateCopy(DataTypeBase input, DataAttributes attributes) {
        return copy(input, DATA_PROCESSOR, Util.setSilentRead(DataProcessorImpl.STATE), NO_DATA_ID, attributes);
    }

    public Number convertToFirmwareUnits(MetaWearBoardPrivate mwPrivate, Number value) {
        return value;
    }
    public abstract Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper);
    Pair<? extends DataTypeBase, ? extends DataTypeBase> dataProcessorTransform(DataProcessorConfig config, DataProcessorImpl dpModule) {
        switch(config.id) {
            case DataProcessorConfig.Buffer.ID:
                return new Pair<>(
                        new UintData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, new DataAttributes(new byte[] {}, (byte) 0, (byte) 0, false)),
                        dataProcessorStateCopy(this, this.attributes)
                );
            case DataProcessorConfig.Accumulator.ID: {
                DataProcessorConfig.Accumulator casted = (DataProcessorConfig.Accumulator) config;
                DataAttributes attributes= new DataAttributes(new byte[] {casted.output}, (byte) 1, (byte) 0, !casted.counter && this.attributes.signed);

                return new Pair<>(
                        casted.counter ? new UintData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, attributes) : dataProcessorCopy(this, attributes),
                        casted.counter ? new UintData(null, DATA_PROCESSOR, Util.setSilentRead(DataProcessorImpl.STATE), DataTypeBase.NO_DATA_ID, attributes) :
                                dataProcessorStateCopy(this, attributes)
                );
            }
            case DataProcessorConfig.Average.ID:
            case DataProcessorConfig.Delay.ID:
            case DataProcessorConfig.Time.ID:
                return new Pair<>(dataProcessorCopy(this, this.attributes.dataProcessorCopy()), null);
            case DataProcessorConfig.Passthrough.ID:
                return new Pair<>(
                        dataProcessorCopy(this, this.attributes.dataProcessorCopy()),
                        new UintData(DATA_PROCESSOR, Util.setSilentRead(DataProcessorImpl.STATE), DataTypeBase.NO_DATA_ID, new DataAttributes(new byte[] {2}, (byte) 1, (byte) 0, false))
                );
            case DataProcessorConfig.Maths.ID: {
                DataProcessorConfig.Maths casted = (DataProcessorConfig.Maths) config;
                DataTypeBase processor = null;
                switch(casted.op) {
                    case ADD:
                        processor = dataProcessorCopy(this, attributes.dataProcessorCopySize((byte) 4));
                        break;
                    case MULTIPLY:
                        processor = dataProcessorCopy(this, attributes.dataProcessorCopySize(Math.abs(casted.rhs) < 1 ? attributes.sizes[0] : 4));
                        break;
                    case DIVIDE:
                        processor = dataProcessorCopy(this, attributes.dataProcessorCopySize(Math.abs(casted.rhs) < 1 ? 4 : attributes.sizes[0]));
                        break;
                    case SUBTRACT:
                        processor = dataProcessorCopy(this, attributes.dataProcessorCopySigned(true));
                        break;
                    case ABS_VALUE:
                        processor = dataProcessorCopy(this, attributes.dataProcessorCopySigned(false));
                        break;
                    case MODULUS: {
                        processor = dataProcessorCopy(this, attributes.dataProcessorCopy());
                        break;
                    }
                    case EXPONENT: {
                        processor = new ByteArrayData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY,
                                attributes.dataProcessorCopySize((byte) 4));
                        break;
                    }
                    case LEFT_SHIFT: {
                        processor = new ByteArrayData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY,
                                attributes.dataProcessorCopySize((byte) Math.min(attributes.sizes[0] + (casted.rhs / 8), 4)));
                        break;
                    }
                    case RIGHT_SHIFT: {
                        processor = new ByteArrayData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY,
                                attributes.dataProcessorCopySize((byte) Math.max(attributes.sizes[0] - (casted.rhs / 8), 1)));
                        break;
                    }
                    case SQRT: {
                        processor = new ByteArrayData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, attributes.dataProcessorCopySigned(false));
                        break;
                    }
                    case CONSTANT:
                        DataAttributes attributes = new DataAttributes(new byte[] {4}, (byte) 1, (byte) 0, casted.rhs >= 0);
                        processor = attributes.signed ? new IntData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, attributes) :
                                new UintData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, attributes);
                        break;
                }
                if (processor != null) {
                    return new Pair<>(processor, null);
                }
                break;
            }
            case DataProcessorConfig.Pulse.ID: {
                DataProcessorConfig.Pulse casted = (DataProcessorConfig.Pulse) config;
                DataTypeBase processor;
                switch(casted.mode) {
                    case WIDTH:
                        processor = new UintData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, new DataAttributes(new byte[] {2}, (byte) 1, (byte) 0, false));
                        break;
                    case AREA:
                        processor = dataProcessorCopy(this, attributes.dataProcessorCopySize((byte) 4));
                        break;
                    case PEAK:
                        processor = dataProcessorCopy(this, attributes.dataProcessorCopy());
                        break;
                    case ON_DETECT:
                        processor = new UintData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false));
                        break;
                    default:
                        processor = null;
                }
                if (processor != null) {
                    return new Pair<>(processor, null);
                }
                break;
            }
            case DataProcessorConfig.Comparison.ID: {
                DataTypeBase processor = null;
                if (config instanceof DataProcessorConfig.SingleValueComparison) {
                    processor = dataProcessorCopy(this, this.attributes.dataProcessorCopy());
                } else if (config instanceof DataProcessorConfig.MultiValueComparison) {
                    DataProcessorConfig.MultiValueComparison casted = (DataProcessorConfig.MultiValueComparison) config;
                    if (casted.mode == ComparisonOutput.PASS_FAIL || casted.mode == ComparisonOutput.ZONE) {
                        processor = new UintData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false));
                    } else {
                        processor = dataProcessorCopy(this, attributes.dataProcessorCopy());
                    }
                }
                if (processor != null) {
                    return new Pair<>(processor, null);
                }
                break;
            }
            case DataProcessorConfig.Threshold.ID: {
                DataProcessorConfig.Threshold casted = (DataProcessorConfig.Threshold) config;
                switch (casted.mode) {
                    case ABSOLUTE:
                        return new Pair<>(dataProcessorCopy(this, attributes.dataProcessorCopy()), null);
                    case BINARY:
                        return new Pair<>(new IntData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY,
                                new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true)), null);
                }
                break;
            }
            case DataProcessorConfig.Differential.ID: {
                DataProcessorConfig.Differential casted = (DataProcessorConfig.Differential) config;
                switch(casted.mode) {
                    case ABSOLUTE:
                        return new Pair<>(dataProcessorCopy(this, attributes.dataProcessorCopy()), null);
                    case DIFFERENCE:
                        throw new IllegalStateException("Differential processor in 'difference' mode must be handled by subclasses");
                    case BINARY:
                        return new Pair<>(new IntData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true)), null);
                }
                break;
            }
            case DataProcessorConfig.Packer.ID: {
                DataProcessorConfig.Packer casted = (DataProcessorConfig.Packer) config;
                return new Pair<>(dataProcessorCopy(this, attributes.dataProcessorCopyCopies(casted.count)), null);
            }
            case DataProcessorConfig.Accounter.ID: {
                DataProcessorConfig.Accounter casted = (DataProcessorConfig.Accounter) config;
                return new Pair<>(dataProcessorCopy(this, new DataAttributes(new byte[] {casted.length, attributes.length()}, (byte) 1, (byte) 0, attributes.signed)), null);
            }
            case DataProcessorConfig.Fuser.ID: {
                byte fusedLength = attributes.length();
                DataProcessorConfig.Fuser casted = (DataProcessorConfig.Fuser) config;

                for(byte id: casted.filterIds) {
                    fusedLength+= dpModule.activeProcessors.get(id).state.attributes.length();
                }

                return new Pair<>(new ArrayData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, new DataAttributes(new byte[] {fusedLength}, (byte) 1, (byte) 0, false)), null);
            }
        }
        throw new IllegalStateException("Unable to determine the DataTypeBase object for config: " + Util.arrayToHexString(config.build()));
    }

    protected DataTypeBase[] createSplits() {
        return null;
    }

    public DataToken slice(byte offset, byte length) {
        if (offset < 0) {
            throw new IndexOutOfBoundsException("offset must be >= 0");
        }
        if (offset + length > attributes.length()) {
            throw new IndexOutOfBoundsException("offset + length is greater than data length (" + attributes.length() + ")");
        }
        return new DataTypeBase(eventConfig, offset, length) {
            @Override
            public DataTypeBase copy(DataTypeBase input, Module module, byte register, byte id, DataAttributes attributes) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, byte[] data, Calendar timestamp, DataPrivate.ClassToObject mapper) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
