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

package com.mbientlab.metawear.impl.platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import bolts.Task;

/**
 * IO operations used by the API, must be implemented by the target platform to use the API.
 * @author Eric Tsai
 */
public interface IO {
    /**
     * Save the data to the local device
     * @param key     Key value identifying the data
     * @param data    Data to save
     * @throws IOException If I/O error occurs
     */
    void localSave(String key, byte[] data) throws IOException;
    /**
     * Retrieves data saved locally to the device
     * @param key    Key value identifying the data
     * @return Stream to read the data
     * @throws IOException If I/O error occurs
     */
    InputStream localRetrieve(String key) throws IOException;

    /**
     * Downloads a file from a URL and stores it locally on the device.  When downloaded, the file
     * can be later retrieved using {@link #findDownloadedFile(String)}.
     * @param srcUrl    URL to retrieve the file from
     * @param dest      Where to store the downloaded file
     * @return Task holding the downloaded file, if successful
     */
    Task<File> downloadFileAsync(String srcUrl, String dest);
    /**
     * Finds a downloaded file matching the name.  Before using the returned object, check if the
     * file exists by calling {@link File#exists()}
     * @param filename    File to search for
     * @return File object representing the desired file
     */
    File findDownloadedFile(String filename);

    /**
     * Outputs a warn level message to the logger
     * @param tag        Value identifying the message
     * @param message    Message to log
     */
    void logWarn(String tag, String message);
    /**
     * Outputs a warn level message to the logger with an exception or error associated with the message
     * @param tag        Value identifying the message
     * @param message    Message to log
     * @param tr         Additional information to provide to the logger
     */
    void logWarn(String tag, String message, Throwable tr);
}
