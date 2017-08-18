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
import com.mbientlab.metawear.data.MagneticField;

/**
 * Bosch sensor measuring magnetic field strength
 * @author Eric Tsai
 */
public interface MagnetometerBmm150 extends Module, Configurable<MagnetometerBmm150.ConfigEditor> {
    /**
     * Recommended configurations for the magnetometer as outlined in the specs sheet.
     * <table summary="Recommended sensor configurations">
     *     <thead>
     *         <tr>
     *             <th>Setting</th>
     *             <th>ODR</th>
     *             <th>Average Current</th>
     *             <th>Noise</th>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>LOW_POWER</td>
     *             <td>10Hz</td>
     *             <td>170&#956;A</td>
     *             <td>1.0&#956;T (xy axis), 1.4&#956;T (z axis)</td>
     *         </tr>
     *         <tr>
     *             <td>REGULAR</td>
     *             <td>10Hz</td>
     *             <td>0.5mA</td>
     *             <td>0.6&#956;T</td>
     *         </tr>
     *         <tr>
     *             <td>ENHANCED_REGULAR</td>
     *             <td>10Hz</td>
     *             <td>0.8mA</td>
     *             <td>0.5&#956;T</td>
     *         </tr>
     *         <tr>
     *             <td>HIGH_ACCURACY</td>
     *             <td>20Hz</td>
     *             <td>4.9mA</td>
     *             <td>0.3&#956;T</td>
     *         </tr>
     *     </tbody>
     * </table>
     * @author Eric Tsai
     */
    enum Preset {
        LOW_POWER,
        REGULAR,
        ENHANCED_REGULAR,
        HIGH_ACCURACY
    }
    /**
     * Sets the power mode to one of the preset configurations
     * @param preset    Preset to use
     */
    void usePreset(Preset preset);

    /**
     * Supported output data rates for the BMM150 sensor
     * @author Eric Tsai
     */
    enum OutputDataRate {
        /** 10Hz */
        ODR_10_HZ,
        /** 2Hz */
        ODR_2_HZ,
        /** 6Hz */
        ODR_6_HZ,
        /** 8Hz */
        ODR_8_HZ,
        /** 15Hz */
        ODR_15_HZ,
        /** 20Hz */
        ODR_20_HZ,
        /** 25Hz */
        ODR_25_HZ,
        /** 30Hz */
        ODR_30_HZ
    }
    /**
     * Sensor configuration editor, only for advanced users.  It is recommended that one of the {@link Preset}
     * configurations be used.
     * @author Eric Tsai
     */
    interface ConfigEditor extends ConfigEditorBase {
        /**
         * Sets the number of repetitions on the XY axis
         * @param reps    nXY repetitions, between [1, 511]
         * @return Calling object
         */
        ConfigEditor xyReps(short reps);
        /**
         * sets the number of repetitions on the Z axis
         * @param reps    nZ repetitions, between [1, 256]
         * @return Calling object
         */
        ConfigEditor zReps(short reps);

        /**
         * Sets the output data rate
         * @param odr    New output data rate
         * @return Calling object
         */
        ConfigEditor outputDataRate(OutputDataRate odr);
    }
    /**
     * Reports measured magnetic field strength, in units of Telsa (T) from the magnetometer.  Combined XYZ data
     * is represented as a {@link MagneticField} object while split data is interpreted as a float.
     */
    interface MagneticFieldDataProducer extends AsyncDataProducer {
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
     * Get an implementation of the MagneticFieldDataProducer interface
     * @return Object controlling B field data
     */
    MagneticFieldDataProducer magneticField();
    /**
     * Variant of B field data that packs multiple data samples into 1 BLE packet to increase the
     * data throughput.  Only streaming is supported for this data producer.
     * @return Object representing packed acceleration data
     */
    AsyncDataProducer packedMagneticField();

    /**
     * Switch the magnetometer into normal mode
     */
    void start();
    /**
     * Switch the magnetometer into sleep mode
     */
    void stop();
    /**
     * Switch the magnetometer into suspend mode.  If placed in suspend mode, sensor settings are reset and will need to be reconfigured.
     */
    void suspend();
}
