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

import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;

import static com.mbientlab.metawear.api.Module.MACRO;

/**
 * Controller for the macro module
 * @author Eric Tsai
 */
public interface Macro extends ModuleController {
    /**
     * Enumeration of registers under the macro module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Enable / Disable the module */
        ENABLE {
            @Override public byte opcode() { return 0x1; }
        },
        /** Start point for adding a macro */
        ADD_MACRO {
            @Override public byte opcode() { return 0x2; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                if ((data[1] & 0x80) == 0x80) {
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedMacroInfo(data[2], data[3] == 0x1, data[4]);
                    }
                } else {
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedMacroId(data[2]);
                    }
                }
            }
        },
        /** Adds a command under the current macro */
        ADD_COMMAND {
            @Override public byte opcode() { return 0x3; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte[] macroCommand= new byte[data.length - 2];
                System.arraycopy(data, 2, macroCommand, 0, macroCommand.length);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedMacroCommand(macroCommand);
                }
            }
        },
        /** End point for adding a macro */
        END_MACRO {
            @Override public byte opcode() { return 0x4; }
        },
        /** Executes a macro */
        EXEC_MACRO {
            @Override public byte opcode() { return 0x5; }
        },
        /** Enables / Disables completed notifications */
        MACRO_NOTIFY_ENABLE {
            @Override public byte opcode() { return 0x6; }
        },
        /** Signals  a macro has finished */
        MACRO_NOTIFY {
            @Override public byte opcode() { return 0x7; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).macroFinished(data[2]);
                }
            }
        },
        /** Erases all the stored macros */
        ERASE_ALL {
            @Override public byte opcode() { return 0x8; }
        },
        /** For commands that are longer than 18 bytes */
        PARTIAL_COMMAND {
            @Override public byte opcode() { return 0x9; }
        };
        
        @Override public Module module() { return MACRO; }
        @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) { }
    }
    /**
     * Callbacks for the Macro module
     * @author Eric Tsai
     */
    public static abstract class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return MACRO; }
        
        /**
         * Called when the macro ID has been received
         * @param id Numerical id referring to the programmed macro
         */
        public void receivedMacroId(byte id) { }
        /**
         * Called when information about a macro has been received
         * @param id Macro ID the info is about
         * @param execOnBoot True if the macro will be executed at boot time
         * @param numCommands Number of commands the macro will execute
         */
        public void receivedMacroInfo(byte id, boolean execOnBoot, byte numCommands) { }
        /**
         * Called when the bytes of a MetaWear command have been received
         * @param command Byte representation of the command
         */
        public void receivedMacroCommand(byte[] command) { }
        /**
         * Called when a macro has finished execution
         * @param id Numerical ID of the macro that finished executing
         */
        public void macroFinished(byte id) { }
    }
    /**
     * Enable the macro module
     */
    public void enableMacros();
    /**
     * Disable the macro module
     */
    public void disableMacros();
    
    /**
     * Record a sequence of commands to be executed.  Calling this function will put 
     * the APi in "record macro" state.  All functions called will instead be stored 
     * on board, to be executed at a later time.  The {@link Callbacks#receivedMacroId(byte)} 
     * callback function will be executed with the unique ID of this macro.
     * @param executeOnBoot True if the macro should be run at boot time
     */
    public void recordMacro(boolean executeOnBoot);
    /**
     * Stop recording the macro
     */
    public void stopRecord();
    /**
     * Retrieves information about the macro.  When data is available, the 
     * {@link Callbacks#receivedMacroInfo(byte, boolean, byte)} callback function will 
     * be called
     * @param macroId Macro ID to lookup
     */
    public void readMacroInfo(byte macroId);
    /**
     * Retrieves the byte representation of the command.  When data is available, 
     * the {@link Callbacks#receivedMacroCommand(byte[])} callback function will be 
     * called
     * @param macroId Macro ID to lookup
     * @param commandNum Command number to lookup
     */
    public void readMacroCommand(byte macroId, byte commandNum);
    /**
     * Execute a macro
     * @param macroId Macro ID to execute
     */
    public void executeMacro(byte macroId);
    /**
     * Enables a call to the {@link Callbacks#macroFinished(byte)} callback function which 
     * signifies the macro has finished executing
     * @param macroId Macro ID to receive a notification from
     */
    public void enableProgressNotfiy(byte macroId);
    /**
     * Disable the completed notification.
     * @param macroId Macro ID to disable notifications from
     */
    public void disableProgressNotfiy(byte macroId);
    /**
     * Erase all macros from the board.  The erase operation will not be  performed 
     * until you disconnect from the board.  If you wish to reset the board after 
     * the erase operation, use the {@link Debug#resetAfterGarbageCollect()} method 
     */
    public void eraseMacros();
}
