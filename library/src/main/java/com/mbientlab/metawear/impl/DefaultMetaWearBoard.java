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
import com.mbientlab.metawear.MessageToken;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteBuilder;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.impl.characteristic.*;
import com.mbientlab.metawear.module.*;
import com.mbientlab.metawear.processor.*;
import com.mbientlab.metawear.data.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by etsai on 6/15/2015.
 */
public abstract class DefaultMetaWearBoard implements MetaWearBoard, Connection.ResponseListener {
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

    public final static byte MW_COMMAND_LENGTH = 18;
    private static final String JSON_FIELD_MSG_CLASS= "message_class", JSON_FIELD_LOG= "logs", JSON_FIELD_ROUTE= "route",
            JSON_FIELD_SUBSCRIBERS="subscribers", JSON_FIELD_NAME= "name", JSON_FIELD_TIMER= "timers", JSON_FIELD_CMD_IDS="cmd_ids",
            JSON_FIELD_MMA8452Q_ODR= "mma8452q_odr", JSON_FIELD_BMI160_ACC_RANGE= "bmi160_acc_range", JSON_FIELD_BMI160_GRYO_RANGE= "bmi160_gyro_range",
            JSON_FIELD_ROUTE_MANAGER= "route_managers";
    private final static double TICK_TIME_STEP= (48.0 / 32768.0) * 1000.0;
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

    private interface Subscription {
        public static final String JSON_FIELD_NOTIFY_BYTES= "notify", JSON_FIELD_SILENCE_BYTES= "silence", JSON_FIELD_HEADER= "header";

        public void unsubscribe();
        public void resubscribe(DataSignal.MessageProcessor processor);
        public JSONObject serializeState();
    }

    private class RouteManagerImpl implements RouteManager {
        private static final String JSON_FIELD_ID= "id", JSON_FIELD_FILTER_IDS="filter_ids", JSON_FIELD_ROUTE_SUB_KEYS= "sub_keys",
                JSON_FIELD_ROUTE_LOG_KEYS= "log_keys";

        private byte routeId= -1;
        private final HashSet<Byte> filterIds= new HashSet<>();
        private final HashSet<String> subscriberKeys= new HashSet<>(), loggerKeys= new HashSet<>(), routeDpKeys = new HashSet<>();
        private final HashSet<Byte> routeEventCmdIds = new HashSet<>();
        private boolean active= true;

        public RouteManagerImpl() { }

        public RouteManagerImpl(JSONObject state) throws JSONException {
            routeId= (byte) state.getInt(JSON_FIELD_ID);

            JSONArray loggerKeysState= state.getJSONArray(JSON_FIELD_ROUTE_LOG_KEYS);
            for(byte i= 0; i < loggerKeysState.length(); i++) {
                loggerKeys.add(loggerKeysState.getString(i));
            }

            JSONArray subKeysState= state.getJSONArray(JSON_FIELD_ROUTE_SUB_KEYS);
            for(byte i= 0; i < subKeysState.length(); i++) {
                subscriberKeys.add(subKeysState.getString(i));
            }

            JSONArray eventCmdIdsState= state.getJSONArray(JSON_FIELD_CMD_IDS);
            for(byte i= 0; i < eventCmdIdsState.length(); i++) {
                routeEventCmdIds.add((byte) eventCmdIdsState.getInt(i));
            }

            JSONArray filterIdsState= state.getJSONArray(JSON_FIELD_FILTER_IDS);
            for(byte i= 0; i < filterIdsState.length(); i++) {
                filterIds.add((byte) filterIdsState.getInt(i));
            }
        }

        public JSONObject serializeState() throws JSONException {
            return new JSONObject().put(JSON_FIELD_ROUTE_LOG_KEYS, new JSONArray(loggerKeys)).put(JSON_FIELD_ROUTE_SUB_KEYS, new JSONArray(subscriberKeys))
                    .put(JSON_FIELD_CMD_IDS, new JSONArray(routeEventCmdIds)).put(JSON_FIELD_ID, routeId).put(JSON_FIELD_FILTER_IDS, new JSONArray(filterIds));
        }

        public void setRouteId(byte id) {
            routeId= id;
            dataRoutes.put(routeId, this);
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public byte id() {
            return routeId;
        }

        @Override
        public void remove() {
            if (!active) {
                return;
            }

            active= false;
            for(String key: subscriberKeys) {
                dataSubscriptions.get(key).unsubscribe();
                dataSubscriptions.remove(key);
            }
            subscriberKeys.clear();

            for(String key: loggerKeys) {
                dataLoggerKeys.get(key).remove();
                dataLoggerKeys.remove(key);
            }
            loggerKeys.clear();

            for(Byte id: filterIds) {
                writeRegister(DataProcessorRegister.REMOVE, id);
                dataProcMsgClasses.remove(new ResponseHeader(DataProcessorRegister.NOTIFY, id));
            }
            filterIds.clear();

            for(Byte id: routeEventCmdIds) {
                writeRegister(EventRegister.REMOVE, id);
            }
            routeEventCmdIds.clear();

            for(String key: routeDpKeys) {
                dataProcessors.remove(key);
            }
            routeDpKeys.clear();

            dataRoutes.remove(routeId);
        }

        @Override
        public void removeProcessor(String subscriptionKey) {
            if (active) {
                if (subscriberKeys.contains(subscriptionKey)) {
                    dataSubscriptions.get(subscriptionKey).unsubscribe();
                } else {
                    throw new RuntimeException("Subscriber key: \"" + subscriptionKey + "\' not found");
                }
            }

        }

        @Override
        public void assignProcessor(String subscriptionKey, DataSignal.MessageProcessor processor) {
            if (active) {
                if (!subscriberKeys.contains(subscriptionKey) && !dataSubscriptions.containsKey(subscriptionKey)) {
                    throw new RuntimeException("Subscriber key: \'" + subscriptionKey + "\' not found");
                }

                if (!dataRoutes.containsKey(routeId)) {
                    throw new RuntimeException("Route already removed");
                }

                dataSubscriptions.get(subscriptionKey).resubscribe(processor);
            }
        }

        @Override
        public void assignLogProcessor(String logKey, DataSignal.MessageProcessor processor) {
            if (active) {
                if (!loggerKeys.contains(logKey) && !dataLoggerKeys.containsKey(logKey)) {
                    throw new RuntimeException("Subscriber key: \'" + logKey + "\' not found");
                }

                if (!dataRoutes.containsKey(routeId)) {
                    throw new RuntimeException("Route already removed");
                }

                dataLoggerKeys.get(logKey).attach(processor);
            }
        }
    }

    private class ProcessorSubscription extends SensorSubscription {
        public ProcessorSubscription(JSONObject state) throws JSONException {
            super(state);
        }

        @Override
        public void unsubscribe() {
            nProcSubscribers--;
            if (nProcSubscribers == 0) {
                writeRegister(DataProcessorRegister.NOTIFY, (byte) 0x0);
            }

            super.unsubscribe();
        }

        @Override
        public void resubscribe(DataSignal.MessageProcessor processor) {
            if (nProcSubscribers == 0) {
                writeRegister(DataProcessorRegister.NOTIFY, (byte) 0x1);
            }
            nProcSubscribers++;
            super.resubscribe(processor);
        }
    }
    private class SensorSubscription implements Subscription {
        private final ResponseHeader header;
        private final byte[] notifyBytes, silenceBytes;
        private final JSONObject state;

        public SensorSubscription(JSONObject state) throws JSONException {
            this.state= state;

            JSONArray headerState= state.getJSONArray(JSON_FIELD_HEADER);
            header= new ResponseHeader((byte) headerState.getInt(0), (byte) headerState.getInt(1), (byte) headerState.getInt(2));

            JSONArray silenceState= state.getJSONArray(JSON_FIELD_SILENCE_BYTES);
            silenceBytes= new byte[silenceState.length()];
            for(byte i= 0; i < silenceState.length(); i++) {
                silenceBytes[i]= (byte) silenceState.getInt(i);
            }

            JSONArray notifyState= state.getJSONArray(JSON_FIELD_NOTIFY_BYTES);
            notifyBytes= new byte[notifyState.length()];
            for(byte i= 0; i < notifyState.length(); i++) {
                notifyBytes[i]= (byte) notifyState.getInt(i);
            }
        }
        @Override
        public void unsubscribe() {
            conn.sendCommand(false, silenceBytes);
            responseProcessors.remove(header);
        }

        @Override
        public void resubscribe(DataSignal.MessageProcessor processor) {
            conn.sendCommand(false, notifyBytes);
            responseProcessors.put(header, processor);
        }

        @Override
        public JSONObject serializeState() {
            return state;
        }
    }

    private abstract class Loggable {
        public static final String JSON_FIELD_LOGGER_IDS = "ids", JSON_FIELD_HANDLER_CLASS= "handler_class", JSON_FIELD_LOG_KEY= "key";

        private final Class<? extends Message> msgClass;
        private DataSignal.MessageProcessor logProcessor;
        private final String key;

        public Loggable(String key, Class<? extends Message> msgClass) {
            this.msgClass= msgClass;
            this.key= key;
        }

        public void processLogMessage(byte logId, Calendar timestamp, byte[] data) {
            try {
                Message logMsg;

                if (msgClass.equals(Bmi160AccelAxisMessage.class)) {
                    logMsg= new Bmi160AccelAxisMessage(timestamp, data, bmi160AccRange.scale());
                } else if (msgClass.equals(Bmi160GyroMessage.class)) {
                    logMsg= new Bmi160GyroMessage(timestamp, data, bmi160GyroRange.scale());
                } else {
                    Constructor<?> cTor= msgClass.getConstructor(Calendar.class, byte[].class);
                    logMsg= (Message) cTor.newInstance(timestamp, data);
                }

                logProcessor.process(logMsg);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException ex) {
                throw new RuntimeException("Cannot instantiate message class", ex);
            }
        }

        public JSONObject serializeState() throws JSONException {
            return new JSONObject().put(JSON_FIELD_LOG_KEY, key)
                    .put(JSON_FIELD_MSG_CLASS, msgClass.getName())
                    .put(JSON_FIELD_HANDLER_CLASS, this.getClass().getName());

        }

        public void attach(DataSignal.MessageProcessor processor) {
            logProcessor= processor;
        }

        public String getKey() {
            return key;
        }

        public abstract void remove();
    }
    private class StandardLoggable extends Loggable {
        private final byte logId;

        public StandardLoggable(String key, byte logId, Class<? extends Message> msgClass) {
            super(key, msgClass);
            this.logId= logId;

            dataLoggers.put(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, logId), this);
        }

        public StandardLoggable(JSONObject state) throws JSONException, ClassNotFoundException {
            this(state.getString(JSON_FIELD_LOG_KEY), (byte) state.getJSONArray(JSON_FIELD_LOGGER_IDS).getInt(0),
                    (Class<Message>) Class.forName(state.getString(JSON_FIELD_MSG_CLASS)));
        }

        @Override
        public JSONObject serializeState() throws JSONException {
            return super.serializeState().put(JSON_FIELD_LOGGER_IDS, new JSONArray().put(logId));
        }

        @Override
        public void remove() {
            dataLoggers.remove(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, logId));
        }
    }
    private class AccelerometerLoggable extends Loggable {
        private final byte xyId, zId;
        private Queue<byte[]> xyLogEntries = new LinkedList<>(), zLogEntries = new LinkedList<>();

        public AccelerometerLoggable(String key, byte xyId, byte zId, Class<? extends Message> msgClass) {
            super(key, msgClass);
            this.xyId= xyId;
            this.zId= zId;

            dataLoggers.put(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, xyId), this);
            dataLoggers.put(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, zId), this);
        }

        public AccelerometerLoggable(JSONObject state) throws JSONException, ClassNotFoundException {
            this(state.getString(JSON_FIELD_LOG_KEY), (byte) state.getJSONArray(JSON_FIELD_LOGGER_IDS).getInt(0),
                    (byte) state.getJSONArray(JSON_FIELD_LOGGER_IDS).getInt(1),
                    (Class<Message>) Class.forName(state.getString(JSON_FIELD_MSG_CLASS)));
        }

        @Override
        public void processLogMessage(byte logId, Calendar timestamp, byte[] data) {
            if (logId == xyId) {
                xyLogEntries.add(data);
            } else if (logId == zId) {
                zLogEntries.add(data);
            } else {
                throw new RuntimeException("Unknown log id in this fn: " + logId);
            }

            if (!xyLogEntries.isEmpty() && !zLogEntries.isEmpty()) {
                byte[] xyEntry = xyLogEntries.poll(), zEntry = zLogEntries.poll();
                byte[] merged = new byte[xyEntry.length + zEntry.length - 2];
                System.arraycopy(xyEntry, 0, merged, 0, xyEntry.length);
                System.arraycopy(zEntry, 0, merged, xyEntry.length, 2);

                super.processLogMessage(logId, timestamp, merged);
            }
        }

        @Override
        public JSONObject serializeState() throws JSONException {
            return super.serializeState().put(JSON_FIELD_LOGGER_IDS, new JSONArray().put(xyId).put(zId));
        }

        @Override
        public void remove() {
            dataLoggers.remove(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, xyId));
            dataLoggers.remove(new ResponseHeader(LoggingRegister.READOUT_NOTIFY, zId));
        }
    }

    private class RouteBuilderImpl implements RouteBuilder, EventListener, RouteHandler {
        private AsyncResultImpl<RouteManager> commitResult;
        private DataSource routeSource;
        private byte nExpectedCmds= 0;

        private final RouteManagerImpl manager= new RouteManagerImpl();
        private final Stack<DataSignalImpl> branches= new Stack<>();
        private final Queue<IdCreator> creators= new LinkedList<>();
        private final HashMap<DataSignalImpl, DataSignal.ActivityMonitor> signalMonitors= new HashMap<>();

        private abstract class DataSignalImpl implements DataSignal, MessageToken, Subscription {
            protected DataSignalImpl parent;
            protected Class<? extends Message> msgClass;
            protected final byte outputSize;

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
                    public DataSignal log(String key) {
                        throw new UnsupportedOperationException("Route has ended, can only commit");
                    }

                    @Override
                    public DataSignal subscribe(String key) {
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
                manager.routeDpKeys.add(key);
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
                manager.routeDpKeys.add(key);
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
                        manager.filterIds.add(id);
                        newProcessor.setId(id);
                    }
                });
            }

            @Override
            public DataSignal log(String key) {
                return log(key, this);
            }

            protected DataSignal log(final String key, final DataSignalImpl source) {
                creators.add(new IdCreator() {
                    @Override
                    public void execute() {
                        writeRegister(LoggingRegister.TRIGGER, source.getTriggerConfig());
                    }

                    @Override
                    public void receivedId(byte id) {
                        Loggable newLogger= new StandardLoggable(key, id, msgClass);

                        if (dataLoggerKeys.containsKey(key)) {
                            throw new RuntimeException("Duplicate logging keys found: " + key);
                        } else {
                            dataLoggerKeys.put(key, newLogger);
                            manager.loggerKeys.add(key);
                        }
                    }
                });
                return this;
            }


            @Override
            public DataSignal subscribe(String key) {
                dataSubscriptions.put(key, this);
                manager.subscriberKeys.add(key);
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
            public byte length() {
                return outputSize;
            }

            @Override
            public byte offset() {
                return 0;
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
                            if (!(editor instanceof Passthrough.StateEditor)) {
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
                            if (!(editor instanceof Delta.StateEditor)) {
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
                            if (!(editor instanceof Accumulator.StateEditor)) {
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
                            if (!(editor instanceof Average.StateEditor)) {
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

            public abstract boolean isSigned();
            public abstract void enableNotifications();
            public abstract byte[] getEventConfig();

        }

        public abstract class DataSource extends DataSignalImpl {
            private final ResponseHeader header;

            protected DataSource(byte outputSize, Class<? extends Message> msgClass, ResponseHeader header) {
                super(outputSize, msgClass);
                this.header= header;
            }

            @Override
            public void unsubscribe() {
                responseProcessors.remove(header);
            }

            @Override
            public void resubscribe(MessageProcessor processor) {
                enableNotifications();
                responseProcessors.put(header, processor);
            }

            @Override
            public JSONObject serializeState() {
                try {
                    JSONObject subscriptionState= new JSONObject();

                    JSONArray notifyBytes= new JSONArray().put(header.module).put(header.register);
                    if (header.id != (byte) 0xff) {
                        notifyBytes.put(header.id);
                    }
                    notifyBytes.put(0x1);

                    JSONArray silenceBytes= new JSONArray().put(header.module).put(header.register);
                    if (header.id != (byte) 0xff) {
                        silenceBytes.put(header.id);
                    }
                    silenceBytes.put(0x0);

                    subscriptionState.put(JSON_FIELD_NOTIFY_BYTES, notifyBytes);
                    subscriptionState.put(JSON_FIELD_SILENCE_BYTES, silenceBytes);
                    subscriptionState.put(JSON_FIELD_HEADER, new JSONArray().put(header.module).put(header.register).put(header.id));
                    return subscriptionState;
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private abstract class ProcessedDataSignal extends DataSignalImpl implements DataProcessor {
            private ResponseHeader header= null;

            protected ProcessedDataSignal(byte outputSize, Class<? extends Message> msgClass) {
                super(outputSize, msgClass);
            }

            @Override
            public void enableNotifications() {
                if (nProcSubscribers == 0) {
                    writeRegister(DataProcessorRegister.NOTIFY, (byte) 0x1);
                }
                nProcSubscribers++;
                writeRegister(DataProcessorRegister.NOTIFY_ENABLE, header.id, (byte) 0x1);

                dataProcMsgClasses.put(header, msgClass);
            }

            @Override
            public void unsubscribe() {
                writeRegister(DataProcessorRegister.NOTIFY_ENABLE, header.id, (byte) 0);
                nProcSubscribers--;
                if (nProcSubscribers == 0) {
                    writeRegister(DataProcessorRegister.NOTIFY, (byte) 0x0);
                }
                responseProcessors.remove(header);
            }

            @Override
            public void resubscribe(MessageProcessor processor) {
                enableNotifications();
                responseProcessors.put(header, processor);
            }

            @Override
            public byte[] getEventConfig() {
                if (header == null) {
                    throw new RuntimeException("ID has not been set yet");
                }
                return new byte[] {DataProcessorRegister.NOTIFY.moduleOpcode(), DataProcessorRegister.NOTIFY.opcode(), header.id};
            }

            @Override
            public void setState(StateEditor editor) {
                byte[] stateBytes;

                if (editor instanceof Accumulator.StateEditor) {
                    Number firmware= numberToFirmwareUnits(((Accumulator.StateEditor) editor).newRunningSum);

                    stateBytes= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(firmware.intValue()).array();
                } else if (editor instanceof Average.StateEditor) {
                    stateBytes= new byte[0];
                } else if (editor instanceof Delta.StateEditor) {
                    Number firmware= numberToFirmwareUnits(((Delta.StateEditor) editor).newPreviousValue);

                    stateBytes= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(firmware.intValue()).array();
                } else if (editor instanceof Passthrough.StateEditor) {
                    stateBytes= ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                            .putShort(((Passthrough.StateEditor) editor).newValue).array();
                } else {
                    throw new ClassCastException("Unrecognized state editor: \'" + editor.getClass() + "\'");
                }

                byte[] parameters= new byte[stateBytes.length + 1];
                System.arraycopy(stateBytes, 0, parameters, 1, stateBytes.length);
                parameters[0]= header.id;
                writeRegister(DataProcessorRegister.STATE, parameters);
            }

            public void setId(byte newId) {
                header= new ResponseHeader(DataProcessorRegister.NOTIFY, newId);
            }

            public abstract byte[] getFilterConfig();
            protected abstract byte[] processorConfigToBytes(ProcessorConfig newConfig);

            @Override
            public void modifyConfiguration(ProcessorConfig newConfig) {
                byte[] configBytes= processorConfigToBytes(newConfig);

                byte[] modifyFilter = new byte[configBytes.length + 1];
                modifyFilter[0]= header.id;
                System.arraycopy(configBytes, 0, modifyFilter, 1, configBytes.length);

                writeRegister(DataProcessorRegister.PARAMETER, modifyFilter);
            }

            @Override
            public JSONObject serializeState() {
                if (header == null) {
                    throw new RuntimeException("ID has not been set yet");
                }

                try {
                    JSONObject subscriptionState= new JSONObject();

                    subscriptionState.put(JSON_FIELD_NOTIFY_BYTES,
                            new JSONArray().put(DataProcessorRegister.NOTIFY_ENABLE.moduleOpcode())
                                .put(DataProcessorRegister.NOTIFY_ENABLE.opcode()).put(header.id).put(0x1));
                    subscriptionState.put(JSON_FIELD_SILENCE_BYTES,
                            new JSONArray().put(DataProcessorRegister.NOTIFY_ENABLE.moduleOpcode())
                                .put(DataProcessorRegister.NOTIFY_ENABLE.opcode()).put(header.id).put(0x0));

                    subscriptionState.put(JSON_FIELD_MSG_CLASS, msgClass.getName());
                    subscriptionState.put(JSON_FIELD_HEADER, new JSONArray().put(header.module).put(header.register).put(header.id));
                    return subscriptionState;
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        private abstract class StaticProcessedDataSignal extends ProcessedDataSignal {
            public StaticProcessedDataSignal(byte outputSize, Class<? extends Message> msgClass) {
                super(outputSize, msgClass);
            }

            @Override
            public DataSignal process(ProcessorConfig config) {
                return parent.process(config, this);
            }

            @Override
            public DataSignal log(String key) {
                parent.log(key, this);
                return this;
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
            routeSource= new DataSource((byte) 1, SwitchMessage.class, new ResponseHeader(SwitchRegister.STATE)) {
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
            routeSource= new DataSource((byte) 2, TemperatureMessage.class, new ResponseHeader(TemperatureRegister.VALUE)) {
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

        private abstract class AccelerometerDataSource extends DataSource {
            private final byte XY_LENGTH = 4, Z_OFFSET = XY_LENGTH;
            private byte xyId = -1, zId = -1;

            private AccelerometerDataSource(byte outputSize, Class<? extends Message> msgClass, ResponseHeader header) {
                super(outputSize, msgClass, header);
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
                    ProcessedDataSignal newProcessor = new ProcessedDataSignal((byte) 2, Mma8452qCombinedAxisMessage.class) {
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
            protected DataSignal log(final String key, final DataSignalImpl source) {
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
                        xyId = id;

                        if (xyId != -1 && zId != -1) {
                            Loggable newLogger= new AccelerometerLoggable(key, xyId, zId, msgClass);
                            if (dataLoggerKeys.containsKey(key)) {
                                throw new RuntimeException("Duplicate logging keys found: " + key);
                            } else {
                                dataLoggerKeys.put(key, newLogger);
                                manager.loggerKeys.add(key);
                            }
                        }
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
                        zId = id;

                        if (xyId != -1 && zId != -1) {
                            Loggable newLogger= new AccelerometerLoggable(key, xyId, zId, msgClass);
                            if (dataLoggerKeys.containsKey(key)) {
                                throw new RuntimeException("Duplicate logging keys found: " + key);
                            } else {
                                dataLoggerKeys.put(key, newLogger);
                                manager.loggerKeys.add(key);
                            }
                        }
                    }
                });
                return this;
            }
        }
        @Override
        public DataSignal fromAccelAxis() {
            if (modelNumber.equals(Constant.METAWEAR_RG_MODULE)) {
                routeSource = new AccelerometerDataSource((byte) 6, Bmi160AccelAxisMessage.class, new ResponseHeader(Bmi160AccelerometerRegister.DATA_INTERRUPT)) {
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
                routeSource = new AccelerometerDataSource((byte) 6, Mma8452qAxisMessage.class, new ResponseHeader(MwrAccelerometerRegister.DATA_VALUE)) {
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
        public DataSignal fromTap() {
            if (modelNumber.equals(Constant.METAWEAR_R_MODULE)) {
                return new DataSource((byte) 1, Mma8452qTapMessage.class, new ResponseHeader(MwrAccelerometerRegister.PULSE_STATUS)) {
                    @Override
                    public boolean isSigned() { return false; }

                    @Override
                    public void enableNotifications() {
                        writeRegister(MwrAccelerometerRegister.PULSE_STATUS, (byte) 1);
                    }

                    @Override
                    public byte[] getEventConfig() {
                        return new byte[] {MwrAccelerometerRegister.PULSE_STATUS.moduleOpcode(), MwrAccelerometerRegister.PULSE_STATUS.opcode(),
                                (byte) 0xff};
                    }

                    @Override
                    public void unsubscribe() {
                        writeRegister(MwrAccelerometerRegister.PULSE_STATUS, (byte) 0);
                        super.unsubscribe();
                    }
                };
            }

            throw new UnsupportedOperationException("Tap detection not yet implemented for R+Gyro");
        }

        @Override
        public DataSignal fromMovement() {
            if (modelNumber.equals(Constant.METAWEAR_R_MODULE)) {
                return new DataSource((byte) 1, Mma8452qMovementMessage.class, new ResponseHeader(MwrAccelerometerRegister.MOVEMENT_VALUE)) {
                    @Override
                    public boolean isSigned() { return false; }

                    @Override
                    public void enableNotifications() {
                        writeRegister(MwrAccelerometerRegister.MOVEMENT_VALUE, (byte) 1);
                    }

                    @Override
                    public byte[] getEventConfig() {
                        return new byte[] {MwrAccelerometerRegister.MOVEMENT_VALUE.moduleOpcode(), MwrAccelerometerRegister.MOVEMENT_VALUE.opcode(),
                                (byte) 0xff};
                    }

                    @Override
                    public void unsubscribe() {
                        writeRegister(MwrAccelerometerRegister.MOVEMENT_VALUE, (byte) 0);
                        super.unsubscribe();
                    }
                };
            }

            throw new UnsupportedOperationException("Tap detection not yet implemented for R+Gyro");
        }

        @Override
        public DataSignal fromOrientation() {
            if (modelNumber.equals(Constant.METAWEAR_R_MODULE)) {
                return new DataSource((byte) 1, Mma8452qOrientationMessage.class, new ResponseHeader(MwrAccelerometerRegister.ORIENTATION_VALUE)) {
                    @Override
                    public boolean isSigned() { return false; }

                    @Override
                    public void enableNotifications() {
                        writeRegister(MwrAccelerometerRegister.ORIENTATION_VALUE, (byte) 1);
                    }

                    @Override
                    public byte[] getEventConfig() {
                        return new byte[] {MwrAccelerometerRegister.ORIENTATION_VALUE.moduleOpcode(), MwrAccelerometerRegister.ORIENTATION_VALUE.opcode(),
                                (byte) 0xff};
                    }

                    @Override
                    public void unsubscribe() {
                        writeRegister(MwrAccelerometerRegister.ORIENTATION_VALUE, (byte) 0);
                        super.unsubscribe();
                    }
                };
            }

            throw new UnsupportedOperationException("Tap detection not yet implemented for R+Gyro");
        }

        @Override
        public DataSignal fromShake() {
            if (modelNumber.equals(Constant.METAWEAR_R_MODULE)) {
                return new DataSource((byte) 1, Mma8452qMovementMessage.class, new ResponseHeader(MwrAccelerometerRegister.SHAKE_STATUS)) {
                    @Override
                    public boolean isSigned() { return false; }

                    @Override
                    public void enableNotifications() {
                        writeRegister(MwrAccelerometerRegister.SHAKE_STATUS, (byte) 1);
                    }

                    @Override
                    public byte[] getEventConfig() {
                        return new byte[] {MwrAccelerometerRegister.SHAKE_STATUS.moduleOpcode(), MwrAccelerometerRegister.SHAKE_STATUS.opcode(),
                                (byte) 0xff};
                    }

                    @Override
                    public void unsubscribe() {
                        writeRegister(MwrAccelerometerRegister.SHAKE_STATUS, (byte) 0);
                        super.unsubscribe();
                    }
                };
            }

            throw new UnsupportedOperationException("Tap detection not yet implemented for R+Gyro");
        }

        @Override
        public DataSignal fromGyro() {
            if (modelNumber.equals(Constant.METAWEAR_RG_MODULE)) {
                routeSource= new AccelerometerDataSource((byte) 6, Bmi160GyroMessage.class, new ResponseHeader(Bmi160GyroRegister.DATA)) {
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

            routeSource= new DataSource((byte) 2, msgClass, new ResponseHeader(analogRegister, pin)) {
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
            routeSource= new DataSource((byte) 1, GpioDigitalMessage.class, new ResponseHeader(GpioRegister.READ_DI, pin)) {

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
            routeSource= new DataSource((byte) 1, GpioDigitalMessage.class, new ResponseHeader(GpioRegister.PIN_CHANGE_NOTIFY, pin)) {
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
            routeSource= new DataSource((byte) 4, GsrMessage.class, new ResponseHeader(GsrRegister.CONDUCTANCE, channel)) {
                @Override
                public boolean isSigned() {
                    return false;
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
            if (modelNumber.equals(Constant.METAWEAR_RG_MODULE)) {
                routeSource= new DataSource((byte) 2, TemperatureMessage.class, new ResponseHeader(MultiChannelTempRegister.TEMPERATURE, src.channel())) {

                    @Override
                    public Number numberToFirmwareUnits(Number input) {
                        return input.floatValue() * 8.0f;
                    }

                    @Override
                    public boolean isSigned() {
                        return false;
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
                    byte routeId= (byte) routeIdGenerator.getAndIncrement();
                    manager.setRouteId(routeId);
                    commitResult.setResult(manager, null);

                    pendingRoutes.poll();
                    if (!pendingRoutes.isEmpty()) {
                        pendingRoutes.peek().addId();
                    } else {
                        commitRoutes.set(false);
                        writeMacroCommands();
                    }
                }
            } else {
                commitResult.setResult(null, new RuntimeException("Connection to MetaWear board lost"));
            }
        }
        public void receivedCommandId(byte id) {
            manager.routeEventCmdIds.add(id);
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

    private interface ResponseProcessor {
        public Response process(byte[] response);
    }
    private final HashMap<ResponseHeader, ResponseProcessor> responses;

    private String modelNumber = null;
    private Logging.DownloadHandler downloadHandler;
    private float notifyProgress= 1.0f;
    private int nLogEntries;
    private ReferenceTick logReferenceTick= null;
    private final AtomicBoolean commitRoutes= new AtomicBoolean(false);
    private final Queue<RouteHandler> pendingRoutes= new LinkedList<>();
    private final HashMap<ResponseHeader, DataSignal.MessageProcessor> responseProcessors= new HashMap<>();
    private final HashMap<ResponseHeader, Loggable> dataLoggers= new HashMap<>();

    ///< Route variables
    private final HashMap<Byte, RouteManagerImpl> dataRoutes= new HashMap<>();
    private final AtomicInteger routeIdGenerator= new AtomicInteger(0);
    private byte nProcSubscribers= 0;
    private final HashMap<ResponseHeader, Class<? extends Message>> dataProcMsgClasses= new HashMap<>();
    private final HashMap<String, DataProcessor> dataProcessors= new HashMap<>();
    private final HashMap<String, Subscription> dataSubscriptions= new HashMap<>();
    private final HashMap<String, Loggable> dataLoggerKeys= new HashMap<>();

    ///< Timer variables
    private final HashMap<Byte, TimerControllerImpl> timerControllers= new HashMap<>();

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
                processLogData(Arrays.copyOfRange(response, 2, 11));

                if (response.length == 20) {
                    processLogData(Arrays.copyOfRange(response, 11, 20));
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
                    } else if (dataProcMsgClasses.get(header).equals(Bmi160GyroMessage.class)) {
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
                TimerControllerImpl timeCtrllr= new TimerControllerImpl(response[2], timeTasks.poll());
                timerControllers.put(response[2], timeCtrllr);

                pendingRoutes.add(timeCtrllr);
                if (!commitRoutes.get()) {
                    commitRoutes.set(true);
                    pendingRoutes.peek().addId();
                }

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
                macroIds.add(response[2]);
                return null;
            }
        });
        responses.put(new ResponseHeader(MacroRegister.NOTIFY), new ResponseProcessor() {
            @Override
            public Response process(byte[] response) {
                macroExecResults.poll().setResult(null, null);
                return null;
            }
        });
    }

    private void processLogData(byte[] logData) {
        byte logId= (byte) (logData[0] & 0xf);
        ResponseHeader header= new ResponseHeader(LoggingRegister.READOUT_NOTIFY, logId);

        long tick= ByteBuffer.wrap(logData, 1, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL;
        Calendar timestamp= (Calendar) logReferenceTick.timestamp().clone();
        timestamp.add(Calendar.MILLISECOND, (int) ((tick - logReferenceTick.tickCount()) * TICK_TIME_STEP));
        byte[] logEntryData= Arrays.copyOfRange(logData, 5, logData.length);

        if (dataLoggers.containsKey(header)) {
            dataLoggers.get(header).processLogMessage(logId, timestamp, logEntryData);
        } else {
            downloadHandler.receivedUnknownLogEntry(logId, timestamp, logEntryData);
        }
    }

    public void setModelNumber(final String modelNumber) {
        this.modelNumber = modelNumber;

        if (modelNumber.equals(Constant.METAWEAR_R_MODULE)) {
            final ResponseProcessor movementProcessor= new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    byte[] respBody= new byte[response.length - 2];
                    System.arraycopy(response, 2, respBody, 0, respBody.length);

                    return new Response(new Mma8452qMovementMessage(respBody), new ResponseHeader(response[0], response[1]));
                }
            };

            responses.put(new ResponseHeader(MwrAccelerometerRegister.DATA_VALUE), new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    byte[] respBody= new byte[response.length - 2];
                    System.arraycopy(response, 2, respBody, 0, respBody.length);

                    return new Response(new Mma8452qAxisMessage(respBody), new ResponseHeader(response[0], response[1]));
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
            responses.put(new ResponseHeader(MwrAccelerometerRegister.PULSE_STATUS), new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    byte[] respBody= new byte[response.length - 2];
                    System.arraycopy(response, 2, respBody, 0, respBody.length);

                    return new Response(new Mma8452qTapMessage(respBody), new ResponseHeader(response[0], response[1]));
                }
            });
            responses.put(new ResponseHeader(MwrAccelerometerRegister.ORIENTATION_VALUE), new ResponseProcessor() {
                @Override
                public Response process(byte[] response) {
                    byte[] respBody= new byte[response.length - 2];
                    System.arraycopy(response, 2, respBody, 0, respBody.length);

                    return new Response(new Mma8452qOrientationMessage(respBody), new ResponseHeader(response[0], response[1]));
                }
            });
            responses.put(new ResponseHeader(MwrAccelerometerRegister.SHAKE_STATUS), movementProcessor);
            responses.put(new ResponseHeader(MwrAccelerometerRegister.MOVEMENT_VALUE), movementProcessor);
        } else if (modelNumber.equals(Constant.METAWEAR_RG_MODULE)) {
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
    public byte[] serializeState() {
        JSONObject boardState= new JSONObject();

        try {
            JSONArray logStates= new JSONArray();
            HashSet<String> seenLogKeys= new HashSet<>();
            for(Loggable it: dataLoggers.values()) {
                if (!seenLogKeys.contains(it.getKey())) {
                    seenLogKeys.add(it.getKey());
                    logStates.put(it.serializeState());
                }
            }

            JSONArray subscriberStates= new JSONArray();
            for(Map.Entry<String, Subscription> it: dataSubscriptions.entrySet()) {
                JSONObject partialState= it.getValue().serializeState();
                partialState.put(JSON_FIELD_NAME, it.getKey());
                subscriberStates.put(partialState);
            }

            JSONObject routeState= new JSONObject().put(JSON_FIELD_LOG, logStates)
                    .put(JSON_FIELD_SUBSCRIBERS, subscriberStates);

            JSONArray routeManagerState= new JSONArray();
            for(RouteManagerImpl it: dataRoutes.values()) {
                routeManagerState.put(it.serializeState());
            }

            JSONArray timerStates= new JSONArray();
            for(TimerControllerImpl it: timerControllers.values()) {
                timerStates.put(it.serialize());
            }

            boardState.put(JSON_FIELD_ROUTE, routeState).put(JSON_FIELD_TIMER, timerStates).put(JSON_FIELD_MMA8452Q_ODR, (mma8452qDataSampling[2] >> 3) & 0x3)
                    .put(JSON_FIELD_BMI160_ACC_RANGE, bmi160AccRange.ordinal()).put(JSON_FIELD_BMI160_GRYO_RANGE, bmi160GyroRange.ordinal())
                    .put(JSON_FIELD_ROUTE_MANAGER, routeManagerState);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return boardState.toString().getBytes();
    }

    @Override
    public void deserializeState(byte[] state) {
        try {
            JSONObject jsonState= new JSONObject(new String(state));

            {
                JSONObject routeState = jsonState.getJSONObject(JSON_FIELD_ROUTE);
                JSONArray routeLoggerStates = routeState.getJSONArray(JSON_FIELD_LOG);

                for (byte j = 0; j < routeLoggerStates.length(); j++) {
                    JSONObject currentLogger = routeLoggerStates.getJSONObject(j);

                    Constructor<?> cTor = Class.forName(currentLogger.getString(Loggable.JSON_FIELD_HANDLER_CLASS)).getConstructor(DefaultMetaWearBoard.class, JSONObject.class);
                    Loggable newLogger = (Loggable) cTor.newInstance(this, currentLogger);

                    dataLoggerKeys.put(newLogger.getKey(), newLogger);
                }

                nProcSubscribers = 0;
                JSONArray subscriberStates = routeState.getJSONArray(JSON_FIELD_SUBSCRIBERS);
                for (byte j = 0; j < subscriberStates.length(); j++) {
                    final JSONObject currentSubscriber = subscriberStates.getJSONObject(j);


                    if (currentSubscriber.opt(JSON_FIELD_MSG_CLASS) != null) {
                        JSONArray headerState = currentSubscriber.getJSONArray(Subscription.JSON_FIELD_HEADER);
                        ResponseHeader header = new ResponseHeader((byte) headerState.getInt(0), (byte) headerState.getInt(1), (byte) headerState.getInt(2));

                        dataProcMsgClasses.put(header, (Class<Message>) Class.forName(currentSubscriber.getString(JSON_FIELD_MSG_CLASS)));
                        nProcSubscribers++;
                        dataSubscriptions.put(currentSubscriber.getString(JSON_FIELD_NAME), new ProcessorSubscription(currentSubscriber));
                    } else {
                        dataSubscriptions.put(currentSubscriber.getString(JSON_FIELD_NAME), new SensorSubscription(currentSubscriber));
                    }
                }
            }

            {
                byte highestId= -1;
                JSONArray routeManagerStates= jsonState.getJSONArray(JSON_FIELD_ROUTE_MANAGER);

                for (byte j= 0; j < routeManagerStates.length(); j++) {
                    RouteManagerImpl currRouteManager= new RouteManagerImpl(routeManagerStates.getJSONObject(j));
                    if (currRouteManager.id() > highestId) {
                        highestId= currRouteManager.id();
                    }
                    dataRoutes.put(currRouteManager.id(), currRouteManager);
                }
                routeIdGenerator.set(highestId + 1);
            }

            {
                int mma8452qOdr= jsonState.getInt(JSON_FIELD_MMA8452Q_ODR);
                mma8452qDataSampling[2] &= 0xc7;
                mma8452qDataSampling[2] |= (mma8452qOdr) << 3;
            }

            {
                bmi160AccRange= Bmi160Accelerometer.AccRange.values()[jsonState.getInt(JSON_FIELD_BMI160_ACC_RANGE)];
                bmi160DataSampling[1]= bmi160AccRange.bitMask();
                bmi160GyroRange= Bmi160Gyro.FullScaleRange.values()[jsonState.getInt(JSON_FIELD_BMI160_GRYO_RANGE)];
                bmi160GyroConfig[1]= bmi160GyroRange.bitMask();
            }

            {
                JSONArray timerStates = jsonState.optJSONArray(JSON_FIELD_TIMER);
                for (byte j = 0; j < timerStates.length(); j++) {
                    TimerControllerImpl currentTimer= new TimerControllerImpl(timerStates.getJSONObject(j));
                    timerControllers.put(currentTimer.id(), currentTimer);
                }
            }
        } catch (JSONException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeRoutes() {
        for(Loggable it: dataLoggerKeys.values()) {
            it.remove();
        }
        dataLoggerKeys.clear();
        writeRegister(LoggingRegister.REMOVE_ALL);

        for(RouteManagerImpl manager: dataRoutes.values()) {
            for(Byte it: manager.routeEventCmdIds) {
                writeRegister(EventRegister.REMOVE, it);
            }
        }

        for(Subscription it: dataSubscriptions.values()) {
            it.unsubscribe();
        }
        dataSubscriptions.clear();

        writeRegister(DataProcessorRegister.REMOVE_ALL);

        dataProcessors.clear();
        dataRoutes.clear();
    }

    @Override
    public RouteManager getRouteManager(byte id) {
        return dataRoutes.get(id);
    }

    @Override
    public RouteBuilder routeData() {
        return new RouteBuilderImpl();
    }

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
    private final ConcurrentLinkedQueue<Timer.Task> timeTasks= new ConcurrentLinkedQueue<>();
    private class TimerImpl implements Timer {

        @Override
        public AsyncResult<Controller> scheduleTask(Task mwTask, int period, boolean delay) {
            return scheduleTask(mwTask, period, delay, (short) -1);
        }

        @Override
        public AsyncResult<Controller> scheduleTask(Task mwTask, int period, boolean delay, short repetitions) {
            AsyncResultImpl<Controller> timerResult= new AsyncResultImpl<>();
            if (isConnected()) {
                timeTasks.add(mwTask);
                timerControllerResults.add(timerResult);

                ByteBuffer buffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);
                buffer.putInt(period).putShort(repetitions).put((byte) (delay ? 0 : 1));
                writeRegister(TimerRegister.TIMER_ENTRY, buffer.array());
            } else {
                timerResult.setResult(null, new RuntimeException("Not connected to a MetaWear board"));
            }

            return timerResult;
        }

        @Override
        public void removeTimers() {
            for(TimerControllerImpl it: timerControllers.values()) {
                it.remove(false);
            }
            timerControllers.clear();
        }

        @Override
        public Controller getController(byte controllerId) {
            return timerControllers.get(controllerId);
        }
    }
    private class TimerControllerImpl implements EventListener, Timer.Controller, RouteHandler {
        private final static String JSON_FIELD_ID= "id";

        private final byte timerId;
        private final byte[] timerEventConfig;
        private byte nExpectedCommands= 0;
        private final HashSet<Byte> eventCmdIds;
        private boolean active= true;

        private Timer.Task task;

        public TimerControllerImpl(byte timerId, Timer.Task task) {
            this.timerId= timerId;
            this.task= task;

            timerEventConfig= new byte[] {TimerRegister.NOTIFY.moduleOpcode(), TimerRegister.NOTIFY.opcode(), timerId};
            eventCmdIds= new HashSet<>();
        }

        public TimerControllerImpl(JSONObject state) throws JSONException {
            timerId= (byte) state.getInt(JSON_FIELD_ID);

            JSONArray eventIds= state.getJSONArray(JSON_FIELD_CMD_IDS);
            eventCmdIds= new HashSet<>();
            for(int j= 0; j < eventIds.length(); j++) {
                eventCmdIds.add((byte) eventIds.getInt(j));
            }

            timerEventConfig= new byte[] {TimerRegister.NOTIFY.moduleOpcode(), TimerRegister.NOTIFY.opcode(), timerId};
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public byte id() {
            return timerId;
        }

        @Override
        public void start() {
            if (active) {
                writeRegister(TimerRegister.START, timerId);
            }
        }

        @Override
        public void stop() {
            if (active) {
                writeRegister(TimerRegister.STOP, timerId);
            }
        }

        @Override
        public void remove() {
            remove(true);
        }

        public void remove(boolean removeFromMap) {
            if (active) {
                active = false;

                writeRegister(TimerRegister.REMOVE, timerId);
                for (Byte id : eventCmdIds) {
                    writeRegister(EventRegister.REMOVE, id);
                }
                eventCmdIds.clear();
                if (removeFromMap) {
                    timerControllers.remove(timerId);
                }
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
                    writeMacroCommands();
                }

                timerControllerResults.poll().setResult(this, null);
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
            task.commands();
            eventConfig= null;
        }

        @Override
        public void receivedId(byte id) { }

        public JSONObject serialize() {
            JSONObject state= new JSONObject();

            try {
                state.put(JSON_FIELD_ID, timerId);
                state.put(JSON_FIELD_CMD_IDS, new JSONArray(eventCmdIds));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            return state;
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
        public void startLogging() {
            startLogging(false);
        }

        @Override
        public void startLogging(boolean overwrite) {
            writeRegister(LoggingRegister.CIRCULAR_BUFFER, (byte) (overwrite ? 1 : 0));
            writeRegister(LoggingRegister.ENABLE, (byte) 1);
        }

        @Override
        public void stopLogging() {
            writeRegister(LoggingRegister.ENABLE, (byte) 0);
        }

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
        public void clearEntries() {
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
            ByteBuffer buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).put((byte) 127).putShort(pulseWidth).put((byte) 1);
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

    private final Queue<byte[]> commands= new LinkedList<>();
    private final ConcurrentLinkedQueue<AsyncResultImpl<Byte>> macroIdResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<AsyncResultImpl<Void>> macroExecResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Byte> macroIds = new ConcurrentLinkedQueue<>();
    private Macro.CodeBlock currentBlock;
    private class MacroImpl implements Macro {
        @Override
        public AsyncResult<Byte> record(CodeBlock block) {
            AsyncResultImpl<Byte> idResult= new AsyncResultImpl<>();

            currentBlock= block;
            macroIdResults.add(idResult);
            commands.clear();
            saveCommand= true;
            block.commands();

            if (!commitRoutes.get()) {
                writeMacroCommands();
            }

            return idResult;
        }

        @Override
        public AsyncResult<Void> execute(byte macroId) {
            AsyncResultImpl<Void> result= new AsyncResultImpl<>();

            macroExecResults.add(result);
            writeRegister(MacroRegister.NOTIFY, (byte) 1);
            writeRegister(MacroRegister.NOTIFY_ENABLE, macroId, (byte) 1);
            writeRegister(MacroRegister.EXECUTE, macroId);

            return result;
        }

        @Override
        public void eraseMacros() {
            writeRegister(MacroRegister.ERASE_ALL);
        }
    }
    private void writeMacroCommands() {
        if (currentBlock != null) {
            saveCommand = false;

            writeRegister(MacroRegister.BEGIN, (byte) (currentBlock.execOnBoot() ? 1 : 0));
            for (byte[] cmd : commands) {
                conn.sendCommand(true, cmd);
            }
            writeRegister(MacroRegister.END);

            macroIdResults.poll().setResult(macroIds.poll(), null);
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
            if (modelNumber.equals(Constant.METAWEAR_R_MODULE)) {
                if (mma8452qModule == null) {
                    mma8452qModule = new Mma8452qAccelerometerImpl();
                }
                return moduleClass.cast(mma8452qModule);
            } else if (modelNumber.equals(Constant.METAWEAR_RG_MODULE)) {
                return moduleClass.cast(new Bmi160AccelerometerImpl());
            }
            return null;
        } else if (moduleClass.equals(Mma8452qAccelerometer.class)) {
            if (modelNumber.equals(Constant.METAWEAR_R_MODULE)) {
                if (mma8452qModule == null) {
                    mma8452qModule = new Mma8452qAccelerometerImpl();
                }
                return moduleClass.cast(mma8452qModule);
            } else {
                return null;
            }
        } else if (moduleClass.equals(Timer.class)) {
            return moduleClass.cast(new TimerImpl());
        } else if (moduleClass.equals(Debug.class)) {
            return moduleClass.cast(new DebugImpl());
        } else if (moduleClass.equals(Logging.class)) {
            return moduleClass.cast(new LoggingImpl());
        } else if (moduleClass.equals(Bmi160Gyro.class)) {
            if (modelNumber.equals(Constant.METAWEAR_RG_MODULE)) {
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

    private Mma8452qAccelerometerImpl mma8452qModule= null;
    private final byte[] mma8452qDataSampling= new byte[] {0, 0, 0x18, 0, 0}, mma8452qOrientationCfg= new byte[5];
    private class Mma8452qAccelerometerImpl implements Mma8452qAccelerometer {
        private final float[][] motionCountSteps= new float[][] {
                {1.25f, 2.5f, 5, 10, 20, 20, 20, 20},
                {1.25f, 2.5f, 5, 10, 20, 80, 80, 80},
                {1.25f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f},
                {1.25f, 2.5f, 5, 10, 20, 80, 160, 160}
        }, transientSteps, orientationSteps;
        private final float[][][] pulseTmltSteps= new float[][][] {
            {{0.625f, 0.625f, 1.25f, 2.5f, 5, 5, 5, 5},
                {0.625f, 0.625f, 1.25f, 2.5f, 5, 20, 20, 20},
                {0.625f, 0.625f, 0.625f, 0.625f, 0.625f, 0.625f, 0.625f, 0.625f},
                {0.625f, 1.25f, 2.5f, 5, 10, 40, 40, 40}},
            {{1.25f, 2.5f, 5, 10, 20, 20, 20, 20},
                {1.25f, 2.5f, 5, 10, 20, 80, 80, 80},
                {1.25f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f},
                {1.25f, 2.5f, 5, 10, 20, 80, 160, 160}}
        }, pulseLtcySteps, pulseWindSteps;
        private final float[] sleepCountSteps= new float[] {320, 320, 320, 320, 320, 320, 320, 640};

        private final float MMA8452Q_G_PER_STEP= 0.063f;

        ///< Tap variables
        private float tapThreshold= 2.f;
        private int tapLatency= 200, tapWindow= 300, tapDuration= 60;
        private Axis tapAxis= Axis.Z;
        private TapType[] tapTypes= new TapType[] {TapType.SINGLE};

        ///< Shake variables
        private float shakeThreshold= 0.5f;
        private int shakeDuration= 50;
        private Axis shakeAxis= Axis.X;

        ///< Sampling variables
        private FullScaleRange selectedFsr= FullScaleRange.FSR_2G;

        ///< Movement variables
        private float movementThreshold= 1.5f;
        private int movementDuration= 100;
        private Axis[] movementAxes= Axis.values();
        private MovementType movementType= MovementType.MOTION;

        ///< Orientation variables
        private int orientationDelay= 100;

        ///< Global variables
        private int activeTimeout= 0;

        public Mma8452qAccelerometerImpl() {
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

        @Override
        public void setAxisSamplingRange(float range) {
            final float[] values= new float[] { 2.f, 4.f, 8.f };
            int closest= closestIndex(values, range);

            selectedFsr= FullScaleRange.values()[closest];
        }

        @Override
        public void setOutputDataRate(float frequency) {
            final float[] values = new float[] {1.56f, 6.25f, 12.5f, 50.f, 100.f, 200.f, 400.f, 800.f};
            int selected = values.length - closestIndex(values, frequency) - 1;

            mma8452qDataSampling[2] &= 0xc7;
            mma8452qDataSampling[2] |= (selected) << 3;
        }

        @Override
        public void setOutputDataRate(OutputDataRate rate) {
            mma8452qDataSampling[2] &= 0xc7;
            mma8452qDataSampling[2] |= rate.ordinal() << 3;
        }

        @Override
        public AutoSleepConfigEditor configureAutoSleep() {
            return new AutoSleepConfigEditor() {
                private SleepModeRate sleepRate;
                private int timeout;
                private PowerMode powerMode;

                @Override
                public AutoSleepConfigEditor setDataRate(SleepModeRate rate) {
                    sleepRate= rate;
                    return this;
                }

                @Override
                public AutoSleepConfigEditor setTimeout(int timeout) {
                    this.timeout= timeout;
                    return this;
                }

                @Override
                public AutoSleepConfigEditor setPowerMode(PowerMode pwrMode) {
                    powerMode= pwrMode;
                    return this;
                }

                @Override
                public void commit() {
                    Mma8452qAccelerometerImpl.this.activeTimeout= timeout;

                    mma8452qDataSampling[2] |= sleepRate.ordinal() << 6;
                    mma8452qDataSampling[3] &= ~(0x3 << 3);
                    mma8452qDataSampling[3] |= (powerMode.ordinal() << 3);
                }
            };
        }
        @Override
        public void enableAutoSleepMode() {
            mma8452qDataSampling[3] |= 0x4;
        }

        @Override
        public void disableAutoSleepMode() {
            mma8452qDataSampling[3] &= 0x4;
        }

        @Override
        public void setPowerMode(PowerMode mode) {
            mma8452qDataSampling[3] &= ~0x3;
            mma8452qDataSampling[3] |= mode.ordinal();
        }

        @Override
        public void startAxisSampling() {
            writeRegister(MwrAccelerometerRegister.DATA_ENABLE, (byte) 1);
        }

        @Override
        public void stopAxisSampling() {
            writeRegister(MwrAccelerometerRegister.DATA_ENABLE, (byte) 0);
        }

        @Override
        public void globalStart() {
            int accelOdr= (mma8452qDataSampling[2] >> 3) & 0x3;
            int pwMode= (mma8452qDataSampling[3] >> 3) & 0x3;

            {
                mma8452qDataSampling[0] &= 0xfc;
                mma8452qDataSampling[0] |= selectedFsr.ordinal();

                if ((mma8452qDataSampling[3] & 0x40) == 0x40) {
                    mma8452qDataSampling[4]= (byte)(activeTimeout / sleepCountSteps[accelOdr]);
                }

                writeRegister(MwrAccelerometerRegister.DATA_CONFIG, mma8452qDataSampling);
            }

            {
                byte[] tapConfig = new byte[8];
                byte nSteps = (byte) (tapThreshold / MMA8452Q_G_PER_STEP);
                int lpfEn= (mma8452qDataSampling[1] & 0x10) >> 4;

                for(TapType it: tapTypes) {
                    switch(it) {
                        case SINGLE:
                            tapConfig[0] |= 1 << (2 * tapAxis.ordinal());
                            break;
                        case DOUBLE:
                            tapConfig[0] |= 1 << (1 + 2 * tapAxis.ordinal());
                            break;
                    }
                }
                tapConfig[0] |= 0x40;

                tapConfig[2] |= nSteps;
                tapConfig[3] |= nSteps;
                tapConfig[4] |= nSteps;
                tapConfig[5]= (byte) (tapDuration / pulseTmltSteps[lpfEn][pwMode][accelOdr]);
                tapConfig[6]= (byte) (tapLatency / pulseLtcySteps[lpfEn][pwMode][accelOdr]);
                tapConfig[7]= (byte) (tapWindow / pulseWindSteps[lpfEn][pwMode][accelOdr]);
                writeRegister(MwrAccelerometerRegister.PULSE_CONFIG, tapConfig);
            }

            {
                byte[] shakeConfig= new byte[4];
                shakeConfig[0] = (byte) ((2 << shakeAxis.ordinal()) | 0x10);
                shakeConfig[2] = (byte) (shakeThreshold / MMA8452Q_G_PER_STEP);
                shakeConfig[3] = (byte)(shakeDuration / transientSteps[pwMode][accelOdr]);
                writeRegister(MwrAccelerometerRegister.SHAKE_CONFIG, shakeConfig);
            }

            {
                byte[] movementConfig= new byte[4];

                movementConfig[0] |= 0x80;
                if (movementType == MovementType.MOTION) {
                    movementConfig[0] |= 0x40;
                }

                byte mask= 0;
                for(Axis it: movementAxes) {
                    mask |= 1 << (it.ordinal() + 3);
                }
                movementConfig[0] |= mask;
                movementConfig[2]= (byte) (movementThreshold / MMA8452Q_G_PER_STEP);
                movementConfig[3]= (byte)(movementDuration / transientSteps[pwMode][accelOdr]);
                writeRegister(MwrAccelerometerRegister.MOVEMENT_CONFIG, movementConfig);
            }

            {
                mma8452qOrientationCfg[2]= (byte) (orientationDelay / orientationSteps[pwMode][accelOdr]);
                writeRegister(MwrAccelerometerRegister.ORIENTATION_CONFIG, mma8452qOrientationCfg);
            }

            writeRegister(MwrAccelerometerRegister.GLOBAL_ENABLE, (byte) 1);
        }

        @Override
        public void globalStop() {
            writeRegister(MwrAccelerometerRegister.GLOBAL_ENABLE, (byte) 0);
        }

        @Override
        public SamplingConfigEditor configureAxisSampling() {
            return new SamplingConfigEditor() {
                private final byte DISABLE_HPF= -1;
                private FullScaleRange selectedFsr= FullScaleRange.FSR_2G;
                private byte hpfCutoff= DISABLE_HPF;

                @Override
                public SamplingConfigEditor setFullScaleRange(FullScaleRange range) {
                    selectedFsr= range;
                    return this;
                }

                @Override
                public SamplingConfigEditor enableHighPassFilter(byte cutoff) {
                    hpfCutoff= cutoff;
                    return this;
                }

                @Override
                public void commit() {
                    if (hpfCutoff != DISABLE_HPF) {
                        mma8452qDataSampling[0] |= 0x10;
                        mma8452qDataSampling[1] |= (hpfCutoff & 0x3);
                    } else {
                        mma8452qDataSampling[0] &= ~0x10;
                    }
                    Mma8452qAccelerometerImpl.this.selectedFsr= this.selectedFsr;
                }
            };
        }

        @Override
        public TapConfigEditor configureTapDetection(final TapType ... type) {
            return new TapConfigEditor() {
                private float tapThreshold= 2.f;
                private int tapLatency= 200, tapWindow= 300, tapDuration= 60;
                private boolean enableLowPassFilter= false;
                private Axis tapAxis= Axis.Z;

                @Override
                public TapConfigEditor setLatency(int latency) {
                    tapLatency= latency;
                    return this;
                }

                @Override
                public TapConfigEditor setWindow(int window) {
                    tapWindow= window;
                    return this;
                }

                @Override
                public TapConfigEditor enableLowPassFilter() {
                    enableLowPassFilter= true;
                    return this;
                }

                @Override
                public TapConfigEditor setAxis(Axis axis) {
                    tapAxis= axis;
                    return this;
                }

                @Override
                public TapConfigEditor setThreshold(float threshold) {
                    tapThreshold= threshold;
                    return this;
                }

                @Override
                public TapConfigEditor setDuration(int duration) {
                    tapDuration= duration;
                    return this;
                }

                @Override
                public void commit() {
                    tapTypes= new TapType[type.length];
                    System.arraycopy(type, 0, tapTypes, 0, type.length);

                    Mma8452qAccelerometerImpl.this.tapAxis= this.tapAxis;
                    Mma8452qAccelerometerImpl.this.tapThreshold= this.tapThreshold;
                    Mma8452qAccelerometerImpl.this.tapLatency= this.tapLatency;
                    Mma8452qAccelerometerImpl.this.tapWindow= this.tapWindow;
                    Mma8452qAccelerometerImpl.this.tapDuration= this.tapDuration;

                    if (enableLowPassFilter) {
                        mma8452qDataSampling[1] |= 0x10;
                    } else {
                        mma8452qDataSampling[1] &= ~0x10;
                    }
                }
            };
        }

        @Override
        public void startTapDetection() {
            writeRegister(MwrAccelerometerRegister.PULSE_ENABLE, (byte) 1);
        }

        @Override
        public void stopTapDetection() {
            writeRegister(MwrAccelerometerRegister.PULSE_ENABLE, (byte) 0);
        }

        @Override
        public ShakeConfigEditor configureShakeDetection() {
            return new ShakeConfigEditor() {
                private float shakeThreshold= 0.5f;
                private int shakeDuration= 50;
                private Axis shakeAxis;

                @Override
                public ShakeConfigEditor setThreshold(float threshold) {
                    shakeThreshold= threshold;
                    return this;
                }

                @Override
                public ShakeConfigEditor setDuration(int duration) {
                    shakeDuration= duration;
                    return this;
                }

                @Override
                public void commit() {
                    Mma8452qAccelerometerImpl.this.shakeAxis= shakeAxis;
                    Mma8452qAccelerometerImpl.this.shakeDuration= this.shakeDuration;
                    Mma8452qAccelerometerImpl.this.shakeThreshold= this.shakeThreshold;
                }

                @Override
                public ShakeConfigEditor setAxis(Axis axis) {
                    this.shakeAxis= axis;
                    return this;
                }
            };
        }

        @Override
        public void startShakeDetection() {
            writeRegister(MwrAccelerometerRegister.SHAKE_ENABLE, (byte) 1);
        }

        @Override
        public void stopShakeDetection() {
            writeRegister(MwrAccelerometerRegister.SHAKE_ENABLE, (byte) 0);
        }

        @Override
        public MovementConfigEditor configureMovementDetection(final MovementType type) {
            return new MovementConfigEditor() {
                private float movementThreshold= (type == MovementType.FREE_FALL ? 0.5f : 1.5f);
                private int movementDuration= 100;
                private Axis[] movementAxes= Axis.values();

                @Override
                public MovementConfigEditor setThreshold(float threshold) {
                    movementThreshold= threshold;
                    return this;
                }

                @Override
                public MovementConfigEditor setDuration(int duration) {
                    movementDuration= duration;
                    return this;
                }

                @Override
                public void commit() {
                    Mma8452qAccelerometerImpl.this.movementType= type;
                    Mma8452qAccelerometerImpl.this.movementAxes= movementAxes;
                    Mma8452qAccelerometerImpl.this.movementThreshold= this.movementThreshold;
                    Mma8452qAccelerometerImpl.this.movementDuration= this.movementDuration;
                }

                @Override
                public MovementConfigEditor setAxes(Axis... axes) {
                    movementAxes= new Axis[axes.length];
                    System.arraycopy(axes, 0, movementAxes, 0, axes.length);
                    return this;
                }
            };
        }

        @Override
        public void startMovementDetection() {
            writeRegister(MwrAccelerometerRegister.MOVEMENT_ENABLE, (byte) 1);
        }

        @Override
        public void stopMovementDetection() {
            writeRegister(MwrAccelerometerRegister.MOVEMENT_ENABLE, (byte) 0);
        }

        @Override
        public OrientationConfigEditor configureOrientationDetection() {
            return new OrientationConfigEditor() {
                private int delay= 150;

                @Override
                public OrientationConfigEditor setDelay(int delay) {
                    this.delay= delay;
                    return this;
                }

                @Override
                public void commit() {
                    orientationDelay= this.delay;
                }
            };
        }

        @Override
        public void startOrientationDetection() {
            mma8452qOrientationCfg[1]= (byte) 0xc0;
            writeRegister(MwrAccelerometerRegister.ORIENTATION_ENABLE, (byte) 1);
        }
        @Override
        public void stopOrientationDetection() {
            writeRegister(MwrAccelerometerRegister.ORIENTATION_ENABLE, (byte) 0);
            mma8452qOrientationCfg[1]= 0x0;
            writeRegister(MwrAccelerometerRegister.ORIENTATION_CONFIG, mma8452qOrientationCfg);
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

    private boolean saveCommand= false;
    private EventListener currEventListener;
    private byte[] eventConfig= null;
    private MessageToken eventParams= null;
    private byte eventDestOffset= 0;
    private void buildBlePacket(byte module, byte register, byte ... parameters) {
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
            conn.sendCommand(false, eventEntry);

            byte[] eventParameters= new byte[cmd.length];
            System.arraycopy(cmd, 2, eventParameters, 2, cmd.length - 2);
            eventParameters[0]= EventRegister.CMD_PARAMETERS.moduleOpcode();
            eventParameters[1]= EventRegister.CMD_PARAMETERS.opcode();
            conn.sendCommand(false, eventParameters);

            if (saveCommand) {
                commands.add(eventEntry);
                commands.add(eventParameters);
            }
        } else {
            conn.sendCommand(false, cmd);
            if (saveCommand) {
                commands.add(cmd);
            }
        }
    }

    private void writeRegister(Register register, byte ... parameters) {
        buildBlePacket(register.moduleOpcode(), register.opcode(), parameters);
    }
    private void readRegister(Register register, byte ... parameters) {
        buildBlePacket(register.moduleOpcode(), (byte) (0x80 | register.opcode()), parameters);
    }
}