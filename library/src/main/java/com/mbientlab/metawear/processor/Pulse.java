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

import java.util.Map;

/**
 * Configuration for the pulse data processor
 * @author Eric Tsai
 */
public class Pulse implements DataSignal.ProcessorConfig {
    public static final String SCHEME_NAME= "pulse";
    public static final String FIELD_OUTPUT = "output", FIELD_THRESHOLD= "threshold", FIELD_WIDTH= "width";

    /**
     * Output modes for the pulse processor
     * @author Eric Tsai
     */
    public enum OutputMode {
        /** Returns the number of samples in the pulse */
        WIDTH,
        /** Returns a running sum of all samples in the pulse */
        AREA,
        /** Returns the highest sample value in the pulse */
        PEAK
    }

    public final OutputMode mode;
    public final Number threshold;
    public final short width;

    /**
     * Constructs a pulse config object from a URI string
     * @param query    String-String map containing the fields from the URI string
     */
    public Pulse(Map<String, String> query) {
        if (!query.containsKey(FIELD_OUTPUT)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_OUTPUT);
        }
        mode = Enum.valueOf(OutputMode.class, query.get(FIELD_OUTPUT).toUpperCase());

        if (!query.containsKey(FIELD_WIDTH)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_WIDTH);
        }
        width= Short.valueOf(query.get(FIELD_WIDTH));

        if (!query.containsKey(FIELD_THRESHOLD)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_THRESHOLD);
        }

        if (query.get(FIELD_THRESHOLD).contains(".")) {
            threshold = Float.valueOf(query.get(FIELD_THRESHOLD));
        } else {
            threshold = Integer.valueOf(query.get(FIELD_THRESHOLD));
        }

    }

    /**
     * Constructs a pulse config object
     * @param mode      Output type for this processor
     * @param threshold    Value the sensor data must exceed for a valid pulse
     * @param width        Number of samples that must be above the threshold for a valid pulse
     */
    public Pulse(OutputMode mode, Number threshold, short width) {
        this.mode = mode;
        this.threshold= threshold;
        this.width= width;
    }
}
