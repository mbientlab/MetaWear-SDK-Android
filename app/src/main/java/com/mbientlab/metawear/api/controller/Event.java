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

import java.util.Arrays;
import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

/**
 * Controller for the event module
 * @author Eric Tsai
 */
public interface Event extends ModuleController {
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Enable the module */
        EVENT_ENABLE {
            @Override public byte opcode() { return 0x1; }
        },
        /** Add a command to */
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
        /** Set the bytes of the command */
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
        /** Remove a command entry */
        REMOVE_ENTRY {
            @Override public byte opcode() { return 0x4; }
        },
        REMOVE_ALL_ENTRIES {
            @Override public byte opcode() { return 0x5; }
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
    
    /**
     * Callbacks for the event module
     * @author Eric Tsai
     */
    public abstract class Callbacks implements ModuleCallbacks {
        @Override public final Module getModule() { return Module.EVENT; }
        
        /**
         * Called when a command user id is received
         * @param id ID for referring to the specific command 
         */
        public void receivedCommandId(byte id) { }
        /**
         * Called when command information is received 
         * @param macroCommand Command object describing the command
         */
        public void receivedCommandInfo(Command macroCommand) { }
        /**
         * Called when the bytes have been received
         * @param commandBytes Bytes of the command to be executed
         */
        public void receivedCommandBytes(byte[] commandBytes) { }
    }
    /**
     * Wrapper class encapsulating command information
     * @author Eric Tsai
     */
    public interface Command {
        /** Register that will trigger the event */
        public com.mbientlab.metawear.api.Register srcRegister();
        /** Register index that will trigger the event */
        public byte srcIndex();
        /** Register that the event will trigger */
        public com.mbientlab.metawear.api.Register destRegister();
    }
    
    /**
     * Enable the event module
     */
    public void enableModule();
    /**
     * Disable the event module
     */
    public void disableModule();
    /**
     * Read the attributes of a command id.  When the data has been received, 
     * the {@link Callbacks#receivedCommandInfo(Event.Command)} callback function will be called
     * @param commandId ID of the command to lookup
     */
    public void commandIdToObject(byte commandId);
    /**
     * Read the bytes representing the command to be executed.  When the data has 
     * been received, the {@link Callbacks#receivedCommandBytes(byte[])} callback 
     * function will be called
     * @param commandId ID of the command to lookup
     */
    public void readCommandBytes(byte commandId);
    
    /**
     * Record a sequence of commands, to be executed when there is activity 
     * the given register.  Each command recorded will have its own id, which 
     * will be passed back via the {@link Callbacks#receivedCommandId(byte)} function.  
     * This version of the function is for registers that do not require an 
     * additional index (i.e. GPIO pin, NeoPixel strand, filter id) 
     * @param srcReg Register to trigger the event
     */
    public void recordMacro(com.mbientlab.metawear.api.Register srcReg);
    /**
     * Record a sequence of commands to be executed when a notification is sent for 
     * the particular register and index.  Each command recorded will have its own id, 
     * which will be passed back via the {@link Callbacks#receivedCommandId(byte)} function
     * @param srcReg Register to trigger the event
     * @param index Register index to trigger the event
     */
    public void recordMacro(com.mbientlab.metawear.api.Register srcReg, byte index);
    /**
     * Experimental function for the Event module that can use variables in place of hardcoded 
     * values for a recorded command.  Currently, this method is for simply recording one 
     * command.  The function is still WIP and will be further refined in later releases.
     * @param srcReg Register to trigger the event
     * @param index Register index to trigger the event
     * @param extra Extra data to define the variable parameters 
     */
    public void recordCommand(com.mbientlab.metawear.api.Register srcReg, byte index, byte[] extra);
    
    /**
     * Stop the macro recording
     * @return Number of commands
     */
    public byte stopRecord();
    /**
     * Remove all recorded macros
     */
    public void removeMacros();
    /**
     * Remove the command from the event handler
     * @param commandId Id of the command to remove
     */
    public void removeCommand(byte commandId);
}
