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

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

import static com.mbientlab.metawear.api.Module.ACCELEROMETER;


/**
 * Controller for the accelerometer module
 * @author Eric Tsai
 * @see com.mbientlab.metawear.api.Module#ACCELEROMETER
 */
public interface Accelerometer extends ModuleController {
    /**
     * Enumeration of registers for the accelerometer module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Checks module enable status and enables/disables module notifications */
        GLOBAL_ENABLE {
            @Override public byte opcode() { return 0x1; }
        },
        /** Checks motion polling status and enables/disables motion polling */
        DATA_ENABLE {
            @Override public byte opcode() { return 0x2; }
        },
        /** Sets or retrieves motion polling configuration */
        DATA_SETTINGS {
            @Override public byte opcode() { return 0x3; }
        },
        /** Stores XYZ motion data. */
        DATA_VALUE {
            @Override public byte opcode() {return 0x4; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                short x= (short)(ByteBuffer.wrap(data, 2, 2).getShort() >> 4), 
                        y= (short)(ByteBuffer.wrap(data, 4, 2).getShort() >> 4), 
                        z= (short)(ByteBuffer.wrap(data, 6, 2).getShort() >> 4);
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).receivedDataValue(x, y, z);
                }
            }
        },
        /** Checks free fall detection status and enables/disables free fall detection */
        FREE_FALL_ENABLE {
            @Override public byte opcode() { return 0x5; }
        },
        /** Sets or retrieves free fall detection configuration */
        FREE_FALL_SETTINGS {
            @Override public byte opcode() { return 0x6; }
        },
        /** Stores free fall state */
        FREE_FALL_VALUE { 
            @Override public byte opcode() { return 0x7; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                for(ModuleCallbacks it: callbacks) {
                    if (data[2] != 0) ((Callbacks)it).inFreeFall();
                    else ((Callbacks)it).stoppedFreeFall();
                }
            }
        },
        /** Sets or retrieves orientation notification status, and enables/disables orientation notifications */
        ORIENTATION_ENABLE {
            @Override public byte opcode() { return 0x8; }
        },
        /** Sets or retrieves the configuration for orientation notifications */
        ORIENTATION_SETTING {
            @Override public byte opcode() { return 0x9; }
        },
        /** Stores current orientation */
        ORIENTATION_VALUE {
            @Override public byte opcode() { return 0xa; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).receivedOrientation(data[2]);
                }
            }
        };
        
        @Override public Module module() { return ACCELEROMETER; }
        @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) { }
    }
    /**
     * Callbacks for the accelerometer module
     * @author Eric Tsai
     */
    public abstract static class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return ACCELEROMETER; }

        /**
         * Called when the accelerometer has sent its XYZ motion data
         * @param x X component of the motion
         * @param y Y component of the motion
         * @param z Z component of the motion
         */
        public void receivedDataValue(short x, short y, short z) { }
        /** Called when free fall is detected */
        public void inFreeFall() { }
        /** Called when free fall has stopped */
        public void stoppedFreeFall() { }
        /** Called when the orientation has changed */
        public void receivedOrientation(byte orientation) { }
    }

    /**
     * Enumeration of components in the accelerometer
     * @author Eric Tsai
     */
    public enum Component {
        DATA(Register.DATA_ENABLE, Register.DATA_SETTINGS),
        FREE_FALL(Register.FREE_FALL_ENABLE, Register.FREE_FALL_SETTINGS),
        ORIENTATION(Register.ORIENTATION_ENABLE, Register.ORIENTATION_SETTING);
        
        public final Register enable, config;

        /**
         * @param enable
         * @param config
         */
        private Component(Register enable, Register config) {
            this.enable = enable;
            this.config = config;
        }
        
    }
    /**
     * Enable notifications from a component
     * @param component Component to enable notifications from
     */
    public void enableNotification(Component component);
    /**
     * Disable notifications from a component
     * @param component Component to disable notifications from
     */
    public void disableNotification(Component component);
    
    /**
     * Read component configuration
     * @param component Component to read configuration from
     */
    public void readComponentConfiguration(Component component);
    /**
     * Set component configuration
     * @param component Component to write configuration to
     */
    public void setComponentConfiguration(Component component, byte[] data);
    
}
