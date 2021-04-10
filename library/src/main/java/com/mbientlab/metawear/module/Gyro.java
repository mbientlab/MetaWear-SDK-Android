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
import com.mbientlab.metawear.data.AngularVelocity;

import java.util.HashMap;

import bolts.Task;

/**
 * Sensor on the BMI160 and BMI270 IMU measuring angular velocity
 * @author Laura Kassovic
 */
public interface Gyro extends Module, Configurable<Gyro.ConfigEditor> {
    enum FilterMode {
        OSR4,
        OSR2,
        NORMAL
    }
    /**
     * Operating frequency of the gyro
     * @author Laura Kassovic
     */
    enum OutputDataRate {
        /** 25Hz */
        ODR_25_HZ,
        /** 50Hz */
        ODR_50_HZ,
        /** 100Hz */
        ODR_100_HZ,
        /** 200Hz */
        ODR_200_HZ,
        /** 400Hz */
        ODR_400_HZ,
        /** 800Hz */
        ODR_800_HZ,
        /** 1600Hz */
        ODR_1600_HZ,
        /** 3200Hz */
        ODR_3200_HZ;

        public final byte bitmask;

        OutputDataRate() {
            this.bitmask= (byte) (ordinal() + 6);
        }
    }
    /**
     * Supported angular rate measurement range
     * @author Laura Kassovic
     */
    enum Range {
        /** +/- 2000 degrees / second */
        FSR_2000(16.4f),
        /** +/- 1000 degrees / second */
        FSR_1000(32.8f),
        /** +/- 500 degrees / second */
        FSR_500(65.6f),
        /** +/- 250 degrees / second */
        FSR_250(131.2f),
        /** +/- 125 degrees / second */
        FSR_125(262.4f);

        public final float scale;
        public final byte bitmask;

        Range(float scale) {
            this.scale= scale;
            this.bitmask= (byte) ordinal();
        }

        private static final HashMap<Byte, Range> bitMaskToRanges;
        static {
            bitMaskToRanges= new HashMap<>();
            for(Range it: Range.values()) {
                bitMaskToRanges.put(it.bitmask, it);
            }
        }
        public static Range bitMaskToRange(byte mask) {
            return bitMaskToRanges.get(mask);
        }
    }
    /**
     * Interface to configure parameters for measuring angular velocity
     * @author Laura Kassovic
     */
    interface ConfigEditor extends ConfigEditorBase {
        /**
         * Set the measurement range
         * @param range    New range to use
         * @return Calling object
         */
        ConfigEditor range(Range range);
        /**
         * Set the output date rate
         * @param odr    New output data rate to use
         * @return Calling object
         */
        ConfigEditor odr(OutputDataRate odr);
        /**
         * Set the filter mode
         * @param mode New filter mode
         * @return Calling object
         */
        ConfigEditor filter(FilterMode mode);
    }
    /**
     * Pulls the current gyro output data rate and data range from the sensor
     * @return Task that is completed when the settings are received
     */
    Task<Void> pullConfigAsync();

    /**
     * Reports measured angular velocity values from the gyro.  Combined XYZ data is represented as an
     * {@link AngularVelocity} object while split data is interpreted as a float.
     * @author Laura Kassovic
     */
    interface AngularVelocityDataProducer extends AsyncDataProducer {
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
     * Get an implementation of the AngularVelocityDataProducer interface
     * @return AngularVelocityDataProducer object
     */
    AngularVelocityDataProducer angularVelocity();
    /**
     * Variant of angular velocity data that packs multiple data samples into 1 BLE packet to increase the
     * data throughput.  Only streaming is supported for this data producer.
     * @return Object representing packed acceleration data
     */
    AsyncDataProducer packedAngularVelocity();

    /**
     * Starts the gyo
     */
    void start();
    /**
     * Stops the gyo
     */
    void stop();
}
