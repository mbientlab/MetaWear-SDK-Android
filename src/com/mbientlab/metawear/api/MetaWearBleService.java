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
package com.mbientlab.metawear.api;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mbientlab.metawear.api.GATT.GATTCharacteristic;
import com.mbientlab.metawear.api.GATT.GATTService;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks.GattOperation;
import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.characteristic.*;
import com.mbientlab.metawear.api.controller.*;
import com.mbientlab.metawear.api.controller.Accelerometer.SamplingConfig.OutputDataRate;
import com.mbientlab.metawear.api.controller.Logging.Trigger;
import com.mbientlab.metawear.api.util.Registers;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Service for maintaining the Bluetooth GATT connection to the MetaWear board
 * @author Eric Tsai
 */
public class MetaWearBleService extends Service {
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

    private class Action {
        /** An non-zero status was returned in a bt gatt callback function */
        public static final String GATT_ERROR=
                "com.mbientlab.com.metawear.api.MetaWearBleService.Action.GATT_ERROR";
        /** Data was received from a MetaWear notification register */
        public static final String NOTIFICATION_RECEIVED=
                "com.mbientlab.com.metawear.api.MetaWearBleService.Action.NOTIFICATION_RECEIVED";
        /** Connected to a Bluetooth device */
        public static final String DEVICE_CONNECTED= 
                "com.mbientlab.com.metawear.api.MetaWearBleService.Action.DEVICE_CONNECTED";
        /** Disconnected from a Bluetooth device */
        public static final String DEVICE_DISCONNECTED= 
                "com.mbientlab.com.metawear.api.MetaWearBleService.Action.DEVICE_DISCONNECTED";
        /** A Bluetooth characteristic was read */
        public static final String CHARACTERISTIC_READ=
                "com.mbientlab.com.metawear.api.MetaWearBleService.Action.CHARACTERISTIC_READ";
        /** Read the RSSI value of the remote device */
        public static final String RSSI_READ=
                "com.mbientlab.com.metawear.api.MetaWearBleService.Action.RSSI_READ";
    }
    private class Extra {
        public static final String EXPLICIT_CLOSE=
                "com.mbientlab.metawear.api.MetaWearBleService.Extra.EXPLICIT_CLOSE";
        public static final String BLUETOOTH_DEVICE=
                "com.mbientlab.metawear.api.MetaWearBleService.Extra.BLUETOOTH_DEVICE";
        /** Extra intent information identifying the gatt operation */
        public static final String GATT_OPERATION=
                "com.mbientlab.com.metawear.api.MetaWearBleService.Extra.GATT_OPERATION";
        /** Extra intent information for a status code */
        public static final String STATUS=
                "com.mbientlab.com.metawear.api.MetaWearBleService.Extra.STATUS";
        /** Extra Intent information for the remote rssi value */
        public static final String RSSI= 
                "com.mbientlab.com.metawear.api.MetaWearBleService.Extra.RSSI";
        /** Extra Intent information for the service UUID */
        public static final String SERVICE_UUID= 
                "com.mbientlab.com.metawear.api.MetaWearBleService.Extra.SERVICE_UUID";
        /** Extra Intent information for the characteristic UUID */
        public static final String CHARACTERISTIC_UUID= 
                "com.mbientlab.com.metawear.api.MetaWearBleService.Extra.CHARACTERISTIC_UUID";
        /** Extra Intent information for the characteristic value */
        public static final String CHARACTERISTIC_VALUE= 
                "com.mbientlab.com.metawear.api.MetaWearBleService.Extra.CHARACTERISTIC_VALUE";
    }
    
    private interface GattAction {
        public void execAction();
    }
    private interface InternalCallback {
        public void process(byte[] data);
    }
    
    private interface EventInfo {
        public byte[] entry();
        public byte[] command();
    }
    private class EventTriggerBuilder {
        private final ArrayList<EventInfo> entryBytes= new ArrayList<>();
        private final Register srcReg;
        private final byte index;
        
        public EventTriggerBuilder(Register srcReg, byte index) {
            this.srcReg= srcReg;
            this.index= index;
        }
        
        public EventTriggerBuilder withDestRegister(final Register destReg, final byte[] command, 
                final boolean isRead) {
            entryBytes.add(new EventInfo() {
                @Override
                public byte[] entry() {
                    byte destOpcode= destReg.opcode();
                    if (isRead) {
                        destOpcode |= 0x80;
                    }
                    return new byte[] {srcReg.module().opcode, srcReg.opcode(), index,
                            destReg.module().opcode, destOpcode, (byte) command.length};
                }

                @Override
                public byte[] command() {
                    return command;
                }
            });
            return this;
        }
        
        public Collection<EventInfo> getEventInfo() {
            return entryBytes;
        }
    }
   
    private final static UUID CHARACTERISTIC_CONFIG= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    private class MetaWearState {
        public MetaWearState(BluetoothDevice mwBoard) {
            this.mwBoard= mwBoard;
        }
        
        /**
         * 
         */
        public MetaWearState() {
            // TODO Auto-generated constructor stub
        }
        
        public void resetState() {
            mwController.clearCallbacks();
            shouldNotify.clear();
            commandBytes.clear();
            readCharUuids.clear();
        }

        public BluetoothDevice mwBoard;
        public byte thermistorMode= 0;
        public EventTriggerBuilder etBuilder;
        public boolean connected, isRecording= false, retainState= true, readyToClose, notifyUser;
        public MetaWearControllerImpl mwController= null;
        public BluetoothGatt mwGatt= null;
        public DeviceState deviceState= null;
        public final ArrayDeque<BluetoothGattCharacteristic> shouldNotify= new ArrayDeque<>();
        public final ConcurrentLinkedQueue<byte[]> commandBytes= new ConcurrentLinkedQueue<>();
        public final ConcurrentLinkedQueue<GATTCharacteristic> readCharUuids= new ConcurrentLinkedQueue<>();
        public final HashMap<Register, InternalCallback> internalCallbacks= new HashMap<>();
        public final HashMap<Byte, ArrayList<ModuleCallbacks>> moduleCallbackMap= new HashMap<>();
        public final HashSet<DeviceCallbacks> deviceCallbacks= new HashSet<>();
    }
    
    /** GATT connection to the ble device */
    private static final HashMap<BluetoothDevice, MetaWearState> metaWearStates= new HashMap<>();
    
    private boolean useLocalBroadcastMnger= false;
    private final ConcurrentLinkedQueue<GattAction> gattActions= new ConcurrentLinkedQueue<>();
    private MetaWearState singleMwState= null;
    private MetaWearController singleController= null;
    
    /**
     * Get the IntentFilter for actions broadcasted by the MetaWear service
     * @return IntentFilter for MetaWear specific actions
     * @see ModuleCallbacks
     * @see DeviceCallbacks
     */
    public static IntentFilter getMetaWearIntentFilter() {
        IntentFilter filter= new IntentFilter();
        
        filter.addAction(Action.NOTIFICATION_RECEIVED);
        filter.addAction(Action.CHARACTERISTIC_READ);
        filter.addAction(Action.DEVICE_CONNECTED);
        filter.addAction(Action.DEVICE_DISCONNECTED);
        filter.addAction(Action.RSSI_READ);
        filter.addAction(Action.GATT_ERROR);
        return filter;
    }
    
    private static BroadcastReceiver mwBroadcastReceiver= null;
    
    /**
     * Get the broadcast receiver for MetaWear intents.  An Activity using the MetaWear service 
     * will need to register this receiver to trigger its callback functions.
     * @return MetaWear specific broadcast receiver
     */
    public static BroadcastReceiver getMetaWearBroadcastReceiver() {
        if (mwBroadcastReceiver == null) {
            mwBroadcastReceiver= new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    BluetoothDevice device= intent.getExtras().getParcelable(Extra.BLUETOOTH_DEVICE);
                    
                    if (device != null && metaWearStates.containsKey(device)) {
                        MetaWearState mwState= metaWearStates.get(device);
                    
                        switch (intent.getAction()) {
                        case Action.GATT_ERROR:
                            int gattStatus= intent.getIntExtra(Extra.STATUS, -1);
                            DeviceCallbacks.GattOperation gattOp= (GattOperation) intent.getExtras().get(Extra.GATT_OPERATION);
                            for(DeviceCallbacks it: mwState.deviceCallbacks) {
                                it.receivedGattError(gattOp, gattStatus);
                            }
                            break;
                        case Action.NOTIFICATION_RECEIVED:
                            final byte[] data= (byte[])intent.getExtras().get(Extra.CHARACTERISTIC_VALUE);
                            if (data.length > 1) {
                                byte moduleOpcode= data[0], registerOpcode= (byte)(0x7f & data[1]);
                                Collection<ModuleCallbacks> callbacks;
                                if (mwState.moduleCallbackMap.containsKey(moduleOpcode) && 
                                        (callbacks= mwState.moduleCallbackMap.get(moduleOpcode)) != null) {
                                    Module.lookupModule(moduleOpcode).lookupRegister(registerOpcode)
                                            .notifyCallbacks(callbacks, data);
                                }
                            }
                            break;
                        case Action.DEVICE_CONNECTED:
                            for(DeviceCallbacks it: mwState.deviceCallbacks) {
                                it.connected();
                            }
                            break;
                        case Action.DEVICE_DISCONNECTED:
                            for(DeviceCallbacks it: mwState.deviceCallbacks) {
                                it.disconnected();
                            }
                            if (!intent.getBooleanExtra(Extra.EXPLICIT_CLOSE, false)) {
                                mwState.mwController.close(false);
                            } else if (!mwState.retainState) {
                                mwState.resetState();
                                metaWearStates.remove(mwState.mwBoard);
                            }
                            break;
                        case Action.CHARACTERISTIC_READ:
                            UUID serviceUuid= (UUID)intent.getExtras().get(Extra.SERVICE_UUID), 
                                    charUuid= (UUID)intent.getExtras().get(Extra.CHARACTERISTIC_UUID);
                            GATTCharacteristic characteristic= GATTService.lookupGATTService(serviceUuid).getCharacteristic(charUuid);
                            for(DeviceCallbacks it: mwState.deviceCallbacks) {
                                it.receivedGATTCharacteristic(characteristic, (byte[])intent.getExtras().get(Extra.CHARACTERISTIC_VALUE));
                            }
                            break;
                        case Action.RSSI_READ:
                            int rssi= intent.getExtras().getInt(Extra.RSSI);
                            for(DeviceCallbacks it: mwState.deviceCallbacks) {
                                it.receivedRemoteRSSI(rssi);
                            }
                            break;
                        }
                    }
                }
            };
        }
        return mwBroadcastReceiver;
    }
    
    private void broadcastIntent(Intent intent) {
        if (useLocalBroadcastMnger) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            sendBroadcast(intent);
        }
    }
    
    private void execGattAction() {
        if (!gattActions.isEmpty()) {
            gattActions.poll().execAction();
        }
    }
    /**
     * Writes a command to MetaWear via the command register UUID
     * @see Characteristics.MetaWear#COMMAND
     */
    private void writeCommand(final MetaWearState mwState) {
        gattActions.add(new GattAction() {
            @Override
            public void execAction() {
                byte[] next= mwState.commandBytes.poll();
                
                if (mwState.mwGatt != null) {
                    BluetoothGattService service= mwState.mwGatt.getService(GATTService.METAWEAR.uuid());
                    BluetoothGattCharacteristic command= service.getCharacteristic(MetaWear.COMMAND.uuid());
                    command.setValue(next);
                    mwState.mwGatt.writeCharacteristic(command);
                }
            }
        });
        execGattAction();
    }

    /**
     * Read a characteristic from MetaWear.
     * An intent with the action CHARACTERISTIC_READ will be broadcasted.
     * @see Action.BluetoothLe#ACTION_CHARACTERISTIC_READ
     */
    private void readCharacteristic(final MetaWearState mwState) {
        gattActions.add(new GattAction() {
            @Override
            public void execAction() {
                GATTCharacteristic charInfo= mwState.readCharUuids.poll();
                
                if (mwState.mwGatt != null) {
                    BluetoothGattService service= mwState.mwGatt.getService(charInfo.gattService().uuid());
            
                    BluetoothGattCharacteristic characteristic= service.getCharacteristic(charInfo.uuid());
                    mwState.mwGatt.readCharacteristic(characteristic);
                }
            }
        });
        execGattAction();
    }

    private abstract class MetaWearControllerImpl extends BluetoothGattCallback implements MetaWearController {
        private final MetaWearState mwState;
        private final HashMap<Module, ModuleController> modules= new HashMap<>();
        
        public MetaWearControllerImpl(MetaWearState mwState) {
            this.mwState= mwState;
        }
        
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Intent intent;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                intent= new Intent(Action.GATT_ERROR);
                intent.putExtra(Extra.STATUS, status);
                intent.putExtra(Extra.GATT_OPERATION, DeviceCallbacks.GattOperation.RSSI_READ);
            } else {
                intent= new Intent(Action.RSSI_READ);
                intent.putExtra(Extra.RSSI, rssi);
            }
            
            intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
            broadcastIntent(intent);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) {
            Intent intent= new Intent();
            boolean broadcast= true;
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                intent.setAction(Action.GATT_ERROR);
                intent.putExtra(Extra.GATT_OPERATION, DeviceCallbacks.GattOperation.CONNECTION_STATE_CHANGE);
                intent.putExtra(Extra.STATUS, status);                
            } else {    
                switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    gatt.discoverServices();
                    intent.setAction(Action.DEVICE_CONNECTED);
                    mwState.connected= true;
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    intent.setAction(Action.DEVICE_DISCONNECTED);
                    mwState.connected= false;
                    break;
                default:
                    broadcast= false;
                    break;
                }    
            }
            if (broadcast) {
                intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
                broadcastIntent(intent);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Intent intent= new Intent(Action.GATT_ERROR);
                intent.putExtra(Extra.STATUS, status);
                intent.putExtra(Extra.GATT_OPERATION, DeviceCallbacks.GattOperation.DISCOVER_SERVICES);
                intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
                broadcastIntent(intent);
            } else {
                for(BluetoothGattService service: gatt.getServices()) {
                    for(BluetoothGattCharacteristic characteristic: service.getCharacteristics()) {
                        int charProps = characteristic.getProperties();
                        if ((charProps & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            mwState.shouldNotify.add(characteristic);
                        }
                    }
                }
                mwState.deviceState= DeviceState.ENABLING_NOTIFICATIONS;
                setupNotification(mwState);
            }
        }

        private void setupNotification(final MetaWearState mwState) {
            gattActions.add(new GattAction() {
                @Override
                public void execAction() {
                    mwState.mwGatt.setCharacteristicNotification(mwState.shouldNotify.peek(), true);
                    BluetoothGattDescriptor descriptor= mwState.shouldNotify.poll().getDescriptor(CHARACTERISTIC_CONFIG);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mwState.mwGatt.writeDescriptor(descriptor);
                }
            });
            execGattAction();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Intent intent= new Intent(Action.GATT_ERROR);
                intent.putExtra(Extra.STATUS, status);
                intent.putExtra(Extra.GATT_OPERATION, DeviceCallbacks.GattOperation.DESCRIPTOR_WRITE);
                intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
                broadcastIntent(intent);
            }

            if (!mwState.shouldNotify.isEmpty()) setupNotification(mwState);
            else mwState.deviceState= DeviceState.READY;
            
            if (mwState.deviceState == DeviceState.READY) {
                mwState.internalCallbacks.put(Temperature.Register.THERMISTOR_MODE, new InternalCallback() {
                    @Override
                    public void process(byte[] data) {
                        mwState.thermistorMode= data[2];
                        mwState.internalCallbacks.remove(Temperature.Register.THERMISTOR_MODE);
                    }
                });
                readRegister(mwState, Temperature.Register.THERMISTOR_MODE);
                if (!mwState.commandBytes.isEmpty()) {
                    mwState.deviceState= DeviceState.WRITING_CHARACTERISTICS;
                    writeCommand(mwState);
                } else if (!mwState.readCharUuids.isEmpty()) {
                    mwState.deviceState= DeviceState.READING_CHARACTERISTICS;
                    readCharacteristic(mwState);
                } else if (mwState.readyToClose) {
                    close(mwState.notifyUser);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            Intent intent;
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                intent= new Intent(Action.GATT_ERROR);
                intent.putExtra(Extra.STATUS, status);
                intent.putExtra(Extra.GATT_OPERATION, DeviceCallbacks.GattOperation.CHARACTERISTIC_READ);
            } else {
                intent= new Intent(Action.CHARACTERISTIC_READ);
                intent.putExtra(Extra.SERVICE_UUID, characteristic.getService().getUuid());
                intent.putExtra(Extra.CHARACTERISTIC_UUID, characteristic.getUuid());
                intent.putExtra(Extra.CHARACTERISTIC_VALUE, characteristic.getValue());
            }
            intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
            broadcastIntent(intent);
            
            if (!mwState.readCharUuids.isEmpty()) readCharacteristic(mwState); 
            else mwState.deviceState= DeviceState.READY;
            
            if (mwState.deviceState == DeviceState.READY) {
                if (!mwState.commandBytes.isEmpty()) {
                    mwState.deviceState= DeviceState.WRITING_CHARACTERISTICS;
                    writeCommand(mwState);
                } else if (!mwState.readCharUuids.isEmpty()) {
                    mwState.deviceState= DeviceState.READING_CHARACTERISTICS;
                    readCharacteristic(mwState);
                } else if (mwState.readyToClose) {
                    close(mwState.notifyUser);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Intent intent= new Intent(Action.GATT_ERROR);
                intent.putExtra(Extra.STATUS, status);
                intent.putExtra(Extra.GATT_OPERATION, DeviceCallbacks.GattOperation.CHARACTERISTIC_WRITE);
                intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
                broadcastIntent(intent);
            }
            
            if (!mwState.commandBytes.isEmpty()) writeCommand(mwState);
            else mwState.deviceState= DeviceState.READY;
            if (mwState.deviceState == DeviceState.READY) {
                if (!mwState.readCharUuids.isEmpty()) {                
                    mwState.deviceState= DeviceState.READING_CHARACTERISTICS;
                    readCharacteristic(mwState);
                } else if (mwState.readyToClose) {
                    close(mwState.notifyUser);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            if (characteristic.getValue().length > 1) {
                Register mwRegister= Module.lookupModule(characteristic.getValue()[0])
                        .lookupRegister(characteristic.getValue()[1]);
                
                byte[] bleData;
                if (mwRegister == Temperature.Register.TEMPERATURE) {
                    bleData= Arrays.copyOf(characteristic.getValue(), characteristic.getValue().length + 1);
                    bleData[bleData.length - 1]= mwState.thermistorMode;
                } else {
                    bleData= characteristic.getValue();
                }
                Intent intent= new Intent(Action.NOTIFICATION_RECEIVED);
                intent.putExtra(Extra.CHARACTERISTIC_VALUE, bleData);
                intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
                broadcastIntent(intent);
                
                if (mwState.internalCallbacks.containsKey(mwRegister)) {
                    mwState.internalCallbacks.get(mwRegister).process(bleData);
                }
            }
        }
        
        @Override
        public MetaWearController addModuleCallback(ModuleCallbacks callback) {
            byte moduleOpcode= callback.getModule().opcode;
            if (!mwState.moduleCallbackMap.containsKey(moduleOpcode)) {
                mwState.moduleCallbackMap.put(moduleOpcode, new ArrayList<ModuleCallbacks>());
            }
            mwState.moduleCallbackMap.get(moduleOpcode).add(callback);
            return this;
        }
        @Override
        public void removeModuleCallback(ModuleCallbacks callback) {
            byte callbackOpcode= callback.getModule().opcode;
            
            if (mwState.moduleCallbackMap.containsKey(callbackOpcode)) {
                mwState.moduleCallbackMap.get(callbackOpcode).remove(callback);
            }
        }
        
        @Override
        public MetaWearController addDeviceCallback(DeviceCallbacks callback) {
            mwState.deviceCallbacks.add(callback);
            return this;
        }
        @Override
        public void removeDeviceCallback(DeviceCallbacks callback) {
            mwState.deviceCallbacks.remove(callback);
        }
        @Override
        public void clearCallbacks() {
            mwState.moduleCallbackMap.clear();
            mwState.deviceCallbacks.clear();
        }
        @Override
        public ModuleController getModuleController(Module module) {
            switch (module) {
            case ACCELEROMETER:
                return getAccelerometerModule();
            case DEBUG:
                return getDebugModule();
            case GPIO:
                return getGPIOModule();
            case IBEACON:
                return getIBeaconModule();
            case LED:
                return getLEDDriverModule();
            case MECHANICAL_SWITCH:
                return getMechanicalSwitchModule();
            case NEO_PIXEL:
                return getNeoPixelDriver();
            case TEMPERATURE:
                return getTemperatureModule();
            case HAPTIC:
                return getHapticModule();
            case EVENT:
                return getEventModule();
            case LOGGING:
                return getLoggingModule();
            case DATA_PROCESSOR:
                return getDataProcessorModule();
            }
            return null;
        }

        private ModuleController getDataProcessorModule() {
            if (!modules.containsKey(Module.DATA_PROCESSOR)) {
                modules.put(Module.DATA_PROCESSOR, new DataProcessor() {
                    @Override
                    public void chainFilters(byte srcFilterId, byte srcSize,
                            FilterConfig config) {
                        byte[] attributes= new byte[config.bytes().length + 5];
                        attributes[0]= Register.FILTER_NOTIFICATION.module().opcode;
                        attributes[1]= Register.FILTER_NOTIFICATION.opcode();
                        attributes[2]= srcFilterId;
                        attributes[3]= (byte)((srcSize - 1) << 5);
                        attributes[4]= (byte)(config.type().ordinal() + 1);
                        
                        System.arraycopy(config.bytes(), 0, attributes, 5, config.bytes().length);
                        writeRegister(mwState, Register.FILTER_CREATE, attributes);
                    }

                    @Override
                    public void addFilter(Trigger trigger, FilterConfig config) {
                        byte[] attributes= new byte[config.bytes().length + 5];
                        attributes[0]= trigger.register().module().opcode;
                        attributes[1]= trigger.register().opcode();
                        attributes[2]= trigger.index();
                        attributes[3]= (byte) (trigger.offset() | ((trigger.length() - 1) << 5));
                        attributes[4]= (byte) (config.type().ordinal() + 1);
                        System.arraycopy(config.bytes(), 0, attributes, 5, config.bytes().length);
                        
                        writeRegister(mwState, Register.FILTER_CREATE, attributes);
                    }

                    @Override
                    public void setFilterConfiguration(byte filterId,
                            FilterConfig config) {
                        byte[] bleData= new byte[config.bytes().length + 2];
                        
                        bleData[0]= filterId;
                        bleData[1]= (byte) (config.type().ordinal() + 1);
                        
                        System.arraycopy(config.bytes(), 0, bleData, 2, config.bytes().length);
                        writeRegister(mwState, Register.FILTER_CONFIGURATION, bleData);
                    }
                    
                    @Override
                    public void resetFilterState(byte filterId) {
                        writeRegister(mwState, Register.FILTER_STATE, filterId);
                    }

                    @Override
                    public void removeFilter(byte filterId) {
                        writeRegister(mwState, Register.FILTER_REMOVE, filterId);
                    }

                    @Override
                    public void enableFilterNotify(byte filterId) {
                        writeRegister(mwState, Register.FILTER_NOTIFICATION, (byte) 1);
                        writeRegister(mwState, Register.FILTER_NOTIFY_ENABLE, filterId, (byte) 1);
                    }

                    @Override
                    public void disableFilterNotify(byte filterId) {
                        writeRegister(mwState, Register.FILTER_NOTIFY_ENABLE, filterId, (byte) 0);
                    }

                    @Override
                    public void enableModule() {
                        writeRegister(mwState, Register.ENABLE, (byte) 1);
                    }

                    @Override
                    public void disableModule() {
                        writeRegister(mwState, Register.ENABLE, (byte) 0);
                    }

                    @Override
                    public void filterIdToObject(byte filterId) {
                        readRegister(mwState, Register.FILTER_CREATE, (byte) 1);
                    }

                    @Override
                    public void removeAllFilters() {
                        for(byte filterId= 0; filterId < 16; filterId++) {
                            writeRegister(mwState, Register.FILTER_REMOVE, filterId);
                        }
                    }
                });
            }
            return modules.get(Module.DATA_PROCESSOR);
        }
        private ModuleController getEventModule() {
            if (!modules.containsKey(Module.EVENT)) {
                modules.put(Module.EVENT, new Event() {
                    @Override
                    public void recordMacro(
                            com.mbientlab.metawear.api.Register srcReg) {
                        recordMacro(srcReg, (byte) -1);
                    }
                    
                    @Override
                    public void recordMacro(
                            com.mbientlab.metawear.api.Register srcReg,
                            byte index) {
                        mwState.isRecording= true;
                        
                        mwState.etBuilder= new EventTriggerBuilder(srcReg, index);
                    }

                    @Override
                    public byte stopRecord() {
                        mwState.isRecording= false;
                        for(EventInfo info: mwState.etBuilder.getEventInfo()) {
                            writeRegister(mwState, Register.ADD_ENTRY, info.entry());
                            writeRegister(mwState, Register.EVENT_COMMAND, info.command());
                        }
                        
                        return (byte) mwState.etBuilder.getEventInfo().size();
                    }

                    @Override 
                    public void removeMacros() {
                        for(byte commandId= 0; commandId < 8; commandId++) {
                            writeRegister(mwState, Register.REMOVE_ENTRY, commandId);
                        }
                    }

                    @Override
                    public void commandIdToObject(byte commandId) {
                        readRegister(mwState, Register.ADD_ENTRY, (byte) 1);
                    }

                    @Override
                    public void readCommandBytes(byte commandId) {
                        readRegister(mwState, Register.EVENT_COMMAND, (byte) 1);
                    }

                    @Override
                    public void enableModule() {
                        writeRegister(mwState, Event.Register.EVENT_ENABLE, (byte) 1);
                    }

                    @Override
                    public void disableModule() {
                        writeRegister(mwState, Event.Register.EVENT_ENABLE, (byte) 0);
                    }

                    @Override
                    public void removeCommand(byte commandId) {
                        writeRegister(mwState, Register.REMOVE_ENTRY, commandId);
                    }
                });
            }
            return modules.get(Module.EVENT);
        }
        private ModuleController getLoggingModule() {
            if (!modules.containsKey(Module.LOGGING)) {
                modules.put(Module.LOGGING, new Logging() {
                    @Override
                    public void startLogging() {
                        writeRegister(mwState, Register.ENABLE, (byte) 1);
                    }

                    @Override
                    public void stopLogging() {
                        writeRegister(mwState, Register.ENABLE, (byte) 0);
                    }

                    @Override
                    public void addTrigger(Trigger triggerObj) {
                        writeRegister(mwState, Register.ADD_TRIGGER, triggerObj.register().module().opcode, triggerObj.register().opcode(), 
                                triggerObj.index(), (byte) (triggerObj.offset() | ((triggerObj.length() - 1) << 5)));
                    }

                    @Override
                    public void triggerIdToObject(byte triggerId) {
                        readRegister(mwState, Register.ADD_TRIGGER, triggerId);
                    }

                    @Override
                    public void removeTrigger(byte triggerId) {
                        writeRegister(mwState, Register.REMOVE_TRIGGER, triggerId);
                    }

                    @Override
                    public void readReferenceTick() {
                        readRegister(mwState, Register.TIME, (byte) 0);
                    }

                    @Override
                    public void readTotalEntryCount() {
                        readRegister(mwState, Register.LENGTH, (byte) 0);
                    }

                    @Override
                    public void downloadLog(int nEntries, int notifyIncrement) {
                        ByteBuffer buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
                        buffer.putShort((short) (nEntries & 0xffff)).putShort((short) (notifyIncrement & 0xffff));
                        
                        writeRegister(mwState, Register.READOUT_NOTIFY, (byte) 1);
                        writeRegister(mwState, Register.READOUT_PROGRESS, (byte) 1);
                        writeRegister(mwState, Register.READOUT, buffer.array());
                    }

                    @Override
                    public void removeAllTriggers() {
                        for(byte triggerId= 0; triggerId < 8; triggerId++) {
                            writeRegister(mwState, Register.REMOVE_TRIGGER, triggerId);
                        }
                    }
                    
                });
            }
            return modules.get(Module.LOGGING);
        }
        private ModuleController getAccelerometerModule() {
            if (!modules.containsKey(Module.ACCELEROMETER)) {
                modules.put(Module.ACCELEROMETER, new Accelerometer() {
                    private static final float MMA8452Q_G_PER_STEP= (float) 0.063;
                    private final HashSet<Component> activeComponents= new HashSet<>();
                    private final HashSet<Component> activeNotifications= new HashSet<>();
                    private final HashMap<Component, byte[]> configurations= new HashMap<>();
                    private OutputDataRate accelOdr= OutputDataRate.ODR_100_HZ;;
                    private byte[] globalConfig= new byte[] {0, 0, 0x18, 0, 0};
                    
                    @Override
                    public void enableComponent(Component component, boolean notify) {
                        if (notify) {
                            enableNotification(component);
                        } else {
                            writeRegister(mwState, Register.GLOBAL_ENABLE, (byte)0);
                            writeRegister(mwState, component.enable, (byte)1);
                            writeRegister(mwState, Register.GLOBAL_ENABLE, (byte)1);
                        }
                    }
                    @Override
                    public void disableComponent(Component component) {
                        disableNotification(component);
                    }
                    
                    @Override
                    public void enableNotification(Component component) {
                        writeRegister(mwState, Register.GLOBAL_ENABLE, (byte)0);
                        writeRegister(mwState, component.enable, (byte)1);
                        writeRegister(mwState, component.status, (byte)1);
                        writeRegister(mwState, Register.GLOBAL_ENABLE, (byte)1);
                    }

                    @Override
                    public void disableNotification(Component component) {
                        writeRegister(mwState, Register.GLOBAL_ENABLE, (byte)0);
                        writeRegister(mwState, component.enable, (byte)0);
                        writeRegister(mwState, component.status, (byte)0);
                        writeRegister(mwState, Register.GLOBAL_ENABLE, (byte)1);
                    }

                    @Override
                    public void readComponentConfiguration(Component component) {
                        readRegister(mwState, component.config, (byte)0);
                    }

                    @Override
                    public void setComponentConfiguration(Component component,
                            byte[] data) {
                        writeRegister(mwState, component.config, data);
                    }

                    @Override
                    public void disableDetection(Component component, boolean saveConfig) {
                        activeComponents.remove(component);
                        activeNotifications.remove(component);
                        
                        if (!saveConfig) {
                            if (component == Component.DATA) {
                                globalConfig= new byte[] {0, 0, 0x18, 0, 0};
                            } else {
                                configurations.remove(component);
                            }
                        }
                    }
                    
                    @Override
                    public void disableAllDetection(boolean saveConfig) {
                        activeComponents.clear();
                        activeNotifications.clear();
                        
                        if (!saveConfig) {
                            configurations.clear();
                            globalConfig= new byte[] {0, 0, 0x18, 0, 0};
                        }
                    }
                    
                    @Override
                    public ThresholdConfig enableTapDetection(TapType type, Axis axis) {
                        byte[] tapConfig;
                        
                        if (!configurations.containsKey(Component.PULSE)) {
                            tapConfig= new byte[] {0x40, 0, 0x40, 0x40, 0x50, 0x18, 0x14, 0x3c};
                            configurations.put(Component.PULSE, tapConfig);
                        } else {
                            tapConfig= configurations.get(Component.PULSE);
                            tapConfig[0] &= 0xc0;
                        }
                        
                        switch (type) {
                        case SINGLE_TAP:
                            tapConfig[0] |= 1 << (2 * axis.ordinal());
                            break;
                        case DOUBLE_TAP:
                            tapConfig[0] |= 1 << (1 + 2 * axis.ordinal());
                            break;
                        }
                        
                        configurations.put(Component.PULSE, tapConfig);
                        activeComponents.add(Component.PULSE);
                        activeNotifications.add(Component.PULSE);
                        
                        return new ThresholdConfig() {
                            @Override
                            public ThresholdConfig withThreshold(float gravity) {
                                byte nSteps= (byte) (gravity / MMA8452Q_G_PER_STEP);
                                byte[] config= configurations.get(Component.PULSE);
                                config[2] |= nSteps;
                                config[3] |= nSteps;                             
                                config[4] |= nSteps;
                                
                                return this;
                            }
                            
                            @Override
                            public AccelerometerConfig withSilentMode() {
                                activeNotifications.remove(Component.PULSE);
                                return this;
                            }

                            @Override
                            public byte[] getBytes() {
                                return configurations.get(Component.PULSE);
                            }
                        };
                    }

                    @Override
                    public ThresholdConfig enableShakeDetection(Axis axis) {
                        byte[] shakeConfig;
                        
                        if (!configurations.containsKey(Component.TRANSIENT)) {
                            shakeConfig= new byte[] {0x10, 0, 0x8, 0x5};
                            configurations.put(Component.TRANSIENT, shakeConfig);
                        } else {
                            shakeConfig= configurations.get(Component.TRANSIENT);
                            shakeConfig[0] &= 0xf1;
                        }
                        shakeConfig[0] |= 2 << axis.ordinal();
                        
                        activeComponents.add(Component.TRANSIENT);
                        activeNotifications.add(Component.TRANSIENT);
                        
                        return new ThresholdConfig() {
                            @Override
                            public ThresholdConfig withThreshold(float gravity) {
                                configurations.get(Component.TRANSIENT)[2]= (byte) (gravity / MMA8452Q_G_PER_STEP);
                                return this;
                            }

                            @Override
                            public AccelerometerConfig withSilentMode() {
                                activeNotifications.remove(Component.TRANSIENT);
                                return this;
                            }

                            @Override
                            public byte[] getBytes() {
                                return configurations.get(Component.TRANSIENT);
                            }
                        };
                    }

                    @Override
                    public AccelerometerConfig enableOrientationDetection() {
                        if (!configurations.containsKey(Component.ORIENTATION)) {
                            configurations.put(Component.ORIENTATION, new byte[] {0, (byte) 0xc0, 0xa, 0, 0});
                        }
                        activeComponents.add(Component.ORIENTATION);
                        activeNotifications.add(Component.ORIENTATION);
                        
                        return new AccelerometerConfig() {
                            @Override
                            public AccelerometerConfig withSilentMode() {
                                activeNotifications.remove(Component.ORIENTATION);
                                return this;
                            }

                            @Override
                            public byte[] getBytes() {
                                return configurations.get(Component.ORIENTATION);
                            }
                        };
                    }
                    
                    class FF_Motion_Config implements ThresholdConfig {
                        @Override
                        public ThresholdConfig withThreshold(float gravity) {
                            configurations.get(Component.FREE_FALL)[2]= (byte) (gravity / MMA8452Q_G_PER_STEP);
                            return this;
                        }

                        @Override
                        public AccelerometerConfig withSilentMode() {
                            activeNotifications.remove(Component.FREE_FALL);
                            return this;
                        }

                        @Override
                        public byte[] getBytes() {
                            return configurations.get(Component.FREE_FALL);
                        }
                    }
                    
                    @Override
                    public ThresholdConfig enableFreeFallDetection() {
                        if (!configurations.containsKey(Component.FREE_FALL)) {
                            configurations.put(Component.FREE_FALL, new byte[] {(byte) 0xb8, 0, 0x3, 0xa});
                        } else {
                            byte[] ffConfig= configurations.get(Component.FREE_FALL);
                            ffConfig[0]= (byte) 0xb8;
                            ffConfig[2]= 0x3;
                        }
                        
                        activeComponents.add(Component.FREE_FALL);
                        activeNotifications.add(Component.FREE_FALL);
                        
                        return new FF_Motion_Config();
                    }
                    
                    public ThresholdConfig enableMotionDetection(Axis ... axes) {
                        byte[] motionConfig;
                        if (!configurations.containsKey(Component.FREE_FALL)) {
                            motionConfig= new byte[] {(byte) 0xc0, 0, 0x20, 0xa};
                            configurations.put(Component.FREE_FALL, motionConfig);
                        } else {
                            motionConfig= configurations.get(Component.FREE_FALL);
                            motionConfig[0]= (byte) 0xc0;
                            motionConfig[2]= 0x20;
                        }
                        
                        byte mask= 0;
                        for(Axis axis: axes) {
                            mask |= 1 << (axis.ordinal() + 3);
                        }
                        motionConfig[0] |= mask;
                        
                        activeComponents.add(Component.FREE_FALL);
                        activeNotifications.add(Component.FREE_FALL);
                        
                        return new FF_Motion_Config();
                    }
                    
                    
                    public void startComponents() {
                        float multiplier= (float) Math.pow(2, accelOdr.ordinal() - OutputDataRate.ODR_100_HZ.ordinal());
                        
                        for(Component active: activeComponents) {
                            if (configurations.containsKey(active)) {
                                byte[] config= configurations.get(active);
                                
                                if (accelOdr != OutputDataRate.ODR_100_HZ) {
                                    switch (active) {
                                    case FREE_FALL:
                                        config[3]= (byte) (Math.max(100 / (10 * multiplier), 20));
                                        break;
                                    case ORIENTATION:
                                        config[2]= (byte) (Math.max(100 / (10 * multiplier), 20));
                                        break;
                                    case PULSE:
                                        config[5]= (byte) (Math.min(Math.max(60 / (2.5 * multiplier), 5), 0.625));
                                        config[6]= (byte) (Math.min(Math.max(200 / (5 * multiplier), 10), 1.25));
                                        config[7]= (byte) (Math.min(Math.max(300 / (5 * multiplier), 10), 1.25));
                                        break;
                                    case TRANSIENT:
                                        config[3]= (byte) (Math.max(50 / (10 * multiplier), 20));
                                        break;
                                    default:
                                        break;
                                    }
                                }
                                setComponentConfiguration(active, config);
                            }
                            
                            writeRegister(mwState, active.enable, (byte)1);
                            if (activeNotifications.contains(active)) {
                                writeRegister(mwState, active.status, (byte)1);
                            }
                        }
                        
                        setComponentConfiguration(Component.DATA, globalConfig);
                        writeRegister(mwState, Register.GLOBAL_ENABLE, (byte)1);
                    }
                    
                    public void stopComponents() {
                        writeRegister(mwState, Register.GLOBAL_ENABLE, (byte) 0);
                        
                        for(Component active: activeComponents) {
                            writeRegister(mwState, active.enable, (byte)0);
                            writeRegister(mwState, active.status, (byte)0);
                        }
                    }
                    
                    public void resetAll() {
                        disableAllDetection(false);
                        
                        writeRegister(mwState, Register.GLOBAL_ENABLE, (byte) 0);
                        
                        for(Component it: Component.values()) {
                            writeRegister(mwState, it.enable, (byte)0);
                            writeRegister(mwState, it.status, (byte)0);
                        }
                    }

                    @Override
                    public SamplingConfig enableXYZSampling() {
                        activeComponents.add(Component.DATA);
                        activeNotifications.add(Component.DATA);
                        
                        return new SamplingConfig() {
                            @Override
                            public SamplingConfig withFullScaleRange(
                                    FullScaleRange range) {
                                globalConfig[0] &= 0xfc; 
                                globalConfig[0] |= range.ordinal();
                                return this;
                            }

                            @Override
                            public AccelerometerConfig withSilentMode() {
                                activeNotifications.remove(Component.DATA);
                                return this;
                            }

                            @Override
                            public SamplingConfig withOutputDataRate(OutputDataRate rate) {
                                globalConfig[2] &= 0xc7;
                                globalConfig[2] |= (rate.ordinal() << 3);
                                accelOdr= rate;
                                return this;
                            }

                            @Override
                            public byte[] getBytes() {
                                return globalConfig;
                            }

                            @Override
                            public SamplingConfig withHighPassFilter(byte cutoff) {
                                globalConfig[0] |= 0x10;
                                globalConfig[1] |= cutoff;
                                return this;
                            }
                            
                            @Override
                            public SamplingConfig withHighPassFilter() {
                                globalConfig[0] |= 0x10;
                                return this;
                            }
                            
                            @Override
                            public SamplingConfig withoutHighPassFilter() {
                                globalConfig[0] &= 0xef;
                                return this;
                            }
                        };
                    }
                });
            }
            return modules.get(Module.ACCELEROMETER);
        }
        private ModuleController getDebugModule() {
            if (!modules.containsKey(Module.DEBUG)) {
                modules.put(Module.DEBUG, new Debug() {
                    @Override
                    public void resetDevice() {
                        writeRegister(mwState, Register.RESET_DEVICE);
                    }
                    @Override
                    public void jumpToBootloader() {
                        writeRegister(mwState, Register.JUMP_TO_BOOTLOADER);
                    }
                });
            }
            return modules.get(Module.DEBUG);
        }
        private ModuleController getGPIOModule() {
            if (!modules.containsKey(Module.GPIO)) {
                modules.put(Module.GPIO, new GPIO() {
                    @Override
                    public void readAnalogInput(byte pin, AnalogMode mode) {
                        readRegister(mwState, mode.register, pin);
                    }
                    @Override
                    public void readDigitalInput(byte pin) {
                        readRegister(mwState, Register.READ_DIGITAL_INPUT, pin);
                    }
                    @Override
                    public void setDigitalOutput(byte pin) {
                        writeRegister(mwState, Register.SET_DIGITAL_OUTPUT, pin);
                    }
                    @Override
                    public void clearDigitalOutput(byte pin) {
                        writeRegister(mwState, Register.CLEAR_DIGITAL_OUTPUT, pin);
                    }                
                    @Override
                    public void setDigitalInput(byte pin, PullMode mode) {
                        writeRegister(mwState, mode.register, pin);
                    }
                    @Override
                    public void setPinChangeType(byte pin, ChangeType type) {
                        writeRegister(mwState, Register.SET_PIN_CHANGE, pin, (byte) type.ordinal());
                    }
                    @Override
                    public void enablePinChangeNotification(byte pin) {
                        writeRegister(mwState, Register.PIN_CHANGE_NOTIFY, (byte) 1);
                        writeRegister(mwState, Register.PIN_CHANGE_NOTIFY_ENABLE, pin, (byte) 1);
                    }
                    @Override
                    public void disablePinChangeNotification(byte pin) {
                        writeRegister(mwState, Register.PIN_CHANGE_NOTIFY_ENABLE, pin, (byte) 0);
                    }
                });
            }
            return modules.get(Module.GPIO);
        }
        private ModuleController getIBeaconModule() {
            if (!modules.containsKey(Module.IBEACON)) {
                modules.put(Module.IBEACON, new IBeacon() {
                    @Override
                    public void enableIBeacon() {
                        writeRegister(mwState, Register.ENABLE, (byte)1);                    
                    }
                    @Override
                    public void disableIBecon() {
                        writeRegister(mwState, Register.ENABLE, (byte)0);
                    }
                    @Override
                    public IBeacon setUUID(UUID uuid) {
                        byte[] uuidBytes= ByteBuffer.wrap(new byte[16])
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .putLong(uuid.getLeastSignificantBits())
                                .putLong(uuid.getMostSignificantBits())
                                .array();
                        writeRegister(mwState, Register.ADVERTISEMENT_UUID, uuidBytes);
                        return this;
                    }
                    @Override
                    public void readSetting(Register register) {
                        readRegister(mwState, register);
                    }
                    @Override
                    public IBeacon setMajor(short major) {
                        writeRegister(mwState, Register.MAJOR, (byte)(major & 0xff), (byte)((major >> 8) & 0xff));
                        return this;
                    }
                    @Override
                    public IBeacon setMinor(short minor) {
                        writeRegister(mwState, Register.MINOR, (byte)(minor & 0xff), (byte)((minor >> 8) & 0xff));
                        return this;
                    }
                    @Override
                    public IBeacon setCalibratedRXPower(byte power) {
                        writeRegister(mwState, Register.RX_POWER, power);
                        return this;
                    }
                    @Override
                    public IBeacon setTXPower(byte power) {
                        writeRegister(mwState, Register.TX_POWER, power);
                        return this;
                    }
                    @Override
                    public IBeacon setAdvertisingPeriod(short freq) {
                        writeRegister(mwState, Register.ADVERTISEMENT_PERIOD, (byte)(freq & 0xff), (byte)((freq >> 8) & 0xff));
                        return this;
                    }
                });
            }
            return modules.get(Module.IBEACON);
        }
        private ModuleController getLEDDriverModule() {
            if (!modules.containsKey(Module.LED)) {
                modules.put(Module.LED, new LED() {
                    public void play(boolean autoplay) {
                        writeRegister(mwState, Register.PLAY, (byte)(autoplay ? 2 : 1));
                    }
                    public void pause() {
                        writeRegister(mwState, Register.PLAY, (byte)0);
                    }
                    public void stop(boolean resetChannels) {
                        writeRegister(mwState, Register.STOP, (byte)(resetChannels ? 1 : 0));
                    }
                    
                    public ChannelDataWriter setColorChannel(final ColorChannel color) {
                        return new ChannelDataWriter() {
                            private byte[] channelData= new byte[15];
                            @Override
                            public ColorChannel getChannel() {
                                return color;
                            }

                            @Override
                            public ChannelDataWriter withHighIntensity(byte intensity) {
                                channelData[2]= intensity;
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withLowIntensity(byte intensity) {
                                channelData[3]= intensity;
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withRiseTime(short time) {
                                channelData[5]= (byte)((time >> 8) & 0xff);
                                channelData[4]= (byte)(time & 0xff);
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withHighTime(short time) {
                                channelData[7]= (byte)((time >> 8) & 0xff);
                                channelData[6]= (byte)(time & 0xff);
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withFallTime(short time) {
                                channelData[9]= (byte)((time >> 8) & 0xff);
                                channelData[8]= (byte)(time & 0xff);
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withPulseDuration(short period) {
                                channelData[11]= (byte)((period >> 8) & 0xff);
                                channelData[10]= (byte)(period & 0xff);
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withPulseOffset(short offset) {
                                channelData[13]= (byte)((offset >> 8) & 0xff);
                                channelData[12]= (byte)(offset & 0xff);
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withRepeatCount(byte count) {
                                channelData[14]= count;;
                                return this;
                            }

                            @Override
                            public void commit() {
                                channelData[0]= (byte)(color.ordinal());
                                channelData[1]= 0x2;    ///< Keep it set to flash for now
                                writeRegister(mwState, Register.MODE, channelData);
                            }
                        };
                    }
                });
            }
            return modules.get(Module.LED);
        }
        private ModuleController getMechanicalSwitchModule() {
            if (!modules.containsKey(Module.MECHANICAL_SWITCH)) {
                modules.put(Module.MECHANICAL_SWITCH, new MechanicalSwitch() {
                    @Override
                    public void enableNotification() {
                        writeRegister(mwState, Register.SWITCH_STATE, (byte)1);
                    }
                    @Override
                    public void disableNotification() {
                        writeRegister(mwState, Register.SWITCH_STATE, (byte)0);
                    }
                });
            }
            return modules.get(Module.MECHANICAL_SWITCH);
        }
        private ModuleController getNeoPixelDriver() {
            if (!modules.containsKey(Module.NEO_PIXEL)) {
                modules.put(Module.NEO_PIXEL, new NeoPixel() {
                    @Override
                    public void readStrandState(byte strand) {
                        readRegister(mwState, Register.INITIALIZE, strand);
                    }
                    @Override
                    public void readHoldState(byte strand) {
                        readRegister(mwState, Register.HOLD, strand);
                    }
                    @Override
                    public void readPixelState(byte strand, byte pixel) {
                        readRegister(mwState, Register.PIXEL, strand, pixel);
                    }
                    @Override
                    public void readRotationState(byte strand) {
                        readRegister(mwState, Register.ROTATE, strand);
                    }
                    @Override
                    public void initializeStrand(byte strand, ColorOrdering ordering,
                            StrandSpeed speed, byte ioPin, byte length) {
                        writeRegister(mwState, Register.INITIALIZE, strand, 
                                (byte)(speed.ordinal() << 2 | ordering.ordinal()), ioPin, length);
                        
                    }
                    @Override
                    public void holdStrand(byte strand, byte holdState) {
                        writeRegister(mwState, Register.HOLD, strand, holdState);
                        
                    }
                    @Override
                    public void clearStrand(byte strand, byte start, byte end) {
                        writeRegister(mwState, Register.CLEAR, strand, start, end);
                        
                    }
                    @Override
                    public void setPixel(byte strand, byte pixel, byte red,
                            byte green, byte blue) {
                        writeRegister(mwState, Register.PIXEL, strand, pixel, red, green, blue);
                        
                    }
                    @Override
                    public void rotateStrand(byte strand, RotationDirection direction, byte repetitions,
                            short delay) {
                        writeRegister(mwState, Register.ROTATE, strand, (byte)direction.ordinal(), repetitions, 
                                (byte)(delay & 0xff), (byte)(delay >> 8 & 0xff));
                    }
                    @Override
                    public void deinitializeStrand(byte strand) {
                        writeRegister(mwState, Register.DEINITIALIZE, strand);
                        
                    }
                });
            }
            return modules.get(Module.NEO_PIXEL);
        }
        private ModuleController getTemperatureModule() {
            if (!modules.containsKey(Module.TEMPERATURE)) {
                modules.put(Module.TEMPERATURE, new Temperature() {
                    @Override
                    public void readTemperature() {
                        readRegister(mwState, Register.TEMPERATURE, (byte) 0);
                    }

                    @Override
                    public SamplingConfigBuilder enableSampling() {
                        return new SamplingConfigBuilder() {
                            private final byte[] samplingConfig= new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
                            private boolean silent= false;
                            
                            @Override
                            public SamplingConfigBuilder withSilentMode() {
                                silent= true;
                                return this;
                            }
                            
                            @Override
                            public SamplingConfigBuilder withSampingPeriod(
                                    int period) {
                                short shortPeriod= (short) (period & 0xffff);
                                
                                samplingConfig[1]= (byte)((shortPeriod >> 8) & 0xff);
                                samplingConfig[0]= (byte)(shortPeriod & 0xff);
                                
                                return this;
                            }

                            @Override
                            public SamplingConfigBuilder withTemperatureDelta(
                                    float delta) {
                                short tempTicks= (short) (delta * 4);
                                
                                samplingConfig[3]= (byte)((tempTicks >> 8) & 0xff);
                                samplingConfig[2]= (byte)(tempTicks & 0xff);
                                
                                return this;
                            }

                            @Override
                            public SamplingConfigBuilder withTemperatureBoundary(
                                    float lower, float upper) {
                                short lowerTicks= (short) (lower * 4), upperTicks= (short) (upper * 4);
                                
                                samplingConfig[5]= (byte)((lowerTicks >> 8) & 0xff);
                                samplingConfig[4]= (byte)(lowerTicks & 0xff);
                                samplingConfig[7]= (byte)((upperTicks >> 8) & 0xff);
                                samplingConfig[6]= (byte)(upperTicks & 0xff);
                                
                                return this;
                            }

                            @Override
                            public void commit() {
                                writeRegister(mwState, Register.MODE, samplingConfig);
                                if (!silent) {
                                    writeRegister(mwState, Register.TEMPERATURE, (byte) 1);
                                    writeRegister(mwState, Register.DELTA_TEMP, (byte) 1);
                                    writeRegister(mwState, Register.THRESHOLD_DETECT, (byte) 1);
                                }
                            }
                        };
                    }

                    @Override
                    public void disableSampling() {
                        writeRegister(mwState, Register.MODE, new byte[] {0, 0, 0, 0, 0, 0, 0, 0});
                        writeRegister(mwState, Register.TEMPERATURE, (byte) 0);
                        writeRegister(mwState, Register.DELTA_TEMP, (byte) 0);
                        writeRegister(mwState, Register.THRESHOLD_DETECT, (byte) 0);
                    }
                    
                    @Override
                    public void enableThermistorMode(byte analogReadPin, byte pulldownPin) {
                        mwState.thermistorMode= (byte) 1;
                        writeRegister(mwState, Register.THERMISTOR_MODE, (byte) 1, analogReadPin, pulldownPin);
                    }
                    
                    @Override
                    public void disableThermistorMode() {
                        mwState.thermistorMode= (byte) 0;
                        writeRegister(mwState, Register.THERMISTOR_MODE, (byte) 0);
                    }
                });
            }
            return modules.get(Module.TEMPERATURE);
        }
        private ModuleController getHapticModule() {
            if (!modules.containsKey(Module.HAPTIC)) {
                modules.put(Module.HAPTIC, new Haptic() {
                    private final static float DEFAULT_DUTY_CYCLE= 100.f;
                    
                    @Override
                    public void startMotor(short pulseWidth) {
                      
                        startMotor(DEFAULT_DUTY_CYCLE, pulseWidth);
                    }

                    @Override
                    public void startBuzzer(short pulseWidth) {
                        writeRegister(mwState, Register.PULSE, (byte)127, (byte)(pulseWidth & 0xff), 
                                (byte)((pulseWidth >> 8) & 0xff), (byte)1);
                    }

                    @Override
                    public void startMotor(float dutyCycle, short pulseWidth) {
                        short converted= (short)((dutyCycle / 100.f) * 248);
                        writeRegister(mwState, Register.PULSE, (byte)(converted & 0xff), (byte)(pulseWidth & 0xff), 
                                (byte)((pulseWidth >> 8) & 0xff), (byte)0);
                    }
                });
            }
            return modules.get(Module.HAPTIC);
        }
        @Override
        public void readDeviceInformation() {
            for(GATTCharacteristic it: DeviceInformation.values()) {
                mwState.readCharUuids.add(it);
            }
            if (mwState.deviceState == DeviceState.READY) {
                mwState.deviceState= DeviceState.READING_CHARACTERISTICS;
                readCharacteristic(mwState);
            }
        }
        @Override
        public void readBatteryLevel() {
            mwState.readCharUuids.add(Battery.BATTERY_LEVEL);
            if (mwState.deviceState == DeviceState.READY) {
                mwState.deviceState= DeviceState.READING_CHARACTERISTICS;
                readCharacteristic(mwState);
            }
        }
        
        @Override
        public void readRemoteRSSI() {
            if (mwState.mwGatt != null) {
                mwState.mwGatt.readRemoteRssi();
            }
        }
        
        @Override
        public boolean isConnected() {
            return mwState.connected;
        }
        
        @Override
        public void setRetainState(boolean retain) {
            mwState.retainState= retain;
        }
    };
    /**
     * Gets a controller for the MetaWear board, in single MetaWear mode.  The MetaWearController returned 
     * does not support the {@link MetaWearController#connect()}, 
     * {@link MetaWearController#close(boolean)}, and {@link MetaWearController#reconnect(boolean)} functions.  
     * You will need to use the deprecated variants of those functions to modify the connection state.
     * @return MetaWear controller, in single MetaWear mode, to interact with the board
     * @deprecated As of v1.3.  Use {@link #getMetaWearController(BluetoothDevice)} instead
     */
    @Deprecated
    public MetaWearController getMetaWearController() {
        if (singleMwState == null) {
            singleMwState= new MetaWearState();
        }
        if (singleController == null) {
            singleController= new MetaWearControllerImpl(singleMwState) {
                @Override
                public void connect() {
                    throw new UnsupportedOperationException("This function is not supported in single metawear mode");
                }
                @Override
                public void reconnect(boolean notify) {
                    throw new UnsupportedOperationException("This function is not supported in single metawear mode");
                }

                @Override
                public void close(boolean notify) {
                    throw new UnsupportedOperationException("This function is not supported in single metawear mode");
                }
                @Override
                public void close(boolean notify, boolean wait) {
                    throw new UnsupportedOperationException("This function is not supported in single metawear mode");
                }
            };
        }
        
        return singleController;
    }
    /**
     * Gets a controller for a specific MetaWear board.  Modifying connection state must be done with 
     * the {@link MetaWearController#connect()}, {@link MetaWearController#close(boolean)}, and 
     * {@link MetaWearController#reconnect(boolean)} functions rather 
     * than their deprecated variants
     * @param mwBoard MetaWear board to interact with
     * @return Controller attached to the specific board
     */
    public MetaWearController getMetaWearController(final BluetoothDevice mwBoard) {
        if (!metaWearStates.containsKey(mwBoard)) {
            metaWearStates.put(mwBoard, new MetaWearState(mwBoard));
        }
        final MetaWearState mwState= metaWearStates.get(mwBoard);
        if (mwState.mwController == null) {
            mwState.mwController= new MetaWearControllerImpl(metaWearStates.get(mwBoard)) {
                @Override
                public void connect() {
                    if (!isConnected()) {
                        if (!metaWearStates.containsKey(mwBoard)) {
                            metaWearStates.put(mwBoard, mwState);
                        }
                        mwState.notifyUser= false;
                        mwState.readyToClose= false;
                        mwState.deviceState= null;
                        mwState.mwGatt= mwState.mwBoard.connectGatt(MetaWearBleService.this, false, this);
                    }
                }
                
                @Override
                public void reconnect(boolean notify) {
                    close(notify);
                    connect();
                }

                @Override
                public void close(boolean notify) {
                    MetaWearBleService.this.close(mwState);
                    if (notify) {
                        Intent intent= new Intent(Action.DEVICE_DISCONNECTED);
                        intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
                        intent.putExtra(Extra.EXPLICIT_CLOSE, true);
                        broadcastIntent(intent);
                    } else {
                        if (!mwState.retainState) {
                            mwState.resetState();
                            metaWearStates.remove(mwState.mwBoard);
                        }
                    }
                }

                @Override
                public void close(boolean notify, boolean wait) {
                    if (wait) {
                        mwState.notifyUser= notify;
                        mwState.readyToClose= true;
                    }
                }
            };
        }
        return mwState.mwController;
    }
    
    /**
     * Set how intents are broadcasted from the service.  Default behaviour is to broadcast 
     * to all receivers 
     * @param useFlag True if {@link android.support.v4.content.LocalBroadcastManager} should 
     * be used to broadcast intents
     */
    public void useLocalBroadcasterManager(boolean useFlag) {
        useLocalBroadcastMnger= useFlag;
    }
    
    /**
     * Connect to the GATT service on the MetaWear device.  This version of the function is for the old 
     * single MetaWear mode.  
     * @param metaWearBoard MetaWear board to connect to
     * @deprecated As of v1.3.  Use {@link MetaWearController#connect()} and retrieve a MetaWearController 
     * with {@link #getMetaWearController(BluetoothDevice)}
     */
    @Deprecated
    public void connect(BluetoothDevice metaWearBoard) {
        if (singleMwState == null) {
            singleMwState= new MetaWearState(metaWearBoard);
        } else {
            singleMwState.mwBoard= metaWearBoard;
        }
        
        if (!metaWearStates.containsKey(metaWearBoard)) {
            close(singleMwState);
            metaWearStates.clear();
            metaWearStates.put(metaWearBoard, singleMwState);
            singleMwState.mwGatt= metaWearBoard.connectGatt(this, false, singleMwState.mwController);
        } else {
            reconnect();
        }
        
    }
    /**
     * Restarts the connection to a board.  This version of the function is for the old 
     * single MetaWear mode.  
     * @deprecated As of v1.3.  Use {@link MetaWearController#reconnect(boolean)} and retrieve a MetaWearController 
     * with {@link #getMetaWearController(BluetoothDevice)}
     */
    @Deprecated
    public void reconnect() {
        if (singleMwState != null && singleMwState.mwBoard != null && singleMwState.mwGatt != null) {
            singleMwState.mwGatt.close();
            singleMwState.mwGatt= null;
            singleMwState.connected= false;
            singleMwState.deviceState= null;
            
            singleMwState.mwGatt= singleMwState.mwBoard.connectGatt(this, false, singleMwState.mwController);
        }
    }
    /**
     * Disconnects from the board.  This version of the function is for the old 
     * single MetaWear mode.
     * @deprecated As of v1.3.  Use {@link MetaWearController#close(boolean)} and 
     * retrieve a MetaWearController with {@link #getMetaWearController(BluetoothDevice)}
     */
    @Deprecated
    public void disconnect() {
        if (singleMwState != null && singleMwState.mwGatt != null) {
            singleMwState.mwGatt.disconnect();
        }
    }
    
    /** 
     * Close the GATT service and free up resources.  This version of the function is for
     * single MetaWear mode.  
     * @param notify True if the {@link MetaWearController.DeviceCallbacks#disconnected()} 
     * function should be called
     * @deprecated As of v1.3.  Use {@link MetaWearController#close(boolean)}  and retrieve 
     * a MetaWearController with {@link #getMetaWearController(BluetoothDevice)}
     */
    @Deprecated
    public void close(boolean notify) {
        if (singleMwState != null && singleMwState.mwGatt != null) {
            singleMwState.mwGatt.close();
            singleMwState.mwGatt= null;
            singleMwState.connected= false;
            
            if (notify) {
                Intent intent= new Intent(Action.DEVICE_DISCONNECTED);
                intent.putExtra(Extra.BLUETOOTH_DEVICE, singleMwState.mwBoard);
                intent.putExtra(Extra.EXPLICIT_CLOSE, true);
                broadcastIntent(intent);
            } else {
                if (!singleMwState.retainState) {
                    singleMwState.resetState();
                    metaWearStates.remove(singleMwState.mwBoard);
                }
            }
        }
    }
    private void close(MetaWearState mwState) {
        if (mwState != null) {
            if (mwState.mwGatt != null) {
                mwState.mwGatt.close();
                mwState.mwGatt= null;
            }
        
            mwState.deviceState= null;
            mwState.connected= false;
        }
    }
    /**
     * Close the GATT service and free up resources.  This version of the function is for  
     * single MetaWear mode.
     * @deprecated As of v1.3.  Use {@link MetaWearController#close(boolean)} and retrieve 
     * a MetaWearController with {@link #getMetaWearController(BluetoothDevice)}
     */
    public void close() {
        close(false);
    }

    /** Binding between the Intent and this service */
    private final Binder serviceBinder= new LocalBinder();

    /** Dummy class for getting the MetaWear BLE service from its binder */
    public class LocalBinder extends Binder {
        /**
         * Get the MetaWearBLEService object
         * @return MetaWearBLEService object
         */
        public MetaWearBleService getService() {
            return MetaWearBleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onDestroy() {
        for(Entry<BluetoothDevice, MetaWearState> it: metaWearStates.entrySet()) {
            close(it.getValue());
        }
        close(singleMwState);
        metaWearStates.clear();
        
        super.onDestroy();
    }

    private void readRegister(MetaWearState mwState,
            com.mbientlab.metawear.api.Register register,
            byte... parameters) {
        if (!mwState.readyToClose) {
            byte[] bleData= Registers.buildReadCommand(register, parameters);
            
            if (mwState.isRecording) {
                mwState.etBuilder.withDestRegister(register, 
                        Arrays.copyOfRange(bleData, 2, bleData.length), true);
            } else {
                queueCommand(mwState, bleData);
            }
        }
    }

    private void writeRegister(MetaWearState mwState,
            com.mbientlab.metawear.api.Register register,
            byte... data) {
        if (!mwState.readyToClose) {
            byte[] bleData= Registers.buildWriteCommand(register, data);
            
            if (mwState.isRecording) {
                mwState.etBuilder.withDestRegister(register, 
                        Arrays.copyOfRange(bleData, 2, bleData.length), false);
            } else {
                queueCommand(mwState, bleData);
            }
        }
    }

    /**
     * @param module
     * @param registerOpcode
     * @param data
     */
    private void queueCommand(MetaWearState mwState, byte[] command) {
        mwState.commandBytes.add(command);
        if (mwState.deviceState == DeviceState.READY) {
            mwState.deviceState= DeviceState.WRITING_CHARACTERISTICS;
            writeCommand(mwState);
        }
    }
}
