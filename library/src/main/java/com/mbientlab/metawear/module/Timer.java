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
import com.mbientlab.metawear.MetaWearBoard;

/**
 * On-board timer for scheduling MetaWear commands
 * @author Eric Tsai
 */
public interface Timer extends MetaWearBoard.Module {
    /**
     * Task that can be scheduled for periodic execution
     * @author Eric Tsai
     */
    interface Task {
        /**
         * MetaWear commands to be executed
         */
        void commands();
    }

    /**
     * Timer controller for managing the task execution
     */
    interface Controller {
        /**
         * Retrieve the ID representing the controller
         * @return Controller ID
         */
        byte id();

        /**
         * Starts the periodic execution, does nothing if controller is inactive
         */
        void start();

        /**
         * Stops the task execution, does nothing if controller is inactive
         */
        void stop();

        /**
         * Removes the timer from the board, does nothing if controller is inactive
         */
        void remove();

        /**
         * Retrieves the active state
         * @return True if the controller is active, false otherwise
         */
        boolean isActive();
    }

    /**
     * Schedules a task to be periodically executed indefinitely
     * @param mwTask    Task to be schedule
     * @param period    How often to execute the task, in milliseconds
     * @param delay     True if the first execution should be delayed by one {@code period} worth of time
     * @return Timer controller, available when the timer has successfully committed
     */
    AsyncOperation<Controller> scheduleTask(Task mwTask, int period, boolean delay);
    /**
     * Schedules a task to be periodically executed for a fixed number of times
     * @param mwTask    Task to be schedule
     * @param period    How often to execute the task, in milliseconds
     * @param delay     True if the first execution should be delayed by one {@code period} worth of time
     * @return Timer controller, available when the timer has successfully committed
     */
    AsyncOperation<Controller> scheduleTask(Task mwTask, int period, boolean delay, short repetitions);

    /**
     * Removed all timers from the board.  All timer controllers will be marked inactive
     */
    void removeTimers();

    /**
     * Retrieve the controller corresponding to the ID
     * @param controllerId    Controller ID to lookup
     * @return Controller corresponding to the ID, null if the lookup failed
     */
    Controller getController(byte controllerId);
}
