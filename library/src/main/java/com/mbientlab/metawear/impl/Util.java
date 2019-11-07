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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by etsai on 9/4/16.
 */
public class Util {
    public static int closestIndex(float[] values, float key) {
        float smallest= Math.abs(values[0] - key);
        int place= 0;

        for(int i= 1; i < values.length; i++) {
            float distance= Math.abs(values[i] - key);
            if (distance < smallest) {
                smallest= distance;
                place= i;
            }
        }

        return place;
    }

    public static String arrayToHexString(byte[] value) {
        if (value == null) {
            return "null";
        }
        if (value.length == 0) {
            return "[]";
        }

        StringBuilder builder= new StringBuilder();
        builder.append(String.format("[0x%02x", value[0]));
        for(int i= 1; i < value.length; i++) {
            builder.append(String.format(", 0x%02x", value[i]));
        }
        builder.append("]");

        return builder.toString();
    }

    static ByteBuffer bytesToSIntBuffer(boolean logData, byte[] data, DataAttributes attributes) {
        final byte[] actual;

        if (logData) {
            actual = new byte[Math.min(data.length, attributes.length())];
            System.arraycopy(data, 0, actual, 0, actual.length);
        } else {
            actual = new byte[Math.min(data.length - attributes.offset, attributes.length())];
            System.arraycopy(data, attributes.offset, actual, 0, actual.length);
        }

        return ByteBuffer.wrap(Util.padByteArray(actual, 4, true)).order(ByteOrder.LITTLE_ENDIAN);
    }

    static ByteBuffer bytesToUIntBuffer(boolean logData, byte[] data, DataAttributes attributes) {
        final byte[] actual;

        if (logData) {
            actual = new byte[Math.min(data.length, attributes.length())];
            System.arraycopy(data, 0, actual, 0, actual.length);
        } else {
            actual = new byte[Math.min(data.length - attributes.offset, attributes.length())];
            System.arraycopy(data, attributes.offset, actual, 0, actual.length);
        }

        return ByteBuffer.wrap(Util.padByteArray(actual, 8, false)).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static byte[] padByteArray(byte[] input, int newSize, boolean signed) {
        if (newSize <= input.length) {
            byte[] copy= new byte[input.length];
            System.arraycopy(input, 0, copy, 0, input.length);

            return copy;
        }

        byte[] copy= new byte[newSize];
        byte padByte;

        if (signed && (input[input.length - 1] & 0x80) == 0x80) {
            padByte= (byte) 0xff;
        } else {
            padByte= 0;
        }

        System.arraycopy(input, 0, copy, 0, input.length < newSize ? input.length : newSize);
        if (newSize > input.length) {
            Arrays.fill(copy, input.length, newSize, padByte);
        }
        return copy;
    }

    static String createProducerChainString(DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
        Deque<DataTypeBase> parents = new LinkedList<>();
        DataTypeBase current = source;

        do {
            parents.push(current);
            current = current.input;
        } while(current != null);

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        while(!parents.isEmpty()) {
            if (!first) {
                builder.append(":");
            }
            builder.append(DataTypeBase.createUri(parents.poll(), mwPrivate));
            first = false;
        }

        return builder.toString();
    }

    static byte clearRead(byte value) {
        return (byte) (value & 0x3f);
    }
    static byte setRead(byte value) {
        return (byte) (0x80 | value);
    }
    static byte setSilentRead(byte value) {
        return (byte) (0xc0 | value);
    }
}
