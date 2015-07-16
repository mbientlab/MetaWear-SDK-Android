/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.module;

/**
 * Created by etsai on 6/19/2015.
 */
public interface Mma8452qAccelerometer extends Accelerometer {
    public void globalStart();
    public void globalStop();

    public SamplingConfig configureXYZSampling();
    public void startAxisSampling();
    public void stopAxisSampling();

    public interface SamplingConfig {
        /**
         * Max range of the accelerometer data
         * @author Eric Tsai
         */
        public enum FullScaleRange {
            FSR_2G,
            FSR_4G,
            FSR_8G
        }
        /**
         * Available data rates on the metawear accelerometer
         * @author Eric Tsai
         */
        public enum OutputDataRate {
            ODR_800_HZ,
            ODR_400_HZ,
            ODR_200_HZ,
            ODR_100_HZ,
            ODR_50_HZ,
            ODR_12_5_HZ,
            ODR_6_25_HZ,
            ODR_1_56_HZ
        }

        /**
         * Sets the max range of the data
         * @param range Data's max range
         * @return Calling object
         */
        public SamplingConfig withFullScaleRange(FullScaleRange range);
        /**
         * Sets the sampling data rate
         * @param rate Data rate to sample at
         * @return Calling object
         */
        public SamplingConfig withOutputDataRate(OutputDataRate rate);
        /**
         * Enables high-pass filtering on the accelerometer axis data.  This version of the
         * function does not alter the cutoff setting
         * @return Calling object
         */
        public SamplingConfig withHighPassFilter();
        /**
         * Enables high-pass filtering on the accelerometer axis data
         * @param cutoff Cutoff frequency setting between [0, 3] where
         * 0 = highest cutoff freq and 3 = lowest cutoff freq
         * @return Calling object
         */
        public SamplingConfig withHighPassFilter(byte cutoff);
        /**
         * Disables high-pass filtering on the accelerometer axis data
         * @return Calling object
         */
        public SamplingConfig withoutHighPassFilter();
    }
}
