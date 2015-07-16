/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.impl;

import com.mbientlab.metawear.AsyncResult;
import com.mbientlab.metawear.DataProcessor;
import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MessageToken;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteBuilder;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.Subscription;
import com.mbientlab.metawear.impl.characteristic.*;
import com.mbientlab.metawear.module.*;
import com.mbientlab.metawear.processor.*;
import com.mbientlab.metawear.data.*;

import java.io.UnsupportedEncodingException;
import java.lang.Math;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by etsai on 6/15/2015.
 */
public abstract class DefaultMetaWearBoard implements MetaWearBoard, Connection.ResponseListener {
    public static final byte MW_COMMAND_LENGTH = 18;

    private static String arrayToHexString(byte[] value) {
        if (value.length == 0) {
            return "[]";
        }

        StringBuilder builder= new StringBuilder();
        builder.append(String.format("[%02x", value[0]));
        for(int i= 1; i < value.length; i++) {
            builder.append(String.format(", %02x", value[i]));
        }
        builder.append("]");

        return builder.toString();
    }

    private static Map<String, String> parseQuery(String query) {
        HashMap<String, String> queryTokens= new HashMap<>();

        for(String token: query.split("&")) {
            String[] keyVal= token.split("=");
            queryTokens.put(keyVal[0], keyVal[1]);
        }

        return queryTokens;
    }

    private static int closestIndex(float[] values, float key) {
        float boundedKey= Math.min(Math.max(values[0], key), values[values.length - 1]);
        byte i;
        for(i= 0; i < values.length - 1; i++) {
            if (values[i] <= boundedKey && values[i + 1] >= boundedKey) {
                break;
            }
        }

        if (i == values.length - 1) {
            return values.length - 1;
        }

        float leftDist= boundedKey - values[i], rightDist= values[i + 1] - boundedKey;
        if (leftDist < rightDist) {
            return i;
        } else {
            return i + 1;
        }
    }

    private static final HashMap<String, Class<? extends DataSignal.ProcessorConfig>> processorSchemes;

    static {
        processorSchemes= new HashMap<>();
        processorSchemes.put(Accumulator.SCHEME_NAME, Accumulator.class);
        processorSchemes.put(Average.SCHEME_NAME, Average.class);
        processorSchemes.put(Delta.SCHEME_NAME, Delta.class);
        processorSchemes.put(com.mbientlab.metawear.processor.Math.SCHEME_NAME, com.mbientlab.metawear.processor.Math.class);
        processorSchemes.put(Rms.SCHEME_NAME, Rms.class);
        processorSchemes.put(Rss.SCHEME_NAME, Rss.class);
        processorSchemes.put(Time.SCHEME_NAME, Time.class);
        processorSchemes.put(Threshold.SCHEME_NAME, Threshold.class);
        processorSchemes.put(Comparison.SCHEME_NAME, Comparison.class);
        processorSchemes.put(Passthrough.SCHEME_NAME, Passthrough.class);
        processorSchemes.put(Sample.SCHEME_NAME, Sample.class);
    }

    protected final Connection conn;

    private interface IdCreator {
        public void execute();
        public void receivedId(byte id);
    }

    private interface Loggable {
        public void receivedLogEntry(byte[] entry);
    }

    private interface EventListener {
        public void receivedCommandId(byte id);
        public void onCommandWrite();
    }

    private interface ReferenceTick {
        public long tickCount();
        public Calendar timestamp();
    }

    private interface RouteHandler {
        public void addId();
        public void receivedId(byte id);
    }

    private class RouteBuilderImpl implements RouteBuilder, EventListener, RouteHandler {
        private AsyncResultImpl<RouteManager> commitResult;
        private DataSource routeSource;
        private boolean hasDataLogger= false;
        private byte nExpectedCmds= 0;
        private final Stack<DataSignalImpl> branches= new Stack<>();
        private final Queue<IdCreator> creators= new LinkedList<>();
        private final HashMap<DataSignalImpl, DataSignal.ActivityMonitor> signalMonitors= new HashMap<>();
        private final LinkedHashSet<DataSignalImpl> activeSignals= new LinkedHashSet<>();
        private final HashSet<Byte> filterIds= new HashSet<>(), eventCmdIds= new HashSet<>(), loggingIds= new HashSet<>();
        private final HashMap<String, DataProcessor> dataProcessors= new HashMap<>();
        private final HashMap<String, Subscription> dataSubscriptions= new HashMap<>();

        private abstract class DataSignalImpl implements DataSignal, Loggable, MessageToken, Subscription {
            protected DataSignalImpl parent;
            protected Class<? extends Message> msgClass;
            protected final byte outputSize;
            protected MessageProcessor logProcessor;
            private boolean subscribed;

            protected DataSignalImpl(byte outputSize, Class<? extends Message> msgClass) {
                this.outputSize= outputSize;
                this.msgClass= msgClass;
            }

            @Override
            public boolean isSubscribed() {
                return subscribed;
            }

            @Override
            public void unsubscribe() {
                subscribed= false;
            }

            @Override
            public DataSignal split() {
                branches.push(this);
                return this;
            }

            @Override
            public DataSignal branch() {
                return branches.isEmpty() ? null : branches.peek();
            }

            @Override
            public DataSignal end() {
                branches.pop();
                return branches.isEmpty() ? new DataSignal() {
                    @Override
                    public DataSignal split() {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal branch() {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal end() {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal log(MessageProcessor processor) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal subscribe(MessageProcessor processor) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal subscribe(String key, MessageProcessor processor) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal monitor(ActivityMonitor monitor) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal process(String key, ProcessorConfig config) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal process(ProcessorConfig config) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal process(String configUri) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal process(String key, String configUri) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public AsyncResult<RouteManager> commit() {
                        return DataSignalImpl.this.commit();
                    }
                }
                : branches.peek();
            }

            @Override
            public DataSignal process(String key, ProcessorConfig config) {
                ProcessedDataSignal newDataSignal= (ProcessedDataSignal) process(config, this);
                if (dataProcessors.containsKey(key)) {
                    throw new RuntimeException("Processor configuration key \'" + key + "\' already present");
                }
                dataProcessors.put(key, newDataSignal);
                return newDataSignal;
            }

            @Override
            public DataSignal process(ProcessorConfig config) {
                return process(config, this);
            }

            @Override
            public DataSignal process(String configUri) {
                String[] uriSplit= configUri.split("\\?");

                if (processorSchemes.containsKey(uriSplit[0])) {
                    try {
                        Map<String, String> query= uriSplit.length > 1 ? parseQuery(uriSplit[1]) : new HashMap<String, String>();
                        Constructor<?> cTor = processorSchemes.get(uriSplit[0]).getConstructor(Map.class);
                        return process((ProcessorConfig) cTor.newInstance(query));
                    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException("Error instantiating data filter \'" + uriSplit[0] + "\'", e);
                    }
                } else {
                    throw new RuntimeException("Processor configuration scheme \'" + uriSplit[0] + "\' not recognized");
                }
            }

            @Override
            public DataSignal process(String key, String configUri) {
                ProcessedDataSignal newDataSignal= (ProcessedDataSignal) process(configUri);
                if (dataProcessors.containsKey(key)) {
                    throw new RuntimeException("Processor configuration key \'" + key + "\' already present");
                }
                dataProcessors.put(key, newDataSignal);
                return newDataSignal;
            }

            protected void postFilterCreate(final ProcessedDataSignal newProcessor, final DataSignalImpl parent) {
                newProcessor.parent= parent;
                creators.add(new IdCreator() {
                    @Override
                    public void execute() {
                        byte[] nextCfg = newProcessor.getFilterConfig();
                        byte[] parentCfg= parent.getTriggerConfig();
                        byte[] addFilter = new byte[parentCfg.length + nextCfg.length];
                        System.arraycopy(parentCfg, 0, addFilter, 0, parentCfg.length);
                        System.arraycopy(nextCfg, 0, addFilter, parentCfg.length, nextCfg.length);
                        writeRegister(DataProcessorRegister.ADD, addFilter);
                    }

                    @Override
                    public void receivedId(byte id) {
                        filterIds.add(id);
                        newProcessor.setId(id);
                    }
                });
            }

            @Override
            public DataSignal log(MessageProcessor processor) {
                return log(processor, this);
            }

            @Override
            public DataSignal subscribe(MessageProcessor processor) {
                activeSignals.add(this);
                return this;
            }

            @Override
            public DataSignal subscribe(String key, MessageProcessor processor) {
                subscribe(processor);
                dataSubscriptions.put(key, this);
                return this;
            }

            @Override
            public DataSignal monitor(ActivityMonitor monitor) {
                signalMonitors.put(this, monitor);
                return this;
            }

            @Override
            public AsyncResult<RouteManager> commit() {
                commitResult= new AsyncResultImpl<>();

                if (isConnected()) {
                    if (hasDataLogger) {
                        writeRegister(LoggingRegister.ENABLE, (byte) 1);
                    }

                    pendingRoutes.add(RouteBuilderImpl.this);
                    if (!commitRoutes.get()) {
                        commitRoutes.set(true);
                        pendingRoutes.peek().addId();
                    }
                } else {
                    commitResult.setResult(null, new RuntimeException("Not connected to a MetaWear board"));
                }

                return commitResult;
            }

            @Override
            public void receivedLogEntry(byte[] entry) {
                long tick= ByteBuffer.wrap(Arrays.copyOfRange(entry, 1, 5)).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL;

                final double TICK_TIME_STEP= (48.0 / 32768.0) * 1000.0;
                Calendar timestamp= (Calendar) logReferenceTick.timestamp().clone();
                timestamp.add(Calendar.MILLISECOND, (int) ((tick - logReferenceTick.tickCount()) * TICK_TIME_STEP));

                final byte[] logEntryData= Arrays.copyOfRange(entry, 5, entry.length);

                try {
                    Message logMsg;

                    if (msgClass.equals(Bmi160AccelAxisMessage.class)) {
                        logMsg= new Bmi160AccelAxisMessage(timestamp, logEntryData, bmi160AccRange.scale());
                    } else if (msgClass.equals(Bmi160GyroMessage.class)) {
                        logMsg= new Bmi160GyroMessage(timestamp, logEntryData, bmi160GyroRange.scale());
                    } else {
                        Constructor<?> cTor= msgClass.getConstructor(Calendar.class, byte[].class);
                        logMsg= (Message) cTor.newInstance(timestamp, logEntryData);
                    }

                    logProcessor.process(logMsg);
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException ex) {
                    throw new RuntimeException("Cannot instantiate message class", ex);
                }
            }

            @Override
            public byte length() {
                return outputSize;
            }

            @Override
            public byte offset() {
                return 0;
            }

            protected DataSignal log(MessageProcessor processor, final DataSignalImpl source) {
                creators.add(new IdCreator() {
                    @Override
                    public void execute() {
                        writeRegister(LoggingRegister.TRIGGER, source.getTriggerConfig());
                    }

                    @Override
                    public void receivedId(byte id) {
                        loggingIds.add(id);
                        dataLoggers.put(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, id), DataSignalImpl.this);
                    }
                });
                logProcessor= processor;
                hasDataLogger= true;
                return this;
            }

            protected DataSignal process(ProcessorConfig config, final DataSignalImpl parent) {
                ProcessedDataSignal newProcessor;

                if (config instanceof Passthrough) {
                    final Passthrough params= (Passthrough) config;
                    newProcessor= new StaticProcessedDataSignal(outputSize, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            return processorConfigToBytes(params);
                        }

                        @Override
                        protected byte[] processorConfigToBytes(ProcessorConfig newConfig) {
                            Passthrough passthroughConfig= (Passthrough) newConfig;
                            ByteBuffer buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0x1)
                                    .put((byte) (passthroughConfig.passthroughMode.ordinal() & 0x7))
                                    .putShort(passthroughConfig.value);
                            return buffer.array();
                        }

                        @Override
                        public void setState(StateEditor editor) {
                            if (!(editor instanceof Passthrough.PassthroughStateEditor)) {
                                throw new ClassCastException("Passthrough filter can only be configured with a PassthroughState object");
                            }
                            super.setState(editor);
                        }

                        @Override
                        public void modifyConfiguration(ProcessorConfig newConfig) {
                            if (!(newConfig instanceof Passthrough)) {
                                throw new ClassCastException("Can only swap the current configuration with another passthrough configuration");
                            }

                            super.modifyConfiguration(newConfig);
                        }
                    };
                } else if (config instanceof Comparison) {
                    final Comparison params= (Comparison) config;
                    newProcessor= new StaticProcessedDataSignal(outputSize, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            return processorConfigToBytes(params);
                        }

                        @Override
                        protected byte[] processorConfigToBytes(ProcessorConfig newFilter) {
                            Comparison comparisonConfig= (Comparison) newFilter;
                            boolean signed= comparisonConfig.signed == null ? isSigned() : comparisonConfig.signed;
                            Number firmwareReference= numberToFirmwareUnits(comparisonConfig.reference);
                            ByteBuffer buffer= ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0x6)
                                    .put((byte) (signed ? 1 : 0)).put((byte) comparisonConfig.compareOp.ordinal()).put((byte) 0)
                                    .putInt(firmwareReference.intValue());
                            return buffer.array();
                        }

                        @Override
                        public void modifyConfiguration(ProcessorConfig newConfig) {
                            if (!(newConfig instanceof Comparison)) {
                                throw new ClassCastException("Can only swap the current configuration with another comparison configuration");
                            }

                            Comparison newCompFilter= (Comparison) newConfig;

                            if (newCompFilter.referenceToken != null) {
                                eventParams= newCompFilter.referenceToken;
                                eventDestOffset= 5;
                            }
                            super.modifyConfiguration(newConfig);
                            eventParams= null;
                        }

                        @Override
                        public void setState(StateEditor editor) {
                            throw new UnsupportedOperationException("Cannot change comparison filter state");
                        }
                    };
                } else if (config instanceof Time) {
                    final Time params = (Time) config;
                    newProcessor = new StaticProcessedDataSignal(outputSize, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            return processorConfigToBytes(params);
                        }

                        @Override
                        protected byte[] processorConfigToBytes(ProcessorConfig newConfig) {
                            ///< Do not allow time mode to be changed
                            Time timeConfig = (Time) newConfig;
                            ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0x8)
                                    .put((byte) ((outputSize - 1) | (params.mode.ordinal() << 3))).putInt(timeConfig.period);
                            return buffer.array();
                        }

                        @Override
                        public void setState(StateEditor editor) {
                            throw new UnsupportedOperationException("Cannot change time processor state");
                        }

                        @Override
                        public void modifyConfiguration(ProcessorConfig newConfig) {
                            if (!(newConfig instanceof Time)) {
                                throw new ClassCastException("Can only swap the current configuration with another time configuration");
                            }

                            super.modifyConfiguration(newConfig);
                        }

                        @Override
                        public boolean isSigned() {
                            return (params.mode == Time.Mode.DIFFERENTIAL) || super.isSigned();
                        }
                    };
                } else if (config instanceof Sample) {
                    final Sample params = (Sample) config;
                    newProcessor = new StaticProcessedDataSignal(outputSize, msgClass) {
                        @Override
                        public void setState(StateEditor editor) {
                            throw new UnsupportedOperationException("Cannot change sample processor state");
                        }

                        @Override
                        public byte[] getFilterConfig() {
                            return processorConfigToBytes(params);
                        }

                        @Override
                        protected byte[] processorConfigToBytes(ProcessorConfig newConfig) {
                            Sample sampleConfig = (Sample) newConfig;
                            return new byte[]{0xa, (byte) (outputSize & 0x3), sampleConfig.binSize};
                        }

                        @Override
                        public void modifyConfiguration(ProcessorConfig newConfig) {
                            if (!(newConfig instanceof Sample)) {
                                throw new ClassCastException("Can only swap the current configuration with another sample configuration");
                            }

                            super.modifyConfiguration(newConfig);
                        }
                    };
                } else if (config instanceof Threshold) {
                    final Threshold params = (Threshold) config;
                    final byte size= (params.mode == Threshold.Mode.BINARY) ? 1 : outputSize;
                    final Class<? extends Message> nextClass= (params.mode == Threshold.Mode.BINARY) ? Message.class : msgClass;

                    newProcessor = new StaticProcessedDataSignal(size, nextClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            return processorConfigToBytes(params);
                        }

                        @Override
                        protected byte[] processorConfigToBytes(ProcessorConfig newConfig) {
                            Threshold thsConfig = (Threshold) newConfig;

                            ///< Do not allow the threshold mode to be changed
                            byte second = (byte) ((DataSignalImpl.this.outputSize - 1) & 0x3 | (isSigned() ? 0x4 : 0) |
                                    (params.mode.ordinal() << 3));
                            ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
                            Number firmwareValue = numberToFirmwareUnits(thsConfig.limit), firmwareHysteresis= numberToFirmwareUnits(thsConfig.hysteresis);
                            buffer.put((byte) 0xd).put(second).putInt(firmwareValue.intValue()).putShort(firmwareHysteresis.shortValue());

                            return buffer.array();
                        }

                        @Override
                        public void setState(StateEditor editor) {
                            throw new UnsupportedOperationException("Cannot change threshold filter state");
                        }

                        @Override
                        public void modifyConfiguration(ProcessorConfig newConfig) {
                            if (!(newConfig instanceof Threshold)) {
                                throw new ClassCastException("Can only swap the current configuration with another threshold configuration");
                            }
                            super.modifyConfiguration(newConfig);
                        }

                        @Override
                        public boolean isSigned() {
                            return (params.mode == Threshold.Mode.BINARY) || super.isSigned();
                        }
                    };
                } else if (config instanceof Delta) {
                    final Delta params = (Delta) config;
                    final byte size = (params.mode == Delta.Mode.BINARY) ? 1 : outputSize;
                    final Class<? extends Message> nextClass = (params.mode == Delta.Mode.BINARY) ? Message.class : msgClass;

                    newProcessor = new StaticProcessedDataSignal(size, nextClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            return processorConfigToBytes(params);
                        }

                        @Override
                        protected byte[] processorConfigToBytes(ProcessorConfig newConfig) {
                            Delta deltaConfig = (Delta) newConfig;

                            ///< Do not allow the delta mode to be changed
                            byte second = (byte) (((this.outputSize - 1) & 0x3) | (isSigned() ? 0x4 : 0) |
                                    (params.mode.ordinal() << 3));
                            Number firmware = numberToFirmwareUnits(deltaConfig.threshold);
                            ByteBuffer config = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0xc).put(second).putInt(firmware.intValue());
                            return config.array();

                        }

                        @Override
                        public void setState(StateEditor editor) {
                            if (!(editor instanceof Delta.DeltaStateEditor)) {
                                throw new ClassCastException("Delta transformer can only be configured with a DeltaState object");
                            }
                            super.setState(editor);
                        }

                        @Override
                        public void modifyConfiguration(ProcessorConfig newConfig) {
                            if (!(newConfig instanceof Delta)) {
                                throw new ClassCastException("Can only swap the current configuration with another delta configuration");
                            }
                            super.modifyConfiguration(newConfig);

                        }

                        @Override
                        public boolean isSigned() {
                            return (params.mode != Delta.Mode.ABSOLUTE) || super.isSigned();
                        }
                    };
                } else if (config instanceof Accumulator) {
                    final Accumulator params = (Accumulator) config;
                    final byte filterOutputSize = params.output == null ? outputSize : params.output;

                    newProcessor = new ProcessedDataSignal(filterOutputSize, msgClass) {
                        @Override
                        protected byte[] processorConfigToBytes(ProcessorConfig newConfig) {
                            return new byte[]{0x2, (byte) (((this.outputSize - 1) & 0x3) | (((parent.outputSize - 1) & 0x3) << 2))};
                        }

                        @Override
                        public boolean isSigned() {
                            return parent.isSigned();
                        }

                        @Override
                        public Number numberToFirmwareUnits(Number input) {
                            return parent.numberToFirmwareUnits(input);
                        }

                        @Override
                        public void setState(StateEditor editor) {
                            if (!(editor instanceof Accumulator.AccumulatorStateEditor)) {
                                throw new RuntimeException("Accumulator transformer can only be configured with an AccumulatorState object");
                            }
                            super.setState(editor);
                        }

                        @Override
                        public byte[] getFilterConfig() {
                            return processorConfigToBytes(params);
                        }

                        @Override
                        public void modifyConfiguration(ProcessorConfig newConfig) {
                            throw new UnsupportedOperationException("Cannot change accumulator configuration");
                        }
                    };
                } else if (config instanceof Average) {
                    final Average params = (Average) config;
                    newProcessor = new ProcessedDataSignal(outputSize, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            return processorConfigToBytes(params);
                        }

                        @Override
                        protected byte[] processorConfigToBytes(ProcessorConfig newConfig) {
                            Average avgConfig = (Average) newConfig;
                            return new byte[]{0x3, (byte) (((outputSize - 1) & 0x3) | (((outputSize - 1) & 0x3) << 2)),
                                    avgConfig.sampleSize};
                        }

                        @Override
                        public boolean isSigned() {
                            return parent.isSigned();
                        }

                        @Override
                        public Number numberToFirmwareUnits(Number input) {
                            return parent.numberToFirmwareUnits(input);
                        }

                        @Override
                        public void setState(StateEditor editor) {
                            if (!(editor instanceof Average.AverageStateEditor)) {
                                throw new RuntimeException("Average transformer can only be configured with an AverageState object");
                            }
                            super.setState(editor);
                        }

                        @Override
                        public void modifyConfiguration(ProcessorConfig newConfig) {
                            if (!(newConfig instanceof Average)) {
                                throw new ClassCastException("Can only swap the current configuration with another average configuration");
                            }

                            super.modifyConfiguration(newConfig);
                        }
                    };
                } else if (config instanceof com.mbientlab.metawear.processor.Math) {
                    final com.mbientlab.metawear.processor.Math params = (com.mbientlab.metawear.processor.Math) config;
                    newProcessor = new ProcessedDataSignal((byte) 4, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            return processorConfigToBytes(params);
                        }

                        @Override
                        protected byte[] processorConfigToBytes(ProcessorConfig newConfig) {
                            com.mbientlab.metawear.processor.Math mathConfig = (com.mbientlab.metawear.processor.Math) newConfig;
                            byte signedMask = (byte) (mathConfig.signed == null ? (isSigned() ? 0x10 : 0x0) : (mathConfig.signed ? 0x10 : 0x0));
                            Number firmwareRhs;

                            switch (mathConfig.mathOp) {
                                case ADD:
                                case SUBTRACT:
                                case MODULUS:
                                    firmwareRhs = numberToFirmwareUnits(mathConfig.rhs);
                                    break;
                                default:
                                    firmwareRhs = mathConfig.rhs;
                            }

                            ///< Do not allow the math operation to be changed
                            ByteBuffer buffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0x9)
                                    .put((byte) ((this.outputSize - 1) & 0x3 | ((parent.outputSize - 1) << 2) | signedMask))
                                    .put((byte) params.mathOp.ordinal()).putInt(firmwareRhs.intValue());
                            return buffer.array();
                        }

                        @Override
                        public boolean isSigned() {
                            return !(params.mathOp == com.mbientlab.metawear.processor.Math.Operation.ABS_VALUE ||
                                    params.mathOp == com.mbientlab.metawear.processor.Math.Operation.SQRT);
                        }

                        @Override
                        public Number numberToFirmwareUnits(Number input) {
                            return parent.numberToFirmwareUnits(input);
                        }

                        @Override
                        public void setState(StateEditor editor) {
                            throw new UnsupportedOperationException("Cannot change math transformer state");
                        }

                        @Override
                        public void modifyConfiguration(ProcessorConfig newConfig) {
                            if (!(newConfig instanceof com.mbientlab.metawear.processor.Math)) {
                                throw new ClassCastException("Can only swap the current configuration with another math configuration");
                            }

                            com.mbientlab.metawear.processor.Math newMathTransformer = (com.mbientlab.metawear.processor.Math) newConfig;
                            if (newMathTransformer.rhsToken != null) {
                                eventParams = newMathTransformer.rhsToken;
                                eventDestOffset = 4;
                            }

                            super.modifyConfiguration(newConfig);
                            eventParams = null;
                        }
                    };
                } else if (config instanceof Rms) {
                    throw new UnsupportedOperationException("Cannot attach rms transformer to single component data");
                } else if (config instanceof Rss) {
                    throw new UnsupportedOperationException("Cannot attach rss transformer to single component data");
                } else {
                    throw new ClassCastException("Unrecognized DataFilter subtype: \'" + config.getClass().toString() + "\'");
                }

                postFilterCreate(newProcessor, parent);
                return newProcessor;
            }

            public abstract boolean isSigned();
            public abstract void enableNotifications();
            public abstract byte[] getEventConfig();

            public Number numberToFirmwareUnits(Number input) {
                return input;
            }

            public byte[] getTriggerConfig() {
                byte[] eventCfg= getEventConfig();
                byte[] triggerCfg= new byte[eventCfg.length + 1];

                System.arraycopy(eventCfg, 0, triggerCfg, 0, eventCfg.length);
                triggerCfg[eventCfg.length]= (byte) ((outputSize - 1) << 5);
                return triggerCfg;
            }
        }

        public abstract class DataSource extends DataSignalImpl implements RouteManager {
            protected DataSource(byte outputSize, Class<? extends Message> msgClass) {
                super(outputSize, msgClass);
            }

            @Override
            public void remove() {
                for(Byte id: filterIds) {
                    writeRegister(DataProcessorRegister.REMOVE, id);
                    responseProcessors.remove(new ResponseHeader(DataProcessorRegister.NOTIFY, id));
                }

                for(Byte id: eventCmdIds) {
                    writeRegister(EventRegister.REMOVE, id);
                }

                for(Byte id: loggingIds) {
                    writeRegister(LoggingRegister.REMOVE, id);
                    dataLoggers.remove(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, id));
                }
            }

            @Override
            public DataProcessor getDataProcessor(String procKey) {
                return dataProcessors.get(procKey);
            }

            @Override
            public Subscription getSubscription(String subKey) {
                return dataSubscriptions.get(subKey);
            }
        }

        public abstract class ProcessedDataSignal extends DataSignalImpl implements DataProcessor {
            private byte id= -1;
            private MessageProcessor processor;

            protected ProcessedDataSignal(byte outputSize, Class<? extends Message> msgClass) {
                super(outputSize, msgClass);
            }

            @Override
            public DataSignal subscribe(MessageProcessor processor) {
                this.processor= processor;
                return super.subscribe(processor);
            }

            @Override
            public void enableNotifications() {
                writeRegister(DataProcessorRegister.NOTIFY, (byte) 0x1);
                writeRegister(DataProcessorRegister.NOTIFY_ENABLE, id, (byte) 0x1);

                ResponseHeader header= new ResponseHeader(DataProcessorRegister.NOTIFY, id);
                responseProcessors.put(header, processor);
                dataProcMsgClasses.put(header, msgClass);
            }

            @Override
            public void unsubscribe() {
                writeRegister(DataProcessorRegister.NOTIFY_ENABLE, id, (byte) 0);
                super.unsubscribe();
            }

            @Override
            public byte[] getEventConfig() {
                if (id == -1) {
                    throw new RuntimeException("ID has not been set yet");
                }
                return new byte[] {DataProcessorRegister.NOTIFY.moduleOpcode(), DataProcessorRegister.NOTIFY.opcode(), id};
            }

            @Override
            public void setState(StateEditor editor) {
                byte[] stateBytes;

                if (editor instanceof Accumulator.AccumulatorStateEditor) {
                    Number firmware= numberToFirmwareUnits(((Accumulator.AccumulatorStateEditor) editor).newRunningSum);

                    stateBytes= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(firmware.intValue()).array();
                } else if (editor instanceof Average.AverageStateEditor) {
                    stateBytes= new byte[0];
                } else if (editor instanceof Delta.DeltaStateEditor) {
                    Number firmware= numberToFirmwareUnits(((Delta.DeltaStateEditor) editor).newPreviousValue);

                    stateBytes= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(firmware.intValue()).array();
                } else if (editor instanceof Passthrough.PassthroughStateEditor) {
                    stateBytes= ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                            .putShort(((Passthrough.PassthroughStateEditor) editor).newValue).array();
                } else {
                    throw new ClassCastException("Unrecognized state editor: \'" + editor.getClass() + "\'");
                }

                byte[] parameters= new byte[stateBytes.length + 1];
                System.arraycopy(stateBytes, 0, parameters, 1, stateBytes.length);
                parameters[0]= id;
                writeRegister(DataProcessorRegister.STATE, parameters);
            }

            public void setId(byte newId) {
                this.id= newId;
            }

            public abstract byte[] getFilterConfig();
            protected abstract byte[] processorConfigToBytes(ProcessorConfig newConfig);

            @Override
            public void modifyConfiguration(ProcessorConfig newConfig) {
                byte[] configBytes= processorConfigToBytes(newConfig);

                byte[] modifyFilter = new byte[configBytes.length + 1];
                modifyFilter[0]= id;
                System.arraycopy(configBytes, 0, modifyFilter, 1, configBytes.length);

                writeRegister(DataProcessorRegister.PARAMETER, modifyFilter);
            }
        }
        public abstract class StaticProcessedDataSignal extends ProcessedDataSignal {
            public StaticProcessedDataSignal(byte outputSize, Class<? extends Message> msgClass) {
                super(outputSize, msgClass);
            }

            @Override
            public DataSignal process(ProcessorConfig config) {
                return parent.process(config, this);
            }

            @Override
            public DataSignal log(MessageProcessor processor) {
                parent.log(processor, this);
                return this;
            }

            @Override
            public void receivedLogEntry(byte[] entry) {
                parent.receivedLogEntry(entry);
            }

            @Override
            public Number numberToFirmwareUnits(Number input) {
                return parent.numberToFirmwareUnits(input);
            }

            @Override
            public boolean isSigned() {
                return parent.isSigned();
            }
        }

        @Override
        public DataSignal fromSwitch() {
            routeSource= new DataSource((byte) 1, SwitchMessage.class) {
                @Override
                public DataSignal subscribe(MessageProcessor processor) {
                    responseProcessors.put(new ResponseHeader(SwitchRegister.STATE), processor);
                    return super.subscribe(processor);
                }

                @Override
                public void remove() {
                    responseProcessors.remove(new ResponseHeader(SwitchRegister.STATE));
                    super.remove();
                }

                @Override
                public boolean isSigned() {
                    return false;
                }

                @Override
                public void enableNotifications() {
                    writeRegister(SwitchRegister.STATE, (byte) 1);
                }

                @Override
                public byte[] getEventConfig() {
                    return new byte[] {SwitchRegister.STATE.moduleOpcode(), SwitchRegister.STATE.opcode(), (byte) 0xff};
                }

                @Override
                public void unsubscribe() {
                    writeRegister(SwitchRegister.STATE, (byte) 0);
                    super.unsubscribe();
                }
            };

            return routeSource;
        }

        @Override
        public DataSignal fromTemperature() {
            routeSource= new DataSource((byte) 2, TemperatureMessage.class) {
                private MessageProcessor processor;

                @Override
                public DataSignal subscribe(MessageProcessor processor) {
                    this.processor= processor;
                    responseProcessors.put(new ResponseHeader(TemperatureRegister.VALUE), processor);
                    return super.subscribe(processor);
                }

                @Override
                public void remove() {
                    responseProcessors.remove(new ResponseHeader(TemperatureRegister.VALUE));
                    super.remove();
                }

                @Override
                public Number numberToFirmwareUnits(Number input) {
                    return input.floatValue() * 8.0f;
                }

                @Override
                public void unsubscribe() {
                    responseProcessors.remove(new ResponseHeader(TemperatureRegister.VALUE));
                    super.unsubscribe();
                }

                @Override
                public boolean isSigned() {
                    return true;
                }

                @Override
                public void enableNotifications() { }

                @Override
                public byte[] getEventConfig() {
                    return new byte[] {TemperatureRegister.VALUE.moduleOpcode(), (byte) (0x80 | TemperatureRegister.VALUE.opcode()), (byte) 0xff};
                }
            };

            return routeSource;
        }

        private abstract class AccelerometerDataSource extends DataSource {
            private final byte XY_LENGTH = 4, Z_OFFSET = XY_LENGTH;
            private byte xyId = -1, zId = -1;
            private Queue<byte[]> xyLogEntries = new LinkedList<>(), zLogEntries = new LinkedList<>();

            private AccelerometerDataSource(byte outputSize, Class<? extends Message> msgClass) {
                super(outputSize, msgClass);
            }

            @Override
            public boolean isSigned() {
                throw new UnsupportedOperationException("isSigned method not supported for raw accelerometer axis data");
            }

            @Override
            protected DataSignal process(ProcessorConfig config, final DataSignalImpl parent) {
                if (config instanceof Comparison) {
                    throw new UnsupportedOperationException("Cannot compare raw accelerometer axis data");
                } else if (config instanceof Sample) {
                    throw new UnsupportedOperationException("Cannot attach sample filter to raw accelerometer axis data");
                } else if (config instanceof Accumulator) {
                    throw new UnsupportedOperationException("Cannot accumulate raw accelerometer axis data");
                } else if (config instanceof Average) {
                    throw new UnsupportedOperationException("Cannot average raw accelerometer axis data");
                } else if (config instanceof com.mbientlab.metawear.processor.Math) {
                    throw new UnsupportedOperationException("Cannot perform arithmetic operations on raw accelerometer axis data");
                } else if (config instanceof Delta) {
                    throw new UnsupportedOperationException("Cannot attach delta transformer to raw accelerometer axis data");
                } else if (config instanceof Rms || config instanceof Rss) {
                    final byte nInputs = 3, rmsMode = (byte) (config instanceof Rms ? 0 : 1);
                    ProcessedDataSignal newProcessor = new ProcessedDataSignal((byte) 2, MwrAccelCombinedAxisMessage.class) {
                        @Override
                        public byte[] getFilterConfig() {
                            return processorConfigToBytes(null);
                        }

                        @Override
                        protected byte[] processorConfigToBytes(ProcessorConfig newConfig) {
                            return new byte[]{0x7,
                                    (byte) (((this.outputSize - 1) & 0x3) | (((this.outputSize - 1) & 0x3) << 2) | ((nInputs - 1) << 4) | 0x80),
                                    rmsMode};
                        }

                        @Override
                        public boolean isSigned() {
                            return false;
                        }

                        @Override
                        public void setState(StateEditor editor) {
                            throw new UnsupportedOperationException("Cannot change rms/rss transformer state");
                        }

                        @Override
                        public void modifyConfiguration(ProcessorConfig newConfig) {
                            throw new UnsupportedOperationException("Cannot change rms/rss configuration");
                        }
                    };

                    postFilterCreate(newProcessor, parent);
                    return newProcessor;
                }
                return super.process(config, parent);
            }

            @Override
            public DataSignal log(MessageProcessor processor, final DataSignalImpl source) {
                final DataSignalImpl self = this;

                creators.add(new IdCreator() {
                    @Override
                    public void execute() {
                        final byte[] triggerConfig = source.getTriggerConfig();
                        final byte[] xyLogCfg = new byte[triggerConfig.length];
                        System.arraycopy(triggerConfig, 0, xyLogCfg, 0, triggerConfig.length - 1);
                        xyLogCfg[triggerConfig.length - 1] = (byte) ((XY_LENGTH - 1) << 5);

                        writeRegister(LoggingRegister.TRIGGER, xyLogCfg);
                    }

                    @Override
                    public void receivedId(byte id) {
                        loggingIds.add(id);
                        xyId = id;
                        dataLoggers.put(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, id), self);
                    }
                });
                creators.add(new IdCreator() {
                    @Override
                    public void execute() {
                        final byte[] triggerConfig = source.getTriggerConfig();
                        final byte[] zLogCfg = new byte[triggerConfig.length];
                        System.arraycopy(triggerConfig, 0, zLogCfg, 0, triggerConfig.length - 1);
                        zLogCfg[triggerConfig.length - 1] = (byte) ((((outputSize - XY_LENGTH) - 1) << 5) | Z_OFFSET);

                        writeRegister(LoggingRegister.TRIGGER, zLogCfg);
                    }

                    @Override
                    public void receivedId(byte id) {
                        loggingIds.add(id);
                        zId = id;
                        dataLoggers.put(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, id), self);
                    }
                });
                this.logProcessor = processor;
                hasDataLogger = true;
                return this;
            }

            @Override
            public void receivedLogEntry(byte[] entry) {
                byte logId = (byte) (entry[0] & 0xf);

                if (logId == xyId) {
                    xyLogEntries.add(entry);
                } else if (logId == zId) {
                    zLogEntries.add(entry);
                } else {
                    throw new RuntimeException("Unknown log id in this fn: " + logId);
                }

                if (!xyLogEntries.isEmpty() && !zLogEntries.isEmpty()) {
                    byte[] xyEntry = xyLogEntries.poll(), zEntry = zLogEntries.poll();
                    byte[] merged = new byte[xyEntry.length + zEntry.length - 7];
                    System.arraycopy(xyEntry, 1, merged, 1, xyEntry.length - 1);
                    System.arraycopy(zEntry, 5, merged, xyEntry.length, 2);

                    super.receivedLogEntry(merged);
                }
            }
        }
        @Override
        public DataSignal fromAccelAxis() {
            if (moduleNumber.equals(Constant.METAWEAR_RG_MODULE)) {
                routeSource = new AccelerometerDataSource((byte) 6, Bmi160AccelAxisMessage.class) {
                    @Override
                    public DataSignal subscribe(MessageProcessor processor) {
                        responseProcessors.put(new ResponseHeader(Bmi160AccelerometerRegister.DATA_INTERRUPT), processor);
                        return super.subscribe(processor);
                    }

                    @Override
                    public void unsubscribe() {
                        writeRegister(Bmi160AccelerometerRegister.DATA_INTERRUPT, (byte) 0);
                        super.unsubscribe();
                    }

                    @Override
                    public void enableNotifications() {
                        writeRegister(Bmi160AccelerometerRegister.DATA_INTERRUPT, (byte) 0x1);
                    }

                    @Override
                    public byte[] getEventConfig() {
                        return new byte[] {Bmi160AccelerometerRegister.DATA_INTERRUPT.moduleOpcode(), Bmi160AccelerometerRegister.DATA_INTERRUPT.opcode(),
                                (byte) 0xff};
                    }

                    @Override
                    public Number numberToFirmwareUnits(Number input) {
                        return input.floatValue() * bmi160AccRange.scale();
                    }
                };
            } else {
                routeSource = new AccelerometerDataSource((byte) 6, MwrAccelAxisMessage.class) {
                    @Override
                    public DataSignal subscribe(MessageProcessor processor) {
                        responseProcessors.put(new ResponseHeader(MwrAccelerometerRegister.DATA_VALUE), processor);
                        return super.subscribe(processor);
                    }

                    @Override
                    public void remove() {
                        responseProcessors.remove(new ResponseHeader(MwrAccelerometerRegister.DATA_VALUE));
                        super.remove();
                    }

                    @Override
                    public void enableNotifications() {
                        writeRegister(MwrAccelerometerRegister.DATA_VALUE, (byte) 1);
                    }

                    @Override
                    public void unsubscribe() {
                        writeRegister(MwrAccelerometerRegister.DATA_VALUE, (byte) 0);
                        super.unsubscribe();
                    }

                    @Override
                    public byte[] getEventConfig() {
                        return new byte[]{MwrAccelerometerRegister.DATA_VALUE.moduleOpcode(), MwrAccelerometerRegister.DATA_VALUE.opcode(),
                                (byte) 0xff};
                    }
                };
            }

            return routeSource;
        }

        @Override
        public DataSignal fromGyro() {
            if (moduleNumber.equals(Constant.METAWEAR_RG_MODULE)) {
                routeSource= new AccelerometerDataSource((byte) 6, Bmi160GyroMessage.class) {
                    @Override
                    public DataSignal subscribe(MessageProcessor processor) {
                        responseProcessors.put(new ResponseHeader(Bmi160GyroRegister.DATA), processor);
                        return super.subscribe(processor);
                    }

                    @Override
                    public void enableNotifications() {
                        writeRegister(Bmi160GyroRegister.DATA, (byte) 1);
                    }

                    @Override
                    public void unsubscribe() {
                        writeRegister(Bmi160GyroRegister.DATA, (byte) 0);
                        super.unsubscribe();
                    }

                    @Override
                    public byte[] getEventConfig() {
                        return new byte[] {Bmi160GyroRegister.DATA.moduleOpcode(), Bmi160GyroRegister.DATA.opcode(), (byte) 0xff};
                    }

                    @Override
                    public Number numberToFirmwareUnits(Number input) {
                        return input.floatValue() * bmi160GyroRange.scale();
                    }
                };
                return routeSource;
            }
            throw new UnsupportedOperationException("Gyro module available for this board");
        }

        @Override
        public DataSignal fromAnalogGpio(final byte pin, final Gpio.AnalogReadMode mode) {
            final Register analogRegister;
            Class<? extends Message> msgClass;

            switch(mode) {
                case ABS_REFERENCE:
                    analogRegister= GpioRegister.READ_AI_ABS_REF;
                    msgClass= GpioAbsRefMessage.class;
                    break;
                case ADC:
                    analogRegister= GpioRegister.READ_AI_ADC;
                    msgClass= GpioAdcMessage.class;
                    break;
                default:
                    analogRegister= null;
                    msgClass= null;
            }

            routeSource= new DataSource((byte) 2, msgClass) {
                private MessageProcessor processor;

                @Override
                public DataSignal subscribe(MessageProcessor processor) {
                    this.processor= processor;
                    responseProcessors.put(new ResponseHeader(analogRegister, pin), processor);
                    return super.subscribe(processor);
                }

                @Override
                public void remove() {
                    responseProcessors.remove(new ResponseHeader(analogRegister, pin));
                    super.remove();
                }

                @Override
                public boolean isSigned() {
                    return false;
                }

                @Override
                public void enableNotifications() { }

                @Override
                public void unsubscribe() {
                    responseProcessors.remove(new ResponseHeader(analogRegister, pin));
                    super.unsubscribe();
                }

                @Override
                public byte[] getEventConfig() {
                    return new byte[] {analogRegister.moduleOpcode(), (byte) (0x80 | analogRegister.opcode()), pin};
                }
            };

            return routeSource;
        }

        @Override
        public DataSignal fromDigitalIn(final byte pin) {
            routeSource= new DataSource((byte) 1, GpioDigitalMessage.class) {
                private MessageProcessor processor;

                @Override
                public DataSignal subscribe(MessageProcessor processor) {
                    this.processor= processor;
                    responseProcessors.put(new ResponseHeader(GpioRegister.READ_DI, pin), processor);
                    return super.subscribe(processor);
                }

                @Override
                public void remove() {
                    responseProcessors.remove(new ResponseHeader(GpioRegister.READ_DI, pin));
                    super.remove();
                }

                @Override
                public void unsubscribe() {
                    responseProcessors.remove(new ResponseHeader(GpioRegister.READ_DI, pin));
                    super.unsubscribe();
                }

                @Override
                public boolean isSigned() {
                    return false;
                }

                @Override
                public void enableNotifications() { }

                @Override
                public byte[] getEventConfig() {
                    return new byte[] {GpioRegister.READ_DI.moduleOpcode(), (byte) (0x80 | GpioRegister.READ_DI.opcode()), pin};
                }
            };

            return routeSource;
        }

        @Override
        public DataSignal fromGpioPinNotify(final byte pin) {
            routeSource= new DataSource((byte) 1, GpioDigitalMessage.class) {
                @Override
                public DataSignal subscribe(MessageProcessor processor) {
                    responseProcessors.put(new ResponseHeader(GpioRegister.PIN_CHANGE_NOTIFY, pin), processor);
                    return super.subscribe(processor);
                }

                @Override
                public void remove() {
                    responseProcessors.remove(new ResponseHeader(GpioRegister.PIN_CHANGE_NOTIFY, pin));
                    super.remove();
                }

                @Override
                public boolean isSigned() {
                    return false;
                }

                @Override
                public void unsubscribe() {
                    writeRegister(GpioRegister.PIN_CHANGE_NOTIFY, (byte) 0);
                    super.unsubscribe();
                }

                @Override
                public void enableNotifications() {
                    writeRegister(GpioRegister.PIN_CHANGE_NOTIFY, (byte) 1);
                }

                @Override
                public byte[] getEventConfig() {
                    return new byte[] {GpioRegister.PIN_CHANGE_NOTIFY.moduleOpcode(), (byte) (0x80 | GpioRegister.PIN_CHANGE_NOTIFY.opcode()), pin};
                }
            };
            return routeSource;
        }

        @Override
        public DataSignal fromGsr(final byte channel) {
            routeSource= new DataSource((byte) 4, GsrMessage.class) {
                private MessageProcessor processor;

                @Override
                public DataSignal subscribe(MessageProcessor processor) {
                    this.processor= processor;
                    responseProcessors.put(new ResponseHeader(GsrRegister.CONDUCTANCE, channel), processor);
                    return super.subscribe(processor);
                }

                @Override
                public void remove() {
                    responseProcessors.remove(new ResponseHeader(GsrRegister.CONDUCTANCE, channel));
                    super.remove();
                }

                @Override
                public boolean isSigned() {
                    return false;
                }

                @Override
                public void unsubscribe() {
                    responseProcessors.remove(new ResponseHeader(GsrRegister.CONDUCTANCE, channel));
                    super.unsubscribe();
                }

                @Override
                public void enableNotifications() { }

                @Override
                public byte[] getEventConfig() {
                    return new byte[] {GsrRegister.CONDUCTANCE.moduleOpcode(), (byte) (0x80 | GsrRegister.CONDUCTANCE.opcode()), channel};
                }
            };
            return routeSource;
        }

        @Override
        public DataSignal fromTemperature(final MultiChannelTemperature.Source src) {
            if (moduleNumber.equals(Constant.METAWEAR_RG_MODULE)) {
                routeSource= new DataSource((byte) 2, TemperatureMessage.class) {
                    private MessageProcessor processor;

                    @Override
                    public DataSignal subscribe(MessageProcessor processor) {
                        this.processor= processor;
                        responseProcessors.put(new ResponseHeader(MultiChannelTempRegister.TEMPERATURE, src.channel()), processor);
                        return super.subscribe(processor);
                    }

                    @Override
                    public void remove() {
                        responseProcessors.remove(new ResponseHeader(MultiChannelTempRegister.TEMPERATURE, src.channel()));
                        super.remove();
                    }

                    @Override
                    public Number numberToFirmwareUnits(Number input) {
                        return input.floatValue() * 8.0f;
                    }

                    @Override
                    public boolean isSigned() {
                        return false;
                    }

                    @Override
                    public void unsubscribe() {
                        responseProcessors.remove(new ResponseHeader(MultiChannelTempRegister.TEMPERATURE, src.channel()));
                        super.unsubscribe();
                    }

                    @Override
                    public void enableNotifications() { }

                    @Override
                    public byte[] getEventConfig() {
                        return new byte[] {MultiChannelTempRegister.TEMPERATURE.moduleOpcode(), MultiChannelTempRegister.TEMPERATURE.opcode(), src.channel()};
                    }
                };
                return routeSource;
            }
            throw new UnsupportedOperationException("Multichannel temperature not supported on this board");
        }

        @Override
        public void receivedId(byte id) {
            creators.poll().receivedId(id);
            addId();
        }

        private void eventCommandsCheck() {
            if (isConnected()) {
                if (nExpectedCmds == 0) {
                    currEventListener = null;
                    commitResult.setResult(routeSource, null);
                    pendingRoutes.poll();
                    if (!pendingRoutes.isEmpty()) {
                        pendingRoutes.peek().addId();
                    } else {
                        commitRoutes.set(false);
                    }
                }
            } else {
                commitResult.setResult(null, new RuntimeException("Connection to MetaWear board lost"));
            }
        }
        public void receivedCommandId(byte id) {
            eventCmdIds.add(id);
            nExpectedCmds--;
            eventCommandsCheck();
        }

        @Override
        public void addId() {
            if (creators.isEmpty()) {
                addTaps();
            } else {
                creators.peek().execute();
            }
        }

        private void addTaps() {
            eventConfig= null;
            for(DataSignalImpl it: activeSignals) {
                it.enableNotifications();
            }
            activeSignals.clear();

            currEventListener = this;
            for(Map.Entry<DataSignalImpl, DataSignal.ActivityMonitor> it: signalMonitors.entrySet()) {
                eventConfig= it.getKey().getEventConfig();
                it.getValue().onSignalActive(dataProcessors, it.getKey());
            }
            signalMonitors.clear();
            eventConfig= null;

            eventCommandsCheck();
        }

        public void onCommandWrite() {
            nExpectedCmds++;
        }
    }

    private String moduleNumber= null;
    private Logging.DownloadHandler downloadHandler;
    private float notifyProgress= 1.0f;
    private int nLogEntries;
    private ReferenceTick logReferenceTick= null;
    private final AtomicBoolean commitRoutes= new AtomicBoolean(false);
    private final Queue<RouteHandler> pendingRoutes= new LinkedList<>();
    private final HashMap<ResponseHeader, DataSignal.MessageProcessor> responseProcessors= new HashMap<>();
    private final HashMap<ResponseHeader, Class<? extends Message>> dataProcMsgClasses= new HashMap<>();
    private final HashMap<ResponseHeader, Loggable> dataLoggers= new HashMap<>();

    protected DefaultMetaWearBoard(Connection conn) {
        this.conn= conn;
        final ResponseProcessor idProcessor= new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                pendingRoutes.peek().receivedId(response[2]);
                return null;
            }
        };

        responses= new HashMap<>();
        responses.put(new ResponseHeader(SwitchRegister.STATE), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                byte[] respBody= new byte[response.length - 2];
                System.arraycopy(response, 2, respBody, 0, respBody.length);

                return new Response(new SwitchMessage(respBody), new ResponseHeader(response[0], response[1]));
            }
        });
        responses.put(new ResponseHeader(EventRegister.ENTRY), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                currEventListener.receivedCommandId(response[2]);
                return null;
            }
        });
        responses.put(new ResponseHeader(LoggingRegister.TIME), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                final Calendar now= Calendar.getInstance();
                final long tick= ByteBuffer.wrap(response, 2, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

                logReferenceTick= new ReferenceTick() {
                    @Override public long tickCount() { return tick; }
                    @Override public Calendar timestamp() { return now; }
                };

                readRegister(LoggingRegister.LENGTH);
                return null;
            }
        });
        responses.put(new ResponseHeader(LoggingRegister.LENGTH), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                nLogEntries= ByteBuffer.wrap(response, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
                int nEntriesNotify= (int) (nLogEntries * notifyProgress);

                writeRegister(LoggingRegister.READOUT, response[2], response[3], (byte) (nEntriesNotify & 0xff), (byte) ((nEntriesNotify >> 8) & 0xff));
                return null;
            }
        });
        responses.put(new ResponseHeader(LoggingRegister.READOUT_NOTIFY), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                ResponseHeader header= new ResponseHeader(LoggingRegister.READOUT_NOTIFY, (byte) (response[2] & 0xf));

                byte[] first= Arrays.copyOfRange(response, 2, 11);
                if (dataLoggers.containsKey(header)) {
                    dataLoggers.get(header).receivedLogEntry(first);
                }
                if (response.length == 20) {
                    header= new ResponseHeader(LoggingRegister.READOUT_NOTIFY, (byte) (response[11] & 0xf));
                    byte[] second= Arrays.copyOfRange(response, 11, 20);
                    if (dataLoggers.containsKey(header)) {
                        dataLoggers.get(header).receivedLogEntry(second);
                    }
                }
                return null;
            }
        });
        responses.put(new ResponseHeader(LoggingRegister.READOUT_PROGRESS), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                int nEntriesLeft= ByteBuffer.wrap(response, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
                downloadHandler.onProgressUpdate(nEntriesLeft, nLogEntries);
                return null;
            }
        });
        responses.put(new ResponseHeader(LoggingRegister.TRIGGER), idProcessor);
        responses.put(new ResponseHeader(DataProcessorRegister.ADD), idProcessor);
        responses.put(new ResponseHeader(DataProcessorRegister.NOTIFY), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                try {
                    byte[] respBody= new byte[response.length - 3];
                    System.arraycopy(response, 3, respBody, 0, respBody.length);
                    ResponseHeader header= new ResponseHeader(response[0], response[1], response[2]);

                    if (dataProcMsgClasses.get(header).equals(Bmi160Accelerometer.class)) {
                        return new Response(new Bmi160AccelAxisMessage(respBody, bmi160AccRange.scale()), header);
                    } else if (dataProcMsgClasses.equals(Bmi160GyroMessage.class)) {
                        return new Response(new Bmi160GyroMessage(respBody, bmi160GyroRange.scale()), header);
                    } else {
                        Constructor<?> cTor = dataProcMsgClasses.get(header).getConstructor(byte[].class);

                        return new Response((Message) cTor.newInstance(respBody), header);
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                        InstantiationException ex) {
                    throw new RuntimeException("Cannot create a message processor for filter output: " + Arrays.toString(response));
                }
            }
        });
        responses.put(new ResponseHeader(TimerRegister.TIMER_ENTRY), new ResponseProcessor() {
            @Override
            public Response process(final byte[] response) {
                timerControllerResults.poll().setResult(new TimerControllerImpl(response[2]), null);
                return null;
            }
        });
        responses.put(new ResponseHeader(GpioRegister.READ_AI_ABS_REF), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                byte[] respBody= new byte[response.length - 3];
                System.arraycopy(response, 3, respBody, 0, respBody.length);
                ResponseHeader header= new ResponseHeader(response[0], response[1], response[2]);

                return new Response(new GpioAbsRefMessage(respBody), header);
            }
        });
        responses.put(new ResponseHeader(GpioRegister.READ_AI_ADC), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                byte[] respBody= new byte[response.length - 3];
                System.arraycopy(response, 3, respBody, 0, respBody.length);
                ResponseHeader header= new ResponseHeader(response[0], response[1], response[2]);

                return new Response(new GpioAdcMessage(respBody), header);
            }
        });
        responses.put(new ResponseHeader(GpioRegister.READ_DI), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                byte[] respBody= new byte[response.length - 3];
                System.arraycopy(response, 3, respBody, 0, respBody.length);
                ResponseHeader header= new ResponseHeader(response[0], response[1], response[2]);

                return new Response(new GpioDigitalMessage(respBody), header);
            }
        });
        responses.put(new ResponseHeader(GpioRegister.PIN_CHANGE_NOTIFY), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                byte[] respBody= new byte[response.length - 3];
                System.arraycopy(response, 3, respBody, 0, respBody.length);
                ResponseHeader header= new ResponseHeader(response[0], response[1], response[2]);

                return new Response(new GpioDigitalMessage(respBody), header);
            }
        });
        responses.put(new ResponseHeader(GsrRegister.CONDUCTANCE), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                byte[] respBody= new byte[response.length - 3];
                System.arraycopy(response, 3, respBody, 0, respBody.length);
                ResponseHeader header= new ResponseHeader(response[0], response[1], response[2]);

                return new Response(new GsrMessage(respBody), header);
            }
        });
        responses.put(new ResponseHeader(IBeaconRegister.UUID), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                ibeaconUuid= new UUID(ByteBuffer.wrap(response, 10, 8).order(ByteOrder.LITTLE_ENDIAN).getLong(),
                        ByteBuffer.wrap(response, 2, 8).order(ByteOrder.LITTLE_ENDIAN).getLong());
                readRegister(IBeaconRegister.MAJOR);
                return null;
            }
        });
        responses.put(new ResponseHeader(IBeaconRegister.MAJOR), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                ibeaconMajor = ByteBuffer.wrap(response, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
                readRegister(IBeaconRegister.MINOR);
                return null;
            }
        });
        responses.put(new ResponseHeader(IBeaconRegister.MINOR), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                ibeaconMinor = ByteBuffer.wrap(response, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
                readRegister(IBeaconRegister.RX_POWER);
                return null;
            }
        });
        responses.put(new ResponseHeader(IBeaconRegister.RX_POWER), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                ibeaconRxPower = response[2];
                readRegister(IBeaconRegister.TX_POWER);
                return null;
            }
        });
        responses.put(new ResponseHeader(IBeaconRegister.TX_POWER), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                ibeaconTxPower = response[2];
                readRegister(IBeaconRegister.PERIOD);
                return null;
            }
        });
        responses.put(new ResponseHeader(IBeaconRegister.PERIOD), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                ibeaconPeriod = ByteBuffer.wrap(response, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
                ibeaconConfigResults.poll().setResult(new IBeacon.Configuration() {
                    @Override
                    public UUID adUuid() { return ibeaconUuid; }

                    @Override
                    public short major() { return ibeaconMajor; }

                    @Override
                    public short minor() { return ibeaconMinor; }

                    @Override
                    public byte rxPower() { return ibeaconRxPower; }

                    @Override
                    public byte txPower() { return ibeaconTxPower; }

                    @Override
                    public short adPeriod() { return ibeaconPeriod; }

                    @Override
                    public String toString() {
                        return String.format("{uuid: %s, major: %d, minor: %d, rx: %d, tx: %d, period: %d}",
                                ibeaconUuid.toString(), ibeaconMajor, ibeaconMinor, ibeaconRxPower, ibeaconTxPower, ibeaconPeriod);
                    }
                }, null);
                return null;
            }
        });
        responses.put(new ResponseHeader(SettingsRegister.DEVICE_NAME), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                byte[] respBody= new byte[response.length - 2];
                System.arraycopy(response, 2, respBody, 0, respBody.length);

                try {
                    advName= new String(respBody, "US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    advName= new String(respBody);
                }
                readRegister(SettingsRegister.ADVERTISING_INTERVAL);
                return null;
            }
        });
        responses.put(new ResponseHeader(SettingsRegister.ADVERTISING_INTERVAL), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                advInterval= (short) ((response[2] & 0xff) | (response[3] << 8));
                advTimeout= response[4];
                readRegister(SettingsRegister.TX_POWER);
                return null;
            }
        });
        responses.put(new ResponseHeader(SettingsRegister.TX_POWER), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                advTxPower= response[2];
                readRegister(SettingsRegister.SCAN_RESPONSE);
                return null;
            }
        });
        responses.put(new ResponseHeader(SettingsRegister.SCAN_RESPONSE), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                advResponse= new byte[response.length - 2];
                System.arraycopy(response, 2, advResponse, 0, advResponse.length);
                advertisementConfigResults.poll().setResult(new Settings.AdvertisementConfig() {
                    @Override
                    public String deviceName() {
                        return advName;
                    }

                    @Override
                    public int interval() {
                        return advInterval & 0xffff;
                    }

                    @Override
                    public short timeout() {
                        return (short) (advTimeout & 0xff);
                    }

                    @Override
                    public byte txPower() {
                        return advTxPower;
                    }

                    @Override
                    public byte[] scanResponse() {
                        return advResponse;
                    }

                    @Override
                    public String toString() {
                        return String.format("{Device Name: %s, Adv Interval: %d, Adv Timeout: %d, Tx Power: %d, Scan Response: %s}",
                                advName, interval(), timeout(), advTxPower, arrayToHexString(advResponse));
                    }
                }, null);
                return null;
            }
        });
        responses.put(new ResponseHeader(I2CRegister.READ_WRITE), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                if (response.length > 3) {
                    byte[] i2cData = new byte[response.length - 3];
                    System.arraycopy(response, 3, i2cData, 0, response.length - 3);

                    i2cReadResults.poll().setResult(i2cData, null);
                } else {
                    i2cReadResults.poll().setResult(null, new RuntimeException("Received I2C data less than 4 bytes: " + arrayToHexString(response)));
                }

                return null;
            }
        });
        responses.put(new ResponseHeader(MacroRegister.BEGIN), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                macroIds.poll().setResult(response[2], null);
                return null;
            }
        });
    }

    public void setModuleNumber(final String moduleNumber) {
        this.moduleNumber= moduleNumber;

        if (moduleNumber.equals(Constant.METAWEAR_R_MODULE)) {
            responses.put(new ResponseHeader(MwrAccelerometerRegister.DATA_VALUE), new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    byte[] respBody= new byte[response.length - 2];
                    System.arraycopy(response, 2, respBody, 0, respBody.length);

                    return new Response(new MwrAccelAxisMessage(respBody), new ResponseHeader(response[0], response[1]));
                }
            });
            responses.put(new ResponseHeader(TemperatureRegister.VALUE), new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    byte[] respBody= new byte[response.length - 2];
                    System.arraycopy(response, 2, respBody, 0, respBody.length);

                    return new Response(new TemperatureMessage(respBody), new ResponseHeader(response[0], response[1]));
                }
            });
        } else if (moduleNumber.equals(Constant.METAWEAR_RG_MODULE)) {
            responses.put(new ResponseHeader(Bmi160AccelerometerRegister.DATA_INTERRUPT), new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    byte[] respBody = new byte[response.length - 2];
                    System.arraycopy(response, 2, respBody, 0, respBody.length);

                    return new Response(new Bmi160AccelAxisMessage(respBody,
                            bmi160AccRange.scale()),
                            new ResponseHeader(response[0], response[1]));
                }
            });
            responses.put(new ResponseHeader(Bmi160GyroRegister.DATA), new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    byte[] respBody = new byte[response.length - 2];
                    System.arraycopy(response, 2, respBody, 0, respBody.length);

                    return new Response(new Bmi160GyroMessage(respBody,
                            bmi160GyroRange.scale()),
                            new ResponseHeader(response[0], response[1]));
                }
            });
            responses.put(new ResponseHeader(Bmi160GyroRegister.CONFIG), new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    bmi160DataSampling[0]= response[0];
                    bmi160DataSampling[1]= response[1];
                    bmi160GyroRange= Bmi160Gyro.FullScaleRange.bitMaskToRange((byte) (response[1] & 0x7));
                    return null;
                }
            });
            responses.put(new ResponseHeader(Bmi160AccelerometerRegister.DATA_CONFIG), new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    bmi160DataSampling[0]= response[0];
                    bmi160DataSampling[1]= response[1];
                    bmi160AccRange= Bmi160Accelerometer.AccRange.bitMaskToRange((byte) (response[1] & 0xf));
                    return null;
                }
            });
            responses.put(new ResponseHeader(MultiChannelTempRegister.TEMPERATURE), new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    byte[] respBody= new byte[response.length - 3];
                    System.arraycopy(response, 3, respBody, 0, respBody.length);

                    return new Response(new TemperatureMessage(respBody), new ResponseHeader(response[0], response[1], response[2]));
                }
            });
            responses.put(new ResponseHeader(MultiChannelTempRegister.INFO), new ResponseProcessor() {
                @Override
                public Response process(final byte[] response) {
                    final MultiChannelTemperature.Source[] sources= new MultiChannelTemperature.Source[response.length - 4];

                    for(byte i= 4; i < response.length; i++) {
                        final MultiChannelTemperature.SourceType type= MultiChannelTemperature.SourceType.values()[response[i]];
                        final byte channel= (byte) (i - 4);
                        MultiChannelTemperature.Source newSource= null;

                        switch (type) {
                            case ON_DIE:
                                newSource= new MultiChannelTemperature.NrfDie() {
                                    @Override
                                    public byte driver() { return response[channel + 4]; }

                                    @Override
                                    public byte channel() { return channel; }

                                    @Override
                                    public MultiChannelTemperature.SourceType type() {
                                        return type;
                                    }
                                };
                                break;
                            case EXT_THERMISTOR:
                                newSource= new MultiChannelTemperature.ExtThermistor() {
                                    @Override
                                    public void configure(byte analogReadPin, byte pulldownPin, boolean activeHigh) {
                                        writeRegister(MultiChannelTempRegister.MODE, channel(), analogReadPin, pulldownPin, (byte) (activeHigh ? 1 : 0));
                                    }

                                    @Override
                                    public byte driver() { return response[channel + 4]; }

                                    @Override
                                    public byte channel() { return channel; }

                                    @Override
                                    public MultiChannelTemperature.SourceType type() {
                                        return type;
                                    }
                                };
                                break;
                            case BMP280:
                                newSource= new MultiChannelTemperature.BMP280() {
                                    @Override
                                    public byte driver() { return response[channel + 4]; }

                                    @Override
                                    public byte channel() { return channel; }

                                    @Override
                                    public MultiChannelTemperature.SourceType type() {
                                        return type;
                                    }
                                };
                                break;
                            case PRESET_THERMISTOR:
                                newSource= new MultiChannelTemperature.PresetThermistor() {
                                    @Override
                                    public byte driver() { return response[channel + 4]; }

                                    @Override
                                    public byte channel() { return channel; }

                                    @Override
                                    public MultiChannelTemperature.SourceType type() {
                                        return type;
                                    }
                                };
                                break;
                        }
                        sources[channel]= newSource;
                    }

                    tempSourcesResults.poll().setResult(sources, null);
                    return null;
                }
            });
        }
    }

    @Override
    public RouteBuilder routeData() {
        return new RouteBuilderImpl();
    }

    private interface ResponseProcessor {
        public Response process(byte[] response);
    }
    private final HashMap<ResponseHeader, ResponseProcessor> responses;

    @Override
    public void receivedResponse(byte[] response) {
        response[1]&= 0x7f;
        ResponseHeader header= new ResponseHeader(response[0], response[1]);

        if (responses.containsKey(header)) {
            Response resp= responses.get(header).process(response);
            if (resp != null && responseProcessors.containsKey(resp.header)) {
                responseProcessors.get(resp.header).process(resp.body);
            }
        }
    }

    private class GsrImpl implements Gsr {
        @Override
        public void readConductance(byte channel) {
            readRegister(GsrRegister.CONDUCTANCE, channel);
        }

        @Override
        public void calibrate() {
            writeRegister(GsrRegister.CALIBRATE);
        }
    }
    private class LedImpl implements Led {
        @Override
        public ColorChannelWriter writeChannelAttributes(final ColorChannel channel) {
            return new ColorChannelWriter() {
                private final byte[] channelData= new byte[15];

                @Override
                public ColorChannelWriter withHighIntensity(byte intensity) {
                    channelData[2]= intensity;
                    return this;
                }

                @Override
                public ColorChannelWriter withLowIntensity(byte intensity) {
                    channelData[3]= intensity;
                    return this;
                }

                @Override
                public ColorChannelWriter withRiseTime(short time) {
                    channelData[5]= (byte)((time >> 8) & 0xff);
                    channelData[4]= (byte)(time & 0xff);
                    return this;
                }

                @Override
                public ColorChannelWriter withHighTime(short time) {
                    channelData[7]= (byte)((time >> 8) & 0xff);
                    channelData[6]= (byte)(time & 0xff);
                    return this;
                }

                @Override
                public ColorChannelWriter withFallTime(short time) {
                    channelData[9]= (byte)((time >> 8) & 0xff);
                    channelData[8]= (byte)(time & 0xff);
                    return this;
                }

                @Override
                public ColorChannelWriter withPulseDuration(short duration) {
                    channelData[11]= (byte)((duration >> 8) & 0xff);
                    channelData[10]= (byte)(duration & 0xff);
                    return this;
                }

                @Override
                public ColorChannelWriter withRepeatCount(byte count) {
                    channelData[14]= count;
                    return this;
                }

                @Override
                public void commit() {
                    channelData[0]= (byte)(channel.ordinal());
                    channelData[1]= 0x2;    ///< Keep it set to flash for now
                    writeRegister(LedRegister.MODE, channelData);
                }
            };
        }

        @Override
        public void play(boolean autoplay) {
            writeRegister(LedRegister.PLAY, (byte) (autoplay ? 2 : 1));
        }

        @Override
        public void pause() {
            writeRegister(LedRegister.PLAY, (byte) 0);
        }

        @Override
        public void stop(boolean resetChannelAttrs) {
            writeRegister(LedRegister.STOP, (byte) (resetChannelAttrs ? 1 : 0));
        }
    }
    private class TemperatureImpl implements Temperature {
        @Override
        public void readTemperarure() {
            readRegister(TemperatureRegister.VALUE);
        }

        @Override
        public void enableThermistorMode(byte analogReadPin, byte pulldownPin) {
            writeRegister(TemperatureRegister.THERMISTOR_MODE, (byte) 1, analogReadPin, pulldownPin);
        }

        @Override
        public void disableThermistorMode() {
            writeRegister(TemperatureRegister.THERMISTOR_MODE, new byte[] {0, 0, 0});
        }
    }
    private class GpioImpl implements Gpio {
        @Override
        public void readAnalogIn(byte pin, AnalogReadMode mode) {
            switch (mode) {
                case ABS_REFERENCE:
                    readRegister(GpioRegister.READ_AI_ABS_REF, pin);
                    break;
                case ADC:
                    readRegister(GpioRegister.READ_AI_ADC, pin);
                    break;
            }
        }

        @Override
        public void readDigitalIn(byte pin) {
            readRegister(GpioRegister.READ_DI, pin);
        }

        @Override
        public void setDigitalInPullMode(byte pin, PinConfig mode) {
            switch (mode) {
                case PULL_UP:
                    writeRegister(GpioRegister.PULL_UP_DI, pin);
                    break;
                case PULL_DOWN:
                    writeRegister(GpioRegister.PULL_DOWN_DI, pin);
                    break;
                case NO_PULL:
                    writeRegister(GpioRegister.NO_PULL_DI, pin);
                    break;
            }
        }

        @Override
        public void setDigitalOut(byte pin) {
            writeRegister(GpioRegister.SET_DO, pin);
        }

        @Override
        public void clearDigitalOut(byte pin) {
            writeRegister(GpioRegister.CLEAR_DO, pin);
        }

        @Override
        public void setPinChangeType(byte pin, PinChangeType type) {
            writeRegister(GpioRegister.PIN_CHANGE, pin, (byte) (type.ordinal() + 1));
        }

        @Override
        public void startPinChangeDetection(byte pin) {
            writeRegister(GpioRegister.PIN_CHANGE_NOTIFY_ENABLE, pin, (byte) 1);
        }

        @Override
        public void stopPinChangeDetection(byte pin) {
            writeRegister(GpioRegister.PIN_CHANGE_NOTIFY_ENABLE, pin, (byte) 0);
        }
    }

    private final ConcurrentLinkedQueue<AsyncResultImpl<Timer.Controller>> timerControllerResults= new ConcurrentLinkedQueue<>();
    private class TimerImpl implements Timer {
        @Override
        public AsyncResult<Controller> createTimer(int period, short repeat, boolean delay) {
            AsyncResultImpl<Controller> timerResult= new AsyncResultImpl<>();
            if (isConnected()) {
                timerControllerResults.add(timerResult);

                ByteBuffer buffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);
                buffer.putInt(period).putShort(repeat).put((byte) (delay ? 0 : 1));
                writeRegister(TimerRegister.TIMER_ENTRY, buffer.array());

            } else {
                timerResult.setResult(null, new RuntimeException("Not connected to a MetaWear board"));
            }

            return timerResult;
        }
    }
    private class TimerControllerImpl implements EventListener, Timer.Controller, RouteHandler {
        private final byte timerId;
        private final byte[] timerEventConfig;
        private byte nExpectedCommands= 0;
        private HashSet<Byte> eventCmdIds;
        private Timer.Task timerTask;

        public TimerControllerImpl(byte timerId) {
            this.timerId= timerId;
            timerEventConfig= new byte[] {TimerRegister.NOTIFY.moduleOpcode(), TimerRegister.NOTIFY.opcode(),timerId};
            eventCmdIds= new HashSet<>();
        }

        @Override
        public void start() {
            writeRegister(TimerRegister.START, timerId);
        }

        @Override
        public void stop() {
            writeRegister(TimerRegister.STOP, timerId);
        }

        @Override
        public void remove() {
            writeRegister(TimerRegister.REMOVE, timerId);
            for(Byte id: eventCmdIds) {
                writeRegister(EventRegister.REMOVE, id);
            }
            eventCmdIds.clear();
            eventCmdIds= null;
        }

        @Override
        public void schedule(Timer.Task timerTask) {
            this.timerTask= timerTask;

            pendingRoutes.add(TimerControllerImpl.this);
            if (!commitRoutes.get()) {
                commitRoutes.set(true);
                pendingRoutes.peek().addId();
            }
        }

        @Override
        public void receivedCommandId(byte id) {
            eventCmdIds.add(id);
            nExpectedCommands--;

            if (nExpectedCommands == 0) {
                currEventListener = null;

                pendingRoutes.poll();
                if (!pendingRoutes.isEmpty()) {
                    pendingRoutes.peek().addId();
                } else {
                    commitRoutes.set(false);
                }
            }
        }

        @Override
        public void onCommandWrite() {
            nExpectedCommands++;
        }

        @Override
        public void addId() {
            currEventListener = this;

            eventConfig= timerEventConfig;
            timerTask.execute();
            eventConfig= null;
        }

        @Override
        public void receivedId(byte id) { }
    }
    private class DebugImpl implements Debug {
        @Override
        public void resetDevice() {
            writeRegister(DebugRegister.RESET_DEVICE);
        }

        @Override
        public void jumpToBootloader() {
            writeRegister(DebugRegister.JUMP_TO_BOOTLOADER);
        }

        @Override
        public void resetAfterGarbageCollect() {
            writeRegister(DebugRegister.DELAYED_RESET);
            writeRegister(DebugRegister.GAP_DISCONNECT);
        }
    }
    private class LoggingImpl implements Logging {
        @Override
        public void downloadLog(float notifyProgress, DownloadHandler handler) {
            DefaultMetaWearBoard.this.notifyProgress= notifyProgress;
            downloadHandler= handler;

            writeRegister(LoggingRegister.READOUT_NOTIFY, (byte) 1);
            if (handler != null) {
                writeRegister(LoggingRegister.READOUT_PROGRESS, (byte) 1);
            }
            readRegister(LoggingRegister.TIME);
        }
        @Override
        public void setCircularBufferMode(boolean enable) {
            writeRegister(LoggingRegister.CIRCULAR_BUFFER, (byte) (enable ? 1 : 0));
        }

        @Override
        public void removeEntries() {
            writeRegister(LoggingRegister.REMOVE_ENTRIES, (byte) 0xff, (byte) 0xff);
        }
    }

    private UUID ibeaconUuid;
    private short ibeaconMajor, ibeaconMinor, ibeaconPeriod;
    private byte ibeaconRxPower, ibeaconTxPower;
    private final ConcurrentLinkedQueue<AsyncResultImpl<IBeacon.Configuration>> ibeaconConfigResults= new ConcurrentLinkedQueue<>();
    private class IBeaconImpl implements IBeacon {
        public IBeaconConfigEditor edit() {
            return new IBeaconConfigEditor() {
                @Override
                public IBeaconConfigEditor withUUID(UUID adUuid) {
                    ibeaconUuid= adUuid;
                    return this;
                }

                @Override
                public IBeaconConfigEditor withMajor(short major) {
                    DefaultMetaWearBoard.this.ibeaconMajor = major;
                    return this;
                }

                @Override
                public IBeaconConfigEditor withMinor(short minor) {
                    DefaultMetaWearBoard.this.ibeaconMinor = minor;
                    return this;
                }

                @Override
                public IBeaconConfigEditor withRxPower(byte power) {
                    ibeaconRxPower = power;
                    return this;
                }

                @Override
                public IBeaconConfigEditor withTxPower(byte power) {
                    ibeaconTxPower = power;
                    return this;
                }

                @Override
                public IBeaconConfigEditor withAdPeriod(short period) {
                    DefaultMetaWearBoard.this.ibeaconPeriod = period;
                    return this;
                }

                @Override
                public void commit() {
                    byte[] uuidBytes= ByteBuffer.allocate(16)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putLong(ibeaconUuid.getLeastSignificantBits())
                            .putLong(ibeaconUuid.getMostSignificantBits())
                            .array();

                    writeRegister(IBeaconRegister.UUID, uuidBytes);
                    writeRegister(IBeaconRegister.MAJOR, (byte)(ibeaconMajor & 0xff), (byte)((ibeaconMajor >> 8) & 0xff));
                    writeRegister(IBeaconRegister.MINOR, (byte)(ibeaconMinor & 0xff), (byte)((ibeaconMinor >> 8) & 0xff));
                    writeRegister(IBeaconRegister.RX_POWER, ibeaconRxPower);
                    writeRegister(IBeaconRegister.TX_POWER, ibeaconTxPower);
                    writeRegister(IBeaconRegister.PERIOD, (byte)(ibeaconPeriod & 0xff), (byte)((ibeaconPeriod >> 8) & 0xff));
                }
            };
        }

        public void enable() {
            writeRegister(IBeaconRegister.ENABLE, (byte) 1);
        }
        public void disable() {
            writeRegister(IBeaconRegister.ENABLE, (byte) 0);
        }

        public AsyncResult<Configuration> readConfiguration() {
            AsyncResultImpl<Configuration> result= new AsyncResultImpl<>();
            ibeaconConfigResults.add(result);
            readRegister(IBeaconRegister.UUID);
            return result;
        }
    }

    private class HapticImpl implements Haptic {
        private final static float DEFAULT_DUTY_CYCLE= 100.f;

        @Override
        public void startMotor(short pulseWidth) {
            startMotor(DEFAULT_DUTY_CYCLE, pulseWidth);
        }

        @Override
        public void startMotor(float dutyCycle, short pulseWidth) {
            short converted= (short)((dutyCycle / 100.f) * 248);
            ByteBuffer buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).put((byte) (converted & 0xff)).putShort(pulseWidth).put((byte) 0);

            writeRegister(HapticRegister.PULSE, buffer.array());
        }

        @Override
        public void startBuzzer(short pulseWidth) {
            ByteBuffer buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).put((byte) 127).putShort(pulseWidth).put((byte) 0);
            writeRegister(HapticRegister.PULSE, buffer.array());
        }
    }
    private class NeoPixelImpl implements NeoPixel {
        @Override
        public void initializeStrand(byte strand, ColorOrdering ordering, StrandSpeed speed, byte gpioPin, byte length) {
            writeRegister(NeoPixelRegister.INITIALIZE, strand, (byte)(speed.ordinal() << 2 | ordering.ordinal()), gpioPin, length);
        }

        @Override
        public void deinitializeStrand(byte strand) {
            writeRegister(NeoPixelRegister.DEINITIALIZE, strand);
        }

        @Override
        public void holdStrand(byte strand) {
            writeRegister(NeoPixelRegister.HOLD, strand, (byte) 1);
        }

        @Override
        public void releaseHold(byte strand) {
            writeRegister(NeoPixelRegister.HOLD, strand, (byte) 0);
        }

        @Override
        public void clearStrand(byte strand, byte start, byte end) {
            writeRegister(NeoPixelRegister.CLEAR, strand, start, end);
        }

        @Override
        public void setPixel(byte strand, byte pixel, byte red, byte green, byte blue) {
            writeRegister(NeoPixelRegister.PIXEL, strand, pixel, red, green, blue);
        }

        @Override
        public void rotate(byte strand, RotationDirection direction, byte repetitions, short period) {
            writeRegister(NeoPixelRegister.ROTATE, strand, (byte)direction.ordinal(), repetitions,
                    (byte)(period & 0xff), (byte)(period >> 8 & 0xff));
        }

        @Override
        public void rotate(byte strand, RotationDirection direction, short period) {
            rotate(strand, direction, (byte) -1, period);
        }

        @Override
        public void stopRotation(byte strand) {
            writeRegister(NeoPixelRegister.ROTATE, new byte[] {strand, 0x0, 0x0, 0x0, 0x0});
        }
    }

    private String advName;
    private short advInterval;
    private byte advTimeout, advTxPower;
    private byte[] advResponse;
    private final ConcurrentLinkedQueue<AsyncResultImpl<Settings.AdvertisementConfig>> advertisementConfigResults= new ConcurrentLinkedQueue<>();
    private class SettingsImpl implements Settings {
        @Override
        public AdvertisementConfigEditor edit() {
            return new AdvertisementConfigEditor() {
                @Override
                public AdvertisementConfigEditor withDeviceName(String name) {
                    advName= name;
                    return this;
                }

                @Override
                public AdvertisementConfigEditor withAdInterval(short interval, byte timeout) {
                    advInterval= interval;
                    advTimeout= timeout;
                    return this;
                }

                @Override
                public AdvertisementConfigEditor withTxPower(byte power) {
                    advTxPower= power;
                    return this;
                }

                @Override
                public AdvertisementConfigEditor setScanResponse(byte[] response) {
                    advResponse= response;
                    return this;
                }

                @Override
                public void commit() {
                    try {
                        writeRegister(SettingsRegister.DEVICE_NAME, advName.getBytes("US-ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        writeRegister(SettingsRegister.DEVICE_NAME, advName.getBytes());
                    }
                    writeRegister(SettingsRegister.ADVERTISING_INTERVAL, (byte) (advInterval & 0xff),
                            (byte) ((advInterval >> 8) & 0xff), advTimeout);
                    writeRegister(SettingsRegister.TX_POWER, advTxPower);
                    if (advResponse.length >= MW_COMMAND_LENGTH) {
                        byte[] first= new byte[13], second= new byte[advResponse.length - 13];
                        System.arraycopy(advResponse, 0, first, 0, first.length);
                        System.arraycopy(advResponse, first.length, second, 0, second.length);

                        writeRegister(SettingsRegister.PARTIAL_SCAN_RESPONSE, first);
                        writeRegister(SettingsRegister.SCAN_RESPONSE, second);
                    } else {
                        writeRegister(SettingsRegister.SCAN_RESPONSE, advResponse);
                    }
                }
            };
        }

        @Override
        public AsyncResult<AdvertisementConfig> readAdConfig() {
            AsyncResultImpl<AdvertisementConfig> result= new AsyncResultImpl<>();
            advertisementConfigResults.add(result);
            readRegister(SettingsRegister.DEVICE_NAME);
            return result;
        }

        @Override
        public void removeBond() {
            writeRegister(SettingsRegister.DELETE_BOND, (byte) 1);
        }

        @Override
        public void keepBond() {
            writeRegister(SettingsRegister.DELETE_BOND, (byte) 0);
        }

        @Override
        public void startAdvertisement() {
            writeRegister(SettingsRegister.START_ADVERTISEMENT);
        }

        @Override
        public void initiateBonding() {
            writeRegister(SettingsRegister.INIT_BOND);
        }
    }

    private final ConcurrentLinkedQueue<AsyncResultImpl<byte[]>> i2cReadResults= new ConcurrentLinkedQueue<>();
    private class I2CImpl implements I2C {
        @Override
        public void writeData(byte deviceAddr, byte registerAddr, byte[] data) {
            byte[] params= new byte[data.length + 4];
            params[0]= deviceAddr;
            params[1]= registerAddr;
            params[2]= (byte) 0xff;
            params[3]= (byte) data.length;
            System.arraycopy(data, 0, params, 4, data.length);

            writeRegister(I2CRegister.READ_WRITE, params);
        }

        @Override
        public AsyncResult<byte[]> readData(byte deviceAddr, byte registerAddr, byte numBytes) {
            AsyncResultImpl<byte[]> result= new AsyncResultImpl<>();

            i2cReadResults.add(result);
            readRegister(I2CRegister.READ_WRITE, deviceAddr, registerAddr, (byte) 0xff, numBytes);
            return result;
        }
    }

    private final ConcurrentLinkedQueue<AsyncResultImpl<Byte>> macroIds= new ConcurrentLinkedQueue<>();
    private class MacroImpl implements Macro {
        @Override
        public AsyncResult<Byte> record(boolean execOnBoot) {
            AsyncResultImpl<Byte> idResult= new AsyncResultImpl<>();

            macroIds.add(idResult);
            writeRegister(MacroRegister.NOTIFY, (byte) 1);
            writeRegister(MacroRegister.BEGIN, (byte) (execOnBoot ? 1 : 0));
            recordingMacro= true;

            return idResult;
        }

        @Override
        public void stop() {
            recordingMacro= false;
            writeRegister(MacroRegister.END);
        }

        @Override
        public void execute(byte macroId) {
            writeRegister(MacroRegister.EXECUTE, macroId);
        }

        @Override
        public void eraseMacros() {
            writeRegister(MacroRegister.ERASE_ALL);
        }
    }

    @Override
    public <T extends Module> T getModule(Class<T> moduleClass) {
        if (moduleClass.equals(Led.class)) {
            return moduleClass.cast(new LedImpl());
        } else if (moduleClass.equals(Temperature.class)) {
            return moduleClass.cast(new TemperatureImpl());
        } else if (moduleClass.equals(Gpio.class)) {
            return moduleClass.cast(new GpioImpl());
        } else if (moduleClass.equals(Accelerometer.class)) {
            if (moduleNumber.equals(Constant.METAWEAR_R_MODULE)) {
                return moduleClass.cast(new Mma8452qAccelerometerImpl());
            } else if (moduleNumber.equals(Constant.METAWEAR_RG_MODULE)) {
                return moduleClass.cast(new Bmi160AccelerometerImpl());
            }
            return null;
        } else if (moduleClass.equals(Timer.class)) {
            return moduleClass.cast(new TimerImpl());
        } else if (moduleClass.equals(Debug.class)) {
            return moduleClass.cast(new DebugImpl());
        } else if (moduleClass.equals(Logging.class)) {
            return moduleClass.cast(new LoggingImpl());
        } else if (moduleClass.equals(Bmi160Gyro.class)) {
            if (moduleNumber.equals(Constant.METAWEAR_RG_MODULE)) {
                return moduleClass.cast(new Bmi160GyroImpl());
            }
            return null;
        } else if (moduleClass.equals(Gsr.class)) {
            return moduleClass.cast(new GsrImpl());
        } else if (moduleClass.equals(IBeacon.class)) {
            return moduleClass.cast(new IBeaconImpl());
        } else if (moduleClass.equals(Haptic.class)) {
            return moduleClass.cast(new HapticImpl());
        } else if (moduleClass.equals(NeoPixel.class)) {
            return moduleClass.cast(new NeoPixelImpl());
        } else if (moduleClass.equals(Settings.class)) {
            return moduleClass.cast(new SettingsImpl());
        } else if (moduleClass.equals(MultiChannelTemperature.class)) {
            return moduleClass.cast(new MultiChannelTemperatureImpl());
        } else if (moduleClass.equals(I2C.class)) {
            return moduleClass.cast(new I2CImpl());
        } else if (moduleClass.equals(Macro.class)) {
            return moduleClass.cast(new MacroImpl());
        }
        throw new ClassCastException("Unrecognized module class: \'" + moduleClass.toString() + "\'");
    }

    private final byte[] mma8452qDataSampling= new byte[] {0, 0, 0x18, 0, 0};
    private class Mma8452qAccelerometerImpl implements Mma8452qAccelerometer {
        @Override
        public void setAxisSamplingRange(float range) {
            final float[] values= new float[] { 2.f, 4.f, 8.f };
            int closest= closestIndex(values, range);

            mma8452qDataSampling[0] &= 0xfc;
            mma8452qDataSampling[0] |= closest;
        }

        @Override
        public void setOutputDataRate(float frequency) {
            final float[] values = new float[]{1.56f, 6.25f, 12.5f, 50.f, 100.f, 200.f, 400.f, 800.f};
            int closest = closestIndex(values, frequency);

            mma8452qDataSampling[2] &= 0xc7;
            mma8452qDataSampling[2] |= (values.length - closest - 1) << 3;
        }

        @Override
        public void startAxisSampling() {
            writeRegister(MwrAccelerometerRegister.DATA_CONFIG, mma8452qDataSampling);
            writeRegister(MwrAccelerometerRegister.DATA_ENABLE, (byte) 1);
        }

        @Override
        public void stopAxisSampling() {
            writeRegister(MwrAccelerometerRegister.DATA_ENABLE, (byte) 0);
        }

        @Override
        public void globalStart() {
            writeRegister(MwrAccelerometerRegister.GLOBAL_ENABLE, (byte) 1);
        }
        @Override
        public void globalStop() {
            writeRegister(MwrAccelerometerRegister.GLOBAL_ENABLE, (byte) 0);
        }

        @Override
        public SamplingConfig configureXYZSampling() {
            return new SamplingConfig() {
                @Override
                public SamplingConfig withFullScaleRange(
                        FullScaleRange range) {
                    mma8452qDataSampling[0] &= 0xfc;
                    mma8452qDataSampling[0] |= range.ordinal();
                    return this;
                }

                public SamplingConfig withOutputDataRate(OutputDataRate rate) {
                    mma8452qDataSampling[2] &= 0xc7;
                    mma8452qDataSampling[2] |= (rate.ordinal() << 3);
                    return this;
                }

                @Override
                public SamplingConfig withHighPassFilter(byte cutoff) {
                    mma8452qDataSampling[0] |= 0x10;
                    mma8452qDataSampling[1] |= (cutoff & 0x3);
                    return this;
                }

                @Override
                public SamplingConfig withHighPassFilter() {
                    mma8452qDataSampling[0] |= 0x10;
                    return this;
                }

                @Override
                public SamplingConfig withoutHighPassFilter() {
                    mma8452qDataSampling[0] &= 0xef;
                    return this;
                }
            };
        }
    }

    private Bmi160Accelerometer.AccRange bmi160AccRange= Bmi160Accelerometer.AccRange.AR_2G;
    private final byte[] bmi160DataSampling= new byte[] {
            (byte) (0x20 | Bmi160Accelerometer.OutputDataRate.ODR_100_HZ.bitMask()),
            Bmi160Accelerometer.AccRange.AR_2G.bitMask()
    };
    private class Bmi160AccelerometerImpl implements Bmi160Accelerometer {
        @Override
        public AxisSamplingConfigEditor configureAxisSampling() {
            return new AxisSamplingConfigEditor() {
                @Override
                public AxisSamplingConfigEditor withDataRange(AccRange range) {
                    bmi160AccRange= range;
                    bmi160DataSampling[1]&= 0xf0;
                    bmi160DataSampling[1]|= range.bitMask();
                    return this;
                }

                @Override
                public AxisSamplingConfigEditor withOutputDataRate(OutputDataRate odr) {
                    bmi160DataSampling[0]&= 0xf0;
                    bmi160DataSampling[0]|= odr.bitMask();
                    return this;
                }

                @Override
                public void commit() {
                    writeRegister(Bmi160AccelerometerRegister.DATA_CONFIG, bmi160DataSampling);
                }
            };
        }

        @Override
        public void setAxisSamplingRange(float range) {
            final float[] values= new float[] { 2.f, 4.f, 8.f, 16.f };
            int closest= closestIndex(values, range);

            bmi160DataSampling[1]&= 0xf0;
            bmi160DataSampling[1]|= Bmi160Accelerometer.AccRange.values()[closest].bitMask();
            writeRegister(Bmi160AccelerometerRegister.DATA_CONFIG, bmi160DataSampling);
        }

        @Override
        public void setOutputDataRate(float frequency) {
            final float[] values= new float[] { 0.78125f, 1.5625f, 3.125f, 6.25f, 12.5f, 25.f, 50.f, 100.f, 200.f, 400.f, 800.f, 1600.f };
            int closest= closestIndex(values, frequency);

            bmi160DataSampling[0]&= 0xf0;
            bmi160DataSampling[0]|= Bmi160Accelerometer.OutputDataRate.values()[closest].bitMask();
            writeRegister(Bmi160AccelerometerRegister.DATA_CONFIG, bmi160DataSampling);
        }

        @Override
        public void startAxisSampling() {
            writeRegister(Bmi160AccelerometerRegister.DATA_INTERRUPT_ENABLE, (byte) 0x1, (byte) 0x0);
        }

        @Override
        public void stopAxisSampling() {
            writeRegister(Bmi160AccelerometerRegister.DATA_INTERRUPT_ENABLE, (byte) 0x0, (byte) 0x1);
        }

        @Override
        public void globalStart() {
            writeRegister(Bmi160AccelerometerRegister.POWER_MODE, (byte) 0x1);
        }
        public void globalStop() {
            writeRegister(Bmi160AccelerometerRegister.POWER_MODE, (byte) 0x0);
        }
    }

    private Bmi160Gyro.FullScaleRange bmi160GyroRange = Bmi160Gyro.FullScaleRange.FSR_2000;
    private final byte[] bmi160GyroConfig= new byte[] {
            (byte) (0x20 | Bmi160Gyro.OutputDataRate.ODR_100_HZ.bitMask()),
            Bmi160Gyro.FullScaleRange.FSR_2000.bitMask(),
    };
    private class Bmi160GyroImpl implements Bmi160Gyro {
        @Override
        public ConfigEditor configure() {
            return new ConfigEditor() {
                @Override
                public ConfigEditor withFullScaleRange(FullScaleRange range) {
                    bmi160GyroRange = range;
                    bmi160GyroConfig[1] &= 0xf8;
                    bmi160GyroConfig[1] |= range.bitMask();
                    return this;
                }

                @Override
                public ConfigEditor withOutputDataRate(OutputDataRate odr) {
                    bmi160GyroConfig[0] &= 0xf0;
                    bmi160GyroConfig[0] |= odr.bitMask();

                    return this;
                }

                @Override
                public void commit() {
                    writeRegister(Bmi160GyroRegister.CONFIG, bmi160GyroConfig);
                }
            };
        }

        @Override
        public void globalStart() {
            writeRegister(Bmi160GyroRegister.DATA_INTERRUPT_ENABLE, (byte) 1, (byte) 0);
            writeRegister(Bmi160GyroRegister.POWER_MODE, (byte) 1);
        }

        @Override
        public void globalStop() {
            writeRegister(Bmi160GyroRegister.POWER_MODE, (byte) 0);
            writeRegister(Bmi160GyroRegister.DATA_INTERRUPT_ENABLE, (byte) 0, (byte) 1);
        }
    }

    private final ConcurrentLinkedQueue<AsyncResultImpl<MultiChannelTemperature.Source[]>> tempSourcesResults= new ConcurrentLinkedQueue<>();
    private class MultiChannelTemperatureImpl implements MultiChannelTemperature {
        @Override
        public AsyncResult<Source[]> readSources() {
            AsyncResultImpl<MultiChannelTemperature.Source[]> result= new AsyncResultImpl<>();
            tempSourcesResults.add(result);
            readRegister(MultiChannelTempRegister.INFO);
            return result;
        }

        @Override
        public void readTemperature(Source src) {
            readRegister(MultiChannelTempRegister.TEMPERATURE, src.channel());
        }
    }

    private boolean recordingMacro= false;
    private EventListener currEventListener;
    private byte[] eventConfig= null;
    private MessageToken eventParams= null;
    private byte eventDestOffset= 0;
    private void buildBlePacket(boolean forceNoMacro, byte module, byte register, byte ... parameters) {
        byte[] cmd= new byte[parameters.length + 2];
        cmd[0]= module;
        cmd[1]= register;
        System.arraycopy(parameters, 0, cmd, 2, parameters.length);

        if (eventConfig != null) {
            currEventListener.onCommandWrite();
            byte[] eventEntry= new byte[] {EventRegister.ENTRY.moduleOpcode(), EventRegister.ENTRY.opcode(),
                    eventConfig[0], eventConfig[1], eventConfig[2],
                    cmd[0], cmd[1], (byte) (cmd.length - 2)};
            if (eventParams != null) {
                byte[] tempEntry= new byte[eventEntry.length + 2];
                System.arraycopy(eventEntry, 0, tempEntry, 0, eventEntry.length);
                tempEntry[eventEntry.length]= (byte) (0x01 | ((eventParams.length() << 1) & 0xff) | ((eventParams.offset() << 4) & 0xff));
                tempEntry[eventEntry.length + 1]= eventDestOffset;
                eventEntry= tempEntry;
            }
            conn.sendCommand(!forceNoMacro && recordingMacro, eventEntry);

            byte[] eventParameters= new byte[cmd.length];
            System.arraycopy(cmd, 2, eventParameters, 2, cmd.length - 2);
            eventParameters[0]= EventRegister.CMD_PARAMETERS.moduleOpcode();
            eventParameters[1]= EventRegister.CMD_PARAMETERS.opcode();
            conn.sendCommand(!forceNoMacro && recordingMacro, eventParameters);
        } else {
            conn.sendCommand(!forceNoMacro && recordingMacro, cmd);
        }
    }
    private void writeRegister(boolean forceNoMacro, Register register, byte ... parameters) {
        buildBlePacket(forceNoMacro, register.moduleOpcode(), register.opcode(), parameters);
    }
    private void writeRegister(Register register, byte ... parameters) {
        writeRegister(false, register, parameters);
    }
    private void readRegister(Register register, byte ... parameters) {
        buildBlePacket(false, register.moduleOpcode(), (byte) (0x80 | register.opcode()), parameters);
    }
}