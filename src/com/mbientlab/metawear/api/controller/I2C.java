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
package com.mbientlab.metawear.api.controller;

import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

import static com.mbientlab.metawear.api.Module.I2C;

/**
 * Controller for the I2C module
 * @author Eric Tsai
 */
public interface I2C extends ModuleController {
    /**
     * Enumeration of registers for the I2C module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Read / Write data through the I2C bus */
        READ_WRITE {
            @Override public byte opcode() { return 0x1; }
            @Override
            public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte[] i2cData= new byte[data.length - 3]; 
                System.arraycopy(data, 3, i2cData, 0, data.length - 3);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedI2CData(data[2], i2cData);
                }
            }
        };
        
        @Override public Module module() { return I2C; }

    }
    
    /**
     * Callbacks for the I2C module
     * @author Eric Tsai
     */
    public abstract static class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return I2C; }
        
        /**
         * Called when data is received from the I2C bus
         * @param index User defined index identifying the data
         * @param data Data received from the bus
         */
        public void receivedI2CData(byte index, byte[] data) { }
    }
    /**
     * Write data via the I2C bus
     * @param deviceAddr Device to write to
     * @param registerAddr Device's register to write to
     * @param index User defined index identifying the data
     * @param data Data to write, up to 10 bytes
     */
    public void writeData(byte deviceAddr, byte registerAddr, byte index, byte[] data);
    /**
     * Write data via the I2C bus without attaching a user id to the data.
     * @param deviceAddr Device to write to
     * @param registerAddr Device's register to write to
     * @param data Data to write, up to 10 bytes
     */
    public void writeData(byte deviceAddr, byte registerAddr, byte[] data);

    /**
     * Read data via the I2C bus.  Data is returned in the {@link Callbacks#receivedI2CData(byte, byte[])} 
     * callback function
     * @param deviceAddr Device to read from
     * @param registerAddr Device's register to read
     * @param index User defined index identifying the data
     * @param numBytes Number of bytes to read
     */
    public void readData(byte deviceAddr, byte registerAddr, byte index, byte numBytes);
    /**
     * Read data via the I2C bus without a user id identifying the read data.  Data is returned in the 
     * {@link Callbacks#receivedI2CData(byte, byte[])} callback function with a user id of 0xff
     * @param deviceAddr Device to read from
     * @param registerAddr Device's register to read
     * @param numBytes Number of bytes to read
     */
    public void readData(byte deviceAddr, byte registerAddr, byte numBytes);
}
