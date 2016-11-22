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

import com.mbientlab.metawear.IllegalRouteOperationException;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.RouteElement;
import com.mbientlab.metawear.builder.RouteMulticast;
import com.mbientlab.metawear.builder.RouteSplit;
import com.mbientlab.metawear.builder.filter.*;
import com.mbientlab.metawear.builder.function.*;
import com.mbientlab.metawear.builder.predicate.PulseOutput;
import com.mbientlab.metawear.impl.DataProcessorImpl.*;
import com.mbientlab.metawear.impl.ColorDetectorTcs34725Impl.ColorAdcData;
import com.mbientlab.metawear.module.DataProcessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import static com.mbientlab.metawear.impl.ModuleId.DATA_PROCESSOR;
import static com.mbientlab.metawear.impl.ModuleId.SENSOR_FUSION;

/**
 * Created by etsai on 9/4/16.
 */
class RouteElementImpl implements RouteElement {
    private static final Version MULTI_CHANNEL_MATH= new Version("1.1.0"), MULTI_COMPARISON_MIN_FIRMWARE= new Version("1.2.3");

    private enum BranchElement {
        MULTICAST,
        SPLIT
    }

    static class Cache {
        public final ArrayList<Tuple3<DataTypeBase, Subscriber, Boolean>> subscribedProducers;
        public final ArrayList<Pair<String, Tuple3<DataTypeBase, Integer, byte[]>>> feedback;
        public final LinkedList<Pair<? extends DataTypeBase, ? extends Action>> reactions;
        public final LinkedList<Processor> dataProcessors;
        public final Version firmware;
        public final MetaWearBoardPrivate mwPrivate;
        public final Stack<RouteElementImpl> stashedSignals= new Stack<>();
        public final Stack<BranchElement> elements;
        public final Stack<Pair<RouteElementImpl, DataTypeBase[]>> splits;
        public final Map<String, Processor> taggedProcessors = new LinkedHashMap<>();

        public Cache(Version firmware, MetaWearBoardPrivate mwPrivate) {
            this.subscribedProducers= new ArrayList<>();
            this.feedback = new ArrayList<>();
            this.reactions= new LinkedList<>();
            this.dataProcessors = new LinkedList<>();
            this.firmware = firmware;
            this.mwPrivate = mwPrivate;
            this.elements= new Stack<>();
            this.splits= new Stack<>();
        }
    }

    public final DataTypeBase source;
    public Cache persistantData= null;

    RouteElementImpl(DataTypeBase source) {
        this.source= source;
    }

    RouteElementImpl(DataTypeBase source, RouteElementImpl original) {
        this.source= source;
        this.persistantData= original.persistantData;
    }

    public void setup(Cache original) {
        this.persistantData= original;
    }

    @Override
    public RouteMulticast multicast() {
        persistantData.elements.push(BranchElement.MULTICAST);
        persistantData.stashedSignals.push(this);
        return new RouteMulticastImpl(this);
    }

    @Override
    public RouteElement to() {
        try {
            return persistantData.stashedSignals.peek();
        } catch (EmptyStackException e) {
            throw new IllegalRouteOperationException("No multicast source to direct data from", e);
        }
    }

    @Override
    public RouteSplit split() {
        if (source.split == null) {
            throw new IllegalRouteOperationException(String.format(Locale.US, "Cannot split source data signal '%s'", source.getClass().getName()));
        }

        persistantData.elements.push(BranchElement.SPLIT);
        persistantData.splits.push(new Pair<>(this, source.split));
        return new RouteSplitImpl(this);
    }

    @Override
    public RouteElement index(int i) {
        try {
            return new RouteElementImpl(persistantData.splits.peek().second[i], this);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalRouteOperationException("Index on split data out of bounds", e);
        }
    }

    @Override
    public RouteElement end() {
        try {
            switch (persistantData.elements.pop()) {
                case MULTICAST:
                    persistantData.stashedSignals.pop();
                    return persistantData.stashedSignals.isEmpty() ? null : persistantData.stashedSignals.peek();
                case SPLIT:
                    persistantData.splits.pop();
                    return persistantData.splits.isEmpty() ? null : persistantData.splits.peek().first;
                default:
                    throw new RuntimeException("Only here so the compiler doesn't complain");
            }
        } catch (EmptyStackException e) {
            throw new IllegalRouteOperationException("No multicast nor split to end the branch on", e);
        }
    }

    @Override
    public RouteElement name(String name) {
        if (persistantData.taggedProcessors.containsKey(name)) {
            throw new IllegalRouteOperationException(String.format("Duplicate processor key \'%s\' found", name));
        }

        persistantData.taggedProcessors.put(name, persistantData.dataProcessors.peekLast());
        return this;
    }

    @Override
    public RouteElement stream(Subscriber subscriber) {
        if (source.attributes.length() > 0) {
            persistantData.subscribedProducers.add(new Tuple3<>(source, subscriber, false));
            return this;
        }
        throw new IllegalRouteOperationException("Cannot subscribe to null data");
    }

    @Override
    public RouteElement log(Subscriber subscriber) {
        if (source.attributes.length() > 0) {
            persistantData.subscribedProducers.add(new Tuple3<>(source, subscriber, true));
            return this;
        }
        throw new IllegalRouteOperationException("Cannot log null data");
    }

    @Override
    public RouteElement react(Action action) {
        persistantData.reactions.add(new Pair<>(source, action));
        return this;
    }

    @Override
    public RouteElement buffer() {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot apply \'buffer\' filter to null data");
        }

        DataTypeBase processor= new UintData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, new DataAttributes(new byte[] {}, (byte) 0, (byte) 0, false));
        byte[] config= new byte[] {0xf, (byte) (source.attributes.length() - 1)};

        return postCreate(source.dataProcessorStateCopy(source, source.attributes), new NullEditor(config, processor, persistantData.mwPrivate));
    }

    private static class CounterInner extends EditorImplBase implements DataProcessor.Counter {
        private static final long serialVersionUID = 4873789798519714460L;

        CounterInner(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void reset() {
            owner.sendCommand(new byte[]{DATA_PROCESSOR.id, DataProcessorImpl.STATE, source.eventConfig[2],
                    0x00, 0x00, 0x00, 0x00});
        }
    }
    private static class AccumulatorInner extends EditorImplBase implements DataProcessor.Accumulator {
        private static final long serialVersionUID = -5044680524938752249L;

        AccumulatorInner(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void reset() {
            owner.sendCommand(new byte[] {DATA_PROCESSOR.id, DataProcessorImpl.STATE, source.eventConfig[2],
                    0x00, 0x00, 0x00, 0x00});
        }
    }
    private RouteElementImpl createReducer(boolean counter) {
        if (!counter) {
            if (source.attributes.length() <= 0) {
                throw new IllegalRouteOperationException("Cannot accumulate null data");
            }
            if (source.attributes.length() > 4) {
                throw new IllegalRouteOperationException("Cannot accumulate data longer than 4 bytes");
            }
        }

        final byte output= 4;

        DataAttributes attributes= new DataAttributes(new byte[] {output}, (byte) 1, (byte) 0, !counter && source.attributes.signed);
        final DataTypeBase processor= counter ?
                new UintData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, attributes) :
                source.dataProcessorCopy(source, attributes);
        byte[] config= new byte[] {0x2, (byte) (((output - 1) & 0x3) | (((source.attributes.length() - 1) & 0x3) << 2) | (counter ? 0x10 : 0))};
        EditorImplBase editor= counter ?
                new CounterInner(config, processor, persistantData.mwPrivate) :
                new AccumulatorInner(config, processor, persistantData.mwPrivate);

        DataTypeBase state= counter ?
                new UintData(null, DATA_PROCESSOR, Util.setSilentRead(DataProcessorImpl.STATE), DataTypeBase.NO_DATA_ID, attributes) :
                processor.dataProcessorStateCopy(source, attributes);
        return postCreate(state, editor);
    }

    @Override
    public RouteElement count() {
        return createReducer(true);
    }

    @Override
    public RouteElement accumulate() {
        return createReducer(false);
    }

    private static class AveragerInner extends EditorImplBase implements DataProcessor.Averager {
        private static final long serialVersionUID = 998301829125848762L;

        AveragerInner(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void modify(byte samples) {
            config[2]= samples;
            owner.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }

        @Override
        public void reset() {
            owner.sendCommand(new byte[] {DATA_PROCESSOR.id, DataProcessorImpl.STATE, source.eventConfig[2]});
        }
    }
    @Override
    public RouteElement average(byte samples) {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot average null data");
        }
        if (source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot average data longer than 4 bytes");
        }

        final DataTypeBase processor = source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
        byte[] config= new byte[]{
                0x3,
                (byte) (((source.attributes.length() - 1) & 0x3) | (((source.attributes.length() - 1) & 0x3) << 2)),
                samples
        };

        return postCreate(null, new AveragerInner(config, processor, persistantData.mwPrivate));
    }

    @Override
    public RouteElement delay(byte samples) {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot delay null data");
        }
        if (source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot delay data longer than 4 bytes");
        }

        DataTypeBase processor = source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
        byte[] config= new byte[]{0xa, (byte) ((source.attributes.length() - 1) & 0x3), samples};

        return postCreate(null, new NullEditor(config, processor, persistantData.mwPrivate));
    }

    private RouteElementImpl createCombiner(DataTypeBase source, byte mode) {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException(String.format(Locale.US, "Cannot apply \'%s\' to null data", mode == 0 ? "rms" : "rss"));
        } else if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException(String.format(Locale.US, "Cannot apply \'%s\' to sensor fusion data", mode == 0 ? "rms" : "rss"));
        }

        final byte signedMask = (byte) (source.attributes.signed ? 0x80 : 0x0);
        // assume sizes array is filled with the same value
        DataAttributes attributes= new DataAttributes(new byte[] {source.attributes.sizes[0]}, (byte) 1, (byte) 0, false);
        DataTypeBase processor= source instanceof CartesianFloatData ?
                new UFloatData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, attributes) :
                new UintData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, attributes);
        byte[] config= new byte[] {
                0x7,
                (byte) (((processor.attributes.sizes[0] - 1) & 0x3) | (((source.attributes.sizes[0] - 1) & 0x3) << 2) | (((source.attributes.sizes.length - 1) & 0x3) << 4) | signedMask),
                mode
        };

        return postCreate(null, new NullEditor(config, processor, persistantData.mwPrivate));
    }

    private enum MathOp {
        /** Add the data */
        ADD,
        /** Multiply the data */
        MULTIPLY,
        /** Divide the data */
        DIVIDE,
        /** Calculate the remainder */
        MODULUS,
        /** Calculate exponentiation of the data */
        EXPONENT,
        /** Calculate square root */
        SQRT,
        /** Perform left shift */
        LEFT_SHIFT,
        /** Perform right shift */
        RIGHT_SHIFT,
        /** Subtract the data */
        SUBTRACT,
        /** Calculates the absolute value */
        ABS_VALUE,
        /** Transforms the input into a constant value */
        CONSTANT
    }

    @Override
    public RouteElement map(Function1 fn) {
        switch(fn) {
            case ABS_VALUE:
                return applyMath(MathOp.ABS_VALUE, 0);
            case RMS:
                if (source instanceof CartesianFloatData || source instanceof ColorAdcData) {
                    return createCombiner(source, (byte) 0);
                }
                return null;
            case RSS:
                if (source instanceof CartesianFloatData || source instanceof ColorAdcData) {
                    return createCombiner(source, (byte) 1);
                }
                return null;
            case SQRT:
                return applyMath(MathOp.SQRT, 0);
        }
        throw new RuntimeException("Only here so the compiler won't get mad");
    }

    @Override
    public RouteElement map(Function2 fn, String ... dataNames) {
        RouteElement next= map(fn, 0);
        if (next != null) {
            for (String key : dataNames) {
                persistantData.feedback.add(new Pair<>(key, new Tuple3<>(((RouteElementImpl) next).source, 4,
                        persistantData.dataProcessors.peekLast().editor.config)));
            }
        }
        return next;
    }

    @Override
    public RouteElement map(Function2 fn, Number rhs) {
        switch(fn) {
            case ADD:
                return applyMath(MathOp.ADD, rhs);
            case MULTIPLY:
                return applyMath(MathOp.MULTIPLY, rhs);
            case DIVIDE:
                return applyMath(MathOp.DIVIDE, rhs);
            case MODULUS:
                return applyMath(MathOp.MODULUS, rhs);
            case EXPONENT:
                return applyMath(MathOp.EXPONENT, rhs);
            case LEFT_SHIFT:
                return applyMath(MathOp.LEFT_SHIFT, rhs);
            case RIGHT_SHIFT:
                return applyMath(MathOp.RIGHT_SHIFT, rhs);
            case SUBTRACT:
                return applyMath(MathOp.SUBTRACT, rhs);
            case CONSTANT:
                return applyMath(MathOp.CONSTANT, rhs);
        }
        throw new RuntimeException("Only here so the compiler won't get mad");
    }

    private DataTypeBase createUnsignedToSigned(DataAttributes attrs) {
        if (source instanceof UFloatData) {
            return new SFloatData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, attrs);
        } else if (source instanceof UintData) {
            return new IntData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, attrs);
        } else {
            return source.dataProcessorCopy(source, attrs);
        }
    }

    private static class MapperInner extends EditorImplBase implements DataProcessor.Mapper {
        private static final long serialVersionUID = 8475942086629415224L;

        MapperInner(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void modifyRhs(Number rhs) {
            final Number scaledRhs;

            switch(MathOp.values()[config[2] - 1]) {
                case ADD:
                case MODULUS:
                case SUBTRACT:
                    scaledRhs= source.convertToFirmwareUnits(owner, rhs);
                    break;
                case SQRT:
                case ABS_VALUE:
                    scaledRhs= 0;
                    break;
                default:
                    scaledRhs= rhs;
            }

            byte[] newRhs= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(scaledRhs.intValue()).array();
            System.arraycopy(newRhs, 0, config, 3, newRhs.length);
            owner.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    private RouteElement applyMath(MathOp op, Number rhs) {
        boolean multiChnlMath= persistantData.firmware.compareTo(MULTI_CHANNEL_MATH) >= 0;

        if (!multiChnlMath && source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot apply math operations on multi-channel data for firmware prior to " + MULTI_CHANNEL_MATH.toString());
        }

        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot apply math operations to null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot apply math operations to sensor fusion data");
        }

        final DataTypeBase processor;

        switch(op) {
            case ADD: {
                DataAttributes newAttrs= source.attributes.dataProcessorCopySize((byte) 4);
                if (rhs.floatValue() < 0) {
                    processor = createUnsignedToSigned(newAttrs);
                } else {
                    processor = source.dataProcessorCopy(source, newAttrs);
                }
                break;
            }
            case MULTIPLY: {
                DataAttributes newAttrs= source.attributes.dataProcessorCopySize(Math.abs(rhs.floatValue()) < 1 ? source.attributes.sizes[0] : 4);

                if (rhs.floatValue() < 0) {
                    processor = createUnsignedToSigned(newAttrs);
                } else {
                    processor = source.dataProcessorCopy(source, newAttrs);
                }
                break;
            }
            case DIVIDE: {
                DataAttributes newAttrs = source.attributes.dataProcessorCopySize(Math.abs(rhs.floatValue()) < 1 ? 4 : source.attributes.sizes[0]);

                if (rhs.floatValue() < 0) {
                    processor= createUnsignedToSigned(newAttrs);
                } else {
                    processor = source.dataProcessorCopy(source, newAttrs);
                }
                break;
            }
            case MODULUS: {
                processor = source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
                break;
            }
            case EXPONENT: {
                DataAttributes newAttrs = source.attributes.dataProcessorCopySize((byte) 4);
                processor = source.dataProcessorCopy(source, newAttrs);
                break;
            }
            case LEFT_SHIFT: {
                byte newChannelSize = (byte) (source.attributes.sizes[0] + (rhs.intValue() / 8));
                if (newChannelSize > 4) {
                    newChannelSize = 4;
                }

                processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopySize(newChannelSize));
                break;
            }
            case RIGHT_SHIFT: {
                byte newChannelSize = (byte) (source.attributes.sizes[0] - (rhs.intValue() / 8));
                if (newChannelSize <= 0) {
                    newChannelSize = 1;
                }

                processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopySize(newChannelSize));
                break;
            }
            case SUBTRACT: {
                processor= createUnsignedToSigned(source.attributes.dataProcessorCopySigned(true));
                break;
            }
            case SQRT:
            case ABS_VALUE: {
                DataAttributes copy= source.attributes.dataProcessorCopySigned(false);

                if (source instanceof SFloatData) {
                    processor= new UFloatData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, copy);
                } else if (source instanceof IntData) {
                    processor= new UintData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, copy);
                } else {
                    processor= source.dataProcessorCopy(source, copy);
                }
                break;
            }
            case CONSTANT:
                processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
                break;
            default:
                processor= null;
                break;
        }

        final Number scaledRhs;
        switch (op) {
            case ADD:
            case MODULUS:
            case SUBTRACT:
                scaledRhs= source.convertToFirmwareUnits(persistantData.mwPrivate, rhs);
                break;
            case SQRT:
            case ABS_VALUE:
                scaledRhs= 0;
                break;
            default:
                scaledRhs= rhs;
        }

        ByteBuffer buffer= ByteBuffer.allocate(multiChnlMath ? 8 : 7)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0x9)
                .put((byte) ((processor.attributes.length() - 1) & 0x3 | ((source.attributes.sizes[0] - 1) << 2) | (source.attributes.signed ? 0x10 : 0)))
                .put((byte) (op.ordinal() + 1))
                .putInt(scaledRhs.intValue());
        if (multiChnlMath) {
            buffer.put((byte) (source.attributes.sizes.length - 1));
        }

        return postCreate(null, new MapperInner(buffer.array(), processor, persistantData.mwPrivate));
    }

    private static class TimeLimiterInner extends EditorImplBase implements DataProcessor.TimeLimiter {
        private static final long serialVersionUID = -3775715195615375018L;

        TimeLimiterInner(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void modify(int period) {
            byte[] newPeriod= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(period).array();
            System.arraycopy(newPeriod, 0, config, 2, newPeriod.length);

            owner.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteElement limit(int period) {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot limit frequency of null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot limit frequency of sensor fusion data");
        }

        final DataTypeBase processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
        byte[] config= ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0x8)
                .put((byte) ((source.attributes.length() - 1))).putInt(period).array();
        return postCreate(null, new TimeLimiterInner(config, processor, persistantData.mwPrivate));
    }

    private static class PassthroughLimiterInner extends EditorImplBase implements DataProcessor.PassthroughLimiter {
        private static final long serialVersionUID = 3157873682587128118L;

        PassthroughLimiterInner(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void set(short value) {
            byte[] newValue= ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();

            owner.sendCommand(DATA_PROCESSOR, DataProcessorImpl.STATE, source.eventConfig[2], newValue);
        }

        @Override
        public void modify(Passthrough type, short value) {
            byte[] newValue= ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
            System.arraycopy(newValue, 0, config, 2, newValue.length);
            config[1]= (byte) (type.ordinal() & 0x7);

            owner.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteElement limit(Passthrough type, short value) {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot limit null data");
        }

        final DataTypeBase processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
        byte[] config= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0x1)
                .put((byte) (type.ordinal() & 0x7))
                .putShort(value).array();
        DataTypeBase state= new UintData(DATA_PROCESSOR, Util.setSilentRead(DataProcessorImpl.STATE), DataTypeBase.NO_DATA_ID,
                new DataAttributes(new byte[] {2}, (byte) 1, (byte) 0, false));
        return postCreate(state, new PassthroughLimiterInner(config, processor, persistantData.mwPrivate));
    }

    private static class PulseFinderInner extends EditorImplBase implements DataProcessor.PulseFinder {
        private static final long serialVersionUID = -274897612921984101L;

        PulseFinderInner(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void modify(Number threshold, short samples) {
            byte[] newConfig= ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(source.convertToFirmwareUnits(owner, threshold).intValue())
                    .putShort(samples)
                    .array();
            System.arraycopy(newConfig, 0, config, 4, newConfig.length);

            owner.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteElement find(PulseOutput type, Number threshold, short samples) {
        if (source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot find pulses for data longer than 4 bytes");
        }

        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot find pulses for null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot find pulses for sensor fusion data");
        }

        final DataTypeBase processor;

        switch(type) {
            case WIDTH:
                processor= new UintData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY,
                        new DataAttributes(new byte[] {2}, (byte) 1, (byte) 0, false));
                break;
            case AREA:
                processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopySize((byte) 4));
                break;
            case PEAK:
                processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
                break;
            case ON_DETECT:
                processor= new UintData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY,
                        new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false));
                break;
            default:
                processor= null;
                break;
        }

        byte[] config= ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0xb)
                .put((byte) (source.attributes.length() - 1))
                .put((byte) 0)
                .put((byte) type.ordinal())
                .putInt(source.convertToFirmwareUnits(persistantData.mwPrivate, threshold).intValue())
                .putShort(samples)
                .array();

        return postCreate(null, new PulseFinderInner(config, processor, persistantData.mwPrivate));
    }

    @Override
    public RouteElement filter(Comparison op, String ... dataNames) {
        return filter(op, ComparisonOutput.ABSOLUTE, dataNames);
    }

    @Override
    public RouteElement filter(Comparison op, Number ... references) {
        return filter(op, ComparisonOutput.ABSOLUTE, references);
    }

    @Override
    public RouteElement filter(Comparison op, ComparisonOutput type, String ... dataNames) {
        RouteElement next= filter(op, type, 0);
        if (next != null) {
            for (String key : dataNames) {
                persistantData.feedback.add(new Pair<>(
                        key,
                        new Tuple3<>(
                                ((RouteElementImpl) next).source,
                                persistantData.firmware.compareTo(MULTI_COMPARISON_MIN_FIRMWARE) < 0 ? 5 : 3,
                                persistantData.dataProcessors.peekLast().editor.config
                        )
                ));
            }
        }

        return next;
    }

    private static class SingleValueComparator extends EditorImplBase implements DataProcessor.Comparator {
        private static final long serialVersionUID = -8891137550284171832L;

        SingleValueComparator(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void modify(Comparison op, Number... references) {
            byte[] newConfig= ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                    .put((byte) op.ordinal())
                    .put((byte) 0)
                    .putInt(source.convertToFirmwareUnits(owner, references[0]).intValue())
                    .array();
            System.arraycopy(newConfig, 0, config, 2, newConfig.length);

            owner.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    private static class MultiValueComparator extends EditorImplBase implements DataProcessor.Comparator {
        private static final long serialVersionUID = 893378903150606106L;

        static void fillReferences(DataTypeBase source, MetaWearBoardPrivate owner, ByteBuffer buffer, byte length, Number... references) {
            switch(length) {
                case 1:
                    for(Number it: references) {
                        buffer.put(source.convertToFirmwareUnits(owner, it).byteValue());
                    }
                    break;
                case 2:
                    for(Number it: references) {
                        buffer.putShort(source.convertToFirmwareUnits(owner, it).shortValue());
                    }
                    break;
                case 4:
                    for(Number it: references) {
                        buffer.putInt(source.convertToFirmwareUnits(owner, it).intValue());
                    }
                    break;
            }
        }


        MultiValueComparator(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void modify(Comparison op, Number... references) {
            ByteBuffer buffer= ByteBuffer.allocate(references.length * source.attributes.length()).order(ByteOrder.LITTLE_ENDIAN);
            fillReferences(source, owner, buffer, source.attributes.length(), references);
            byte[] newRef= buffer.array();

            byte[] newConfig= new byte[2 + references.length * source.attributes.length()];
            newConfig[0]= config[0];
            newConfig[1]= (byte) ((config[1] & ~0x38) | (op.ordinal() << 3));

            System.arraycopy(newRef, 0, newConfig, 2, newRef.length);
            config= newConfig;

            owner.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteElement filter(Comparison op, ComparisonOutput type, Number... references) {
        if (source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot compare data longer than 4 bytes");
        }

        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot compare null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot compare sensor sensor fusion data");
        }

        if (persistantData.firmware.compareTo(MULTI_COMPARISON_MIN_FIRMWARE) < 0) {
            boolean signed= source.attributes.signed || references[0].floatValue() < 0;
            final Number scaledReference= source.convertToFirmwareUnits(persistantData.mwPrivate, references[0]);

            final DataTypeBase processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
            byte[] config= ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                    .put((byte) 0x6)
                    .put((byte) (signed ? 1 : 0))
                    .put((byte) op.ordinal())
                    .put((byte) 0)
                    .putInt(scaledReference.intValue()).array();
            return postCreate(null, new SingleValueComparator(config, processor, persistantData.mwPrivate));
        }

        boolean anySigned= false;
        for(Number it: references) {
            anySigned|= it.floatValue() < 0;
        }
        boolean signed= source.attributes.signed || anySigned;

        final DataTypeBase processor;
        if (type == ComparisonOutput.PASS_FAIL || type == ComparisonOutput.ZONE) {
            DataAttributes newAttrs= new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false);
            processor= new UintData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, newAttrs);
        }  else {
            processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
        }

        ByteBuffer buffer= ByteBuffer.allocate(2 + references.length * source.attributes.length()).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0x6)
                .put((byte) ((signed ? 1 : 0) | ((source.attributes.length() - 1) << 1) | (op.ordinal() << 3) | (type.ordinal() << 6)));
        MultiValueComparator.fillReferences(processor, persistantData.mwPrivate, buffer, source.attributes.length(), references);

        return postCreate(null, new MultiValueComparator(buffer.array(), processor, persistantData.mwPrivate));
    }

    private static class ThresholdDetectorInner extends EditorImplBase implements DataProcessor.ThresholdDetector {
        private static final long serialVersionUID = 7819456776691980008L;

        ThresholdDetectorInner(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void modify(Number threshold, Number hysteresis) {
            byte[] newConfig= ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(source.convertToFirmwareUnits(owner, threshold).intValue())
                    .putShort(source.convertToFirmwareUnits(owner, hysteresis).shortValue())
                    .array();
            System.arraycopy(newConfig, 0, config, 2, newConfig.length);

            owner.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteElement filter(ThresholdOutput output, Number threshold) {
        return filter(output, threshold, 0);
    }

    @Override
    public RouteElement filter(ThresholdOutput output, Number threshold, Number hysteresis) {
        if (source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot use threshold filter on data longer than 4 bytes");
        }

        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot use threshold filter on null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot use threshold filter on sensor fusion data");
        }

        final DataTypeBase processor;
        switch (output) {
            case ABSOLUTE:
                processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
                break;
            case BINARY:
                processor= new IntData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY,
                        new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true));
                break;
            default:
                processor= null;
                break;
        }

        Number firmwareValue = source.convertToFirmwareUnits(persistantData.mwPrivate, threshold), firmwareHysteresis= source.convertToFirmwareUnits(persistantData.mwPrivate, hysteresis);
        byte[] config= ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0xd)
                .put((byte) ((source.attributes.length() - 1) & 0x3 | (source.attributes.signed ? 0x4 : 0) | (output.ordinal() << 3)))
                .putInt(firmwareValue.intValue())
                .putShort(firmwareHysteresis.shortValue())
                .array();

        return postCreate(null, new ThresholdDetectorInner(config, processor, persistantData.mwPrivate));
    }

    private static class DifferentialDetectorInner extends EditorImplBase implements DataProcessor.DifferentialDetector {
        private static final long serialVersionUID = -1856057294039232525L;

        DifferentialDetectorInner(byte[] config, DataTypeBase source, MetaWearBoardPrivate owner) {
            super(config, source, owner);
        }

        @Override
        public void modify(Number difference) {
            byte[] newDiff= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(source.convertToFirmwareUnits(owner, difference).intValue()).array();
            System.arraycopy(newDiff, 0, config, 2, newDiff.length);

            owner.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteElement filter(DifferentialOutput output, Number difference) {
        if (source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot use differential filter for data longer than 4 bytes");
        }

        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot use differential filter for null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot use differential filter on sensor fusion data");
        }

        final DataTypeBase processor;
        switch(output) {
            case ABSOLUTE:
                processor= source.dataProcessorCopy(source, source.attributes.dataProcessorCopy());
                break;
            case DIFFERENCE:
                processor= createUnsignedToSigned(source.attributes.dataProcessorCopySigned(true));
                break;
            case BINARY:
                processor= new IntData(source, DATA_PROCESSOR, DataProcessorImpl.NOTIFY,
                        new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, true));
                break;
            default:
                processor= null;
                break;
        }

        Number firmwareUnits = source.convertToFirmwareUnits(persistantData.mwPrivate, difference);
        byte[] config= ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0xc)
                .put((byte) (((source.attributes.length() - 1) & 0x3) | (source.attributes.signed ? 0x4 : 0) | (output.ordinal() << 3)))
                .putInt(firmwareUnits.intValue())
                .array();

        return postCreate(null, new DifferentialDetectorInner(config, processor, persistantData.mwPrivate));
    }

    private RouteElementImpl postCreate(DataTypeBase state, EditorImplBase editor) {
        persistantData.dataProcessors.add(new Processor(state, editor));
        return new RouteElementImpl(editor.source, this);
    }
}

