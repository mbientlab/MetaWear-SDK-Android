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
import com.mbientlab.metawear.MetaWearBoard.Module;

import java.util.HashMap;

/**
 * Created by etsai on 9/20/16.
 */
public interface GyroBmi160 extends Module {
    /**
     * Operating frequency of the gyro
     * @author Eric Tsai
     */
    enum OutputDataRate {
        ODR_25_HZ,
        ODR_50_HZ,
        ODR_100_HZ,
        ODR_200_HZ,
        ODR_400_HZ,
        ODR_800_HZ,
        ODR_1600_HZ,
        ODR_3200_HZ;

        public final byte bitmask;

        OutputDataRate() {
            this.bitmask= (byte) (ordinal() + 6);
        }
    }

    /**
     * Supported angular rate measurement range
     * @author Eric Tsai
     */
    enum Range {
        /** +/- 2000 degrees / second */
        FSR_2000(16.4f),
        /** +/- 1000 degrees / second */
        FSR_1000(32.8f),
        /** +/- 500 degrees / second */
        FSR_500(65.6f),
        /** +/- 250 degrees / second */
        FSR_250(131.2f),
        /** +/- 125 degrees / second */
        FSR_125(262.4f);

        public final float scale;
        public final byte bitmask;

        Range(float scale) {
            this.scale= scale;
            this.bitmask= (byte) ordinal();
        }

        private static final HashMap<Byte, Range> bitMaskToRanges;
        static {
            bitMaskToRanges= new HashMap<>();
            for(Range it: Range.values()) {
                bitMaskToRanges.put(it.bitmask, it);
            }
        }
        public static Range bitMaskToRange(byte mask) {
            return bitMaskToRanges.get(mask);
        }
    }

    /**
     * Interface to configure parameters for measuring angular rate
     * @author Eric Tsai
     */
    interface ConfigEditor {
        /**
         * Sets the measurement range
         * @param range    New range to use
         * @return Calling object
         */
        ConfigEditor range(Range range);

        /**
         * Sets the output date rate
         * @param odr    New output data rate to use
         * @return Calling object
         */
        ConfigEditor odr(OutputDataRate odr);

        /**
         * Writes the new settings to the board
         */
        void commit();
    }
    /**
     * Configures the settings for measuring angular rates
     * @return Editor object to configure various settings
     */
    ConfigEditor configure();

    interface RotationalSpeedDataProducer extends AsyncDataProducer {
        String xAxisName();
        String yAxisName();
        String zAxisName();
    }

    RotationalSpeedDataProducer angularVelocity();
    AsyncDataProducer packedAngularVelocity();

    void start();
    void stop();
}
