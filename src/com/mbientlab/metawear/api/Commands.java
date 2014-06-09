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

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.mbientlab.metawear.api.BroadcastReceiverBuilder.*;

/**
 * Collection of all MetaWear commands
 * @author Eric Tsai
 */
public class Commands {
    public static byte MAX_SIZE= 10;
    
    /**
     * Dispatcher for calling the functions in the Notification subclasses
     * @author Eric Tsai
     */
    private interface NotificationDispatcher {
        /**
         * Convert the bytes of data into the parameters required for a notification callback
         * @param notifications Collection of notifications to call the appropriate function on
         * @param data Response data from MetaWear
         */
        public void dispatch(Collection<ModuleNotification> notifications, byte[] data);
    }
    
    /**
     * Modules on the MetaWear board
     * @author Eric Tsai
     */
    public enum Module {
        MECHANICAL_SWITCH((byte)0x01),
        LED_DRIVER((byte)0x2),
        ACCELEROMETER((byte)0x3),
        TEMPERATURE((byte)0x4),
        GPIO((byte)0x5),
        TEST((byte)0xfe);
        
        /** Numerical id of the module */
        public final byte id;
        
        private Module(byte id) {
            this.id= id;
        }
    }
    
    /**
     * Registers of the MetaWear modules
     * @author Eric Tsai
     */
    public enum Register {
        SWITCH_STATE(Module.MECHANICAL_SWITCH, (byte)0x1, new NotificationDispatcher() {
            @Override
            public void dispatch(Collection<ModuleNotification> notifications, byte[] data) {
                for(ModuleNotification it: notifications) {
                    if (data[2] == 0x1) ((MechanicalSwitch)it).pressed();
                    else ((MechanicalSwitch)it).released();
                }
            }
        }),
        
        LED_COLOR(Module.LED_DRIVER, (byte)0x1, null),
        LED_ENABLE(Module.LED_DRIVER, (byte)0x2, null),
        
        GLOBAL_ENABLE(Module.ACCELEROMETER, (byte)0x1, null),
        DATA_ENABLE(Module.ACCELEROMETER, (byte)0x2, null),
        DATA_SETTINGS(Module.ACCELEROMETER, (byte)0x3, null),
        DATA_VALUE(Module.ACCELEROMETER, (byte)0x4, new NotificationDispatcher() {
            @Override
            public void dispatch(Collection<ModuleNotification> notifications,
                    byte[] data) {
                for(ModuleNotification it: notifications) {
                    ((Accelerometer)it).receivedDataValue((short)(ByteBuffer.wrap(data, 2, 2).getShort() >> 4), 
                            (short)(ByteBuffer.wrap(data, 4, 2).getShort() >> 4), 
                            (short)(ByteBuffer.wrap(data, 6, 2).getShort() >> 4));
                }
            }
        }),
        FREE_FALL_ENABLE(Module.ACCELEROMETER, (byte)0x5, null),
        FREE_FALL_SETTINGS(Module.ACCELEROMETER, (byte)0x6, null),
        FREE_FALL_VALUE(Module.ACCELEROMETER, (byte)0x7, new NotificationDispatcher() {
            @Override
            public void dispatch(Collection<ModuleNotification> notifications,
                    byte[] data) {
                for(ModuleNotification it: notifications) {
                    if (data[2] != 0) ((Accelerometer)it).inFreeFall();
                    else ((Accelerometer)it).stoppedFreeFall();
                }
            }
        }),
        ORIENTATION_ENABLE(Module.ACCELEROMETER, (byte)0x8, null),
        ORIENTATION_SETTING(Module.ACCELEROMETER, (byte)0x9, null),
        ORIENTATION_VALUE(Module.ACCELEROMETER, (byte)0xa, new NotificationDispatcher() {
            @Override
            public void dispatch(Collection<ModuleNotification> notifications,
                    byte[] data) {
                for(ModuleNotification it: notifications) {
                    ((Accelerometer)it).receivedOrientation(data[2]);
                }
            }
        }),
        
        TEMPERATURE(Module.TEMPERATURE, (byte)0x1, new NotificationDispatcher() {
            @Override
            public void dispatch(Collection<ModuleNotification> notifications,
                    byte[] data) {
                byte[] reverse= new byte[] {data[3], data[2]};
                float degrees= (float)(Short.valueOf(ByteBuffer.wrap(reverse).getShort()).floatValue() / 4.0);
                for(ModuleNotification it: notifications) {
                    ((Temperature)it).receivedTemperature(degrees);;
                }
            }
        }),
        
        SET_DIGITAL_OUTPUT(Module.GPIO, (byte)0x1, null),
        CLEAR_DIGITAL_OUTPUT(Module.GPIO, (byte)0x2, null),
        SET_DIGITAL_IN_PULL_UP(Module.GPIO, (byte)0x3, null),
        SET_DIGITAL_IN_PULL_DOWN(Module.GPIO, (byte)0x4, null),
        SET_DIGITAL_IN_NO_PULL(Module.GPIO, (byte)0x5, null),
        READ_ANALOG_INPUT_ABS_VOLTAGE(Module.GPIO, (byte)0x6, new NotificationDispatcher() {
            @Override
            public void dispatch(Collection<ModuleNotification> notifications,
                    byte[] data) {
                short value= ByteBuffer.wrap(data, 2, 2).getShort();
                for(ModuleNotification it: notifications) {
                    ((GPIO)it).receivedAnalogInputAsAbsValue(value);
                }
            }
        }),
        READ_ANALOG_INPUT_SUPPLY_RATIO(Module.GPIO, (byte)0x7, new NotificationDispatcher() {
            @Override
            public void dispatch(Collection<ModuleNotification> notifications,
                    byte[] data) {
                short value= (short)(ByteBuffer.wrap(data, 2, 2).getShort() >> 6);
                for(ModuleNotification it: notifications) {
                    ((GPIO)it).receivedAnalogInputAsSupplyRatio(value);
                }
            }
        }),
        READ_DIGITAL_INPUT(Module.GPIO, (byte)0x8, new NotificationDispatcher() {
            @Override
            public void dispatch(Collection<ModuleNotification> notifications,
                    byte[] data) {
                for(ModuleNotification it: notifications) {
                    ((GPIO)it).receivedDigitalInput(data[2]);
                }
            }
        }),
        
        RESET_DEVICE(Module.TEST, (byte)0x1, null),
        JUMP_TO_BOOTLOADER(Module.TEST, (byte)0x2, null);
        
        /** Module the register corresponds to */
        public final Module module;
        /** Numerical id of the register */
        public final byte id;
        /** Dispatcher to call the appropriate notification functions */
        private final NotificationDispatcher dispatcher;
        
        private Register(Module module, byte id, NotificationDispatcher dispatcher) {
            this.module= module;
            this.id= id;
            this.dispatcher= dispatcher;
        }
        
        /**
         * Constructs a sequence of bytes corresponding to a read
         * @param params Additional parameters to pass to MetaWear 
         * @return Bytes for a read command
         */
        public byte[] buildReadCommand(byte ... params) {
            return  buildCommand((byte)(0x80 | this.id), params);
        }
        /**
         * Constructs a sequence of bytes corresponding to a write
         * @param params Additional parameters to pass to MetaWear 
         * @return Bytes for a write command
         */
        public byte[] buildWriteCommand(byte ... params) {
            return buildCommand(id, params);
        }
        private byte[] buildCommand(byte registerId, byte ... params) {
            byte[] command= new byte[MAX_SIZE];
            
            command[0]= module.id;
            command[1]= registerId;
            int index= 2;
            for(byte it: params) {
                command[index]= it;
                index++;
            }
            return command;
        }
        
        /**
         * Call the appropriate notification functions with the interpreted data
         * @param notifications Collection of notifications that want to be notified
         * @param data Response from MetaWear
         */
        public void notifyCallbacks(Collection<ModuleNotification> notifications, byte[] data) {
            if (dispatcher != null) {
                dispatcher.dispatch(notifications, data);
            }
        }
    }
    
    /** Stores all the registers that belong under each module */
    private final static HashMap<Module, Set<Register>> metaWearCommands;
    /** Caches the results of getRegister */
    private final static HashMap<Byte, HashMap<Byte, Register>> registerLookupCache;
    
    static {
        metaWearCommands= new HashMap<>();
        for(Register it: Register.values()) {
            if (!metaWearCommands.containsKey(it.module)) {
                metaWearCommands.put(it.module, new HashSet<Register>());
            }
            metaWearCommands.get(it.module).add(it);
        }
        
        registerLookupCache= new HashMap<>();
    }
    
    /**
     * Get all of the registers that belong to the requested module
     * @param module Module to look up
     * @return Registers for the module
     */
    public static Collection<Register> getRegisters(Module module) {
        return metaWearCommands.get(module);
    }
    
    /**
     * Find the enum entry corresponding to the numerical module and register values
     * @param moduleId Numerical module id to look up
     * @param registerId Numerical register id to lookup
     * @return Register enum if the moduleId and registerId result in a valid enum, null otherwise
     */
    public static Register getRegister(byte moduleId, byte registerId) {
        if (!registerLookupCache.containsKey(moduleId) || (registerLookupCache.containsKey(moduleId) 
                && !registerLookupCache.get(moduleId).containsKey(registerId))) {
            for(Register it: Register.values()) {
                if (it.module.id == moduleId && it.id == registerId) {
                    if (!registerLookupCache.containsKey(moduleId)) {
                        registerLookupCache.put(moduleId, new HashMap<Byte, Register>());
                    }
                    registerLookupCache.get(moduleId).put(registerId, it);
                    break;
                }
            }
        }
        return registerLookupCache.get(moduleId).get(registerId);
    }
    /**
     * Convenience method for checking if an register belongs under a module
     * @param module Module to search through
     * @param register Register to check
     * @return True if the register belongs to the given module, false otherwise
     */
    public static boolean isRegisterUnderModule(Module module, Register register) {
        return metaWearCommands.get(module).contains(register);
    }
}
