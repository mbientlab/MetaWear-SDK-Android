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
 * Created by etsai on 10/3/16.
 */

public interface SerialPassthrough extends Module {
    interface I2c extends ForcedDataProducer {
        /**
         * Read data via the I2C bus.  This version of the function uses the data route system.  The user ID
         * and data length (numBytes parameters) must match their respective parameters in the SourceSelector.fromId
         * function
         * @param deviceAddr      Device to read from
         * @param registerAddr    Device's register to read
         */
        void read(byte deviceAddr, byte registerAddr);
    }

    /**
     * Support SPI frequencies
     * @author Eric Tsai
     */
    enum SpiFrequency {
        FREQ_125_KHZ,
        FREQ_250_KHZ,
        FREQ_500_KHZ,
        FREQ_1_MHZ,
        FREQ_2_MHZ,
        FREQ_4_MHZ,
        FREQ_8_MHZ
    }

    /**
     * Builder to construct common parameters for an SPI read/write operation.  All of these parameters
     * must be set before starting the operation
     * @author Eric Tsai
     */
    interface SpiParameterBuilder {
        /**
         * Pin for slave select
         * @param pin    Pin value
         * @return Calling object
         */
        SpiParameterBuilder slaveSelectPin(byte pin);
        /**
         * Pin for serial clock
         * @param pin     Pin value
         * @return Calling object
         */
        SpiParameterBuilder clockPin(byte pin);
        /**
         * Pin for master output, slave input
         * @param pin    Pin value
         * @return Calling object
         */
        SpiParameterBuilder mosiPin(byte pin);
        /**
         * Pin for master input, slave output
         * @param pin    Pin value
         * @return Calling object
         */
        SpiParameterBuilder misoPin(byte pin);
        /**
         * Call to have LSB sent first
         * @return Calling object
         */
        SpiParameterBuilder lsbFirst();
        /**
         * SPI operating mode, see <a href="https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus#Mode_numbers">SPI Wiki Page</a>
         * for details on the mode values
         * @param mode    Value between [0, 3]
         * @return Calling object
         */
        SpiParameterBuilder mode(byte mode);
        /**
         * SPI operating frequency
         * @param freq    Operating frequency
         * @return Calling object
         */
        SpiParameterBuilder frequency(SpiFrequency freq);
        /**
         * Call to use the nRF pin mappings rather than the GPIO pin mappings
         * @return Calling object
         */
        SpiParameterBuilder useNativePins();
        /**
         * Commit the parameters
         */
        void commit();
    }

    interface Spi extends ForcedDataProducer {
        /**
         * Reads data from a device through the SPI bus
         * @param data      DataToken to write on the bus before reading, null if nothing to write
         * @return Builder to set additional parameters
         */
        SpiParameterBuilder read(byte[] data);
    }

    I2c i2cData(byte length, byte id);
    /**
     * Write data via the I2C bus.
     * @param deviceAddr Device to write to
     * @param registerAddr Device's register to write to
     * @param data DataToken to write, up to 10 bytes
     */
    void writeI2c(byte deviceAddr, byte registerAddr, byte[] data);

    Spi spiData(byte length, byte id);
    /**
     * Writes data to a device through the SPI bus
     * @param data    DataToken to send
     * @return Builder to set additional parameters
     */
    SpiParameterBuilder writeSpi(byte[] data);
}
