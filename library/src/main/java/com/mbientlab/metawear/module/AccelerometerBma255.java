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
 * Extension of the {@link AccelerometerBosch} interface providing finer control of the BMA255 accelerometer
 * @author Eric Tsai
 */
public interface AccelerometerBma255 extends AccelerometerBosch {
    /**
     * Operating frequencies of the accelerometer
     * @author Eric Tsai
     */
    enum OutputDataRate {
        /** 15.62 Hz */
        ODR_15_62HZ(15.62f),
        /** 31.26 Hz */
        ODR_31_26HZ(31.26f),
        /** 62.5 Hz */
        ODR_62_5HZ(62.5f),
        /** 125 Hz */
        ODR_125HZ(125f),
        /** 250 Hz */
        ODR_250HZ(250f),
        /** 500 Hz */
        ODR_500HZ(500f),
        /** 1000 Hz */
        ODR_1000HZ(1000f),
        /** 2000 Hz */
        ODR_2000HZ(2000f);

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
     * Accelerometer configuration editor specific to the BMA255 accelerometer
     * @author Eric Tsai
     */
    interface ConfigEditor extends Accelerometer.ConfigEditor<ConfigEditor> {
        /**
         * Set the output data rate
         * @param odr    New output data rate
         * @return Calling object
         */
        ConfigEditor odr(OutputDataRate odr);
        /**
         * Set the data range
         * @param fsr    New data range
         * @return Calling object
         */
        ConfigEditor range(AccRange fsr);
    }
    /**
     * Configure the BMA255 accelerometer
     * @return Editor object specific to the BMA255 accelerometer
     */
    @Override
    ConfigEditor configure();

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

        /** Periods represented as a float value */
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
     * Configuration editor specific to BMA255 flat detection
     * @author Eric Tsai
     */
    interface FlatConfigEditor extends AccelerometerBosch.FlatConfigEditor<FlatConfigEditor> {
        FlatConfigEditor holdTime(FlatHoldTime time);
    }
    /**
     * Extension of the {@link AccelerometerBosch.FlatDataProducer} interface providing
     * configuration options specific to the BMA255 accelerometer
     * @author Eric Tsai
     */
    interface FlatDataProducer extends AccelerometerBosch.FlatDataProducer {
        /**
         * Configure the flat detection algorithm
         * @return BMA255 specific configuration editor object
         */
        @Override
        FlatConfigEditor configure();
    }
    /**
     * Get an implementation of the BMA255 specific FlatDataProducer interface
     * @return BMA255 specific FlatDataProducer object
     */
    @Override
    FlatDataProducer flat();
}
