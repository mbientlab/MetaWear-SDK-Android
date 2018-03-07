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
 * A sample of sensor data
 * @author Eric Tsai
 */
public interface Data {
    /**
     * Time of when the data was received (streaming) or created (logging)
     * @return Data timestamp
     */
    Calendar timestamp();
    /**
     * String representation of the timestamp in the format <code>YYYY-MM-DDTHH:MM:SS.LLL</code>.  The timezone
     * of the string will be the local device's current timezone.
     * @return Formatted string representing the timestamp
     */
    String formattedTimestamp();
    /**
     * LSB to units ratio.  Only used if developer is manually type casting the returned byte array from
     * the {@link #bytes()} method
     * @return Value corresponding to 1 unit
     */
    float scale();
    /**
     * Raw byte representation of the data value
     * @return Byte array of the value
     */
    byte[] bytes();
    /**
     * Classes that can be used when calling {@link #value(Class)}
     * @return Array of valid classes
     */
    Class<?>[] types();

    /**
     * Converts the data bytes to a usable data type
     * @param clazz     Class type to convert the value to
     * @param <T>       Runtime type the return value is casted as
     * @return Data value as the specified type
     * @throws ClassCastException if the data cannot be casted to desired type
     */
    <T> T value(Class<T> clazz);
    /**
     * Extra information attached to this data sample
     * @param clazz     Class type to convert the value to
     * @param <T>       Runtime type the return value is casted as
     * @return Extra data casted as the specified type
     * @throws ClassCastException if the data cannot be casted to the desired type
     */
    <T> T extra(Class<T> clazz);
}
