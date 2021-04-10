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

import com.mbientlab.metawear.TaskTimeoutException;
import com.mbientlab.metawear.impl.DataProcessorImpl.ProcessorEntry;
import com.mbientlab.metawear.impl.platform.TimedTask;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Logging;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;
import bolts.TaskCompletionSource;

import static com.mbientlab.metawear.impl.Constant.Module.DATA_PROCESSOR;
import static com.mbientlab.metawear.impl.Constant.Module.LOGGING;

/**
 * Created by etsai on 9/4/16.
 */
class LoggingImpl extends ModuleImplBase implements Logging {
    private static final long serialVersionUID = 5585806147100904291L;
    private final static double TICK_TIME_STEP= (48.0 / 32768.0) * 1000.0;
    private static final byte LOG_ENTRY_SIZE= 4, REVISION_EXTENDED_LOGGING = 2, MMS_REVISION = 3;
    private static final byte ENABLE = 1,
            TRIGGER = 2,
            REMOVE = 3,
            TIME = 4,
            LENGTH = 5,
            READOUT = 6, READOUT_NOTIFY = 7, READOUT_PROGRESS = 8,
            REMOVE_ENTRIES = 9, REMOVE_ALL = 0xa,
            CIRCULAR_BUFFER = 0xb,
            READOUT_PAGE_COMPLETED = 0xd, READOUT_PAGE_CONFIRM = 0xe,
            PAGE_FLUSH = 0x10;

    private static class TimeReference implements Serializable {
        private static final long serialVersionUID = -4058532490858952714L;

        final byte resetUid;
        long tick;
        final Calendar timestamp;

        TimeReference(byte resetUid, long tick, Calendar timestamp) {
            this.timestamp= timestamp;
            this.tick= tick;
            this.resetUid= resetUid;
        }
    }
    static class DataLogger extends DeviceDataConsumer implements Serializable {
        private static final long serialVersionUID = -5621099865981017205L;

        private final LinkedHashMap<Byte, LinkedList<byte[]>> logEntries= new LinkedHashMap<>();

        DataLogger(DataTypeBase source) {
            super(source);
        }

        void addId(byte id) {
            logEntries.put(id, new LinkedList<>());
        }

        public void remove(MetaWearBoardPrivate mwPrivate) {
            for(byte id: logEntries.keySet()) {
                mwPrivate.sendCommand(new byte[]{Constant.Module.LOGGING.id, LoggingImpl.REMOVE, id});
            }
        }

        void register(Map<Byte, DataLogger> loggers) {
            for(byte id: logEntries.keySet()) {
                loggers.put(id, this);
            }
        }

        void handleLogMessage(final MetaWearBoardPrivate mwPrivate, byte logId, final Calendar timestamp, byte[] data, Logging.LogDownloadErrorHandler handler) {
            if (subscriber == null) {
                if (handler != null) {
                    handler.receivedError(Logging.DownloadError.UNHANDLED_LOG_DATA, logId, timestamp, data);
                } else {
                    mwPrivate.logWarn(String.format(Locale.US, "No subscriber to handle log data: {logId: %d, time: %d, data: %s}",
                            logId, timestamp.getTimeInMillis(), Util.arrayToHexString(data)));
                }

                return;
            }

            if (logEntries.containsKey(logId)) {
                logEntries.get(logId).add(data);
            } else if (handler != null) {
                handler.receivedError(Logging.DownloadError.UNKNOWN_LOG_ENTRY, logId, timestamp, data);
            }

            boolean noneEmpty= true;
            for(Queue<byte[]> cachedValues: logEntries.values()) {
                noneEmpty&= !cachedValues.isEmpty();
            }

            if (noneEmpty) {
                ArrayList<byte[]> entries= new ArrayList<>(logEntries.values().size());
                for(Queue<byte[]> cachedValues: logEntries.values()) {
                    entries.add(cachedValues.poll());
                }

                final byte[] merged= new byte[source.attributes.length()];
                int offset= 0;
                for(int i= 0; i < entries.size(); i++) {
                    int copyLength= Math.min(entries.get(i).length, source.attributes.length() - offset);
                    System.arraycopy(entries.get(i), 0, merged, offset, copyLength);
                    offset+= entries.get(i).length;
                }

                call(source.createMessage(true, mwPrivate, merged, timestamp, null));
            }
        }

        @Override
        public void enableStream(MetaWearBoardPrivate mwPrivate) {
        }

        @Override
        public void disableStream(MetaWearBoardPrivate mwPrivate) {
        }

        @Override
        public void addDataHandler(final MetaWearBoardPrivate mwPrivate) {
        }
    }

    // Logger state
    private final HashMap<Byte, TimeReference> logReferenceTicks= new HashMap<>();
    private final HashMap<Byte, Long> lastTimestamp= new HashMap<>();
    private TimeReference latestReference;
    private final HashMap<Byte, DataLogger> dataLoggers= new HashMap<>();
    private HashMap<Byte, Long> rollbackTimestamps = new HashMap<>();

    private transient long nLogEntries;
    private transient int nUpdates;
    private transient LogDownloadUpdateHandler updateHandler;
    private transient LogDownloadErrorHandler errorHandler;

    private transient AtomicReference<TaskCompletionSource<Void>> downloadTask;
    private transient TimedTask<byte[]> createLoggerTask, syncLoggerConfigTask;
    private transient TimedTask<Void> queryTimeTask;

    LoggingImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
    }

    @Override
    public void disconnected() {
        rollbackTimestamps.putAll(lastTimestamp);
        TaskCompletionSource<Void> taskSource = downloadTask.getAndSet(null);
        if (taskSource != null) {
            taskSource.setError(new RuntimeException("Lost connection while downloading log data"));
        }
    }

    void removeDataLogger(boolean sync, DataLogger logger) {
        if (sync) {
            logger.remove(mwPrivate);
        }

        for(byte id: logger.logEntries.keySet()) {
            dataLoggers.remove(id);
        }
    }

    private void completeDownloadTask() {
        rollbackTimestamps.clear();
        TaskCompletionSource<Void> taskSource = downloadTask.getAndSet(null);
        if (taskSource != null) {
            taskSource.setResult(null);
        } else {
            mwPrivate.logWarn("Log download finished but no Task object to complete");
        }
    }

    @Override
    public void tearDown() {
        dataLoggers.clear();

        mwPrivate.sendCommand(new byte[] {LOGGING.id, REMOVE_ALL});
    }

    private transient HashMap<Tuple3<Byte, Byte, Byte>, Byte> placeholder;
    private DataTypeBase guessLogSource(Collection<DataTypeBase> producers, Tuple3<Byte, Byte, Byte> key, byte offset, byte length) {
        List<DataTypeBase> possible = new ArrayList<>();

        for(DataTypeBase it: producers) {
            if (it.eventConfig[0] == key.first && it.eventConfig[1] == key.second && it.eventConfig[2] == key.third) {
                possible.add(it);
            }
        }

        DataTypeBase original = null;
        boolean multipleEntries = false;
        for(DataTypeBase it: possible) {
            if (it.attributes.length() > 4) {
                original = it;
                multipleEntries = true;
            }
        }

        if (multipleEntries) {
            if (offset == 0 && length > LOG_ENTRY_SIZE) {
                return original;
            }
            if (!placeholder.containsKey(key) && length == LOG_ENTRY_SIZE) {
                placeholder.put(key, length);
                return original;
            }
            if (placeholder.containsKey(key)) {
                byte newLength = (byte) (placeholder.get(key) + length);
                if (newLength == original.attributes.length()) {
                    placeholder.remove(key);
                }
                return original;
            }
        }

        for(DataTypeBase it: possible) {
            if (it.attributes.offset == offset && it.attributes.length() == length) {
                return it;
            }
        }
        return null;
    }

    @Override
    protected void init() {
        createLoggerTask = new TimedTask<>();
        syncLoggerConfigTask = new TimedTask<>();

        downloadTask = new AtomicReference<>();
        if (rollbackTimestamps == null) {
            rollbackTimestamps = new HashMap<>();
        }

        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, Util.setRead(TRIGGER)), response -> syncLoggerConfigTask.setResult(response));
        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, TRIGGER), response -> createLoggerTask.setResult(response));
        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, READOUT_NOTIFY), response -> {
            processLogData(Arrays.copyOfRange(response, 2, 11));

            if (response.length == 20) {
                processLogData(Arrays.copyOfRange(response, 11, 20));
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, READOUT_PROGRESS), response -> {
            byte[] padded= new byte[8];
            System.arraycopy(response, 2, padded, 0, response.length - 2);
            long nEntriesLeft= ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).getLong(0);

            if (nEntriesLeft == 0) {
                completeDownloadTask();
            } else if (updateHandler != null) {
                updateHandler.receivedUpdate(nEntriesLeft, nLogEntries);
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, Util.setRead(TIME)), response -> {
            byte[] padded= new byte[8];
            System.arraycopy(response, 2, padded, 0, 4);
            final long tick= ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).getLong(0);
            byte resetUid= (response.length > 6) ? response[6] : -1;

            // if in the middle of a log download, don't update the reference
            // rollbackTimestamps var is cleared after readout progress hits 0
            if (rollbackTimestamps.isEmpty()) {
                latestReference = new TimeReference(resetUid, tick, Calendar.getInstance());
                if (resetUid != -1) {
                    logReferenceTicks.put(latestReference.resetUid, latestReference);
                }
            }

            if (queryTimeTask != null) {
                queryTimeTask.setResult(null);
                queryTimeTask = null;
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, Util.setRead(LENGTH)), response -> {
            int payloadSize= response.length - 2;

            byte[] padded= new byte[8];
            System.arraycopy(response, 2, padded, 0, payloadSize);
            nLogEntries= ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).getLong();

            if (nLogEntries == 0) {
                completeDownloadTask();
            } else {
                if (updateHandler != null) {
                    updateHandler.receivedUpdate(nLogEntries, nLogEntries);
                }

                long nEntriesNotify = nUpdates == 0 ? 0 : (long) (nLogEntries * (1.0 / nUpdates));

                ///< In little endian, [A, B, 0, 0] is equal to [A, B]
                ByteBuffer readoutCommand = ByteBuffer.allocate(payloadSize + 4).order(ByteOrder.LITTLE_ENDIAN)
                        .put(response, 2, payloadSize).putInt((int) nEntriesNotify);
                mwPrivate.sendCommand(LOGGING, READOUT, readoutCommand.array());
            }
        });

        if (mwPrivate.lookupModuleInfo(LOGGING).revision >= REVISION_EXTENDED_LOGGING) {
            this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, READOUT_PAGE_COMPLETED), response -> mwPrivate.sendCommand(new byte[] {LOGGING.id, READOUT_PAGE_CONFIRM}));
        }
    }

    @Override
    public void start(boolean overwrite) {
        mwPrivate.sendCommand(new byte[] {LOGGING.id, CIRCULAR_BUFFER, (byte) (overwrite ? 1 : 0)});
        mwPrivate.sendCommand(new byte[] {LOGGING.id, ENABLE, 1});
    }

    @Override
    public void stop() {
        mwPrivate.sendCommand(new byte[] {LOGGING.id, ENABLE, 0});
    }

    @Override
    public Task<Void> downloadAsync(int nUpdates, LogDownloadUpdateHandler updateHandler, LogDownloadErrorHandler errorHandler) {
        TaskCompletionSource<Void> taskSource = downloadTask.get();
        if (taskSource != null) {
            return taskSource.getTask();
        }

        this.nUpdates = nUpdates;
        this.updateHandler= updateHandler;
        this.errorHandler= errorHandler;

        if (mwPrivate.lookupModuleInfo(LOGGING).revision >= REVISION_EXTENDED_LOGGING) {
            mwPrivate.sendCommand(new byte[] {LOGGING.id, READOUT_PAGE_COMPLETED, 1});
        }
        mwPrivate.sendCommand(new byte[] {LOGGING.id, READOUT_NOTIFY, 1});
        mwPrivate.sendCommand(new byte[] {LOGGING.id, READOUT_PROGRESS, 1});
        mwPrivate.sendCommand(new byte[] {LOGGING.id, Util.setRead(LENGTH)});

        taskSource = new TaskCompletionSource<>();
        downloadTask.set(taskSource);
        return taskSource.getTask();
    }

    @Override
    public Task<Void> downloadAsync(int nUpdates, LogDownloadUpdateHandler updateHandler) {
        return downloadAsync(nUpdates, updateHandler, null);
    }

    @Override
    public Task<Void> downloadAsync(LogDownloadErrorHandler errorHandler) {
        return downloadAsync(0, null, errorHandler);
    }

    @Override
    public Task<Void> downloadAsync() {
        return downloadAsync(0, null, null);
    }

    @Override
    public void clearEntries() {
        if (mwPrivate.lookupModuleInfo(LOGGING).revision >= REVISION_EXTENDED_LOGGING) {
            mwPrivate.sendCommand(new byte[] {LOGGING.id, READOUT_PAGE_COMPLETED, (byte) 1});
        }
        mwPrivate.sendCommand(new byte[] {LOGGING.id, REMOVE_ENTRIES, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
    }

    @Override
    public void flushPage() {
        if (mwPrivate.lookupModuleInfo(LOGGING).revision >= MMS_REVISION) {
            mwPrivate.sendCommand(new byte[] {LOGGING.id, PAGE_FLUSH, (byte) 1});
        }
    }

    Task<Void> queryTime() {
        queryTimeTask = new TimedTask<>();
        return queryTimeTask.execute("Did not receive log reference response within %dms", Constant.RESPONSE_TIMEOUT,
                () -> mwPrivate.sendCommand(new byte[] { Constant.Module.LOGGING.id, Util.setRead(LoggingImpl.TIME) }));
    }

    Task<Queue<DataLogger>> queueLoggers(Queue<DataTypeBase> producers) {
        final Queue<DataLogger> loggers = new LinkedList<>();
        final Capture<Boolean> terminate = new Capture<>(false);

        return Task.forResult(null).continueWhile(() -> !terminate.get() && !producers.isEmpty(), ignored -> {
            final DataLogger next = new DataLogger(producers.poll());
            final byte[] eventConfig= next.source.eventConfig;

            final byte nReqLogIds= (byte) ((next.source.attributes.length() - 1) / LOG_ENTRY_SIZE + 1);
            final Capture<Byte> remainder= new Capture<>(next.source.attributes.length());
            final Capture<Byte> i = new Capture<>((byte) 0);

            return Task.forResult(null).continueWhile(() -> !terminate.get() && i.get() < nReqLogIds, ignored2 -> {
                final int entrySize= Math.min(remainder.get(), LOG_ENTRY_SIZE), entryOffset= LOG_ENTRY_SIZE * i.get() + next.source.attributes.offset;

                final byte[] command= new byte[6];
                command[0]= LOGGING.id;
                command[1]= TRIGGER;
                System.arraycopy(eventConfig, 0, command, 2, eventConfig.length);
                command[5]= (byte) (((entrySize - 1) << 5) | entryOffset);

                return createLoggerTask.execute("Did not receive log id within %dms", Constant.RESPONSE_TIMEOUT,
                        () -> mwPrivate.sendCommand(command)
                ).continueWithTask(task -> {
                    if (task.isFaulted()) {
                        terminate.set(true);
                        return Task.<Void>forError(new TaskTimeoutException(task.getError(), next));
                    }

                    next.addId(task.getResult()[2]);
                    i.set((byte) (i.get() + 1));
                    remainder.set((byte) (remainder.get() - LOG_ENTRY_SIZE));

                    return Task.forResult(null);
                });
            }).onSuccessTask(ignored2 -> {
                loggers.add(next);
                next.register(dataLoggers);
                return Task.forResult(null);
            });
        }).continueWithTask(task -> {
            if (task.isFaulted()) {
                boolean taskTimeout = task.getError() instanceof TaskTimeoutException;
                if (taskTimeout) {
                    loggers.add((DataLogger) ((TaskTimeoutException) task.getError()).partial);
                }
                while(!loggers.isEmpty()) {
                    loggers.poll().remove(LoggingImpl.this.mwPrivate);
                }
                return Task.forError(taskTimeout ? (Exception) task.getError().getCause() : task.getError());
            }

            return Task.forResult(loggers);
        });
    }


    private void processLogData(byte[] logEntry) {
        final byte logId= (byte) (logEntry[0] & 0x1f), resetUid = (byte) (((logEntry[0] & ~0x1f) >> 5) & 0x7);

        byte[] padded= new byte[8];
        System.arraycopy(logEntry, 1, padded, 0, 4);
        long tick= ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).getLong(0);

        if (!rollbackTimestamps.containsKey(resetUid) || rollbackTimestamps.get(resetUid) < tick) {
            final byte[] logData = Arrays.copyOfRange(logEntry, 5, logEntry.length);
            final Calendar realTimestamp = computeTimestamp(resetUid, tick);

            if (dataLoggers.containsKey(logId)) {
                dataLoggers.get(logId).handleLogMessage(mwPrivate, logId, realTimestamp, logData, errorHandler);
            } else if (errorHandler != null) {
                errorHandler.receivedError(DownloadError.UNKNOWN_LOG_ENTRY, logId, realTimestamp, logData);
            }
        }
    }

    Calendar computeTimestamp(byte resetUid, long tick) {
        TimeReference reference= logReferenceTicks.containsKey(resetUid) ? logReferenceTicks.get(resetUid) : latestReference;

        if (lastTimestamp.containsKey(resetUid) && lastTimestamp.get(resetUid) > tick) {
            long diff = (tick - lastTimestamp.get(resetUid)) & 0xffffffffL;
            long offset = diff + (lastTimestamp.get(resetUid) - reference.tick);
            reference.timestamp.setTimeInMillis(reference.timestamp.getTimeInMillis() + (long) (offset * TICK_TIME_STEP));
            reference.tick = tick;

            if (rollbackTimestamps.containsKey(resetUid)) {
                rollbackTimestamps.put(resetUid, tick);
            }
        }
        lastTimestamp.put(resetUid, tick);

        long offset = (long) ((tick - reference.tick) * TICK_TIME_STEP);
        final Calendar timestamp= (Calendar) reference.timestamp.clone();
        timestamp.setTimeInMillis(timestamp.getTimeInMillis() + offset);

        return timestamp;
    }

    private Task<Collection<DataLogger>> queryActiveLoggersInnerAsync(final byte id) {
        final Map<DataTypeBase, Byte> nRemainingLoggers = new HashMap<>();
        final Capture<Byte> offset = new Capture<>();
        final Capture<byte[]> response = new Capture<>();
        final DataProcessorImpl dataprocessor = (DataProcessorImpl) mwPrivate.getModules().get(DataProcessor.class);

        final Deque<Byte> fuserIds = new LinkedList<>();
        final Deque<Pair<DataTypeBase, ProcessorEntry>> fuserConfigs = new LinkedList<>();
        final Capture<Continuation<Deque<ProcessorEntry>, Task<DataTypeBase>>> onProcessorSynced = new Capture<>();
        onProcessorSynced.set(task -> {
            Deque<ProcessorEntry> result = task.getResult();
            ProcessorEntry first = result.peek();
            DataTypeBase type = guessLogSource(mwPrivate.getDataTypes(), new Tuple3<>(first.source[0], first.source[1], first.source[2]), first.offset, first.length);

            byte revision = mwPrivate.lookupModuleInfo(DATA_PROCESSOR).revision;
            while(!result.isEmpty()) {
                ProcessorEntry current = result.poll();
                if (current.config[0] == DataProcessorConfig.Fuser.ID) {
                    for(int i = 0; i < (current.config[1] & 0x1f); i++) {
                        fuserIds.push(current.config[i + 2]);
                    }
                    fuserConfigs.push(new Pair<>(type, current));;
                } else {
                    DataProcessorConfig config = DataProcessorConfig.from(mwPrivate.getFirmwareVersion(), revision, current.config);
                    Pair<? extends DataTypeBase, ? extends DataTypeBase> next = type.dataProcessorTransform(config,
                            (DataProcessorImpl) mwPrivate.getModules().get(DataProcessor.class));

                    next.first.eventConfig[2] = current.id;
                    if (next.second != null) {
                        next.second.eventConfig[2] = current.id;
                    }
                    dataprocessor.addProcessor(current.id, next.second, type, config);
                    type = next.first;
                }
            }

            if (fuserIds.size() == 0) {
                while(fuserConfigs.size() != 0) {
                    Pair<DataTypeBase, ProcessorEntry> top = fuserConfigs.poll();
                    DataProcessorConfig config = DataProcessorConfig.from(mwPrivate.getFirmwareVersion(), revision, top.second.config);
                    Pair<? extends DataTypeBase, ? extends DataTypeBase> next = top.first.dataProcessorTransform(config,
                            (DataProcessorImpl) mwPrivate.getModules().get(DataProcessor.class));

                    next.first.eventConfig[2] = top.second.id;
                    if (next.second != null) {
                        next.second.eventConfig[2] = top.second.id;
                    }
                    dataprocessor.addProcessor(top.second.id, next.second, top.first, config);

                    type = next.first;
                }
                return Task.forResult(type);
            } else {
                return dataprocessor.pullChainAsync(fuserIds.poll()).onSuccessTask(onProcessorSynced.get());
            }
        });

        return syncLoggerConfigTask.execute("Did not receive logger config for id=" + id + " within %dms", Constant.RESPONSE_TIMEOUT,
                () -> mwPrivate.sendCommand(new byte[] {0x0b, Util.setRead(TRIGGER), id})
        ).onSuccessTask(task -> {
            response.set(task.getResult());
            if (response.get().length > 2) {
                offset.set((byte) (response.get()[5] & 0x1f));
                byte length = (byte) (((response.get()[5] >> 5) & 0x3) + 1);

                if (response.get()[2] == DATA_PROCESSOR.id && (response.get()[3] == DataProcessorImpl.NOTIFY || Util.clearRead(response.get()[3]) == DataProcessorImpl.STATE)) {
                    return dataprocessor.pullChainAsync(response.get()[4]).onSuccessTask(onProcessorSynced.get());
                } else {
                    return Task.forResult(guessLogSource(mwPrivate.getDataTypes(), new Tuple3<>(response.get()[2], response.get()[3], response.get()[4]), offset.get(), length));
                }
            } else {
                return Task.cancelled();
            }
        }).onSuccessTask(task -> {
            DataTypeBase dataTypeBase = task.getResult();

            if (response.get()[2] == DATA_PROCESSOR.id && Util.clearRead(response.get()[3]) == DataProcessorImpl.STATE) {
                dataTypeBase = dataprocessor.lookupProcessor(response.get()[4]).state;
            }

            if (!nRemainingLoggers.containsKey(dataTypeBase) && dataTypeBase.attributes.length() > LOG_ENTRY_SIZE) {
                nRemainingLoggers.put(dataTypeBase, (byte) Math.ceil((float) (dataTypeBase.attributes.length() / LOG_ENTRY_SIZE)));
            }

            DataLogger logger = null;
            for(DataLogger it: dataLoggers.values()) {
                if (Arrays.equals(it.source.eventConfig, dataTypeBase.eventConfig) && it.source.attributes.equals(dataTypeBase.attributes)) {
                    logger = it;
                    break;
                }
            }

            if (logger == null || (offset.get() != 0 && !nRemainingLoggers.containsKey(dataTypeBase))) {
                logger = new DataLogger(dataTypeBase);
            }
            logger.addId(id);
            dataLoggers.put(id, logger);

            if (nRemainingLoggers.containsKey(dataTypeBase)) {
                byte remaining = (byte) (nRemainingLoggers.get(dataTypeBase) - 1);
                nRemainingLoggers.put(dataTypeBase, remaining);
                if (remaining < 0) {
                    nRemainingLoggers.remove(dataTypeBase);
                }
            }
            return Task.forResult(null);
        }).continueWithTask(task -> {
            if (!task.isFaulted()) {
                byte nextId = (byte) (id + 1);
                if (nextId < mwPrivate.lookupModuleInfo(LOGGING).extra[0]) {
                    return queryActiveLoggersInnerAsync(nextId);
                }
                Collection<DataLogger> orderedLoggers = new ArrayList<>();
                for(Byte it: new TreeSet<>(dataLoggers.keySet())) {
                    if (!orderedLoggers.contains(dataLoggers.get(it))) {
                        orderedLoggers.add(dataLoggers.get(it));
                    }
                }
                return Task.forResult(orderedLoggers);
            }
            return Task.forError(task.getError());
        });
    }
    Task<Collection<DataLogger>> queryActiveLoggersAsync() {
        placeholder = new HashMap<>();
        return queryActiveLoggersInnerAsync((byte) 0);
    }
}
