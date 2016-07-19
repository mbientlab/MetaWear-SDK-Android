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

import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.module.Bmi160Accelerometer.AnyMotionConfigEditor;
import com.mbientlab.metawear.module.Bmi160Accelerometer.LowHighDetectionConfigEditor;
import com.mbientlab.metawear.module.Bmi160Accelerometer.NoMotionConfigEditor;
import com.mbientlab.metawear.module.Bmi160Accelerometer.OrientationMode;
import com.mbientlab.metawear.module.Bmi160Accelerometer.SlowMotionConfigEditor;
import com.mbientlab.metawear.module.Bmi160Accelerometer.TapConfigEditor;
import com.mbientlab.metawear.module.Bmi160Accelerometer.TapType;

//TODO Keep code DRY, have Bma255 and Bmi160 extend from common interface
/**
 * Controls the BMA255 accelerometer.  This sensor is only an accelerometer and is available on
 * MetaEnvironment and MetaDetect boards.  It shares many features with the BMI160 accelerometer minus
 * the step counter and significant motion detection
 * @author Eric Tsai
 */
public interface Bma255Accelerometer extends Accelerometer {
    /**
     * Operating frequencies of the accelerometer
     * @author Eric Tsai
     */
    enum OutputDataRate {
        ODR_15_62HZ {
            @Override public float frequency() { return 15.62f; }
        },
        ODR_31_26HZ {
            @Override public float frequency() { return 31.26f; }
        },
        ODR_62_5HZ {
            @Override public float frequency() { return 62.5f; }
        },
        ODR_125HZ {
            @Override public float frequency() { return 125f; }
        },
        ODR_250HZ {
            @Override public float frequency() { return 250f; }
        },
        ODR_500HZ {
            @Override public float frequency() { return 500f; }
        },
        ODR_1000HZ {
            @Override public float frequency() { return 1000f; }
        },
        ODR_2000HZ {
            @Override public float frequency() { return 2000f; }
        };

        public byte bitMask() { return (byte) (ordinal() + 8); }
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
     * Types of motion detection on the BMA255 chip
     * @author Eric Tsai
     */
    enum MotionType {
        /** Detects if there is no motion */
        NO_MOTION,
        /** Same as any motion exceed without information on which axis triggered the interrupt */
        SLOW_MOTION,
        /** Detects motion using the slope of successive acceleration signals */
        ANY_MOTION
    }

    /**
     * Enumeration of hold times for flat detection
     * @author Eric Tsai
     */
    enum FlatHoldTime {
        /** 0 milliseconds */
        FHT_0_MS,
        /** 512 milliseconds */
        FHT_512_MS,
        /** 1024 milliseconds */
        FHT_1024_MS,
        /** 2048 milliseconds */
        FHT_2048_MS
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
        SamplingConfigEditor setFullScaleRange(Bmi160Accelerometer.AccRange range);
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
         * Handle data from low/high detection
         * @return Object representing low/high interrupt data
         */
        DataSignal fromLowHigh();
        /**
         * Handle data from motion detection
         * @return Object representing motion data
         */
        DataSignal fromMotion();
        /**
         * Handle data from tap detection
         * @return Object representing tap data
         */
        DataSignal fromTap();
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
     * Configures the settings for orientation detection
     * @return Editor object to configure the settings
     */
    OrientationConfigEditor configureOrientationDetection();

    /**
     * Configures the settings for acceleration sampling
     * @return Editor object to configure the settings
     */
    SamplingConfigEditor configureAxisSampling();

    /**
     * Configures any motion detection
     * @return Editor object to configure the settings
     */
    AnyMotionConfigEditor configureAnyMotionDetection();
    /**
     * Configures no motion detection
     * @return Editor object to configure the detection settings
     */
    NoMotionConfigEditor configureNoMotionDetection();
    /**
     * Configures slow motion detection
     * @return Editor object to configure the detection settings
     */
    SlowMotionConfigEditor configureSlowMotionDetection();
    /**
     * Enables motion detection
     * @param type    Type of motion to detect
     */
    void enableMotionDetection(MotionType type);
    /**
     * Disables motion detection
     */
    void disableMotionDetection();

    /**
     * Configures tap detection
     * @return Editor object to configure the detection settings
     */
    TapConfigEditor configureTapDetection();
    /**
     * Enables tap detection
     * @param types    Tap types to detect
     */
    void enableTapDetection(TapType... types);
    /**
     * Disables tap detection
     */
    void disableTapDetection();

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
     * Initiates the creation of a route for BMA255 sensor data
     * @return Selection of available data sources
     */
    @Override
    SourceSelector routeData();
}
