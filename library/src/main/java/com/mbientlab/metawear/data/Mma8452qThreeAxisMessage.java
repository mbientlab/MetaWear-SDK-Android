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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

/**
 * Container class for accelerometer data from the MMA8452Q chip.  By default, data is interpreted as
 * a CartesianShort type and reported in units of milliG's.  Interpreting the data as a CartesianFloat converts
 * the data to G's
 * @author Eric Tsai
 * @see CartesianFloat
 * @see CartesianShort
 */
public class Mma8452qThreeAxisMessage extends Message {
    private final short[] milliGs;
    private final float[] accelGs;

    public Mma8452qThreeAxisMessage(byte[] data) {
        this(null, data);
    }

    public Mma8452qThreeAxisMessage(Calendar timestamp, byte[] data) {
        super(timestamp, data);

        ByteBuffer buffer= ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        milliGs= new short[] {buffer.getShort(), buffer.getShort(), buffer.getShort()};
        accelGs= new float[] {milliGs[0] / 1000.f, milliGs[1] / 1000.f, milliGs[2] / 1000.f};
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(CartesianShort.class) || type.equals(Cartesian.class)) {
            return type.cast(new CartesianShort() {
                @Override
                public Short x() {
                    return milliGs[0];
                }

                @Override
                public Short y() {
                    return milliGs[1];
                }

                @Override
                public Short z() {
                    return milliGs[2];
                }
            });
        }

        if (type.equals(CartesianFloat.class)) {
            return type.cast(new CartesianFloat() {
                @Override
                public Float x() {
                    return accelGs[0];
                }

                @Override
                public Float y() {
                    return accelGs[1];
                }

                @Override
                public Float z() {
                    return accelGs[2];
                }
            });
        }

        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));
    }
}
