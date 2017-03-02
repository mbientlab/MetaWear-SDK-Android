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

import com.mbientlab.metawear.DataProducer;
import com.mbientlab.metawear.MetaWearBoard.Module;

import bolts.Task;

/**
 * Bridge for serial communication to connected sensors
 * @author Eric Tsai
 */
public interface SerialPassthrough extends Module {
    /**
     * Data received from the I2C bus
     * @author Eric Tsai
     */
    interface I2C extends DataProducer {
        /**
         * Read data via the I2C bus
         * @param deviceAddr      Device to read from
         * @param registerAddr    Device's register to read
         */
        void read(byte deviceAddr, byte registerAddr);
    }

    /**
     * Supported SPI frequencies
     * @author Eric Tsai
     */
    enum SpiFrequency {
        /** 125 KHz */
        FREQ_125_KHZ,
        /** 250 KHz */
        FREQ_250_KHZ,
        /** 500 KHz */
        FREQ_500_KHZ,
        /** 1 MHz */
        FREQ_1_MHZ,
        /** 2 MHz */
        FREQ_2_MHZ,
        /** 4 MHz */
        FREQ_4_MHZ,
        /** 8 MHz */
        FREQ_8_MHZ
    }
    /**
     * Builder to construct common parameters for an SPI read/write operation.  The {@link #lsbFirst()}
     * and {@link #useNativePins()} parameters are optional and default to false if not set; the {@link #data(byte[])}
     * parameter is also optional for a read operation but required for writes.  All other parameters are required.
     * @param <T>    Return type of the commit function
     * @author Eric Tsai
     */
    interface SpiParameterBuilder<T> {
        /**
         * Data to write to the sensor.  If used with a read operation, the data will be written prior to the read
         * @param data    Data to write
         * @return Calling object
         */
        SpiParameterBuilder<T> data(byte[] data);
        /**
         * Pin for slave select
         * @param pin    Pin value
         * @return Calling object
         */
        SpiParameterBuilder<T> slaveSelectPin(byte pin);
        /**
         * Pin for serial clock
         * @param pin     Pin value
         * @return Calling object
         */
        SpiParameterBuilder<T> clockPin(byte pin);
        /**
         * Pin for master output, slave input
         * @param pin    Pin value
         * @return Calling object
         */
        SpiParameterBuilder<T> mosiPin(byte pin);
        /**
         * Pin for master input, slave output
         * @param pin    Pin value
         * @return Calling object
         */
        SpiParameterBuilder<T> misoPin(byte pin);
        /**
         * Call to have LSB sent first
         * @return Calling object
         */
        SpiParameterBuilder<T> lsbFirst();
        /**
         * SPI operating mode, see <a href="https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus#Mode_numbers">SPI Wiki Page</a>
         * for details on the mode values
         * @param mode    Value between [0, 3]
         * @return Calling object
         */
        SpiParameterBuilder<T> mode(byte mode);
        /**
         * SPI operating frequency
         * @param freq    Operating frequency
         * @return Calling object
         */
        SpiParameterBuilder<T> frequency(SpiFrequency freq);
        /**
         * Call to use the nRF pin mappings rather than the GPIO pin mappings
         * @return Calling object
         */
        SpiParameterBuilder<T> useNativePins();
        /**
         * Commit the parameters
         */
        T commit();
    }
    /**
     * Data received from the SPI bus
     * @author Eric Tsai
     */
    interface SPI extends DataProducer {
        /**
         * Reads data from a device through the SPI bus
         * @return Builder to set additional parameters
         */
        SpiParameterBuilder<Void> read();
    }

    /**
     * Get an object representing the I2C data corresponding to the id.  If the id value cannot be matched
     * with an existing object, the API will create a new object using the {@code length} parameter otherwise
     * the existing object will be returned
     * @param length    Expected length of the data
     * @param id        Value between [0, 254]
     * @return I2C object representing I2C data
     */
    I2C i2c(byte length, byte id);
    /**
     * Write data to a sensor via the I2C bus.
     * @param deviceAddr Device to write to
     * @param registerAddr Device's register to write to
     * @param data DataToken to write, up to 10 bytes
     */
    void writeI2c(byte deviceAddr, byte registerAddr, byte[] data);
    /**
     * Read data from a sensor via the I2C bus.  Unlike {@link I2C#read(byte, byte)}, this function provides
     * a direct way to access I2C data as opposed to creating a data route.
     * @param deviceAddr      Address of the slave device
     * @param registerAddr    Register on the slave device to access
     * @param length          How many bytes to read
     * @return Task holding the returned value
     */
    Task<byte[]> readI2cAsync(byte deviceAddr, byte registerAddr, byte length);

    /**
     * Get an object representing the SPI data corresponding to the id.  If the id value cannot be matched
     * with an existing object, the API will create a new object using the {@code length} parameter otherwise
     * the existing object will be returned
     * @param length    Expected length of the data
     * @param id        Value between [0, 14]
     * @return SPI object representing SPI data, null if the SPI bus is not accessible with the current firmware
     */
    SPI spi(byte length, byte id);
    /**
     * Write data to a sensor via the SPI bus.  The data to be written to the board is set with the
     * {@link SpiParameterBuilder#data(byte[])} method
     * @return Builder to set additional parameters, null if the SPI bus is not accessible with the current firmware
     */
    SpiParameterBuilder<Void> writeSpi();
    /**
     * Read data from a sensor via the SPI bus.  Unlike {@link SPI#read()}, this function provides a direct
     * way to access SPI data as opposed to creating a data route.  If the SPI bus is not accessible with
     * the current firmware, the operation will fail with an {@link UnsupportedOperationException}.
     * @param length    How many bytes to read
     * @return Editor object to configure the read parameters
     */
    SpiParameterBuilder<Task<byte[]>> readSpiAsync(byte length);
}
