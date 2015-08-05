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

/**
 * Controller for interacting with the BMP280 pressure sensor.  This sensor is only available on MetaWear
 * R+Gyro boards.
 * @author Eric Tsai
 */
public interface Bmp280Barometer extends Barometer {
    /**
     * Supported oversampling modes on the BMP280 sensor
     * @author Eric Tsai
     */
    enum OversamplingMode {
        ULTRA_LOW_POWER,
        LOW_POWER,
        STANDARD,
        HIGH,
        ULTRA_HIGH
    }

    /**
     * Supported filter modes on the BMP280 sensor
     * @author Eric Tsai
     */
    enum FilterMode {
        OFF,
        AVG_2,
        AVG_4,
        AVG_8,
        AVG_16
    }

    /**
     * Supported stand by times on the BMP280 sensor
     * @author Eric Tsai
     */
    enum StandbyTime {
        TIME_0_5,
        TIME_62_5,
        TIME_125,
        TIME_250,
        TIME_500,
        TIME_1000,
        TIME_2000,
        TIME_4000
    }

    /**
     * Interface for configuring pressure sampling
     * @author Eric Tsai
     */
    interface ConfigEditor {
        /**
         * Sets the oversampling mode for pressure sampling
         * @param mode    New oversampling mode
         * @return Calling object
         */
        ConfigEditor setPressureOversampling(OversamplingMode mode);

        /**
         * Sets the filter mode for pressure sampling
         * @param mode    New filter mod
         * @return Calling object
         */
        ConfigEditor setFilterMode(FilterMode mode);

        /**
         * Sets the standby time
         * @param time    New standby time
         * @return Calling object
         */
        ConfigEditor setStandbyTime(StandbyTime time);

        /**
         * Writes the new settings to the board
         */
        void commit();
    }

    /**
     * Configures the settings for operating the pressure sensor
     * @return Editor to configure various settings
     */
    ConfigEditor configure();

    /**
     * Enable altitude sampling
     */
    void enableAltitudeSampling();

    /**
     * Disable altitude sampling
     */
    void disableAltitudeSampling();
}

