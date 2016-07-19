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
import java.util.Locale;

/**
 * Container class for acceleration data from the BMI160 and BMA255 accelerometers.  By default, data is interpreted as
 * a CartesianFloat type and reported in units of G's.  Interpreting the data as a CartesianShort converts
 * the data to milliG's
 * @author Eric Tsai
 * @see CartesianFloat
 * @see CartesianShort
 */
public class Bmi160ThreeAxisMessage extends Message {
    private final float scale;
    private final float[] scaledValues;
    private final short[] scaledShortValues;

    public Bmi160ThreeAxisMessage(byte[] data, float scale) {
        this(null, data, scale);
    }

    public Bmi160ThreeAxisMessage(Calendar timestamp, byte[] data, float scale) {
        super(timestamp, data);

        this.scale= scale;

        if (data.length >= 6) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            short x = buffer.getShort(), y = buffer.getShort(), z = buffer.getShort();

            scaledValues = new float[]{
                    x / scale,
                    y / scale,
                    z / scale,
            };
            scaledShortValues = new short[]{
                    (short) ((x * 1000) / scale),
                    (short) ((y * 1000) / scale),
                    (short) ((z * 1000) / scale)
            };
        } else {
            scaledValues= null;
            scaledShortValues= null;
        }
    }

    /**
     * Retrieves the LSB to g ratio
     * @return Value corresponding to 1 g
     */
    public float getScale() {
        return scale;
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(CartesianFloat.class)) {
            return type.cast(new CartesianFloat() {
                @Override
                public Float x() { return scaledValues[0]; }

                @Override
                public Float y() { return scaledValues[1]; }

                @Override
                public Float z() { return scaledValues[2]; }
            });
        }

        if (type.equals(CartesianShort.class)) {
            return type.cast(new CartesianShort() {
                @Override
                public Short x() { return scaledShortValues[0]; }

                @Override
                public Short y() { return scaledShortValues[1]; }

                @Override
                public Short z() { return scaledShortValues[2]; }
            });
        }

        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "{%s, scale: %.3f}", super.toString(), scale);
    }
}
