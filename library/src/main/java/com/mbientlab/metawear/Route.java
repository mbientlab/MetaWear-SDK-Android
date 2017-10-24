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

/**
 * Defines how data flows from a data producer to an endpoint
 * @author Eric Tsai
 */
public interface Route {
    /**
     * Generates a string identifying the data producer chain the subscriber is receiving data from.
     * This value can be matched with the output from {@link AnonymousRoute#identifier()} if syncing data
     * using the {@link AnonymousRoute} interface.
     * @param pos    Numerical position of the subscriber to interact with, starting from 0
     * @return String identifying the data chain, null if <code>param</code> value is out of bounds
     */
    String generateIdentifier(int pos);
    /**
     * Sets the environment values  passed into the {@link Subscriber#apply(Data, Object...) apply} function
     * @param pos   Numerical position of the subscriber to interact with, starting from 0
     * @param env   Environment values to use with the subscriber
     * @return True if operation succeeded, false otherwise
     */
    boolean setEnvironment(int pos, Object ... env);
    /**
     * Quiets the stream the subscriber is listening to, does nothing if the subscriber is handling log data
     * @param pos   Numerical position of the subscriber to interact with, starting at 0
     * @return True if operation succeeded, false otherwise
     */
    boolean unsubscribe(int pos);
    /**
     * Reactivates the stream the subscriber is listening to.  If the subscriber
     * originally listened to log data, the function only updates the subscriber.
     * @param pos   Numerical position of the subscriber to interact with, starting at 0
     * @return True if operation succeeded, false otherwise
     */
    boolean resubscribe(int pos);
    /**
     * Reactivates the stream the subscriber is listening to and updates the data subscriber.  If the subscriber
     * originally listened to log data, the function only updates the subscriber.
     * @param pos   Numerical position of the subscriber to interact with, starting at 0
     * @param subscriber    New subscriber to handle the received data
     * @return True if operation succeeded, false otherwise
     */
    boolean resubscribe(int pos, Subscriber subscriber);

    /**
     * Removes the route and marks the object as inactive
     */
    void remove();
    /**
     *  Checks  if the route is
     * @return True if route is active
     */
    boolean isActive();
    /**
     * Unique value identifying the route.  This value can be used with {@link MetaWearBoard#lookupRoute(int)} lookupRoute}
     * to retrieve the current object
     * @return Numerical ID identifying the route
     */
    int id();
}
