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
import com.mbientlab.metawear.ConfigEditorBase;
import com.mbientlab.metawear.Configurable;
import com.mbientlab.metawear.data.CartesianAxis;
import com.mbientlab.metawear.data.Sign;
import com.mbientlab.metawear.data.TapType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

/**
 * Extension of the {@link Accelerometer} interface providing finer control of the BMI270 accelerometer
 * @author Laura Kassovic
 */
public interface AccelerometerBmi270 extends Accelerometer {
    enum FilterMode {
        OSR4,
        OSR2,
        NORMAL
    }
    /**
     * Operating frequencies of the BMI270 accelerometer
     * @author Laura Kassovic
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
     * Available data ranges
     * @author Laura Kassovic
     */
    enum AccRange {
        /** +/-2g */
        AR_2G((byte) 0x0, 16384f, 2f),
        /** +/-4g */
        AR_4G((byte) 0x1, 8192f, 4f),
        /** +/-8g */
        AR_8G((byte) 0x2, 4096, 8f),
        /** +/-16g */
        AR_16G((byte) 0x3, 2048f, 16f);

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
     * Accelerometer configuration editor specific to the BMI270 accelerometer
     * @author Laura Kassovic
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
        /**
         * Set the filter mode.  This parameter is ignored if the data rate is less than 12.5Hz
         * @param mode New filter mode
         * @return Calling object
         */
        ConfigEditor filter(FilterMode mode);
    }

    /**
     * Configure the BMI270 accelerometer
     * @return Editor object specific to the BMI270 accelerometer
     */
    @Override
    ConfigEditor configure();
}
