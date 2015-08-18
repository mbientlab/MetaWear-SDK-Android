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

import java.util.Map;

/**
 * Configuration for the counter data processor.  This class was slightly modified after v2.0.0 to correctly
 * represent the actions of the counter.  The constructor now takes in a byte corresponding to how many bytes
 * are available to the counter, not an int the counter should count up to.
 * @author Eric Tsai
 */
public class Counter implements DataSignal.ProcessorConfig {
    public static final String SCHEME_NAME= "counter";
    public static final String FIELD_SIZE = "size";

    /**
     * @deprecated This field incorrectly represented the underlying implementation i.e. users could not
     * set a counter max value, only how many bytes the counter has to use.  Use {@link #FIELD_SIZE} instead.
     */
    @Deprecated
    public static final String FIELD_LIMIT = "limit";

    /**
     * Representation of the counter's internal state
     * @author Eric Tsai
     */
    public static class State implements DataProcessor.State {
        public final int newCount;

        /**
         * Constructs a new counter processor state
         * @param newCount    Number of samples the counter has seen
         */
        public State(int newCount) {
            this.newCount= newCount;
        }
    }

    public final byte size;

    /**
     * Constructs a counter config object from a URI string
     * @param query    String-String map containing the fields from the URI string
     */
    public Counter(Map<String, String> query) {
        if (query.containsKey(FIELD_SIZE)) {
            size = Byte.valueOf(query.get(FIELD_SIZE));
        } else {
            size = 1;
        }
    }

    /**
     * Constructs a counter that counts up to 255
     */
    public Counter() {
        size = 1;
    }

    /**
     * Constructs a config object with user defined counter size
     * @param size Number of bytes to allocate for the counter, between [1-4]
     */
    public Counter(byte size) {
        this.size = size;
    }
}
