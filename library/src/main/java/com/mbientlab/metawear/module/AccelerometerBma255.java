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

/**
 * Created by etsai on 9/1/16.
 */
public interface AccelerometerBma255 extends AccelerometerBosch {
    /**
     * Operating frequencies of the accelerometer
     * @author Eric Tsai
     */
    enum OutputDataRate {
        ODR_15_62HZ(15.62f),
        ODR_31_26HZ(31.26f),
        ODR_62_5HZ(62.5f),
        ODR_125HZ(125f),
        ODR_250HZ(250f),
        ODR_500HZ(500f),
        ODR_1000HZ(1000f),
        ODR_2000HZ(2000f);

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

    interface Bma255ConfigEditor extends ConfigEditorBase<Bma255ConfigEditor> {
        Bma255ConfigEditor odr(OutputDataRate odr);
        Bma255ConfigEditor range(AccRange fsr);
        void commit();
    }

    @Override
    Bma255ConfigEditor configure();

    /**
     * Types of motion detection on the BMI160 chip
     * @author Eric Tsai
     */
    enum MotionType {
        /** Detects if there is no motion */
        NO_MOTION,
        /** Same as any motion exceed without information on which axis triggered the interrupt */
        SLOW_MOTION,
        /** Detects motion using the slope of successive acceleration signals */
        ANY_MOTION,
    }

    /**
     * Enumeration of hold times for flat detection
     * @author Eric Tsai
     */
    enum FlatHoldTime {
        /** 0 milliseconds */
        FHT_0_MS(0f),
        /** 512 milliseconds */
        FHT_512_MS(512),
        /** 1024 milliseconds */
        FHT_1024_MS(1024),
        /** 2048 milliseconds */
        FHT_2048_MS(2048);

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
}
