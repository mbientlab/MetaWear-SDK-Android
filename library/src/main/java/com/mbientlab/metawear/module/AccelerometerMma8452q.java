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
import com.mbientlab.metawear.datatype.CartesianAxis;

/**
 * Accelerometer for the MetaWear R board
 * @author Eric Tsai
 */
public interface AccelerometerMma8452q extends Accelerometer {
    /**
     * Supported data rates on the MMA8452Q accelerometer
     * @author Eric Tsai
     */
    enum OutputDataRate {
        ODR_800_HZ(800f),
        ODR_400_HZ(400f),
        ODR_200_HZ(200f),
        ODR_100_HZ(100f),
        ODR_50_HZ(50f),
        ODR_12_5_HZ(12.5f),
        ODR_6_25_HZ(6.25f),
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
     * Max ranges of the MMA8452Q accelerometer data
     * @author Eric Tsai
     */
    enum FullScaleRange {
        FSR_2G(2f),
        FSR_4G(4f),
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

    interface Mma8452qConfigEditor extends ConfigEditorBase<Mma8452qConfigEditor> {
        Mma8452qConfigEditor odr(OutputDataRate odr);
        Mma8452qConfigEditor range(FullScaleRange fsr);
        void commit();
    }

    @Override
    Mma8452qConfigEditor configure();

    interface OrientationDataProducer extends AsyncDataProducer {
        /**
         * Interface for configuring settings for orientation detection
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
             * Write the changes to the board
             */
            void commit();
        }

        ConfigEditor configure();
    }
    OrientationDataProducer orientationDetection();

    /**
     * Base class for configuring the built-in classification algorithms
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
         * Write the new settings to the board
         */
        void commit();
    }

    interface Shake {
        boolean exceedsThreshold(CartesianAxis axis);
        Polarity polarity(CartesianAxis axis);
    }

    interface ShakeDataProducer extends AsyncDataProducer {
        /**
         * Interface for configuring shake detection
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

        ConfigEditor configure();
    }
    ShakeDataProducer shakeDetection();

    /**
     * Detectable tap types
     * @author Eric Tsai
     */
    enum TapType {
        SINGLE,
        DOUBLE
    }

    interface Tap {
        boolean active(CartesianAxis axis);
        Polarity polarity(CartesianAxis axis);
        TapType type();
    }

    interface TapDataProducer extends AsyncDataProducer {
        /**
         * Interface for configuring tap detection parameters
         * @author Eric Tsai
         */
        interface ConfigEditor extends ClassificationConfigEditor<ConfigEditor> {
            /**
             * Set the latency value
             * @param latency Wait time, in ms, between the end of the 1st shock and
             * when the 2nd shock can be detected
             * @return Calling object
             */
            ConfigEditor latency(int latency);
            /**
             * Set the window value
             * @param window Time, in ms, in which a second shock must begin after
             * the latency expires
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
            ConfigEditor type(TapType ... types);
        }

        ConfigEditor configure();
    }
    TapDataProducer tapDetection();

    /**
     * Detectable movement types on the sensor
     * @author Eric Tsai
     */
    enum MovementType {
        FREE_FALL,
        MOTION
    }
    interface Movement {
        boolean crossedThreshold(CartesianAxis axis);
        Polarity polarity(CartesianAxis axis);
    }
    interface MovementDataProducer extends AsyncDataProducer {
        /**
         * Interface for configuring motion / free fall detection
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

        ConfigEditor configure(MovementType type);
    }
    MovementDataProducer movementDetection();

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
     * Enumeration of the available power modes on the accelerometer
     * @author Eric Tsai
     */
    enum PowerMode {
        NORMAL,
        LOW_NOISE_LOW_POWER,
        HIGH_RES,
        LOW_POWER
    }

    interface AutoSleep {
        /**
         * Interface for configuring auto sleep mode
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
             * Sets the timeout period
             * @param timeout    How long to idle in active mode before switching to sleep mode, in milliseconds
             * @return Calling object
             */
            ConfigEditor timeout(int timeout);
            /**
             * Sets the power mode while in sleep mode
             * @param powerMode    New power mode to use
             * @return Calling object
             */
            ConfigEditor powerMode(PowerMode powerMode);
            void commit();
        }
        ConfigEditor configure();
        void enable();
        void disable();
    }
    AutoSleep autosleep();
}
