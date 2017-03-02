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
 * Extension of the {@link BarometerBosch} interface providing finer control over the barometer on
 * the BMP280 pressure sensor
 * @author Eric Tsai
 */
public interface BarometerBmp280 extends BarometerBosch {
    /**
     * Supported stand by times on the BMP280 sensor
     * @author Eric Tsai
     */
    enum StandbyTime {
        /** 0.5ms */
        TIME_0_5(0.5f),
        /** 62.5ms */
        TIME_62_5(62.5f),
        /** 125ms */
        TIME_125(125f),
        /** 250ms */
        TIME_250(250f),
        /** 500ms */
        TIME_500(500f),
        /** 1000ms */
        TIME_1000(1000f),
        /** 2000ms */
        TIME_2000(2000f),
        /** 4000ms */
        TIME_4000(4000f);

        public final float time;

        StandbyTime(float time) {
            this.time= time;
        }

        public static float[] times() {
            StandbyTime[] entries= StandbyTime.values();
            float[] times= new float[entries.length];

            int i= 0;
            for(StandbyTime it: entries) {
                times[i]= it.time;
                i++;
            }

            return times;
        }
    }

    /**
     * Barometer configuration editor specific to the BMP280 barometer
     * @author Eric Tsai
     */
    interface ConfigEditor extends BarometerBosch.ConfigEditor<ConfigEditor> {
        /**
         * Set the standby time
         * @param time    New standby time
         * @return Calling object
         */
        ConfigEditor standbyTime(StandbyTime time);
    }

    /**
     * Configures BMP280 barometer
     * @return Editor object specific to the BMP280 barometer
     */
    @Override
    ConfigEditor configure();
}
