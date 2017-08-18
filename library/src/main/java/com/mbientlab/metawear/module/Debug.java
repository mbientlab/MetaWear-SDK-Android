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

import com.mbientlab.metawear.DataToken;
import com.mbientlab.metawear.builder.RouteComponent.Action;
import com.mbientlab.metawear.CodeBlock;
import com.mbientlab.metawear.MetaWearBoard.Module;

import bolts.Task;

/**
 * Auxiliary functions, for advanced use only
 * @author Eric Tsai
 */
public interface Debug extends Module {
    /**
     * Issues a firmware reset command to the board
     * @return Task that is completed when connection is lost, or cancelled if the function is called
     * within the {@link CodeBlock#program()} or {@link Action#execute(DataToken)} methods
     */
    Task<Void> resetAsync();
    /**
     * Commands the board to terminate the BLE link
     * @return Task that is completed when connection is lost, or cancelled if the function is called
     * within the {@link CodeBlock#program()} or {@link Action#execute(DataToken)} methods
     */
    Task<Void> disconnectAsync();
    /**
     * Restarts the board in MetaBoot mode.  This function must be called in order to update the firmware.
     * @return Task that is completed when connection is lost, or cancelled if the function is called
     * within the {@link CodeBlock#program()} or {@link Action#execute(DataToken)} methods
     */
    Task<Void> jumpToBootloaderAsync();
    /**
     * Tells the board to reset after performing garbage collection.  Use this function in lieu of
     * {@link #resetAsync()} to reset the board after erasing macros or log data.
     */
    void resetAfterGc();

    /**
     * Writes a 4 byte value that persists until a reset, can be later retrieved with {@link #readTmpValueAsync()}
     * @param value    Value to write
     */
    void writeTmpValue(int value);
    /**
     * Reads the temp value written by {@link #writeTmpValue(int)}
     * @return Task that is completed once the value is received
     */
    Task<Integer> readTmpValueAsync();

    /**
     * Places the board in a powered down state after the next reset.  When in power save mode, press the switch to wake the board up.
     * @return True if feature is supported, false if powersave cannot be enabled
     */
    boolean enablePowersave();
}
