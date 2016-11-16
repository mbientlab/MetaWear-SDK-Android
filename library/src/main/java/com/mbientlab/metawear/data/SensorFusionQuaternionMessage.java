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
import com.mbientlab.metawear.module.SensorFusion.Quaternion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by etsai on 11/8/16.
 */

public class SensorFusionQuaternionMessage extends Message {
    private final float w, x, y, z;

    public SensorFusionQuaternionMessage(byte[] data) {
        this(null, data);
    }

    public SensorFusionQuaternionMessage(Calendar timestamp, byte[] data) {
        super(timestamp, data);

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        w = buffer.getFloat();
        x = buffer.getFloat();
        y = buffer.getFloat();
        z = buffer.getFloat();
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(Quaternion.class)) {
            return type.cast(new Quaternion() {
                @Override
                public String toString() {
                    return String.format(Locale.US, "{w: %.3f, x: %.3f, y: %.3f, z: %.3f}",
                            w(), x(), y(), z());
                }

                @Override
                public float w() {
                    return w;
                }

                @Override
                public float x() {
                    return x;
                }

                @Override
                public float y() {
                    return y;
                }

                @Override
                public float z() {
                    return z;
                }
            });
        }
        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));
    }
}
