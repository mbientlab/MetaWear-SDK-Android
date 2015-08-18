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
     * Read the analog input voltage
     * @param pin     GPIO pin to read
     * @param mode    Analog read mode
     */
    void readAnalogIn(byte pin, AnalogReadMode mode);

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
         * @deprecated Method renamed to fit the naming scheme of the class, use {@link #fromAnalogIn(byte, AnalogReadMode)} instead
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
    }

    /**
     * Initiates the creation of a route for gpio data
     * @return Selection of available data sources
     */
    SourceSelector routeData();
}
