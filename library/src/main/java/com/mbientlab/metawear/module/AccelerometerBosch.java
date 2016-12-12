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
import com.mbientlab.metawear.data.CartesianAxis;

import java.util.HashMap;

/**
 * Generic class providing high level access for a Bosch accelerometer.  If you know specifically which
 * accelerometer is on your board, use the appropriate Accelerometer subclass instead.
 * @author Eric Tsai
 * @see AccelerometerBma255
 * @see AccelerometerBmi160
 */
public interface AccelerometerBosch extends Accelerometer {
    /**
     * Available data ranges
     * @author Eric Tsai
     */
    enum AccRange {
        /** +/-2g */
        AR_2G((byte) 0x3, 16384f, 2f),
        /** +/-4g */
        AR_4G((byte) 0x5, 8192f, 4f),
        /** +/-8g */
        AR_8G((byte) 0x8, 4096, 8f),
        /** +/-16g */
        AR_16G((byte) 0xc, 2048f, 16f);

        public final byte bitmask;
        public final float scale, range;

        private static final HashMap<Byte, AccRange> bitMasksToRange;
        static {
            bitMasksToRange= new HashMap<>();
            for(AccRange it: values()) {
                bitMasksToRange.put(it.bitmask, it);
            }
        }

        AccRange(byte bitmask, float scale, float range) {
            this.bitmask = bitmask;
            this.scale = scale;
            this.range= range;
        }

        public static AccRange bitMaskToRange(byte bitMask) {
            return bitMasksToRange.get(bitMask);
        }

        public static float[] ranges() {
            AccRange[] values= values();
            float[] ranges= new float[values.length];
            for(byte i= 0; i < ranges.length; i++) {
                ranges[i]= values[i].range;
            }

            return ranges;
        }
    }

    /**
     * Calculation modes controlling the conditions that determine the sensor's orientation
     * @author Eric Tsai
     */
    enum OrientationMode {
        /** Default mode */
        SYMMETRICAL,
        HIGH_ASYMMETRICAL,
        LOW_ASYMMETRICAL
    }
    /**
     * On-board algorithm that detects changes in the sensor's orientation
     * @author Eric Tsai
     */
    interface OrientationDataProducer extends AsyncDataProducer {
        /**
         * Configuration editor for the orientation detection algorithm
         * @author Eric Tsai
         */
        interface ConfigEditor {
            /**
             * Sets the hysteresis offset for portrait/landscape detection
             * @param hysteresis    New offset angle, in degrees
             * @return Calling object
             */
            ConfigEditor hysteresis(float hysteresis);
            /**
             * Sets the orientation calculation mode
             * @param mode    New calculation mode
             * @return Calling object
             */
            ConfigEditor mode(OrientationMode mode);
            /**
             * writes the new settings to the sensor
             */
            void commit();
        }
        /**
         * Configure the orientation detection algorithm
         * @return Configuration editor object
         */
        ConfigEditor configure();
    }
    /**
     * Gets an object to control the orientation detection algorithm
     * @return Object controlling the orientation detection algorithm
     */
    OrientationDataProducer orientationDetector();

    /**
     * On-board algorithm that detects when the senor is laying flat or not
     * @author Eric Tsai
     */
    interface FlatDataProducer extends AsyncDataProducer {
        /**
         * Accelerometer agnostic interface for configuring flat detection algorithm
         * @param <T>    Type of flat detection config editor
         * @author Eric Tsai
         */
        interface ConfigEditorBase<T extends ConfigEditorBase> {
            /**
             * Sets the delay for which the flat value must remain stable for a flat interrupt.  The closest,
             * valid delay will be chosen depending on underlying sensor
             * @param time    Delay time for a stable value
             * @return Calling object
             */
            T holdTime(float time);
            /**
             * Sets the threshold defining a flat position
             * @param angle    Threshold angle, between [0, 44.8] degrees
             * @return Calling object
             */
            T flatTheta(float angle);
            /**
             * Writes the new settings to the board
             */
            void commit();
        }
        /**
         * Configure the flat detection algorithm
         * @return Generic editor object
         */
        ConfigEditorBase<? extends ConfigEditorBase> configure();
    }
    /**
     * Gets an object to control the flat detection algorithm
     * @return Object controlling the flat detection algorithm
     */
    FlatDataProducer flatDetector();

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
     * Direction of motion for high-g interrupts
     * @author Eric Tsai
     */
    enum Sign {
        POSITIVE,
        NEGATIVE
    }
    /**
     * Wrapper class encapsulating the data from a low/high g interrupt
     */
    interface LowHighResponse {
        /**
         * Checks if the interrupt from from high-g motion.  If it is not high-g motion, there is no
         * need to call {@link #highG(CartesianAxis)}.
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
        boolean highG(CartesianAxis axis);
        /**
         * Get the direction of the interrupt
         * @return Direction of the high-g motion interrupt
         */
        Sign highSign();
    }
    /**
     * On-board algorithm that detects when low (i.e. free fall) or high g acceleration is measured
     * @author Eric Tsai
     */
    interface LowHighDataProducer extends AsyncDataProducer {
        /**
         * Interface for configuring low/high g detection
         * @author Eric Tsai
         */
        interface ConfigEditor {
            /**
             * Enable low g detection on all 3 axes
             * @return Calling object
             */
            ConfigEditor enableLowG();
            /**
             * Enable high g detection on the x-axis
             * @return Calling object
             */
            ConfigEditor enableHighGx();
            /**
             * Enable high g detection on the y-axis
             * @return Calling object
             */
            ConfigEditor enableHighGy();
            /**
             * Enable high g detection on the z-axis
             * @return Calling object
             */
            ConfigEditor enableHighGz();

            /**
             * Sets the minimum amount of time the acceleration must stay below (ths + hys) for an interrupt
             * @param duration    Duration between [2.5, 640] milliseconds
             * @return Calling object
             */
            ConfigEditor lowDuration(int duration);
            /**
             * Sets the threshold that triggers a low-g interrupt
             * @param threshold    Low-g interrupt threshold, between [0.00391, 2.0] g
             * @return Calling object
             */
            ConfigEditor lowThreshold(float threshold);
            /**
             * Sets the hysteresis level for low-g interrupt
             * @param hysteresis    Low-g interrupt hysteresis, between [0, 0.375]g
             * @return Calling object
             */
            ConfigEditor lowHysteresis(float hysteresis);
            /**
             * Sets mode for low-g detection
             * @param mode    Low-g detection mode
             * @return Calling object
             */
            ConfigEditor lowGMode(LowGMode mode);
            /**
             * Sets the minimum amount of time the acceleration sign does not change for an interrupt
             * @param duration    Duration between [2.5, 640] milliseconds
             * @return Calling object
             */
            ConfigEditor highDuration(int duration);
            /**
             * Sets the threshold for clearing high-g interrupt
             * @param threshold    High-g clear interrupt threshold
             * @return Calling object
             */
            ConfigEditor highThreshold(float threshold);
            /**
             * Sets the hysteresis level for clearing the high-g interrupt
             * @param hysteresis    Hysteresis for clearing high-g interrupt
             * @return Calling object
             */
            ConfigEditor highHysteresis(float hysteresis);
            /**
             * Writes the new settings to the board
             */
            void commit();
        }
        /**
         * Configure the low/high g detection algorithm
         * @return Configuration editor object
         */
        ConfigEditor configure();
    }
    /**
     * Gets an object to control the low/high g detection algorithm
     * @return Object controlling the low/high g detection algorithm
     */
    LowHighDataProducer lowHighDetector();

    /**
     * Wrapper class encapsulating responses from motion detection.  Only any motion detection has
     * responses
     * @author Eric Tsai
     */
    interface MotionResponse {
        /**
         * Slope sign of the triggering signal
         * @return Positive or negative
         */
        Sign anyMotionSign();
        /**
         * Checks if any motion was detected on that axis
         * @param axis    Axis to check
         * @return True if the axis triggered the any motion interrupt
         */
        boolean anyMotionDetected(CartesianAxis axis);
    }
    /**
     * On-board algorithm that detects different types of motion
     * @author Eric Tsai
     */
    interface MotionDataProducer extends AsyncDataProducer {
        /**
         * Configuration editor for no-motion detection
         * @author Eric Tsai
         */
        interface NoMotionConfigEditor {
            /**
             * Sets the duration
             * @param duration    Time, in milliseconds, for which no slope data points exceed the threshold
             * @return Calling object
             */
            NoMotionConfigEditor duration(int duration);

            /**
             * Sets the tap threshold.  This value is shared with slow motion detection.
             * @param threshold    Threshold, in Gs, for which no slope data points must exceed
             * @return Calling object
             */
            NoMotionConfigEditor threshold(float threshold);
            /**
             * Writes the settings to the board
             */
            void commit();
        }
        /**
         * Configure the no-motion detection algorithm
         * @return Calling object
         */
        NoMotionConfigEditor configureNoMotion();

        /**
         * Configuration editor for no-motion detection
         * @author Eric Tsai
         */
        interface AnyMotionConfigEditor {
            /**
             * Sets the duration
             * @param duration    Number of consecutive slope data points that are above the threshold
             * @return Calling object
             */
            AnyMotionConfigEditor duration(int duration);
            /**
             * Sets the threshold that the slope data points must be above
             * @param threshold    Any motion threshold, in Gs
             * @return Calling object
             */
            AnyMotionConfigEditor threshold(float threshold);
            /**
             * Writes the settings to the board
             */
            void commit();
        }
        /**
         * Configure the any-motion detection algorithm
         * @return Calling object
         */
        AnyMotionConfigEditor configureAnyMotion();

        /**
         * Configuration editor for slow-motion detection
         * @author Eric Tsai
         */
        interface SlowMotionConfigEditor {
            /**
             * Sets the count
             * @param count    Number of consecutive slope data points that must be above the threshold
             * @return Calling object
             */
            SlowMotionConfigEditor count(byte count);
            /**
             * Sets the tap threshold.  This value is shared with no motion detection
             * @param threshold    Threshold, in Gs, for which no slope data points must exceed
             * @return Calling object
             */
            SlowMotionConfigEditor threshold(float threshold);
            /**
             * Writes the settings to the board
             */
            void commit();
        }
        /**
         * Configure the slow-motion detection algorithm
         * @return Calling object
         */
        SlowMotionConfigEditor configureSlowMotion();
    }
    /**
     * Gets an object to control the motion detection algorithm
     * @return Object controlling the motion detection algorithm
     */
    MotionDataProducer motionDetector();

    /**
     * Available quiet times for double tap detection
     * @author Eric Tsai
     */
    enum TapQuietTime {
        /** 30ms */
        TQT_30_MS,
        /** 20ms */
        TQT_20_MS
    }

    /**
     * Available shock times for tap detection
     * @author Eric Tsai
     */
    enum TapShockTime {
        /** 50ms */
        TST_50_MS,
        /** 75ms */
        TST_75_MS
    }

    /**
     * Available windows for double tap detection
     * @author Eric Tsai
     */
    enum DoubleTapWindow {
        /** 50ms */
        DTW_50_MS,
        /** 100ms */
        DTW_100_MS,
        /** 150ms */
        DTW_150_MS,
        /** 200ms */
        DTW_200_MS,
        /** 250ms */
        DTW_250_MW,
        /** 375ms */
        DTW_375_MS,
        /** 500ms */
        DTW_500_MS,
        /** 700ms */
        DTW_700_MS
    }

    /**
     * Tap types to detect on the BMI160 chip
     * @author Eric Tsai
     */
    enum TapType {
        SINGLE,
        DOUBLE
    }

    /**
     * Wrapper class encapsulating responses from tap detection
     * @author Eric Tsai
     */
    interface TapResponse {
        /**
         * Get tap type of the response
         * @return Single or double tap
         */
        TapType type();
        /**
         * Sign of the triggering signal
         * @return Positive or negative
         */
        Sign sign();
    }
    /**
     * On-board algorithm that detects taps
     * @author Eric Tsai
     */
    interface TapDataProducer extends AsyncDataProducer {
        /**
         * Configuration editor for the tap detection algorithm
         * @author Eric Tsai
         */
        interface ConfigEditor {
            /**
             * Sets the time that must pass before a second tap can occur
             * @param time    New quiet time
             * @return Calling object
             */
            ConfigEditor quietTime(TapQuietTime time);
            /**
             * Sets the time to lock the data in the status register
             * @param time    New shock time
             * @return Calling object
             */
            ConfigEditor shockTime(TapShockTime time);
            /**
             * Sets the length of time for a second shock to occur for a double tap
             * @param window    New double tap window
             * @return Calling object
             */
            ConfigEditor doubleTapWindow(DoubleTapWindow window);
            /**
             * Sets the threshold that the acceleration difference must exceed for a tap, in g's
             * @param threshold    New tap threshold
             * @return Calling object
             */
            ConfigEditor threshold(float threshold);
            /**
             * Sets which tap types to detect
             * @param types    Tap types to detect
             * @return Calling object
             */
            ConfigEditor type(TapType ... types);
            /**
             * Writes the configuration to the sensor
             */
            void commit();
        }
        /**
         * Configure the tap detection algorithm
         * @return Configuration editor object
         */
        ConfigEditor configure();
    }
    /**
     * Gets an object to control the tap detection algorithm
     * @return Object controlling the tap detection algorithm
     */
    TapDataProducer tapDetector();
}
