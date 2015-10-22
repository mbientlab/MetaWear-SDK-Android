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
     */
    enum OrientationMode {
        /** Default mode */
        SYMMETRICAL,
        HIGH_ASYMMETRICAL,
        LOW_ASYMMETRICAL
    }

    /**
     * Configures the settings for orientation detection
     * @return Editor object to configure various settings
     */
    OrientationConfigEditor configureOrientationDetection();

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
     * Configures the settings for sampling axis data
     * @return Editor object to configure various settings
     */
    SamplingConfigEditor configureAxisSampling();

    /**
     * Interface for configuring axis sampling
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
}
