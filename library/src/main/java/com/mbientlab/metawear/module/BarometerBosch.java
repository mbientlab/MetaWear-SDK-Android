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
import com.mbientlab.metawear.MetaWearBoard.Module;

/**
 * Generic interface providing high level access for a barometer. If you know specifically which
 * barometer is on your board, use the appropriate Barometer subclass instead.
 * @author Eric Tsai
 * @see BarometerBme280
 * @see BarometerBmp280
 */
public interface BarometerBosch extends Module {
    /**
     * Supported oversampling modes on a Bosch barometer
     * @author Eric Tsai
     */
    enum OversamplingMode {
        SKIP,
        ULTRA_LOW_POWER,
        LOW_POWER,
        STANDARD,
        HIGH,
        ULTRA_HIGH
    }

    /**
     * Available IIR (infinite impulse response) filter coefficient for the Bosch pressure sensors
     * @author Eric Tsai
     */
    enum FilterCoeff {
        OFF,
        AVG_2,
        AVG_4,
        AVG_8,
        AVG_16
    }

    /**
     * Barometer agnostic interface for configuring the sensor
     * @param <T>    Type of barometer config editor
     */
    interface ConfigEditorBase<T extends ConfigEditorBase> {
        /**
         * Sets the oversampling mode for pressure sampling
         * @param mode    New oversampling mode
         * @return Calling object
         */
        T pressureOversampling(OversamplingMode mode);

        /**
         * Sets the IIR coefficient for pressure sampling
         * @param coeff    New filter coefficient
         * @return Calling object
         */
        T filterCoeff(FilterCoeff coeff);

        /**
         * Sets the standby time.  The closest, valid standby time will be chosen
         * depending on the underlying sensor
         * @param time    New standby time
         * @return Calling object
         */
        T standbyTime(float time);
        /**
         * Writes the new settings to the barometer
         */
        void commit();
    }
    /**
     * Configure the barometer
     * @return Generic editor object
     */
    ConfigEditorBase<? extends ConfigEditorBase> configure();

    /**
     * Gets an object to control the pressure data
     * @return Object controlling pressure data
     */
    AsyncDataProducer pressure();
    /**
     * Gets an object to control the altitude data
     * @return Object controlling altitude data
     */
    AsyncDataProducer altitude();

    /**
     * Start data sampling
     */
    void start();
    /**
     * Stop data sampling
     */
    void stop();
}
