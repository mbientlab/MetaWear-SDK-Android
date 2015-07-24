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

package com.mbientlab.metawear.data;

import com.mbientlab.metawear.Message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

/**
 * Created by etsai on 6/16/2015.
 */
public class Mma8452qAxisMessage extends Message {
    private final short[] milliGs;
    private final float[] accelGs;

    public Mma8452qAxisMessage(byte[] data) {
        this(null, data);
    }

    public Mma8452qAxisMessage(Calendar timestamp, byte[] data) {
        super(timestamp, data);

        ByteBuffer buffer= ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        milliGs= new short[] {buffer.getShort(), buffer.getShort(), buffer.getShort()};
        accelGs= new float[] {milliGs[0] / 1000.f, milliGs[1] / 1000.f, milliGs[2] / 1000.f};
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(AccelAxisG.class)) {
            return type.cast(new AccelAxisG() {
                @Override
                public float x() {
                    return accelGs[0];
                }

                @Override
                public float y() {
                    return accelGs[1];
                }

                @Override
                public float z() {
                    return accelGs[2];
                }
            });
        } else if (type.equals(AccelAxisMilliG.class)) {
            return type.cast(new AccelAxisMilliG() {
                @Override
                public short x() {
                    return milliGs[0];
                }

                @Override
                public short y() {
                    return milliGs[1];
                }

                @Override
                public short z() {
                    return milliGs[2];
                }
            });
        }
        return super.getData(type);
    }
}
