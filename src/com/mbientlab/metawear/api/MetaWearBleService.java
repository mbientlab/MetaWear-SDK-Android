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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.mbientlab.metawear.api.GATT.GATTCharacteristic;
import com.mbientlab.metawear.api.GATT.GATTService;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks.GattOperation;
import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.characteristic.*;
import com.mbientlab.metawear.api.controller.*;
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
import android.os.Build;
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
        public boolean execAction();
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
        
        private boolean hasParamConfig;
        private byte length, offset, destOffset;
        
        public EventTriggerBuilder(Register srcReg, byte index) {
            this.srcReg= srcReg;
            this.index= index;
            hasParamConfig= false;
        }
        
        public void setParameterConfig(byte length, byte offset, byte destOffset) {
            hasParamConfig= true;
            this.length= length;
            this.offset= offset;
            this.destOffset= destOffset;
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
                    if (hasParamConfig) {
                        return new byte[] {srcReg.module().opcode, srcReg.opcode(), index,
                                destReg.module().opcode, destOpcode, (byte) command.length, 
                                (byte) (0x01 | ((length << 1) & 0xff) | ((offset << 4) & 0xff)), 
                                destOffset};
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
    private final static int RECORDING_EVENT= 0x1, RECORDING_MACRO= 0x2;
    private final static float[][] motionCountSteps, transientSteps;
    private final static float[][][] pulseTmltSteps, pulseLtcySteps, pulseWindSteps;
    private final static float[] sleepCountSteps;
    
    static {
        motionCountSteps= new float[][] {
                {1.25f, 2.5f, 5, 10, 20, 20, 20, 20},
                {1.25f, 2.5f, 5, 10, 20, 80, 80, 80},
                {1.25f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f},
                {1.25f, 2.5f, 5, 10, 20, 80, 160, 160}
        };
        
        pulseTmltSteps= new float[][][] {
                {{0.625f, 0.625f, 1.25f, 2.5f, 5, 5, 5, 5},
                {0.625f, 0.625f, 1.25f, 2.5f, 5, 20, 20, 20},
                {0.625f, 0.625f, 0.625f, 0.625f, 0.625f, 0.625f, 0.625f, 0.625f},
                {0.625f, 1.25f, 2.5f, 5, 10, 40, 40, 40}},
                {{1.25f, 2.5f, 5, 10, 20, 20, 20, 20},
                {1.25f, 2.5f, 5, 10, 20, 80, 80, 80},
                {1.25f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f},
                {1.25f, 2.5f, 5, 10, 20, 80, 160, 160}}
        };
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
        
        sleepCountSteps= new float[] {320, 320, 320, 320, 320, 320, 320, 640};
    }
    
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
            queuedGattActions.clear();
            numGattActions.set(0);
            numDescriptors.set(0);
        }

        public byte[] globalConfig= null;
        public BluetoothDevice mwBoard;
        public int isRecording= 0;
        public EventTriggerBuilder etBuilder;
        public boolean connected, retainState= true, readyToClose, notifyUser;
        public MetaWearControllerImpl mwController= null;
        public BluetoothGatt mwGatt= null;
        public DeviceState deviceState= null;
        public final AtomicInteger numGattActions= new AtomicInteger(0), numDescriptors= new AtomicInteger(0);
        public final ConcurrentLinkedQueue<GattAction> queuedGattActions= new ConcurrentLinkedQueue<>();
        public final HashMap<Register, InternalCallback> internalCallbacks= new HashMap<>();
        public final HashMap<Byte, ArrayList<ModuleCallbacks>> moduleCallbackMap= new HashMap<>();
        public final HashSet<DeviceCallbacks> deviceCallbacks= new HashSet<>();
    }
    
    /** GATT connection to the ble device */
    private static final HashMap<BluetoothDevice, MetaWearState> metaWearStates= new HashMap<>();
    
    private final AtomicBoolean isExecGattActions= new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<GattAction> gattActions= new ConcurrentLinkedQueue<>();
    private MetaWearState singleMwState= null;
    private MetaWearControllerImpl singleController= null;
    private LocalBroadcastManager localBroadcastMngr;
    
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
                            if (!mwState.retainState) {
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
        if (localBroadcastMngr != null) {
            localBroadcastMngr.sendBroadcast(intent);
        } else {
            sendBroadcast(intent);
        }
    }
    
    private void execGattAction(boolean fromCallback) {
        if (!gattActions.isEmpty() && (fromCallback || !isExecGattActions.get())) {
            isExecGattActions.set(true);
            boolean lastResult= false;
            while(!gattActions.isEmpty() && (lastResult= gattActions.poll().execAction()) == false) { }
            
            if (!lastResult && gattActions.isEmpty()) {
                isExecGattActions.set(false);
            }
        }
    }

    /**
     * Read a characteristic from MetaWear.
     * An intent with the action CHARACTERISTIC_READ will be broadcasted.
     * @see Action.BluetoothLe#ACTION_CHARACTERISTIC_READ
     */
    private void readCharacteristic(final MetaWearState mwState, final GATTCharacteristic gattChar) {
        queueGattAction(mwState, new GattAction() {
            @Override
            public boolean execAction() {
                if (mwState.mwGatt != null) {
                    BluetoothGattService service= mwState.mwGatt.getService(gattChar.gattService().uuid());
            
                    BluetoothGattCharacteristic characteristic= service.getCharacteristic(gattChar.uuid());
                    mwState.mwGatt.readCharacteristic(characteristic);
                    return true;
                }
                return false;
            }
        });
    }
    
    private void queueGattAction(final MetaWearState mwState, GattAction action) {
        mwState.numGattActions.incrementAndGet();
        if (mwState.deviceState != DeviceState.READY) {
            mwState.queuedGattActions.add(action);
        } else {
            gattActions.add(action);
        }
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
                    if (mwState.numDescriptors.get() == 0 && mwState.numGattActions.get() == 0)  {
                        if (mwState.mwGatt != null) {
                            mwState.mwGatt.close();
                            mwState.mwGatt= null;
                        }
                        mwState.deviceState= null;
                        mwState.connected= false;
                    } else {
                        mwState.readyToClose= true;
                    }
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
                    for(final BluetoothGattCharacteristic characteristic: service.getCharacteristics()) {
                        int charProps = characteristic.getProperties();
                        if ((charProps & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            mwState.numDescriptors.incrementAndGet();
                            gattActions.add(new GattAction() {
                                @Override
                                public boolean execAction() {
                                    if (mwState.mwGatt != null) {
                                        mwState.mwGatt.setCharacteristicNotification(characteristic, true);
                                        BluetoothGattDescriptor descriptor= characteristic.getDescriptor(CHARACTERISTIC_CONFIG);
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        mwState.mwGatt.writeDescriptor(descriptor);
                                        return true;
                                    }
                                    return false;
                                }
                            });
                        }
                    }
                }
                mwState.deviceState= DeviceState.ENABLING_NOTIFICATIONS;
                execGattAction(false);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor, int status) {
            mwState.numDescriptors.decrementAndGet();
            isExecGattActions.set(!gattActions.isEmpty());
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Intent intent= new Intent(Action.GATT_ERROR);
                intent.putExtra(Extra.STATUS, status);
                intent.putExtra(Extra.GATT_OPERATION, DeviceCallbacks.GattOperation.DESCRIPTOR_WRITE);
                intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
                broadcastIntent(intent);
            }
            
            if (mwState.numDescriptors.get() == 0) {
                if (mwState.readyToClose) {
                    close(mwState.notifyUser);
                } else {
                    mwState.internalCallbacks.put(Accelerometer.Register.DATA_SETTINGS, new InternalCallback() {
                        @Override
                        public void process(byte[] data) {                        
                            mwState.globalConfig= new byte[data.length - 2];
                            System.arraycopy(data, 2, mwState.globalConfig, 0, mwState.globalConfig.length);
                            mwState.internalCallbacks.remove(Accelerometer.Register.DATA_SETTINGS);
                        }
                    });
                    queueRegisterAction(mwState, false, Accelerometer.Register.DATA_SETTINGS);
                    
                    mwState.deviceState= DeviceState.READY;
                    gattActions.addAll(mwState.queuedGattActions);
                    mwState.queuedGattActions.clear();
                }
            }
            
            execGattAction(true);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            mwState.numGattActions.decrementAndGet();
            isExecGattActions.set(!gattActions.isEmpty());
            
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
            
            if (mwState.readyToClose && mwState.numGattActions.get() == 0) {
                close(mwState.notifyUser);
            }
            execGattAction(true);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            mwState.numGattActions.decrementAndGet();
            isExecGattActions.set(!gattActions.isEmpty());
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Intent intent= new Intent(Action.GATT_ERROR);
                intent.putExtra(Extra.STATUS, status);
                intent.putExtra(Extra.GATT_OPERATION, DeviceCallbacks.GattOperation.CHARACTERISTIC_WRITE);
                intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
                broadcastIntent(intent);
            }
            
            if (mwState.readyToClose && mwState.numGattActions.get() == 0) {
                close(mwState.notifyUser);
            }
            execGattAction(true);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            if (characteristic.getValue().length > 1) {
                Register mwRegister= Module.lookupModule(characteristic.getValue()[0])
                        .lookupRegister(characteristic.getValue()[1]);
                
                Intent intent= new Intent(Action.NOTIFICATION_RECEIVED);
                intent.putExtra(Extra.CHARACTERISTIC_VALUE, characteristic.getValue());
                intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
                broadcastIntent(intent);
                
                if (mwState.internalCallbacks.containsKey(mwRegister)) {
                    mwState.internalCallbacks.get(mwRegister).process(characteristic.getValue());
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
            case TIMER:
                return getTimerModule();
            case I2C:
                return getI2CModule();
            case SETTINGS:
                return getSettingsModule();
            case MACRO:
                return getMacroModule();
            }
            return null;
        }

        private ModuleController getMacroModule() {
            if (!modules.containsKey(Module.MACRO)) {
                modules.put(Module.MACRO, new Macro() {
                    @Override
                    public void enableMacros() {
                        queueRegisterAction(mwState, true, Register.ENABLE, (byte) 1);
                    }
                    
                    @Override
                    public void disableMacros() {
                        queueRegisterAction(mwState, true, Register.ENABLE, (byte) 0);
                    }
                    
                    @Override
                    public void recordMacro(boolean executeOnBoot) {
                        queueRegisterAction(mwState, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, 
                                true, Register.ADD_MACRO, (byte) (executeOnBoot ? 1 : 0));
                        mwState.isRecording|= RECORDING_MACRO;
                    }

                    @Override
                    public void stopRecord() {
                        mwState.isRecording&= ~RECORDING_MACRO;
                        queueRegisterAction(mwState, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                                true, Register.END_MACRO);
                    }

                    @Override
                    public void readMacroInfo(byte macroId) {
                        queueRegisterAction(mwState, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                                false, Register.ADD_MACRO, macroId);
                    }
                    
                    @Override
                    public void readMacroCommand(byte macroId, byte commandNum) {
                        queueRegisterAction(mwState, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                                false, Register.ADD_COMMAND, macroId, commandNum);
                    }
                    
                    @Override
                    public void executeMacro(byte macroId) {
                        queueRegisterAction(mwState, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                                true, Register.EXEC_MACRO, macroId);
                    }

                    @Override
                    public void enableProgressNotfiy(byte macroId) {
                        queueRegisterAction(mwState, true, Register.MACRO_NOTIFY, (byte) 1);
                        queueRegisterAction(mwState, true, Register.MACRO_NOTIFY_ENABLE, macroId, (byte) 1);
                    }
                    
                    @Override
                    public void disableProgressNotfiy(byte macroId) {
                        queueRegisterAction(mwState, true, Register.MACRO_NOTIFY_ENABLE, macroId, (byte) 0);
                    }

                    @Override
                    public void eraseMacros() {
                        queueRegisterAction(mwState, true, Register.ERASE_ALL);
                    }
                });
            }
            return modules.get(Module.MACRO);
        }
        private ModuleController getSettingsModule() {
            if (!modules.containsKey(Module.SETTINGS)) {
                modules.put(Module.SETTINGS, new Settings() {
                    @Override
                    public Settings setDeviceName(String name) {
                        try {
                            queueRegisterAction(mwState, true, Register.DEVICE_NAME, name.getBytes("US-ASCII"));
                        } catch (UnsupportedEncodingException e) {
                            queueRegisterAction(mwState, true, Register.DEVICE_NAME, name.getBytes());
                        }
                        return this;
                    }

                    @Override
                    public Settings setAdvertisingInterval(short interval, byte timeout) {
                        byte[] params= new byte[] {(byte) (interval & 0xff), 
                                (byte) ((interval >> 8) & 0xff), timeout};
                        queueRegisterAction(mwState, true, Register.ADVERTISING_INTERVAL, params);
                        return this;
                    }

                    @Override
                    public Settings setTXPower(byte power) {
                        queueRegisterAction(mwState, true, Register.TX_POWER, power);
                        return this;
                    }

                    @Override
                    public void removeBond() {
                        queueRegisterAction(mwState, true, Register.DELETE_BOND, (byte) 1);
                    }

                    @Override
                    public void keepBond() {
                        queueRegisterAction(mwState, true, Register.DELETE_BOND, (byte) 0);
                    }

                    @Override
                    public void startAdvertisement() {
                        queueRegisterAction(mwState, true, Register.START_ADVERTISEMENT);
                    }

                    @Override
                    public void initiateBonding() {
                        queueRegisterAction(mwState, true, Register.INIT_BOND);
                    }

                    @Override
                    public void readDeviceName() {
                        queueRegisterAction(mwState, false, Register.DEVICE_NAME);
                    }

                    @Override
                    public void readAdvertisingParams() {
                        queueRegisterAction(mwState, false, Register.ADVERTISING_INTERVAL);
                    }

                    @Override
                    public void readTxPower() {
                        queueRegisterAction(mwState, false, Register.TX_POWER);
                    }
                });
            };
            return modules.get(Module.SETTINGS);
        }
        private ModuleController getI2CModule() {
            if (!modules.containsKey(Module.I2C)) {
                modules.put(Module.I2C, new I2C() {
                    @Override
                    public void writeData(byte deviceAddr, byte registerAddr,
                            byte index, byte[] data) {
                        byte[] params= new byte[data.length + 4];
                        params[0]= deviceAddr;
                        params[1]= registerAddr;
                        params[2]= index;
                        params[3]= (byte) data.length;
                        System.arraycopy(data, 0, params, 4, data.length);
                        
                        queueRegisterAction(mwState, true, Register.READ_WRITE, params);
                    }

                    @Override
                    public void readData(byte deviceAddr, byte registerAddr,
                            byte index, byte numBytes) {
                        queueRegisterAction(mwState, false, Register.READ_WRITE, deviceAddr, registerAddr,
                                index, numBytes);
                    }

                    @Override
                    public void writeData(byte deviceAddr, byte registerAddr,
                            byte[] data) {
                        writeData(deviceAddr, registerAddr, (byte) 0xff, data);
                    }

                    @Override
                    public void readData(byte deviceAddr, byte registerAddr,
                            byte numBytes) {
                        readData(deviceAddr, registerAddr, (byte) 0xff, numBytes);
                    }
                });
            }
            return modules.get(Module.I2C);
        }
        private ModuleController getTimerModule() {
            if (!modules.containsKey(Module.TIMER)) {
                modules.put(Module.TIMER, new Timer() {
                    @Override
                    public void addTimer(int period, short repeat, boolean delay) {
                        ByteBuffer buffer= ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);
                        buffer.putInt(period).putShort(repeat).put((byte) (delay ? 1 : 0));
                        
                        queueRegisterAction(mwState, true, Register.TIMER_ENTRY, buffer.array());
                    }

                    @Override
                    public void startTimer(byte timerId) {
                        queueRegisterAction(mwState, true, Register.START, timerId);
                    }

                    @Override
                    public void stopTimer(byte timerId) {
                        queueRegisterAction(mwState, true, Register.STOP, timerId);
                    }

                    @Override
                    public void removeTimer(byte timerId) {
                        queueRegisterAction(mwState, true, Register.REMOVE, timerId);
                    }

                    @Override
                    public void enableNotification(byte timerId) {
                        queueRegisterAction(mwState, true, Register.TIMER_NOTIFY, (byte) 1);
                        queueRegisterAction(mwState, true, Register.TIMER_NOTIFY_ENABLE, timerId, (byte) 1);
                    }

                    @Override
                    public void disableNotification(byte timerId) {
                        queueRegisterAction(mwState, true, Register.TIMER_NOTIFY_ENABLE, timerId, (byte) 0);
                    }
                });
            }
            return modules.get(Module.TIMER);
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
                        queueRegisterAction(mwState, true, Register.FILTER_CREATE, attributes);
                    }

                    private void addFilter(boolean read, Trigger trigger, FilterConfig config) {
                        byte[] attributes= new byte[config.bytes().length + 5];
                        attributes[0]= trigger.register().module().opcode;
                        attributes[1]= trigger.register().opcode();
                        attributes[2]= trigger.index();
                        attributes[3]= (byte) (trigger.offset() | ((trigger.length() - 1) << 5));
                        attributes[4]= (byte) (config.type().ordinal() + 1);
                        if (read) {
                            attributes[1]|= 0x80;
                        }
                        System.arraycopy(config.bytes(), 0, attributes, 5, config.bytes().length);
                        
                        queueRegisterAction(mwState, true, Register.FILTER_CREATE, attributes);
                    }
                    @Override
                    public void addReadFilter(Trigger trigger, FilterConfig config) {
                        addFilter(true, trigger, config);
                    }
                    @Override
                    public void addFilter(Trigger trigger, FilterConfig config) {
                        addFilter(false, trigger, config);
                    }

                    @Override
                    public void setFilterConfiguration(byte filterId,
                            FilterConfig config) {
                        byte[] bleData= new byte[config.bytes().length + 2];
                        
                        bleData[0]= filterId;
                        bleData[1]= (byte) (config.type().ordinal() + 1);
                        
                        System.arraycopy(config.bytes(), 0, bleData, 2, config.bytes().length);
                        queueRegisterAction(mwState, true, Register.FILTER_CONFIGURATION, bleData);
                    }
                    
                    @Override
                    public void resetFilterState(byte filterId) {
                        queueRegisterAction(mwState, true, Register.FILTER_STATE, filterId);
                    }
                    
                    @Override
                    public void setFilterState(byte filterId, byte[] state) {
                        byte[] mergedState= new byte[state.length + 1];
                        mergedState[0]= filterId;
                        System.arraycopy(state, 0, mergedState, 1, state.length);
                        queueRegisterAction(mwState, true, Register.FILTER_STATE, mergedState);
                    }

                    @Override
                    public void removeFilter(byte filterId) {
                        queueRegisterAction(mwState, true, Register.FILTER_REMOVE, filterId);
                    }

                    @Override
                    public void enableFilterNotify(byte filterId) {
                        queueRegisterAction(mwState, true, Register.FILTER_NOTIFICATION, (byte) 1);
                        queueRegisterAction(mwState, true, Register.FILTER_NOTIFY_ENABLE, filterId, (byte) 1);
                    }

                    @Override
                    public void disableFilterNotify(byte filterId) {
                        queueRegisterAction(mwState, true, Register.FILTER_NOTIFY_ENABLE, filterId, (byte) 0);
                    }

                    @Override
                    public void enableModule() {
                        queueRegisterAction(mwState, true, Register.ENABLE, (byte) 1);
                    }

                    @Override
                    public void disableModule() {
                        queueRegisterAction(mwState, true, Register.ENABLE, (byte) 0);
                    }

                    @Override
                    public void filterIdToObject(byte filterId) {
                        queueRegisterAction(mwState, false, Register.FILTER_CREATE, (byte) 1);
                    }

                    @Override
                    public void removeAllFilters() {
                        queueRegisterAction(mwState, true, Register.FILTER_REMOVE_ALL);
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
                        mwState.isRecording|= RECORDING_EVENT;
                        
                        mwState.etBuilder= new EventTriggerBuilder(srcReg, index);
                    }
                    
                    @Override
                    public void recordCommand(
                            com.mbientlab.metawear.api.Register srcReg, 
                            byte index, byte[] extra) {
                        recordMacro(srcReg, index);
                        
                        if (extra.length >= 3) {
                            mwState.etBuilder.setParameterConfig(extra[0], extra[1], extra[2]);
                        }
                    }

                    @Override
                    public byte stopRecord() {
                        mwState.isRecording&= ~RECORDING_EVENT;
                        for(EventInfo info: mwState.etBuilder.getEventInfo()) {
                            queueRegisterAction(mwState, true, Register.ADD_ENTRY, info.entry());
                            queueRegisterAction(mwState, true, Register.EVENT_COMMAND, info.command());
                        }
                        
                        byte numEntries= (byte) mwState.etBuilder.getEventInfo().size();
                        mwState.etBuilder= null;
                        return numEntries;
                    }

                    @Override 
                    public void removeMacros() {
                        queueRegisterAction(mwState, true, Register.REMOVE_ALL_ENTRIES);
                    }

                    @Override
                    public void commandIdToObject(byte commandId) {
                        queueRegisterAction(mwState, false, Register.ADD_ENTRY, (byte) 1);
                    }

                    @Override
                    public void readCommandBytes(byte commandId) {
                        queueRegisterAction(mwState, false, Register.EVENT_COMMAND, (byte) 1);
                    }

                    @Override
                    public void enableModule() {
                        queueRegisterAction(mwState, true, Event.Register.EVENT_ENABLE, (byte) 1);
                    }

                    @Override
                    public void disableModule() {
                        queueRegisterAction(mwState, true, Event.Register.EVENT_ENABLE, (byte) 0);
                    }

                    @Override
                    public void removeCommand(byte commandId) {
                        queueRegisterAction(mwState, true, Register.REMOVE_ENTRY, commandId);
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
                        queueRegisterAction(mwState, true, Register.ENABLE, (byte) 1);
                    }

                    @Override
                    public void stopLogging() {
                        queueRegisterAction(mwState, true, Register.ENABLE, (byte) 0);
                    }

                    private void addTrigger(boolean read, Trigger triggerObj) {
                        byte registerOp= triggerObj.register().opcode();
                        if (read) {
                            registerOp|= 0x80;
                        }
                        queueRegisterAction(mwState, true, Register.ADD_TRIGGER, 
                                triggerObj.register().module().opcode, 
                                registerOp, triggerObj.index(), 
                                (byte) (triggerObj.offset() | ((triggerObj.length() - 1) << 5)));
                    }
                    @Override
                    public void addTrigger(Trigger triggerObj) {
                        addTrigger(false, triggerObj);
                    }
                    
                    @Override
                    public void addReadTrigger(Trigger triggerObj) {
                        addTrigger(true, triggerObj);
                    }

                    @Override
                    public void triggerIdToObject(byte triggerId) {
                        queueRegisterAction(mwState, false, Register.ADD_TRIGGER, triggerId);
                    }

                    @Override
                    public void removeTrigger(byte triggerId) {
                        queueRegisterAction(mwState, true, Register.REMOVE_TRIGGER, triggerId);
                    }

                    @Override
                    public void readReferenceTick() {
                        queueRegisterAction(mwState, false, Register.TIME, (byte) 0);
                    }

                    @Override
                    public void readTotalEntryCount() {
                        queueRegisterAction(mwState, false, Register.LENGTH, (byte) 0);
                    }

                    @Override
                    public void downloadLog(int nEntries, int notifyIncrement) {
                        ByteBuffer buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
                        buffer.putShort((short) (nEntries & 0xffff)).putShort((short) (notifyIncrement & 0xffff));
                        
                        queueRegisterAction(mwState, true, Register.READOUT_NOTIFY, (byte) 1);
                        queueRegisterAction(mwState, true, Register.READOUT_PROGRESS, (byte) 1);
                        queueRegisterAction(mwState, true, Register.READOUT, buffer.array());
                    }

                    @Override
                    public void removeAllTriggers() {
                        queueRegisterAction(mwState, true, Register.REMOVE_ALL_TRIGGERS);
                    }
                    
                    public void removeLogEntries(short nEntries) {
                        queueRegisterAction(mwState, true, Register.REMOVE_ENTRIES, (byte) (nEntries & 0xff), (byte)((nEntries >> 8) & 0xff));
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
                    private float tapDuration= 60.f, tapLatency= 200.f, tapWindow= 300.f;
                    
                    @Override
                    public void enableComponent(Component component, boolean notify) {
                        if (notify) {
                            enableNotification(component);
                        } else {
                            queueRegisterAction(mwState, true, Register.GLOBAL_ENABLE, (byte)0);
                            queueRegisterAction(mwState, true, component.enable, (byte)1);
                            queueRegisterAction(mwState, true, Register.GLOBAL_ENABLE, (byte)1);
                        }
                    }
                    @Override
                    public void disableComponent(Component component) {
                        disableNotification(component);
                    }
                    
                    @Override
                    public void enableNotification(Component component) {
                        queueRegisterAction(mwState, true, Register.GLOBAL_ENABLE, (byte)0);
                        queueRegisterAction(mwState, true, component.enable, (byte)1);
                        queueRegisterAction(mwState, true, component.status, (byte)1);
                        queueRegisterAction(mwState, true, Register.GLOBAL_ENABLE, (byte)1);
                    }

                    @Override
                    public void disableNotification(Component component) {
                        queueRegisterAction(mwState, true, Register.GLOBAL_ENABLE, (byte)0);
                        queueRegisterAction(mwState, true, component.enable, (byte)0);
                        queueRegisterAction(mwState, true, component.status, (byte)0);
                        queueRegisterAction(mwState, true, Register.GLOBAL_ENABLE, (byte)1);
                    }

                    @Override
                    public void readComponentConfiguration(Component component) {
                        queueRegisterAction(mwState, false, component.config, (byte)0);
                    }

                    @Override
                    public void setComponentConfiguration(Component component,
                            byte[] data) {
                        queueRegisterAction(mwState, true, component.config, data);
                    }

                    @Override
                    public void disableDetection(Component component, boolean saveConfig) {
                        activeComponents.remove(component);
                        activeNotifications.remove(component);
                        
                        if (!saveConfig) {
                            if (component == Component.DATA) {
                                mwState.globalConfig[2]= 0x18;
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
                            mwState.globalConfig[2]= 0x18;
                        }
                    }
                    
                    @Override
                    public TapConfig enableTapDetection(TapType type, Axis axis) {
                        byte[] tapConfig;
                        
                        if (!configurations.containsKey(Component.PULSE)) {
                            tapConfig= new byte[] {0x40, 0, 0x40, 0x40, 0x50, 0x18, 0x14, 0x3c};
                            configurations.put(Component.PULSE, tapConfig);
                        } else {
                            tapConfig= configurations.get(Component.PULSE);
                            tapConfig[0] &= 0xc0;
                        }
                        
                        if (type == TapType.SINGLE_TAP || type == TapType.BOTH) {
                            tapConfig[0] |= 1 << (2 * axis.ordinal());
                        }
                        if (type == TapType.DOUBLE_TAP || type == TapType.BOTH) {
                            tapConfig[0] |= 1 << (1 + 2 * axis.ordinal());
                        }

                        configurations.put(Component.PULSE, tapConfig);
                        activeComponents.add(Component.PULSE);
                        activeNotifications.add(Component.PULSE);
                        
                        return new TapConfig() {
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

                            @Override
                            public TapConfig withDuration(float duration) {
                                tapDuration= duration;
                                return this;
                            }

                            @Override
                            public TapConfig withLatency(float latency) {
                                tapLatency= latency;
                                return this;
                            }

                            @Override
                            public TapConfig withWindow(float window) {
                                tapWindow= window;
                                return this;
                            }

                            @Override
                            public TapConfig withLowPassFilter(boolean enabled) {
                                if (enabled) {
                                    mwState.globalConfig[1] |= 0x10;
                                } else {
                                    mwState.globalConfig[1] &= 0xef;
                                }
                                return this;
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
                        int accelOdr= (mwState.globalConfig[2] >> 3) & 0x3;
                        int pwMode= (mwState.globalConfig[3] >> 3) & 0x3;
                        
                        for(Component active: activeComponents) {
                            if (configurations.containsKey(active)) {
                                byte[] config= configurations.get(active);
                                
                                switch (active) {
                                case FREE_FALL:
                                    config[3]= (byte) (100 / motionCountSteps[pwMode][accelOdr]);
                                    break;
                                case PULSE:
                                    int lpfEn= (mwState.globalConfig[1] & 0x10) >> 4;
                                    config[5]= (byte) (tapDuration / pulseTmltSteps[lpfEn][pwMode][accelOdr]);
                                    config[6]= (byte) (tapLatency / pulseLtcySteps[lpfEn][pwMode][accelOdr]);
                                    config[7]= (byte) (tapWindow / pulseWindSteps[lpfEn][pwMode][accelOdr]);
                                    break;
                                case TRANSIENT:
                                    config[3]= (byte)(50 / transientSteps[pwMode][accelOdr]);
                                    break;
                                default:
                                    break;
                                }
                                
                                setComponentConfiguration(active, config);
                            }
                            
                            queueRegisterAction(mwState, true, active.enable, (byte)1);
                            if (activeNotifications.contains(active)) {
                                queueRegisterAction(mwState, true, active.status, (byte)1);
                            }
                        }
                        
                        setComponentConfiguration(Component.DATA, mwState.globalConfig);
                        queueRegisterAction(mwState, true, Register.GLOBAL_ENABLE, (byte)1);
                    }
                    
                    public void stopComponents() {
                        queueRegisterAction(mwState, true, Register.GLOBAL_ENABLE, (byte) 0);
                        
                        for(Component active: activeComponents) {
                            queueRegisterAction(mwState, true, active.enable, (byte)0);
                            queueRegisterAction(mwState, true, active.status, (byte)0);
                        }
                    }
                    
                    public void resetAll() {
                        disableAllDetection(false);
                        
                        queueRegisterAction(mwState, true, Register.GLOBAL_ENABLE, (byte) 0);
                        
                        for(Component it: Component.values()) {
                            queueRegisterAction(mwState, true, it.enable, (byte)0);
                            queueRegisterAction(mwState, true, it.status, (byte)0);
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
                                mwState.globalConfig[0] &= 0xfc; 
                                mwState.globalConfig[0] |= range.ordinal();
                                return this;
                            }

                            @Override
                            public AccelerometerConfig withSilentMode() {
                                activeNotifications.remove(Component.DATA);
                                return this;
                            }

                            @Override
                            public SamplingConfig withOutputDataRate(OutputDataRate rate) {
                                mwState.globalConfig[2] &= 0xc7;
                                mwState.globalConfig[2] |= (rate.ordinal() << 3);
                                return this;
                            }

                            @Override
                            public byte[] getBytes() {
                                return mwState.globalConfig;
                            }

                            @Override
                            public SamplingConfig withHighPassFilter(byte cutoff) {
                                mwState.globalConfig[0] |= 0x10;
                                mwState.globalConfig[1] |= cutoff;
                                return this;
                            }
                            
                            @Override
                            public SamplingConfig withHighPassFilter() {
                                mwState.globalConfig[0] |= 0x10;
                                return this;
                            }
                            
                            @Override
                            public SamplingConfig withoutHighPassFilter() {
                                mwState.globalConfig[0] &= 0xef;
                                return this;
                            }
                        };
                    }
                    @Override
                    public void enableAutoSleepMode() {
                        mwState.globalConfig[3] |= 0x4;
                        queueRegisterAction(mwState, true, Component.DATA.status, mwState.globalConfig);
                    }
                    @Override
                    public void enableAutoSleepMode(SleepModeRate sleepRate, int timeout) {
                        mwState.globalConfig[2] &= 0x3f;
                        mwState.globalConfig[2] |= sleepRate.ordinal() << 6;
                        mwState.globalConfig[4]= (byte)(timeout / sleepCountSteps[(mwState.globalConfig[2] >> 3) & 0x3]);
                        enableAutoSleepMode();
                    }
                    @Override
                    public void disableAutoSleepMode() {
                        mwState.globalConfig[3] &= ~(0x4);
                        queueRegisterAction(mwState, true, Component.DATA.status, mwState.globalConfig);
                    }
                    @Override
                    public void setPowerMode(PowerMode mode) {
                        mwState.globalConfig[3] &= ~(0x3 << 3);
                        mwState.globalConfig[3] |= (mode.ordinal() << 3);
                        
                        mwState.globalConfig[3] &= ~0x3;
                        mwState.globalConfig[3] |= mode.ordinal();
                        queueRegisterAction(mwState, true, Component.DATA.status, mwState.globalConfig);
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
                        queueRegisterAction(mwState, true, Register.RESET_DEVICE);
                    }
                    
                    @Override
                    public void jumpToBootloader() {
                        queueRegisterAction(mwState, true, Register.JUMP_TO_BOOTLOADER);
                    }
                    
                    @Override
                    public void resetAfterGarbageCollect() {
                        queueRegisterAction(mwState, true, Register.DELAYED_RESET);
                        queueRegisterAction(mwState, true, Register.GAP_DISCONNECT);
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
                        queueRegisterAction(mwState, false, mode.register, pin);
                    }
                    @Override
                    public void readDigitalInput(byte pin) {
                        queueRegisterAction(mwState, false, Register.READ_DIGITAL_INPUT, pin);
                    }
                    @Override
                    public void setDigitalOutput(byte pin) {
                        queueRegisterAction(mwState, true, Register.SET_DIGITAL_OUTPUT, pin);
                    }
                    @Override
                    public void clearDigitalOutput(byte pin) {
                        queueRegisterAction(mwState, true, Register.CLEAR_DIGITAL_OUTPUT, pin);
                    }                
                    @Override
                    public void setDigitalInput(byte pin, PullMode mode) {
                        queueRegisterAction(mwState, true, mode.register, pin);
                    }
                    @Override
                    public void setPinChangeType(byte pin, ChangeType type) {
                        queueRegisterAction(mwState, true, Register.SET_PIN_CHANGE, pin, (byte) type.ordinal());
                    }
                    @Override
                    public void enablePinChangeNotification(byte pin) {
                        queueRegisterAction(mwState, true, Register.PIN_CHANGE_NOTIFY, (byte) 1);
                        queueRegisterAction(mwState, true, Register.PIN_CHANGE_NOTIFY_ENABLE, pin, (byte) 1);
                    }
                    @Override
                    public void disablePinChangeNotification(byte pin) {
                        queueRegisterAction(mwState, true, Register.PIN_CHANGE_NOTIFY_ENABLE, pin, (byte) 0);
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
                        queueRegisterAction(mwState, true, Register.ENABLE, (byte)1);                    
                    }
                    @Override
                    public void disableIBecon() {
                        queueRegisterAction(mwState, true, Register.ENABLE, (byte)0);
                    }
                    @Override
                    public IBeacon setUUID(UUID uuid) {
                        byte[] uuidBytes= ByteBuffer.wrap(new byte[16])
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .putLong(uuid.getLeastSignificantBits())
                                .putLong(uuid.getMostSignificantBits())
                                .array();
                        queueRegisterAction(mwState, true, Register.ADVERTISEMENT_UUID, uuidBytes);
                        return this;
                    }
                    @Override
                    public void readSetting(Register register) {
                        queueRegisterAction(mwState, false, register);
                    }
                    @Override
                    public IBeacon setMajor(short major) {
                        queueRegisterAction(mwState, true, Register.MAJOR, (byte)(major & 0xff), (byte)((major >> 8) & 0xff));
                        return this;
                    }
                    @Override
                    public IBeacon setMinor(short minor) {
                        queueRegisterAction(mwState, true, Register.MINOR, (byte)(minor & 0xff), (byte)((minor >> 8) & 0xff));
                        return this;
                    }
                    @Override
                    public IBeacon setCalibratedRXPower(byte power) {
                        queueRegisterAction(mwState, true, Register.RX_POWER, power);
                        return this;
                    }
                    @Override
                    public IBeacon setTXPower(byte power) {
                        queueRegisterAction(mwState, true, Register.TX_POWER, power);
                        return this;
                    }
                    @Override
                    public IBeacon setAdvertisingPeriod(short freq) {
                        queueRegisterAction(mwState, true, Register.ADVERTISEMENT_PERIOD, (byte)(freq & 0xff), (byte)((freq >> 8) & 0xff));
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
                        queueRegisterAction(mwState, true, Register.PLAY, (byte)(autoplay ? 2 : 1));
                    }
                    public void pause() {
                        queueRegisterAction(mwState, true, Register.PLAY, (byte)0);
                    }
                    public void stop(boolean resetChannels) {
                        queueRegisterAction(mwState, true, Register.STOP, (byte)(resetChannels ? 1 : 0));
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
                                queueRegisterAction(mwState, true, Register.MODE, channelData);
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
                        queueRegisterAction(mwState, true, Register.SWITCH_STATE, (byte)1);
                    }
                    @Override
                    public void disableNotification() {
                        queueRegisterAction(mwState, true, Register.SWITCH_STATE, (byte)0);
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
                        queueRegisterAction(mwState, false, Register.INITIALIZE, strand);
                    }
                    @Override
                    public void readHoldState(byte strand) {
                        queueRegisterAction(mwState, false, Register.HOLD, strand);
                    }
                    @Override
                    public void readPixelState(byte strand, byte pixel) {
                        queueRegisterAction(mwState, false, Register.PIXEL, strand, pixel);
                    }
                    @Override
                    public void readRotationState(byte strand) {
                        queueRegisterAction(mwState, false, Register.ROTATE, strand);
                    }
                    @Override
                    public void initializeStrand(byte strand, ColorOrdering ordering,
                            StrandSpeed speed, byte ioPin, byte length) {
                        queueRegisterAction(mwState, true, Register.INITIALIZE, strand, 
                                (byte)(speed.ordinal() << 2 | ordering.ordinal()), ioPin, length);
                        
                    }
                    @Override
                    public void holdStrand(byte strand, byte holdState) {
                        queueRegisterAction(mwState, true, Register.HOLD, strand, holdState);
                        
                    }
                    @Override
                    public void clearStrand(byte strand, byte start, byte end) {
                        queueRegisterAction(mwState, true, Register.CLEAR, strand, start, end);
                        
                    }
                    @Override
                    public void setPixel(byte strand, byte pixel, byte red,
                            byte green, byte blue) {
                        queueRegisterAction(mwState, true, Register.PIXEL, strand, pixel, red, green, blue);
                        
                    }
                    @Override
                    public void rotateStrand(byte strand, RotationDirection direction, byte repetitions,
                            short delay) {
                        queueRegisterAction(mwState, true, Register.ROTATE, strand, (byte)direction.ordinal(), repetitions, 
                                (byte)(delay & 0xff), (byte)(delay >> 8 & 0xff));
                    }
                    @Override
                    public void deinitializeStrand(byte strand) {
                        queueRegisterAction(mwState, true, Register.DEINITIALIZE, strand);
                        
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
                        queueRegisterAction(mwState, false, Register.TEMPERATURE, (byte) 0);
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
                                queueRegisterAction(mwState, true, Register.MODE, samplingConfig);
                                if (!silent) {
                                    queueRegisterAction(mwState, true, Register.TEMPERATURE, (byte) 1);
                                    queueRegisterAction(mwState, true, Register.DELTA_TEMP, (byte) 1);
                                    queueRegisterAction(mwState, true, Register.THRESHOLD_DETECT, (byte) 1);
                                }
                            }
                        };
                    }

                    @Override
                    public void disableSampling() {
                        queueRegisterAction(mwState, true, Register.MODE, new byte[] {0, 0, 0, 0, 0, 0, 0, 0});
                        queueRegisterAction(mwState, true, Register.TEMPERATURE, (byte) 0);
                        queueRegisterAction(mwState, true, Register.DELTA_TEMP, (byte) 0);
                        queueRegisterAction(mwState, true, Register.THRESHOLD_DETECT, (byte) 0);
                    }
                    
                    @Override
                    public void enableThermistorMode(byte analogReadPin, byte pulldownPin) {
                        queueRegisterAction(mwState, true, Register.THERMISTOR_MODE, (byte) 1, analogReadPin, pulldownPin);
                    }
                    
                    @Override
                    public void disableThermistorMode() {
                        queueRegisterAction(mwState, true, Register.THERMISTOR_MODE, (byte) 0);
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
                        queueRegisterAction(mwState, true, Register.PULSE, (byte)127, (byte)(pulseWidth & 0xff), 
                                (byte)((pulseWidth >> 8) & 0xff), (byte)1);
                    }

                    @Override
                    public void startMotor(float dutyCycle, short pulseWidth) {
                        short converted= (short)((dutyCycle / 100.f) * 248);
                        queueRegisterAction(mwState, true, Register.PULSE, (byte)(converted & 0xff), (byte)(pulseWidth & 0xff), 
                                (byte)((pulseWidth >> 8) & 0xff), (byte)0);
                    }
                });
            }
            return modules.get(Module.HAPTIC);
        }
        @Override
        public void readDeviceInformation() {
            for(GATTCharacteristic it: DeviceInformation.values()) {
                readCharacteristic(mwState, it);
            }
            
            execGattAction(false);
        }
        @Override
        public void readBatteryLevel() {
            readCharacteristic(mwState, Battery.BATTERY_LEVEL);
            execGattAction(false);
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
                    MetaWearBleService.this.close(notify);
                }
                @Override
                public void close(boolean notify, boolean wait) {
                    throw new UnsupportedOperationException("This function is not supported in single metawear mode");
                }
                @Override
                public void waitToClose(boolean notify) {
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
                    if (!isConnected()) return;

                    MetaWearBleService.this.close(mwState);
                    if (notify) {
                        Intent intent= new Intent(Action.DEVICE_DISCONNECTED);
                        intent.putExtra(Extra.BLUETOOTH_DEVICE, mwState.mwBoard);
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
                    } else {
                        close(notify);
                    }
                }
                
                @Override
                public void waitToClose(boolean notify) {
                    mwState.notifyUser= notify;
                    mwState.readyToClose= true;
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
     * @deprecated As of v1.4, use {@link #clearLocalBroadcastManager()} and 
     * {@link #useLocalBroadcastManager(LocalBroadcastManager)} instead 
     */
    @Deprecated
    public void useLocalBroadcasterManager(boolean useFlag) {
        localBroadcastMngr= (useFlag) ? LocalBroadcastManager.getInstance(this) : null;
    }
    /**
     * Clears the stored local broadcast manager, which reverts to using the Service's intent broadcaster
     */
    public void clearLocalBroadcastManager() {
        localBroadcastMngr= null;
    }
    /**
     * Has the service broadcast intents with a {@link android.support.v4.content.LocalBroadcastManager} 
     * @param localBroadcastMngr Local broadcast manager to use
     */
    public void useLocalBroadcastManager(final LocalBroadcastManager localBroadcastMngr) {
        this.localBroadcastMngr= localBroadcastMngr;
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
        
        if (singleMwState.mwController == null) {
            singleMwState.mwController= (MetaWearControllerImpl) getMetaWearController();
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
        if (singleMwState != null && singleMwState.mwBoard != null) {
            if (singleMwState.mwGatt != null) {
                singleMwState.mwGatt.close();
                singleMwState.mwGatt= null;
            }
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
    
    private void queueRegisterAction(final MetaWearState mwState, boolean write, Register register, byte ... parameters) {
        queueRegisterAction(mwState, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, write, register, parameters);
    }

    private static final byte MACRO_MAX_LENGTH= 18;
    private void queueRegisterAction(final MetaWearState mwState, int desiredWriteType, boolean write, 
            Register register, byte ... parameters) {
        if (!mwState.readyToClose) {
            byte[] command= (write) ? Registers.buildWriteCommand(register, parameters) : 
                    Registers.buildReadCommand(register, parameters);
            byte[] macroBytes= null;
       
            if ((mwState.isRecording & RECORDING_EVENT) == RECORDING_EVENT) {
                mwState.etBuilder.withDestRegister(register, 
                        Arrays.copyOfRange(command, 2, command.length), !write);
            } else if ((mwState.isRecording & RECORDING_MACRO) == RECORDING_MACRO) {
                if (command.length > MACRO_MAX_LENGTH) {
                    byte lengthDiff= (byte) (command.length - MACRO_MAX_LENGTH);
                    macroBytes= new byte[lengthDiff + 2];
                    macroBytes[0]= Module.MACRO.opcode;
                    macroBytes[1]= Macro.Register.PARTIAL_COMMAND.opcode();
                    System.arraycopy(command, 0, macroBytes, 2, lengthDiff);
                    queueCommand(mwState, macroBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    
                    macroBytes= new byte[MACRO_MAX_LENGTH + 2];
                    macroBytes[0]= Module.MACRO.opcode;
                    macroBytes[1]= Macro.Register.ADD_COMMAND.opcode();
                    System.arraycopy(command, lengthDiff, macroBytes, 2, MACRO_MAX_LENGTH);
                    queueCommand(mwState, macroBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                } else {
                    macroBytes= new byte[command.length + 2];
                    macroBytes[0]= Module.MACRO.opcode;
                    macroBytes[1]= Macro.Register.ADD_COMMAND.opcode();
                    System.arraycopy(command, 0, macroBytes, 2, command.length);
                    queueCommand(mwState, macroBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                }
            } else {
                queueCommand(mwState, command, desiredWriteType);
            }
        }
    }
    
    private void queueCommand(final MetaWearState mwState, final byte[] bleData, final int writeType) {
        queueGattAction(mwState, new GattAction() {
            @Override
            public boolean execAction() {
                if (mwState.mwGatt != null) {
                    BluetoothGattService service= mwState.mwGatt.getService(GATTService.METAWEAR.uuid());
                    BluetoothGattCharacteristic command= service.getCharacteristic(MetaWear.COMMAND.uuid());
                    command.setWriteType(writeType);
                    command.setValue(bleData);
                    mwState.mwGatt.writeCharacteristic(command);
                    if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT && 
                            Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        ///< For some reason, SDK 18 doesn't implement write with response so we'll 
                        ///< delay the flow for 100ms.  This value is what worked on a 2013 Nexus 7 
                        ///< tablet running Android 4.3
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            ///< Not good if we end up here
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        
        execGattAction(false);
    }

}
