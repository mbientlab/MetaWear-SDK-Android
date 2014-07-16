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
 * PROVIDED “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, 
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

import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.Register;

/**
 * Helper functions that operate on a Register object.
 * @author Eric Tsai
 */
public class Registers {
    /**
     * Builds an array of bytes corresponding to reading a register value.  The output 
     * of this function needs to be written to the Command characteristic to retrieve 
     * the value of a register.
     * @param register Register to read from
     * @param parameters Additional parameters for the read command e.g. a pixel index 
     * or a io pin number
     * @return Array of bytes to read a register value
     * @see com.mbientlab.metawear.api.characteristic.MetaWear#COMMAND
     */
    public static byte[] buildReadCommand(Register register, byte ... parameters) {
        return buildCommand(register.module(), (byte)(0x80 | register.opcode()), parameters);
    }
    /**
     * Builds an array of bytes corresponding to writing a register value.  The output of 
     * this function needs to be written to the Command characteristic to set the value 
     * of a register.
     * @param register Register to write to
     * @param parameters Additional parameters for the write command e.g. LED color or 
     * a disable/enable bit
     * @return Array of bytes to write a register value
     * @see com.mbientlab.metawear.api.characteristic.MetaWear#COMMAND
     */
    public static byte[] buildWriteCommand(Register register, byte ... parameters) {
        return buildCommand(register.module(), register.opcode(), parameters);
    }
    private static byte[] buildCommand(Module module, byte registerOpcode, byte ... parameters) {
        // TODO Auto-generated method stub
        byte[] command= new byte[parameters.length + 2]; 
        
        command[0]= module.opcode;
        command[1]= registerOpcode;
        int index= 2;
        for(byte it: parameters) {
            command[index]= it;
            index++;
        }
        
        return command;
    }
}
