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
import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteBuilder;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.impl.characteristic.*;
import com.mbientlab.metawear.module.*;
import com.mbientlab.metawear.processor.*;
import com.mbientlab.metawear.data.*;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by etsai on 6/15/2015.
 */
public abstract class DefaultMetaWearBoard implements MetaWearBoard, Connection.ResponseListener {
    private static Map<String, String> parseQuery(String query) {
        HashMap<String, String> queryTokens= new HashMap<>();

        for(String token: query.split("&")) {
            String[] keyVal= token.split("=");
            queryTokens.put(keyVal[0], keyVal[1]);
        }

        return queryTokens;
    }

    private static final HashMap<String, Class<? extends DataSignal.DataTransformer>> transformerSchemes;
    private static final HashMap<String, Class<? extends DataSignal.DataFilter>> filterSchemes;

    static {
        transformerSchemes= new HashMap<>();
        transformerSchemes.put(Accumulator.SCHEME_NAME, Accumulator.class);
        transformerSchemes.put(Average.SCHEME_NAME, Average.class);
        transformerSchemes.put(com.mbientlab.metawear.processor.Math.SCHEME_NAME, com.mbientlab.metawear.processor.Math.class);
        transformerSchemes.put(Rms.SCHEME_NAME, Rms.class);
        transformerSchemes.put(Rss.SCHEME_NAME, Rss.class);
        transformerSchemes.put(Time.SCHEME_NAME, Time.class);

        filterSchemes= new HashMap<>();
        filterSchemes.put(Comparison.SCHEME_NAME, Comparison.class);
        filterSchemes.put(Passthrough.SCHEME_NAME, Passthrough.class);
        filterSchemes.put(Time.SCHEME_NAME, Time.class);
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

    private class RouteBuilderImpl implements RouteBuilder, EventListener {
        public abstract class DataSignalImpl implements DataSignal, Loggable {
            protected DataSignalImpl parent;
            protected Class<? extends Message> msgClass;
            protected final byte outputSize;
            protected MessageProcessor logProcessor;

            protected DataSignalImpl(byte outputSize, Class<? extends Message> msgClass) {
                this.outputSize= outputSize;
                this.msgClass= msgClass;
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
                    public DataSignal monitor(ActivityMonitor monitor) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal filter(String uri) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal filter(DataFilter filter) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal transform(String uri) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal transform(DataTransformer transformer) {
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
            public DataSignal filter(String uri) {
                String[] uriSplit= uri.split("\\?");

                if (filterSchemes.containsKey(uriSplit[0])) {
                    try {
                        Map<String, String> query= uriSplit.length > 1 ? parseQuery(uriSplit[1]) : new HashMap<String, String>();
                        Constructor<?> cTor = filterSchemes.get(uriSplit[0]).getConstructor(Map.class);
                        return filter((DataFilter) cTor.newInstance(query));
                    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException("Error instantiating data filter \'" + uriSplit[0] + "\'", e);
                    }
                } else {
                    throw new RuntimeException("Data filter \'" + uriSplit[0] + "\' not recognized");
                }
            }

            @Override
            public DataSignal transform(String uri) {
                String[] uriSplit= uri.split("\\?");

                if (transformerSchemes.containsKey(uriSplit[0])) {
                    try {
                        Map<String, String> query= uriSplit.length > 1 ? parseQuery(uriSplit[1]) : new HashMap<String, String>();
                        Constructor<?> cTor = transformerSchemes.get(uriSplit[0]).getConstructor(Map.class);
                        return transform((DataTransformer) cTor.newInstance(query));
                    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException("Error instantiating data filter \'" + uriSplit[0] + "\'", e);
                    }
                } else {
                    throw new RuntimeException("Data filter \'" + uriSplit[0] + "\' not recognized");
                }
            }

            @Override
            public DataSignal filter(DataFilter filter) {
                return filter(filter, this);
            }

            @Override
            public DataSignal transform(DataTransformer transformer) {
                return transform(transformer, this);
            }

            protected void postFilterCreate(final DataProcessor newProcessor, final DataSignalImpl parent) {
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

                try {
                    Constructor<?> cTor= msgClass.getConstructor(Calendar.class, byte[].class);
                    Message logMsg= (Message) cTor.newInstance(timestamp, Arrays.copyOfRange(entry, 5, entry.length));
                    logProcessor.process(logMsg);
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException ex) {
                    throw new RuntimeException("Cannot instantiate message class", ex);
                }
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

            protected DataSignal filter(DataFilter filter, final DataSignalImpl parent) {
                DataProcessor newProcessor;

                if (filter instanceof Passthrough) {
                    final Passthrough params= (Passthrough) filter;
                    newProcessor= new StaticDataProcessor(outputSize, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            ByteBuffer buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0x1)
                                    .put((byte) (params.passthroughMode.ordinal() & 0x7)).putShort(params.value);
                            return buffer.array();
                        }
                    };
                } else if (filter instanceof Comparison) {
                    final Comparison params= (Comparison) filter;
                    newProcessor= new StaticDataProcessor(outputSize, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            boolean signed= params.signed == null ? isSigned() : params.signed;
                            Number firmwareReference= numberToFirmwareUnits(params.reference);
                            ByteBuffer buffer= ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0x6)
                                    .put((byte) (signed ? 1 : 0)).put((byte) params.compareOp.ordinal()).put((byte) 0)
                                    .putInt(firmwareReference.intValue());
                            return buffer.array();
                        }
                    };
                } else if (filter instanceof Time) {
                    final Time params= (Time) filter;
                    newProcessor= new StaticDataProcessor(outputSize, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            ByteBuffer buffer= ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0x8)
                                    .put((byte) (outputSize - 1)).putInt(params.period);
                            return buffer.array();
                        }
                    };
                } else {
                    throw new ClassCastException("Unrecognized DataFilter subtype: \'" + filter.getClass().toString() + "\'");
                }

                postFilterCreate(newProcessor, parent);
                return newProcessor;
            }

            protected DataSignal transform(DataTransformer transformer, final DataSignalImpl parent) {
                DataProcessor newProcessor;

                if (transformer instanceof Accumulator) {
                    final Accumulator params= (Accumulator) transformer;
                    final byte filterOutputSize= params.output == null ? outputSize : params.output;

                    newProcessor= new DataProcessor(filterOutputSize, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            return new byte[] {0x2, (byte) (((this.outputSize - 1) & 0x3) | (((DataSignalImpl.this.outputSize - 1) & 0x3) << 2))};
                        }

                        @Override
                        public boolean isSigned() {
                            return DataSignalImpl.this.isSigned();
                        }

                        @Override
                        public Number numberToFirmwareUnits(Number input) {
                            return parent.numberToFirmwareUnits(input);
                        }
                    };
                } else if (transformer instanceof Average) {
                    final Average params= (Average) transformer;
                    newProcessor= new DataProcessor(outputSize, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            return new byte[] {0x3, (byte) (((outputSize - 1) & 0x3) | (((outputSize - 1) & 0x3) << 2)),
                                    params.sampleSize};
                        }

                        @Override
                        public boolean isSigned() {
                            return DataSignalImpl.this.isSigned();
                        }

                        @Override
                        public Number numberToFirmwareUnits(Number input) {
                            return parent.numberToFirmwareUnits(input);
                        }
                    };
                } else if (transformer instanceof com.mbientlab.metawear.processor.Math) {
                    final com.mbientlab.metawear.processor.Math params= (com.mbientlab.metawear.processor.Math) transformer;
                    newProcessor= new DataProcessor((byte) 4, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            byte signedMask= (byte) (params.signed == null ? (isSigned() ? 0x10 : 0x0) : (params.signed ? 0x10 : 0x0));
                            Number firmwareRhs;

                            switch(params.mathOp) {
                                case ADD:
                                case SUBTRACT:
                                case MODULUS:
                                    firmwareRhs= numberToFirmwareUnits(params.rhs);
                                    break;
                                default:
                                    firmwareRhs= params.rhs;
                            }

                            ByteBuffer buffer= ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0x9)
                                    .put((byte) ((this.outputSize - 1) & 0x3 | ((DataSignalImpl.this.outputSize - 1) << 2) | signedMask))
                                    .put((byte) params.mathOp.ordinal()).putInt(firmwareRhs.intValue());
                            return buffer.array();
                        }

                        @Override
                        public boolean isSigned() {
                            return true;
                        }

                        @Override
                        public Number numberToFirmwareUnits(Number input) {
                            return parent.numberToFirmwareUnits(input);
                        }
                    };
                } else if (transformer instanceof Time) {
                    final Time params= (Time) transformer;
                    newProcessor= new DataProcessor(outputSize, msgClass) {
                        @Override
                        public byte[] getFilterConfig() {
                            ByteBuffer buffer= ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0x8)
                                    .put((byte) ((outputSize - 1) | (1 << 3))).putInt(params.period);
                            return buffer.array();
                        }

                        @Override
                        public boolean isSigned() {
                            return DataSignalImpl.this.isSigned();
                        }

                        @Override
                        public Number numberToFirmwareUnits(Number input) {
                            return parent.numberToFirmwareUnits(input);
                        }
                    };
                } else if (transformer instanceof Rms) {
                    throw new UnsupportedOperationException("Cannot attach rms transformer to single component data");
                } else if (transformer instanceof Rss) {
                    throw new UnsupportedOperationException("Cannot attach rss transformer to single component data");
                } else {
                    throw new ClassCastException("Unrecognized DataTransformer subtype: \'" + transformer.getClass().toString() + "\'");
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
        }

        public abstract class DataProcessor extends DataSignalImpl {
            private byte id= -1;
            private MessageProcessor processor;

            protected DataProcessor(byte outputSize, Class<? extends Message> msgClass) {
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
            public byte[] getEventConfig() {
                if (id == -1) {
                    throw new RuntimeException("ID has not been set yet");
                }
                return new byte[] {DataProcessorRegister.NOTIFY.moduleOpcode(), DataProcessorRegister.NOTIFY.opcode(), id};
            }

            public void setId(byte newId) {
                this.id= newId;
            }

            public abstract byte[] getFilterConfig();
        }
        public abstract class StaticDataProcessor extends DataProcessor {
            public StaticDataProcessor(byte outputSize, Class<? extends Message> msgClass) {
                super(outputSize, msgClass);
            }

            @Override
            public DataSignal filter(DataFilter filter) {
                return parent.filter(filter, this);
            }

            @Override
            public DataSignal transform(DataTransformer transformer) {
                return parent.transform(transformer, this);
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

        private AsyncResultImpl<RouteManager> commitResult;
        private DataSource routeSource;
        private boolean hasDataLogger= false;
        private byte nExpectedCmds= 0;
        private final Stack<DataSignalImpl> branches= new Stack<>();
        private final Queue<IdCreator> creators= new LinkedList<>();
        private final HashMap<DataSignalImpl, DataSignal.ActivityMonitor> signalMonitors= new HashMap<>();
        private final LinkedHashSet<DataSignalImpl> activeSignals= new LinkedHashSet<>();
        private final HashSet<Byte> filterIds= new HashSet<>(), eventCmdIds= new HashSet<>(), loggingIds= new HashSet<>();

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
            };

            return routeSource;
        }

        @Override
        public DataSignal fromTemperature() {
            routeSource= new DataSource((byte) 2, TemperatureMessage.class) {
                @Override
                public DataSignal subscribe(MessageProcessor processor) {
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

        @Override
        public DataSignal fromAccelAxis() {
            routeSource= new DataSource((byte) 6, MwrAccelAxisMessage.class) {
                private final byte XY_LENGTH= 4, Z_OFFSET= XY_LENGTH;
                private byte xyId= -1, zId= -1;
                private Queue<byte[]> xyLogEntries= new LinkedList<>(), zLogEntries= new LinkedList<>();

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
                public byte[] getEventConfig() {
                    return new byte[] {MwrAccelerometerRegister.DATA_VALUE.moduleOpcode(), MwrAccelerometerRegister.DATA_VALUE.opcode(),
                            (byte) 0xff};
                }

                @Override
                public DataSignal log(MessageProcessor processor, final DataSignalImpl source) {
                    final DataSignalImpl self= this;

                    creators.add(new IdCreator() {
                        @Override
                        public void execute() {
                            final byte[] triggerConfig= source.getTriggerConfig();
                            final byte[] xyLogCfg= new byte[triggerConfig.length];
                            System.arraycopy(triggerConfig, 0, xyLogCfg, 0, triggerConfig.length - 1);
                            xyLogCfg[triggerConfig.length - 1]= (byte) ((XY_LENGTH - 1) << 5);

                            writeRegister(LoggingRegister.TRIGGER, xyLogCfg);
                        }

                        @Override
                        public void receivedId(byte id) {
                            loggingIds.add(id);
                            xyId= id;
                            dataLoggers.put(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, id), self);
                        }
                    });
                    creators.add(new IdCreator() {
                        @Override
                        public void execute() {
                            final byte[] triggerConfig= source.getTriggerConfig();
                            final byte[] zLogCfg= new byte[triggerConfig.length];
                            System.arraycopy(triggerConfig, 0, zLogCfg, 0, triggerConfig.length - 1);
                            zLogCfg[triggerConfig.length - 1]= (byte) ((((outputSize - XY_LENGTH) - 1) << 5) | Z_OFFSET);

                            writeRegister(LoggingRegister.TRIGGER, zLogCfg);
                        }

                        @Override
                        public void receivedId(byte id) {
                            loggingIds.add(id);
                            zId= id;
                            dataLoggers.put(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, id), self);
                        }
                    });
                    this.logProcessor= processor;
                    hasDataLogger= true;
                    return this;
                }

                @Override
                public void receivedLogEntry(byte[] entry) {
                    byte logId= (byte) (entry[0] & 0xf);

                    if (logId == xyId) {
                        xyLogEntries.add(entry);
                    } else if (logId == zId) {
                        zLogEntries.add(entry);
                    } else {
                        throw new RuntimeException("Unknown log id in this fn: " + logId);
                    }

                    if (!xyLogEntries.isEmpty() && !zLogEntries.isEmpty()) {
                        byte[] xyEntry= xyLogEntries.poll(), zEntry= zLogEntries.poll();
                        byte[] merged= new byte[xyEntry.length + zEntry.length - 7];
                        System.arraycopy(xyEntry, 1, merged, 1, xyEntry.length - 1);
                        System.arraycopy(zEntry, 5, merged, xyEntry.length, 2);

                        super.receivedLogEntry(merged);
                    }
                }

                @Override
                public boolean isSigned() {
                    throw new UnsupportedOperationException("isSigned method not supported for raw accelerometer axis data");
                }

                @Override
                public DataSignal filter(DataFilter filter) {
                    if (filter instanceof Comparison) {
                        throw new UnsupportedOperationException("Cannot compare raw accelerometer axis data");
                    }
                    return super.filter(filter);
                }

                @Override
                public DataSignal transform(DataTransformer transformer, final DataSignalImpl parent) {
                    if (transformer instanceof Accumulator) {
                        throw new UnsupportedOperationException("Cannot accumulate raw accelerometer axis data");
                    } else if (transformer instanceof Average) {
                        throw new UnsupportedOperationException("Cannot average raw accelerometer axis data");
                    } else if (transformer instanceof com.mbientlab.metawear.processor.Math) {
                        throw new UnsupportedOperationException("Cannot perform arithmetic operations on raw accelerometer axis data");
                    } else if (transformer instanceof Rms || transformer instanceof Rss) {
                        final byte nInputs= 3, rmsMode= (byte) (transformer instanceof Rms ? 0 : 1);
                        DataProcessor newProcessor= new DataProcessor((byte) 2, MwrAccelCombinedAxisMessage.class) {
                            @Override
                            public byte[] getFilterConfig() {
                                return new byte[] {0x7,
                                        (byte) (((this.outputSize - 1) & 0x3) | (((this.outputSize - 1) & 0x3) << 2) | ((nInputs - 1) << 4) | 0x80),
                                        rmsMode};
                            }

                            @Override
                            public boolean isSigned() {
                                return false;
                            }

                        };

                        postFilterCreate(newProcessor, parent);
                        return newProcessor;
                    }

                    return super.transform(transformer, parent);
                }
            };

            return routeSource;
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
                @Override
                public DataSignal subscribe(MessageProcessor processor) {
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
                public byte[] getEventConfig() {
                    return new byte[] {analogRegister.moduleOpcode(), (byte) (0x80 | analogRegister.opcode()), pin};
                }
            };

            return routeSource;
        }

        @Override
        public DataSignal fromDigitalIn(final byte pin) {
            routeSource= new DataSource((byte) 1, GpioDigitalMessage.class) {
                @Override
                public DataSignal subscribe(MessageProcessor processor) {
                    responseProcessors.put(new ResponseHeader(GpioRegister.READ_DI, pin), processor);
                    return super.subscribe(processor);
                }

                @Override
                public void remove() {
                    responseProcessors.remove(new ResponseHeader(GpioRegister.READ_DI, pin));
                    super.remove();
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

        public void receivedId(byte id) {
            creators.poll().receivedId(id);
            addId();
        }

        private void eventCommandsCheck() {
            if (isConnected()) {
                if (nExpectedCmds == 0) {
                    currEventListner = null;
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

        private void addId() {
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

            currEventListner= this;
            for(Map.Entry<DataSignalImpl, DataSignal.ActivityMonitor> it: signalMonitors.entrySet()) {
                eventConfig= it.getKey().getEventConfig();
                it.getValue().onSignalActive();
            }
            signalMonitors.clear();
            eventConfig= null;

            eventCommandsCheck();
        }

        public void onCommandWrite() {
            nExpectedCmds++;
        }
    }

    private interface ReferenceTick {
        public long tickCount();
        public Calendar timestamp();
    }

    private final byte[] mma8452qDataSampling= new byte[] {0, 0, 0x18, 0, 0};

    private Logging.DownloadHandler downloadHandler;
    private float notifyProgress= 1.0f;
    private int nLogEntries;
    private ReferenceTick logReferenceTick= null;
    private final AtomicBoolean commitRoutes= new AtomicBoolean(false);
    private final Queue<RouteBuilderImpl> pendingRoutes= new LinkedList<>();
    private final HashMap<ResponseHeader, DataSignal.MessageProcessor> responseProcessors= new HashMap<>();
    private final HashMap<ResponseHeader, Class<? extends Message>> dataProcMsgClasses= new HashMap<>();
    private final HashMap<ResponseHeader, Loggable> dataLoggers= new HashMap<>();
    private final ConcurrentLinkedQueue<AsyncResultImpl<Timer.Controller>> timerControllerResults= new ConcurrentLinkedQueue<>();

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
        responses.put(new ResponseHeader(TemperatureRegister.VALUE), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                byte[] respBody= new byte[response.length - 2];
                System.arraycopy(response, 2, respBody, 0, respBody.length);

                return new Response(new TemperatureMessage(respBody), new ResponseHeader(response[0], response[1]));
            }
        });
        responses.put(new ResponseHeader(MwrAccelerometerRegister.DATA_VALUE), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                byte[] respBody= new byte[response.length - 2];
                System.arraycopy(response, 2, respBody, 0, respBody.length);

                return new Response(new MwrAccelAxisMessage(respBody), new ResponseHeader(response[0], response[1]));
            }
        });
        responses.put(new ResponseHeader(EventRegister.ENTRY), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                currEventListner.receivedCommandId(response[2]);
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
                    Constructor<?> cTor= dataProcMsgClasses.get(header).getConstructor(byte[].class);

                    return new Response((Message) cTor.newInstance(respBody), header);
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
    }

    private EventListener currEventListner;
    private byte[] eventConfig= null;
    private void buildBlePacket(byte module, byte register, byte ... parameters) {
        byte[] cmd= new byte[parameters.length + 2];
        cmd[0]= module;
        cmd[1]= register;
        System.arraycopy(parameters, 0, cmd, 2, parameters.length);

        if (eventConfig != null) {
            currEventListner.onCommandWrite();
            byte[] eventEntry= new byte[] {EventRegister.ENTRY.moduleOpcode(), EventRegister.ENTRY.opcode(),
                    eventConfig[0], eventConfig[1], eventConfig[2],
                    cmd[0], cmd[1], (byte) (cmd.length - 2)};
            conn.sendCommand(eventEntry);

            byte[] eventParameters= new byte[cmd.length];
            System.arraycopy(cmd, 2, eventParameters, 2, cmd.length - 2);
            eventParameters[0]= EventRegister.CMD_PARAMETERS.moduleOpcode();
            eventParameters[1]= EventRegister.CMD_PARAMETERS.opcode();
            conn.sendCommand(eventParameters);
        } else {
            conn.sendCommand(cmd);
        }
    }
    private void writeRegister(Register register, byte ... parameters) {
        buildBlePacket(register.moduleOpcode(), register.opcode(), parameters);
    }
    private void readRegister(Register register, byte ... parameters) {
        buildBlePacket(register.moduleOpcode(), (byte) (0x80 | register.opcode()), parameters);
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
    private class MwrAccelerometerImpl implements MwrAccelerometer {
        @Override
        public void globalStart() {
            writeRegister(MwrAccelerometerRegister.GLOBAL_ENABLE, (byte) 1);
        }
        @Override
        public void globalStop() {
            writeRegister(MwrAccelerometerRegister.GLOBAL_ENABLE, (byte) 0);
        }

        @Override
        public void startXYZSampling() {
            writeRegister(MwrAccelerometerRegister.DATA_CONFIG, mma8452qDataSampling);
            writeRegister(MwrAccelerometerRegister.DATA_ENABLE, (byte) 1);
        }

        @Override
        public void stopXYZSampling() {
            writeRegister(MwrAccelerometerRegister.DATA_ENABLE, (byte) 0);
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
    private class TimerControllerImpl implements EventListener, Timer.Controller {
        private final byte timerId;
        private final byte[] timerEventConfig;
        private byte nExpectedCommands= 0;
        private HashSet<Byte> eventCmdIds;

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
        public void monitor(DataSignal.ActivityMonitor monitor) {
            currEventListner= this;

            eventConfig= timerEventConfig;
            monitor.onSignalActive();
            eventConfig= null;
        }

        @Override
        public void receivedCommandId(byte id) {
            eventCmdIds.add(id);
            nExpectedCommands--;

            if (nExpectedCommands == 0) {
                currEventListner= null;
            }
        }

        @Override
        public void onCommandWrite() {
            nExpectedCommands++;
        }
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

    @Override
    public <T extends GenericModule> T getModule(Class<T> moduleClass) {
        if (moduleClass.equals(Led.class)) {
            return moduleClass.cast(new LedImpl());
        } else if (moduleClass.equals(Temperature.class)) {
            return moduleClass.cast(new TemperatureImpl());
        } else if (moduleClass.equals(Gpio.class)) {
            return moduleClass.cast(new GpioImpl());
        } else if (moduleClass.equals(MwrAccelerometer.class)) {
            return moduleClass.cast(new MwrAccelerometerImpl());
        } else if (moduleClass.equals(Timer.class)) {
            return moduleClass.cast(new TimerImpl());
        } else if (moduleClass.equals(Debug.class)) {
            return moduleClass.cast(new DebugImpl());
        } else if (moduleClass.equals(Logging.class)) {
            return moduleClass.cast(new LoggingImpl());
        }
        throw new ClassCastException("Unrecognized module class: \'" + moduleClass.toString() + "\'");
    }
}