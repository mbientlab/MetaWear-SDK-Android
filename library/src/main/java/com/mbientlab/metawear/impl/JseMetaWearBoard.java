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
import static com.mbientlab.metawear.impl.Constant.Module.MACRO;

import com.mbientlab.metawear.AnonymousRoute;
import com.mbientlab.metawear.CodeBlock;
import com.mbientlab.metawear.DataToken;
import com.mbientlab.metawear.DeviceInformation;
import com.mbientlab.metawear.IllegalFirmwareFile;
import com.mbientlab.metawear.IllegalRouteOperationException;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Model;
import com.mbientlab.metawear.Observer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.TaskTimeoutException;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent.Action;
import com.mbientlab.metawear.impl.DataProcessorImpl.Processor;
import com.mbientlab.metawear.impl.LoggingImpl.DataLogger;
import com.mbientlab.metawear.impl.RouteComponentImpl.Cache;
import com.mbientlab.metawear.impl.dfu.Image;
import com.mbientlab.metawear.impl.dfu.Info2;
import com.mbientlab.metawear.impl.platform.BatteryService;
import com.mbientlab.metawear.impl.platform.BtleGatt;
import com.mbientlab.metawear.impl.platform.BtleGatt.WriteType;
import com.mbientlab.metawear.impl.platform.BtleGattCharacteristic;
import com.mbientlab.metawear.impl.platform.DeviceInformationService;
import com.mbientlab.metawear.impl.platform.IO;
import com.mbientlab.metawear.impl.platform.TimedTask;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.AmbientLightLtr329;
import com.mbientlab.metawear.module.BarometerBme280;
import com.mbientlab.metawear.module.BarometerBmp280;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.ColorTcs34725;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.GyroBmi270;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.HumidityBme280;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Macro;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.ProximityTsl2671;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.SerialPassthrough;
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Timer;
import com.mbientlab.metawear.module.Timer.ScheduledTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import bolts.CancellationTokenSource;
import bolts.Capture;
import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * Platform agnostic implementation of the {@link MetaWearBoard} interface using only standard Java APIs.  Platform specific functionality
 * is abstracted with the {@link IO} and {@link BtleGatt} interfaces.
 * @author Eric Tsai
 */
public class JseMetaWearBoard implements MetaWearBoard {
    private static final BtleGattCharacteristic MW_CMD_GATT_CHAR= new BtleGattCharacteristic(
            METAWEAR_GATT_SERVICE,
            UUID.fromString("326A9001-85CB-9195-D9DD-464CFBBAE75A")
    ), MW_NOTIFY_CHAR = new BtleGattCharacteristic(
            METAWEAR_GATT_SERVICE,
            UUID.fromString("326A9006-85CB-9195-D9DD-464CFBBAE75A")
    );

    private static final long RELEASE_INFO_TTL = 1800000L;
    private static final byte READ_INFO_REGISTER= Util.setRead((byte) 0x0);
    private final static String DEFAULT_FIRMWARE_BUILD = "vanilla", LOG_TAG = "metawear", RELEASES_URL = "https://mbientlab.com/releases", INFO_JSON = "info2.json";
    private final static String BOARD_INFO= "com.mbientlab.metawear.impl.JseMetaWearBoard.BOARD_INFO",
            BOARD_STATE = "com.mbientlab.metawear.impl.JseMetaWearBoard.BOARD_STATE";

    interface RegisterResponseHandler {
        void onResponseReceived(byte[] response);
    }

    private enum RouteType {
        DATA,
        TIMER,
        EVENT
    }

    private static class BoardInfo implements Serializable {
        private static final long serialVersionUID = 4634576514040923829L;

        final HashMap<Constant.Module, ModuleInfo> moduleInfo= new HashMap<>();
        Version firmware= new Version(0, 0, 0);
        String modelNumber= null, hardwareRevision= null;
    }
    private static class PersistentData implements Serializable {
        private static final long serialVersionUID = -6736797000323634463L;

        int routeIdCounter;
        BoardInfo boardInfo;
        final HashMap<Integer, RouteInner> activeRoutes= new HashMap<>();
        final HashMap<Integer, ObserverInner> activeEventManagers= new HashMap<>();
        final HashMap<String, DataTypeBase> taggedProducers= new HashMap<>();
        final LinkedHashMap<Class<? extends Module>, Module> modules= new LinkedHashMap<>();

        PersistentData() {
            routeIdCounter= 0;
        }
    }

    // Persistent data
    private PersistentData persist= new PersistentData();

    // routes
    private final Queue<Tuple3<RouteBuilder, ? extends RouteComponentImpl, TaskCompletionSource<Route>>> pendingRoutes= new ConcurrentLinkedQueue<>();
    private final Queue<Tuple3<TaskCompletionSource<ScheduledTask>, CodeBlock, byte[]>> pendingTaskManagers= new ConcurrentLinkedQueue<>();
    private final Queue<Tuple3<TaskCompletionSource<Observer>, DataTypeBase, CodeBlock>> pendingEventManagers= new ConcurrentLinkedQueue<>();
    private final Queue<RouteType> routeTypes= new ConcurrentLinkedQueue<>();
    private LoggingImpl logger;
    private DataProcessorImpl dataprocessor;
    private TimerImpl mwTimer;
    private EventImpl event;
    private MacroImpl macro;

    private final HashSet<Pair<Byte, Byte>> dataIdHeaders= new HashSet<>();
    private final Map<Tuple3<Byte, Byte, Byte>, LinkedHashSet<RegisterResponseHandler>> dataHandlers= new HashMap<>();
    private final Map<Pair<Byte, Byte>, RegisterResponseHandler> registerResponseHandlers= new HashMap<>();

    private final String macAddress, libVersion;
    private final IO io;
    private final BtleGatt gatt;

    // module discovery
    private TimedTask<byte[]> readModuleInfoTask;

    // Device Information
    private String serialNumber, manufacturer;

    // Connection
    private CancellationTokenSource connectCts = null;
    private UnexpectedDisconnectHandler unexpectedDcHandler;
    private Task<Void> connectTask = null;
    private boolean connected;

    private final MetaWearBoardPrivate mwPrivate = new MetaWearBoardPrivate() {
        @Override
        public Task<Void> boardDisconnect() {
            return gatt.remoteDisconnectAsync();
        }

        @Override
        public void sendCommand(byte[] command) {
            if (event.activeDataType != null) {
                event.convertToEventCommand(command);
            } else {
                gatt.writeCharacteristicAsync(
                        MW_CMD_GATT_CHAR,
                        command[0] == MACRO.id ? WriteType.DEFAULT : WriteType.WITHOUT_RESPONSE,
                        command
                );

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
        public void sendCommand(Constant.Module module, byte register, byte... parameters) {
            byte[] command= new byte[parameters.length + 2];
            System.arraycopy(parameters, 0, command, 2, parameters.length);
            command[0]= module.id;
            command[1]= register;

            sendCommand(command);
        }

        @Override
        public void sendCommand(Constant.Module module, byte register, byte id, byte... parameters) {
            byte[] command= new byte[parameters.length + 3];
            System.arraycopy(parameters, 0, command, 3, parameters.length);
            command[0]= module.id;
            command[1]= register;
            command[2]= id;

            sendCommand(command);
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
        public ModuleInfo lookupModuleInfo(Constant.Module id) {
            return persist.boardInfo.moduleInfo.get(id);
        }

        @Override
        public Collection<DataTypeBase> getDataTypes() {
            return persist.taggedProducers.values();
        }

        @Override
        public Map<Class<? extends Module>, Module> getModules() {
            return Collections.unmodifiableMap(persist.modules);
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
                dataHandlers.put(key, new LinkedHashSet<>());
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
        public Task<Route> queueRouteBuilder(RouteBuilder builder, String producerTag) {
            TaskCompletionSource<Route> taskSrc= new TaskCompletionSource<>();
            if (persist.taggedProducers.containsKey(producerTag)) {
                pendingRoutes.add(new Tuple3<>(builder, new RouteComponentImpl(persist.taggedProducers.get(producerTag)), taskSrc));
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

        @Override
        public void logWarn(String message) {
            io.logWarn(LOG_TAG, message);
        }

        @Override
        public Version getFirmwareVersion() {
            return persist.boardInfo.firmware;
        }
    };

    /**
     * Constructs a JseMetaWearBoard object
     * @param gatt          Object for handing Bluetooth LE GATT operations
     * @param io            Object for handling IO operations
     * @param macAddress    Device's MAC address
     */
    public JseMetaWearBoard(BtleGatt gatt, IO io, String macAddress, String libVersion) {
        this.io = io;
        this.gatt = gatt;
        this.macAddress = macAddress;
        this.libVersion = libVersion;

        readModuleInfoTask = new TimedTask<>();
        gatt.onDisconnect(new BtleGatt.DisconnectHandler() {
            @Override
            public void onDisconnect() {
                connected = false;
                for(Module it: persist.modules.values()) {
                    ((ModuleImplBase) it).disconnected();
                }
            }

            @Override
            public void onUnexpectedDisconnect(int status) {
                onDisconnect();

                if (unexpectedDcHandler != null) {
                    unexpectedDcHandler.disconnected(status);
                }
            }
        });
    }

    public String getFirmware() {
        return persist.boardInfo.firmware.toString();
    }

    public String getModelNumber() {
        return persist.boardInfo.modelNumber;
    }
    
    @Override
    public Model getModel() {
        if (persist.boardInfo.modelNumber == null) {
            return null;
        }

        boolean hasModuleInfo = !(inMetaBootMode() || persist.boardInfo.moduleInfo.isEmpty());
        switch(persist.boardInfo.modelNumber) {
            case "0":
                return Model.METAWEAR_R;
            case "1":
                if (hasModuleInfo) {
                    if (persist.boardInfo.moduleInfo.get(Constant.Module.BAROMETER).present() && persist.boardInfo.moduleInfo.get(Constant.Module.AMBIENT_LIGHT).present()) {
                        return Model.METAWEAR_RPRO;
                    }
                    return Model.METAWEAR_RG;
                }
                return null;
            case "2":
                if (!hasModuleInfo) {
                    return null;
                }
                if (persist.boardInfo.moduleInfo.get(Constant.Module.MAGNETOMETER).present()) {
                    return Model.METAWEAR_CPRO;
                }
                switch(persist.boardInfo.moduleInfo.get(Constant.Module.ACCELEROMETER).implementation) {
                    case AccelerometerBmi270Impl.IMPLEMENTATION:
                        return Model.METAMOTION_S;
                    case AccelerometerBmi160Impl.IMPLEMENTATION:
                        return Model.METAWEAR_C;
                    case AccelerometerBma255Impl.IMPLEMENTATION:
                        if (persist.boardInfo.moduleInfo.get(Constant.Module.PROXIMITY).present()) {
                            return Model.METADETECT;
                        }
                        if (persist.boardInfo.moduleInfo.get(Constant.Module.HUMIDITY).present()) {
                            return Model.METAENV;
                        }
                        return null;
                    default:
                        return null;
                }
            case "3":
                return Model.METAHEALTH;
            case "4":
                return Model.METATRACKER;
            case "5":
                if (persist.boardInfo.moduleInfo.get(Constant.Module.AMBIENT_LIGHT).present()) {
                    return Model.METAMOTION_R;
                } else {
                    return Model.METAMOTION_RL;
                }
            case "6":
                return Model.METAMOTION_C;
            case "8":
                return Model.METAMOTION_S;
        }
        return null;
    }

    @Override
    public String getModelString() {
        Model model;

        if ((model = getModel()) != null) {
            switch (model) {
                case METAWEAR_R:
                    return "MetaWear R";
                case METAWEAR_RG:
                    return "MetaWear RG";
                case METAWEAR_RPRO:
                    return "MetaWear RPro";
                case METAWEAR_C:
                    return "MetaWear C";
                case METAWEAR_CPRO:
                    return "MetaWear CPro";
                case METAENV:
                    return "MetaEnvironment";
                case METADETECT:
                    return "MetaDetector";
                case METAHEALTH:
                    return "MetaHealth";
                case METATRACKER:
                    return "MetaTracker";
                case METAMOTION_R:
                    return "MetaMotion R";
                case METAMOTION_RL:
                    return "MetaMotion RL";
                case METAMOTION_C:
                    return "MetaMotion C";
                case METAMOTION_S:
                    return "MetaMotion S";
            }
            return null;
        } else {
            return null;
        }
    }

    @Override
    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public Task<Integer> readRssiAsync() {
        return gatt.readRssiAsync();
    }

    @Override
    public Task<Byte> readBatteryLevelAsync() {
        return gatt.readCharacteristicAsync(BatteryService.BATTERY_LEVEL).onSuccessTask(task -> Task.forResult(task.getResult()[0]));
    }

    @Override
    public Task<DeviceInformation> readDeviceInformationAsync() {
        if (serialNumber != null && manufacturer != null) {
            return Task.forResult(new DeviceInformation(manufacturer, persist.boardInfo.modelNumber, serialNumber,
                    persist.boardInfo.firmware.toString(), persist.boardInfo.hardwareRevision));
        }
        return gatt.readCharacteristicAsync(new BtleGattCharacteristic[] {
                DeviceInformationService.SERIAL_NUMBER,
                DeviceInformationService.MANUFACTURER_NAME
        }).onSuccessTask(task -> {
            serialNumber = new String(task.getResult()[0]);
            manufacturer = new String(task.getResult()[1]);

            return Task.forResult(new DeviceInformation(manufacturer, persist.boardInfo.modelNumber, serialNumber,
                    persist.boardInfo.firmware.toString(), persist.boardInfo.hardwareRevision));
        });
    }

    private String generateFileName(String build, Version version, String filename) {
        return String.format(Locale.US, "%s_%s_%s_%s_%s",
                persist.boardInfo.hardwareRevision,
                persist.boardInfo.modelNumber,
                build, version.toString(), filename
        );
    }

    private Task<File> downloadFirmwareFile(String build, Version version, String filename) {
        String dlUrl = String.format(Locale.US, "%s/metawear/%s/%s/%s/%s/%s",
                RELEASES_URL,
                persist.boardInfo.hardwareRevision,
                persist.boardInfo.modelNumber,
                build, version.toString(), filename
        );

        return io.downloadFileAsync(dlUrl, generateFileName(build, version, filename));
    }

    private Task<JSONObject> retrieveInfoJson() {
        File info1Json = io.findDownloadedFile(INFO_JSON);
        return (!info1Json.exists() || info1Json.lastModified() < Calendar.getInstance().getTimeInMillis() - RELEASE_INFO_TTL ?
                io.downloadFileAsync(String.format(Locale.US, "%s/metawear/%s", RELEASES_URL, INFO_JSON), INFO_JSON) :
                Task.forResult(info1Json)).onSuccessTask(task -> {
                    StringBuilder builder = new StringBuilder();
                    InputStream ins = new FileInputStream(task.getResult());

                    byte data[] = new byte[1024];
                    int count;
                    while ((count = ins.read(data)) != -1) {
                        builder.append(new String(data, 0, count));
                    }

                    return Task.forResult(new JSONObject(builder.toString()));
                });
    }

    private static final Version BOOTLOADER_CUTOFF = new Version("1.4.0"),
            OLD_BOOTLOADER = new Version("0.2.1"), NEW_BOOTLOADER = new Version("0.3.1");
    private List<Tuple3<String, Version, String>> traverseBlDeps(JSONObject bootloader, Version target, Version current) throws JSONException {
        JSONObject value = bootloader.getJSONObject(target.toString());
        Version required = new Version(value.getString("required-bootloader"));

        List<Tuple3<String, Version, String>> files = (required.compareTo(current) == 0) ?
                new ArrayList<>() : traverseBlDeps(bootloader, required, current);
        files.add(new Tuple3<>("bootloader", target, value.getString("filename")));
        return files;
    }

    @Override
    public Task<List<File>> downloadFirmwareUpdateFilesAsync(final String version) {
        if (persist.boardInfo.hardwareRevision == null) {
            return Task.forError(new IllegalStateException("Hardware revision unavailable"));
        }
        if (persist.boardInfo.modelNumber == null) {
            return Task.forError(new IllegalStateException("Model number unavailable"));
        }
        if (!connected) {
            return Task.forError(new IllegalStateException("Android device not connected to the board"));
        }

        return retrieveInfoJson().onSuccessTask(task -> {
            JSONObject models= task.getResult().getJSONObject(persist.boardInfo.hardwareRevision);
            JSONObject builds = models.getJSONObject(persist.boardInfo.modelNumber);

            ModuleInfo mInfo = persist.boardInfo.moduleInfo.get(Constant.Module.SETTINGS);
            String build = mInfo != null && mInfo.extra.length >= 2 && mInfo.extra[1] != 0 ? String.format(Locale.US, "%d", ((short) mInfo.extra[1] & 0xff)) : DEFAULT_FIRMWARE_BUILD;

            Pair<JSONObject, Version> result = findFirmwareAttrs(builds.getJSONObject(build), version);

            {
                String minLibVersion = result.first.getString("min-android-version");
                if (new Version(minLibVersion).compareTo(new Version(libVersion)) > 0) {
                    throw new UnsupportedOperationException(String.format(Locale.US, "You must use Android SDK >= v'%s' with firmware v'%s'", minLibVersion, result.second.toString()));
                }
            }

            Version reqBl = new Version(result.first.getString("required-bootloader"));
            final Version currBl = !inMetaBootMode() ? (persist.boardInfo.firmware.compareTo(BOOTLOADER_CUTOFF) < 0 ? OLD_BOOTLOADER : NEW_BOOTLOADER) : persist.boardInfo.firmware;
            if (currBl.compareTo(reqBl) > 0) {
                throw new IllegalFirmwareFile(String.format(Locale.US, "Cannot use firmware v'%s' with this board", result.second.toString()));
            }

            List<Tuple3<String, Version, String>> files = currBl.compareTo(reqBl) < 0 ?
                    traverseBlDeps(builds.getJSONObject("bootloader"), reqBl, currBl) :
                    new ArrayList<>();
            files.add(new Tuple3<>(build, result.second, result.first.getString("filename")));

            final List<File> dests = new ArrayList<>();
            Task<Void> task2 = Task.forResult(null);
            for(Tuple3<String, Version, String> info: files) {
                final String destName = generateFileName(info.first, info.second, info.third);
                final File dest = io.findDownloadedFile(destName);

                task2 = task2.onSuccessTask(ignored ->
                    !dest.exists() ? downloadFirmwareFile(info.first, info.second, info.third) : Task.forResult(dest)
                ).onSuccessTask(fileTask -> {
                    dests.add(fileTask.getResult());
                    return Task.forResult(null);
                });
            }

            return task2.onSuccessTask(ignored -> Task.forResult(dests));
        });
    }

    @Override
    public Task<List<File>> downloadFirmwareUpdateFilesAsync() {
        return downloadFirmwareUpdateFilesAsync(null);
    }

    @Override
    public Task<List<File>> downloadFirmwareUpdateFilesAsyncV2(final String version) {
        if (persist.boardInfo.hardwareRevision == null) {
            return Task.forError(new IllegalStateException("Hardware revision unavailable"));
        }
        if (persist.boardInfo.modelNumber == null) {
            return Task.forError(new IllegalStateException("Model number unavailable"));
        }
        if (!connected) {
            return Task.forError(new IllegalStateException("Android device not connected to the board"));
        }

        final Capture<Task<Void>> dcTask = new Capture<>();
        final Capture<Image> firmware = new Capture<>();
        final Capture<Info2> info = new Capture<>();
        final Capture<List<File>> files = new Capture<>(new ArrayList<>());
        return retrieveInfoJson().onSuccessTask(task -> {
            ModuleInfo mInfo = persist.boardInfo.moduleInfo.get(Constant.Module.SETTINGS);
            String build = mInfo != null && mInfo.extra.length >= 2 && mInfo.extra[1] != 0 ?
                    String.format(Locale.US, "%d", ((short) mInfo.extra[1] & 0xff)) :
                    DEFAULT_FIRMWARE_BUILD;

            info.set(new Info2(task.getResult(), libVersion));
            firmware.set(
                    version != null ?
                    info.get().findFirmwareImage(persist.boardInfo.hardwareRevision, persist.boardInfo.modelNumber, build, version) :
                    info.get().findFirmwareImage(persist.boardInfo.hardwareRevision, persist.boardInfo.modelNumber, build)
            );

            return downloadFirmwareFile(build, new Version(firmware.get().version), firmware.get().filename);
        }).onSuccessTask(task -> {
            files.get().add(task.getResult());
            return inMetaBootMode() ? Task.forResult(null) :
                    getModule(Debug.class).jumpToBootloaderAsync()
                        .onSuccessTask(ignored -> connectWithRetryAsync(3))
                        .onSuccessTask(ignored -> !inMetaBootMode() ?
                                Task.forError(new IllegalStateException("Board is still in MetaWear mode")) :
                                Task.forResult(null)
                        );
        }).onSuccessTask(ignored -> {
            if (!inMetaBootMode()) {
                return Task.forError(new IllegalStateException("Board is still in MetaWear mode"));
            }

            List<Image> bootloaders = info.get().findBootloaderImages(
                    persist.boardInfo.hardwareRevision,
                    persist.boardInfo.modelNumber,
                    persist.boardInfo.firmware.toString(),
                    firmware.get().requiredBootloader
            );
            dcTask.set(disconnectAsync());

            List<Task<File>> tasks = new ArrayList<>();
            for(Image it: bootloaders) {
                tasks.add(it.downloadAsync(io));
            }
            return Task.whenAllResult(tasks);
        }).onSuccessTask(task -> {
            files.get().addAll(0, task.getResult());
            return dcTask.get();
        }).onSuccessTask(ignored -> Task.forResult(files.get()));
    }

    @Override
    public Task<List<File>> downloadFirmwareUpdateFilesAsyncV2() {
        return downloadFirmwareUpdateFilesAsyncV2(null);
    }


    private Pair<JSONObject, Version> findFirmwareAttrs(JSONObject firmwareVersions, String version) throws JSONException, IllegalFirmwareFile {
        final JSONObject attrs;
        final Version target;
        if (version != null) {
            try {
                attrs = firmwareVersions.getJSONObject(version);
                target = new Version(version);
            } catch (JSONException ignored) {
                throw new IllegalFirmwareFile(String.format(Locale.US, "Firmware v'%s' does not exist for this board", version));
            }
        } else {
            Iterator<String> versionKeys = firmwareVersions.keys();
            TreeSet<Version> versions = new TreeSet<>();
            while (versionKeys.hasNext()) {
                versions.add(new Version(versionKeys.next()));
            }

            Iterator<Version> it = versions.descendingIterator();
            if (it.hasNext()) {
                target = it.next();
                attrs = firmwareVersions.getJSONObject(target.toString());
            } else {
                throw new IllegalStateException("No information available for this board");
            }
        }

        return new Pair<>(attrs, target);
    }
    @Override
    public Task<String> findLatestAvailableFirmwareAsync() {
        if (persist.boardInfo.hardwareRevision == null) {
            return Task.forError(new IllegalStateException("Hardware revision unavailable"));
        }
        if (persist.boardInfo.modelNumber == null) {
            return Task.forError(new IllegalStateException("Model number unavailable"));
        }

        if (!connected) {
            return Task.forError(new IllegalStateException("Android device not connected to the board"));
        }
        if (inMetaBootMode()) {
            return Task.forError(new IllegalStateException("Cannot determine if newer firmware is available for MetaBoot boards"));
        }
        return retrieveInfoJson().onSuccessTask(task -> {
            JSONObject models= task.getResult().getJSONObject(persist.boardInfo.hardwareRevision);
            JSONObject builds = models.getJSONObject(persist.boardInfo.modelNumber);

            ModuleInfo info = persist.boardInfo.moduleInfo.get(Constant.Module.SETTINGS);
            String build = info != null && info.extra.length >= 2 && info.extra[1] != 0 ? String.format(Locale.US, "%d", info.extra[1]) : DEFAULT_FIRMWARE_BUILD;

            Pair<JSONObject, Version> result = findFirmwareAttrs(builds.getJSONObject(build), null);
            return Task.forResult(result.second.compareTo(persist.boardInfo.firmware) > 0 ? result.second.toString() : null);
        });
    }

    @Override
    public Task<File> downloadFirmwareAsync(final String version) {
        return downloadFirmwareUpdateFilesAsync(version).onSuccessTask(task -> {
            if (task.getResult().size() > 1) {
                throw new UnsupportedOperationException(String.format(Locale.US, "Updating to firmware v'%s' requires multiple files, use 'downloadFirmwareUpdateFilesAsync' instead", version));
            } else {
                return Task.forResult(task.getResult().get(0));
            }
        });
    }

    @Override
    public Task<File> downloadLatestFirmwareAsync() {
        return downloadFirmwareAsync(null);
    }

    @Override
    public Task<Boolean> checkForFirmwareUpdateAsync() {
        return findLatestAvailableFirmwareAsync().onSuccessTask(task -> Task.forResult(task.getResult() != null));
    }

    public void loadBoardAttributes() throws IOException, ClassNotFoundException {
        if (persist.boardInfo == null) {
            InputStream ins = io.localRetrieve(BOARD_INFO);
            if (ins != null) {
//                try( BufferedReader br =
//                             new BufferedReader( new InputStreamReader(ins, "UTF-8" )))
//                {
//                    StringBuilder sb = new StringBuilder();
//                    String line;
//                    while(( line = br.readLine()) != null ) {
//                        sb.append( line );
//                        sb.append( '\n' );
//                    }
//                    System.out.println(sb.toString());
//                }

                ObjectInputStream ois = new ObjectInputStream(ins);
                BoardInfo boardInfoState = (BoardInfo) ois.readObject();

                if (boardInfoState != null) {
                    persist.boardInfo = boardInfoState;
                    for (ModuleInfo it : boardInfoState.moduleInfo.values()) {
                        instantiateModule(it);
                    }
                } else {
                    persist.boardInfo = new BoardInfo();
                }
            } else {
                persist.boardInfo = new BoardInfo();
            }
        }
    }

    private Task<Queue<ModuleInfo>> discoverModules(Collection<Constant.Module> ignore) {
        final Queue<ModuleInfo> info = new LinkedList<>();
        final Queue<Constant.Module> modules = new LinkedList<>();
        final Capture<Boolean> terminate = new Capture<>(false);

        for(Constant.Module it: Constant.Module.values()) {
            if (!ignore.contains(it)) {
                modules.add(it);
            }
        }

        return Task.forResult(null).continueWhile(() -> !terminate.get() && !modules.isEmpty(), ignored -> {
            final Constant.Module next = modules.poll();
            return readModuleInfoTask.execute("Did not receive info for module (" + next.friendlyName + ") within %dms", Constant.RESPONSE_TIMEOUT,
                    () -> gatt.writeCharacteristicAsync(MW_CMD_GATT_CHAR, WriteType.WITHOUT_RESPONSE, new byte[] { next.id, READ_INFO_REGISTER })
            ).continueWithTask(task -> {
                if (task.isFaulted()) {
                    terminate.set(true);
                    return Task.<Void>forError(task.getError());
                } else {
                    info.add(new ModuleInfo(task.getResult()));
                    return Task.<Void>forResult(null);
                }
            });
        }).continueWithTask(task -> task.isFaulted() ? Task.forError(new TaskTimeoutException(task.getError(), info)) : Task.forResult(info));
    }

    @Override
    public Task<Void> connectAsync() {
        if (connectTask != null && !connectTask.isCompleted()) {
            return connectTask;
        }

        connectCts = new CancellationTokenSource();
        final Capture<Boolean> serviceDiscoveryRefresh = new Capture<>();

        connectTask = gatt.connectAsync().onSuccessTask(task -> {
            if (connectCts.isCancellationRequested()) {
                return Task.cancelled();
            }

            loadBoardAttributes();

            return gatt.readCharacteristicAsync(DeviceInformationService.FIRMWARE_REVISION);
        }).onSuccessTask(task -> {
            if (connectCts.isCancellationRequested()) {
                return Task.cancelled();
            }

            Version readFirmware = new Version(new String(task.getResult()));
            if (persist.boardInfo.firmware.compareTo(readFirmware) != 0) {
                persist.boardInfo.firmware = readFirmware;
                serviceDiscoveryRefresh.set(true);
            } else {
                serviceDiscoveryRefresh.set(false);
            }

            if (persist.boardInfo.modelNumber == null || persist.boardInfo.hardwareRevision == null) {
                return gatt.readCharacteristicAsync(new BtleGattCharacteristic[]{
                        DeviceInformationService.MODEL_NUMBER,
                        DeviceInformationService.HARDWARE_REVISION
                });
            }
            return Task.forResult(null);
        }).onSuccessTask(task -> {
            if (connectCts.isCancellationRequested()) {
                return Task.cancelled();
            }

            if (task.getResult() != null) {
                persist.boardInfo.modelNumber = new String(task.getResult()[0]);
                persist.boardInfo.hardwareRevision = new String(task.getResult()[1]);
            }

            return gatt.enableNotificationsAsync(MW_NOTIFY_CHAR, value -> {
                Pair<Byte, Byte> header= new Pair<>(value[0], value[1]);
                Tuple3<Byte, Byte, Byte> dataHandlerKey= new Tuple3<>(value[0], value[1], dataIdHeaders.contains(header) ? value[2] : DataTypeBase.NO_DATA_ID);

                if (dataHandlers.containsKey(dataHandlerKey)) {
                    for(RegisterResponseHandler handler: dataHandlers.get(dataHandlerKey)) {
                        handler.onResponseReceived(value);
                    }
                } else if (registerResponseHandlers.containsKey(header)) {
                    registerResponseHandlers.get(header).onResponseReceived(value);
                } else if (value[1] == READ_INFO_REGISTER) {
                    readModuleInfoTask.setResult(value);
                }
            });
        }).onSuccessTask(task -> {
            if (connectCts.isCancellationRequested()) {
                return Task.cancelled();
            }

            Collection<Constant.Module> ignore = new HashSet<>();
            if (serviceDiscoveryRefresh.get()) {
                persist.routeIdCounter= 0;
                persist.taggedProducers.clear();
                persist.activeEventManagers.clear();
                persist.activeRoutes.clear();
                persist.boardInfo.moduleInfo.clear();
                persist.modules.clear();
            }
            ignore.addAll(persist.boardInfo.moduleInfo.keySet());

            return discoverModules(ignore);
        }).onSuccessTask(task -> {
            if (connectCts.isCancellationRequested()) {
                return Task.cancelled();
            }

            for(ModuleInfo it: task.getResult()) {
                persist.boardInfo.moduleInfo.put(Constant.Module.lookupEnum(it.id), it);
                instantiateModule(it);
            }

            return logger == null ? Task.forResult(null) : logger.queryTime();
        }).continueWithTask(task -> {
            if (task.isCancelled()) {
                return task;
            }

            if (task.isFaulted()) {
                if (inMetaBootMode()) {
                    connected = true;
                    return Task.forResult(null);
                } else {
                    if (task.getError() instanceof TaskTimeoutException) {
                        Queue<ModuleInfo> partial = (Queue<ModuleInfo>) ((TaskTimeoutException) task.getError()).partial;
                        for(ModuleInfo it: partial) {
                            persist.boardInfo.moduleInfo.put(Constant.Module.lookupEnum(it.id), it);
                            instantiateModule(it);
                        }
                    }

                    return gatt.localDisconnectAsync().continueWithTask(ignored ->
                            task.getError() instanceof TaskTimeoutException ? Task.forError((Exception) task.getError().getCause()) : task
                    );
                }
            }

            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
                ObjectOutputStream oos = new ObjectOutputStream(buffer);
                oos.writeObject(persist.boardInfo);
                oos.close();

                io.localSave(BOARD_INFO, buffer.toByteArray());
            } catch (IOException e) {
                io.logWarn(LOG_TAG, "Cannot serialize MetaWear module info", e);
            } finally {
                connected= true;
            }

            return Task.forResult(null);
        });

        return connectTask;
    }

    @Override
    public Task<Void> connectWithRetryAsync(int retries) {
        final Capture<Integer> remaining = new Capture<>(retries);
        final Capture<Task<Void>> lastResult = new Capture<>();
        return Task.forResult(null).continueWhile(() -> remaining.get() > 0, ignored ->
                connectAsync().continueWithTask(task -> {
                    lastResult.set(task);
                    remaining.set(task.isFaulted() || task.isCancelled() ? remaining.get() - 1 : -1);
                    return Task.forResult(null);
                })
        ).continueWithTask(ignored -> lastResult.get());
    }

    @Override
    public Task<Void> connectAsync(long delay) {
        return Task.delay(delay).continueWithTask(task -> connectAsync());
    }

    @Override
    public Task<Void> disconnectAsync() {
        connectCts.cancel();

        return gatt.localDisconnectAsync();
    }

    @Override
    public void onUnexpectedDisconnect(UnexpectedDisconnectHandler handler) {
        unexpectedDcHandler = handler;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean inMetaBootMode() {
        return gatt.serviceExists(METABOOT_SERVICE);
    }

    @Override
    public <T extends Module> T getModule(Class<T> moduleClass) {
        if (inMetaBootMode()) {
            return null;
        }
        return persist.modules.containsKey(moduleClass) ? moduleClass.cast(persist.modules.get(moduleClass)) : null;
    }

    @Override
    public <T extends Module> T getModuleOrThrow(Class<T> moduleClass) throws UnsupportedModuleException {
        if (inMetaBootMode()) {
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
    public void serialize() throws IOException {
        ByteArrayOutputStream buffer= new ByteArrayOutputStream(1024);
        serialize(buffer);
        buffer.close();
        io.localSave(BOARD_STATE, buffer.toByteArray());
    }

    @Override
    public void serialize(OutputStream outs) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(outs);
        oos.writeObject(persist);
        oos.flush();
    }

    private void resetVars() {
        dataIdHeaders.clear();
        dataHandlers.clear();
    }

    private void deserializeInner(InputStream stateStream) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(stateStream);
        persist= (PersistentData) ois.readObject();

        if (persist != null) {
            resetVars();

            for (Module it : persist.modules.values()) {
                ((ModuleImplBase) it).restoreTransientVars(mwPrivate);
            }

            for (RouteInner it : persist.activeRoutes.values()) {
                it.restoreTransientVars(mwPrivate);
            }

            for (ObserverInner it : persist.activeEventManagers.values()) {
                it.restoreTransientVar(mwPrivate);
            }

            logger = (LoggingImpl) persist.modules.get(Logging.class);
            dataprocessor = (DataProcessorImpl) persist.modules.get(DataProcessor.class);
            mwTimer = (TimerImpl) persist.modules.get(Timer.class);
            event = (EventImpl) persist.modules.get(EventImpl.class);
            macro = (MacroImpl) persist.modules.get(Macro.class);
        }
    }

    @Override
    public void deserialize() throws IOException, ClassNotFoundException {
        deserializeInner(io.localRetrieve(BOARD_STATE));
    }

    @Override
    public void deserialize(InputStream ins) throws IOException, ClassNotFoundException {
        deserializeInner(ins);
    }

    @Override
    public Task<JSONObject> dumpModuleInfo(JSONObject partial) {
        final Map<String, JSONObject> diagnosticResult = new HashMap<>();
        Collection<Constant.Module> ignore = new HashSet<>();

        if (partial != null) {
            {
                Iterator<String> it = partial.keys();
                while (it.hasNext()) {
                    String current = it.next();
                    diagnosticResult.put(current, partial.optJSONObject(current));
                }
            }

            for(Constant.Module it: Constant.Module.values()) {
                if (partial.has(it.friendlyName)) {
                    ignore.add(it);
                }
            }
        }

        return discoverModules(ignore).continueWithTask(task -> {
            Queue<ModuleInfo> result = task.isFaulted() ?
                    (Queue<ModuleInfo>) ((TaskTimeoutException) task.getError()).partial :
                    task.getResult();

            while(!result.isEmpty()) {
                ModuleInfo current = result.poll();
                diagnosticResult.put(Constant.Module.lookupEnum(current.id).friendlyName, current.toJSON());
            }

            JSONObject actual = new JSONObject(diagnosticResult);
            return !task.isFaulted() ? Task.forResult(actual) : Task.forError(new TaskTimeoutException((Exception) task.getError().getCause(), actual));
        });
    }

    private static class AnonymousRouteInner implements AnonymousRoute {
        private final DeviceDataConsumer consumer;
        private final MetaWearBoardPrivate bridge;

        AnonymousRouteInner(DeviceDataConsumer consumer, MetaWearBoardPrivate bridge) {
            this.consumer = consumer;
            this.bridge = bridge;
        }

        @Override
        public String identifier() {
            return Util.createProducerChainString(consumer.source, bridge);
        }

        @Override
        public void subscribe(Subscriber subscriber) {
            consumer.subscriber = subscriber;
        }

        @Override
        public void setEnvironment(Object... env) {
            consumer.environment = env;
        }
    }
    @Override
    public Task<AnonymousRoute[]> createAnonymousRoutesAsync() {
        Accelerometer accelerometer = getModule(Accelerometer.class);
        final Gyro gyro = getModule(Gyro.class);
        final SensorFusionBosch sensorFusion = getModule(SensorFusionBosch.class);

        return (accelerometer == null ? Task.<Void>forResult(null) : accelerometer.pullConfigAsync())
                .onSuccessTask(task -> gyro == null ? Task.forResult(null) : gyro.pullConfigAsync())
                .onSuccessTask(task -> sensorFusion == null ? Task.forResult(null) : sensorFusion.pullConfigAsync())
                .onSuccessTask(task -> logger.queryActiveLoggersAsync())
                .onSuccessTask(task -> {
                    AnonymousRoute[] routes = new AnonymousRoute[task.getResult().size()];
                    int i = 0;
                    for(DataLogger it: task.getResult()) {
                        routes[i] = new AnonymousRouteInner(it, mwPrivate);
                        i++;
                    }
                    return Task.forResult(routes);
                });
    }

    private void sendCommand(int dest, DataToken input, Constant.Module module, byte register, byte id, byte... parameters) {
        byte[] command= new byte[parameters.length + 3];
        System.arraycopy(parameters, 0, command, 3, parameters.length);
        command[0]= module.id;
        command[1]= register;
        command[2]= id;

        mwPrivate.sendCommand(command, dest, input);
    }

    private void instantiateModule(ModuleInfo info) {
        if (!info.present()) {
            return;
        }

        switch(Constant.Module.lookupEnum(info.id)) {
            case SWITCH:
                persist.modules.put(Switch.class, new SwitchImpl(mwPrivate));
                break;
            case LED:
                persist.modules.put(Led.class, new LedImpl(mwPrivate));
                break;
            case ACCELEROMETER:
                Accelerometer acc;
                switch(info.implementation) {
                    case AccelerometerMma8452qImpl.IMPLEMENTATION:
                        acc= new AccelerometerMma8452qImpl(mwPrivate);
                        persist.modules.put(Accelerometer.class, acc);
                        persist.modules.put(AccelerometerMma8452q.class, acc);
                        break;
                    case AccelerometerBmi160Impl.IMPLEMENTATION:
                        acc= new AccelerometerBmi160Impl(mwPrivate);
                        persist.modules.put(Accelerometer.class, acc);
                        persist.modules.put(AccelerometerBosch.class, acc);
                        persist.modules.put(AccelerometerBmi160.class, acc);
                        break;
                    case AccelerometerBmi270Impl.IMPLEMENTATION:
                        acc= new AccelerometerBmi270Impl(mwPrivate);
                        persist.modules.put(Accelerometer.class, acc);
                        persist.modules.put(AccelerometerBosch.class, acc);
                        persist.modules.put(AccelerometerBmi160.class, acc);
                        break;
                    case AccelerometerBma255Impl.IMPLEMENTATION:
                        acc= new AccelerometerBma255Impl(mwPrivate);
                        persist.modules.put(Accelerometer.class, acc);
                        persist.modules.put(AccelerometerBosch.class, acc);
                        persist.modules.put(AccelerometerBma255.class, acc);
                        break;
                }
                break;
            case TEMPERATURE:
                persist.modules.put(Temperature.class, new TemperatureImpl(mwPrivate));
                break;
            case HAPTIC:
                persist.modules.put(Haptic.class, new HapticImpl(mwPrivate));
                break;
            case DATA_PROCESSOR:
                dataprocessor= new DataProcessorImpl(mwPrivate);
                persist.modules.put(DataProcessor.class, dataprocessor);
                break;
            case EVENT:
                event = new EventImpl(mwPrivate);
                persist.modules.put(EventImpl.class, event);
                break;
            case LOGGING:
                logger= new LoggingImpl(mwPrivate);
                persist.modules.put(Logging.class, logger);
                break;
            case TIMER:
                mwTimer= new TimerImpl(mwPrivate);
                persist.modules.put(Timer.class, mwTimer);
                break;
            case SERIAL_PASSTHROUGH:
                persist.modules.put(SerialPassthrough.class, new SerialPassthroughImpl(mwPrivate));
                break;
            case MACRO:
                macro = new MacroImpl(mwPrivate);
                persist.modules.put(Macro.class, macro);
                break;
            case SETTINGS:
                persist.modules.put(Settings.class, new SettingsImpl(mwPrivate));
                break;
            case BAROMETER:
                BarometerBosch baro;
                switch(info.implementation) {
                    case BarometerBmp280Impl.IMPLEMENTATION:
                        baro= new BarometerBmp280Impl(mwPrivate);
                        persist.modules.put(BarometerBosch.class, baro);
                        persist.modules.put(BarometerBmp280.class, baro);
                        break;
                    case BarometerBme280Impl.IMPLEMENTATION:
                        baro= new BarometerBme280Impl(mwPrivate);
                        persist.modules.put(BarometerBosch.class, baro);
                        persist.modules.put(BarometerBme280.class, baro);
                        break;
                }
                break;
            case GYRO:
                Gyro gyro;
                switch(info.implementation) {
                    case GyroBmi160Impl.IMPLEMENTATION:
                        gyro= new GyroBmi160Impl(mwPrivate);
                        persist.modules.put(Gyro.class, gyro);
                        persist.modules.put(GyroBmi160.class, gyro);
                        break;
                    case GyroBmi270Impl.IMPLEMENTATION:
                        gyro= new GyroBmi270Impl(mwPrivate);
                        persist.modules.put(Gyro.class, gyro);
                        persist.modules.put(GyroBmi270.class, gyro);
                        break;
                }
                break;
            case AMBIENT_LIGHT:
                persist.modules.put(AmbientLightLtr329.class, new AmbientLightLtr329Impl(mwPrivate));
                break;
            case MAGNETOMETER:
                persist.modules.put(MagnetometerBmm150.class, new MagnetometerBmm150Impl(mwPrivate));
                break;
            case HUMIDITY:
                persist.modules.put(HumidityBme280.class, new HumidityBme280Impl(mwPrivate));
                break;
            case COLOR_DETECTOR:
                persist.modules.put(ColorTcs34725.class, new ColorTcs34725Impl(mwPrivate));
                break;
            case PROXIMITY:
                persist.modules.put(ProximityTsl2671.class, new ProximityTsl2671Impl(mwPrivate));
                break;
            case SENSOR_FUSION:
                persist.modules.put(SensorFusionBosch.class, new SensorFusionBoschImpl(mwPrivate));
                break;
            case DEBUG:
                persist.modules.put(Debug.class, new DebugImpl(mwPrivate));
                break;
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

        private transient MetaWearBoardPrivate mwPrivate;

        RouteInner(LinkedList<Byte> eventCmdIds, ArrayList<DeviceDataConsumer> consumers, LinkedList<Byte> dataprocessors,
                   HashSet<String> processorNames, int id, MetaWearBoardPrivate mwPrivate) {
            this.eventCmdIds= eventCmdIds;
            this.consumers = consumers;
            this.dataprocessors= dataprocessors;
            this.processorNames = processorNames;
            this.id= id;
            this.active= true;
            this.mwPrivate = mwPrivate;
        }

        void restoreTransientVars(MetaWearBoardPrivate mwPrivate) {
            this.mwPrivate = mwPrivate;

            for(DeviceDataConsumer it: consumers) {
                it.addDataHandler(mwPrivate);
            }
        }

        @Override
        public String generateIdentifier(int pos) {
            try {
                return Util.createProducerChainString(consumers.get(pos).source, mwPrivate);
            } catch (IndexOutOfBoundsException ignored) {
                return null;
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
                consumers.get(pos).disableStream(mwPrivate);
                return true;
            } catch (IndexOutOfBoundsException ignored) {
                return false;
            }
        }

        @Override
        public boolean resubscribe(int pos) {
            try {
                consumers.get(pos).enableStream(mwPrivate);
                return true;
            } catch (IndexOutOfBoundsException ignored) {
                return false;
            }
        }

        @Override
        public boolean resubscribe(int pos, Subscriber subscriber) {
            try {
                consumers.get(pos).subscriber= subscriber;
                consumers.get(pos).enableStream(mwPrivate);
                return true;
            } catch (IndexOutOfBoundsException ignored) {
                return false;
            }
        }

        void remove(boolean sync) {
            if (active) {
                active = false;
                for(String it: processorNames) {
                    mwPrivate.removeProducerTag(it);
                }

                LoggingImpl logging= (LoggingImpl) mwPrivate.getModules().get(Logging.class);
                for(DeviceDataConsumer it: consumers) {
                    if (it instanceof DataLogger) {
                        logging.removeDataLogger(sync, (DataLogger) it);
                    } else {
                        it.disableStream(mwPrivate);
                    }
                }

                for(byte it: dataprocessors) {
                    mwPrivate.removeProcessor(sync, it);
                }
                dataprocessors.clear();

                if (sync) {
                    EventImpl event = (EventImpl) mwPrivate.getModules().get(EventImpl.class);
                    for(Byte it: eventCmdIds) {
                        event.removeEventCommand(it);
                    }

                    mwPrivate.removeRoute(id);
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

        private transient MetaWearBoardPrivate mwPrivate;

        private ObserverInner(int id, LinkedList<Byte> eventCmdIds) {
            this.eventCmdIds = eventCmdIds;
            this.id = id;
            active= true;
        }

        void restoreTransientVar(MetaWearBoardPrivate mwPrivate) {
            this.mwPrivate = mwPrivate;
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
                    EventImpl event = (EventImpl) mwPrivate.getModules().get(EventImpl.class);
                    for (Byte it : eventCmdIds) {
                        event.removeEventCommand(it);
                    }

                    mwPrivate.removeEventManager(id);
                }
            }
        }
    }
    private void createRoute(boolean ready) {
        if (!routeTypes.isEmpty() && (ready || routeTypes.size() == 1)) {
            switch(routeTypes.peek()) {
                case DATA: {
                    final Tuple3<RouteBuilder, ? extends RouteComponentImpl, TaskCompletionSource<Route>> current= pendingRoutes.peek();
                    final LinkedList<Byte> createdProcessors= new LinkedList<>();
                    final LinkedList<DataLogger> createdLoggers= new LinkedList<>();
                    final Cache signalVars= new Cache(mwPrivate);
                    final HashSet<Integer> loggerIndices= new HashSet<>();
                    Task<Queue<Byte>> queueProcessorTask;

                    try {
                        current.second.setup(signalVars);
                        current.first.configure(current.second);

                        for (Map.Entry<String, Processor> it : signalVars.taggedProcessors.entrySet()) {
                            if (persist.taggedProducers.containsKey(it.getKey())) {
                                throw new IllegalRouteOperationException(String.format("Duplicate processor key \'%s\' found", it.getKey()));
                            }
                            mwPrivate.tagProducer(it.getKey(), it.getValue().editor.source);
                        }
                        queueProcessorTask= dataprocessor.queueDataProcessors(signalVars.dataProcessors);
                    } catch (IllegalRouteOperationException e) {
                        queueProcessorTask= Task.forError(e);
                    }

                    queueProcessorTask.onSuccessTask(task -> {
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
                    }).onSuccessTask(task -> {
                        createdLoggers.addAll(task.getResult());

                        final Queue<Pair<? extends DataTypeBase, ? extends CodeBlock>> eventCodeBlocks = new LinkedList<>();
                        for(final Pair<String, Tuple3<DataTypeBase, Integer, byte[]>> it: signalVars.feedback) {
                            if (!persist.taggedProducers.containsKey(it.first)) {
                                throw new IllegalRouteOperationException("\'" + it.first + "\' is not associated with any data producer or name component");
                            }
                            final DataTypeBase feedbackSource = persist.taggedProducers.get(it.first);
                            eventCodeBlocks.add(new Pair<>(feedbackSource, () -> sendCommand(it.second.second, persist.taggedProducers.get(it.first), DATA_PROCESSOR, DataProcessorImpl.PARAMETER, it.second.first.eventConfig[2], it.second.third)));
                        }
                        for(final Pair<? extends DataTypeBase, ? extends Action> it: signalVars.reactions) {
                            final DataTypeBase source= it.first;
                            eventCodeBlocks.add(new Pair<>(source, () -> it.second.execute(source)));
                        }
                        return event.queueEvents(eventCodeBlocks);
                    }).continueWith(task -> {
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
                                    newConsumer.enableStream(mwPrivate);
                                }
                                i++;
                            }

                            RouteInner newRoute = new RouteInner(task.getResult(), consumers, createdProcessors, processorNames, persist.routeIdCounter, mwPrivate);
                            persist.activeRoutes.put(persist.routeIdCounter, newRoute);
                            persist.routeIdCounter++;
                            current.third.setResult(newRoute);
                        }

                        pendingRoutes.poll();
                        routeTypes.poll();
                        createRoute(true);

                        return null;
                    });
                    break;
                }
                case TIMER: {
                    final Tuple3<TaskCompletionSource<ScheduledTask>, CodeBlock, byte[]> current = pendingTaskManagers.peek();
                    final Capture<Byte> ScheduledTaskId= new Capture<>();

                    mwTimer.create(current.third).onSuccessTask(task -> {
                        ScheduledTaskId.set(task.getResult().eventConfig[2]);
                        final Queue<Pair<? extends DataTypeBase, ? extends CodeBlock>> eventCodeBlocks = new LinkedList<>();
                        eventCodeBlocks.add(new Pair<>(task.getResult(), current.second));
                        return event.queueEvents(eventCodeBlocks);
                    }).continueWith(task -> {
                        if (task.isFaulted()) {
                            current.first.setError(task.getError());
                        } else {
                            current.first.setResult(mwTimer.createTimedEventManager(ScheduledTaskId.get(), task.getResult()));
                        }

                        pendingTaskManagers.poll();
                        routeTypes.poll();
                        createRoute(true);

                        return null;
                    });
                    break;
                }
                case EVENT: {
                    final Tuple3<TaskCompletionSource<Observer>, DataTypeBase, CodeBlock> current = pendingEventManagers.peek();
                    final Queue<Pair<? extends DataTypeBase, ? extends CodeBlock>> eventCodeBlocks = new LinkedList<>();

                    eventCodeBlocks.add(new Pair<>(current.second, current.third));
                    event.queueEvents(eventCodeBlocks).continueWith(task -> {
                        if (task.isFaulted()) {
                            current.first.setError(task.getError());
                        } else {
                            ObserverInner newManager= new ObserverInner(persist.routeIdCounter, task.getResult());
                            newManager.restoreTransientVar(mwPrivate);
                            persist.activeEventManagers.put(persist.routeIdCounter, newManager);
                            persist.routeIdCounter++;

                            current.first.setResult(newManager);
                        }

                        pendingEventManagers.poll();
                        routeTypes.poll();
                        createRoute(true);

                        return null;
                    });
                    break;
                }
            }
        }
    }
}
