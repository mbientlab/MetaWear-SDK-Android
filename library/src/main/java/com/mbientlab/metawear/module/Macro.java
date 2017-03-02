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

import com.mbientlab.metawear.MetaWearBoard.Module;

import bolts.Task;

/**
 * Firmware feature that saves MetaWear commands to the on-board flash memory
 * @author Eric Tsai
 */
public interface Macro extends Module {
    /**
     * Variant of {@link #startRecord(boolean)} with {@code execOnBoot} set to true
     */
    void startRecord();
    /**
     * Begin macro recording.  Every MetaWear command issued will be recorded to the flash memory.
     * @param execOnBoot    True if the commands should be executed when the board powers on
     */
    void startRecord(boolean execOnBoot);
    /**
     * Ends macro recording
     * @return Task containing the id of the recorded task
     */
    Task<Byte> endRecordAsync();

    /**
     * Execute the commands corresponding to the macro ID
     * @param id        Numerical ID of the macro to execute
     */
    void execute(byte id);
    /**
     * Remove all macros on the flash memory.  The erase operation will not be performed until
     * you disconnect from the board.  If you wish to reset the board after the erase operation,
     * use the {@link Debug#resetAfterGc()} method.
     */
    void eraseAll();
}
