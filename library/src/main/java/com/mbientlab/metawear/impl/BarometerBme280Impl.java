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

package com.mbientlab.metawear.impl;

import com.mbientlab.metawear.module.BarometerBme280;

import static com.mbientlab.metawear.impl.Constant.Module.BAROMETER;

/**
 * Created by etsai on 9/20/16.
 */
class BarometerBme280Impl extends BarometerBoschImpl implements BarometerBme280 {
    static final byte IMPLEMENTATION= 1;
    private static final long serialVersionUID = -8762879587731816595L;

    BarometerBme280Impl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
    }

    @Override
    public BarometerBme280.ConfigEditor configure() {
        return new BarometerBme280.ConfigEditor() {
            private OversamplingMode samplingMode= OversamplingMode.STANDARD;
            private FilterCoeff filterCoeff = FilterCoeff.OFF;
            private StandbyTime time= StandbyTime.TIME_0_5;
            private byte tempOversampling= 1;

            @Override
            public BarometerBme280.ConfigEditor standbyTime(StandbyTime time) {
                this.time= time;
                return this;
            }

            @Override
            public void commit() {
                mwPrivate.sendCommand(new byte[] {BAROMETER.id, CONFIG,
                        (byte) ((samplingMode.ordinal() << 2) | (tempOversampling << 5)),
                        (byte) ((filterCoeff.ordinal() << 2) | (time.ordinal() << 5))});
            }

            @Override
            public BarometerBme280.ConfigEditor pressureOversampling(OversamplingMode mode) {
                samplingMode= mode;
                tempOversampling= (byte) ((mode == OversamplingMode.ULTRA_HIGH) ? 2 : 1);
                return this;
            }

            @Override
            public BarometerBme280.ConfigEditor filterCoeff(FilterCoeff coeff) {
                filterCoeff = coeff;
                return this;
            }

            @Override
            public BarometerBme280.ConfigEditor standbyTime(float time) {
                float[] availableTimes= StandbyTime.times();
                return standbyTime(StandbyTime.values()[Util.closestIndex(availableTimes, time)]);
            }
        };
    }
}
