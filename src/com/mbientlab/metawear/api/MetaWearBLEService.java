/**
 * Copyright 2014 MbientLab Inc. All rights reserved.
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
 * PROVIDED “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, 
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
package com.mbientlab.metawear.api;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

/**
 * Service for maintaining the Bluetooth GATT connection to the MetaWear board
 * @author Eric Tsai
 */
public class MetaWearBLEService extends Service {
    /**
     * Represents the state of MetaWear service
     * @author Eric Tsai
     */
    private enum DeviceState {
        /** Service is in the process of enabling notifications */
        ENABLING_NOTIFICATIONS,
        /** Service is reading characteristics */
        READING_CHARACTERISTICS,
        /** Service is writing characteristics */
        WRITING_CHARACTERISTICS,
        /** Service is ready to process a command */
        READY;
    }

    /** BLE standard UUID for device information service */
    private final static UUID deviceInfoService= UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    /** UUID for MetaWear specific service */
    private final static UUID metaWearService= UUID.fromString("326A9000-85CB-9195-D9DD-464CFBBAE75A");
    /** BLE standard UUID for configuring characteristics */
    private final static UUID characteristicConfig= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /** GATT connection to the ble device */
    private BluetoothGatt metaWearGatt;
    /** Current state of the device */
    private DeviceState deviceState;
    /** Bytes still be to written to the MetaWear command characteristic */
    private final ConcurrentLinkedQueue<byte[]> commandBytes= new ConcurrentLinkedQueue<>();
    /** Characteristic UUIDs still to be read from MetaWear */
    private final ConcurrentLinkedQueue<UUID> readCharUuids= new ConcurrentLinkedQueue<>();

    /**
     * Get an IntentFilter for MetaWear specific actions used by the MetaWear modules
     * @return IntentFilter for MetaWear specific actions
     * @see BroadcastReceiverBuilder
     */
    public static IntentFilter getMetaWearIntentFilter() {
        IntentFilter filter= new IntentFilter();
        
        filter.addAction(Actions.MetaWear.NOTIFICATION_RECEIVED);
        return filter;
    }

    /**
     * Get an IntentFilter for general Bluetooth LE broadcasts
     * @return IntentFilter for general Bluetooth LE broadcasts
     */
    public static IntentFilter getBLEIntentFilter() {
        IntentFilter filter= new IntentFilter();
        
        filter.addAction(Actions.BluetoothLE.CHARACTERISTIC_READ);
        filter.addAction(Actions.BluetoothLE.DEVICE_CONNECTED);
        filter.addAction(Actions.BluetoothLE.DEVICE_DISCONNECTED);
        return filter;
    }
    
    /** Interacts with the MetaWear board */
    private final MetaWearController controller= new MetaWearController() {
        private final Collection<NotificationRegister> globalEnables= new HashSet<>(); 
        
        @Override
        public void setLEDState(LEDState state) {
            queueCommand(Commands.Register.LED_ENABLE.buildWriteCommand(state.setting));
        }
        @Override
        public void setLEDColor(byte red, byte green, byte blue) {
            queueCommand(Commands.Register.LED_COLOR.buildWriteCommand(red, green, blue));
        }
        
        @Override
        public void readTemperature() {
            queueCommand(Commands.Register.TEMPERATURE.buildReadCommand((byte)0));
        }
        
        @Override
        public void readAnalogInput(byte pin, AnalogMode mode) {
            queueCommand(mode.register.buildReadCommand(pin));
        }
        @Override
        public void readDigitalInput(byte pin) {
            queueCommand(Commands.Register.READ_DIGITAL_INPUT.buildReadCommand(pin));
        }
        @Override
        public void setDigitalOutput(byte pin) {
            queueCommand(Commands.Register.SET_DIGITAL_OUTPUT.buildWriteCommand(pin));
        }
        @Override
        public void clearDigitalOutput(byte pin) {
            queueCommand(Commands.Register.CLEAR_DIGITAL_OUTPUT.buildWriteCommand(pin));
        }
        
        @Override
        public void setDigitalInput(byte pin, PullMode mode) {
            queueCommand(mode.register.buildWriteCommand(pin));
        }
        
        @Override
        public void resetDevice() {
            queueCommand(Commands.Register.RESET_DEVICE.buildWriteCommand());
        }
        @Override
        public void jumpToBootloader() {
            queueCommand(Commands.Register.JUMP_TO_BOOTLOADER.buildWriteCommand());
        }
        
        @Override
        public void readDeviceInformation() {
            for(Characteristics.DeviceInformation it: Characteristics.DeviceInformation.values()) {
                readCharUuids.add(it.uuid);
            }
            if (deviceState == DeviceState.READY) {
                deviceState= DeviceState.READING_CHARACTERISTICS;
                readCharacteristic();
            }
        }
        
        @Override
        public void enableNotification(NotificationRegister notifyRegister) {
            queueCommand(notifyRegister.register.buildWriteCommand((byte)1));
            if (notifyRegister.requireGlobalEnable) {
                queueCommand(Commands.Register.GLOBAL_ENABLE.buildWriteCommand((byte)1));
                globalEnables.add(notifyRegister);
            }
        }
        @Override
        public void disableNotification(NotificationRegister notifyRegister) {
            queueCommand(notifyRegister.register.buildWriteCommand((byte)0));
            globalEnables.remove(notifyRegister);
            if (globalEnables.isEmpty()) queueCommand(Commands.Register.GLOBAL_ENABLE.buildWriteCommand((byte)0));
        }
    };
    /**
     * Adds a command to the write queue
     * @param data Sequence of byte representing a command for MetaWear
     */
    private void queueCommand(byte[] data) {
        commandBytes.add(data);
        if (deviceState == DeviceState.READY) {
            deviceState= DeviceState.WRITING_CHARACTERISTICS;
            writeCommand();
        }
    }
    /**
     * Writes a command to MetaWear via the command register UUID
     * @see Characteristics.MetaWear#COMMAND
     */
    private void writeCommand() {
        BluetoothGattService service= metaWearGatt.getService(metaWearService);
        BluetoothGattCharacteristic command= service.getCharacteristic(Characteristics.MetaWear.COMMAND.uuid);
        command.setValue(commandBytes.poll());
        metaWearGatt.writeCharacteristic(command);
    }

    /**
     * Read a characteristic from MetaWear.
     * An intent with the action CHARACTERISTIC_READ will be broadcasted.
     * @see Actions.BluetoothLE#CHARACTERISTIC_READ
     */
    private void readCharacteristic() {
        BluetoothGattService service= metaWearGatt.getService(deviceInfoService);

        BluetoothGattCharacteristic characteristic= service.getCharacteristic(readCharUuids.poll());
        metaWearGatt.readCharacteristic(characteristic);
    }
    
    /** MetaWear specific Bluetooth GATT callback */
    private final BluetoothGattCallback metaWearGattCallback= new BluetoothGattCallback() {
        private ArrayDeque<BluetoothGattCharacteristic> shouldNotify= new ArrayDeque<>();
        
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) {
            Intent intent= new Intent();
            boolean broadcast= true;
            
            switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                gatt.discoverServices();
                intent.setAction(Actions.BluetoothLE.DEVICE_CONNECTED);
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                intent.setAction(Actions.BluetoothLE.DEVICE_DISCONNECTED);
                break;
            default:
                broadcast= false;
                break;
            }
            if (broadcast) sendBroadcast(intent);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            for(BluetoothGattService service: gatt.getServices()) {
                for(BluetoothGattCharacteristic characteristic: service.getCharacteristics()) {
                    int charProps = characteristic.getProperties();
                    if ((charProps & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        shouldNotify.add(characteristic);
                    }
                }
            }
            deviceState= DeviceState.ENABLING_NOTIFICATIONS;
            setupNotification();
        }

        private void setupNotification() {
            metaWearGatt.setCharacteristicNotification(shouldNotify.peek(), true);
            BluetoothGattDescriptor descriptor= shouldNotify.poll().getDescriptor(characteristicConfig);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            metaWearGatt.writeDescriptor(descriptor);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor, int status) {
            if (!shouldNotify.isEmpty()) setupNotification();
            else deviceState= DeviceState.READY;
            
            if (deviceState == DeviceState.READY && !commandBytes.isEmpty()) {
                deviceState= DeviceState.WRITING_CHARACTERISTICS;
                writeCommand();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            Intent intent= new Intent(Actions.BluetoothLE.CHARACTERISTIC_READ);
            intent.putExtra(BroadcastReceiverBuilder.CHARACTERISTIC_UUID, characteristic.getUuid());
            intent.putExtra(BroadcastReceiverBuilder.CHARACTERISTIC_VALUE, characteristic.getValue());
            sendBroadcast(intent);

            if (!readCharUuids.isEmpty()) readCharacteristic(); 
            else deviceState= DeviceState.READY;
            
            if (deviceState == DeviceState.READY) {
                if (!commandBytes.isEmpty()) {
                    deviceState= DeviceState.WRITING_CHARACTERISTICS;
                    writeCommand();
                } else if (!readCharUuids.isEmpty()) {
                    deviceState= DeviceState.READING_CHARACTERISTICS;
                    readCharacteristic();
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            if (!commandBytes.isEmpty()) writeCommand();
            else deviceState= DeviceState.READY;
            if (deviceState == DeviceState.READY && !readCharUuids.isEmpty()) {
                deviceState= DeviceState.READING_CHARACTERISTICS;
                readCharacteristic();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            Intent intent= new Intent(Actions.MetaWear.NOTIFICATION_RECEIVED);
            intent.putExtra(BroadcastReceiverBuilder.CHARACTERISTIC_VALUE, characteristic.getValue());
            sendBroadcast(intent);
        }
    };

    /**
     * Get a controller to the connected MetaWear device 
     * @return MetaWear controller
     */
    public MetaWearController getMetaWearController() {
        return controller;
    }
    /**
     * Connect to the GATT service on the NetaWear device
     * @param metaWearDevice MetaWear device to connect to
     */
    public void connect(BluetoothDevice metaWearDevice) {
        metaWearGatt= metaWearDevice.connectGatt(this, false, metaWearGattCallback);
    }

    /** Disconnect from the GATT service */
    public void disconnect() {
        if (metaWearGatt != null) {
            metaWearGatt.close();
            metaWearGatt= null;
        }
    }

    /** Binding between the Intent and this service */
    private final Binder serviceBinder= new LocalBinder();

    /** Dummy class for getting the MetaWear BLE service from its binder */
    public class LocalBinder extends Binder {
        /**
         * Get the MetaWearBLEService object
         * @return MetaWearBLEService object
         */
        public MetaWearBLEService getService() {
            return MetaWearBLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return serviceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        return super.onUnbind(intent);
    }
}