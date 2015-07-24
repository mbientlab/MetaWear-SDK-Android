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
import com.mbientlab.metawear.module.Mma8452qAccelerometer.Axis;
import com.mbientlab.metawear.module.Mma8452qAccelerometer.MovementData;

import java.util.Calendar;

/**
 * Created by etsai on 7/21/2015.
 */
public class Mma8452qMovementMessage extends Message {
    private final MovementData movementData;

    public Mma8452qMovementMessage(byte[] data) {
        this(null, data);
    }

    public Mma8452qMovementMessage(Calendar timestamp, byte[] data) {
        super(timestamp, data);

        final byte ffMtSrc= data[0];

        movementData= new MovementData() {
            @Override
            public boolean crossedThreshold(Axis axis) {
                byte mask= (byte) (2 << (2 * axis.ordinal()));
                return (ffMtSrc & mask) == mask;
            }

            @Override
            public Polarity polarity(Axis axis) {
                byte mask= (byte) (1 << (2 * axis.ordinal()));
                return (ffMtSrc & mask) == mask ? Polarity.NEGATIVE : Polarity.POSITIVE;
            }

            @Override
            public String toString() {
                boolean first= true;
                StringBuilder builder= new StringBuilder();

                builder.append("{");
                for(Axis it: Axis.values()) {
                    builder.append(String.format("%sAxis%s:{crossedThreshold: %s, polarity: %s}",
                            (first ? "" : ", "), it.toString(), crossedThreshold(it), polarity(it)));
                    first= false;
                }
                builder.append("}");
                return builder.toString();
            }
        };
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(MovementData.class)) {
            return type.cast(movementData);
        }
        return super.getData(type);
    }
}
