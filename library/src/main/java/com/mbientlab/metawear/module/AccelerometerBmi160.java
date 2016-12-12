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
import com.mbientlab.metawear.ForcedDataProducer;

/**
 * Extension of the {@link AccelerometerBosch} interface that provides finer control of the BMI160 accelerometer features
 * @author Eric Tsai
 */
public interface AccelerometerBmi160 extends AccelerometerBosch {
    /**
     * Operating frequencies of the BMI160 accelerometer
     * @author Eric Tsai
     */
    enum OutputDataRate {
        /** 0.78125 Hz */
        ODR_0_78125_HZ(0.78125f),
        /** 1.5625 Hz */
        ODR_1_5625_HZ(1.5625f),
        /** 3.125 Hz */
        ODR_3_125_HZ(3.125f),
        /** 6.25 Hz */
        ODR_6_25_HZ(6.25f),
        /** 12.5 Hz */
        ODR_12_5_HZ(12.5f),
        /** 25 Hz */
        ODR_25_HZ(25f),
        /** 50 Hz */
        ODR_50_HZ(50f),
        /** 100 Hz */
        ODR_100_HZ(100f),
        /** 200 Hz */
        ODR_200_HZ(200f),
        /** 400 Hz */
        ODR_400_HZ(400f),
        /** 800 Hz */
        ODR_800_HZ(800f),
        /** 1600 Hz */
        ODR_1600_HZ(1600f);

        /** Frequency represented as a float value */
        public final float frequency;

        OutputDataRate(float frequency) {
            this.frequency = frequency;
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
     * Accelerometer configuration editor specific to the BMI160 accelerometer
     * @author Eric Tsai
     */
    interface Bmi160ConfigEditor extends ConfigEditorBase<Bmi160ConfigEditor> {
        /**
         * Sets the output data rate
         * @param odr    New output data rate
         * @return Calling object
         */
        Bmi160ConfigEditor odr(OutputDataRate odr);
        /**
         * Sets the data range
         * @param fsr    New data range
         * @return Calling object
         */
        Bmi160ConfigEditor range(AccRange fsr);
    }

    /**
     * Configure the BMI160 accelerometer
     * @return Editor object specific to the BMI160 accelerometer
     */
    @Override
    Bmi160ConfigEditor configure();

    /**
     * Operation modes for the step detector
     * @author Eric Tsai
     */
    enum StepDetectorMode {
        /** Default mode with a balance between false positives and false negatives */
        NORMAL,
        /** Mode for light weighted persons that gives few false negatives but eventually more false positives */
        SENSITIVE,
        /** Gives few false positives but eventually more false negatives */
        ROBUST
    }

    /**
     * Configuration editor for the step detection algorithm
     * @author Eric Tsai
     */
    interface StepDetectorConfigEditor {
        /**
         * Sets the operational mode of the step detector balancing sensitivity and robustness.
         * @param mode    Detector sensitivity
         * @return Calling object
         */
        StepDetectorConfigEditor mode(StepDetectorMode mode);
        /**
         * Write the configuration to the sensor
         */
        void commit();
    }
    /**
     * Interrupt driven step detection where each detected step triggers a data interrupt
     * @author Eric Tsai
     */
    interface StepDetectorDataProducer extends AsyncDataProducer {
        /**
         * Configure the step detection algorithm
         * @return Editor object
         */
        StepDetectorConfigEditor configure();
    }
    /**
     * Gets an object to control the step detection algorithm
     * @return Object controlling the step detection algorithm
     */
    StepDetectorDataProducer stepDetector();

    /**
     * Accumulates the number of detected steps in a counter that will send it's current value on request
     * @author Eric Tsai
     */
    interface StepCounterDataProducer extends ForcedDataProducer {
        /**
         * Configure the step detection algorithm
         * @return Editor object
         */
        StepDetectorConfigEditor configure();
        /**
         * Resets the internal step counter
         */
        void reset();
    }
    /**
     * Gets an object to control the step counter algorithm
     * @return Object controlling the step counter algorithm
     */
    StepCounterDataProducer stepCounter();

    /**
     * Enumeration of hold times for flat detection
     * @author Eric Tsai
     */
    enum FlatHoldTime {
        /** 0 milliseconds */
        FHT_0_MS(0),
        /** 640 milliseconds */
        FHT_640_MS(640),
        /** 1280 milliseconds */
        FHT_1280_MS(1280),
        /** 2560 milliseconds */
        FHT_2560_MS(2560);

        /** Delays represented as a float value */
        public final float delay;

        FlatHoldTime(float delay) {
            this.delay = delay;
        }

        public static float[] delays() {
            FlatHoldTime[] values= values();
            float[] delayValues= new float[values.length];
            for(byte i= 0; i < delayValues.length; i++) {
                delayValues[i]= values[i].delay;
            }

            return delayValues;
        }
    }

    /**
     * Extension of the FlatDataProducer specific to the BMA255 accelerometer
     * @author Eric Tsai
     */
    interface FlatDataProducer extends AccelerometerBosch.FlatDataProducer {
        /**
         * Configuration editor specific to BMI160 flat detection
         * @author Eric Tsai
         */
        interface ConfigEditor extends ConfigEditorBase<ConfigEditor> {
            ConfigEditor holdTime(FlatHoldTime time);
        }
        /**
         * Configures the flat detection algorithm
         * @return Configuration editor object
         */
        @Override
        ConfigEditor configure();
    }
    /**
     * Gets an object to control the flat detection algorithm
     * @return Object controlling flat detection
     */
    @Override
    FlatDataProducer flatDetector();

    /**
     * Skip times available for significant motion detection
     * @author Eric Tsai
     */
    enum SkipTime {
        /** 1.5s */
        ST_1_5_S,
        /** 3s */
        ST_3_S,
        /** 6s */
        ST_6_S,
        /** 12s */
        ST_12_S
    }
    /**
     * Proof times available for significant motion detection
     * @author Eric Tsai
     */
    enum ProofTime {
        /** 0.25s */
        PT_0_25_S,
        /** 0.5s */
        PT_0_5_S,
        /** 1s */
        PT_1_S,
        /** 2s */
        PT_2_S
    }

    /**
     * Extension of the MotionDataProducer specific to the BMI160 accelerometer
     * @author Eric Tsai
     */
    interface MotionDataProducer extends AccelerometerBosch.MotionDataProducer {
        /**
         * Configuration editor specific to BMI160 significant motion detection
         * @author Eric Tsai
         */
        interface SignificantMotionConfigEditor {
            /**
             * Sets the skip time
             * @param time    Number of seconds to sleep after movement is detected
             * @return Calling object
             */
            SignificantMotionConfigEditor skipTime(SkipTime time);
            /**
             * Sets the proof time
             * @param time    Number of seconds that movement must still be detected after the skip time passed
             * @return Calling object
             */
            SignificantMotionConfigEditor proofTime(ProofTime time);
            /**
             * Writes the settings to the sensor
             */
            void commit();
        }
        SignificantMotionConfigEditor configureSignificantMotion();
    }
    /**
     * Gets an object to control the motion detection algorithm
     * @return Object controlling motion detection
     */
    @Override
    MotionDataProducer motionDetector();
}
