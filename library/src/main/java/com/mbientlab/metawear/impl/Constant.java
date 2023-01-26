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

import java.util.HashMap;

/**
 * Created by etsai on 9/20/16.
 */
class Constant {
    static final long RESPONSE_TIMEOUT = 1000L;
    static final byte COMMAND_LENGTH = 18, MAX_BTLE_LENGTH = COMMAND_LENGTH + 2;

    enum Module {
        SWITCH(0x01, "Switch"),
        LED(0x02, "Led"),
        ACCELEROMETER(0x03, "Accelerometer"),
        TEMPERATURE(0x04, "Temperature"),
        GPIO(0x05, "Gpio"),
        NEO_PIXEL(0x06, "Neo_Pixel"),
        IBEACON(0x07, "IBeacon"),
        HAPTIC(0x08, "Haptic"),
        DATA_PROCESSOR(0x09, "DataProcessor"),
        EVENT(0x0a, "Event"),
        LOGGING(0x0b, "Logging"),
        TIMER(0x0c, "Timer"),
        SERIAL_PASSTHROUGH(0x0d, "SerialPassthrough"),
        MACRO(0x0f, "Macro"),
        GSR(0x10, "Conductance"),
        SETTINGS(0x11, "Settings"),
        BAROMETER(0x12, "Barometer"),
        GYRO(0x13, "Gyro"),
        AMBIENT_LIGHT(0x14, "AmbientLight"),
        MAGNETOMETER(0x15, "Magnetometer"),
        HUMIDITY(0x16, "Humidity"),
        COLOR_DETECTOR(0x17, "Color"),
        PROXIMITY(0x18, "Proximity"),
        SENSOR_FUSION(0x19, "SensorFusion"),
        DEBUG(0xfe, "Debug");

        public final byte id;
        public final String friendlyName;

        Module(int id, String friendlyName) {
            this.id= (byte) id;
            this.friendlyName = friendlyName;
        }

        private static final HashMap<Byte, Module> byteToEnum;
        static {
            byteToEnum = new HashMap<>();
            for(Module it: Module.values()) {
                byteToEnum.put(it.id, it);
            }
        }

        public static Module lookupEnum(byte id) {
            return byteToEnum.get(id);
        }
    }
}
