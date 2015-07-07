/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.processor;

import com.mbientlab.metawear.DataSignal;

import java.util.Map;
import java.util.MissingResourceException;

/**
 * Created by eric on 6/20/2015.
 */
public class Passthrough implements DataSignal.DataFilter {
    public static final String SCHEME_NAME= "passthrough";
    public static final String FIELD_MODE= "mode", FIELD_VALUE= "value";

    /**
     * Operation modes for the passthrough filter
     * @author Eric Tsai
     */
    public enum Mode {
        /** Allow all data through */
        ALL,
        /** Only allow data through if value == 1*/
        CONDITIONAL,
        /** Only allow a fixed number of data samples through */
        COUNT
    }

    public final Mode passthroughMode;
    public final short value;

    public Passthrough(Map<String, String> query) {
        if (!query.containsKey(FIELD_MODE)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_MODE);
        } else {
            passthroughMode= Enum.valueOf(Mode.class, query.get(FIELD_MODE).toUpperCase());
        }

        if (passthroughMode != Mode.ALL) {
            if (!query.containsKey(FIELD_VALUE)) {
                throw new RuntimeException("Missing required field in URI: " + FIELD_VALUE);
            } else {
                value = Short.valueOf(query.get(FIELD_VALUE));
            }
        } else {
            value= 0;
        }
    }

    /**
     * Constructs a configuration object for a passthrough filter
     * @param passthroughMode Operation mode of the filter
     * @param value
     */
    public Passthrough(Mode passthroughMode, short value) {
        this.passthroughMode = passthroughMode;
        this.value = value;
    }

    /**
     * Constructs configuration for a passthrough filter that operates in {@link Mode#ALL All} mode
     */
    public Passthrough() {
        this(Mode.ALL, (short) 0);
    }
}
