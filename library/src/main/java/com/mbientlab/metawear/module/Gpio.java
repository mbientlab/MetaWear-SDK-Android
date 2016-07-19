/*
 * Copyright 2014-2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights granted under the terms of a software
 * license agreement between the user who downloaded the software, his/her employer (which must be your
 * employer) and MbientLab Inc, (the "License").  You may not use this Software unless you agree to abide by the
 * terms of the License which can be found at www.mbientlab.com/terms.  The License limits your use, and you
 * acknowledge, that the Software may be modified, copied, and distributed when used in conjunction with an
 * MbientLab Inc, product.  Other than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this Software and/or its documentation for any
 * purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT WARRANTY
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL MBIENTLAB OR ITS LICENSORS BE LIABLE OR
 * OBLIGATED UNDER CONTRACT, NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software, contact MbientLab via email:
 * hello@mbientlab.com.
 */

package com.mbientlab.metawear.module;

import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.MetaWearBoard;

/**
 * Interacts with the general purpose I/O pins
 * @author Eric Tsai
 */
public interface Gpio extends MetaWearBoard.Module {
    /**
     * Read modes for analog input
     * @author Eric Tsai
     */
    enum AnalogReadMode {
        /** Read analog input voltage as an absolute reference */
        ABS_REFERENCE,
        /** Read analog input voltage as an ADC value (supply ratio) */
        ADC
    }

    /**
     * Input pin configuration types
     * @author Eric Tsai
     */
    enum PullMode {
        PULL_UP,
        PULL_DOWN,
        NO_PULL
    }

    /**
     * Pin change types
     * @author Eric Tsai
     */
    enum PinChangeType {
        /** Notify on the rising edge during a change */
        RISING,
        /** Notify on the falling edge during a change */
        FALLING,
        /** Notify on any edge during a change */
        ANY
    }

    /**
     * Builder to construct parameters for the enhanced analog input read.
     * @author Eric Tsai
     */
    interface AnalogInParameterBuilder {
        /**
         * Sets the GPIO pin that will be pulled up before the read.  If unused, no pin will be pulled up
         * @param pin    Pullup pin
         * @return Calling object
         */
        AnalogInParameterBuilder pullUpPin(byte pin);
        /**
         * Sets the GPIO pin that will be pulled down before the read.  If unused, no pin will be pulled down
         * @param pin    Pulldown pin
         * @return Calling object
         */
        AnalogInParameterBuilder pullDownPin(byte pin);
        /**
         * Sets how long to wait before reading from the pin.  If unused, the read will happen when the command is issued.
         * @param delay    Delay time, in microseconds (&#956;s)
         * @return Calling object
         */
        AnalogInParameterBuilder delay(short delay);
        /**
         * GPIO pin the returned data identifies as.  If used, the virtual pin value must match the pin used
         * for {@link #readAnalogIn(byte, AnalogReadMode)} and {@link #readAnalogIn(byte, AnalogReadMode, boolean)}
         * when constructing a data route for analog data.
         * @param pin    Virtual pin identifying the data
         * @return Calling object
         */
        AnalogInParameterBuilder virtualPin(byte pin);
        /**
         * Sets silent mode.  If unused, it will default to false.  The silent value must match the silent flag used
         * for {@link #readAnalogIn(byte, AnalogReadMode, boolean)}.
         * @param silent    True if read should be silent
         * @return Calling object
         */
        AnalogInParameterBuilder silent(boolean silent);
        /**
         * Commit the read parameters to the board
         */
        void commit();
    }

    /**
     * Read the analog input voltage
     * @param pin     GPIO pin to read
     * @param mode    Analog read mode
     */
    void readAnalogIn(byte pin, AnalogReadMode mode);
    /**
     * Read the analog input voltage
     * @param pin     GPIO pin to read
     * @param mode    Analog read mode
     */
    void readAnalogIn(byte pin, AnalogReadMode mode, boolean silent);
    /**
     * Enhanced version of the readAnalogIn functions that combines analog reads with pullup/pulldown commands
     * in one function.  This function is only available on boards running firmware v1.2.2 or later.
     * @param pin     GPIO pin to read
     * @param mode    Analog read mode
     * @return Builder to assign desired parameters
     */
    AnalogInParameterBuilder initiateAnalogInRead(byte pin, AnalogReadMode mode);

    /**
     * Sets pull mode on a pin
     * @param pin     GPIO pin to configure
     * @param mode    New pull mode
     */
    void setPinPullMode(byte pin, PullMode mode);

    /**
     * Read the digital input state
     * @param pin    GPIO pin to read
     */
    void readDigitalIn(byte pin);
    /**
     * Read the digital input state
     * @param pin    GPIO pin to read
     * @param silent True if read should be silent
     */
    void readDigitalIn(byte pin, boolean silent);
    /**
     * Sets the digital output state of a pin
     * @param pin    GPIO pin to set
     */
    void setDigitalOut(byte pin);
    /**
     * Clears the digital output state of a pin
     * @param pin    GPIO pin to clear
     */
    void clearDigitalOut(byte pin);

    /**
     * Sets change type to monitor
     * @param pin     GPIO pin to configure
     * @param type    Change type to monitor
     */
    void setPinChangeType(byte pin, PinChangeType type);
    /**
     * Starts pin change detection
     * @param pin    GPIO pin to monitor
     */
    void startPinChangeDetection(byte pin);
    /**
     * Stops pin change detection
     * @param pin    GPIO pin to stop monitoring
     */
    void stopPinChangeDetection(byte pin);

    /**
     * Selector for available Gpio data sources
     * @author Eric Tsai
     */
    interface SourceSelector {
        /**
         * @deprecated Method renamed to fit the naming scheme of the class, use {@link #fromAnalogIn(byte, Gpio.AnalogReadMode)} instead
         */
        @Deprecated
        DataSignal fromAnalogGpio(byte pin, Gpio.AnalogReadMode mode);
        /**
         * @deprecated Method renamed to fit the naming scheme of the class, use {@link #fromDigitalInChange(byte)} instead
         */
        @Deprecated
        DataSignal fromGpioPinNotify(byte pin);

        /**
         * Handle analog input data
         * @param pin    GPIO pin the analog data is coming from
         * @param mode   Read mode used to retrieve the data
         * @return Object representing the data from an analog input
         * @see Gpio#readAnalogIn(byte, AnalogReadMode)
         */
        DataSignal fromAnalogIn(byte pin, Gpio.AnalogReadMode mode);
        /**
         * Handle digital input data
         * @param pin    GPIO pin the digital data is coming from
         * @return Object representing the data from a digital input
         * @see Gpio#readDigitalIn(byte)
         */
        DataSignal fromDigitalIn(byte pin);
        /**
         * Handle digital state change notification
         * @param pin    GPIO pin the notification is coming from
         * @return Object representing the data from a state change notification
         * @see Gpio#startPinChangeDetection(byte)
         */
        DataSignal fromDigitalInChange(byte pin);
        /**
         * Handle analog input data.  This version of the function pairs with the
         * {@link #readAnalogIn(byte, AnalogReadMode, boolean)} variant.
         * @param pin    GPIO pin the analog data is coming from
         * @param mode   Read mode used to retrieve the data
         * @param silent Same value as the silent parameter for calling {@link #readAnalogIn(byte, AnalogReadMode, boolean)}
         * @return Object representing the data from an analog input
         * @see Gpio#readAnalogIn(byte, AnalogReadMode)
         */
        DataSignal fromAnalogIn(byte pin, Gpio.AnalogReadMode mode, boolean silent);
        /**
         * Handle digital input data.  This version of the function pairs with the
         * {@link #readDigitalIn(byte, boolean)} variant.
         * @param pin    GPIO pin the digital data is coming from
         * @param silent Same value as the silent parameter for calling {@link #readDigitalIn(byte, boolean)}
         * @return Object representing the data from a digital input
         * @see Gpio#readDigitalIn(byte)
         */
        DataSignal fromDigitalIn(byte pin, boolean silent);
    }

    /**
     * Initiates the creation of a route for gpio data
     * @return Selection of available data sources
     */
    SourceSelector routeData();
}
