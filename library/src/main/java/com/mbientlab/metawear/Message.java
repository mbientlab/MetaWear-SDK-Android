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

package com.mbientlab.metawear;

import java.util.Calendar;

/**
 * Container class for a sample of sensor data.
 * @author Eric Tsai
 */
public abstract class Message {
    private final Calendar timestamp;
    private final byte[] data;

    /**
     * Creates a new Message for data streamed to the phone / tablet.
     * @param data    Data from a sensor stream
     */
    public Message(byte[] data) {
        this(null, data);
    }

    /**
     * Creates a new Message for data received from the logger.
     * @param timestamp    Date and time of when the data was created
     * @param data         Sensor data from the logger
     */
    public Message(Calendar timestamp, byte[] data) {
        this.timestamp= (timestamp == null ? Calendar.getInstance() : timestamp);
        this.data= data;
    }

    /**
     * Retrieves the timestamp of when the data was either logged or received from a stream.  The
     * timestamp will not be accurate for high frequency streams (i.e  data from the accelerometer).
     * @return Data creation date
     */
    public Calendar getTimestamp() {
        return timestamp;
    }

    /**
     * Retrieves the data timestamp as a formatted string.
     * @return Formatted string representation of the timestamp
     * @see #getTimestamp()
     */
    public String getTimestampAsString() {
        return String.format("%tY%<tm%<td-%<tH%<tM%<tS%<tL", timestamp);
    }

    /**
     * Retrieves the sensor data as is.
     * @return Raw sensor data as a byte array
     */
    public byte[] getData() {
        byte[] copy= new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }

    /**
     * Interprets the data as the given type.  Note that sensors can have different interpretations for
     * the same data type.
     * @param type    Type to interpret the data as
     * @return Data as the specified type
     */
    public abstract <T> T getData(Class<T> type);

    @Override
    public String toString() {
        StringBuilder builder= new StringBuilder();
        builder.append(String.format("%02x", data[0]));
        for(int i= 1; i < data.length; i++) {
            builder.append(String.format(", %02x", data[i]));
        }

        return String.format("{timestamp: %s, data: [%s]}", getTimestampAsString(), builder.toString());
    }
}
