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

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.MetaWearBoard.Module;

/**
 * General purpose I/O pins for connecting external sensors
 * @author Eric Tsai
 */
public interface Gpio extends Module {
    /** Value indicating that the pin is unused for {@link Analog#read(byte, byte, short, byte)} */
    byte UNUSED_READ_PIN = (byte) 0xff;
    /** Value indicating that the delay is unused for {@link Analog#read(byte, byte, short, byte)} */
    byte UNUSED_READ_DELAY = 0;
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
     * Input pin configuration types
     * @author Eric Tsai
     */
    enum PullMode {
        PULL_UP,
        PULL_DOWN,
        NO_PULL
    }

    /**
     * Represents one GPIO pin
     * @author Eric Tsai
     */
    interface Pin {
        /**
         * Checks if the pin is a virtual pin
         * @return True if virtual
         */
        boolean isVirtual();

        /**
         * Set the pin change type to look for
         * @param type    New pin change type
         */
        void setChangeType(PinChangeType type);
        /**
         * Set the pin's pull mode
         * @param mode    New pull mode
         */
        void setPullMode(PullMode mode);
        /**
         * Clear the pin's output voltage i.e. logical low
         */
        void clearOutput();
        /**
         * Set the pin's output voltage i.e. logical high
         */
        void setOutput();

        /**
         * Get an implementation of the Analog interface for ADC data, represented as a short
         * @return Object representing analog adc data
         */
        Analog analogAdc();
        /**
         * Get an implementation of the Analog interface for analog reference voltage data, represented
         * as a float with units of volts (V).
         * @return Object representing analog reference voltage data
         */
        Analog analogAbsRef();
        /**
         * Get an implementation of the ForcedDataProducer interface for digital data, represented as a byte
         * @return Object representing digital data
         */
        ForcedDataProducer digital();
        /**
         * Get an implementation of the AsyncDataProducer interface for the pin monitoring data, represented as a byte
         * @return Object representing digital pin monitoring data
         */
        AsyncDataProducer monitor();
    }

    /**
     * Measures analog data from a gpio pin
     * @author Eric Tsai
     */
    interface Analog extends ForcedDataProducer {
        /**
         * Variant of the {@link #read()} function that provides finer control of the analog read operation
         * @param pullup      Pin to be pulled up before the read, set to {@link #UNUSED_READ_PIN} if not used
         * @param pulldown    Pin to be pulled down before the read, set to {@link #UNUSED_READ_PIN} if not used
         * @param delay       How long to wait before reading from the pin, in microseconds, set to {@link #UNUSED_READ_DELAY} if not used
         * @param virtual     Pin number the data will identify as, set to {@link #UNUSED_READ_PIN} if not used.  Objects representing virtual
         *                    pins are created with {@link #getVirtualPin(byte)}
         */
        void read(byte pullup, byte pulldown, short delay, byte virtual);
    }
    /**
     * Retrieves a {@link Pin} object representing a physical GPIO pin
     * @param index    Index to retrieve
     * @return Object for a physical pin, null if index represents a non-existent pin
     */
    Pin pin(byte index);
    /**
     * Gets an object representing a virtual pin for data directed to it by the {@link Analog#read(byte, byte, short, byte)} method
     * @param index    Index the virtual pin is using
     * @return Virtual pin
     */
    Pin getVirtualPin(byte index);
}
