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
    /**
     * Enumeration of the available power modes on the accelerometer
     * @author Eric Tsai
     */
    public enum PowerMode {
        NORMAL,
        LOW_NOISE_LOW_POWER,
        HIGH_RES,
        LOW_POWER
    }

    public enum TapType {
        SINGLE,
        DOUBLE
    }

    /**
     * Axes available for motion detection.  These axis entries are relative to the
     * orientation of the accelerometer chip.
     * @author etsai
     */
    public enum Axis {
        X,
        Y,
        Z
    }

    public enum MovementType {
        FREE_FALL,
        MOTION
    }

    /**
     * Available data rates on the MWR accelerometer (MMA8452Q)
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
     * Orientation definitions for the accelerometer.  The entries are defined
     * from the perspective of the accelerometer chip's placement and orientation,
     * not from the MetaWear board's perspective.
     * @author Eric Tsai
     */
    public enum Orientation {
        FRONT_PORTRAIT_UP,
        FRONT_PORTRAIT_DOWN,
        FRONT_LANDSCAPE_RIGHT {
            @Override public boolean isPortrait() { return false; }
        },
        FRONT_LANDSCAPE_LEFT {
            @Override public boolean isPortrait() { return false; }
        },
        BACK_PORTRAIT_UP {
            @Override public boolean isFront() { return false; }
        },
        BACK_PORTRAIT_DOWN {
            @Override public boolean isFront() { return false; }
        },
        BACK_LANDSCAPE_RIGHT {
            @Override public boolean isFront() { return false; }
            @Override public boolean isPortrait() { return false; }
        },
        BACK_LANDSCAPE_LEFT {
            @Override public boolean isFront() { return false; }
            @Override public boolean isPortrait() { return false; }
        };

        public boolean isFront() { return true; }
        public boolean isPortrait() { return true; }

    }

    /**
     * Max range of the accelerometer data on the MWR accelerometer (MMA8452Q)
     * @author Eric Tsai
     */
    public enum FullScaleRange {
        FSR_2G,
        FSR_4G,
        FSR_8G
    }

    /**
     * Enumeration of sleep mode data rates
     * @author Eric Tsai
     */
    public enum SleepModeRate {
        SMR_50_HZ,
        SMR_12_5_HZ,
        SMR_6_25_HZ,
        SMR_1_56_HZ
    }

    /**
     * Wrapper class encapsulating movement information received from the board
     * @author Eric Tsai
     */
    public interface MovementData {
        /**
         * Axis information for the detection callback functions
         * @author Eric Tsai
         */
        public enum Polarity {
            /** Movement is in the positive polarity */
            POSITIVE,
            /** Movement is in the negative polarity */
            NEGATIVE,
        }

        public boolean crossedThreshold(Axis axis);
        /**
         * Returns the polarity the board is moving in on the specific axis
         * @param axis Axis to check
         * @return Direction enum value indicating the movement polarity
         */
        public Polarity polarity(Axis axis);
    }

    public interface TapData extends MovementData {
        public TapType type();
    }

    public interface OrientationConfigEditor {
        public OrientationConfigEditor setDelay(int delay);
        public void commit();
    }
    public interface SamplingConfigEditor {
        /**
         * Sets the max range of the data
         * @param range Data's max range
         * @return Calling object
         */
        public SamplingConfigEditor setFullScaleRange(FullScaleRange range);

        /**
         * Enables high-pass filtering on the accelerometer axis data and sets the high pass filter cutoff frequency.
         * @param cutoff Cutoff frequency setting between [0, 3] where 0 = highest cutoff freq and 3 = lowest cutoff freq
         * @return Calling object
         */
        public SamplingConfigEditor enableHighPassFilter(byte cutoff);

        public void commit();
    }
    public interface AutoSleepConfigEditor {
        public AutoSleepConfigEditor setDataRate(SleepModeRate rate);
        public AutoSleepConfigEditor setTimeout(int timeout);
        public AutoSleepConfigEditor setPowerMode(PowerMode pwrMode);
        public void commit();
    }
    public interface ShakeConfigEditor extends DetectionConfigEditor<ShakeConfigEditor> {
        public ShakeConfigEditor setAxis(Axis axis);
    }
    public interface MovementConfigEditor extends DetectionConfigEditor<MovementConfigEditor> {
        public MovementConfigEditor setAxes(Axis ... axes);
    }
    public interface TapConfigEditor extends DetectionConfigEditor<TapConfigEditor> {
        /**
         * Set the latency value
         * @param latency Wait time, in ms, between the end of the 1st shock and
         * when the 2nd shock can be detected
         * @return Calling object
         */
        public TapConfigEditor setLatency(int latency);
        /**
         * Set the window value
         * @param window Time, in ms, in which a second shock must begin after
         * the latency expires
         * @return Calling object
         */
        public TapConfigEditor setWindow(int window);
        /**
         * Set how the tap detection should processes the data
         * @return Calling object
         */
        public TapConfigEditor enableLowPassFilter();
        public TapConfigEditor setAxis(Axis axis);
    }
    public interface DetectionConfigEditor<T extends DetectionConfigEditor> {
        public T setThreshold(float threshold);
        public T setDuration(int duration);
        public void commit();
    }

    public void globalStart();
    public void globalStop();

    /**
     * Sets the sampling data rate
     * @param rate Data rate to sample at
     * @return Calling object
     */
    public void setOutputDataRate(OutputDataRate rate);

    public AutoSleepConfigEditor configureAutoSleep();
    public void enableAutoSleepMode();
    public void disableAutoSleepMode();

    /**
     * Sets the power mode of the accelerometer
     * @param mode Power mode to use
     */
    public void setPowerMode(PowerMode mode);

    public SamplingConfigEditor configureAxisSampling();
    public void startAxisSampling();
    public void stopAxisSampling();

    public TapConfigEditor configureTapDetection(TapType ... type);
    public void startTapDetection();
    public void stopTapDetection();

    public ShakeConfigEditor configureShakeDetection();
    public void startShakeDetection();
    public void stopShakeDetection();

    public MovementConfigEditor configureMovementDetection(MovementType type);
    public void startMovementDetection();
    public void stopMovementDetection();

    public OrientationConfigEditor configureOrientationDetection();
    public void startOrientationDetection();
    public void stopOrientationDetection();
}