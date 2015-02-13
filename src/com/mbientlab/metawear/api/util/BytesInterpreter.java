/*
 * Copyright 2014 MbientLab Inc. All rights reserved.
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
package com.mbientlab.metawear.api.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Helper functions to convert bytes into meaningful data
 * @author Eric Tsai
 */
public class BytesInterpreter {
    /**
     * Convert raw accelerometer data into Gs based on its configuration.  This version is 
     * for the values received from the receivedDataValue callback function.  Starting from 
     * firmware v0.9.0, the accelerometer data is already converted to milli Gs.  You do not need 
     * to use this function if you are on firmware 0.9.0 or higher.
     * @param dataConfig Byte array representing the configuration data
     * @param axisAccel Acceleration data as passed back in the callback function
     * @return Number of Gs the bytes represent
     */
    public static float bytesToGs(byte[] dataConfig, short axisAccel) {
        byte scale= (byte) (1 << (dataConfig[0] & 0x3));
        
        return (float) (axisAccel * (scale / 1024.0));
    }
    /**
     * Convert raw accelerometer data into Gs based on its configuration.  This version 
     * is for processing the bytes as received from the MetaWear board.  Starting from 
     * firmware v0.9.0, the accelerometer data is already converted to milli Gs.  You do not need 
     * to use this function if you are on firmware 0.9.0 or higher.
     * @param dataConfig Byte array representing the configuration data
     * @param accelOutput Unprocessed acceleration data as received from the Metawear board
     * @return Number of Gs the bytes represent
     */
    public static float bytesToGs(byte[] dataConfig, byte[] accelOutput) {
        short output= ByteBuffer.wrap(accelOutput).order(ByteOrder.LITTLE_ENDIAN).getShort();
        
        return bytesToGs(dataConfig, output);
    }
    /**
     * Convert raw accelerometer data into Gs.  This version is for processing data  
     * received from the logger and boards using firmware 0.9.0 or higher.
     * @param accelOutput Bytes from the log entry
     * @param offset Index offset to start the conversion from.  For 1 axis triggers, this 
     * value should always be 0.  For 2 axis triggers (XY or YZ), use 0 for the first axis, 
     * and 2 for the second axis.
     * @return Number of Gs the bytes represent
     */
    public static float logBytesToGs(byte[] accelOutput, byte offset) {
        short output= ByteBuffer.wrap(accelOutput, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        return output / 1000.0f;
    }
    /**
     * Convert raw temperature data into Celsius.  This method is for firmware prior to v1.0.0. 
     * @param tempOutput Byte representation of temperature data
     * @param thermistorMode True if the data was recorded in thermistor mode
     * @return Converted temperature in Celsius
     */
    public static float bytesToTemp(byte[] tempOutput, boolean thermistorMode) {
        short temp= ByteBuffer.wrap(tempOutput).order(ByteOrder.LITTLE_ENDIAN).getShort();
        return temp * (thermistorMode ? 0.125f : 0.25f);
    }
    /**
     * This version of the function is for data received from firmware v1.0.0
     * @param tempOutput Byte representation of the temp data from the logger
     * @return Converted temperature in Celsius
     */
    public static float bytesToTemp(byte[] tempOutput) {
        return bytesToTemp(tempOutput, true);
    }
    
    /**
     * Convert data into the switch state 
     * @param switchOutput Output data as received from the Metawear board
     * @return True if the switch is pressed, false if released
     */
    public static boolean bytesToSwitchState(byte[] switchOutput) {
        return (switchOutput[0] & 0x1) == 0x1;
    }
}
