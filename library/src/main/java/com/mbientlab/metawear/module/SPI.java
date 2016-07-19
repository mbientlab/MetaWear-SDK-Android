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

package com.mbientlab.metawear.module;

import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.MetaWearBoard;

/**
 * Communicates with the SPI bus, providing direct access sensors connected via SPI
 * @author Eric Tsai
 */
public interface SPI extends MetaWearBoard.Module {
    /**
     * Selector for the SPI data sources
     * @author Eric Tsai
     */
    interface SourceSelector {
        /**
         * Handle data from an SPI read.  The parameters for this function must match their respective
         * parameters from the {@link #readData(byte, byte, byte[])} function
         * @param numBytes    Number of bytes to expect from the read
         * @param id          User ID identifying the data
         * @return Object representing I2C data
         */
        DataSignal fromId(byte numBytes, byte id);
    }

    /**
     * Builder to construct common parameters for an SPI read/write operation.  All of these parameters
     * must be set before starting the operation
     * @author Eric Tsai
     */
    interface ParameterBuilder {
        /**
         * Pin for slave select
         * @param pin    Pin value
         * @return Calling object
         */
        ParameterBuilder slaveSelectPin(byte pin);
        /**
         * Pin for serial clock
         * @param pin     Pin value
         * @return Calling object
         */
        ParameterBuilder clockPin(byte pin);
        /**
         * Pin for master output, slave input
         * @param pin    Pin value
         * @return Calling object
         */
        ParameterBuilder mosiPin(byte pin);
        /**
         * Pin for master input, slave output
         * @param pin    Pin value
         * @return Calling object
         */
        ParameterBuilder misoPin(byte pin);
        /**
         * Call to have LSB sent first
         * @return Calling object
         */
        ParameterBuilder lsbFirst();
        /**
         * SPI operating mode, see <a href="https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus#Mode_numbers">SPI Wiki Page</a>
         * for details on the mode values
         * @param mode    Value between [0, 3]
         * @return Calling object
         */
        ParameterBuilder mode(byte mode);
        /**
         * SPI operating frequency
         * @param freq    Operating frequency
         * @return Calling object
         */
        ParameterBuilder frequency(Frequency freq);
        /**
         * Call to use the nRF pin mappings rather than the GPIO pin mappins
         * @return Calling object
         */
        ParameterBuilder useNativePins();
        /**
         * Commit the parameters
         */
        void commit();
    }

    /**
     * Support SPI frequencies
     * @author Eric Tsai
     */
    enum Frequency {
        FREQ_125_KHZ,
        FREQ_250_KHZ,
        FREQ_500_KHZ,
        FREQ_1_MHZ,
        FREQ_2_MHZ,
        FREQ_4_MHZ,
        FREQ_8_MHZ
    }

    /**
     * Writes data to a device through the SPI bus
     * @param data    Data to send
     * @return Builder to set additional parameters
     */
    ParameterBuilder writeData(byte[] data);
    /**
     * Reads data from a device through the SPI bus
     * @param numBytes  Number of bytes to read
     * @param id        User ID identifying the read
     * @param data      Data to write on the bus before reading, null if nothing to write
     * @return Builder to set additional parameters
     */
    ParameterBuilder readData(byte numBytes, byte id, byte[] data);
    /**
     * Initiates the creation of a data route
     * @return Selection of available data sources
     */
    SourceSelector routeData();
}
