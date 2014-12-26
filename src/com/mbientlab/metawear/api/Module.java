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
package com.mbientlab.metawear.api;

import java.util.HashMap;

import com.mbientlab.metawear.api.controller.*;

/**
 * Enumeration of modules on the MetaWear board.  A module can be thought of as a supported 
 * service by the MetaWear board.  Each module is designed for a specific purpose and 
 * communicates with the user via a collection of registers.
 * @author Eric Tsai
 * @see Register
 */
public enum Module {
    /** Miniature push button switch */
    MECHANICAL_SWITCH((byte)0x01, MechanicalSwitch.Register.values()),
    /** Ultra-Bright RGB LED */
    LED((byte)0x2, com.mbientlab.metawear.api.controller.LED.Register.values()),
    /** 3-axis accelerometer */
    ACCELEROMETER((byte)0x3, Accelerometer.Register.values()),
    /** Temperature sensor */
    TEMPERATURE((byte)0x4, Temperature.Register.values()),
    /** General purpose I/O*/
    GPIO((byte)0x5, com.mbientlab.metawear.api.controller.GPIO.Register.values()),
    /** Neo pixel */
    NEO_PIXEL((byte)0x6, NeoPixel.Register.values()),
    /** IBeacon  */
    IBEACON((byte)0x7, IBeacon.Register.values()),
    HAPTIC((byte)0x8, Haptic.Register.values()),
    DATA_PROCESSOR((byte) 0x9, DataProcessor.Register.values()),
    EVENT((byte)0xa, Event.Register.values()),
    LOGGING((byte)0xb, Logging.Register.values()),
    /** Debug mode for testing purposes */
    DEBUG((byte)0xfe, Debug.Register.values());
    
    /** Opcode of the module */
    public final byte opcode;
    private final HashMap<Byte, Register> registers;
    
    private Module(byte opcode, Register[] registers) {
        this.opcode= opcode;
        this.registers= new HashMap<>();
        
        for(Register it: registers) {
            if (this.registers.containsKey(it.opcode())) {
                throw new RuntimeException(String.format("Duplicate opcpode found (opcpode = %d)", 
                        it.opcode()));
            }
            this.registers.put(it.opcode(), it);
        }
    }
    
    /**
     * Find the register belonging to the module with the specific opcode
     * @param opcode Register opcode to search
     * @return The register with the matching opcode, or null if no match is found
     */
    public Register lookupRegister(byte opcode) {
        byte regCode= (byte) (0x7f & opcode);
        return registers.get(regCode);
    }
    
    private static final HashMap<Byte, Module> opcodeMap;
    static {
        opcodeMap= new HashMap<>();
        for(Module it: values()) {
            if (opcodeMap.containsKey(it.opcode)) {
                throw new RuntimeException(String.format("Duplicate opcpode found for module '%s' and '%s'", 
                        opcodeMap.get(it.opcode).name(), it.name()));
            }
            opcodeMap.put(it.opcode, it);
        }
    }
    /**
     * Find the Module with the opcode
     * @param opcode Module opcode to search
     * @return Module with the matching opcode, or null if no match is found
     */
    public static Module lookupModule(byte opcode) {
        return opcodeMap.get(opcode);
    }
}
