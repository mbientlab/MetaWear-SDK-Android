/*
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
package com.mbientlab.metawear.api.controller;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

import static com.mbientlab.metawear.api.Module.IBEACON;

/**
 * Controller for the IBeacon module
 * @author Eric Tsai
 * @see com.mbientlab.metawear.api.Module#IBEACON
 */
public interface IBeacon extends ModuleController {
    /**
     * Enumeration of registers under the IBeacon module.  
     * The registers also function as keys for the IBeacon settings. 
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Checks the enable status and enables/disables IBeacon mode */
        ENABLE { 
            @Override public byte opcode() { return 0x1; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                for(ModuleCallbacks it: callbacks) ((Callbacks)it).receivedEnableState(data[2]);
            }
        },
        /**
         * Contains IBeacon advertisement UUID.  Reading from the register will trigger 
         * a call to receivedUUID with the advertisement uuid
         * @see Callbacks#receivedUUID(UUID)
         */
        ADVERTISEMENT_UUID {
            @Override public byte opcode() { return 0x2; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                UUID uuid= new UUID(ByteBuffer.wrap(data, 2, 8).getLong(), 
                        ByteBuffer.wrap(data, 10, 8).getLong());
                for(ModuleCallbacks it: callbacks) ((Callbacks)it).receivedUUID(uuid);
            }
        },
        /**
         * Contains advertisement major number.  Reading from the register will trigger 
         * a call to receivedMajor with the major number
         * @see Callbacks#receivedMajor(short)
         */
        MAJOR {
            @Override public byte opcode() { return 0x3; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                short major= ByteBuffer.wrap(data, 2, 2).getShort();
                for(ModuleCallbacks it: callbacks) ((Callbacks)it).receivedMajor(major);
            }
        },
        /**
         * Contains advertisement minor number.  Reading from the register will trigger 
         * a call to receivedMinor with the minor number
         * @see Callbacks#receivedMinor(short)
         */
        MINOR {
            @Override public byte opcode() { return 0x4; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                short minor= ByteBuffer.wrap(data, 2, 2).getShort();
                for(ModuleCallbacks it: callbacks) ((Callbacks)it).receivedMinor(minor);
            }
        },
        /**
         * Contains the receiving power.  Reading from the register will trigger 
         * a call to receivedRXPower with the receiving power
         * @see Callbacks#receivedRXPower(byte)
         */
        RX_POWER {
            @Override public byte opcode() { return 0x5; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) {
                for(ModuleCallbacks it: callbacks) ((Callbacks)it).receivedRXPower(data[2]);
            }
        },
        /**
         * Contains the transmitting power.  Reading from the register will trigger 
         * a call to receivedTXPower with the transmitting power
         * @see Callbacks#receivedTXPower(byte)
         */
        TX_POWER {
            @Override public byte opcode() { return 0x6; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                for(ModuleCallbacks it: callbacks) ((Callbacks)it).receivedTXPower(data[2]);
            }
        },
        /**
         * Contains the advertisement period.  Reading from the register will trigger 
         * a call to receivedPeriod with the advertisement period
         * @see Callbacks#receivedPeriod(short)
         */
        ADVERTISEMENT_PERIOD {
            @Override public byte opcode() { return 0x7; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) {
                short period= ByteBuffer.wrap(data, 2, 2).getShort();
                for(ModuleCallbacks it: callbacks) ((Callbacks)it).receivedPeriod(period);
            }
        };
        
        @Override public Module module() { return IBEACON; }
    }
    /**
     * Callbacks for the IBeacon module
     * @author Eric Tsai
     */
    public abstract static class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return Module.IBEACON; }
        
        /**
         * Called when the enable state has been read
         * @param state 0 if disabled, 1 if enabled
         */
        public void receivedEnableState(byte state) { }
        /**
         * Called when the advertisement UUID has been read
         * @param uuid Advertisement UUID
         */
        public void receivedUUID(UUID uuid) { }
        /**
         * Called when the major number has been read
         * @param value Value of the major number
         */
        public void receivedMajor(short value) { }
        /**
         * Called when the minor number has been read
         * @param value Value of the minor number
         */
        public void receivedMinor(short value) { }
        /**
         * Called when the calibrated receiving power has been read
         * @param power Calibrated receive power, default is -55dBm
         */
        public void receivedRXPower(byte power) { }
        /**
         * Called when the transmitting power has been read
         * @param power Transmitting power, default is 0dBm
         */
        public void receivedTXPower(byte power) { }
        /**
         * Called when the advertising period has been read
         * @param period Advertising period in milliseconds
         */
        public void receivedPeriod(short period) { }
    }
    
    /**
     * Enable the IBeacon module
     */
    public void enableIBeacon();
    /**
     * Disable the IBeacon module
     */
    public void disableIBecon();
    /**
     * Reads the IBeacon setting.  
     * @param setting Setting to read
     */
    public void readSetting(Register setting);
    /**
     * Set IBeacon advertisement UUID
     * @param uuid Advertisement UUID
     */
    public IBeacon setUUID(UUID uuid);
    
    /**
     * Set IBeacon advertisement major number
     * @param major Advertisement major number
     */
    public IBeacon setMajor(short major);
    
    /**
     * Set IBeacon advertisement minor number
     * @param minor Advertisement minor number
     */
    public IBeacon setMinor(short minor);
    /**
     * Set IBeacon receiving power
     * @param power Receiving power
     */
    public IBeacon setCalibratedRXPower(byte power);
    /**
     * Set IBeacon transmitting power
     * @param power Transmitting power
     */
    public IBeacon setTXPower(byte power);
    /**
     * Set IBeacon advertisement period
     * @param period Advertisement period, in milliseconds
     */
    public IBeacon setAdvertisingPeriod(short period);
}
