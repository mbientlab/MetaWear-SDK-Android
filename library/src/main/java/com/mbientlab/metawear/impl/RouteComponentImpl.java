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
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.RouteMulticast;
import com.mbientlab.metawear.builder.RouteSplit;
import com.mbientlab.metawear.builder.filter.*;
import com.mbientlab.metawear.builder.function.*;
import com.mbientlab.metawear.builder.predicate.PulseOutput;
import com.mbientlab.metawear.impl.DataProcessorImpl.*;
import com.mbientlab.metawear.impl.ColorTcs34725Impl.ColorAdcData;
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

import static com.mbientlab.metawear.impl.Constant.MAX_BTLE_LENGTH;
import static com.mbientlab.metawear.impl.Constant.Module.DATA_PROCESSOR;
import static com.mbientlab.metawear.impl.Constant.Module.SENSOR_FUSION;

/**
 * Created by etsai on 9/4/16.
 */
class RouteComponentImpl implements RouteComponent {
    static final Version MULTI_CHANNEL_MATH= new Version("1.1.0"), MULTI_COMPARISON_MIN_FIRMWARE= new Version("1.2.3");

    private enum BranchElement {
        MULTICAST,
        SPLIT
    }

    static class Cache {
        final ArrayList<Tuple3<DataTypeBase, Subscriber, Boolean>> subscribedProducers;
        final ArrayList<Pair<String, Tuple3<DataTypeBase, Integer, byte[]>>> feedback;
        final LinkedList<Pair<? extends DataTypeBase, ? extends Action>> reactions;
        final LinkedList<Processor> dataProcessors;
        public final MetaWearBoardPrivate mwPrivate;
        final Stack<RouteComponentImpl> stashedSignals= new Stack<>();
        final Stack<BranchElement> elements;
        final Stack<Pair<RouteComponentImpl, DataTypeBase[]>> splits;
        final Map<String, Processor> taggedProcessors = new LinkedHashMap<>();

        Cache(MetaWearBoardPrivate mwPrivate) {
            this.subscribedProducers= new ArrayList<>();
            this.feedback = new ArrayList<>();
            this.reactions= new LinkedList<>();
            this.dataProcessors = new LinkedList<>();
            this.mwPrivate = mwPrivate;
            this.elements= new Stack<>();
            this.splits= new Stack<>();
        }
    }

    private final DataTypeBase source;
    Cache persistantData= null;

    RouteComponentImpl(DataTypeBase source) {
        this.source= source;
    }

    RouteComponentImpl(DataTypeBase source, RouteComponentImpl original) {
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
    public RouteComponent to() {
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
    public RouteComponent index(int i) {
        try {
            return new RouteComponentImpl(persistantData.splits.peek().second[i], this);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalRouteOperationException("Index on split data out of bounds", e);
        }
    }

    @Override
    public RouteComponent end() {
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
    public RouteComponent name(String name) {
        if (persistantData.taggedProcessors.containsKey(name)) {
            throw new IllegalRouteOperationException(String.format("Duplicate processor key \'%s\' found", name));
        }

        persistantData.taggedProcessors.put(name, persistantData.dataProcessors.peekLast());
        return this;
    }

    @Override
    public RouteComponent stream(Subscriber subscriber) {
        if (source.attributes.length() > 0) {
            source.markLive();
            persistantData.subscribedProducers.add(new Tuple3<>(source, subscriber, false));
            return this;
        }
        throw new IllegalRouteOperationException("Cannot subscribe to null data");
    }

    @Override
    public RouteComponent log(Subscriber subscriber) {
        if (source.attributes.length() > 0) {
            persistantData.subscribedProducers.add(new Tuple3<>(source, subscriber, true));
            return this;
        }
        throw new IllegalRouteOperationException("Cannot log null data");
    }

    @Override
    public RouteComponent react(Action action) {
        persistantData.reactions.add(new Pair<>(source, action));
        return this;
    }

    @Override
    public RouteComponent buffer() {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot apply \'buffer\' filter to null data");
        }

        DataProcessorConfig config = new DataProcessorConfig.Buffer(source.attributes.length());
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));
        return postCreate(next.second, new NullEditor(config, next.first, persistantData.mwPrivate));
    }

    private static class CounterEditorInner extends EditorImplBase implements DataProcessor.CounterEditor {
        private static final long serialVersionUID = 4873789798519714460L;

        CounterEditorInner(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void reset() {
            mwPrivate.sendCommand(new byte[]{DATA_PROCESSOR.id, DataProcessorImpl.STATE, source.eventConfig[2],
                    0x00, 0x00, 0x00, 0x00});
        }

        @Override
        public void set(int value) {
            ByteBuffer buffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN)
                    .put(DATA_PROCESSOR.id)
                    .put(DataProcessorImpl.STATE)
                    .put(source.eventConfig[2])
                    .putInt(value);
            mwPrivate.sendCommand(buffer.array());
        }
    }
    private static class AccumulatorEditorInner extends EditorImplBase implements DataProcessor.AccumulatorEditor {
        private static final long serialVersionUID = -5044680524938752249L;

        AccumulatorEditorInner(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void reset() {
            mwPrivate.sendCommand(new byte[] {DATA_PROCESSOR.id, DataProcessorImpl.STATE, source.eventConfig[2],
                    0x00, 0x00, 0x00, 0x00});
        }

        @Override
        public void set(Number value) {
            Number scaledValue = source.convertToFirmwareUnits(mwPrivate, value);
            ByteBuffer buffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN)
                    .put(DATA_PROCESSOR.id)
                    .put(DataProcessorImpl.STATE)
                    .put(source.eventConfig[2])
                    .putInt(scaledValue.intValue());

            mwPrivate.sendCommand(buffer.array());
        }
    }
    private RouteComponentImpl createReducer(boolean counter) {
        if (!counter) {
            if (source.attributes.length() <= 0) {
                throw new IllegalRouteOperationException("Cannot accumulate null data");
            }
            if (source.attributes.length() > 4) {
                throw new IllegalRouteOperationException("Cannot accumulate data longer than 4 bytes");
            }
        }

        final byte output= 4;
        DataProcessorConfig config = new DataProcessorConfig.Accumulator(counter, output, source.attributes.length());
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        EditorImplBase editor= counter ?
                new CounterEditorInner(config, next.first, persistantData.mwPrivate) :
                new AccumulatorEditorInner(config, next.first, persistantData.mwPrivate);

        return postCreate(next.second, editor);
    }

    @Override
    public RouteComponent count() {
        return createReducer(true);
    }

    @Override
    public RouteComponent accumulate() {
        return createReducer(false);
    }

    private static class AverageEditorInner extends EditorImplBase implements DataProcessor.AverageEditor {
        private static final long serialVersionUID = 998301829125848762L;

        AverageEditorInner(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void modify(byte samples) {
            config[2]= samples;
            mwPrivate.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }

        @Override
        public void reset() {
            mwPrivate.sendCommand(new byte[] {DATA_PROCESSOR.id, DataProcessorImpl.STATE, source.eventConfig[2]});
        }
    }
    private RouteComponent applyAverager(byte nSamples, boolean hpf, String name) {
        boolean hasHpf = persistantData.mwPrivate.lookupModuleInfo(DATA_PROCESSOR).revision >= DataProcessorImpl.HPF_REVISION;
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException(String.format("Cannot apply %s filter to null data", name));
        }
        if (source.attributes.length() > 4 && !hasHpf) {
            throw new IllegalRouteOperationException(String.format("Cannot apply %s filter to data longer than 4 bytes", name));
        }
        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException(String.format("Cannot apply  %s filter to sensor fusion data", name));
        }

        DataProcessorConfig config = new DataProcessorConfig.Average(source.attributes, nSamples, hpf, hasHpf);
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        return postCreate(next.second, new AverageEditorInner(config, next.first, persistantData.mwPrivate));
    }
    @Override
    public RouteComponent highpass(byte nSamples) {
        if (persistantData.mwPrivate.lookupModuleInfo(DATA_PROCESSOR).revision < DataProcessorImpl.HPF_REVISION) {
            throw new IllegalRouteOperationException("High pass filtering not supported on this firmware version");
        }
        return applyAverager(nSamples, true, "high-pass");
    }
    @Override
    public RouteComponent lowpass(byte nSamples) {
        return applyAverager(nSamples, false, "low-pass");
    }
    @Override
    public RouteComponent average(byte nSamples) {
        return lowpass(nSamples);
    }

    @Override
    public RouteComponent delay(byte samples) {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot delay null data");
        }

        boolean expanded = persistantData.mwPrivate.lookupModuleInfo(DATA_PROCESSOR).revision >= DataProcessorImpl.EXPANDED_DELAY;
        int maxLength = expanded ? 16 : 4;
        if (source.attributes.length() > maxLength) {
            throw new IllegalRouteOperationException(String.format(Locale.US, "Firmware does not support delayed data longer than %d bytes", maxLength));
        }

        DataProcessorConfig config = new DataProcessorConfig.Delay(expanded, source.attributes.length(), samples);
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        return postCreate(next.second, new NullEditor(config, next.first, persistantData.mwPrivate));
    }

    private RouteComponentImpl createCombiner(DataTypeBase source, boolean rss) {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException(String.format(Locale.US, "Cannot apply \'%s\' to null data", !rss ? "rms" : "rss"));
        } else if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException(String.format(Locale.US, "Cannot apply \'%s\' to sensor fusion data", !rss ? "rms" : "rss"));
        }

        // assume sizes array is filled with the same value
        DataProcessorConfig config = new DataProcessorConfig.Combiner(source.attributes, rss);
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next= source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        return postCreate(next.second, new NullEditor(config, next.first, persistantData.mwPrivate));
    }

    @Override
    public RouteComponent map(Function1 fn) {
        switch(fn) {
            case ABS_VALUE:
                return applyMath(DataProcessorConfig.Maths.Operation.ABS_VALUE, 0);
            case RMS:
                if (source instanceof FloatVectorData || source instanceof ColorAdcData) {
                    return createCombiner(source, false);
                }
                return null;
            case RSS:
                if (source instanceof FloatVectorData || source instanceof ColorAdcData) {
                    return createCombiner(source, true);
                }
                return null;
            case SQRT:
                return applyMath(DataProcessorConfig.Maths.Operation.SQRT, 0);
        }
        throw new RuntimeException("Only here so the compiler won't get mad");
    }

    @Override
    public RouteComponent map(Function2 fn, String ... dataNames) {
        RouteComponent next= map(fn, 0);
        if (next != null) {
            for (String key : dataNames) {
                persistantData.feedback.add(new Pair<>(key, new Tuple3<>(((RouteComponentImpl) next).source, 4,
                        persistantData.dataProcessors.peekLast().editor.config)));
            }
        }
        return next;
    }

    @Override
    public RouteComponent map(Function2 fn, Number rhs) {
        switch(fn) {
            case ADD:
                return applyMath(DataProcessorConfig.Maths.Operation.ADD, rhs);
            case MULTIPLY:
                return applyMath(DataProcessorConfig.Maths.Operation.MULTIPLY, rhs);
            case DIVIDE:
                return applyMath(DataProcessorConfig.Maths.Operation.DIVIDE, rhs);
            case MODULUS:
                return applyMath(DataProcessorConfig.Maths.Operation.MODULUS, rhs);
            case EXPONENT:
                return applyMath(DataProcessorConfig.Maths.Operation.EXPONENT, rhs);
            case LEFT_SHIFT:
                return applyMath(DataProcessorConfig.Maths.Operation.LEFT_SHIFT, rhs);
            case RIGHT_SHIFT:
                return applyMath(DataProcessorConfig.Maths.Operation.RIGHT_SHIFT, rhs);
            case SUBTRACT:
                return applyMath(DataProcessorConfig.Maths.Operation.SUBTRACT, rhs);
            case CONSTANT:
                return applyMath(DataProcessorConfig.Maths.Operation.CONSTANT, rhs);
        }
        throw new RuntimeException("Only here so the compiler won't get mad");
    }

    private static class MapEditorInner extends EditorImplBase implements DataProcessor.MapEditor {
        private static final long serialVersionUID = 8475942086629415224L;

        MapEditorInner(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void modifyRhs(Number rhs) {
            final Number scaledRhs;

            switch(DataProcessorConfig.Maths.Operation.values()[config[2] - 1]) {
                case ADD:
                case MODULUS:
                case SUBTRACT:
                    scaledRhs= source.convertToFirmwareUnits(mwPrivate, rhs);
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
            mwPrivate.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    private RouteComponent applyMath(DataProcessorConfig.Maths.Operation op, Number rhs) {
        boolean multiChnlMath= persistantData.mwPrivate.getFirmwareVersion().compareTo(MULTI_CHANNEL_MATH) >= 0;

        if (!multiChnlMath && source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot apply math operations on multi-channel data for firmware prior to " + MULTI_CHANNEL_MATH.toString());
        }

        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot apply math operations to null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot apply math operations to sensor fusion data");
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

        DataProcessorConfig.Maths config = new DataProcessorConfig.Maths(source.attributes, multiChnlMath, op, scaledRhs.intValue());
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));
        config.output = next.first.attributes.sizes[0];
        return postCreate(next.second, new MapEditorInner(config, next.first, persistantData.mwPrivate));
    }

    private static class TimeEditorInner extends EditorImplBase implements DataProcessor.TimeEditor {
        private static final long serialVersionUID = -3775715195615375018L;

        TimeEditorInner(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void modify(int period) {
            byte[] newPeriod= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(period).array();
            System.arraycopy(newPeriod, 0, config, 2, newPeriod.length);

            mwPrivate.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteComponent limit(int period) {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot limit frequency of null data");
        }

        boolean hasTimePassthrough = persistantData.mwPrivate.lookupModuleInfo(DATA_PROCESSOR).revision >= DataProcessorImpl.TIME_PASSTHROUGH_REVISION;
        if (!hasTimePassthrough && source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot limit frequency of sensor fusion data");
        }

        DataProcessorConfig config = new DataProcessorConfig.Time(source.attributes.length(), (byte) (hasTimePassthrough ? 2 : 0), period);
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next= source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        return postCreate(next.second, new TimeEditorInner(config, next.first, persistantData.mwPrivate));
    }

    private static class PassthroughEditorInner extends EditorImplBase implements DataProcessor.PassthroughEditor {
        private static final long serialVersionUID = 3157873682587128118L;

        PassthroughEditorInner(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void set(short value) {
            byte[] newValue= ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();

            mwPrivate.sendCommand(DATA_PROCESSOR, DataProcessorImpl.STATE, source.eventConfig[2], newValue);
        }

        @Override
        public void modify(Passthrough type, short value) {
            byte[] newValue= ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
            System.arraycopy(newValue, 0, config, 2, newValue.length);
            config[1]= (byte) (type.ordinal() & 0x7);

            mwPrivate.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteComponent limit(Passthrough type, short value) {
        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot limit null data");
        }

        DataProcessorConfig config = new DataProcessorConfig.Passthrough(type, value);

        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));
        return postCreate(next.second, new PassthroughEditorInner(config, next.first, persistantData.mwPrivate));
    }

    private static class PulseEditorInner extends EditorImplBase implements DataProcessor.PulseEditor {
        private static final long serialVersionUID = -274897612921984101L;

        PulseEditorInner(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void modify(Number threshold, short samples) {
            byte[] newConfig= ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(source.convertToFirmwareUnits(mwPrivate, threshold).intValue())
                    .putShort(samples)
                    .array();
            System.arraycopy(newConfig, 0, config, 4, newConfig.length);

            mwPrivate.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteComponent find(PulseOutput output, Number threshold, short samples) {
        if (source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot find pulses for data longer than 4 bytes");
        }

        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot find pulses for null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot find pulses for sensor fusion data");
        }

        DataProcessorConfig config = new DataProcessorConfig.Pulse(source.attributes.length(),
                source.convertToFirmwareUnits(persistantData.mwPrivate, threshold).intValue(), samples, output);
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        return postCreate(next.second, new PulseEditorInner(config, next.first, persistantData.mwPrivate));
    }

    @Override
    public RouteComponent filter(Comparison op, String ... dataNames) {
        return filter(op, ComparisonOutput.ABSOLUTE, dataNames);
    }

    @Override
    public RouteComponent filter(Comparison op, Number ... references) {
        return filter(op, ComparisonOutput.ABSOLUTE, references);
    }

    @Override
    public RouteComponent filter(Comparison op, ComparisonOutput output, String ... dataNames) {
        RouteComponent next= filter(op, output, 0);
        if (next != null) {
            for (String key : dataNames) {
                persistantData.feedback.add(new Pair<>(
                        key,
                        new Tuple3<>(
                                ((RouteComponentImpl) next).source,
                                persistantData.mwPrivate.getFirmwareVersion().compareTo(MULTI_COMPARISON_MIN_FIRMWARE) < 0 ? 5 : 3,
                                persistantData.dataProcessors.peekLast().editor.config
                        )
                ));
            }
        }

        return next;
    }

    private static class SingleValueComparatorEditor extends EditorImplBase implements DataProcessor.ComparatorEditor {
        private static final long serialVersionUID = -8891137550284171832L;

        SingleValueComparatorEditor(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void modify(Comparison op, Number... references) {
            byte[] newConfig= ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                    .put((byte) op.ordinal())
                    .put((byte) 0)
                    .putInt(source.convertToFirmwareUnits(mwPrivate, references[0]).intValue())
                    .array();
            System.arraycopy(newConfig, 0, config, 2, newConfig.length);

            mwPrivate.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    private static class MultiValueComparatorEditor extends EditorImplBase implements DataProcessor.ComparatorEditor {
        private static final long serialVersionUID = 893378903150606106L;

        static void fillReferences(DataTypeBase source, MetaWearBoardPrivate mwPrivate, ByteBuffer buffer, byte length, Number... references) {
            switch(length) {
                case 1:
                    for(Number it: references) {
                        buffer.put(source.convertToFirmwareUnits(mwPrivate, it).byteValue());
                    }
                    break;
                case 2:
                    for(Number it: references) {
                        buffer.putShort(source.convertToFirmwareUnits(mwPrivate, it).shortValue());
                    }
                    break;
                case 4:
                    for(Number it: references) {
                        buffer.putInt(source.convertToFirmwareUnits(mwPrivate, it).intValue());
                    }
                    break;
            }
        }


        MultiValueComparatorEditor(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void modify(Comparison op, Number... references) {
            ByteBuffer buffer= ByteBuffer.allocate(references.length * source.attributes.length()).order(ByteOrder.LITTLE_ENDIAN);
            fillReferences(source, mwPrivate, buffer, source.attributes.length(), references);
            byte[] newRef= buffer.array();

            byte[] newConfig= new byte[2 + references.length * source.attributes.length()];
            newConfig[0]= config[0];
            newConfig[1]= (byte) ((config[1] & ~0x38) | (op.ordinal() << 3));

            System.arraycopy(newRef, 0, newConfig, 2, newRef.length);
            config= newConfig;

            mwPrivate.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteComponent filter(Comparison op, ComparisonOutput mode, Number... references) {
        if (source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot compare data longer than 4 bytes");
        }

        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot compare null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot compare sensor sensor fusion data");
        }

        if (persistantData.mwPrivate.getFirmwareVersion().compareTo(MULTI_COMPARISON_MIN_FIRMWARE) < 0) {
            boolean signed= source.attributes.signed || references[0].floatValue() < 0;
            final Number scaledReference= source.convertToFirmwareUnits(persistantData.mwPrivate, references[0]);

            DataProcessorConfig config = new DataProcessorConfig.SingleValueComparison(signed, op, scaledReference.intValue());
            Pair<? extends DataTypeBase, ? extends DataTypeBase> next= source.dataProcessorTransform(config,
                    (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));
            return postCreate(next.second, new SingleValueComparatorEditor(config, next.first, persistantData.mwPrivate));
        }

        boolean anySigned= false;
        Number[] scaled = new Number[references.length];
        for(int i = 0; i < references.length; i++) {
            anySigned|= references[i].floatValue() < 0;
            scaled[i] = source.convertToFirmwareUnits(persistantData.mwPrivate, references[i]);
        }
        boolean signed= source.attributes.signed || anySigned;

        DataProcessorConfig config = new DataProcessorConfig.MultiValueComparison(signed, source.attributes.length(), op, mode, scaled);
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        return postCreate(next.second, new MultiValueComparatorEditor(config, next.first, persistantData.mwPrivate));
    }

    private static class ThresholdEditorInner extends EditorImplBase implements DataProcessor.ThresholdEditor {
        private static final long serialVersionUID = 7819456776691980008L;

        ThresholdEditorInner(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void modify(Number threshold, Number hysteresis) {
            byte[] newConfig= ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(source.convertToFirmwareUnits(mwPrivate, threshold).intValue())
                    .putShort(source.convertToFirmwareUnits(mwPrivate, hysteresis).shortValue())
                    .array();
            System.arraycopy(newConfig, 0, config, 2, newConfig.length);

            mwPrivate.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteComponent filter(ThresholdOutput output, Number threshold) {
        return filter(output, threshold, 0);
    }

    @Override
    public RouteComponent filter(ThresholdOutput mode, Number threshold, Number hysteresis) {
        if (source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot use threshold filter on data longer than 4 bytes");
        }

        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot use threshold filter on null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot use threshold filter on sensor fusion data");
        }

        Number firmwareValue = source.convertToFirmwareUnits(persistantData.mwPrivate, threshold),
                firmwareHysteresis= source.convertToFirmwareUnits(persistantData.mwPrivate, hysteresis);
        DataProcessorConfig config = new DataProcessorConfig.Threshold(source.attributes.length(), source.attributes.signed, mode,
                firmwareValue.intValue(), firmwareHysteresis.shortValue());
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));
        return postCreate(next.second, new ThresholdEditorInner(config, next.first, persistantData.mwPrivate));
    }

    private static class DifferentialEditorInner extends EditorImplBase implements DataProcessor.DifferentialEditor {
        private static final long serialVersionUID = -1856057294039232525L;

        DifferentialEditorInner(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void modify(Number distance) {
            byte[] newDiff= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(source.convertToFirmwareUnits(mwPrivate, distance).intValue()).array();
            System.arraycopy(newDiff, 0, config, 2, newDiff.length);

            mwPrivate.sendCommand(DATA_PROCESSOR, DataProcessorImpl.PARAMETER, source.eventConfig[2], config);
        }
    }
    @Override
    public RouteComponent filter(DifferentialOutput mode, Number distance) {
        if (source.attributes.length() > 4) {
            throw new IllegalRouteOperationException("Cannot use differential filter for data longer than 4 bytes");
        }

        if (source.attributes.length() <= 0) {
            throw new IllegalRouteOperationException("Cannot use differential filter for null data");
        }

        if (source.eventConfig[0] == SENSOR_FUSION.id) {
            throw new IllegalRouteOperationException("Cannot use differential filter on sensor fusion data");
        }


        Number firmwareUnits = source.convertToFirmwareUnits(persistantData.mwPrivate, distance);
        DataProcessorConfig config = new DataProcessorConfig.Differential(source.attributes.length(), source.attributes.signed, mode, firmwareUnits.intValue());
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        return postCreate(next.second, new DifferentialEditorInner(config, next.first, persistantData.mwPrivate));
    }

    private static class PackerEditorInner extends EditorImplBase implements DataProcessor.PackerEditor {
        private static final long serialVersionUID = 9016863579834915719L;

        PackerEditorInner(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }

        @Override
        public void clear() {
            mwPrivate.sendCommand(new byte[] {DATA_PROCESSOR.id, DataProcessorImpl.STATE, source.eventConfig[2]});
        }
    }
    @Override
    public RouteComponent pack(byte count) {
        if (persistantData.mwPrivate.lookupModuleInfo(DATA_PROCESSOR).revision < DataProcessorImpl.ENHANCED_STREAMING_REVISION) {
            throw new IllegalRouteOperationException("Current firmware does not support data packing");
        }

        if (source.attributes.length() * count + 3 > MAX_BTLE_LENGTH) {
            throw new IllegalRouteOperationException("Not enough space in the ble packet to pack " + count + " data samples");
        }

        DataProcessorConfig config = new DataProcessorConfig.Packer(source.attributes.length(), count);
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        return postCreate(next.second, new PackerEditorInner(config, next.first, persistantData.mwPrivate));
    }

    @Override
    public RouteComponent account() {
        return account(AccountType.TIME);
    }

    @Override
    public RouteComponent account(AccountType type) {
        if (persistantData.mwPrivate.lookupModuleInfo(DATA_PROCESSOR).revision < DataProcessorImpl.ENHANCED_STREAMING_REVISION) {
            throw new IllegalRouteOperationException("Current firmware does not support data accounting");
        }

        final byte size = (byte) (type == AccountType.TIME ? 4 : Math.min(4, Constant.MAX_BTLE_LENGTH - 3 - source.attributes.length()));
        if (type == AccountType.TIME && source.attributes.length() + size + 3 > Constant.MAX_BTLE_LENGTH || type == AccountType.COUNT && size < 0) {
            throw new IllegalRouteOperationException("Not enough space left in the ble packet to add accounter information");
        }

        DataProcessorConfig config = new DataProcessorConfig.Accounter(size, type);
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        return postCreate(next.second, new NullEditor(config, next.first, persistantData.mwPrivate));
    }

    @Override
    public RouteComponent fuse(String... bufferNames) {
        if (persistantData.mwPrivate.lookupModuleInfo(DATA_PROCESSOR).revision < DataProcessorImpl.FUSE_REVISION) {
            throw new IllegalRouteOperationException("Current firmware does not support data fusing");
        }

        DataProcessorConfig config = new DataProcessorConfig.Fuser(bufferNames);
        Pair<? extends DataTypeBase, ? extends DataTypeBase> next = source.dataProcessorTransform(config,
                (DataProcessorImpl) persistantData.mwPrivate.getModules().get(DataProcessor.class));

        return postCreate(next.second, new NullEditor(config, next.first, persistantData.mwPrivate));
    }

    private RouteComponentImpl postCreate(DataTypeBase state, EditorImplBase editor) {
        persistantData.dataProcessors.add(new Processor(state, editor));
        return new RouteComponentImpl(editor.source, this);
    }
}

