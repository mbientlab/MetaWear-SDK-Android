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
import static com.mbientlab.metawear.module.Bmm150Magnetometer.ThresholdDetectionType.*;
import com.mbientlab.metawear.module.Bmm150Magnetometer.ThresholdDetectionType;
import com.mbientlab.metawear.module.Bmm150Magnetometer.ThresholdInterrupt;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Container class for threshold detection data from the BMM150 magnetometer.  The data can be interpreted as a
 * ThresholdInterrupt type or a boolean array.  The boolean array is indexed by the ThresholdDetectionType
 * enum values.
 * @author Eric Tsai
 * @see ThresholdInterrupt
 * @see ThresholdDetectionType
 */
public class Bmm150ThresholdMessage extends Message {
    private final boolean[] interrupts;
    private final ThresholdInterrupt interruptObj;

    public Bmm150ThresholdMessage(byte[] data) {
        this(null, data);
    }

    public Bmm150ThresholdMessage(Calendar timestamp, final byte[] data) {
        super(timestamp, data);

        ThresholdDetectionType[] types= ThresholdDetectionType.values();
        interrupts= new boolean[types.length];

        byte mask= 0x1;
        for(ThresholdDetectionType type: types) {
            interrupts[type.ordinal()]= (data[0] & mask) == mask;
            mask <<= 1;
        }

        interruptObj = new ThresholdInterrupt() {
            @Override
            public boolean crossed(ThresholdDetectionType type) {
                return interrupts[type.ordinal()];
            }

            @Override
            public String toString() {
                return String.format("{low x: %s, low y: %s, low z: %s, high x: %s, high y: %s, high z: %s}",
                        crossed(LOW_X), crossed(LOW_Y), crossed(LOW_Z), crossed(HIGH_X), crossed(HIGH_Y), crossed(HIGH_Z));
            }
        };
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(ThresholdInterrupt.class)) {
            return type.cast(interruptObj);
        } else if (type.equals(boolean[].class)){
            return type.cast(Arrays.copyOf(interrupts, interrupts.length));
        }
        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));
    }
}
