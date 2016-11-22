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

import java.util.Calendar;

import bolts.Task;

/**
 * Created by etsai on 9/4/16.
 */
public interface Logging extends Module {
    enum DownloadError {
        UNKNOWN_LOG_ENTRY,
        UNHANDLED_LOG_DATA
    }

    interface LogDownloadUpdateHandler {
        /**
         * Called when a progress update is received from the board
         * @param nEntriesLeft    Number of entries left to download
         * @param totalEntries    Total number of entries
         */
        void receivedUpdate(long nEntriesLeft, long totalEntries);
    }

    interface LogDownloadErrorHandler {
        /**
         * Called when a log entry has been received but cannot be matched to a data logger
         * @param errorType    Type of error received
         * @param logId        Numerical ID of the log entry
         * @param timestamp    Date and time of when the data was recorded
         * @param data         Byte array representation of the sensor data
         */
        void receivedError(DownloadError errorType, byte logId, Calendar timestamp, byte[] data);
    }

    /**
     * Start logging sensor data
     * @param overwrite    True if older entries should be overwritten when the logger is full
     */
    void start(boolean overwrite);
    /**
     * Stop logging sensor data
     */
    void stop();
    /**
     * Download saved data from the flash memory with periodic progress updates and error handling
     * @param nUpdates          How many progress updates to send
     * @param updateHandler     Handler to accept download notifications
     * @param errorHandler      Handler to process encountered errors during the download
     * @return Object holding the result of the task
     */
    Task<Void> download(int nUpdates, LogDownloadUpdateHandler updateHandler, LogDownloadErrorHandler errorHandler);
    /**
     * Download saved data from the flash memory with periodic progress updates
     * @param nUpdates          How many progress updates to send
     * @param updateHandler     Handler to accept download notifications
     * @return Object holding the result of the task
     */
    Task<Void> download(int nUpdates, LogDownloadUpdateHandler updateHandler);
    /**
     * Download saved data from the flash memory with no progress updates
     * @return Object holding the result of the task
     */
    Task<Void> download(LogDownloadErrorHandler errorHandler);
    /**
     * Download saved data from the flash memory with no progress updates nor error handling
     * @return Object holding the result of the task
     */
    Task<Void> download();

    /**
     * Clear all stored logged data from the board.  The erase operation will not be performed until
     * you disconnect from the board.
     */
    void clearEntries();
}
