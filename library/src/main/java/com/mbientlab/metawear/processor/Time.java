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
 * Configuration for the time data processor
 * @author Eric Tsai
 */
public class Time implements DataSignal.ProcessorConfig {
    private static final HashMap<String, OutputMode> MODE_SHORT_NAMES;
    public static final String SCHEME_NAME= "time";
    public static final String FIELD_PERIOD= "period", FIELD_MODE= "mode";

    static {
        MODE_SHORT_NAMES = new HashMap<>();
        MODE_SHORT_NAMES.put("abs", OutputMode.ABSOLUTE);
        MODE_SHORT_NAMES.put("diff", OutputMode.DIFFERENTIAL);
    }

    /**
     * Output modes for the time processor
     */
    public enum OutputMode {
        /** No change to the input */
        ABSOLUTE,
        /** Return the difference between the current and previous values */
        DIFFERENTIAL
    }

    public final OutputMode mode;
    public final int period;

    /**
     * Constructs a time config object from a URI string
     * @param query    String-String map containing the fields from the URI string
     */
    public Time(Map<String, String> query) {
        if (!query.containsKey(FIELD_PERIOD)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_PERIOD);
        } else {
            period= Integer.valueOf(query.get(FIELD_PERIOD));
        }

        if (!query.containsKey(FIELD_MODE)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_MODE);
        } else {
            if (MODE_SHORT_NAMES.containsKey(query.get(FIELD_MODE).toLowerCase())) {
                mode = MODE_SHORT_NAMES.get(query.get(FIELD_MODE).toLowerCase());
            } else {
                mode = Enum.valueOf(OutputMode.class, query.get(FIELD_MODE).toUpperCase());
            }
        }
    }

    /**
     * Constructs a config object for a time data processor
     * @param mode      Operation mode of the processor
     * @param period    How often to alllow data through, in milliseconds
     */
    public Time(OutputMode mode, int period) {
        this.period= period;
        this.mode= mode;
    }
}
