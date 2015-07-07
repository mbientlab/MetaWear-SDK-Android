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

/**
 * Created by eric on 6/20/2015.
 */
public class Comparison implements DataSignal.DataFilter {
    public static final String SCHEME_NAME= "comparison";
    public final static String FIELD_OP= "operation", FIELD_SIGNED= "signed", FIELD_REFERENCE="reference";

    /**
     * Available comparison operations for the filter
     */
    public enum Operation {
        /** Equal */
        EQ,
        /** Not equal */
        NEQ,
        /** Less than */
        LT,
        /** Less than or equal to */
        LTE,
        /** Greater than */
        GT,
        /** Greater than or equal to */
        GTE,
    }

    public final Boolean signed;
    public final Operation compareOp;
    public final Number reference;

    public Comparison(Map<String, String> query) {
        if (!query.containsKey(FIELD_OP)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_OP);
        } else {
            compareOp= Enum.valueOf(Operation.class, query.get(FIELD_OP).toUpperCase());
        }

        if (query.containsKey(FIELD_SIGNED)) {
            signed= Boolean.valueOf(query.get(FIELD_SIGNED).toUpperCase());
        } else {
            signed= null;
        }

        if (!query.containsKey(FIELD_REFERENCE)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_REFERENCE);
        } else {
            if (query.get(FIELD_REFERENCE).contains(".")) {
                reference= Float.valueOf(query.get(FIELD_REFERENCE));
            } else {
                reference= Integer.valueOf(query.get(FIELD_REFERENCE));
            }
        }
    }

    /**
     * Constructs a config object with inferred signed or unsigned comparison
     * @param op Comparison operation to filter on
     * @param reference Value to compare against
     */
    public Comparison(Operation op, Number reference) {
        this(op, reference, null);
    }

    /**
     * Constructs a config object with user explicitly requesting a signed or unsigned comparison
     * @param op Comparison operation to filter on
     * @param reference Value to compare against
     * @param signed True if a signed comparison should be used, false for unsigned
     */
    public Comparison(Operation op, Number reference, Boolean signed) {
        this.compareOp= op;
        this.reference= reference;
        this.signed= signed;
    }
}
