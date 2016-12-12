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

import com.mbientlab.metawear.CodeBlock;
import com.mbientlab.metawear.DataToken;
import com.mbientlab.metawear.DeviceInformation;
import com.mbientlab.metawear.IllegalRouteOperationException;
import com.mbientlab.metawear.Observer;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.builder.RouteElement.Action;
import com.mbientlab.metawear.impl.RouteElementImpl.Cache;
import com.mbientlab.metawear.impl.DataProcessorImpl.Processor;
import com.mbientlab.metawear.impl.LoggingImpl.DataLogger;
import com.mbientlab.metawear.module.*;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.module.Timer.ScheduledTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;
import bolts.TaskCompletionSource;

import static com.mbientlab.metawear.impl.ModuleId.DATA_PROCESSOR;
import static com.mbientlab.metawear.impl.ModuleId.MACRO;
import static com.mbientlab.metawear.impl.Platform.GattCharWriteType.*;

/**
 * Created by etsai on 8/31/16.
 */
public class MetaWearBoardImpl implements MetaWearBoard, MetaWearBoardPrivate {
    private static final ScheduledExecutorService SCHEDULED_TASK_THREADPOOL = Executors.newScheduledThreadPool(4);
    private static final long RELEASE_INFO_TTL = 1800000L;
    private static final byte READ_INFO_REGISTER= Util.setRead((byte) 0x0);
    private static final Pair<UUID, UUID> MW_CMD_GATT_CHAR= new Pair<>(
            METAWEAR_SERVICE_UUID,
            UUID.fromString("326A9001-85CB-9195-D9DD-464CFBBAE75A")
    );
    private final static String FIRMWARE_BUILD= "vanilla";

    public interface RegisterResponseHandler {
        void onResponseReceived(byte[] response);
    }

    private enum RouteType {
        DATA,
        TIMER,
        EVENT
    }

    private static class BoardInfo implements Serializable {
        private static final long serialVersionUID = 4634576514040923829L;

        public final Map<ModuleId, ModuleInfo> moduleInfo= new HashMap<>();
        public Version firmware= new Version(0, 0, 0);
        public String modelNumber= null, hardwareRevision= null;
    }
    private static class PersistentData implements Serializable {
        private static final long serialVersionUID = -6736797000323634463L;

        int routeIdCounter;
        BoardInfo boardInfo;
        final Map<Integer, RouteInner> activeRoutes= new HashMap<>();
        final Map<Integer, ObserverInner> activeEventManagers= new HashMap<>();
        final Map<String, DataTypeBase> taggedProducers= new HashMap<>();
        final Map<Class<? extends Module>, Module> modules= new LinkedHashMap<>();

        PersistentData() {
            routeIdCounter= 0;
        }
    }
    private static class RouteInner implements Route, Serializable {
        private static final long serialVersionUID = -8537409673730434416L;

        private final LinkedList<Byte> eventCmdIds;
        private final ArrayList<DeviceDataConsumer> consumers;
        private final LinkedList<Byte> dataprocessors;
        private final HashSet<String> processorNames;
        private final int id;
        private boolean active;

        private transient MetaWearBoardPrivate owner;

        RouteInner(LinkedList<Byte> eventCmdIds, ArrayList<DeviceDataConsumer> consumers, LinkedList<Byte> dataprocessors, HashSet<String> processorNames, int id, MetaWearBoardPrivate owner) {
            this.eventCmdIds= eventCmdIds;
            this.consumers = consumers;
            this.dataprocessors= dataprocessors;
            this.processorNames = processorNames;
            this.id= id;
            this.active= true;
            this.owner = owner;
        }

        void restoreTransientVars(MetaWearBoardPrivate owner) {
            this.owner= owner;

            for(DeviceDataConsumer it: consumers) {
                it.addDataHandler(owner);
            }
        }

        @Override
        public boolean setEnvironment(int pos, Object ... env) {
            try {
                consumers.get(pos).environment= env;
                return true;
            } catch (IndexOutOfBoundsException ignored) {
                return false;
            }
        }

        @Override
        public boolean unsubscribe(int pos) {
            try {
                consumers.get(pos).disableStream(owner);
                return true;
            } catch (IndexOutOfBoundsException ignored) {
                return false;
            }
        }

        @Override
        public boolean resubscribe(int pos, Subscriber subscriber) {
            try {
                consumers.get(pos).subscriber= subscriber;
                consumers.get(pos).enableStream(owner);
                return true;
            } catch (IndexOutOfBoundsException ignored) {
                return false;
            }
        }

        void remove(boolean sync) {
            if (active) {
                active = false;
                for(String it: processorNames) {
                    owner.removeProducerTag(it);
                }

                LoggingImpl logging= (LoggingImpl) owner.getModules().get(Logging.class);
                for(DeviceDataConsumer it: consumers) {
                    if (it instanceof DataLogger) {
                        logging.removeDataLogger(sync, (DataLogger) it);
                    } else {
                        it.disableStream(owner);
                    }
                }

                for(byte it: dataprocessors) {
                    owner.removeProcessor(sync, it);
                }
                dataprocessors.clear();

                if (sync) {
                    EventImpl event = (EventImpl) owner.getModules().get(EventImpl.class);
                    for(Byte it: eventCmdIds) {
                        event.removeEventCommand(it);
                    }

                    owner.removeRoute(id);
                }
            }
        }

        @Override
        public void remove() {
            remove(true);
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public int id() {
            return id;
        }
    }
    private static class ObserverInner implements Observer, Serializable {
        private static final long serialVersionUID = -991370121066262533L;

        private final LinkedList<Byte> eventCmdIds;
        private final int id;
        private boolean active;

        private MetaWearBoardPrivate owner;

        private ObserverInner(int id, LinkedList<Byte> eventCmdIds) {
            this.eventCmdIds = eventCmdIds;
            this.id = id;
            active= true;
        }

        void restoreTransientVar(MetaWearBoardPrivate owner) {
            this.owner= owner;
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public void remove() {
            remove(true);
        }

        public void remove(boolean sync) {
            if (active) {
                active= false;

                if (sync) {
                    EventImpl event = (EventImpl) owner.getModules().get(EventImpl.class);
                    for (Byte it : eventCmdIds) {
                        event.removeEventCommand(it);
                    }

                    owner.removeEventManager(id);
                }
            }
        }
    }

    // Persistent data
    private PersistentData persist= new PersistentData();

    // routes
    private final Queue<Tuple3<RouteBuilder, ? extends RouteElementImpl, TaskCompletionSource<Route>>> pendingRoutes= new ConcurrentLinkedQueue<>();
    private final Queue<Tuple3<TaskCompletionSource<ScheduledTask>, CodeBlock, byte[]>> pendingTaskManagers= new ConcurrentLinkedQueue<>();
    private final Queue<Tuple3<TaskCompletionSource<Observer>, DataTypeBase, CodeBlock>> pendingEventManagers= new ConcurrentLinkedQueue<>();
    private final Queue<RouteType> routeTypes= new ConcurrentLinkedQueue<>();
    private LoggingImpl logger;
    private DataProcessorImpl dataprocessor;
    private TimerImpl mwTimer;
    private EventImpl event;
    private MacroImpl macro;

    private final HashSet<Pair<Byte, Byte>> dataIdHeaders= new HashSet<>();
    private final Map<Tuple3<Byte, Byte, Byte>, ArrayList<RegisterResponseHandler>> dataHandlers= new HashMap<>();
    private final Map<Pair<Byte, Byte>, RegisterResponseHandler> registerResponseHandlers= new HashMap<>();

    private final String macAddress;
    private final Platform platform;

    // Device Information
    private String serialNumber, manufacturer;
    private final AtomicReference<TaskCompletionSource<DeviceInformation>> readDevInfoTaskSource = new AtomicReference<>();
    private ScheduledFuture<?> readDevInfoFuture;
    private final Runnable readDevInfoTimeout = new Runnable() {
        @Override
        public void run() {
            readDevInfoFuture.cancel(false);
            readDevInfoTaskSource.getAndSet(null).setError(new TimeoutException("Reading device information timed out"));
        }
    };

    // Connection
    private ScheduledFuture<?> serviceDiscoveryFuture;
    private final AtomicReference<TaskCompletionSource<Void>> connectTaskSource = new AtomicReference<>();
    private TaskCompletionSource<Byte> readBatteryTask;
    private UnexpectedDisconnectHandler unexpectedDcHandler;
    private boolean connected;
    private final Continuation<Void, Void> timeReadContinuation = new Continuation<Void, Void>() {
        @Override
        public Void then(Task<Void> task) throws Exception {
            serviceDiscoveryFuture.cancel(false);
            try {
                platform.serializeBoardInfo(persist.boardInfo);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                connected= true;
                connectTaskSource.getAndSet(null).setResult(null);
            }

            return null;
        }
    };

    // module discovery
    private Queue<ModuleId> moduleQueries;

    public MetaWearBoardImpl(Platform platform, String macAddress) {
        this.platform= platform;
        this.macAddress = macAddress;
    }

    public void disconnected() {
        connected = false;
        for(Module it: persist.modules.values()) {
            ((ModuleImplBase) it).disconnected();
        }
    }

    @Override
    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public Task<Integer> readRssiAsync() {
        return platform.readRssiTask();
    }

    @Override
    public Task<Byte> readBatteryLevelAsync() {
        readBatteryTask= new TaskCompletionSource<>();
        platform.readGattCharacteristic(BtleCharacteristics.BATTERY_LEVEL);
        return readBatteryTask.getTask();
    }

    @Override
    public Task<DeviceInformation> readDeviceInformationAsync() {
        if (readDevInfoTaskSource.get() != null) {
            return readDevInfoTaskSource.get().getTask();
        }

        if (serialNumber != null && manufacturer != null) {
            TaskCompletionSource<DeviceInformation> taskSource = new TaskCompletionSource<>();
            taskSource.setResult(new DeviceInformation(manufacturer, persist.boardInfo.modelNumber, serialNumber,
                    persist.boardInfo.firmware.toString(), persist.boardInfo.hardwareRevision));
            return taskSource.getTask();
        }

        readDevInfoTaskSource.set(new TaskCompletionSource<DeviceInformation>());
        platform.readGattCharacteristic(serialNumber == null ? BtleCharacteristics.DEV_INFO_SERIAL_NUMBER : BtleCharacteristics.DEV_INFO_MANUFACTURER_NAME);
        readDevInfoFuture = scheduleTask(readDevInfoTimeout, 500L);
        return readDevInfoTaskSource.get().getTask();
    }

    private String buildFirmwareFileName(Pair<Version, String> latest) {
        return String.format(Locale.US, "%s_%s_%s_%s_%s",
                persist.boardInfo.hardwareRevision,
                persist.boardInfo.modelNumber,
                FIRMWARE_BUILD, latest.first.toString(), latest.second
        );
    }

    private Task<File> downloadFirmware(Pair<Version, String> latest) {
        String dlUrl = String.format(Locale.US, "https://releases.mbientlab.com/metawear/%s/%s/%s/%s/%s",
                persist.boardInfo.hardwareRevision,
                persist.boardInfo.modelNumber,
                FIRMWARE_BUILD, latest.first.toString(), latest.second
        );

        return platform.downloadFile(dlUrl, buildFirmwareFileName(latest));
    }

    @Override
    public Task<File> downloadLatestFirmwareAsync() {
        final TaskCompletionSource<File> taskSource = new TaskCompletionSource<>();
        File info1Json = platform.findFile("info1.json");

        if (!info1Json.exists() || info1Json.lastModified() < Calendar.getInstance().getTimeInMillis() - RELEASE_INFO_TTL) {
            platform.downloadFile("https://releases.mbientlab.com/metawear/info1.json", "info1.json")
                    .onSuccessTask(new Continuation<File, Task<File>>() {
                        @Override
                        public Task<File> then(Task<File> task) throws Exception {
                            return downloadFirmware(findLatestRelease(task.getResult()));
                        }
                    }).continueWith(new Continuation<File, Void>() {
                        @Override
                        public Void then(Task<File> task) throws Exception {
                            if (task.isFaulted()) {
                                taskSource.setError(task.getError());
                            } else {
                                taskSource.setResult(task.getResult());
                            }
                            return null;
                        }
                    });
        } else {
            try {
                Pair<Version, String> latest = findLatestRelease(info1Json);
                File firmware = platform.findFile(buildFirmwareFileName(latest));

                if (!firmware.exists()) {
                    downloadFirmware(latest)
                            .continueWith(new Continuation<File, Void>() {
                                @Override
                                public Void then(Task<File> task) throws Exception {
                                    if (task.isFaulted()) {
                                        taskSource.setError(task.getError());
                                    } else {
                                        taskSource.setResult(task.getResult());
                                    }
                                    return null;
                                }
                            });
                } else {
                    taskSource.setResult(firmware);
                }
            } catch (Exception e) {
                taskSource.setError(e);
            }
        }

        return taskSource.getTask();
    }

    @Override
    public Task<Boolean> checkForFirmwareUpdateAsync() {
        final TaskCompletionSource<Boolean> taskSource = new TaskCompletionSource<>();

        File info1Json = platform.findFile("info1.json");
        if (!info1Json.exists() || info1Json.lastModified() < Calendar.getInstance().getTimeInMillis() - RELEASE_INFO_TTL) {
            platform.downloadFile("https://releases.mbientlab.com/metawear/info1.json", "info1.json")
                    .onSuccessTask(new Continuation<File, Task<Boolean>>() {
                        @Override
                        public Task<Boolean> then(Task<File> task) throws Exception {
                            return Task.forResult(persist.boardInfo.firmware.compareTo(findLatestRelease(task.getResult()).first) < 0);
                        }
                    }).continueWith(new Continuation<Boolean, Void>() {
                        @Override
                        public Void then(Task<Boolean> task) throws Exception {
                            if (task.isFaulted()) {
                                taskSource.setError(task.getError());
                            } else {
                                taskSource.setResult(task.getResult());
                            }
                            return null;
                        }
                    });
        } else {
            try {
                taskSource.setResult(persist.boardInfo.firmware.compareTo(findLatestRelease(info1Json).first) < 0);
            } catch (Exception e) {
                taskSource.setError(e);
            }
        }

        return taskSource.getTask();
    }

    private Pair<Version, String> findLatestRelease(File releaseInfo) throws JSONException, IOException {
        String info1Json;

        {
            InputStream ins = new FileInputStream(releaseInfo);
            StringBuilder builder = new StringBuilder();

            byte data[] = new byte[1024];
            int count;
            while ((count = ins.read(data)) != -1) {
                builder.append(new String(data, 0, count));
            }

            info1Json = builder.toString();
        }

        JSONObject root = new JSONObject(info1Json);
        JSONObject models= root.getJSONObject(persist.boardInfo.hardwareRevision);
        JSONObject builds = models.getJSONObject(persist.boardInfo.modelNumber);
        JSONObject availableVersions = builds.getJSONObject(FIRMWARE_BUILD);

        Iterator<String> versionKeys = availableVersions.keys();
        TreeSet<Version> versions = new TreeSet<>();
        while (versionKeys.hasNext()) {
            versions.add(new Version(versionKeys.next()));
        }

        Iterator<Version> it = versions.descendingIterator();
        if (it.hasNext()) {
            Version latest = it.next();
            return new Pair<>(latest, availableVersions.getJSONObject(latest.toString()).getString("filename"));
        }

        throw new RuntimeException("No information available for this board");
    }

    private final Continuation<Void, Void> connectContinuation = new Continuation<Void, Void>() {
        @Override
        public Void then(Task<Void> task) throws Exception {
            if (task.isFaulted()) {
                connectTaskSource.getAndSet(null).setError(task.getError());
            } else {
                serviceDiscoveryFuture = scheduleTask(new Runnable() {
                    @Override
                    public void run() {
                        platform.disconnect();
                        connectTaskSource.getAndSet(null).setError(new TimeoutException("Service discovery timed out"));
                    }
                }, 7500L);

                if (persist.boardInfo == null) {
                    BoardInfo boardInfoState = (BoardInfo) platform.deserializeBoardInfo();
                    if (boardInfoState != null) {
                        persist.boardInfo = boardInfoState;
                        for (ModuleInfo it : boardInfoState.moduleInfo.values()) {
                            instantiateModule(it);
                        }
                    } else {
                        persist.boardInfo = new BoardInfo();
                    }
                }

                platform.readGattCharacteristic(BtleCharacteristics.DEV_INFO_FIRMWARE_VERSION);
            }
            return null;
        }
    };
    @Override
    public Task<Void> connectAsync() {
        if (connectTaskSource.get() == null) {
            connectTaskSource.set(new TaskCompletionSource<Void>());
            platform.connect().continueWith(connectContinuation);
        }

        return connectTaskSource.get().getTask();
    }

    @Override
    public Task<Void> connectAsync(long delay) {
        if (connectTaskSource.get() == null) {
            connectTaskSource.set(new TaskCompletionSource<Void>());
            scheduleTask(new Runnable() {
                @Override
                public void run() {
                    platform.connect().continueWith(connectContinuation);
                }
            }, delay);
        }
        return connectTaskSource.get().getTask();
    }

    @Override
    public Task<Void> disconnectAsync() {
        if (connectTaskSource.get() != null) {
            connectTaskSource.getAndSet(null).setCancelled();
        }

        return platform.disconnect();
    }

    @Override
    public void onUnexpectedDisconnect(UnexpectedDisconnectHandler handler) {
        this.unexpectedDcHandler= handler;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean inMetaBoot() {
        return platform.inMetaBoot();
    }

    @Override
    public <T extends Module> T getModule(Class<T> moduleClass) {
        if (inMetaBoot()) {
            return null;
        }
        return persist.modules.containsKey(moduleClass) ? moduleClass.cast(persist.modules.get(moduleClass)) : null;
    }

    @Override
    public <T extends Module> T getModuleOrThrow(Class<T> moduleClass) throws UnsupportedModuleException {
        if (inMetaBoot()) {
            throw new UnsupportedModuleException("Cannot access modules while in MetaBoot mode");
        }
        T module= getModule(moduleClass);
        if (module != null) {
            return module;
        }
        throw new UnsupportedModuleException(String.format("Module \'%s\' not supported on this board", moduleClass.toString()));
    }

    @Override
    public Route lookupRoute(int id) {
        return persist.activeRoutes.get(id);
    }

    @Override
    public Observer lookupObserver(int id) {
        return persist.activeEventManagers.get(id);
    }

    @Override
    public void tearDown() {
        for(RouteInner it: persist.activeRoutes.values()) {
            it.remove(false);
        }

        for(ObserverInner it: persist.activeEventManagers.values()) {
            it.remove(false);
        }

        for(Module it: persist.modules.values()) {
            ((ModuleImplBase) it).tearDown();
        }

        persist.routeIdCounter= 0;
        persist.activeRoutes.clear();
        persist.activeEventManagers.clear();
    }

    @Override
    public void serialize() {
        try {
            platform.serializeBoardState(persist);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deserialize() {
        try {
            persist= (PersistentData) platform.deserializeBoardState();

            for(Module it: persist.modules.values()) {
                ((ModuleImplBase) it).restoreTransientVars(this);
            }

            for(RouteInner it: persist.activeRoutes.values()) {
                it.restoreTransientVars(this);
            }

            for(ObserverInner it: persist.activeEventManagers.values()) {
                it.restoreTransientVar(this);
            }

            logger= (LoggingImpl) persist.modules.get(Logging.class);
            dataprocessor= (DataProcessorImpl) persist.modules.get(DataProcessor.class);
            mwTimer= (TimerImpl) persist.modules.get(Timer.class);
            event = (EventImpl) persist.modules.get(EventImpl.class);
            macro = (MacroImpl) persist.modules.get(Macro.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Task<Void> boardDisconnect() {
        return platform.boardDisconnect();
    }

    @Override
    public void sendCommand(byte[] command) {
        if (event.getEventConfig() != null) {
            event.convertToEventCommand(command);
        } else {
            platform.writeGattCharacteristic(command[0] == MACRO.id ? WRITE_WITH_RESPONSE : WRITE_WITHOUT_RESPONSE,
                    MW_CMD_GATT_CHAR, command);

            if (macro.isRecording()) {
                macro.collectCommand(command);
            }
        }
    }

    @Override
    public void sendCommand(byte[] command, int dest, DataToken input) {
        DataTypeBase producer= (DataTypeBase) input;
        event.feedbackParams= new Tuple3<>(producer.attributes.length(), producer.attributes.offset, (byte) dest);
        sendCommand(command);
        event.feedbackParams= null;
    }

    @Override
    public void sendCommand(ModuleId module, byte register, byte... parameters) {
        byte[] command= new byte[parameters.length + 2];
        System.arraycopy(parameters, 0, command, 2, parameters.length);
        command[0]= module.id;
        command[1]= register;

        sendCommand(command);
    }

    @Override
    public void sendCommand(ModuleId module, byte register, byte id, byte... parameters) {
        byte[] command= new byte[parameters.length + 3];
        System.arraycopy(parameters, 0, command, 3, parameters.length);
        command[0]= module.id;
        command[1]= register;
        command[2]= id;

        sendCommand(command);
    }

    private void sendCommand(int dest, DataToken input, ModuleId module, byte register, byte id, byte... parameters) {
        byte[] command= new byte[parameters.length + 3];
        System.arraycopy(parameters, 0, command, 3, parameters.length);
        command[0]= module.id;
        command[1]= register;
        command[2]= id;

        sendCommand(command, dest, input);
    }

    @Override
    public void tagProducer(String name, DataTypeBase producer) {
        persist.taggedProducers.put(name, producer);
    }

    @Override
    public DataTypeBase lookupProducer(String name) {
        return persist.taggedProducers.get(name);
    }

    @Override
    public boolean hasProducer(String name) {
        return persist.taggedProducers.containsKey(name);
    }

    @Override
    public void removeProducerTag(String name) {
        persist.taggedProducers.remove(name);
    }

    @Override
    public boolean creatingRoutes() {
        return !routeTypes.isEmpty();
    }

    @Override
    public ModuleInfo lookupModuleInfo(ModuleId id) {
        return persist.boardInfo.moduleInfo.get(id);
    }

    @Override
    public Map<Class<? extends Module>, Module> getModules() {
        return Collections.unmodifiableMap(persist.modules);
    }

    private void startServiceDiscovery(boolean refresh) {
        if (inMetaBoot()) {
            serviceDiscoveryFuture.cancel(false);
            connected= true;
            connectTaskSource.getAndSet(null).setResult(null);
            return;
        }

        if (refresh) {
            persist.routeIdCounter= 0;
            persist.taggedProducers.clear();
            persist.activeEventManagers.clear();
            persist.activeRoutes.clear();
            persist.boardInfo.moduleInfo.clear();
            persist.modules.clear();
        }

        ModuleId[] moduleIds= ModuleId.values();
        moduleQueries= new LinkedList<>();
        for(ModuleId it: moduleIds) {
            if (refresh || !persist.boardInfo.moduleInfo.containsKey(it)) {
                moduleQueries.add(it);
            }
        }

        if (moduleQueries.isEmpty()) {
            logger.queryTime().continueWith(timeReadContinuation);
        } else {
            platform.writeGattCharacteristic(WRITE_WITHOUT_RESPONSE, MW_CMD_GATT_CHAR, new byte[]{moduleQueries.poll().id, READ_INFO_REGISTER});
        }
    }

    public void handleReadGattCharResponse(Pair<UUID, UUID> characteristic, byte[] value) {
        if (characteristic.equals(BtleCharacteristics.DEV_INFO_FIRMWARE_VERSION)) {
            Version readFirmware= new Version(new String(value));
            if (persist.boardInfo.firmware.compareTo(readFirmware) != 0) {
                persist.boardInfo.firmware= readFirmware;

                if (persist.boardInfo.modelNumber == null) {
                    platform.readGattCharacteristic(BtleCharacteristics.DEV_INFO_MODEL_NUMBER);
                } else if (persist.boardInfo.hardwareRevision == null) {
                    platform.readGattCharacteristic(BtleCharacteristics.DEV_INFO_HARDWARE_REVISION);
                } else {
                    startServiceDiscovery(true);
                }
            } else {
                startServiceDiscovery(false);
            }
        } else if (characteristic.equals(BtleCharacteristics.DEV_INFO_MODEL_NUMBER)) {
            persist.boardInfo.modelNumber= new String(value);
            if (persist.boardInfo.hardwareRevision == null) {
                platform.readGattCharacteristic(BtleCharacteristics.DEV_INFO_HARDWARE_REVISION);
            } else {
                startServiceDiscovery(true);
            }
        } else if (characteristic.equals(BtleCharacteristics.DEV_INFO_HARDWARE_REVISION)) {
            persist.boardInfo.hardwareRevision= new String(value);
            startServiceDiscovery(true);
        } else if (characteristic.equals(BtleCharacteristics.BATTERY_LEVEL)) {
            readBatteryTask.setResult(value[0]);
        } else if (characteristic.equals(BtleCharacteristics.DEV_INFO_SERIAL_NUMBER)) {
            serialNumber = new String(value);
            if (manufacturer == null) {
                platform.readGattCharacteristic(BtleCharacteristics.DEV_INFO_MANUFACTURER_NAME);
            } else {
                readDevInfoFuture.cancel(false);
                readDevInfoTaskSource.getAndSet(null).setResult(new DeviceInformation(manufacturer, persist.boardInfo.modelNumber,
                        serialNumber, persist.boardInfo.firmware.toString(), persist.boardInfo.hardwareRevision));
            }
        } else if (characteristic.equals(BtleCharacteristics.DEV_INFO_MANUFACTURER_NAME)) {
            manufacturer = new String(value);
            readDevInfoFuture.cancel(false);
            readDevInfoTaskSource.getAndSet(null).setResult(new DeviceInformation(manufacturer, persist.boardInfo.modelNumber,
                    serialNumber, persist.boardInfo.firmware.toString(), persist.boardInfo.hardwareRevision));
        }
    }

    private void instantiateModule(ModuleInfo info) {
        if (!info.present()) {
            return;
        }

        switch(ModuleId.lookupEnum(info.id)) {
            case SWITCH:
                persist.modules.put(Switch.class, new SwitchImpl(MetaWearBoardImpl.this));
                break;
            case LED:
                persist.modules.put(Led.class, new LedImpl(MetaWearBoardImpl.this));
                break;
            case ACCELEROMETER:
                Accelerometer acc;
                switch(info.implementation) {
                    case AccelerometerMma8452qImpl.IMPLEMENTATION:
                        acc= new AccelerometerMma8452qImpl(this);
                        persist.modules.put(Accelerometer.class, acc);
                        persist.modules.put(AccelerometerMma8452q.class, acc);
                        break;
                    case AccelerometerBmi160Impl.IMPLEMENTATION:
                        acc= new AccelerometerBmi160Impl(this);
                        persist.modules.put(Accelerometer.class, acc);
                        persist.modules.put(AccelerometerBosch.class, acc);
                        persist.modules.put(AccelerometerBmi160.class, acc);
                        break;
                    case AccelerometerBma255Impl.IMPLEMENTATION:
                        acc= new AccelerometerBma255Impl(this);
                        persist.modules.put(Accelerometer.class, acc);
                        persist.modules.put(AccelerometerBosch.class, acc);
                        persist.modules.put(AccelerometerBma255.class, acc);
                        break;
                }
                break;
            case TEMPERATURE:
                persist.modules.put(Temperature.class, new TemperatureImpl(this));
                break;
            case GPIO:
                persist.modules.put(Gpio.class, new GpioImpl(this));
                break;
            case NEO_PIXEL:
                persist.modules.put(NeoPixel.class, new NeoPixelImpl(this));
                break;
            case IBEACON:
                persist.modules.put(IBeacon.class, new IBeaconImpl(this));
                break;
            case HAPTIC:
                persist.modules.put(Haptic.class, new HapticImpl(this));
                break;
            case DATA_PROCESSOR:
                dataprocessor= new DataProcessorImpl(this);
                persist.modules.put(DataProcessor.class, dataprocessor);
                break;
            case EVENT:
                event = new EventImpl(this);
                persist.modules.put(EventImpl.class, event);
                break;
            case LOGGING:
                logger= new LoggingImpl(this);
                persist.modules.put(Logging.class, logger);
                break;
            case TIMER:
                mwTimer= new TimerImpl(this);
                persist.modules.put(Timer.class, mwTimer);
                break;
            case SERIAL_PASSTHROUGH:
                persist.modules.put(SerialPassthrough.class, new SerialPassthroughImpl(this));
                break;
            case MACRO:
                macro = new MacroImpl(this);
                persist.modules.put(Macro.class, macro);
                break;
            case GSR:
                persist.modules.put(Gsr.class, new GsrImpl(this));
                break;
            case SETTINGS:
                persist.modules.put(Settings.class, new SettingsImpl(this));
                break;
            case BAROMETER:
                BarometerBosch baro;
                switch(info.implementation) {
                    case BarometerBmp280Impl.IMPLEMENTATION:
                        baro= new BarometerBmp280Impl(this);
                        persist.modules.put(BarometerBosch.class, baro);
                        persist.modules.put(BarometerBmp280.class, baro);
                        break;
                    case BarometerBme280Impl.IMPLEMENTATION:
                        baro= new BarometerBme280Impl(this);
                        persist.modules.put(BarometerBosch.class, baro);
                        persist.modules.put(BarometerBme280.class, baro);
                        break;
                }
                break;
            case GYRO:
                persist.modules.put(GyroBmi160.class, new GyroBmi160Impl(this));
                break;
            case AMBIENT_LIGHT:
                persist.modules.put(AmbientLightLtr329.class, new AmbientLightLtr329Impl(this));
                break;
            case MAGNETOMETER:
                persist.modules.put(MagnetometerBmm150.class, new MagnetometerBmm150Impl(this));
                break;
            case HUMIDITY:
                persist.modules.put(HumidityBme280.class, new HumidityBme280Impl(this));
                break;
            case COLOR_DETECTOR:
                persist.modules.put(ColorDetectorTcs34725.class, new ColorDetectorTcs34725Impl(this));
                break;
            case PROXIMITY:
                persist.modules.put(ProximityTsl2671.class, new ProximityTsl2671Impl(this));
                break;
            case SENSOR_FUSION:
                persist.modules.put(SensorFusionBosch.class, new SensorFusionBoschImpl(this));
                break;
            case DEBUG:
                persist.modules.put(Debug.class, new DebugImpl(this));
                break;
        }
    }

    public void handleNotifyCharResponse(final byte[] response) {
        Pair<Byte, Byte> header= new Pair<>(response[0], response[1]);
        Tuple3<Byte, Byte, Byte> dataHandlerKey= new Tuple3<>(response[0], response[1], dataIdHeaders.contains(header) ? response[2] : DataTypeBase.NO_DATA_ID);

        if (dataHandlers.containsKey(dataHandlerKey)) {
            for(RegisterResponseHandler handler: dataHandlers.get(dataHandlerKey)) {
                handler.onResponseReceived(response);
            }
        } else if (registerResponseHandlers.containsKey(header)) {
            registerResponseHandlers.get(header).onResponseReceived(response);
        } else if (response[1] == READ_INFO_REGISTER) {
            ModuleInfo current= new ModuleInfo(response);
            persist.boardInfo.moduleInfo.put(ModuleId.lookupEnum(response[0]), current);

            instantiateModule(current);

            if (moduleQueries.isEmpty()) {
                logger.queryTime().continueWith(timeReadContinuation);
            } else {
                platform.writeGattCharacteristic(WRITE_WITHOUT_RESPONSE, MW_CMD_GATT_CHAR, new byte[] {moduleQueries.poll().id, READ_INFO_REGISTER});
            }
        }
    }

    public void handleUnexpectedDisconnect(int status) {
        if (unexpectedDcHandler != null) {
            unexpectedDcHandler.disconnected(status);
        }
    }

    @Override
    public void addResponseHandler(Pair<Byte, Byte> key, RegisterResponseHandler handler) {
        registerResponseHandlers.put(key, handler);
    }

    @Override
    public void addDataIdHeader(Pair<Byte, Byte> key) {
        dataIdHeaders.add(key);
    }

    @Override
    public void addDataHandler(Tuple3<Byte, Byte, Byte> key, RegisterResponseHandler handler) {
        if (!dataHandlers.containsKey(key)) {
            dataHandlers.put(key, new ArrayList<RegisterResponseHandler>());
        }
        dataHandlers.get(key).add(handler);
    }

    @Override
    public void removeDataHandler(Tuple3<Byte, Byte, Byte> key, RegisterResponseHandler handler) {
        if (dataHandlers.containsKey(key) && dataHandlers.get(key).contains(handler)) {
            dataHandlers.get(key).remove(handler);
        }
    }

    @Override
    public int numDataHandlers(Tuple3<Byte, Byte, Byte> key) {
        return dataHandlers.containsKey(key) ? dataHandlers.get(key).size() : 0;
    }

    @Override
    public void removeRoute(int id) {
        persist.activeRoutes.remove(id);
    }

    @Override
    public void removeProcessor(boolean sync, byte id) {
        dataprocessor.removeProcessor(sync, id);
    }

    @Override
    public void removeEventManager(int id) {
        persist.activeEventManagers.remove(id);
    }

    @Override
    public ScheduledFuture<?> scheduleTask(Runnable r, long delay) {
        return SCHEDULED_TASK_THREADPOOL.schedule(r, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public Task<Route> queueRouteBuilder(RouteBuilder builder, String producerTag) {
        TaskCompletionSource<Route> taskSrc= new TaskCompletionSource<>();
        if (persist.taggedProducers.containsKey(producerTag)) {
            pendingRoutes.add(new Tuple3<>(builder, new RouteElementImpl(persist.taggedProducers.get(producerTag)), taskSrc));
            routeTypes.add(RouteType.DATA);
            createRoute(false);
        } else {
            taskSrc.setError(new NullPointerException(String.format(Locale.US, "Producer tag \'%s\' does not exist", producerTag)));
        }
        return taskSrc.getTask();
    }

    @Override
    public Task<ScheduledTask> queueTaskManager(CodeBlock mwCode, byte[] timerConfig) {
        TaskCompletionSource<ScheduledTask> createManagerTask= new TaskCompletionSource<>();
        pendingTaskManagers.add(new Tuple3<>(createManagerTask, mwCode, timerConfig));
        routeTypes.add(RouteType.TIMER);
        createRoute(false);
        return createManagerTask.getTask();
    }

    @Override
    public Task<Observer> queueEvent(DataTypeBase owner, CodeBlock codeBlock) {
        TaskCompletionSource<Observer> createManagerTask= new TaskCompletionSource<>();
        pendingEventManagers.add(new Tuple3<>(createManagerTask, owner, codeBlock));
        routeTypes.add(RouteType.EVENT);
        createRoute(false);
        return createManagerTask.getTask();
    }

    private void createRoute(boolean ready) {
        if (!routeTypes.isEmpty() && (ready || routeTypes.size() == 1)) {
            switch(routeTypes.poll()) {
                case DATA: {
                    final Tuple3<RouteBuilder, ? extends RouteElementImpl, TaskCompletionSource<Route>> current= pendingRoutes.peek();
                    final LinkedList<Byte> createdProcessors= new LinkedList<>();
                    final LinkedList<DataLogger> createdLoggers= new LinkedList<>();
                    final Cache signalVars= new Cache(persist.boardInfo.firmware, this);
                    final HashSet<Integer> loggerIndices= new HashSet<>();
                    Task<Queue<Byte>> queueProcessorTask;

                    try {
                        current.second.setup(signalVars);
                        current.first.configure(current.second);

                        for (Map.Entry<String, Processor> it : signalVars.taggedProcessors.entrySet()) {
                            if (persist.taggedProducers.containsKey(it.getKey())) {
                                throw new IllegalRouteOperationException(String.format("Duplicate processor key \'%s\' found", it.getKey()));
                            }
                            tagProducer(it.getKey(), it.getValue().editor.source);
                        }
                        queueProcessorTask= dataprocessor.queueDataProcessors(signalVars.dataProcessors);
                    } catch (IllegalRouteOperationException e) {
                        queueProcessorTask= Task.forError(e);
                    }

                    queueProcessorTask.onSuccessTask(new Continuation<Queue<Byte>, Task<Queue<DataLogger>>>() {
                        @Override
                        public Task<Queue<DataLogger>> then(Task<Queue<Byte>> task) throws Exception {
                            createdProcessors.addAll(task.getResult());
                            dataprocessor.assignNameToId(signalVars.taggedProcessors);

                            int i= 0;
                            Queue<DataTypeBase> producersToLog= new LinkedList<>();
                            for(Tuple3<DataTypeBase, Subscriber, Boolean> it: signalVars.subscribedProducers) {
                                if (it.third) {
                                    producersToLog.add(it.first);
                                    loggerIndices.add(i);
                                }
                                i++;
                            }
                            return logger.queueLoggers(producersToLog);
                        }
                    }).onSuccessTask(new Continuation<Queue<DataLogger>, Task<LinkedList<Byte>>>() {
                        @Override
                        public Task<LinkedList<Byte>> then(Task<Queue<DataLogger>> task) throws Exception {
                            createdLoggers.addAll(task.getResult());

                            final Queue<Pair<? extends DataTypeBase, ? extends CodeBlock>> eventCodeBlocks = new LinkedList<>();
                            for(final Pair<String, Tuple3<DataTypeBase, Integer, byte[]>> it: signalVars.feedback) {
                                final DataTypeBase feedbackSource = persist.taggedProducers.get(it.first);
                                eventCodeBlocks.add(new Pair<>(feedbackSource, new CodeBlock() {
                                    @Override
                                    public void program() {
                                        sendCommand(it.second.second, it.second.first, DATA_PROCESSOR, DataProcessorImpl.PARAMETER, it.second.first.eventConfig[2], it.second.third);
                                    }
                                }));
                            }
                            for(final Pair<? extends DataTypeBase, ? extends Action> it: signalVars.reactions) {
                                final DataTypeBase source= it.first;
                                eventCodeBlocks.add(new Pair<>(source, new CodeBlock() {
                                    @Override
                                    public void program() {
                                        it.second.execute(source);
                                    }
                                }));
                            }
                            return event.queueEvents(eventCodeBlocks);
                        }
                    }).continueWith(new Continuation<LinkedList<Byte>, Void>() {
                        @Override
                        public Void then(Task<LinkedList<Byte>> task) throws Exception {
                            pendingRoutes.poll();

                            if (task.isFaulted()) {
                                for(DataLogger it: createdLoggers) {
                                    logger.removeDataLogger(true, it);
                                }
                                for(byte it: createdProcessors) {
                                    dataprocessor.removeProcessor(true, it);
                                }
                                for(String it: signalVars.taggedProcessors.keySet()) {
                                    persist.taggedProducers.remove(it);
                                }
                                current.third.setError(task.getError());
                            } else {
                                HashSet<String> processorNames = new HashSet<>();
                                processorNames.addAll(signalVars.taggedProcessors.keySet());

                                int i= 0;
                                ArrayList<DeviceDataConsumer> consumers= new ArrayList<>();
                                for (Tuple3<DataTypeBase, Subscriber, Boolean> it : signalVars.subscribedProducers) {
                                    if (loggerIndices.contains(i)) {
                                        createdLoggers.peek().subscriber= it.second;
                                        consumers.add(createdLoggers.poll());
                                    } else {
                                        DeviceDataConsumer newConsumer= new StreamedDataConsumer(it.first, it.second);
                                        consumers.add(newConsumer);
                                        newConsumer.enableStream(MetaWearBoardImpl.this);
                                    }
                                    i++;
                                }

                                RouteInner newRoute = new RouteInner(task.getResult(), consumers, createdProcessors, processorNames, persist.routeIdCounter, MetaWearBoardImpl.this);
                                persist.activeRoutes.put(persist.routeIdCounter, newRoute);
                                persist.routeIdCounter++;
                                current.third.setResult(newRoute);
                            }
                            createRoute(true);

                            return null;
                        }
                    });
                    break;
                }
                case TIMER: {
                    final Tuple3<TaskCompletionSource<ScheduledTask>, CodeBlock, byte[]> current = pendingTaskManagers.peek();
                    final Capture<Byte> ScheduledTaskId= new Capture<>();

                    mwTimer.create(current.third).onSuccessTask(new Continuation<DataTypeBase, Task<LinkedList<Byte>>>() {
                        @Override
                        public Task<LinkedList<Byte>> then(Task<DataTypeBase> task) throws Exception {
                            ScheduledTaskId.set(task.getResult().eventConfig[2]);
                            final Queue<Pair<? extends DataTypeBase, ? extends CodeBlock>> eventCodeBlocks = new LinkedList<>();
                            eventCodeBlocks.add(new Pair<>(task.getResult(), current.second));
                            return event.queueEvents(eventCodeBlocks);
                        }
                    }).continueWith(new Continuation<LinkedList<Byte>, Void>() {
                        @Override
                        public Void then(Task<LinkedList<Byte>> task) throws Exception {
                            pendingTaskManagers.poll();

                            if (task.isFaulted()) {
                                current.first.setError(task.getError());
                            } else {
                                current.first.setResult(mwTimer.createTimedEventManager(ScheduledTaskId.get(), task.getResult()));
                            }
                            createRoute(true);
                            return null;
                        }
                    });
                    break;
                }
                case EVENT: {
                    final Tuple3<TaskCompletionSource<Observer>, DataTypeBase, CodeBlock> current = pendingEventManagers.peek();
                    final Queue<Pair<? extends DataTypeBase, ? extends CodeBlock>> eventCodeBlocks = new LinkedList<>();

                    eventCodeBlocks.add(new Pair<>(current.second, current.third));
                    event.queueEvents(eventCodeBlocks).continueWith(new Continuation<LinkedList<Byte>, Void>() {
                        @Override
                        public Void then(Task<LinkedList<Byte>> task) throws Exception {
                            pendingEventManagers.poll();

                            if (task.isFaulted()) {
                                current.first.setError(task.getError());
                            } else {
                                ObserverInner newManager= new ObserverInner(persist.routeIdCounter, task.getResult());
                                newManager.restoreTransientVar(MetaWearBoardImpl.this);
                                persist.activeEventManagers.put(persist.routeIdCounter, newManager);
                                persist.routeIdCounter++;

                                current.first.setResult(newManager);
                            }
                            createRoute(true);
                            return null;
                        }
                    });
                    break;
                }
            }
        }
    }
}
