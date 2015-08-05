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

package com.mbientlab.metawear;

import java.util.Collection;

/**
 * Manages a data route
 * @author Eric Tsai
 */
public interface RouteManager {
    /**
     * Retrieves the ID
     * @return ID identifying the route manager
     */
    int id();

    /**
     * Removes the underlying route from the board and marks the manager as inactive
     */
    void remove();
    /**
     * Retrieves the active state of the manager.  If the manager is not active, none of the class methods will
     * have any effect.
     * @return True if the manager is still active
     */
    boolean isActive();

    /**
     * Retrieves available subscription keys for this route
     * @return Collection of subscription keys
     */
    Collection<String> getStreamKeys();
    /**
     * Unsubscribes from a data stream.  No changes will be made if the stream key is invalid.
     * @param streamKey    Unique key identifying the stream
     * @return True if the operation was successful
     */
    boolean unsubscribe(String streamKey);

    /**
     * Subscribes to a data stream.  No changes will be made if the stream key is invalid.
     * @param streamKey    Unique key identifying the stream
     * @param processor    Processor to handle the stream data
     * @return True if the operation was successful
     */
    boolean subscribe(String streamKey, MessageHandler processor);

    /**
     * Retrieves available logging keys for this route
     * @return Collection of logging keys
     */
    Collection<String> getLogKeys();

    /**
     * Assigns a handler to process data received from the logger.  No changes will be made if the log key is invalid.
     * @param logKey       Unique key identifying the logger
     * @param processor    Processor to handle the log data
     * @return True if the operation was successful
     */
    boolean setLogMessageHandler(String logKey, MessageHandler processor);
    /**
     * Processes messages received from data producers (i.e. sensors or on-board data processor).  The
     * messages can be emitted live or received from the logger
     * @author Eric Tsai
     */
    interface MessageHandler {
        /**
         * Called when a message has been received
         * @param msg    Message received from the producer
         */
        void process(Message msg);
    }
}
