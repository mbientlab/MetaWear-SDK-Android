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
import com.mbientlab.metawear.module.Tcs34725ColorDetector.ColorAdc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Locale;

/**
 * Container class for color ADC data from the TCS34725 color detector.  Data is interpreted as a ColorAdc type.
 * @author Eric Tsai
 * @see ColorAdc
 */
public class Tcs34725ColorMessage extends Message {
    private final ColorAdc adc;

    public Tcs34725ColorMessage(byte[] data) {
        this(null, data);
    }

    public Tcs34725ColorMessage(Calendar timestamp, final byte[] data) {
        super(timestamp, data);

        if (data.length >= 8) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            final int[] adcValues = new int[]{
                    buffer.getShort() & 0xffff,
                    buffer.getShort() & 0xffff,
                    buffer.getShort() & 0xffff,
                    buffer.getShort() & 0xffff
            };

            adc = new ColorAdc() {
                @Override
                public int clear() {
                    return adcValues[0];
                }

                @Override
                public int red() {
                    return adcValues[1];
                }

                @Override
                public int green() {
                    return adcValues[2];
                }

                @Override
                public int blue() {
                    return adcValues[3];
                }

                @Override
                public String toString() {
                    return String.format(Locale.US, "{clear: %d, red: %d, green: %d, blue: %d}", clear(), red(), green(), blue());
                }
            };
        } else {
            adc = null;
        }
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(ColorAdc.class)) {
            return type.cast(adc);
        }
        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));
    }
}
