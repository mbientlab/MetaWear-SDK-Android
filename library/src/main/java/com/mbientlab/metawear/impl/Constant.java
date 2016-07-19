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

/**
 * Created by etsai on 7/8/2015.
 */
public class Constant {
    public static final String METAWEAR_R_MODULE = "0", METAWEAR_RG_MODULE= "1", METAWEAR_C_MODULE= "2";
    public static final Version MULTI_CHANNEL_TEMP_MIN_FIRMWARE= new Version(1, 0, 4), SERVICE_DISCOVERY_MIN_FIRMWARE = MULTI_CHANNEL_TEMP_MIN_FIRMWARE,
            MULTI_CHANNEL_MATH= new Version(1, 1, 0), DISCONNECTED_EVENT= MULTI_CHANNEL_MATH, MULTI_COMPARISON_MIN_FIRMWARE= new Version(1, 2, 3);

    public static final byte SINGLE_CHANNEL_TEMP_IMPLEMENTATION= 0, MULTI_CHANNEL_TEMP_IMPLEMENTATION= 1;

    public static final byte MMA8452Q_IMPLEMENTATION= 0, BMI160_IMPLEMENTATION= 1, BMA255_IMPLEMENTATION= 3;
    public static final byte BMI160_GYRO_IMPLEMENTATION= 0;

    public static final byte LTR329_LIGHT_SENSOR= 0;
    public static final byte BMP280_BAROMETER= 0, BME280_BAROMETER= 1;

    public static final byte EXTENDED_LOGGING_REVISION= 2;

    public static final byte BMM150_MAGNETOMETER= 0;
    public static final byte SETTINGS_CONN_PARAMS_REVISION= 1, SETTINGS_DISCONNECTED_EVENT_REVISION= 2, SETTINGS_BATTERY_REVISION= 3;

    public static final byte GSR_IMPLEMENTATION= 1;
    public static final byte GPIO_ENHANCED_ANALOG= 2;

    public static final byte LED_DELAYED_REVISION= 1;

    public static final byte SPI_REVISION = 1;
}
