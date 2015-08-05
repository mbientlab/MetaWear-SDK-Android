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

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the threshold data processor
 * @author Eric Tsai
 */
public class Threshold implements DataSignal.ProcessorConfig {
    private static final HashMap<String, OutputMode> modeShortNames;
    public static final String SCHEME_NAME= "threshold";
    public static final String FIELD_LIMIT= "limit", FIELD_HYSTERESIS="hysteresis", FIELD_MODE= "mode";

    static {
        modeShortNames= new HashMap<>();
        modeShortNames.put("abs", OutputMode.ABSOLUTE);
        modeShortNames.put("bin", OutputMode.BINARY);
    }

    /**
     * Output modes for the threshold processor
     */
    public enum OutputMode {
        /** Return the data as is */
        ABSOLUTE,
        /** 1 if the data exceeded the threshold, -1 if below */
        BINARY
    }

    public final Number limit, hysteresis;
    public final OutputMode mode;

    /**
     * Constructs a threshold config object from a URI string
     * @param query    String-String map containing the fields from the URI string
     */
    public Threshold(Map<String, String> query) {
        if (!query.containsKey(FIELD_LIMIT)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_LIMIT);
        } else {
            if (query.get(FIELD_LIMIT).contains(".")) {
                limit= Float.valueOf(query.get(FIELD_LIMIT));
            } else {
                limit= Integer.valueOf(query.get(FIELD_LIMIT));
            }
        }

        if (query.containsKey(FIELD_HYSTERESIS)) {
            if (query.get(FIELD_HYSTERESIS).contains(".")) {
                hysteresis = Float.valueOf(query.get(FIELD_HYSTERESIS));
            } else {
                hysteresis = Integer.valueOf(query.get(FIELD_HYSTERESIS));
            }
        } else {
            hysteresis= 0;
        }

        if (!query.containsKey(FIELD_MODE)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_MODE);
        } else {
            if (modeShortNames.containsKey(query.get(FIELD_MODE).toLowerCase())) {
                mode = modeShortNames.get(query.get(FIELD_MODE).toLowerCase());
            } else {
                mode = Enum.valueOf(OutputMode.class, query.get(FIELD_MODE).toUpperCase());
            }
        }
    }

    /**
     * Constructs a threshold config object, setting the hysteresis distance
     * @param limit         Boundary the data must cross
     * @param mode          Operation mode for the processor
     * @param hysteresis    Minimum distance between the limit and value to signal a successful crossing.
     *                      Use with data that may frequently oscillate around the threshold limit.
     */
    public Threshold(Number limit, OutputMode mode, Number hysteresis) {
        this.limit= limit;
        this.hysteresis= hysteresis;
        this.mode= mode;
    }

    /**
     * Constructs a threshold config object
     * @param limit    Boundary the data must cross
     * @param mode     Operation mode for the processor
     */
    public Threshold(Number limit, OutputMode mode) {
        this(limit, mode, 0);
    }
}
