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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * Created by etsai on 8/31/16.
 */
class JunitPlatform implements IO, BtleGatt {
    private static final ScheduledExecutorService SCHEDULED_TASK_THREADPOOL = Executors.newSingleThreadScheduledExecutor();

    interface MwBridge {
        void disconnected();
        void sendMockResponse(byte[] response);
        void sendMockGattCharReadValue(BtleGattCharacteristic gattChar, byte[] value);
    }

    public int nConnects = 0, nDisconnects = 0;
    public MetaWearBoardInfo boardInfo= new MetaWearBoardInfo();
    public String firmware= "1.2.3", boardStateSuffix;
    public boolean delayModuleInfoResponse= false, deserializeModuleInfo= false, serializeModuleInfo = false, enableMetaBootState = false, delayReadDevInfo = false;
    private final Map<Byte, byte[]> customModuleInfo= new HashMap<>();

    public byte maxProcessors= 28, maxLoggers= 8, maxTimers= 8, maxEvents= 28;
    public byte timerId= 0, eventId= 0, loggerId= 0, dataProcessorId= 0, macroId = 0;
    private final MwBridge bridge;
    private final ArrayList<byte[]> commandHistory= new ArrayList<>(), connectCmds= new ArrayList<>();
    private final ArrayList<BtleGattCharacteristic> gattCharReadHistory = new ArrayList<>();
    public BtleGatt.Callback btleCallback;

    public JunitPlatform(MwBridge bridge) {
        this.bridge= bridge;
    }

    public void addCustomModuleInfo(byte[] info) {
        customModuleInfo.put(info[0], info);
    }

    private void scheduleMockResponse(final byte[] response) {
        SCHEDULED_TASK_THREADPOOL.schedule(new Runnable() {
            @Override
            public void run() {
                bridge.sendMockResponse(response);
            }
        }, 20, TimeUnit.MILLISECONDS);
    }

    private void scheduleMockGattCharReadValue(final BtleGattCharacteristic gattChar, final byte[] response) {
        SCHEDULED_TASK_THREADPOOL.schedule(new Runnable() {
            @Override
            public void run() {
                bridge.sendMockGattCharReadValue(gattChar, response);
            }
        }, 20, TimeUnit.MILLISECONDS);
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
            return new FileInputStream("src/test/res/board_module_info");
        }
        return boardStateSuffix != null ?
                new FileInputStream(String.format(Locale.US, "src/test/res/board_state_%s", boardStateSuffix)) :
                null;
    }

    @Override
    public void registerCallback(Callback callback) {
        btleCallback = callback;
    }

    @Override
    public void writeCharacteristic(BtleGattCharacteristic gattCharr, BtleGatt.GattCharWriteType writeType, byte[] value) {
        if (value[1] == (byte) 0x80) {
            connectCmds.add(value);
            byte[] response = customModuleInfo.containsKey(value[0]) ?
                    customModuleInfo.get(value[0]) :
                    boardInfo.moduleResponses.get(value[0]);

            if (delayModuleInfoResponse) {
                scheduleMockResponse(response);
            } else {
                bridge.sendMockResponse(response);
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
                byte[] response = {value[2], 0x2, macroId};
                macroId++;
                scheduleMockResponse(response);
            } else if (value[0] == (byte) 0xb && value[1] == (byte) 0x85) {
                bridge.sendMockResponse(new byte[] {0x0b, (byte) 0x85, (byte) 0x9e, 0x01, 0x00, 0x00});
            }
        }
    }

    @Override
    public void readCharacteristic(BtleGattCharacteristic gattChar) {
        byte[] value;

        gattCharReadHistory.add(gattChar);
        if (gattChar.equals(DeviceInformationService.FIRMWARE_REVISION)) {
            value= firmware.getBytes();
            scheduleMockGattCharReadValue(gattChar, value);
        } else if (gattChar.equals(DeviceInformationService.HARDWARE_REVISION)) {
            value= boardInfo.hardwareRevision;
            scheduleMockGattCharReadValue(gattChar, value);
        } else if (gattChar.equals(DeviceInformationService.MODEL_NUMBER)) {
            value= boardInfo.modelNumber;
            scheduleMockGattCharReadValue(gattChar, value);
        } else if (!delayReadDevInfo && gattChar.equals(DeviceInformationService.MANUFACTURER_NAME)) {
            value= boardInfo.manufacturer;
            scheduleMockGattCharReadValue(gattChar, value);
        } else if (!delayReadDevInfo && gattChar.equals(DeviceInformationService.SERIAL_NUMBER)) {
            value= boardInfo.serialNumber;
            scheduleMockGattCharReadValue(gattChar, value);
        }
    }

    @Override
    public boolean serviceExists(UUID serviceUuid) {
        return serviceUuid.equals(MetaWearBoard.METABOOT_SERVICE) && enableMetaBootState;
    }

    @Override
    public Task<Void> disconnectAsync() {
        nDisconnects++;
        bridge.disconnected();
        return Task.forResult(null);
    }

    @Override
    public Task<Void> boardDisconnectAsync() {
        return disconnectAsync();
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
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public File findDownloadedFile(String filename) {
        throw new UnsupportedOperationException("Not yet implemented");
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
        byte[][] cmdArray= new byte[commandHistory.size()][];
        for(int i= 0; i < commandHistory.size(); i++) {
            cmdArray[i]= commandHistory.get(i);
        }

        return cmdArray;
    }

    public byte[] getLastCommand() {
        return commandHistory.get(commandHistory.size() - 1);
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
