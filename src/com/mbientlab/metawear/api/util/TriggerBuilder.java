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

import com.mbientlab.metawear.api.Register;
import com.mbientlab.metawear.api.controller.Accelerometer;
import com.mbientlab.metawear.api.controller.DataProcessor;
import com.mbientlab.metawear.api.controller.DataProcessor.FilterConfig;
import com.mbientlab.metawear.api.controller.GPIO;
import com.mbientlab.metawear.api.controller.Logging;
import com.mbientlab.metawear.api.controller.Logging.Trigger;

/**
 * Helper class to construct Trigger objects needed for the DataProcessor and Logging classes.  
 * All triggers are usable for the DataProcessor class but some triggers are not suitable for 
 * the Logging class.
 * @author Eric Tsai
 * @see LoggingTrigger
 */
public class TriggerBuilder {   
    private static final Trigger accelTrigger= new Trigger() {
        @Override public Register register() { return Accelerometer.Register.DATA_VALUE; }
        @Override public byte index() { return (byte) 0xff; }
        @Override public byte offset() { return 0; }
        @Override public byte length() { return 6; }
    };
    
    /**
     * Constructs a trigger for all axis data from the accelerometer.  This trigger is not 
     * suitable for the logger.  To log axis data, users should use the accelerometer 
     * triggers defined in the {@link LoggingTrigger} enum.
     * @return Trigger object for axis data
     * @see LoggingTrigger#ACCELEROMETER_X_AXIS
     * @see LoggingTrigger#ACCELEROMETER_Y_AXIS
     * @see LoggingTrigger#ACCELEROMETER_Z_AXIS
     */
    public static Trigger buildAccelerometerTrigger() {
        return accelTrigger;
    }
    /**
     * Constructs a trigger for reading GPIO analog values.  This trigger must be used with the 
     * read variants of trigger functions such as addReadFilter and addReadTrigger.
     * @param readSupplyRatio True if the supply ratio should be used, false to use absolute voltage
     * @param gpioPin GPIO pin to read from
     * @return Trigger object for reading analog values from a GPIO pin
     * @see DataProcessor#addReadFilter(Trigger, FilterConfig)
     * @see Logging#addReadTrigger(Trigger)
     */
    public static Trigger buildGPIOAnalogTrigger(final boolean readSupplyRatio, final byte gpioPin) {
        return new Trigger() {
            @Override public Register register() {
                return readSupplyRatio ? 
                    GPIO.Register.READ_ANALOG_INPUT_SUPPLY_RATIO :
                    GPIO.Register.READ_ANALOG_INPUT_ABS_VOLTAGE;
            }
            @Override public byte index() { return gpioPin; }
            @Override public byte offset() { return 0; }
            @Override public byte length() { return GPIO.ANALOG_DATA_SIZE; }
        }; 
    }
    /**
     * Constructs a trigger for reading GPIO digital values.  This trigger must be used with the 
     * read variants of trigger functions such as addReadFilter and addReadTrigger.
     * @param gpioPin GPIO pin to read from
     * @return Trigger object for reading digital values from a GPIO pin
     * @see DataProcessor#addReadFilter(Trigger, FilterConfig)
     * @see Logging#addReadTrigger(Trigger)
     */
    public static Trigger buildGPIODigitalTrigger(final byte gpioPin) {
        return new Trigger() {
            @Override public Register register() { return GPIO.Register.READ_DIGITAL_INPUT; }
            @Override public byte index() { return gpioPin; }
            @Override public byte offset() { return 0; }
            @Override public byte length() { return GPIO.DIGITAL_DATA_SIZE; }
        }; 
    }
    /**
     * Constructs a trigger for GPIO pin state change notifications.
     * @param gpioPin GPIO pin to received notifications on
     * @return Trigger object for pin state changes
     */
    public static Trigger buildPinChangeNotifyTrigger(final byte gpioPin) {
        return new Trigger() {
            @Override public Register register() { return GPIO.Register.PIN_CHANGE_NOTIFY; }
            @Override public byte index() { return gpioPin; }
            @Override public byte offset() { return 0; }
            @Override public byte length() { return GPIO.PIN_CHANGE_NOTIFY_SIZE; }
        };
    }
    /**
     * Constructs a trigger for the output of a data filter.  This filter is not suitable 
     * for {@link Logging#addTrigger(Trigger)} if the data size is greater than 4 bytes
     * @param filterId Numerical ID of the filter the trigger represents
     * @param dataLength How many bytes the filter data is
     * @return Trigger for the filter data
     * @see com.mbientlab.metawear.api.controller.DataProcessor.Callbacks#receivedFilterId(byte)
     */
    public static Trigger buildDataFilterTrigger(final byte filterId, final byte dataLength) {
        return new Trigger() {
            @Override public Register register() { return DataProcessor.Register.FILTER_NOTIFICATION; }
            @Override public byte index() { return filterId; }
            @Override public byte offset() { return 0; }
            @Override public byte length() { return dataLength; }
        };
    }
}
