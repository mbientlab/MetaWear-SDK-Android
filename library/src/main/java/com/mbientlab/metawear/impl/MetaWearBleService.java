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

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.mbientlab.metawear.AsyncResult;
import com.mbientlab.metawear.MetaWearBoard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by etsai on 6/15/2015.
 */
public class MetaWearBleService extends Service {
    private final static UUID CHARACTERISTIC_CONFIG= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            METAWEAR_SERVICE= UUID.fromString("326A9000-85CB-9195-D9DD-464CFBBAE75A"),
            METAWEAR_COMMAND= UUID.fromString("326A9001-85CB-9195-D9DD-464CFBBAE75A"),
            METAWEAR_NOTIFY= UUID.fromString("326A9006-85CB-9195-D9DD-464CFBBAE75A");;

    private enum GattActionKey {
        NONE,
        DESCRIPTOR_WRITE,
        CHAR_READ,
        CHAR_WRITE,
        RSSI_READ
    }

    private class GattConnectionState {
        public String manufacturer= null, serialNumber= null, firmwareVersion= null, hardwareVersion= null;

        public final AtomicInteger nDescriptors= new AtomicInteger(0);
        public final AtomicBoolean isConnected= new AtomicBoolean(false), isReady= new AtomicBoolean(false);
        public MetaWearBoard.ConnectionStateHandler connectionHandler;
        public final ConcurrentLinkedQueue<AsyncResultImpl<Integer>> rssiResult= new ConcurrentLinkedQueue<>();

        public boolean deviceInfoReady() {
            return manufacturer != null && serialNumber != null && firmwareVersion != null && hardwareVersion != null;
        }

        public MetaWearBoard.DeviceInformation buildDeviceInfo() {
            return new MetaWearBoard.DeviceInformation() {
                @Override public String manufacturer() { return manufacturer; }
                @Override public String serialNumber() { return serialNumber; }
                @Override public String firmwareVersion() { return firmwareVersion; }
                @Override public String hardwareVersion() { return hardwareVersion; }
                @Override public String toString() {
                    return String.format("{manufacturer: %s, serialNumber: %s, firmwareVersion: %s, hardwareVersion: %s}",
                            manufacturer, serialNumber, firmwareVersion, hardwareVersion);
                }
            };
        }

        public void reset() {
            nDescriptors.set(0);
            isReady.set(false);
            isConnected.set(false);
            manufacturer= null;
            serialNumber= null;
            firmwareVersion= null;
            hardwareVersion= null;
        }
    }
    private interface Action {
        public boolean execute();
    }
    private class GattActionManager {
        private ConcurrentLinkedQueue<Action> actions= new ConcurrentLinkedQueue<>();
        private AtomicBoolean isExecActions= new AtomicBoolean();
        private GattActionKey gattKey= GattActionKey.NONE;

        public void setExpectedGattKey(GattActionKey newKey) {
            gattKey= newKey;
        }

        public void updateExecActionsState() {
            isExecActions.set(!actions.isEmpty());
        }
        public void queueAction(Action newAction) {
            actions.add(newAction);
        }
        public void executeNext(GattActionKey key) {
            if (!actions.isEmpty()) {
                if (key == gattKey || !isExecActions.get()) {
                    isExecActions.set(true);
                    boolean lastResult= false;
                    while(!actions.isEmpty() && (lastResult= actions.poll().execute()) == false) { }

                    if (!lastResult && actions.isEmpty()) {
                        isExecActions.set(false);
                    }
                }
            } else {
                isExecActions.set(false);
            }
        }
    }

    private class AndroidBleConnection implements Connection {
        private BluetoothGatt gatt;

        public void setBluetoothGatt(BluetoothGatt newGatt) {
            this.gatt= newGatt;
        }

        @Override
        public void sendCommand(final byte[] command) {
            gattManager.queueAction(new Action() {
                @Override
                public boolean execute() {
                    if (gatt != null && gattConnectionStates.get(gatt.getDevice()).isReady.get()) {
                        gattManager.setExpectedGattKey(GattActionKey.CHAR_WRITE);
                        BluetoothGattService service= gatt.getService(METAWEAR_SERVICE);
                        BluetoothGattCharacteristic mwCmdChar= service.getCharacteristic(METAWEAR_COMMAND);
                        mwCmdChar.setValue(command);
                        gatt.writeCharacteristic(mwCmdChar);
                        return true;
                    }
                    return false;
                }
            });
            gattManager.executeNext(GattActionKey.NONE);
        }
    }

    private class DefaultMetaWearBoardImpl extends DefaultMetaWearBoard {
        private AtomicBoolean readingDevInfo= new AtomicBoolean();
        private final ConcurrentLinkedQueue<AsyncResultImpl<DeviceInformation>> readDevInfoResult= new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<AsyncResultImpl<Byte>> batteryResult= new ConcurrentLinkedQueue<>();
        private final BluetoothDevice btDevice;
        private BluetoothGatt gatt;

        public DefaultMetaWearBoardImpl(BluetoothDevice btDevice) {
            super(new AndroidBleConnection());
            this.btDevice= btDevice;
            gattConnectionStates.put(btDevice, new GattConnectionState());
        }

        public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {
            GattConnectionState state= gattConnectionStates.get(btDevice);

            if (characteristic.getService().getUuid().equals(DevInfoCharacteristic.MANUFACTURER_NAME.serviceUuid())) {
                if (status != 0) {
                    readDevInfoResult.poll().setResult(null,
                            new RuntimeException(String.format(Locale.US, "Error reading device information characteristics (%d)", status)));

                    if (!readDevInfoResult.isEmpty()) {
                        queueDeviceInfoQueries(state);
                    }
                } else {
                    if (characteristic.getUuid().equals(DevInfoCharacteristic.MANUFACTURER_NAME.uuid())) {
                        state.manufacturer= new String(characteristic.getValue());
                    } else if (characteristic.getUuid().equals(DevInfoCharacteristic.SERIAL_NUMBER.uuid())) {
                        state.serialNumber= new String(characteristic.getValue());
                    } else if (characteristic.getUuid().equals(DevInfoCharacteristic.FIRMWARE_VERSION.uuid())) {
                        state.firmwareVersion= new String(characteristic.getValue());
                    } else if (characteristic.getUuid().equals(DevInfoCharacteristic.HARDWARE_VERSION.uuid())) {
                        state.hardwareVersion= new String(characteristic.getValue());
                    }

                    if (state.deviceInfoReady()) {
                        readingDevInfo.set(false);

                        while(!readDevInfoResult.isEmpty()) {
                            readDevInfoResult.poll().setResult(state.buildDeviceInfo(), null);
                        }
                    }
                }
            } else if (characteristic.getService().getUuid().equals(BattLevelCharacteristic.LEVEL.serviceUuid())) {
                if (status != 0) {
                    batteryResult.poll().setResult(null,
                            new RuntimeException(String.format(Locale.US, "Error reading battery level characteristic (%d)", status)));
                } else {
                    batteryResult.poll().setResult(characteristic.getValue()[0], null);
                }
            }
        }

        @Override
        public AsyncResult<DeviceInformation> readDeviceInformation() {
            AsyncResultImpl<DeviceInformation> result= new AsyncResultImpl<>();
            final GattConnectionState state= gattConnectionStates.get(btDevice);

            if (state.deviceInfoReady()) {
                result.setResult(state.buildDeviceInfo(), null);
            } else {
                readDevInfoResult.add(result);
                if (!readingDevInfo.get()) {
                    readingDevInfo.set(true);

                    queueDeviceInfoQueries(state);

                    gattManager.executeNext(GattActionKey.NONE);
                }
            }

            return result;
        }

        @Override
        public AsyncResult<Integer> readRssi() {
            final GattConnectionState state= gattConnectionStates.get(btDevice);
            AsyncResultImpl<Integer> result= new AsyncResultImpl<>();

            state.rssiResult.add(result);
            gattManager.queueAction(new Action() {
                @Override
                public boolean execute() {
                    if (gatt != null && state.isReady.get()) {
                        gattManager.setExpectedGattKey(GattActionKey.RSSI_READ);
                        gatt.readRemoteRssi();
                        return true;
                    }
                    gattConnectionStates.get(btDevice).rssiResult.poll().setResult(null,
                            new RuntimeException("Bluetooth LE connection not established"));
                    return false;
                }
            });
            gattManager.executeNext(GattActionKey.NONE);
            return result;
        }

        @Override
        public AsyncResult<Byte> readBatteryLevel() {
            AsyncResultImpl<Byte> result= new AsyncResultImpl<>();
            batteryResult.add(result);
            gattManager.queueAction(new Action() {
                @Override
                public boolean execute() {
                    if (gatt != null && gattConnectionStates.get(gatt.getDevice()).isReady.get()) {
                        gattManager.setExpectedGattKey(GattActionKey.CHAR_READ);
                        BluetoothGattService service= gatt.getService(BattLevelCharacteristic.LEVEL.serviceUuid());
                        BluetoothGattCharacteristic batLevelChar= service.getCharacteristic(BattLevelCharacteristic.LEVEL.uuid());
                        gatt.readCharacteristic(batLevelChar);
                        return true;
                    }
                    batteryResult.poll().setResult(null,
                            new RuntimeException("Bluetooth LE connection not established"));
                    return false;
                }
            });
            gattManager.executeNext(GattActionKey.NONE);
            return result;
        }

        private void queueDeviceInfoQueries(final GattConnectionState state) {
            ArrayList<DevInfoCharacteristic> charsToRead= new ArrayList<>();

            if (state.manufacturer == null) {
                charsToRead.add(DevInfoCharacteristic.MANUFACTURER_NAME);
            }

            if (state.serialNumber == null) {
                charsToRead.add(DevInfoCharacteristic.SERIAL_NUMBER);
            }

            if (state.firmwareVersion == null) {
                charsToRead.add(DevInfoCharacteristic.FIRMWARE_VERSION);
            }

            if (state.hardwareVersion == null) {
                charsToRead.add(DevInfoCharacteristic.HARDWARE_VERSION);
            }

            for(final DevInfoCharacteristic it: charsToRead) {
                gattManager.queueAction(new Action() {
                    @Override
                    public boolean execute() {
                        if (gatt != null && state.isReady.get()) {
                            gattManager.setExpectedGattKey(GattActionKey.CHAR_READ);
                            BluetoothGattService service= gatt.getService(it.serviceUuid());
                            BluetoothGattCharacteristic devInfoChar= service.getCharacteristic(it.uuid());
                            gatt.readCharacteristic(devInfoChar);
                            return true;
                        }
                        return false;
                    }
                });
            }
        }

        public void close() {
            if (gatt != null) {
                gatt.close();
            }
            gatt= null;
        }

        @Override
        public void setConnectionStateHandler(ConnectionStateHandler handler) {
            gattConnectionStates.get(btDevice).connectionHandler= handler;
        }

        @Override
        public void connect() {
            if (!isConnected()) {
                close();
                gattConnectionStates.get(btDevice).reset();

                gatt= btDevice.connectGatt(MetaWearBleService.this, false, gattCallback);
                ((AndroidBleConnection) conn).setBluetoothGatt(gatt);
            }
        }

        @Override
        public void disconnect() {
            if (gatt != null) {
                gatt.disconnect();
            }
        }
        @Override
        public boolean isConnected() {
            return gattConnectionStates.get(btDevice).isReady.get();
        }
    }

    private final GattActionManager gattManager= new GattActionManager();
    private final HashMap<BluetoothDevice, DefaultMetaWearBoardImpl> mwBoards= new HashMap<>();
    private final HashMap<BluetoothDevice, GattConnectionState> gattConnectionStates= new HashMap<>();

    private final BluetoothGattCallback gattCallback= new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            GattConnectionState state= gattConnectionStates.get(gatt.getDevice());

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    gattConnectionStates.get(gatt.getDevice()).isConnected.set(status == 0);
                    if (status != 0) {
                        state.connectionHandler.failure(status, new RuntimeException(
                                String.format(Locale.US, "Error connecting to gatt server (%d)", status)));
                    } else {
                        gatt.discoverServices();
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i("MetaWearManagerService", "Disconnected");

                    state.isConnected.set(false);
                    state.isReady.set(false);
                    if (state.connectionHandler != null) {
                        state.connectionHandler.disconnected();
                    }
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            if (status != 0) {
                GattConnectionState state= gattConnectionStates.get(gatt.getDevice());
                state.isConnected.set(false);
                state.connectionHandler.failure(status, new RuntimeException(
                        String.format(Locale.US, "Error discovering Bluetooth services (%d)", status)));
            } else {
                for (BluetoothGattService service : gatt.getServices()) {
                    for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        int charProps = characteristic.getProperties();
                        if ((charProps & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            gattConnectionStates.get(gatt.getDevice()).nDescriptors.incrementAndGet();

                            gattManager.queueAction(new Action() {
                                @Override
                                public boolean execute() {
                                    if (gatt != null && gattConnectionStates.get(gatt.getDevice()).isConnected.get()) {
                                        gatt.setCharacteristicNotification(characteristic, true);

                                        gattManager.setExpectedGattKey(GattActionKey.DESCRIPTOR_WRITE);
                                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_CONFIG);
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        gatt.writeDescriptor(descriptor);

                                        return true;
                                    }
                                    return false;
                                }
                            });
                        }
                    }
                }
            }

            gattManager.executeNext(GattActionKey.NONE);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] value= characteristic.getValue();
            StringBuilder builder= new StringBuilder();
            builder.append(String.format("%02x", value[0]));
            for(int i= 1; i < value.length; i++) {
                builder.append(String.format("-%02x", value[i]));
            }
            Log.i("MetaWearManagerService", "Wrote a command: " + builder.toString());
            gattManager.updateExecActionsState();
            gattManager.executeNext(GattActionKey.CHAR_WRITE);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(METAWEAR_NOTIFY)) {
                mwBoards.get(gatt.getDevice()).receivedResponse(characteristic.getValue());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            gattManager.updateExecActionsState();

            GattConnectionState state= gattConnectionStates.get(gatt.getDevice());
            if (status != 0) {
                state.connectionHandler.failure(status, new RuntimeException(String.format(Locale.US, "Error writing descriptors (%d)", status)));
            } else {
                int newCount= state.nDescriptors.decrementAndGet();
                if (newCount == 0) {
                    state.isReady.set(true);
                    state.connectionHandler.connected();
                }
            }

            gattManager.executeNext(GattActionKey.DESCRIPTOR_WRITE);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            gattManager.updateExecActionsState();

            if (status != 0) {
                gattConnectionStates.get(gatt.getDevice()).rssiResult.poll().setResult(null,
                        new RuntimeException(String.format(Locale.US, "Error reading RSSI value (%d)", status)));
            } else {
                gattConnectionStates.get(gatt.getDevice()).rssiResult.poll().setResult(rssi, null);
            }

            gattManager.executeNext(GattActionKey.RSSI_READ);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            gattManager.updateExecActionsState();
            mwBoards.get(gatt.getDevice()).onCharacteristicRead(characteristic, status);
            gattManager.executeNext(GattActionKey.CHAR_READ);
        }
    };

    private final IBinder mBinder= new LocalBinder();
    public class LocalBinder extends Binder {
        public MetaWearBoard getMetaWearBoard(BluetoothDevice btDevice) {
            if (!mwBoards.containsKey(btDevice)) {
                mwBoards.put(btDevice, new DefaultMetaWearBoardImpl(btDevice));
            }
            return mwBoards.get(btDevice);
        }
    }

    @Override
    public void onDestroy() {
        for(Map.Entry<BluetoothDevice, DefaultMetaWearBoardImpl> it: mwBoards.entrySet()) {
            it.getValue().close();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
