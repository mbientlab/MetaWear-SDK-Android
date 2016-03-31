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

import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.MetaWearBoard;

/**
 * Created by etsai on 10/21/2015.
 */
public interface Conductance extends MetaWearBoard.Module {
    interface SourceSelector {
        /**
         * Handles conductance data from calls to {@link #readConductance(byte, boolean)}
         * @param silent    Same value as the silent parameter for calling {@link #readConductance(byte, boolean)}
         * @return Object representing conductance data from the channel
         */
        DataSignal fromChannel(byte channel, boolean silent);
    }

    /**
     * Read the conductance value from a GSR channel
     * @param channel    Channel to read from
     */
    void readConductance(byte channel);
    /**
     * Read the conductance value from a GSR channel
     * @param channel    Channel to read from
     * @param silent     True if the read should be silent
     */
    void readConductance(byte channel, boolean silent);

    /**
     * Initiates automatic calibration.  This should be done before the first conductance read or
     * if there are changes in temperature
     */
    void calibrate();

    /**
     * Initiates the creation of a route for conductance data.  This version defaults to non-silent reads.
     * @param channel    GSR channel to route data for
     * @return Object representing the data from the specific channel
     */
    DataSignal routeData(byte channel);

    /**
     * Initiates the creation of a route for conductance data
     * @return Selection of available data sources
     */
    SourceSelector routeData();
}
