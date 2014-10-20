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

package com.mbientlab.metawear.api.controller;

import java.util.Arrays;
import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

/**
 * @author etsai
 *
 */
public interface Event extends ModuleController {
    public enum Register implements com.mbientlab.metawear.api.Register {
        EVENT_ENABLE {
            @Override public byte opcode() { return 0x1; }
        },
        ADD_ENTRY {
            @Override public byte opcode() { return 0x2; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    final byte[] data) {
                if ((data[1] & 0x80) == 0x80) {
                    Command macroCmd= new Command() {
                        @Override
                        public com.mbientlab.metawear.api.Register srcRegister() {
                            return Module.lookupModule(data[2]).lookupRegister(data[3]);
                        }

                        @Override
                        public com.mbientlab.metawear.api.Register destRegister() {
                            return Module.lookupModule(data[5]).lookupRegister(data[6]);
                        }

                        @Override
                        public byte srcIndex() {
                            return data[4];
                        }
                    };
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedCommandInfo(macroCmd);
                    }
                } else {
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedCommandId(data[2]);
                    }
                }
            }
        },
        EVENT_COMMAND {
            @Override public byte opcode() { return 0x3; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte[] commandBytes= Arrays.copyOfRange(data, 2, data.length);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedCommandBytes(commandBytes);
                }
            }
        },
        REMOVE_ENTRY {
            @Override public byte opcode() { return 0x4; }
        };

        /* (non-Javadoc)
         * @see com.mbientlab.metawear.api.Register#module()
         */
        @Override
        public Module module() { return Module.EVENT; }

        /* (non-Javadoc)
         * @see com.mbientlab.metawear.api.Register#notifyCallbacks(java.util.Collection, byte[])
         */
        @Override
        public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) { }
        
    }
    
    public abstract class Callbacks implements ModuleCallbacks {
        @Override public final Module getModule() { return Module.EVENT; }
        
        public void receivedCommandId(byte id) { }
        public void receivedCommandInfo(Command macroCommand) { }
        public void receivedCommandBytes(byte[] commandBytes) { }
    }
    
    public interface Command {
        public com.mbientlab.metawear.api.Register srcRegister();
        public byte srcIndex();
        public com.mbientlab.metawear.api.Register destRegister();
    }
    
    public void readCommandInfo(byte commandId);
    public void readCommandBytes(byte commandId);
    
    public void recordMacro(com.mbientlab.metawear.api.Register srcReg);
    public byte stopRecord();
    public void resetMacros();
}
