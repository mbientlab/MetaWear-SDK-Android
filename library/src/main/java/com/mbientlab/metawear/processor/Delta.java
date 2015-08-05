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

package com.mbientlab.metawear.processor;

import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.module.DataProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the delta data processor
 * @author Eric Tsai
 */
public class Delta implements DataSignal.ProcessorConfig {
    private static final HashMap<String, OutputMode> modeShortNames;
    public final static String SCHEME_NAME= "delta";
    public final static String FIELD_MODE= "mode", FIELD_THRESHOLD = "threshold";

    static {
        modeShortNames= new HashMap<>();
        modeShortNames.put("abs", OutputMode.ABSOLUTE);
        modeShortNames.put("diff", OutputMode.DIFFERENTIAL);
        modeShortNames.put("bin", OutputMode.BINARY);
    }

    /**
     * Representation of the delta processor's internal state.
     * @author Eric Tsai
     */
    public static class State implements DataProcessor.State {
        public final Number newPreviousValue;

        /**
         * Constructs a new delta transformer state
         * @param newPrevValue    New value to compare against
         */
        public State(Number newPrevValue) {
            this.newPreviousValue= newPrevValue;
        }
    }

    /**
     * Output modes for this data processor
     * @author Eric Tsai
     */
    public enum OutputMode {
        /** Return the data as is */
        ABSOLUTE,
        /** Return the difference between the value and its reference point */
        DIFFERENTIAL,
        /** 1 if the difference is positive, -1 if negative */
        BINARY
    }

    public final OutputMode mode;
    public final Number threshold;

    /**
     * Constructs a delta config object from a URI string
     * @param query    String-String map containing the fields from the URI string
     */
    public Delta(Map<String, String> query) {
        if (!query.containsKey(FIELD_MODE)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_MODE);
        } else {
            if (modeShortNames.containsKey(query.get(FIELD_MODE).toLowerCase())) {
                mode = modeShortNames.get(query.get(FIELD_MODE).toLowerCase());
            } else {
                mode = Enum.valueOf(OutputMode.class, query.get(FIELD_MODE).toUpperCase());
            }
        }

        if (!query.containsKey(FIELD_THRESHOLD)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_THRESHOLD);
        } else {
            if (query.get(FIELD_THRESHOLD).contains(".")) {
                threshold = Float.valueOf(query.get(FIELD_THRESHOLD));
            } else {
                threshold = Integer.valueOf(query.get(FIELD_THRESHOLD));
            }
        }
    }

    /**
     * Constructs a config object for the specific settings
     * @param mode         Operation mode of the processor
     * @param threshold    Min distance from the reference value to allow the input to pass
     */
    public Delta(OutputMode mode, Number threshold) {
        this.mode = mode;
        this.threshold = threshold;
    }
}
