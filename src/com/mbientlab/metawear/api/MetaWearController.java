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

/**
 * API for interacting with the MetaWear board
 * @author Eric Tsai
 */
public interface MetaWearController {
    /**
     * States the LED can be in
     * @author Eric Tsai
     */
    public enum LEDState {
        /** LED is off */
        OFF((byte)0),
        /** LED is on */
        ON((byte)1),
        /** LED is blinking */
        BLINK((byte)2);
        
        /** Byte value of the setting */
        public final byte setting;
        
        private LEDState(byte setting) {
            this.setting= setting;
        }
    }
    
    /**
     * MetaWear registers that can broadcast notifications when their values change 
     * @author Eric Tsai
     */
    public enum NotificationRegister {
        /**
         * Accelerometer XYZ motion data.
         * @see BroadcastReceiverBuilder.Accelerometer#receivedDataValue(short, short, short)
         * @see Commands.Register#DATA_ENABLE
         */
        ACCELEROMETER_DATA(Commands.Register.DATA_ENABLE, true),
        /**
         * Accelerometer Free fall detection
         * @see BroadcastReceiverBuilder.Accelerometer#inFreeFall()
         * @see BroadcastReceiverBuilder.Accelerometer#stoppedFreeFall()
         * @see Commands.Register#FREE_FALL_ENABLE
         */
        FREE_FALL_DETECTION(Commands.Register.FREE_FALL_ENABLE, true),
        /**
         * Accelerometer orientation changes
         * @see BroadcastReceiverBuilder.Accelerometer#receivedOrientation(byte)
         * @see Commands.Register#ORIENTATION_ENABLE
         */
        ORIENTATION(Commands.Register.ORIENTATION_ENABLE, true),
        /**
         * Switch state changes
         * @see BroadcastReceiverBuilder.MechanicalSwitch#released()
         * @see BroadcastReceiverBuilder.MechanicalSwitch#pressed()
         * @see Commands.Register#SWITCH_STATE
         */
        MECHANICAL_SWITCH(Commands.Register.SWITCH_STATE, false);
        
        /** Register corresponding to the notification */
        public final Commands.Register register;
        /** True if the notification also requires a global enable */
        public final boolean requireGlobalEnable;
        
        private NotificationRegister(Commands.Register register, boolean globalEnable) {
            this.register= register;
            this.requireGlobalEnable= globalEnable;
        }
    }
    /**
     * Available reading modes from the GPIO analog pins
     * @author Eric Tsai
     */
    public enum AnalogMode {
        /**
         * Read voltage as an absolute value
         * @see BroadcastReceiverBuilder.GPIO#receivedAnalogInputAsAbsValue(short)
         * @see Commands.Register#READ_ANALOG_INPUT_ABS_VOLTAGE
         */
        ABSOLUTE_VALUE(Commands.Register.READ_ANALOG_INPUT_ABS_VOLTAGE),
        /**
         * Read voltage as a supply ratio
         * @see BroadcastReceiverBuilder.GPIO#receivedAnalogInputAsSupplyRatio(short)
         * @see Commands.Register#READ_ANALOG_INPUT_SUPPLY_RATIO
         */
        SUPPLY_RATIO(Commands.Register.READ_ANALOG_INPUT_SUPPLY_RATIO);
        
        /** Op code corresponding to the specific read */
        public final Commands.Register register;
        
        /**
         * Construct an enum entry with the desired register
         * @param register Register the enum represents
         */
        private AnalogMode(Commands.Register register) {
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
         * @see Commands.Register#SET_DIGITAL_IN_PULL_UP
         */
        UP(Commands.Register.SET_DIGITAL_IN_PULL_UP),
        /**
         * Set with pull down
         * @see Commands.Register#SET_DIGITAL_IN_PULL_DOWN
         */
        DOWN(Commands.Register.SET_DIGITAL_IN_PULL_DOWN),
        /**
         * Set with no pull
         * @see Commands.Register#SET_DIGITAL_IN_NO_PULL
         */
        NONE(Commands.Register.SET_DIGITAL_IN_NO_PULL);
        
        /** Register corresponding to the pull mode */
        public final Commands.Register register;
        
        private PullMode(Commands.Register register) {
            this.register= register;
        }
    }
    
    /**
     * Set the LED state to: off, on, or pulse
     * @param state State to set the LED to
     * @see LEDState
     */
    public void setLEDState(LEDState state);
    /**
     * Sets the LED color
     * @param red Number between [0-255] for the red value 
     * @param green Number between [0-255] for the green value
     * @param blue Number between [0-255] for the blue value
     */
    public void setLEDColor(byte red, byte green, byte blue);
    
    /**
     * Read the temperature reported from MetaWear.
     * The function Temperature.receivedTemperature will be called the the data is available
     * @see BroadcastReceiverBuilder.Temperature#receivedTemperature(float)
     */
    public void readTemperature();
    
    
    /**
     * Read the value of an analog pin.
     * When the data is ready, GPIO.receivedAnalogInputAsAbsValue will be called if the analog mode 
     * is set to ABSOLUTE_VALUE.  If mode is set to SUPPLY_RATIO, GPIO.receivedAnalogInputAsSupplyRatio 
     * will be called instead 
     * @param pin Pin to read
     * @param mode Read mode on the pin
     * @see BroadcastReceiverBuilder.GPIO#receivedAnalogInputAsAbsValue(short)
     * @see BroadcastReceiverBuilder.GPIO#receivedAnalogInputAsSupplyRatio(short)
     */
    public void readAnalogInput(byte pin, AnalogMode mode);
    /**
     * Read the value of a digital pin.
     * When data is available, GPIO.receivedDigitalInput will be called
     * @param pin Pin to read
     * @see BroadcastReceiverBuilder.GPIO#receivedDigitalInput(byte)
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
    
    /** Restart the device */
    public void resetDevice();
    /** Jump to the MetaWear bootloader */
    public void jumpToBootloader();
    
    /**
     * Enable a MetaWear notification
     * @param notification Notification to enable
     */
    public void enableNotification(NotificationRegister notification);
    /**
     * Disable a MetaWear notification
     * @param notification Notification to disable
     */
    public void disableNotification(NotificationRegister notification);
    
    /**
     * Reads general device information from the connected MetaWear board.
     * A Intent with the action CHARACTERISTIC_READ will be broadcasted
     * @see Actions.BluetoothLE#CHARACTERISTIC_READ
     */
    public void readDeviceInformation();
}
