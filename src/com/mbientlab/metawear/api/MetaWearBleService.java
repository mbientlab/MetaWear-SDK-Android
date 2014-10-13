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
 * PROVIDED AS IS WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, 
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mbientlab.metawear.api.GATT.GATTCharacteristic;
import com.mbientlab.metawear.api.GATT.GATTService;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.characteristic.*;
import com.mbientlab.metawear.api.controller.*;
import com.mbientlab.metawear.api.controller.Accelerometer.SamplingConfig.OutputDataRate;
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
    }
    private class Extra {
        public static final String SERVICE_UUID= 
                "com.mbientlab.com.metawear.api.MetaWearBleService.Extra.SERVICE_UUID";
        /** Extra Intent information for the characteristic UUID */
        public static final String CHARACTERISTIC_UUID= 
                "com.mbientlab.com.metawear.api.MetaWearBleService.Extra.CHARACTERISTIC_UUID";
        /** Extra Intent information for the characteristic value */
        public static final String CHARACTERISTIC_VALUE= 
                "com.mbientlab.com.metawear.api.MetaWearBleService.Extra.CHARACTERISTIC_VALUE";
    }

    private final static UUID CHARACTERISTIC_CONFIG= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final static HashMap<Byte, ArrayList<ModuleCallbacks>> moduleCallbackMap= new HashMap<>();
    private final static HashSet<DeviceCallbacks> deviceCallbacks= new HashSet<>();
    
    private boolean connected;
    /** GATT connection to the ble device */
    private BluetoothGatt metaWearGatt;
    /** Current state of the device */
    private DeviceState deviceState;
    private BluetoothDevice metaWearBoard;
    /** Bytes still be to written to the MetaWear command characteristic */
    private final ConcurrentLinkedQueue<byte[]> commandBytes= new ConcurrentLinkedQueue<>();
    /** Characteristic UUIDs still to be read from MetaWear */
    private final ConcurrentLinkedQueue<GATTCharacteristic> readCharUuids= new ConcurrentLinkedQueue<>();

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
        return filter;
    }
    
    private static BroadcastReceiver mwBroadcastReceiver;
    
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
                    switch (intent.getAction()) {
                    case Action.NOTIFICATION_RECEIVED:
                        final byte[] data= (byte[])intent.getExtras().get(Extra.CHARACTERISTIC_VALUE);
                        if (data.length > 1) {
                            byte moduleOpcode= data[0], registerOpcode= (byte)(0x7f & data[1]);
                            Collection<ModuleCallbacks> callbacks;
                            if (moduleCallbackMap.containsKey(moduleOpcode) && 
                                    (callbacks= moduleCallbackMap.get(moduleOpcode)) != null) {
                                Module.lookupModule(moduleOpcode).lookupRegister(registerOpcode)
                                        .notifyCallbacks(callbacks, data);
                            }
                        }
                        break;
                    case Action.DEVICE_CONNECTED:
                        for(DeviceCallbacks it: deviceCallbacks) {
                            it.connected();
                        }
                        break;
                    case Action.DEVICE_DISCONNECTED:
                        for(DeviceCallbacks it: deviceCallbacks) {
                            it.disconnected();
                        }
                        break;
                    case Action.CHARACTERISTIC_READ:
                        UUID serviceUuid= (UUID)intent.getExtras().get(Extra.SERVICE_UUID), 
                                charUuid= (UUID)intent.getExtras().get(Extra.CHARACTERISTIC_UUID);
                        GATTCharacteristic characteristic= GATTService.lookupGATTService(serviceUuid).getCharacteristic(charUuid);
                        for(DeviceCallbacks it: deviceCallbacks) {
                            it.receivedGATTCharacteristic(characteristic, (byte[])intent.getExtras().get(Extra.CHARACTERISTIC_VALUE));
                        }
                        break;
                    }
                }
            };
        }
        return mwBroadcastReceiver;
    }
    
    /** Interacts with the MetaWear board */
    private final MetaWearController controller= new MetaWearController() {
        private final HashMap<Module, ModuleController> modules= new HashMap<>();
        
        @Override
        public MetaWearController addModuleCallback(ModuleCallbacks callback) {
            byte moduleOpcode= callback.getModule().opcode;
            if (!moduleCallbackMap.containsKey(moduleOpcode)) {
                moduleCallbackMap.put(moduleOpcode, new ArrayList<ModuleCallbacks>());
            }
            moduleCallbackMap.get(moduleOpcode).add(callback);
            return this;
        }
        @Override
        public void removeModuleCallback(ModuleCallbacks callback) {
            moduleCallbackMap.get(callback.getModule().opcode).remove(callback);
        }
        
        @Override
        public MetaWearController addDeviceCallback(DeviceCallbacks callback) {
            deviceCallbacks.add(callback);
            return this;
        }
        @Override
        public void removeDeviceCallback(DeviceCallbacks callback) {
            deviceCallbacks.remove(callback);
        }
        
        @Override
        public ModuleController getModuleController(Module opcode) {
            switch (opcode) {
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
            case LOGGING:
                return getLoggingModule();
            default:
                break;
            }
            return null;
        }

        private ModuleController getLoggingModule() {
            if (!modules.containsKey(Module.LOGGING)) {
                modules.put(Module.LOGGING, new Logging() {
                    @Override
                    public void startLogging() {
                        writeRegister(Register.ENABLE, (byte) 1);
                    }

                    @Override
                    public void stopLogging() {
                        writeRegister(Register.ENABLE, (byte) 0);
                    }

                    @Override
                    public void addTrigger(Trigger triggerObj) {
                        writeRegister(Register.ADD_TRIGGER, triggerObj.register().module().opcode, triggerObj.register().opcode(), 
                                triggerObj.index(), (byte) (triggerObj.offset() | ((triggerObj.length() - 1) << 5)));
                    }

                    @Override
                    public void triggerIdToObject(byte triggerId) {
                        readRegister(Register.ADD_TRIGGER, triggerId);
                    }

                    @Override
                    public void removeTrigger(byte triggerId) {
                        writeRegister(Register.REMOVE_TRIGGER, triggerId);
                    }

                    @Override
                    public void readReferenceTick() {
                        readRegister(Register.TIME, (byte) 0);
                    }

                    @Override
                    public void readTotalEntryCount() {
                        readRegister(Register.LENGTH, (byte) 0);
                    }

                    @Override
                    public void downloadLog(int nEntries, int notifyIncrement) {
                        ByteBuffer buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
                        buffer.putShort((short) (nEntries & 0xffff)).putShort((short) (notifyIncrement & 0xffff));
                        
                        writeRegister(Register.READOUT_NOTIFY, (byte) 1);
                        writeRegister(Register.READOUT_PROGRESS, (byte) 1);
                        writeRegister(Register.READOUT, buffer.array());
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
                            writeRegister(Register.GLOBAL_ENABLE, (byte)0);
                            writeRegister(component.enable, (byte)1);
                            writeRegister(Register.GLOBAL_ENABLE, (byte)1);
                        }
                    }
                    @Override
                    public void disableComponent(Component component) {
                        disableNotification(component);
                    }
                    
                    @Override
                    public void enableNotification(Component component) {
                        writeRegister(Register.GLOBAL_ENABLE, (byte)0);
                        writeRegister(component.enable, (byte)1);
                        writeRegister(component.status, (byte)1);
                        writeRegister(Register.GLOBAL_ENABLE, (byte)1);
                    }

                    @Override
                    public void disableNotification(Component component) {
                        writeRegister(Register.GLOBAL_ENABLE, (byte)0);
                        writeRegister(component.enable, (byte)0);
                        writeRegister(component.status, (byte)0);
                        writeRegister(Register.GLOBAL_ENABLE, (byte)1);
                    }

                    @Override
                    public void readComponentConfiguration(Component component) {
                        readRegister(component.config, (byte)0);
                    }

                    @Override
                    public void setComponentConfiguration(Component component,
                            byte[] data) {
                        writeRegister(component.config, data);
                    }

                    @Override
                    public void disableDetection(Component component, boolean saveConfig) {
                        activeComponents.remove(component);
                        activeNotifications.remove(component);
                        
                        if (!saveConfig) {
                            configurations.remove(component);
                        }
                    }
                    
                    @Override
                    public void disableAllDetection(boolean saveConfig) {
                        activeComponents.clear();
                        activeNotifications.clear();
                        
                        if (!saveConfig) {
                            configurations.clear();
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
                            
                            writeRegister(active.enable, (byte)1);
                            if (activeNotifications.contains(active)) {
                                writeRegister(active.status, (byte)1);
                            }
                        }
                        
                        setComponentConfiguration(Component.DATA, globalConfig);
                        writeRegister(Register.GLOBAL_ENABLE, (byte)1);
                    }
                    
                    public void stopComponents() {
                        writeRegister(Register.GLOBAL_ENABLE, (byte) 0);
                        
                        for(Component active: activeComponents) {
                            writeRegister(active.enable, (byte)0);
                            writeRegister(active.status, (byte)0);
                        }
                    }
                    
                    public void resetAll() {
                        disableAllDetection(false);
                        
                        writeRegister(Register.GLOBAL_ENABLE, (byte) 0);
                        
                        for(Component it: Component.values()) {
                            writeRegister(it.enable, (byte)0);
                            writeRegister(it.status, (byte)0);
                        }
                    }

                    @Override
                    public SamplingConfig enableXYZSampling() {
                        globalConfig= new byte[] {0, 0, 0x18, 0, 0};
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
                        writeRegister(Register.RESET_DEVICE);
                    }
                    @Override
                    public void jumpToBootloader() {
                        writeRegister(Register.JUMP_TO_BOOTLOADER);
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
                        readRegister(mode.register, pin);
                    }
                    @Override
                    public void readDigitalInput(byte pin) {
                        readRegister(Register.READ_DIGITAL_INPUT, pin);
                    }
                    @Override
                    public void setDigitalOutput(byte pin) {
                        writeRegister(Register.SET_DIGITAL_OUTPUT, pin);
                    }
                    @Override
                    public void clearDigitalOutput(byte pin) {
                        writeRegister(Register.CLEAR_DIGITAL_OUTPUT, pin);
                    }                
                    @Override
                    public void setDigitalInput(byte pin, PullMode mode) {
                        writeRegister(mode.register, pin);
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
                        writeRegister(Register.ENABLE, (byte)1);                    
                    }
                    @Override
                    public void disableIBecon() {
                        writeRegister(Register.ENABLE, (byte)0);
                    }
                    @Override
                    public IBeacon setUUID(UUID uuid) {
                        byte[] uuidBytes= ByteBuffer.wrap(new byte[16])
                                .putLong(uuid.getMostSignificantBits())
                                .putLong(uuid.getLeastSignificantBits())
                                .array();
                        writeRegister(Register.ADVERTISEMENT_UUID, uuidBytes);
                        return this;
                    }
                    @Override
                    public void readSetting(Register register) {
                        readRegister(register);
                    }
                    @Override
                    public IBeacon setMajor(short major) {
                        writeRegister(Register.MAJOR, (byte)(major >> 8 & 0xff), (byte)(major & 0xff));
                        return this;
                    }
                    @Override
                    public IBeacon setMinor(short minor) {
                        writeRegister(Register.MINOR, (byte)(minor >> 8 & 0xff), (byte)(minor & 0xff));
                        return this;
                    }
                    @Override
                    public IBeacon setCalibratedRXPower(byte power) {
                        writeRegister(Register.RX_POWER, power);
                        return this;
                    }
                    @Override
                    public IBeacon setTXPower(byte power) {
                        writeRegister(Register.TX_POWER, power);
                        return this;
                    }
                    @Override
                    public IBeacon setAdvertisingPeriod(short freq) {
                        writeRegister(Register.ADVERTISEMENT_PERIOD, (byte)(freq >> 8 & 0xff), (byte)(freq & 0xff));
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
                        writeRegister(Register.PLAY, (byte)(autoplay ? 2 : 1));
                    }
                    public void pause() {
                        writeRegister(Register.PLAY, (byte)0);
                    }
                    public void stop(boolean resetChannels) {
                        writeRegister(Register.STOP, (byte)(resetChannels ? 1 : 0));
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
                                channelData[5]= (byte)(time >> 8);
                                channelData[4]= (byte)(time & 0xff);
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withHighTime(short time) {
                                channelData[7]= (byte)(time >> 8);
                                channelData[6]= (byte)(time & 0xff);
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withFallTime(short time) {
                                channelData[9]= (byte)(time >> 8);
                                channelData[8]= (byte)(time & 0xff);
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withPulseDuration(short period) {
                                channelData[11]= (byte)(period >> 8);
                                channelData[10]= (byte)(period & 0xff);
                                return this;
                            }

                            @Override
                            public ChannelDataWriter withPulseOffset(short offset) {
                                channelData[13]= (byte)(offset >> 8);
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
                                writeRegister(Register.MODE, channelData);
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
                        writeRegister(Register.SWITCH_STATE, (byte)1);
                    }
                    @Override
                    public void disableNotification() {
                        writeRegister(Register.SWITCH_STATE, (byte)0);
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
                        readRegister(Register.INITIALIZE, strand);
                    }
                    @Override
                    public void readHoldState(byte strand) {
                        readRegister(Register.HOLD, strand);
                    }
                    @Override
                    public void readPixelState(byte strand, byte pixel) {
                        readRegister(Register.PIXEL, strand, pixel);
                    }
                    @Override
                    public void readRotationState(byte strand) {
                        readRegister(Register.ROTATE, strand);
                    }
                    @Override
                    public void initializeStrand(byte strand, ColorOrdering ordering,
                            StrandSpeed speed, byte ioPin, byte length) {
                        writeRegister(Register.INITIALIZE, strand, 
                                (byte)(speed.ordinal() << 2 | ordering.ordinal()), ioPin, length);
                        
                    }
                    @Override
                    public void holdStrand(byte strand, byte holdState) {
                        writeRegister(Register.HOLD, strand, holdState);
                        
                    }
                    @Override
                    public void clearStrand(byte strand, byte start, byte end) {
                        writeRegister(Register.CLEAR, strand, start, end);
                        
                    }
                    @Override
                    public void setPixel(byte strand, byte pixel, byte red,
                            byte green, byte blue) {
                        writeRegister(Register.PIXEL, strand, pixel, red, green, blue);
                        
                    }
                    @Override
                    public void rotateStrand(byte strand, RotationDirection direction, byte repetitions,
                            short delay) {
                        writeRegister(Register.ROTATE, strand, (byte)direction.ordinal(), repetitions, 
                                (byte)(delay & 0xff), (byte)(delay >> 8 & 0xff));
                    }
                    @Override
                    public void deinitializeStrand(byte strand) {
                        writeRegister(Register.DEINITIALIZE, strand);
                        
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
                        readRegister(Register.TEMPERATURE, (byte)0);
                    }
                });
            }
            return modules.get(Module.TEMPERATURE);
        }
        private ModuleController getHapticModule() {
            if (!modules.containsKey(Module.HAPTIC)) {
                modules.put(Module.HAPTIC, new Haptic() {
                    @Override
                    public void startMotor(short pulseWidth) {
                        writeRegister(Register.PULSE, (byte)128, (byte)(pulseWidth & 0xff), (byte)(pulseWidth >> 8), (byte)1);
                    }

                    @Override
                    public void startBuzzer(short pulseWidth) {
                        writeRegister(Register.PULSE, (byte)248, (byte)(pulseWidth & 0xff), (byte)(pulseWidth >> 8), (byte)0);
                    }
                });
            }
            return modules.get(Module.HAPTIC);
        }
        @Override
        public void readDeviceInformation() {
            for(GATTCharacteristic it: DeviceInformation.values()) {
                readCharUuids.add(it);
            }
            if (deviceState == DeviceState.READY) {
                deviceState= DeviceState.READING_CHARACTERISTICS;
                readCharacteristic();
            }
        }
        @Override
        public void readBatteryLevel() {
            readCharUuids.add(Battery.BATTERY_LEVEL);
            if (deviceState == DeviceState.READY) {
                deviceState= DeviceState.READING_CHARACTERISTICS;
                readCharacteristic();
            }
        }
        
        @Override
        public boolean isConnected() {
            return connected;
        }
        
    };
    
    /**
     * Writes a command to MetaWear via the command register UUID
     * @see Characteristics.MetaWear#COMMAND
     */
    private void writeCommand() {
        BluetoothGattService service= metaWearGatt.getService(GATTService.METAWEAR.uuid());
        BluetoothGattCharacteristic command= service.getCharacteristic(MetaWear.COMMAND.uuid());
        command.setValue(commandBytes.poll());
        metaWearGatt.writeCharacteristic(command);
    }

    /**
     * Read a characteristic from MetaWear.
     * An intent with the action CHARACTERISTIC_READ will be broadcasted.
     * @see Action.BluetoothLe#ACTION_CHARACTERISTIC_READ
     */
    private void readCharacteristic() {
        GATTCharacteristic charInfo= readCharUuids.poll();
        BluetoothGattService service= metaWearGatt.getService(charInfo.gattService().uuid());

        BluetoothGattCharacteristic characteristic= service.getCharacteristic(charInfo.uuid());
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
                intent.setAction(Action.DEVICE_CONNECTED);
                connected= true;
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                intent.setAction(Action.DEVICE_DISCONNECTED);
                connected= false;
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
            BluetoothGattDescriptor descriptor= shouldNotify.poll().getDescriptor(CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            metaWearGatt.writeDescriptor(descriptor);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor, int status) {
            if (!shouldNotify.isEmpty()) setupNotification();
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
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            Intent intent= new Intent(Action.CHARACTERISTIC_READ);
            intent.putExtra(Extra.SERVICE_UUID, characteristic.getService().getUuid());
            intent.putExtra(Extra.CHARACTERISTIC_UUID, characteristic.getUuid());
            intent.putExtra(Extra.CHARACTERISTIC_VALUE, characteristic.getValue());
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
            Intent intent= new Intent(Action.NOTIFICATION_RECEIVED);
            intent.putExtra(Extra.CHARACTERISTIC_VALUE, characteristic.getValue());
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
     * Connect to the GATT service on the MetaWear device
     * @param metaWearBoard MetaWear device to connect to
     */
    public void connect(BluetoothDevice metaWearBoard) {
        if (!metaWearBoard.equals(this.metaWearBoard)) {
            commandBytes.clear();
            readCharUuids.clear();
        }
        deviceState= null;
        close();
        this.metaWearBoard= metaWearBoard;
        metaWearGatt= metaWearBoard.connectGatt(this, false, metaWearGattCallback);
    }
    
    public void reconnect() {
        if (metaWearBoard != null) {
            deviceState= null;
            close();
            metaWearGatt= metaWearBoard.connectGatt(this, false, metaWearGattCallback);
        }
    }
    
    public void disconnect() {
        if (metaWearGatt != null) {
            metaWearGatt.disconnect();
        }
    }
    /** Close the GATT service and free up resources */
    public void close(boolean notify) {
        if (metaWearGatt != null) {
            metaWearGatt.close();
            metaWearGatt= null;
            connected= false;
            if (notify) {
                for(DeviceCallbacks it: deviceCallbacks) {
                    it.disconnected();
                }
            }
        }
    }
    /** Close the GATT service and free up resources */
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
        // TODO Auto-generated method stub
        return serviceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private void readRegister(com.mbientlab.metawear.api.Register register,
            byte... parameters) {
        queueCommand(Registers.buildReadCommand(register, parameters));
    }

    private void writeRegister(com.mbientlab.metawear.api.Register register,
            byte... data) {
        queueCommand(Registers.buildWriteCommand(register, data));
    }

    /**
     * @param module
     * @param registerOpcode
     * @param data
     */
    private void queueCommand(byte[] command) {
        commandBytes.add(command);
        if (deviceState == DeviceState.READY) {
            deviceState= DeviceState.WRITING_CHARACTERISTICS;
            writeCommand();
        }
    }
}