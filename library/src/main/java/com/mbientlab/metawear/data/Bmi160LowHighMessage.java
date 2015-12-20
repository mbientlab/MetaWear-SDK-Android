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

package com.mbientlab.metawear.data;

import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.module.Bmi160Accelerometer.Axis;
import com.mbientlab.metawear.module.Bmi160Accelerometer.LowHighResponse;
import com.mbientlab.metawear.module.Bmi160Accelerometer.Sign;
import java.util.Calendar;


/**
 * Container class for low/high G detection data from the BMI160 sensor.  Data is interpreted as a LowHighResponse
 * object
 * @author Eric Tsai
 * @see LowHighResponse
 */
public class Bmi160LowHighMessage extends Message {
    private final LowHighResponse response;

    public Bmi160LowHighMessage(byte[] data) {
        this(null, data);
    }

    public Bmi160LowHighMessage(Calendar timestamp, byte[] data) {
        super(timestamp, data);

        final Sign sign;
        final boolean highInt, lowInt;
        final byte highFirst;

        highInt= (data[0] & 0x1) == 0x1;
        lowInt= (data[0] & 0x2) == 0x2;
        highFirst= (byte) ((data[0] & 0x1c) >> 2);
        sign= (data[0] & 0x20) == 0x20 ? Sign.NEGATIVE : Sign.POSITIVE;

        response= new LowHighResponse() {
            @Override
            public boolean isHigh() {
                return highInt;
            }

            @Override
            public boolean isLow() {
                return lowInt;
            }

            @Override
            public boolean highG(Axis axis) {
                byte mask= (byte) (0x1 << axis.ordinal());
                return (highFirst & mask) == mask;
            }

            @Override
            public Sign highSign() {
                return sign;
            }

            @Override
            public String toString() {
                return String.format("{low: %s, high: %s, high_x: %s, high_y: %s, high_z: %s, high_direction: %s}",
                        isLow(), isLow(), highG(Axis.X), highG(Axis.Y), highG(Axis.Z), highSign().toString());
            }
        };
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(LowHighResponse.class)) {
            return type.cast(response);
        }

        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));
    }
}
