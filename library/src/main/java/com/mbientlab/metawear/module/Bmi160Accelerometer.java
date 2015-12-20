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

import java.util.HashMap;

/**
 * Controller for interacting with the accelerometer feature of the BMI160 sensor.  This sensor is only
 * available on MetaWear R+Gyro boards.
 * @author Eric Tsai
 */
public interface Bmi160Accelerometer extends Accelerometer {
    /**
     * Supported g-ranges for the accelerometer
     * @author Eric Tsai
     */
    enum AccRange {
        AR_2G {
            @Override
            public byte bitMask() { return 0x3; }

            @Override
            public float scale() { return 16384.f; }
        },
        AR_4G {
            @Override
            public byte bitMask() { return 0x5; }

            @Override
            public float scale() { return 8192.f; }
        },
        AR_8G {
            @Override
            public byte bitMask() { return 0x8; }

            @Override
            public float scale() { return 4096.f; }
        },
        AR_16G {
            @Override
            public byte bitMask() { return 0xc; }

            @Override
            public float scale() { return 2048.f; }
        };

        public abstract byte bitMask();
        public abstract float scale();

        private static final HashMap<Byte, AccRange> bitMasksToRange;
        static {
            bitMasksToRange= new HashMap<>();
            for(AccRange it: values()) {
                bitMasksToRange.put(it.bitMask(), it);
            }
        }

        public static AccRange bitMaskToRange(byte bitMask) {
            return bitMasksToRange.get(bitMask);
        }
    }

    /**
     * Operating frequency of the accelerometer
     * @author Eric Tsai
     */
    enum OutputDataRate {
        ODR_0_78125_HZ {
            @Override
            public float frequency() { return 0.78125f; }
        },
        ODR_1_5625_HZ {
            @Override
            public float frequency() { return 1.5625f; }
        },
        ODR_3_125_HZ {
            @Override
            public float frequency() { return 3.125f; }
        },
        ODR_6_25_HZ {
            @Override
            public float frequency() { return 6.25f; }
        },
        ODR_12_5_HZ {
            @Override
            public float frequency() { return 12.5f; }
        },
        ODR_25_HZ {
            @Override
            public float frequency() { return 25f; }
        },
        ODR_50_HZ {
            @Override
            public float frequency() { return 50f; }
        },
        ODR_100_HZ {
            @Override
            public float frequency() { return 100f; }
        },
        ODR_200_HZ {
            @Override
            public float frequency() { return 200f; }
        },
        ODR_400_HZ {
            @Override
            public float frequency() { return 400f; }
        },
        ODR_800_HZ {
            @Override
            public float frequency() { return 800f; }
        },
        ODR_1600_HZ {
            @Override
            public float frequency() { return 1600f; }
        };

        public byte bitMask() { return (byte) (ordinal() + 1); }
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
     * Orientation definitions for the BMI160 accelerometer as defined from the placement and orientation of
     * the BMI160 sensor.  For board orientation, use the {@link Accelerometer.BoardOrientation} enum.
     * @author Eric Tsai
     */
    enum SensorOrientation {
        FACE_UP_PORTRAIT_UPRIGHT,
        FACE_UP_PORTRAIT_UPSIDE_DOWN,
        FACE_UP_LANDSCAPE_LEFT,
        FACE_UP_LANDSCAPE_RIGHT,
        FACE_DOWN_PORTRAIT_UPRIGHT,
        FACE_DOWN_PORTRAIT_UPSIDE_DOWN,
        FACE_DOWN_LANDSCAPE_LEFT,
        FACE_DOWN_LANDSCAPE_RIGHT
    }

    /**
     * Calculation modes that control the conditions that determine the board's orientation
     * @author Eric Tsai
     */
    enum OrientationMode {
        /** Default mode */
        SYMMETRICAL,
        HIGH_ASYMMETRICAL,
        LOW_ASYMMETRICAL
    }

    /**
     * Direction of motion for high-g interrupts
     * @author Eric Tsai
     */
    enum Sign {
        POSITIVE,
        NEGATIVE
    }

    /**
     * Sensitivity setting for the step counter
     * @author Eric Tsai
     */
    enum StepSensitivity {
        /** Default mode with a balance between false positives and false negatives */
        NORMAL,
        /** Mode for light weighted persons that gives few false negatives but eventually more false positives */
        SENSITIVE,
        /** Gives few false positives but eventually more false negatives */
        ROBUST
    }

    /**
     * Enumeration of hold times for flat detection
     * @author Eric Tsai
     */
    enum FlatHoldTime {
        /** 0 milliseconds */
        FHT_0_MS,
        /** 640 milliseconds */
        FHT_640_MS,
        /** 1280 milliseconds */
        FHT_1280_MS,
        /** 2560 milliseconds */
        FHT_2560_MS
    }

    /**
     * Interrupt modes for low-g detection
     * @author Eric Tsai
     */
    enum LowGMode {
        /** Compare |acc_x|, |acc_y|, |acc_z| with the low threshold */
        SINGLE,
        /** Compare |acc_x| + |acc_y| + |acc_z| with the low threshold */
        SUM
    }

    /**
     * Axes available for motion detection on the BMI160 chip.  These axis entries are relative to the
     * orientation of the accelerometer chip.
     * @author Eric Tsai
     */
    enum Axis {
        X,
        Y,
        Z
    }

    /**
     * Interface for configuring orientation detection
     * @author Eric Tsai
     */
    interface OrientationConfigEditor {
        /**
         * Sets the hysteresis offset for portrait/landscape detection
         * @param hysteresis    New offset angle, in degrees
         * @return Calling object
         */
        OrientationConfigEditor setHysteresis(float hysteresis);
        /**
         * Sets the orientation calculation mode
         * @param mode    New calculation mode
         * @return Calling object
         */
        OrientationConfigEditor setMode(OrientationMode mode);

        /**
         * writes the new settings to the board
         */
        void commit();
    }

    /**
     * Interface for configuring acceleration sampling
     * @author Eric Tsai
     */
    interface SamplingConfigEditor {
        /**
         * Sets the accelerometer data range
         * @param range    New g-range to use
         * @return Calling object
         */
        SamplingConfigEditor setFullScaleRange(AccRange range);
        /**
         * Sets the accelerometer output data rate
         * @param odr    New output data rate to use
         * @return Calling object
         */
        SamplingConfigEditor setOutputDataRate(OutputDataRate odr);
        /**
         * Writes the new settings to the board
         */
        void commit();
    }

    /**
     * Interface for configuring flat detection
     * @author Eric Tsai
     */
    interface FlatDetectionConfigEditor {
        /**
         * Sets the delay for which the flat value must remain stable for a flat interrupt
         * @param time    Delay time for a stable value
         * @return Calling object
         */
        FlatDetectionConfigEditor setHoldTime(FlatHoldTime time);
        /**
         * Sets the threshold defining a flat position
         * @param angle    Threshold angle, between [0, 44.8] degrees
         * @return Calling object
         */
        FlatDetectionConfigEditor setFlatTheta(float angle);
        /**
         * Writes the new settings to the board
         */
        void commit();
    }

    /**
     * Interface for configuring the step detector
     * @author Eric Tsai
     */
    interface StepDetectionConfigEditor {
        /**
         * Sets the sensitivity setting of the step detector.  The setting balances sensitivity and robustness.
         * @param sensitivity    Detector sensitivity
         * @return Calling object
         */
        StepDetectionConfigEditor setSensitivity(StepSensitivity sensitivity);
        /**
         * Enables the step counter.  Users must enable the step counter to use the readStepCounter function.
         * @return Calling object
         */
        StepDetectionConfigEditor enableStepCounter();
        /**
         * Writes the new settings to the board
         */
        void commit();
    }

    /**
     * Interface for configuring low/high G detection
     * @author Eric Tsai
     */
    interface LowHighDetectionConfigEditor {
        /**
         * Sets the minimum amount of time the acceleration must stay below (ths + hys) for an interrupt
         * @param duration    Duration between [2.5, 640] milliseconds
         * @return Calling object
         */
        LowHighDetectionConfigEditor setLowDuration(int duration);
        /**
         * Sets the threshold that triggers a low-g interrupt
         * @param threshold    Low-g interrupt threshold, between [0.00391, 2.0] g
         * @return Calling object
         */
        LowHighDetectionConfigEditor setLowThreshold(float threshold);
        /**
         * Sets the hysteresis level for low-g interrupt
         * @param hysteresis    Low-g interrupt hysteresis, between [0, 0.375]g
         * @return Calling object
         */
        LowHighDetectionConfigEditor setLowHysteresis(float hysteresis);
        /**
         * Sets mode for low-g detection
         * @param mode    Low-g detection mode
         * @return Calling object
         */
        LowHighDetectionConfigEditor setLowGMode(LowGMode mode);
        /**
         * Sets the minimum amount of time the acceleration sign does not change for an interrupt
         * @param duration    Duration between [2.5, 640] milliseconds
         * @return Calling object
         */
        LowHighDetectionConfigEditor setHighDuration(int duration);
        /**
         * Sets the threshold for clearing high-g interrupt
         * @param threshold    High-g clear interrupt threshold
         * @return Calling object
         */
        LowHighDetectionConfigEditor setHighThreshold(float threshold);
        /**
         * Sets the hysteresis level for clearing the high-g interrupt
         * @param hysteresis    Hysteresis for clearing high-g interrupt
         * @return Calling object
         */
        LowHighDetectionConfigEditor setHighHysteresis(float hysteresis);
        /**
         * Writes the new settings to the board
         */
        void commit();
    }

    /**
     * Wrapper class encapsulating the data from a low/high g intereupt
     */
    interface LowHighResponse {
        /**
         * Checks if the interrupt from from high-g motion.  If it is not high-g motion, there is no
         * need to call {@link #highG(Bmi160Accelerometer.Axis)}.
         * @return True if high-g motion
         */
        boolean isHigh();
        /**
         * Checks if the interrupt from from low-g motion
         * @return True if low-g motion
         */
        boolean isLow();
        /**
         * Check if the specific axis triggered high-g interrupt
         * @param axis    Axis to check
         * @return True if axis triggered high-g interrupt
         */
        boolean highG(Axis axis);
        /**
         * Get the direction of the interrupt
         * @return Direction of the high-g motion interrupt
         */
        Sign highSign();
    }

    /**
     * Selector for available data sources on the BMI160 sensor
     * @author Eric Tsai
     */
    interface SourceSelector extends Accelerometer.SourceSelector {
        /**
         * Handle data from flat detection
         * @return Object representing flat data
         */
        DataSignal fromFlat();
        /**
         * Handle data from step detection
         * @return Object representing step data
         */
        DataSignal fromStepDetection();
        /**
         * Handle data from step counter
         * @param silent    Same value as the silent parameter used for calling {@link #readStepCounter(boolean)}
         * @return Object representing step counts
         */
        DataSignal fromStepCounter(boolean silent);
        /**
         * Handle data from low/high detection
         * @return Object representing low/high interrupt data
         */
        DataSignal fromLowHigh();
    }

    /**
     * Configures the settings for acceleration sampling
     * @return Editor object to configure the settings
     */
    SamplingConfigEditor configureAxisSampling();

    /**
     * Configures the settings for orientation detection
     * @return Editor object to configure the settings
     */
    OrientationConfigEditor configureOrientationDetection();

    /**
     * Configures the settings for flat detection
     * @return Editor object to configure the settings
     */
    FlatDetectionConfigEditor configureFlatDetection();
    /**
     * Enables flat detection
     */
    void enableFlatDetection();
    /**
     * Disables flat detection
     */
    void disableFlatDetection();

    /**
     * Configures the settings for step detection
     * @return Editor object to configure the settings
     */
    StepDetectionConfigEditor configureStepDetection();
    /**
     * Reads the current value in the step counter.  The step counter must first be enabled with a {@link StepDetectionConfigEditor} object.
     * @param silent True if the read should be silent
     */
    void readStepCounter(boolean silent);
    /**
     * Resets the step counter
     */
    void resetStepCounter();
    /**
     * Enable step detection
     */
    void enableStepDetection();
    /**
     * Disable step detection
     */
    void disableStepDetection();

    /**
     * Configures the settings for low/high G detection
     * @return Editor object to configure the settings
     */
    LowHighDetectionConfigEditor configureLowHighDetection();
    /**
     * Enables low/high G detection
     * @param lowG      True if low-g should be detected
     * @param highGx    True if high-g on the x-axis should be detected
     * @param highGy    True if high-g on the y-axis should be detected
     * @param highGz    True if high-g on the z-axis should be detected
     */
    void enableLowHighDetection(boolean lowG, boolean highGx, boolean highGy, boolean highGz);
    /**
     * Disable low/high G detection
     */
    void disableLowHighDetection();

    /**
     * Starts the accelerometer in low power mode
     * @param size      Number of samples to be averaged for undersampling
     */
    void startLowPower(byte size);

    /**
     * Initiates the creation of a route for BMI160 sensor data
     * @return Selection of available data sources
     */
    @Override
    SourceSelector routeData();
}
