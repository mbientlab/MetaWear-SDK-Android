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
package com.mbientlab.metawear.api.controller;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

import static com.mbientlab.metawear.api.Module.SETTINGS;

/**
 * Controller for the settings module
 * @author Eric Tsai
 */
public interface Settings extends ModuleController {
    /**
     * Enumeration of registers for the settings module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Sets / Reads the device name */
        DEVICE_NAME {
            @Override public byte opcode() { return 0x1; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte[] nameBytes= new byte[data.length - 2];
                String name= null;
                
                System.arraycopy(data, 2, nameBytes, 0, data.length - 2);
                try {
                    name= new String(nameBytes, "US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    name= new String(nameBytes);
                }
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedDeviceName(name);
                }
            }
        },
        /** Advertisement parameters */
        ADVERTISING_INTERVAL {
            @Override public byte opcode() { return 0x2; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                int interval= ((data[2] & 0xff) | (data[3] << 8)) & 0xffff;
                short timeout= (short) (data[4] & 0xff);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedAdvertisementParams(interval, timeout);
                }
            }
        },
        /** Tx Power */
        TX_POWER {
            @Override public byte opcode() { return 0x3; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedTXPower(data[2]);
                }
            }
        },
        /** Sets bonding state on disconnect */
        DELETE_BOND {
            @Override public byte opcode() { return 0x4; }
        },
        /** Starts an advertisement */
        START_ADVERTISEMENT {
            @Override public byte opcode() { return 0x5; }
        },
        /** Initiates the Bluetooth bonding process */
        INIT_BOND {
            @Override public byte opcode() { return 0x6; }
        };
        
        @Override public Module module() { return SETTINGS; }
        @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) { }
    }
    /**
     * Callbacks for the settings module
     * @author Eric Tsai
     */
    public abstract static class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return SETTINGS; }
        
        /**
         * Called when the device name has been received from the board
         * @param name Up to 8 ASCII characters
         */
        public void receivedDeviceName(String name) { }
        /**
         * Called when the advertisement parameters have been received from the board
         * @param interval Advertisement interval
         * @param timeout Advertisement timeout
         */
        public void receivedAdvertisementParams(int interval, short timeout) { }
        /**
         * Called when the tx power has been received
         * @param power TX power
         */
        public void receivedTXPower(byte power) { }
    }

    /**
     * Remove Bluetooth bond to the board on disconnect
     */
    public void removeBond();
    /**
     * Keep the bond on disconnect
     */
    public void keepBond();
    /**
     * Trigger the board to start advertising
     */
    public void startAdvertisement();
    /**
     * Triggers the board to initiate the Bluetooth bonding process with the 
     * connected Android device
     */
    public void initiateBonding();
    
    /**
     * Read the device name of the board.  When data is available, the 
     * {@link Callbacks#receivedDeviceName(String)} callback function is 
     * executed
     */
    public void readDeviceName();
    /**
     * Read advertisement parameeters from the board.  When data is availlable, 
     * the {@link Callbacks#receivedAdvertisementParams(int, short)} callback 
     * function is executed
     */
    public void readAdvertisingParams();
    /**
     * Read the tx power from the board.  When data is available, the 
     * {@link Callbacks#receivedTXPower(byte)} callback function is executed
     */
    public void readTxPower();
    /**
     * Sets the device name
     * @param name Max of 8 ASCII characters
     * @return Calling object
     */
    public Settings setDeviceName(String name);
    /**
     * Sets the advertisement parameters.  The interval is in units of 0.625 seconds, thus values 
     * set in this function may differ by 1 millisecond when read back with the 
     * {@link #readAdvertisingParams()} function.
     * @param interval Advertisement interval, between [0, 65535] milliseconds
     * @param timeout Advertisement timeout, between [0, 180] seconds where 0 indicates no timeout
     * @return CAlling object
     */
    public Settings setAdvertisingInterval(short interval, byte timeout);
    /**
     * Sets the TX power.  If a non valid value is set, the nearest valid value will be used instead
     * @param power Valid values are: 4, 0, -4, -8, -12, -16, -20, -30
     * @return Calling object
     */
    public Settings setTXPower(byte power);
}
