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

import com.mbientlab.metawear.ConfigEditorBase;
import com.mbientlab.metawear.Configurable;
import com.mbientlab.metawear.DataToken;
import com.mbientlab.metawear.MetaWearBoard.Module;

import java.util.Locale;
import java.util.UUID;

import bolts.Task;

/**
 * Apple developed protocol for Bluetooth LE proximity sensing
 * @author Eric Tsai
 */
public interface IBeacon extends Module, Configurable<IBeacon.ConfigEditor> {
    /**
     * Enable IBeacon advertising.  You will need to disconnect from the board to advertise as an IBeacon
     */
    void enable();
    /**
     * Disable IBeacon advertising
     */
    void disable();

    /**
     * Interface for configuring IBeacon settings
     * @author Eric Tsai
     */
    interface ConfigEditor extends ConfigEditorBase {
        /**
         * Set the advertising UUID
         * @param adUuid    New advertising UUID
         * @return Calling object
         */
        ConfigEditor uuid(UUID adUuid);
        /**
         * Set the advertising major number
         * @param major    New advertising major number
         * @return Calling object
         */
        ConfigEditor major(short major);
        /**
         * Set the advertising major number
         * @param major    New advertising major number
         * @return Calling object
         */
        ConfigEditor major(DataToken major);
        /**
         * Set the advertising minor number
         * @param minor    New advertising minor number
         * @return Calling object
         */
        ConfigEditor minor(short minor);
        /**
         * Set the advertising minor number
         * @param minor    New advertising minor number
         * @return Calling object
         */
        ConfigEditor minor(DataToken minor);
        /**
         * Set the advertising receiving power
         * @param power    New advertising receiving power
         * @return Calling object
         */
        ConfigEditor rxPower(byte power);
        /**
         * Set the advertising transmitting power
         * @param power    New advertising transmitting power
         * @return Calling object
         */
        ConfigEditor txPower(byte power);
        /**
         * Set the advertising period
         * @param period    New advertising period, in milliseconds
         * @return Calling object
         */
        ConfigEditor period(short period);
    }
    /**
     * Wrapper class encapsulating the IBeacon configuration
     * @author Eric Tsai
     */
    class Configuration {
        /** Advertising UUID */
        public UUID uuid;
        /** Advertising major value */
        public short major;
        /** Advertising minor value */
        public short minor;
        /** Advertising period */
        public short period;
        /** Advertising receiving power */
        public byte rxPower;
        /** Advertising transmitting power */
        public byte txPower;

        public Configuration() {

        }

        public Configuration(UUID uuid, short major, short minor, short period, byte rxPower, byte txPower) {
            this.uuid = uuid;
            this.major = major;
            this.minor = minor;
            this.period = period;
            this.rxPower = rxPower;
            this.txPower = txPower;
        }

        @Override
        public boolean equals(Object o) {
            // Generated by IntelliJ
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Configuration that = (Configuration) o;

            return major == that.major && minor == that.minor && period == that.period && rxPower == that.rxPower && txPower == that.txPower && uuid.equals(that.uuid);
        }

        @Override
        public int hashCode() {
            // Generated by IntelliJ
            int result = uuid.hashCode();
            result = 31 * result + (int) major;
            result = 31 * result + (int) minor;
            result = 31 * result + (int) period;
            result = 31 * result + (int) rxPower;
            result = 31 * result + (int) txPower;
            return result;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "{uuid: %s, major: %d, minor: %d, rx: %d, tx: %d, period: %d}",
                    uuid, major, minor, rxPower, txPower, period);
        }
    }

    /**
     * Read the current IBeacon configuration
     * @return Configuration object that will be available when the read operation completes
     */
    Task<Configuration> readConfigAsync();
}
