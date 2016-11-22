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
 * Created by etsai on 9/1/16.
 */
public interface AccelerometerBmi160 extends AccelerometerBosch {
    /**
     * Operating frequencies of the BMI160 accelerometer
     * @author Eric Tsai
     */
    enum OutputDataRate {
        ODR_0_78125_HZ(0.78125f),
        ODR_1_5625_HZ(1.5625f),
        ODR_3_125_HZ(3.125f),
        ODR_6_25_HZ(6.25f),
        ODR_12_5_HZ(12.5f),
        ODR_25_HZ(25f),
        ODR_50_HZ(50f),
        ODR_100_HZ(100f),
        ODR_200_HZ(200f),
        ODR_400_HZ(400f),
        ODR_800_HZ(800f),
        ODR_1600_HZ(1600f);

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

    interface Bmi160ConfigEditor extends ConfigEditorBase<Bmi160ConfigEditor> {
        Bmi160ConfigEditor odr(OutputDataRate odr);
        Bmi160ConfigEditor range(AccRange fsr);
    }

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
    interface StepDetectorConfigEditor {
        /**
         * Sets the operational mode of the step detector.  The setting balances sensitivity and robustness.
         * @param mode    Detector sensitivity
         * @return Calling object
         */
        StepDetectorConfigEditor mode(StepDetectorMode mode);
        void commit();
    }

    interface StepDetectorDataProducer extends AsyncDataProducer {
        StepDetectorConfigEditor configure();
    }
    StepDetectorDataProducer stepDetector();

    interface StepCounterDataProducer extends ForcedDataProducer {
        StepDetectorConfigEditor configure();
        void reset();
    }
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

        public final float period;

        FlatHoldTime(float period) {
            this.period = period;
        }

        public static float[] periods() {
            FlatHoldTime[] values= values();
            float[] pers= new float[values.length];
            for(byte i= 0; i < pers.length; i++) {
                pers[i]= values[i].period;
            }

            return pers;
        }
    }

    interface FlatDataProducer extends AccelerometerBosch.FlatDataProducer {
        interface ConfigEditor extends ConfigEditorBase<ConfigEditor> {
            ConfigEditor holdTime(FlatHoldTime time);
        }

        @Override
        ConfigEditor configure();
    }
    @Override
    FlatDataProducer flatDetector();

    /**
     * Skip times available for significant motion detection
     * @author Eric Tsai
     */
    enum SkipTime {
        ST_1_5_S,
        ST_3_S,
        ST_6_S,
        ST_12_S
    }
    /**
     * Proof times available for significant motion detection
     * @author Eric Tsai
     */
    enum ProofTime {
        PT_0_25_S,
        PT_0_5_S,
        PT_1_S,
        PT_2_S
    }

    interface MotionDataProducer extends AccelerometerBosch.MotionDataProducer {
        /**
         * Interface for configuring significant motion detection
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
             * Writes the settings to the board
             */
            void commit();
        }
        SignificantMotionConfigEditor configureSignificantMotion();
    }
    @Override
    MotionDataProducer motionDetector();
}
