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

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.MetaWearBoard;

import java.util.UUID;

/**
 * Controller for IBeacon mode
 * @author Eric Tsai
 */
public interface IBeacon extends MetaWearBoard.Module {
    /**
     * Configures IBeacon settings
     * @return Editor object to configure various settings
     */
    ConfigEditor configure();

    /**
     * Enable IBeacon mode.  You will need to disconnect from the board to advertise as an IBeacon
     */
    void enable();

    /**
     * Disable IBeacon mode
     */
    void disable();

    /**
     * Interface for configuring IBeacon settings
     * @author Eric Tsai
     */
    interface ConfigEditor {
        /**
         * Sets the advertising UUID
         * @param adUuid    New advertising UUID
         * @return Calling object
         */
        ConfigEditor setUUID(UUID adUuid);

        /**
         * Sets the advertising major number
         * @param major    New advertising major number
         * @return Calling object
         */
        ConfigEditor setMajor(short major);

        /**
         * Sets the advertising minor number
         * @param minor    New advertising minor number
         * @return Calling object
         */
        ConfigEditor setMinor(short minor);

        /**
         * Sets the advertising receiving power
         * @param power    New advertising receiving power
         * @return Calling object
         */
        ConfigEditor setRxPower(byte power);

        /**
         * Sets the advertising transmitting power
         * @param power    New advertising transmitting power
         * @return Calling object
         */
        ConfigEditor setTxPower(byte power);

        /**
         * Sets the advertising period
         * @param period    New advertising period, in milliseconds
         * @return Calling object
         */
        ConfigEditor setAdPeriod(short period);

        /**
         * Writes the new settings to the board
         */
        void commit();
    }

    /**
     * Wrapper class encapsulating the IBeacon configuration settings
     * @author Eric Tsai
     */
    interface Configuration {
        /**
         * Retrieves the advertising UUID
         * @return Advertising UUID
         */
        UUID adUuid();

        /**
         * Retrieves the advertising major number
         * @return Advertising major number
         */
        short major();

        /**
         * Retrieves the advertising minor number
         * @return Advertising minor number
         */
        short minor();

        /**
         * Retrieves the advertising receiving power
         * @return Advertising receiving power
         */
        byte rxPower();

        /**
         * Retrieves the advertising transmitting power
         * @return Advertising transmitting power
         */
        byte txPower();

        /**
         * Retrieves the advertising period
         * @return Advertising period, in milliseconds
         */
        short adPeriod();
    }

    /**
     * Read the current IBeacon configuration
     * @return Configuration object that will be available when the read operation completes
     */
    AsyncOperation<Configuration> readConfiguration();
}