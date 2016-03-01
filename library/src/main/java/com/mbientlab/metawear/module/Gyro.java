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
 * Generic class providing high level access to a gyro.  If you know specifically which gyro is on your
 * board, use the appropriate gyro subclass instead
 * @author Eric Tsai
 * @see Bmi160Gyro
 */
public interface Gyro extends MetaWearBoard.Module {
    /**
    * Sets the operating frequency of the gyro.  The closest, valid frequency will be chosen
    * depending on underlying sensor
    * @param frequency    Operating frequency, in Hz
    * @return Selected operating frequency
    */
    float setOutputDataRate(float frequency);
    /**
     * Sets the max range for sampling the angular rate of the gyro.  The closest, valid range will be
     * chosen depending on the underlying sensor
     * @param range    Max sampling range, in degrees per second
     * @return Selected angular rate range
     */
    float setAngularRateRange(float range);

    /**
     * Switches the gyro into an active state
     */
    void start();
    /**
     * Switches the gyro back into standby moee
     */
    void stop();

    /**
     * Generic selector for gyro data sources
     * @author Eric Tsai
     */
    interface SourceSelector {
        /**
         * Handle data from all 3 gyro axes
         * @return Object representing the data from the gyro XYZ axis sampling
         */
        DataSignal fromAxes();

        /**
         * Handle data from the x-axis.  Streaming data from only the x-axis is not supported
         * @return Object representing the data from the gyro x-axis sampling
         */
        DataSignal fromXAxis();

        /**
         * Handle data from the y-axis.  Streaming data from only the y-axis is not supported
         * @return Object representing the data from the gyro y-axis sampling
         */
        DataSignal fromYAxis();

        /**
         * Handle data from the z-axis.  Streaming data from only the z-axis is not supported
         * @return Object representing the data from the gyro z-axis sampling
         */
        DataSignal fromZAxis();
    }

    /**
     * Initiates the creation of a route for gyro data
     * @return Selection of available data sources
     */
    SourceSelector routeData();
}
