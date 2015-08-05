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

import com.mbientlab.metawear.module.DataProcessor;

import java.util.Map;

/**
 * Represents data from a data producer (sensor or on-board data processor) and defines what to do with the data
 * @author Eric Tsai
 */
public interface DataSignal {
    /**
     * Splits a signal, allowing multiple processors to be attached to the same data.
     * @return Calling object
     */
    DataSignal split();

    /**
     * Starts a new branch on the most recent split signal.
     * @return Most recent signal that was split
     */
    DataSignal branch();

    /**
     * Signifies an end the current branch and reverts control back to the previous split signal.  If no
     * previously split signals are available, the route must be committed.
     * @return Previous split signal if available
     */
    DataSignal end();

    /**
     * Token representing sensor/data processor output.  The token is used for feedback or feedforward loops
     * where the current data is used in place of a fixed value.  For example, you can use this feature to have
     * a dynamic comparison filter on sensor data as opposed to always comparing against a fixed reference.
     * @author Eric Tsai
     * @see com.mbientlab.metawear.processor.Maths
     * @see com.mbientlab.metawear.processor.Comparison
     */
    interface DataToken {
        byte length();
        byte offset();
    }

    /**
     * Reacts to data being emitted from a producer i.e. when the data signal is active
     * @author Eric Tsai
     */
    interface ActivityHandler {
        /**
         * Executes MetaWear commands when the signal is active
         * @param processors    Processors added with an identifying key from previously added routes
         *                      and the current route
         * @param token         Object representing the signal data
         */
        void onSignalActive(Map<String, DataProcessor> processors, DataToken token);
    }

    /**
     * Logs the data from the most recent data processor or sensor.  You must attach a MessageHandler
     * to the logger before initiating a log download
     * @param key    Unique key identifying this logger
     * @return Calling object
     * @see RouteManager#setLogMessageHandler(String, RouteManager.MessageHandler)
     */
    DataSignal log(String key);
    /**
     * Creates a data stream that will broadcast data live from the most recent producer to your mobile device.
     * @param key    Unique key identifying the stream
     * @return Calling object
     * @see RouteManager#subscribe(String, RouteManager.MessageHandler)
     * @see RouteManager#unsubscribe(String)
     */
    DataSignal stream(String key);

    /**
     * Attaches a monitor to the most recent data producer that watches for data activity
     * @param handler    Handler to deal with when the signal is active
     * @return Calling object
     */
    DataSignal monitor(ActivityHandler handler);

    /**
     * Base class representing processor configurations
     * @author Eric Tsai
     */
    interface ProcessorConfig {}
    /**
     * Attaches a data processor to the signal to filter or transform the data.  This function is for
     * processors that are part of a feedback or feedforward loop.
     * @param key       Unique key identifying the processor
     * @param config    Token holding the processor configuration
     * @return Calling object
     */
    DataSignal process(String key, ProcessorConfig config);
    /**
     * Attaches a data processor to the signal to filter to transform the data.
     * @param config    Token holding the processor configuration
     * @return Calling object
     */
    DataSignal process(ProcessorConfig config);
    /**
     * Attaches a data processor to the signal to filter or transform the data.
     * @param configUri    String URI specifying a processor configuration
     * @return Calling object
     */
    DataSignal process(String configUri);
    /**
     * Attaches a data processor to the signal to filter or transform the data.  This function is for
     * processors that are part of a feedback or feedforward loop.
     * @param key          Unique key identifying the processor
     * @param configUri    String URI specifying a processor configuration
     * @return Calling object
     */
    DataSignal process(String key, String configUri);

    /**
     * Writes the data route to the board.
     * @return RouteManager that will be available if the route is successfully written
     */
    AsyncOperation<RouteManager> commit();

}
