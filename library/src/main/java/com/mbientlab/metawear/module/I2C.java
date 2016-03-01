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

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.MetaWearBoard;

/**
 * Communicates with the I2C bus, providing direct access sensors connected via I2C
 * @author Eric Tsai
 */
public interface I2C extends MetaWearBoard.Module {
    /**
     * Selector for the I2C data sources
     * @author Eric Tsai
     */
    interface SourceSelector {
        /**
         * Handle data from an I2C read.  The parameters for this function must match their respective
         * parameters from the alternate {@link #readData(byte, byte, byte, byte)} function
         * @param numBytes    Number of bytes to expect from the read
         * @param id          User ID identifying the data
         * @return Object representing I2C data
         */
        DataSignal fromId(byte numBytes, byte id);
    }

    /**
     * Write data via the I2C bus without attaching a user id to the data.
     * @param deviceAddr Device to write to
     * @param registerAddr Device's register to write to
     * @param data Data to write, up to 10 bytes
     */
    void writeData(byte deviceAddr, byte registerAddr, byte[] data);

    /**
     * Read data via the I2C bus and stream result to user.  This version of the function does not use
     * the data route system, it only streams data to the user.
     * @param deviceAddr Device to read from
     * @param registerAddr Device's register to read
     * @param numBytes Number of bytes to read
     * @return Byte array containing the register data, available when read operation completes
     */
    AsyncOperation<byte[]> readData(byte deviceAddr, byte registerAddr, byte numBytes);

    /**
     * Read data via the I2C bus.  This version of the function uses the data route system.  The user ID
     * and data length (numBytes parameters) must match their respective parameters in the SourceSelector.fromId
     * function
     * @param deviceAddr      Device to read from
     * @param registerAddr    Device's register to read
     * @param numBytes        User id identifying the data
     * @param id              Number of bytes to read
     * @see com.mbientlab.metawear.module.I2C.SourceSelector#fromId(byte, byte)
     */
    AsyncOperation<Void> readData(byte deviceAddr, byte registerAddr, byte numBytes, byte id);

    /**
     * Initiates the creation of a data route
     * @return Selection of available data sources
     */
    SourceSelector routeData();
}
