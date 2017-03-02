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

package com.mbientlab.metawear.module;

import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.MetaWearBoard.Module;

/**
 * Accesses the temperature sensors
 * @author Eric Tsai
 */
public interface Temperature extends Module {
    /**
     * Available types of temperature sensors.  Different boards will have a different combination
     * of sensor types
     * @author Eric Tsai
     */
    enum SensorType {
        /** Temperature measured by the nRF SOC */
        NRF_SOC,
        /** Temperature measured by an externally connected thermistor */
        EXT_THERMISTOR,
        /** Temperature measured by either the BMP280 or BME280 sensor */
        BOSCH_ENV,
        /** Temperature measured by an on-board thermistor */
        PRESET_THERMISTOR
    }
    /**
     * Data measured by a temperature sensor
     * @author Eric Tsai
     */
    interface Sensor extends ForcedDataProducer {
        /**
         * Get the type of temperature sensor measuring the data
         * @return Sensor type
         */
        SensorType type();
    }
    /**
     * Temperature data measured by an externally connected thermistor
     * @author Eric Tsai
     */
    interface ExternalThermistor extends Sensor {
        /**
         * Configures the settings for the thermistor
         * @param dataPin           GPIO pin that reads the data
         * @param pulldownPin       GPIO pin the pulldown resistor is connected to
         * @param activeHigh        True if the pulldown pin is active high
         */
        void configure(byte dataPin, byte pulldownPin, boolean activeHigh);
    }

    /**
     * Get an array of available temperature sensors
     * @return Temperature sensors array
     */
    Sensor[] sensors();
    /**
     * Find all temperature sensors whose {@link Sensor#type()} function matches the {@code type} parameter
     * @param type    Sensor type to look for
     * @return Array of sensors matching the sensor type, null if no matches found
     */
    Sensor[] findSensors(SensorType type);
}
