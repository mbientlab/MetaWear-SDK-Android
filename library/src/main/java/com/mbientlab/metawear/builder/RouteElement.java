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

package com.mbientlab.metawear.builder;

import com.mbientlab.metawear.DataToken;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.filter.*;
import com.mbientlab.metawear.builder.function.*;
import com.mbientlab.metawear.builder.predicate.*;

/**
 * RouteBuilder component representing an element in a data route
 * @author Eric Tsai
 */
public interface RouteElement {
    /**
     * Similar to a {@link com.mbientlab.metawear.CodeBlock} except this interface is specifically for
     * data producers
     * @author Eric Tsai
     */
    interface Action {
        /**
         * Writes the MetaWear commands to the board to be executed every time new token is created
         * @param token    Token representing the on-board token
         */
        void execute(DataToken token);
    }

    /**
     * Splits the route directing the input data to different end points
     * @return Element for building multicast routes
     */
    RouteMulticast multicast();
    /**
     * Signals the creation of a new multicast branch
     * @return RouteElement from the most recent multicast component
     */
    RouteElement to();
    /**
     * Separates multi=component data into its individual values
     * @return Element for building routes for component data values
     */
    RouteSplit split();
    /**
     * Gets a specific component value from the split data value
     * @param i    Position in the split values array to return
     * @return Object representing the component value
     */
    RouteElement index(int i);
    /**
     * Signifies the end of a split or multicast route.  Control is return to the most recent multicast or split route element
     * @return Most recent multicast or split route
     */
    RouteElement end();

    /**
     * Assigns a user-defined name identifying the data processor.  The key can be used to modify the processor configuration,
     * read its state, and create feedback or feedforward loops with other data processors
     * @param name    Value to name the most recent data processor
     * @return Calling object
     * @see com.mbientlab.metawear.module.DataProcessor#edit(String, Class)
     * @see com.mbientlab.metawear.module.DataProcessor#state(String)
     */
    RouteElement name(String name);
    /**
     * Streams the input data to the Android device
     * @param subscriber    Subscriber to handle the received data
     * @return Calling object
     */
    RouteElement stream(Subscriber subscriber);
    /**
     * Records the input data to the on-board logger, retrieved when a log download is started
     * @param subscriber    Subscriber to handle the received data
     * @return Calling object
     */
    RouteElement log(Subscriber subscriber);
    /**
     * Programs the board to react in response to data being created by the most resent sensor or processor
     * @param action    On-board action to execute
     * @return Calling object
     */
    RouteElement react(Action action);

    /**
     * Stores the input data in memory.  The currently stored value can be extracted by reading he buffer state.
     * As this buffer has no output, the route cannot continue thus the route either ends here or control is
     * passed back to the most recent multicast or splitter
     * @return Object for continuing the route
     */
    RouteElement buffer();

    /**
     * Counts the number of data samples that have passed through this component and outputs the current count
     * @return Object representing the output of the counter
     */
    RouteElement count();
    /**
     * Accumulates a running sum of all data samples passing through this component and outputs the current tally
     * @return Object representing the output of the counter
     */
    RouteElement accumulate();
    /**
     * Computes a moving average over the previous N samples.  This component will not output data until the first average i.e.
     * until N samples have been received.
     * @param samples    Number of samples to average over
     * @return Object representing the output of the averager
     */
    RouteElement average(byte samples);
    /**
     * Stops data from leaving until at least N samples have been collected.
     * @param samples    Number of samples to collect
     * @return Object representing the output of the sample delay component
     */
    RouteElement delay(byte samples);

    /**
     * Apply a 1 input function to all of the input data
     * @param fn    Function to use
     * @return Object representing the output of the mapper
     */
    RouteElement map(Function1 fn);
    /**
     * Apply a 2 input function to all of the input data
     * @param fn    Function to use
     * @param rhs   Second input for the function
     * @return Object representing the output of the mapper
     */
    RouteElement map(Function2 fn, Number rhs);
    /**
     * Variant of {@link #map(Function2, Number)} where the rhs value is the output of another
     * sensor or processor
     * @param fn          Function to apply to the input data
     * @param dataNames   Keys identifying which sensor or processor data to feed into the mapper
     * @return Object representing the output of the mapper
     */
    RouteElement map(Function2 fn, String ... dataNames);

    /**
     * Reduce the amount of data allowed through such that the output data rate matches the period
     * @param period    How often to allow data through, in milliseconds (ms)
     * @return Object representing the output of the limiter
     */
    RouteElement limit(int period);
    /**
     * Only allow data through under certain user controlled conditions
     * @param type     Passthrough operation type
     * @param value    Initial value to set the passthrough limiter to
     * @return Object representing the output of the limiter
     */
    RouteElement limit(Passthrough type, short value);

    /**
     * Scans the input data for a pulse.  When one is detected, output a summary of the scanned data
     * @param type         Type of summary data to output
     * @param threshold    Value the sensor data must exceed for a valid pulse
     * @param samples      Minimum number of samples that must be above the threshold for a valid pulse
     * @return Object representing the output of the pulse finder
     */
    RouteElement find(PulseOutput type, Number threshold, short samples);

    /**
     * Remove data from the route that does not satisfy the comparison
     * @param op            Comparison operation to perform
     * @param references    Reference values to compare against, can be multiple values if the board
     *                      is on firmware v1.2.3 or later
     * @return Object representing the output of the comparator filter
     */
    RouteElement filter(Comparison op, Number ... references);
    /**
     * Variant of the {@link #filter(Comparison, Number...)} function where the reference values are outputs
     * from other sensors or processors
     * @param op          Comparison operation to perform
     * @param dataNames   Names identifying which sensor or processor data to use as the reference value when
     *                    new values are produced
     * @return Object representing the output of the comparator filter
     */
    RouteElement filter(Comparison op, String ... dataNames);
    /**
     * Variant of {@link #filter(Comparison, Number...)} where the filter output can instead output values that
     * provide different details about the comparison.  This function is only available on firmware v1.2.3 or
     * later.  If used with older firmware, the 'type' parameters is ignored.
     * @param op            Comparison operation to perform.
     * @param type          Type of output the filter should produce
     * @param references    Reference values to compare against, can be multiple values if the board
     *                      is on firmware v1.2.3 or later
     * @return Object representing the output of the comparator filter
     */
    RouteElement filter(Comparison op, ComparisonOutput type, Number ... references);
    /**
     * Variant of {@link #filter(Comparison, ComparisonOutput, Number...)} where reference values are outputs
     * from other sensors or processors.
     * @param op          Comparison operation to perform
     * @param type        Output type of the filter
     * @param dataNames   Names identifying which sensor or processor data to use as the reference value when
     *                    new values are produced
     * @return Object representing the output of the comparator filter
     */
    RouteElement filter(Comparison op, ComparisonOutput type, String ... dataNames);
    /**
     * Remove data from the route that doesn't not cross the threshold
     * @param output       Type of output the filter will produce
     * @param threshold    Threshold boundary the data must cross
     * @return Object representing the output of the threshold filter
     */
    RouteElement filter(ThresholdOutput output, Number threshold);
    /**
     * Variant of {@link #filter(ThresholdOutput, Number)} with a configurable hysteresis value for data
     * that frequently oscillates around the threshold boundary
     * @param output       Type of output the filter will produce
     * @param threshold    Threshold boundary the data must cross
     * @param hysteresis   Minimum distance between the boundary and value that indicates a successful crossing
     * @return Object representing the output of the threshold filter
     */
    RouteElement filter(ThresholdOutput output, Number threshold, Number hysteresis);
    /**
     * Removes data that it is not a minimum distance away from a reference value.  The reference value is
     * continually updated to be the previous passing value
     * @param output        Type of output the filter will produce
     * @param difference    Minimum distance from the reference value
     * @return Object representing the output of the differential filter
     */
    RouteElement filter(DifferentialOutput output, Number difference);

}
