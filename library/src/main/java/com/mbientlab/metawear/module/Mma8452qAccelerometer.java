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

/**
 * Controller for interacting with the MMA8452Q sensor.  This sensor is only available on MetaWear R boards.
 * @author Eric Tsai
 */
public interface Mma8452qAccelerometer extends Accelerometer {
    /**
     * Enumeration of the available power modes on the accelerometer
     * @author Eric Tsai
     */
    enum PowerMode {
        NORMAL,
        LOW_NOISE_LOW_POWER,
        HIGH_RES,
        LOW_POWER
    }

    /**
     * Detectable tap types
     * @author Eric Tsai
     */
    enum TapType {
        SINGLE,
        DOUBLE
    }

    /**
     * Axes available for motion detection.  These axis entries are relative to the
     * orientation of the accelerometer chip.
     * @author Eric Tsai
     */
    enum Axis {
        X,
        Y,
        Z
    }

    /**
     * Detectable movement types on the sensor
     * @author Eric Tsai
     */
    enum MovementType {
        FREE_FALL,
        MOTION
    }

    /**
     * Available data rates on the MWR accelerometer (MMA8452Q)
     * @author Eric Tsai
     */
    enum OutputDataRate {
        ODR_800_HZ {
            @Override
            public float frequency() { return 800f; }
        },
        ODR_400_HZ {
            @Override
            public float frequency() { return 400f; }
        },
        ODR_200_HZ {
            @Override
            public float frequency() { return 200f; }
        },
        ODR_100_HZ {
            @Override
            public float frequency() { return 100f; }
        },
        ODR_50_HZ {
            @Override
            public float frequency() { return 50f; }
        },
        ODR_12_5_HZ {
            @Override
            public float frequency() { return 12.5f; }
        },
        ODR_6_25_HZ {
            @Override
            public float frequency() { return 6.25f; }
        },
        ODR_1_56_HZ {
            @Override
            public float frequency() { return 1.56f; }
        };

        public abstract float frequency();

        public static float[] frequencies() {
            OutputDataRate[] values= values();
            float[] freqs= new float[values.length];
            for(byte i= 0; i < freqs.length; i++) {
                freqs[i]= values[i].frequency();
            }

            return freqs;
        }
    }

    /**
     * Orientation definitions for the accelerometer.  The entries are defined
     * from the perspective of the accelerometer chip's placement and orientation,
     * not from the MetaWear board's perspective.
     * @author Eric Tsai
     */
    enum Orientation {
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
    enum FullScaleRange {
        FSR_2G,
        FSR_4G,
        FSR_8G
    }

    /**
     * Enumeration of sleep mode data rates
     * @author Eric Tsai
     */
    enum SleepModeRate {
        SMR_50_HZ,
        SMR_12_5_HZ,
        SMR_6_25_HZ,
        SMR_1_56_HZ
    }

    /**
     * Wrapper class encapsulating movement information received from the board
     * @author Eric Tsai
     */
    interface MovementData {
        /**
         * Axis information for the detection callback functions
         * @author Eric Tsai
         */
        enum Polarity {
            /** Movement is in the positive polarity */
            POSITIVE,
            /** Movement is in the negative polarity */
            NEGATIVE,
        }

        /**
         * Retrieves the threshold status
         * @param axis    Axis to check
         * @return True if motion had crossed the threshold
         */
        boolean crossedThreshold(Axis axis);

        /**
         * Returns the polarity the board is moving in on the specific axis
         * @param axis Axis to check
         * @return Direction enum value indicating the movement polarity
         */
        Polarity polarity(Axis axis);
    }
    /**
     * Wrapper class encapsulating tap data received from the accelerometer.
     * @author Eric Tsai
     */
    interface TapData extends MovementData {
        TapType type();
    }

    /**
     * Interface for configuring settings for orientation detection
     */
    interface OrientationConfigEditor {
        /**
         * Sets the time for which the sensor's orientation must remain in the new position before a position
         * change is triggered.  This is used to filter out false positives from shaky hands or other small vibrations
         * @param delay    How long the sensor must remain in the new position, in milliseconds
         * @return Calling object
         */
        OrientationConfigEditor setDelay(int delay);

        /**
         * Write the changes to the board
         */
        void commit();
    }

    /**
     * Interface for configuring axis sampling
     * @author Eric Tsai
     */
    interface SamplingConfigEditor {
        /**
         * Sets the max range of the data
         * @param range Data's max range
         * @return Calling object
         */
        SamplingConfigEditor setFullScaleRange(FullScaleRange range);

        /**
         * Enables high-pass filtering on the accelerometer axis data and sets the high pass filter cutoff frequency.
         * @param cutoff Cutoff frequency setting between [0, 3] where 0 = highest cutoff freq and 3 = lowest cutoff freq
         * @return Calling object
         */
        SamplingConfigEditor enableHighPassFilter(byte cutoff);

        /**
         * Write the new settings to the board
         */
        void commit();
    }

    /**
     * Interface for configuring auto sleep mode
     * @author Eric Tsai
     */
    interface AutoSleepConfigEditor {
        /**
         * Sets the operating frequency in sleep mode
         * @param rate    New sleep mode data rate
         * @return Calling object
         */
        AutoSleepConfigEditor setDataRate(SleepModeRate rate);

        /**
         * Sets the timeout period
         * @param timeout    How long to idle in active mode before switching to sleep mode, in milliseconds
         * @return Calling object
         */
        AutoSleepConfigEditor setTimeout(int timeout);

        /**
         * Sets the power mode while in sleep mode
         * @param pwrMode    New power mode to use
         * @return Calling object
         */
        AutoSleepConfigEditor setPowerMode(PowerMode pwrMode);
        void commit();
    }
    /**
     * Interface for configuring shake detection
     * @author Eric Tsai
     */
    interface ShakeConfigEditor extends DetectionConfigEditor<ShakeConfigEditor> {
        /**
         * Sets the axis to detect shaking motion
         * @param axis    Axis to detect shaking
         * @return Calling object
         */
        ShakeConfigEditor setAxis(Axis axis);
    }
    /**
     * Interface for configuring motion / free fall detection
     * @author Eric Tsai
     */
    interface MovementConfigEditor extends DetectionConfigEditor<MovementConfigEditor> {
        /**
         * Sets the axes to be detected for movement
         * @param axes    Axes to detect movement
         * @return Calling object
         */
        MovementConfigEditor setAxes(Axis ... axes);
    }
    /**
     * Interface for configuring tap detection parameters
     * @author Eric Tsai
     */
    interface TapConfigEditor extends DetectionConfigEditor<TapConfigEditor> {
        /**
         * Set the latency value
         * @param latency Wait time, in ms, between the end of the 1st shock and
         * when the 2nd shock can be detected
         * @return Calling object
         */
        TapConfigEditor setLatency(int latency);
        /**
         * Set the window value
         * @param window Time, in ms, in which a second shock must begin after
         * the latency expires
         * @return Calling object
         */
        TapConfigEditor setWindow(int window);
        /**
         * Set how the tap detection should processes the data
         * @return Calling object
         */
        TapConfigEditor enableLowPassFilter();

        /**
         * Sets the axis to detect tapping on
         * @param axis    Axis to detect tapping
         * @return Calling object
         */
        TapConfigEditor setAxis(Axis axis);
    }

    /**
     * Base class for configuring motion detection om the chip
     * @author Eric Tsai
     */
    interface DetectionConfigEditor<T extends DetectionConfigEditor> {
        /**
         * Sets the threshold of the motion
         * @param threshold    Threshold, in G's
         * @return Calling object
         */
        T setThreshold(float threshold);

        /**
         * Sets the duration for which a condition must be met to trigger a data event
         * @param duration    Duration for the condition to be met
         * @return Calling object
         */
        T setDuration(int duration);

        /**
         * Write the new settings to the board
         */
        void commit();
    }

    /**
     * Selector for available data sources on the chip
     * @author Eric Tsai
     */
    interface SourceSelector extends Accelerometer.SourceSelector {
        /**
         * Handle data from tap detection
         * @return Object representing the tap data
         */
        DataSignal fromTap();

        /**
         * Handle data from orientation changes
         * @return Object representing the orientation data
         */
        DataSignal fromOrientation();

        /**
         * Handle the data from shake detection
         * @return Object representing the shake data
         */
        DataSignal fromShake();

        /**
         * Handle the data from motion / free fall detection
         * @return Object representing movement data
         */
        DataSignal fromMovement();
    }

    /**
     * Initiates the creation of a route for MMA8452Q sensor data
     * @return Selection of available data sources
     */
    @Override
    SourceSelector routeData();

    /**
     * Sets the sampling data rate
     * @param rate Data rate to sample at
     */
    void setOutputDataRate(OutputDataRate rate);

    /**
     * Configures auto sleep mode
     * @return Editor to configure sleep settings
     */
    AutoSleepConfigEditor configureAutoSleep();
    /**
     * Enable autosleep mode
     */
    void enableAutoSleepMode();
    /**
     * Disable autosleep mode
     */
    void disableAutoSleepMode();

    /**
     * Sets the power mode of the accelerometer
     * @param mode Power mode to use
     */
    void setPowerMode(PowerMode mode);

    /**
     * Configures axis sampling
     * @return Editor to configure sample settings
     */
    SamplingConfigEditor configureAxisSampling();

    /**
     * Configures settings for tap detection
     * @return Editor to configure tap settings
     */
    TapConfigEditor configureTapDetection();
    /**
     * Enables tap detection when accelerometer is active
     * @param types    Tap types to look for
     */
    void enableTapDetection(TapType ... types);
    /**
     * Disables tap detection
     */
    void disableTapDetection();

    /**
     * Configures shake detection settings
     * @return Editor to configure shake settings
     */
    ShakeConfigEditor configureShakeDetection();
    /**
     * Enables shake detection when the accelerometer is active
     */
    void enableShakeDetection();
    /**
     * Disables shake detection
     */
    void disableShakeDetection();

    /**
     * Configures motion detection settings.  This will overwrite free fall settings as they are mutually
     * exclusive on this chip
     * @return Editor to configure motion settings
     */
    MovementConfigEditor configureMotionDetection();
    /**
     * Configures free fall detection settings.  This will overwrite motion settings as they are mutually
     * exclusive on this chip
     * @return Editor to configure free fall settings
     */
    MovementConfigEditor configureFreeFallDetection();
    /**
     * Enables movement detection when the accelerometer is active
     * @param type    Movement type to detect, either free fall or general motion
     */
    void enableMovementDetection(MovementType type);
    /**
     * Disables movement detection
     */
    void disableMovementDetection();

    /**
     * Configures orientation settings
     * @return Editor to configure orientation settings
     */
    OrientationConfigEditor configureOrientationDetection();
    /**
     * Enables orientation detection when the accelerometer is active
     */
    void enableOrientationDetection();
    /**
     * Disables orientation detection
     */
    void disableOrientationDetection();
}
