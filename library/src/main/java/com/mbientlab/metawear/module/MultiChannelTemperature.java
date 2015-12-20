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

import com.mbientlab.metawear.DataSignal;

import java.util.List;

/**
 * Communicates with various temperature sources.  Only available on firmware v1.0.4 and higher
 * @author Eric Tsai
 */
public interface MultiChannelTemperature extends Temperature {
    /**
     * Constants listing the channel positions for available temperature sources on the MetaWear R board.
     * These constants can be used to index the sources list returned from {@link #getSources()}.
     * @author Eric Tsai
     * @see #getSources()
     */
    class MetaWearRChannel {
        public static final byte NRF_DIE= 0, EXT_THERMISTOR= 1;
    }

    /**
     * Constants listing the channel positions for available temperature sources on the MetaWear R+Gyro/Pro board.
     * These constants can be used to index the sources list returned from {@link #getSources()}.
     * @author Eric Tsai
     * @see #getSources()
     */
    class MetaWearProChannel {
        public static final byte NRF_DIE= 0, ON_BOARD_THERMISTOR= 1, EXT_THERMISTOR= 2, BMP_280= 3;
    }
    /**
     * Wrapper class encapsulating information about a temperature source
     * @author Eric Tsai
     */
    interface Source {
        /**
         * Retrieves the driver ID
         * @return Driver ID
         */
        byte driver();

        /**
         * Retrieves the channel position.  Different boards may have difference channel arrangements
         * for the same driver
         * @return Channel position
         */
        byte channel();

        /**
         * Retrieves a readable name for the temperature source
         * @return Name for the source
         */
        String getName();
    }

    /**
     * Temperature provided by the nrf chip
     * @author Eric Tsai
     */
    interface NrfDie extends Source { }
    /**
     * Temperature provided by an external thermistor
     * @author Eric Tsai
     */
    interface ExtThermistor extends Source {
        /**
         * Configures the settings for the thermistor
         * @param analogReadPin    GPIO pin that reads the data
         * @param pulldownPin      GPIO pin the pulldown resistor is connected to
         * @param activeHigh       True if the pulldown pin is active high
         */
        void configure(byte analogReadPin, byte pulldownPin, boolean activeHigh);
    }
    /**
     * Temperature provided by the BMP280 barometer sensor
     * @author Eric Tsai
     */
    interface BMP280 extends Source { }
    /**
     * Temperature provided by an pre-configured thermistor
     * @author Eric Tsai
     */
    interface PresetThermistor extends Source { }

    /**
     * Read the available temperature sources on the board
     * @return Read only list of the available sources
     */
    List<Source> getSources();

    /**
     * Read the temperature from the specified source
     * @param src    Source to read from
     */
    void readTemperature(Source src);
    /**
     * Read the temperature from the specified source
     * @param src    Source to read from
     * @param silent True if read should be silent
     */
    void readTemperature(Source src, boolean silent);

    /**
     * Selector for available temperature sources
     * @author Eric Tsai
     */
    interface SourceSelector extends Temperature.SourceSelector {
        /**
         * Handle data from a specific temperature source
         * @param src    Temperature source the route is for
         * @return Object representing temperature data
         */
        DataSignal fromSource(Source src);
        /**
         * Handle data from a specific temperature source.  This version of the function pairs with
         * the {@link #readTemperature(Source, boolean)} variant
         * @param src    Temperature source the route is for
         * @param silent Same value as the silent parameter for calling {@link #readTemperature(Source, boolean)}
         * @return Object representing temperature data
         */
        DataSignal fromSource(Source src, boolean silent);
    }

    /**
     * Initiates the creation of a route for temperature data
     * @return Selection of available data sources
     */
    @Override
    SourceSelector routeData();
}
