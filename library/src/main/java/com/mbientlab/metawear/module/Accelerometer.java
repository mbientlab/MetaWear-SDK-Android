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
import com.mbientlab.metawear.ConfigEditorBase;
import com.mbientlab.metawear.Configurable;
import com.mbientlab.metawear.MetaWearBoard.Module;
import com.mbientlab.metawear.data.Acceleration;

import bolts.Task;

/**
 * Measures sources of acceleration, such as gravity or motion.  This interface is provides general
 * access to an accelerometer. If you know specifically which accelerometer is on your board, use the
 * appropriate subclass instead.
 * @author Laura Kassovic
 * @see AccelerometerBma255
 * @see AccelerometerBmi160
 * @see AccelerometerBmi270
 * @see AccelerometerMma8452q
 */
public interface Accelerometer extends Module, Configurable<Accelerometer.ConfigEditor<? extends Accelerometer.ConfigEditor>> {
    /**
     * Reports measured acceleration values from the accelerometer.  Combined xyz acceleration data is represented
     * by the {@link Acceleration} class and split data is interpreted as a float.
     * @author Eric Tsai
     */
    interface AccelerationDataProducer extends AsyncDataProducer {
        /**
         * Get the name for x-axis data
         * @return X-axis data name
         */
        String xAxisName();
        /**
         * Get the name for y-axis data
         * @return Y-axis data name
         */
        String yAxisName();
        /**
         * Get the name for z-axis data
         * @return Z-axis data name
         */
        String zAxisName();
    }
    /**
     * Get an implementation of the {@link AccelerationDataProducer} interface
     * @return AccelerationDataProducer object
     */
    AccelerationDataProducer acceleration();
    /**
     * Variant of acceleration data that packs multiple data samples into 1 BLE packet to increase the
     * data throughput.  Only streaming is supported for this data producer.
     * @return AsyncDataProducer object for packed acceleration data
     */
    AsyncDataProducer packedAcceleration();

    /**
     * Switch the accelerometer into active mode
     */
    void start();
    /**
     * Switch the accelerometer into standby mode
     */
    void stop();

    /**
     * Accelerometer agnostic interface for configuring the sensor
     * @param <T>    Type of accelerometer config editor
     */
    interface ConfigEditor<T extends ConfigEditor> extends ConfigEditorBase {
        /**
         * Generic function for setting the output data rate.  The closest, valid frequency will be chosen
         * depending on the underlying sensor
         * @param odr    New output data rate, in Hz
         * @return Calling object
         */
        T odr(float odr);
        /**
         * Generic function for setting the data range.  The closest, valid range will be chosen
         * depending on the underlying sensor
         * @param fsr    New data range, in g's
         * @return Calling object
         */
        T range(float fsr);
    }
    /**
     * Get the output data rate.  The returned value is only meaningful if the API has configured the sensor
     * @return Selected output data rate
     */
    float getOdr();
    /**
     * Get the data range.  The returned value is only meaningful if the API has configured the sensor
     * @return Selected data range
     */
    float getRange();
    /**
     * Pulls the current accelerometer output data rate and data range from the sensor
     * @return Task that is completed when the settings are received
     */
    Task<Void> pullConfigAsync();
}
