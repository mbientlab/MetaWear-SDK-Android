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
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;

import java.util.Calendar;

/**
 * Offline logging for sensor data
 * @author Eric Tsai
 */
public interface Logging extends MetaWearBoard.Module {
    /**
     * Processes notifications from the data logger
     * @author Eric Tsai
     */
    abstract class DownloadHandler {
        /**
         * Called when a progress update is received from the board
         * @param nEntriesLeft    Number of entries left to download
         * @param totalEntries    Total number of entries
         */
        public void onProgressUpdate(int nEntriesLeft, int totalEntries) { }

        /**
         * Called when a log entry has been received but cannot be matched to a route logger
         * @param logId        Numerical ID of the log entry
         * @param timestamp    Date and time of when the data was recorded
         * @param data         Byte array representation of the sensor data
         */
        public void receivedUnknownLogEntry(byte logId, Calendar timestamp, byte[] data) { }

        /**
         * Called when a log entry has been received but has no
         * {@link com.mbientlab.metawear.RouteManager.MessageHandler MessageHandler} to process the
         * received entry
         * @param logMessage    Log message received from the board
         */
        public void receivedUnhandledLogEntry(Message logMessage) { }
    }

    /**
     * Start logging sensor data.  This version of the method will not overwrite older entries if
     * the log is full
     */
    void startLogging();
    /**
     * Start logging sensor data
     * @param overwrite    True if older entries should be overwritten when the logger is full
     */
    void startLogging(boolean overwrite);
    /**
     * Stop logging sensor data
     */
    void stopLogging();

    /**
     * Start downloading the recorded sensor data
     * @param notifyProgress    How often to send progress updates, expressed as a fraction between [0, 1]
     *                          where 0= no updates, 0.1= 10 updates, 0.25= 4 updates, etc.
     * @param handler           Handler to use for logger notifications
     * @return Number of log entries to download, reported asynchronously
     */
    AsyncOperation<Integer> downloadLog(float notifyProgress, DownloadHandler handler);

    /**
     * Clear all stored logged data from the board.  The erase operation will not be performed until
     * you disconnect from the board.  If you wish to reset the board after the erase operation,
     * use the {@link Debug#resetAfterGarbageCollect()} method.
     */
    void clearEntries();

    /**
     * Clear a set number of log entries stored on the board.  The erase operation will not be performed until
     * you disconnect from the board.  If you wish to reset the board after the erase operation,
     * use the {@link Debug#resetAfterGarbageCollect()} method.
     */
    void clearEntries(long nEntries);

    /**
     * Retrieves the number of log entries that can be written to the board.
     * @return Number of log entries the board can store
     */
    long getLogCapacity();
}
