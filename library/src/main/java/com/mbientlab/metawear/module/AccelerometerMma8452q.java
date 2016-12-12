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

/**
 * Extension of the {@link Accelerometer} interface that provides finer control of the MMA8452Q accelerometer
 * @author Eric Tsai
 */
public interface AccelerometerMma8452q extends Accelerometer {
    /**
     * Supported data rates on the MMA8452Q accelerometer
     * @author Eric Tsai
     */
    enum OutputDataRate {
        /** 800Hz */
        ODR_800_HZ(800f),
        /** 400Hz */
        ODR_400_HZ(400f),
        /** 200Hz */
        ODR_200_HZ(200f),
        /** 100Hz */
        ODR_100_HZ(100f),
        /** 50Hz */
        ODR_50_HZ(50f),
        /** 12.5Hz */
        ODR_12_5_HZ(12.5f),
        /** 6.25Hz */
        ODR_6_25_HZ(6.25f),
        /** 1.56Hz */
        ODR_1_56_HZ(1.56f);

        public final float frequency;

        OutputDataRate(float frequency) {
            this.frequency= frequency;
        }

        public static float[] frequencies() {
            OutputDataRate[] values= values();
            float[] freqs= new float[values.length];
            for(byte i= 0; i < freqs.length; i++) {
                freqs[i]= values[i].frequency;
            }

            return freqs;
        }
    }

    /**
     * Available data ranges
     * @author Eric Tsai
     */
    enum FullScaleRange {
        /** +/-2g */
        FSR_2G(2f),
        /** +/-4g */
        FSR_4G(4f),
        /** +/-8g */
        FSR_8G(8f);

        public final float range;

        FullScaleRange(float range) {
            this.range = range;
        }

        public static float[] ranges() {
            FullScaleRange[] values= values();
            float[] ranges= new float[values.length];
            for(byte i= 0; i < ranges.length; i++) {
                ranges[i]= values[i].range;
            }

            return ranges;
        }
    }

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
     * Accelerometer configuration editor specific to the MMA8452Q accelerometer
     * @author Eric Tsai
     */
    interface Mma8452qConfigEditor extends ConfigEditorBase<Mma8452qConfigEditor> {
        /**
         * Sets the output data rate
         * @param odr    New output data rate
         * @return Calling object
         */
        Mma8452qConfigEditor odr(OutputDataRate odr);
        /**
         * Sets the data range
         * @param fsr    New data range
         * @return Calling object
         */
        Mma8452qConfigEditor range(FullScaleRange fsr);
    }
    /**
     * Configure the MMA8452Q accelerometer
     * @return Editor object specific to the MMA8452Q accelerometer
     */
    @Override
    Mma8452qConfigEditor configure();
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
             * Sets the time for which the sensor's orientation must remain in the new position before a position
             * change is triggered.  This is used to filter out false positives from shaky hands or other small vibrations
             * @param delay    How long the sensor must remain in the new position, in milliseconds
             * @return Calling object
             */
            ConfigEditor delay(int delay);
            /**
             * Write the changes to the accelerometer
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
    OrientationDataProducer orientationDetection();

    /**
     * Base class for configuring the built-in classification algorithms
     * @param <T>    Type of classification config editor
     * @author Eric Tsai
     */
    interface ClassificationConfigEditor<T extends ClassificationConfigEditor> {
        /**
         * Sets the threshold
         * @param threshold    Threshold, in G's
         * @return Calling object
         */
        T threshold(float threshold);
        /**
         * Sets the duration for which a condition must be met to trigger a data event
         * @param duration    Duration for the condition to be met
         * @return Calling object
         */
        T duration(int duration);
        /**
         * Write the new settings to the accelerometer
         */
        void commit();
    }

    /**
     * Wrapper class encapsulating shake data
     * @author Eric Tsai
     */
    interface Shake {
        boolean exceedsThreshold(CartesianAxis axis);
        Polarity polarity(CartesianAxis axis);
    }
    /**
     * On-board algorithm that detects when the sensor is shaken
     * @author Eric Tsai
     */
    interface ShakeDataProducer extends AsyncDataProducer {
        /**
         * Configuration editor for the shake detection algorithm
         * @author Eric Tsai
         */
        interface ConfigEditor extends ClassificationConfigEditor<ConfigEditor> {
            /**
             * Sets the axis to detect shaking motion
             * @param axis    Axis to detect shaking
             * @return Calling object
             */
            ConfigEditor axis(CartesianAxis axis);
        }
        /**
         * Configure the shake detection algorithm
         * @return Configuration editor object
         */
        ConfigEditor configure();
    }
    /**
     * Gets an object to control the shake detection algorithm
     * @return Object controlling the shake detection algorithm
     */
    ShakeDataProducer shakeDetection();

    /**
     * Detectable tap types
     * @author Eric Tsai
     */
    enum TapType {
        SINGLE,
        DOUBLE
    }

    /**
     * Wrapper class encapsulating tap data
     * @author Eric Tsai
     */
    interface Tap {
        boolean active(CartesianAxis axis);
        Polarity polarity(CartesianAxis axis);
        TapType type();
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
        interface ConfigEditor extends ClassificationConfigEditor<ConfigEditor> {
            /**
             * Set the wait time between the end of the 1st shock and when the 2nd shock can be detected
             * @param latency    New latency time, in milliseconds
             * @return Calling object
             */
            ConfigEditor latency(int latency);
            /**
             * Set the time in which a second shock must begin after the latency expires
             * @param window    New window time, in milliseconds
             * @return Calling object
             */
            ConfigEditor window(int window);
            /**
             * Set how the tap detection should processes the data
             * @param enable        True to enable low pass filtering
             * @return Calling object
             */
            ConfigEditor lowPassFilter(boolean enable);
            /**
             * Sets the axis to detect tapping on
             * @param axis    Axis to detect tapping
             * @return Calling object
             */
            ConfigEditor axis(CartesianAxis axis);
            /**
             * Sets which tap types to detect
             * @param types    Tap types to detect
             * @return Calling object
             */
            ConfigEditor type(TapType ... types);
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
    TapDataProducer tapDetection();

    /**
     * Detectable movement types on the sensor
     * @author Eric Tsai
     */
    enum MovementType {
        FREE_FALL,
        MOTION
    }
    /**
     * Wrapper interface encapsulating movement data
     * @author Eric Tsai
     */
    interface Movement {
        boolean crossedThreshold(CartesianAxis axis);
        Polarity polarity(CartesianAxis axis);
    }
    /**
     * On-board algorithm that detects sensor movement
     * @author Eric Tsai
     */
    interface MovementDataProducer extends AsyncDataProducer {
        /**
         * Configuration editor for the movement detection algorithm
         * @author Eric Tsai
         */
        interface ConfigEditor extends ClassificationConfigEditor<ConfigEditor> {
            /**
             * Sets the axes to be detected for movement
             * @param axes    Axes to detect movement
             * @return Calling object
             */
            ConfigEditor axes(CartesianAxis ... axes);
        }
        /**
         * Configure the movement detection algorithm
         * @param type    Type of movement the algorithm should detect
         * @return Configuration editor object
         */
        ConfigEditor configure(MovementType type);
    }
    /**
     * Gets an object to control the movement detection algorithm
     * @return Object controlling the movement detection algorithm
     */
    MovementDataProducer movementDetection();

    /**
     * Enumeration of sleep mode data rates
     * @author Eric Tsai
     */
    enum SleepModeRate {
        /** 50Hz */
        SMR_50_HZ,
        /** 12.5Hz */
        SMR_12_5_HZ,
        /** 6.25Hz */
        SMR_6_25_HZ,
        /** 1.56Hz */
        SMR_1_56_HZ
    }

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
     * Accelerometer feature where the sensor transitions between different sampling rates depending on
     * the frequency of interrupts
     * @author Eric Tsai
     */
    interface AutoSleep {
        /**
         * Configuration editor for the auto sleep mode
         * @author Eric Tsai
         */
        interface ConfigEditor {
            /**
             * Sets the operating frequency in sleep mode
             * @param rate    New sleep mode data rate
             * @return Calling object
             */
            ConfigEditor dataRate(SleepModeRate rate);
            /**
             * Sets how long to idle in active mode before switching to sleep mode
             * @param timeout    New timeout value, in milliseconds
             * @return Calling object
             */
            ConfigEditor timeout(int timeout);
            /**
             * Sets the power mode while in sleep mode
             * @param powerMode    New power mode to use
             * @return Calling object
             */
            ConfigEditor powerMode(PowerMode powerMode);
            /**
             * Write the changes
             */
            void commit();
        }
        /**
         * Configure auto sleep mode
         * @return Configuration editor object
         */
        ConfigEditor configure();
        /**
         * Enable auto sleep mode.  Change will not be written to the sensor until {@link Mma8452qConfigEditor#commit()} is called
         */
        void enable();
        /**
         * Disable auto sleep mode.  Change will not be written to the sensor until {@link Mma8452qConfigEditor#commit()} is called
         */
        void disable();
    }
    /**
     * Gets an object to control the auto sleep feature
     * @return Object controlling the auto sleep feature
     */
    AutoSleep autosleep();
}
