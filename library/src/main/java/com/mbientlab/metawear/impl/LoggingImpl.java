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

import android.util.Log;

import com.mbientlab.metawear.impl.MetaWearBoardImpl.RegisterResponseHandler;
import com.mbientlab.metawear.module.Logging;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import bolts.Task;
import bolts.TaskCompletionSource;

import static com.mbientlab.metawear.impl.ModuleId.LOGGING;

/**
 * Created by etsai on 9/4/16.
 */
class LoggingImpl extends ModuleImplBase implements Logging {
    private static final long serialVersionUID = 5585806147100904291L;
    private final static double TICK_TIME_STEP= (48.0 / 32768.0) * 1000.0;
    private static final byte LOG_ENTRY_SIZE= 4, REVISION_EXTENDED_LOGGING = 2;
    private static final byte ENABLE = 1,
            TRIGGER = 2,
            REMOVE = 3,
            TIME = 4,
            LENGTH = 5,
            READOUT = 6, READOUT_NOTIFY = 7, READOUT_PROGRESS = 8,
            REMOVE_ENTRIES = 9, REMOVE_ALL = 0xa,
            CIRCULAR_BUFFER = 0xb,
            READOUT_PAGE_COMPLETED = 0xd, READOUT_PAGE_CONFIRM = 0xe;

    private static class TimeReference implements Serializable {
        private static final long serialVersionUID = -4058532490858952714L;

        final byte resetUid;
        final long tick;
        final Calendar timestamp;

        public TimeReference(byte resetUid, long tick, Calendar timestamp) {
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
            logEntries.put(id, new LinkedList<byte[]>());
        }

        public void remove(MetaWearBoardPrivate owner) {
            for(byte id: logEntries.keySet()) {
                owner.sendCommand(new byte[]{ModuleId.LOGGING.id, LoggingImpl.REMOVE, id});
            }
        }

        void register(Map<Byte, DataLogger> loggers) {
            for(byte id: logEntries.keySet()) {
                loggers.put(id, this);
            }
        }

        void handleLogMessage(final MetaWearBoardPrivate owner, byte logId, final Calendar timestamp, byte[] data, Logging.LogDownloadErrorHandler handler) {
            if (subscriber == null) {
                if (handler != null) {
                    handler.receivedError(Logging.DownloadError.UNHANDLED_LOG_DATA, logId, timestamp, data);
                } else {
                    Log.e("MetaWear", "No subscriber available for the log data");
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

                call(source.createMessage(true, owner, merged, timestamp));
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

    private transient long nLogEntries;
    private transient int nUpdates;
    private transient LogDownloadUpdateHandler updateHandler;
    private transient LogDownloadErrorHandler errorHandler;

    private transient byte nReqLogIds;
    private transient ScheduledFuture<?> timeoutFuture;
    private transient DataLogger nextLogger;
    private transient Queue<DataTypeBase> pendingProducers;
    private transient Queue<DataLogger> successfulLoggers;
    private transient TaskCompletionSource<Queue<DataLogger>> createLoggerTask;
    private transient AtomicReference<TaskCompletionSource<Void>> downloadTask;
    private transient TaskCompletionSource<Void> queryTimeTask;
    private transient Runnable taskTimeout;

    LoggingImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
    }

    @Override
    public void disconnected() {
        if (downloadTask.get() != null) {
            TaskCompletionSource<Void> taskSource = downloadTask.getAndSet(null);
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

    @Override
    public void tearDown() {
        logReferenceTicks.clear();
        lastTimestamp.clear();
        dataLoggers.clear();

        mwPrivate.sendCommand(new byte[] {LOGGING.id, REMOVE_ALL});
    }

    @Override
    protected void init() {
        downloadTask = new AtomicReference<>();
        taskTimeout = new Runnable() {
            @Override
            public void run() {
                while(!successfulLoggers.isEmpty()) {
                    successfulLoggers.poll().remove(LoggingImpl.this.mwPrivate);
                }
                nextLogger.remove(LoggingImpl.this.mwPrivate);
                pendingProducers= null;
                createLoggerTask.setError(new TimeoutException("Creating logger timed out"));
            }
        };

        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, TRIGGER), new RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                nReqLogIds--;
                nextLogger.addId(response[2]);

                if (nReqLogIds == 0) {
                    nextLogger.register(dataLoggers);
                    successfulLoggers.add(nextLogger);
                    timeoutFuture.cancel(false);
                    createLogger();
                }
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, READOUT_NOTIFY), new RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                processLogData(Arrays.copyOfRange(response, 2, 11));

                if (response.length == 20) {
                    processLogData(Arrays.copyOfRange(response, 11, 20));
                }
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, READOUT_PROGRESS), new RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                byte[] padded= new byte[8];
                System.arraycopy(response, 2, padded, 0, response.length - 2);
                long nEntriesLeft= ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).getLong(0);

                if (nEntriesLeft == 0) {
                    lastTimestamp.clear();
                    TaskCompletionSource<Void> taskSource = downloadTask.getAndSet(null);
                    taskSource.setResult(null);
                } else if (updateHandler != null) {
                    updateHandler.receivedUpdate(nEntriesLeft, nLogEntries);
                }
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, Util.setRead(TIME)), new RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                byte[] padded= new byte[8];
                System.arraycopy(response, 2, padded, 0, 4);
                final long tick= ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).getLong(0);
                byte resetUid= (response.length > 6) ? response[6] : -1;

                latestReference= new TimeReference(resetUid, tick, Calendar.getInstance());
                if (resetUid != -1) {
                    logReferenceTicks.put(latestReference.resetUid, latestReference);
                }

                queryTimeTask.setResult(null);
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, Util.setRead(LENGTH)), new RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                int payloadSize= response.length - 2;

                byte[] padded= new byte[8];
                System.arraycopy(response, 2, padded, 0, payloadSize);
                nLogEntries= ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).getLong();

                if (nLogEntries == 0) {
                    TaskCompletionSource<Void> taskSource = downloadTask.getAndSet(null);
                    taskSource.setResult(null);
                } else {
                    long nEntriesNotify = nUpdates == 0 ? 0 : (long) (nLogEntries * (1.0 / nUpdates));

                    ///< In little endian, [A, B, 0, 0] is equal to [A, B]
                    ByteBuffer readoutCommand = ByteBuffer.allocate(payloadSize + 4).order(ByteOrder.LITTLE_ENDIAN)
                            .put(response, 2, payloadSize).putInt((int) nEntriesNotify);

                    mwPrivate.sendCommand(LOGGING, READOUT, readoutCommand.array());
                }
            }
        });

        if (mwPrivate.lookupModuleInfo(LOGGING).revision >= REVISION_EXTENDED_LOGGING) {
            this.mwPrivate.addResponseHandler(new Pair<>(LOGGING.id, READOUT_PAGE_COMPLETED), new RegisterResponseHandler() {
                @Override
                public void onResponseReceived(byte[] response) {
                    mwPrivate.sendCommand(new byte[] {LOGGING.id, READOUT_PAGE_CONFIRM});
                }
            });
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
    public Task<Void> download(int nUpdates, LogDownloadUpdateHandler updateHandler, LogDownloadErrorHandler errorHandler) {
        this.nUpdates = nUpdates;
        this.updateHandler= updateHandler;
        this.errorHandler= errorHandler;

        if (mwPrivate.lookupModuleInfo(LOGGING).revision >= REVISION_EXTENDED_LOGGING) {
            mwPrivate.sendCommand(new byte[] {LOGGING.id, READOUT_PAGE_COMPLETED, 1});
        }
        mwPrivate.sendCommand(new byte[] {LOGGING.id, READOUT_NOTIFY, 1});
        mwPrivate.sendCommand(new byte[] {LOGGING.id, READOUT_PROGRESS, (byte) (updateHandler != null ? 1 : 0)});
        mwPrivate.sendCommand(new byte[] {LOGGING.id, Util.setRead(LENGTH)});

        TaskCompletionSource<Void> taskSource = new TaskCompletionSource<>();
        downloadTask.set(taskSource);
        return taskSource.getTask();
    }

    @Override
    public Task<Void> download(int nUpdates, LogDownloadUpdateHandler updateHandler) {
        return download(nUpdates, updateHandler, null);
    }

    @Override
    public Task<Void> download(LogDownloadErrorHandler errorHandler) {
        return download(0, null, errorHandler);
    }

    @Override
    public Task<Void> download() {
        return download(0, null, null);
    }

    @Override
    public void clearEntries() {
        if (mwPrivate.lookupModuleInfo(LOGGING).revision >= REVISION_EXTENDED_LOGGING) {
            mwPrivate.sendCommand(new byte[] {LOGGING.id, READOUT_PAGE_COMPLETED, (byte) 1});
        }
        mwPrivate.sendCommand(new byte[] {LOGGING.id, REMOVE_ENTRIES, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
    }

    Task<Void> queryTime() {
        queryTimeTask = new TaskCompletionSource<>();
        mwPrivate.sendCommand(new byte[] { ModuleId.LOGGING.id, Util.setRead(LoggingImpl.TIME) });
        return queryTimeTask.getTask();
    }

    Task<Queue<DataLogger>> queueLoggers(Queue<DataTypeBase> producers) {
        pendingProducers= producers;
        createLoggerTask= new TaskCompletionSource<>();
        successfulLoggers= new LinkedList<>();
        createLogger();
        return createLoggerTask.getTask();
    }

    private void createLogger() {
        if (!pendingProducers.isEmpty()) {
            nextLogger = new DataLogger(pendingProducers.poll());
            final byte[] eventConfig= nextLogger.source.eventConfig;

            nReqLogIds= (byte) ((nextLogger.source.attributes.length() - 1) / LOG_ENTRY_SIZE + 1);
            int remainder= nextLogger.source.attributes.length();

            for(byte i= 0; i < nReqLogIds; i++, remainder-= LOG_ENTRY_SIZE) {
                final int entrySize= Math.min(remainder, LOG_ENTRY_SIZE), entryOffset= LOG_ENTRY_SIZE * i + nextLogger.source.attributes.offset;

                byte[] command= new byte[6];
                command[0]= LOGGING.id;
                command[1]= TRIGGER;
                System.arraycopy(eventConfig, 0, command, 2, eventConfig.length);
                command[5]= (byte) (((entrySize - 1) << 5) | entryOffset);

                mwPrivate.sendCommand(command);
            }
            timeoutFuture= mwPrivate.scheduleTask(taskTimeout, nReqLogIds * 250L);
        } else {
            createLoggerTask.setResult(successfulLoggers);
        }
    }

    private void processLogData(byte[] logEntry) {
        final byte logId= (byte) (logEntry[0] & 0x1f), resetUid= (byte) ((logEntry[0] & ~0x1f) >> 5);
        TimeReference reference= logReferenceTicks.containsKey(resetUid) ? logReferenceTicks.get(resetUid) : latestReference;

        byte[] padded= new byte[8];
        System.arraycopy(logEntry, 1, padded, 0, 4);
        long tick= ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).getLong(0);

        final Calendar timestamp= (Calendar) reference.timestamp.clone();
        timestamp.add(Calendar.MILLISECOND, (int) ((tick - reference.tick) * TICK_TIME_STEP));

        if (!lastTimestamp.containsKey(logId) || lastTimestamp.get(logId) < tick) {
            lastTimestamp.put(logId, tick);

            final byte[] logData= Arrays.copyOfRange(logEntry, 5, logEntry.length);

            if (dataLoggers.containsKey(logId)) {
                dataLoggers.get(logId).handleLogMessage(mwPrivate, logId, timestamp, logData, errorHandler);
            } else if (errorHandler != null){
                errorHandler.receivedError(DownloadError.UNKNOWN_LOG_ENTRY, logId, timestamp, logData);
            }
        }
    }
}
