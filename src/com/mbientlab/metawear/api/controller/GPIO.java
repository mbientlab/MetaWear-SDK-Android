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
import java.nio.ByteOrder;
import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

import static com.mbientlab.metawear.api.Module.GPIO;

/**
 * Controller for the general purpose I/O module
 * @see com.mbientlab.metawear.api.Module#GPIO
 * @author Eric Tsai
 */
public interface GPIO extends ModuleController {
    /**
     * Enumeration of registers under the GPIO module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Sets a digital output pin */
        SET_DIGITAL_OUTPUT {
            @Override public byte opcode() { return 0x1; }
        },
        /** Clears a digital output pin */
        CLEAR_DIGITAL_OUTPUT {
            @Override public byte opcode() { return 0x2; }
        },
        /** Sets a digital input pin in pull up mode */
        SET_DIGITAL_IN_PULL_UP {
            @Override public byte opcode() { return 0x3; }
        },
        /** Sets a digital input pin in pull down mode */
        SET_DIGITAL_IN_PULL_DOWN {
            @Override public byte opcode() { return 0x4; }
        },
        /** Clears a digital input pin in with no pull mode */
        SET_DIGITAL_IN_NO_PULL {
            @Override public byte opcode() { return 0x5; }
            
        },
        /** Reads the analog input voltage as an absolute value */
        READ_ANALOG_INPUT_ABS_VOLTAGE {
            @Override public byte opcode() { return 0x6; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                short value= ByteBuffer.wrap(data, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).receivedAnalogInputAsAbsValue(value);
                }
            }
        },
        /** Reads the analog input voltage as a ratio to the supply voltage */
        READ_ANALOG_INPUT_SUPPLY_RATIO {
            @Override public byte opcode() { return 0x7; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                short value= ByteBuffer.wrap(data, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).receivedAnalogInputAsSupplyRatio(value);
                }
            }
        },
        /** Reads the value from a digital input pin */
        READ_DIGITAL_INPUT {
            @Override public byte opcode() { return 0x8; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).receivedDigitalInput(data[2]);
                }
            }
        };
        
        public static Register[] values= Register.values();
        
        @Override public Module module() { return GPIO; }
        @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) { }
    }
    /**
     * Callbacks for the GPIO module 
     * @author Eric Tsai
     */
    public abstract static class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return Module.GPIO; }
        
        /**
         * Called when the analog input has been read as an absolute value
         * @param value Value in mV
         */
        public void receivedAnalogInputAsAbsValue(short value) { }
        /**
         * Called when the analog input has been read as a supply ratio
         * @param value 10 bit representation of the voltage where 0 = 0V and 1023 = 3V
         */
        public void receivedAnalogInputAsSupplyRatio(short value) { }
        /**
         * Called when the digital input has been read
         * @param value Either 0 or 1
         */
        public void receivedDigitalInput(byte value) { }
    }

    /**
     * Available reading modes from the GPIO analog pins
     * @author Eric Tsai
     */
    public enum AnalogMode {
        /**
         * Read voltage as an absolute value
         * @see Callbacks#receivedAnalogInputAsAbsValue(short)
         * @see Register#READ_ANALOG_INPUT_ABS_VOLTAGE
         */
        ABSOLUTE_VALUE(Register.READ_ANALOG_INPUT_ABS_VOLTAGE),
        /**
         * Read voltage as a supply ratio
         * @see Callbacks#receivedAnalogInputAsSupplyRatio(short)
         * @see Register#READ_ANALOG_INPUT_SUPPLY_RATIO
         */
        SUPPLY_RATIO(Register.READ_ANALOG_INPUT_SUPPLY_RATIO);
        
        /** Op code corresponding to the specific read */
        public final Register register;
        
        /**
         * Construct an enum entry with the desired register
         * @param register Register the enum represents
         */
        private AnalogMode(Register register) {
            this.register= register;
        }
    }
    
    /**
     * Pull modes for setting digital input pins
     * @author Eric Tsai
     */
    public enum PullMode {
        /**
         * Set with pull up
         * @see Register#SET_DIGITAL_IN_PULL_UP
         */
        UP(Register.SET_DIGITAL_IN_PULL_UP),
        /**
         * Set with pull down
         * @see Register#SET_DIGITAL_IN_PULL_DOWN
         */
        DOWN(Register.SET_DIGITAL_IN_PULL_DOWN),
        /**
         * Set with no pull
         * @see Register#SET_DIGITAL_IN_NO_PULL
         */
        NONE(Register.SET_DIGITAL_IN_NO_PULL);
        
        public static final PullMode[] values= PullMode.values();
        /** Register corresponding to the pull mode */
        public final Register register;
        
        private PullMode(Register register) {
            this.register= register;
        }
    }
    /**
     * Read the value of an analog pin.
     * When the data is ready, GPIO.receivedAnalogInputAsAbsValue will be called if the analog mode 
     * is set to ABSOLUTE_VALUE.  If mode is set to SUPPLY_RATIO, GPIO.receivedAnalogInputAsSupplyRatio 
     * will be called instead 
     * @param pin Pin to read
     * @param mode Read mode on the pin
     * @see Callbacks#receivedAnalogInputAsAbsValue(short)
     * @see Callbacks#receivedAnalogInputAsSupplyRatio(short)
     */
    public void readAnalogInput(byte pin, AnalogMode mode);
    /**
     * Read the value of a digital pin.
     * When data is available, GPIO.receivedDigitalInput will be called
     * @param pin Pin to read
     * @see Callbacks#receivedDigitalInput(byte)
     */
    public void readDigitalInput(byte pin);
    /**
     * Set a digital output pin
     * @param pin Pin to set
     */
    public void setDigitalOutput(byte pin);
    /**
     * Clear a digital output pin
     * @param pin Pin to clear
     */
    public void clearDigitalOutput(byte pin);
    /**
     * Set a digital input pin
     * @param pin Pin to set
     * @param mode Pull mode to use
     */
    public void setDigitalInput(byte pin, PullMode mode);
}
