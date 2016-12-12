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
import com.mbientlab.metawear.CodeBlock;
import com.mbientlab.metawear.Observer;
import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.MetaWearBoard.Module;

import java.util.Locale;

import bolts.Task;

/**
 * Configures Bluetooth settings and auxiliary hardware and firmware features
 * @author Eric Tsai
 */
public interface Settings extends Module {
    /**
     * Configures the connection parameters
     * @return Editor object to configure the connection parameters
     */
    ConnectionParametersEditor configureConnectionParameters();
    /**
     * Reads the current connection parameters
     * @return ConnectionParameters object, available when the read is completed
     */
    Task<ConnectionParameters> readConnectionParameters();

    /**
     * Configures advertisement settings
     * @return Editor object to configure settings
     */
    AdvertisementConfigEditor configure();
    /**
     * Reads the current advertisement settings
     * @return Advertisement configuration object, available when the read is completed
     */
    Task<AdvertisementConfig> readAdConfig();

    /**
     * Starts advertising
     */
    void startAdvertisement();

    /**
     * Wrapper class encapsulating the advertisement configuration
     * @author Eric Tsai
     */
    interface AdvertisementConfig {
        /**
         * Retrieves the device's advertising name
         * @return Device name
         */
        String deviceName();

        /**
         * Retrieves the advertising interval
         * @return Advertising interval
         */
        int interval();

        /**
         * Retrieves the advertising timeout
         * @return Advertising timeout
         */
        short timeout();

        /**
         * Retrieves the advertising transmitting power
         * @return Advertising transmitting power
         */
        byte txPower();

        /**
         * Retrieves the scan response
         * @return Advertising packet
         */
        byte[] scanResponse();
    }
    /**
     * Interface for configuring the advertisement settings
     * @author Eric Tsai
     */
    interface AdvertisementConfigEditor {
        /**
         * Sets the device's advertising name
         * @param name    Device name, max of 8 ASCII characters
         * @return Calling object
         */
        AdvertisementConfigEditor deviceName(String name);

        /**
         * Sets advertising intervals
         * @param interval    Advertisement interval, between [0, 65535] milliseconds
         * @param timeout     Advertisement timeout, between [0, 180] seconds where 0 indicates no timeout
         * @return Calling object
         */
        AdvertisementConfigEditor adInterval(short interval, byte timeout);

        /**
         * Sets advertising transmitting power.  If a non valid value is set, the nearest valid value will be used instead
         * @param power    Valid values are: 4, 0, -4, -8, -12, -16, -20, -30
         * @return Calling object
         */
        AdvertisementConfigEditor txPower(byte power);

        /**
         * Set a custom scan response packet
         * @param response    Byte representation of the response
         * @return Calling object
         */
        AdvertisementConfigEditor scanResponse(byte[] response);

        /**
         * Writes the new settings to the board
         */
        void commit();
    }

    /**
     * Wrapper class containing the connection parameters
     * @author Eric Tsai
     */
    interface ConnectionParameters {
        /**
         * Retrieves the minimum connection interval
         * @return Minimum connection interval
         */
        float minConnectionInterval();

        /**
         * Retrieves the maximum connection interval
         * @return Maximum connection interval
         */
        float maxConnectionInterval();

        /**
         * Retrieves the slave latency value
         * @return Slave latency value
         */
        short slaveLatency();

        /**
         * Retrieves the supervisor timeout value
         * @return Supervisor timeout value
         */
        short supervisorTimeout();
    }
    /**
     * Interface for configuring the Bluetooth LE connection parameters
     * @author Eric Tsai
     */
    interface ConnectionParametersEditor {
        /**
         * Sets the lower bound of the connection interval
         * @param interval    Lower bound, at least 7.5ms
         * @return Calling object
         */
        ConnectionParametersEditor minConnectionInterval(float interval);

        /**
         * Sets the upper bound of the connection interval
         * @param interval    Upper bound, at most 4000ms
         * @return Calling object
         */
        ConnectionParametersEditor maxConnectionInterval(float interval);

        /**
         * Sets the number of connection intervals to skip
         * @param latency    Number of connection intervals to skip, between [0, 1000]
         * @return Calling object
         */
        ConnectionParametersEditor slaveLatency(short latency);

        /**
         * Sets the maximum amount of time between data exchanges until the connection is considered to be lost
         * @param timeout    Timeout value between [10, 32000] ms
         * @return Calling object
         */
        ConnectionParametersEditor supervisorTimeout(short timeout);

        /**
         * Writes the new connection parameters to the board
         */
        void commit();
    }

    /**
     * Wrapper class encapsulating the battery state data
     * @author Eric Tsai
     */
    final class BatteryState {
        /** Percent charged, between [0, 100] */
        public final byte charge;
        /** Battery voltage level in mV */
        public final short voltage;

        public BatteryState(byte charge, short voltage) {
            this.charge = charge;
            this.voltage = voltage;
        }

        @Override
        public boolean equals(Object o) {
            // generated by intellij
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BatteryState that = (BatteryState) o;

            return charge == that.charge && voltage == that.voltage;

        }

        @Override
        public int hashCode() {
            // generated by intellij

            int result = (int) charge;
            result = 31 * result + (int) voltage;
            return result;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "{charge: %d%%, voltage: %dmV}", charge, voltage);
        }
    }
    /**
     * Produces battery data that can be used with the firmware features
     * @author Eric Tsai
     */
    interface BatteryDataProducer extends ForcedDataProducer {
        /**
         * Get the name for battery charge data
         * @return Battery charge data name
         */
        String chargeName();
        /**
         * Get the name for battery voltage data
         * @return Battery voltage data name
         */
        String voltageName();
    }
    /**
     * Gets an object to use the battery data
     * @return Object representing battery data, null if battery data is not supported
     */
    BatteryDataProducer battery();
    /**
     * Gets an object to control power status notifications
     * @return Object representing power status notifications, null if power status not supported
     */
    AsyncDataProducer powerStatus();
    /**
     * Gets an object to control charging status notifications
     * @return Object representing charging status notifications, null if charging status not supported
     */
    AsyncDataProducer chargeStatus();
    /**
     * Reads the current power status if available.  On unsupported boards and firmware, this operation will
     * fail with an {@link UnsupportedOperationException}
     * @return Task holding the power status; 1 if power source is attached, 0 otherwise
     */
    Task<Byte> readPowerStatusAsync();
    /**
     * Reads the current charge status.  On unsupported boards and firmware, this operation will
     * fail with an {@link UnsupportedOperationException}
     * @return Task holding the charge status; 1 if battery is charging, 0 otherwise
     */
    Task<Byte> readChargeStatusAsync();

    /**
     * Programs a task that will be execute on-board when a disconnect occurs
     * @param codeBlock    MetaWear commands composing the task
     * @return Task holding the result of the program request
     */
    Task<Observer> onDisconnect(CodeBlock codeBlock);
}
