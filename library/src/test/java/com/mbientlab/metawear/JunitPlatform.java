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

package com.mbientlab.metawear;

import com.mbientlab.metawear.impl.platform.BtleGatt;
import com.mbientlab.metawear.impl.platform.BtleGattCharacteristic;
import com.mbientlab.metawear.impl.platform.DeviceInformationService;
import com.mbientlab.metawear.impl.platform.IO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * Created by etsai on 8/31/16.
 */
public class JunitPlatform implements IO, BtleGatt {
    public static final File RES_PATH = new File(new File("src", "test"), "res");
    private static final ScheduledExecutorService SCHEDULED_TASK_THREADPOOL = Executors.newSingleThreadScheduledExecutor();

    interface MwBridge {
        void disconnected();
        void sendMockResponse(byte[] response);
    }

    public int nConnects = 0, nDisconnects = 0;
    public MetaWearBoardInfo boardInfo= new MetaWearBoardInfo();
    public String firmware= "1.2.3", boardStateSuffix;
    public boolean delayModuleInfoResponse= false;
    public boolean deserializeModuleInfo= false;
    public final boolean serializeModuleInfo = false;
    public boolean enableMetaBootState = false;
    public boolean delayReadDevInfo = false;
    private final Map<Byte, byte[]> customModuleInfo= new HashMap<>();
    private final Map<Integer, byte[]> customResponses = new HashMap<>();

    public byte maxProcessors= 28, maxLoggers= 8, maxTimers= 8, maxEvents= 28, maxModule = -1;
    public byte timerId= 0, eventId= 0, loggerId= 0, dataProcessorId= 0, macroId = 0;
    private final MwBridge bridge;
    final ArrayList<byte[]> commandHistory= new ArrayList<>(), connectCmds= new ArrayList<>();
    private final ArrayList<BtleGattCharacteristic> gattCharReadHistory = new ArrayList<>();
    NotificationListener notificationListener;
    DisconnectHandler dcHandler;

    public JunitPlatform(MwBridge bridge) {
        this.bridge= bridge;
    }

    public void addCustomModuleInfo(byte[] info) {
        customModuleInfo.put(info[0], info);
    }
    public void removeCustomModuleInfo(byte id) {
        customModuleInfo.remove(id);
    }
    public void addCustomResponse(byte[] command, byte[] response) {
        customResponses.put(Arrays.hashCode(command), response);
    }

    private void scheduleMockResponse(final byte[] response) {
        SCHEDULED_TASK_THREADPOOL.schedule(() -> bridge.sendMockResponse(response), 20, TimeUnit.MILLISECONDS);
    }

    public void scheduleTask(Runnable r, long timeout) {
        SCHEDULED_TASK_THREADPOOL.schedule(r, timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void localSave(String key, byte[] data) throws IOException {
        String prefix = key.substring(key.lastIndexOf(".") + 1).toLowerCase();
        if (!prefix.equals("board_info") || serializeModuleInfo) {
            FileOutputStream fos = new FileOutputStream(String.format(Locale.US, "build/%s_%s", prefix, boardStateSuffix));
            fos.write(data);
            fos.close();
        }
    }

    @Override
    public InputStream localRetrieve(String key) throws IOException {
        String prefix = key.substring(key.lastIndexOf(".") + 1).toLowerCase();
        if (prefix.equals("board_info") && deserializeModuleInfo) {
            return new FileInputStream(new File(RES_PATH, "board_module_info"));
        }
        return boardStateSuffix != null ?
                new FileInputStream(new File(RES_PATH, String.format(Locale.US, "board_state_%s", boardStateSuffix))) :
                null;
    }

    @Override
    public Task<Void> writeCharacteristicAsync(BtleGattCharacteristic gattCharr, WriteType writeType, byte[] value) {
        if (!customResponses.isEmpty()) {
            for (int i = 2; i < Math.min(3, value.length) + 1; i++) {
                byte[] prefix = new byte[i];
                System.arraycopy(value, 0, prefix, 0, prefix.length);

                int hash = Arrays.hashCode(prefix);
                if (customResponses.containsKey(hash)) {
                    commandHistory.add(value);
                    scheduleMockResponse(customResponses.get(hash));
                    return Task.forResult(null);
                }
            }
        }

        if (value[1] == (byte) 0x80) {
            connectCmds.add(value);

            if (maxModule == -1 || value[0] <= maxModule) {
                byte[] response = customModuleInfo.containsKey(value[0]) ?
                        customModuleInfo.get(value[0]) :
                        boardInfo.moduleResponses.get(value[0]);

                if (delayModuleInfoResponse) {
                    scheduleMockResponse(response);
                } else {
                    bridge.sendMockResponse(response);
                }
            }
        } else if (value[0] == (byte) 0xb && value[1] == (byte) 0x84) {
            connectCmds.add(value);
            scheduleMockResponse(new byte[] {0x0b, (byte) 0x84, 0x15, 0x04, 0x00, 0x00, 0x05});
        } else {
            commandHistory.add(value);

            if (eventId < maxEvents && value[0] == 0xa && value[1] == 0x3) {
                byte[] response= {value[0], 0x2, eventId};
                eventId++;
                scheduleMockResponse(response);
            } else if (timerId < maxTimers && value[0] == 0xc && value[1] == 0x2) {
                byte[] response= {value[0], 0x2, timerId};
                timerId++;
                scheduleMockResponse(response);
            } else if (loggerId < maxLoggers && value[0] == 0xb && value[1] == 0x2) {
                byte[] response= {value[0], 0x2, loggerId};
                loggerId++;
                scheduleMockResponse(response);
            } else if (dataProcessorId < maxProcessors && value[0] == 0x9 && value[1] == 0x2) {
                byte[] response = {value[0], 0x2, dataProcessorId};
                dataProcessorId++;
                scheduleMockResponse(response);
            } else if (value[0] == 0xf && value[1] == 0x2) {
                byte[] response = {value[0], 0x2, macroId};
                macroId++;
                scheduleMockResponse(response);
            } else if (value[0] == (byte) 0xb && value[1] == (byte) 0x85) {
                bridge.sendMockResponse(new byte[] {0x0b, (byte) 0x85, (byte) 0x9e, 0x01, 0x00, 0x00});
            }
        }

        return Task.forResult(null);
    }

    @Override
    public Task<byte[]> readCharacteristicAsync(BtleGattCharacteristic gattChar) {
        gattCharReadHistory.add(gattChar);
        if (gattChar.equals(DeviceInformationService.FIRMWARE_REVISION)) {
            return Task.delay(20L).continueWithTask(task -> Task.forResult(firmware.getBytes()));
        } else if (gattChar.equals(DeviceInformationService.HARDWARE_REVISION)) {
            return Task.delay(20L).continueWithTask(task -> Task.forResult(boardInfo.hardwareRevision));
        } else if (gattChar.equals(DeviceInformationService.MODEL_NUMBER)) {
            return Task.delay(20L).continueWithTask(task -> Task.forResult(boardInfo.modelNumber));
        } else if (gattChar.equals(DeviceInformationService.MANUFACTURER_NAME)) {
            return Task.delay(20L).continueWithTask(task -> delayReadDevInfo ? Task.forError(new TimeoutException("Reading gatt characteristic timed out")) : Task.forResult(boardInfo.manufacturer));
        } else if (gattChar.equals(DeviceInformationService.SERIAL_NUMBER)) {
            return Task.delay(20L).continueWithTask(task -> delayReadDevInfo ? Task.forError(new TimeoutException("Reading gatt characteristic timed out")) : Task.forResult(boardInfo.serialNumber));
        }

        return Task.forResult(null);
    }

    @Override
    public Task<Void> enableNotificationsAsync(BtleGattCharacteristic characteristic, NotificationListener listener) {
        if (enableMetaBootState && !characteristic.serviceUuid.equals(MetaWearBoard.METABOOT_SERVICE)) {
            return Task.forError(new IllegalStateException("Service " + characteristic.serviceUuid.toString() + " does not exist"));
        }
        notificationListener = listener;
        return Task.forResult(null);
    }

    @Override
    public void onDisconnect(DisconnectHandler handler) {
        dcHandler = handler;
    }

    @Override
    public boolean serviceExists(UUID serviceUuid) {
        return enableMetaBootState && serviceUuid.equals(MetaWearBoard.METABOOT_SERVICE) ||
                serviceUuid.equals(MetaWearBoard.METAWEAR_GATT_SERVICE);
    }

    @Override
    public Task<byte[][]> readCharacteristicAsync(BtleGattCharacteristic[] characteristics) {
        final ArrayList<Task<byte[]>> tasks = new ArrayList<>();
        for(BtleGattCharacteristic it: characteristics) {
            tasks.add(readCharacteristicAsync(it));
        }

        return Task.whenAll(tasks).onSuccessTask(task -> {
            byte[][] valuesArray = new byte[tasks.size()][];
            for (int i = 0; i < valuesArray.length; i++) {
                valuesArray[i] = tasks.get(i).getResult();
            }

            return Task.forResult(valuesArray);
        });
    }

    @Override
    public Task<Void> localDisconnectAsync() {
        nDisconnects++;
        bridge.disconnected();
        return Task.forResult(null);
    }

    @Override
    public Task<Void> remoteDisconnectAsync() {
        return localDisconnectAsync();
    }

    @Override
    public Task<Void> connectAsync() {
        nConnects++;
        return Task.forResult(null);
    }

    @Override
    public Task<Integer> readRssiAsync() {
        TaskCompletionSource<Integer> source= new TaskCompletionSource<>();
        source.trySetError(new UnsupportedOperationException("Reading rssi not supported in JUnit tests"));
        return source.getTask();
    }

    @Override
    public Task<File> downloadFileAsync(String srcUrl, String dest) {
        if (srcUrl.endsWith("firmware.zip") || srcUrl.endsWith("bl.zip") || srcUrl.endsWith("sd_bl.zip")) {
            return Task.forResult(new File(dest));
        } else if (srcUrl.endsWith("info2.json")) {
            return Task.forResult(new File(RES_PATH, "info2.json"));
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public File findDownloadedFile(String filename) {
        // create a dummy File object
        return new File(RES_PATH, filename);
    }

    @Override
    public void logWarn(String tag, String message) {
        System.out.println(String.format(Locale.US, "%s: %s", tag, message));
    }

    @Override
    public void logWarn(String tag, String message, Throwable tr) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);

        System.out.println(String.format(Locale.US, "%s: %s%n%s", tag, message, sw.toString()));
    }

    byte[][] getConnectCommands() {
        byte[][] cmdArray= new byte[connectCmds.size()][];
        for(int i= 0; i < connectCmds.size(); i++) {
            cmdArray[i]= connectCmds.get(i);
        }

        return cmdArray;
    }

    byte[][] getCommands() {
        return getCommands(0, commandHistory.size());
    }

    byte[][] getCommands(int start, int end) {
        byte[][] cmdArray= new byte[end - start][];
        for(int i= start; i < end; i++) {
            cmdArray[i - start]= commandHistory.get(i);
        }

        return cmdArray;
    }

    byte[][] getCommands(int start) {
        return getCommands(start, commandHistory.size());
    }

    public byte[] getLastCommand() {
        return commandHistory.isEmpty() ? null : commandHistory.get(commandHistory.size() - 1);
    }

    byte[][] getLastCommands(int count) {
        byte[][] cmdArray= new byte[count][];
        for(int i= 0; i < count; i++) {
            int index= commandHistory.size() - (count - i);
            cmdArray[i]= commandHistory.get(index);
        }

        return cmdArray;
    }

    BtleGattCharacteristic[] getGattCharReadHistory() {
        BtleGattCharacteristic[] array = new BtleGattCharacteristic[gattCharReadHistory.size()];
        gattCharReadHistory.toArray(array);
        return array;
    }
}
