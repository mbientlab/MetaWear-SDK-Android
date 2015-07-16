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

import com.mbientlab.metawear.MetaWearBoard;

import java.util.HashMap;

/**
 * Created by etsai on 7/8/2015.
 */
public interface Bmi160Accelerometer extends Accelerometer {
    public enum AccRange {
        AR_2G {
            @Override
            public byte bitMask() { return 0x3; }

            @Override
            public int scale() { return 16384; }
        },
        AR_4G {
            @Override
            public byte bitMask() { return 0x5; }

            @Override
            public int scale() { return 8192; }
        },
        AR_8G {
            @Override
            public byte bitMask() { return 0x8; }

            @Override
            public int scale() { return 4096; }
        },
        AR_16G {
            @Override
            public byte bitMask() { return 0xc; }

            @Override
            public int scale() { return 2048; }
        };

        public abstract byte bitMask();
        public abstract int scale();

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

    public enum OutputDataRate {
        ODR_0_78125_HZ,
        ODR_1_5625_HZ,
        ODR_3_125_HZ,
        ODR_6_25_HZ,
        ODR_12_5_HZ,
        ODR_25_HZ,
        ODR_50_HZ,
        ODR_100_HZ,
        ODR_200_HZ,
        ODR_400_HZ,
        ODR_800_HZ,
        ODR_1600_HZ;

        public byte bitMask() { return (byte) (ordinal() + 1); }
    }

    public AxisSamplingConfigEditor configureAxisSampling();
    public void startAxisSampling();
    public void stopAxisSampling();

    public interface AxisSamplingConfigEditor {
        public AxisSamplingConfigEditor withDataRange(AccRange range);
        public AxisSamplingConfigEditor withOutputDataRate(OutputDataRate odr);
        public void commit();
    }
}
