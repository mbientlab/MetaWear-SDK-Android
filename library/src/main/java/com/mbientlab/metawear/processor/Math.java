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
import com.mbientlab.metawear.MessageToken;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by eric on 6/20/2015.
 */
public class Math implements DataSignal.ProcessorConfig {
    private static final HashMap<String, Operation> opShortNames;

    static {
        opShortNames= new HashMap<>();
        opShortNames.put("add", Operation.ADD);
        opShortNames.put("mult", Operation.MULTIPLY);
        opShortNames.put("div", Operation.DIVIDE);
        opShortNames.put("mod", Operation.MODULUS);
        opShortNames.put("exp", Operation.EXPONENT);
        opShortNames.put("sqrt", Operation.SQRT);
        opShortNames.put("lshift", Operation.LEFT_SHIFT);
        opShortNames.put("rshift", Operation.RIGHT_SHIFT);
        opShortNames.put("sub", Operation.SUBTRACT);
        opShortNames.put("abs", Operation.ABS_VALUE);
    }

    public static final String SCHEME_NAME= "math";
    public final static String FIELD_OP= "operation", FIELD_RHS= "rhs", FIELD_SIGNED= "signed";

    /**
     * Available math operations for the transformer
     */
    public enum Operation {
        /** No operation to perform on the data */
        NO_OP {
            @Override
            public boolean requiresRhs() { return false; }
        },
        /** Add the data */
        ADD,
        /** Multiply the data */
        MULTIPLY,
        /** Divide the data */
        DIVIDE,
        /** Calculate the remainder */
        MODULUS,
        /** Calculate exponentiation of the data */
        EXPONENT,
        /** Calculate square root */
        SQRT {
            @Override
            public boolean requiresRhs() { return false; }
        },
        /** Perform left shift */
        LEFT_SHIFT,
        /** Perform right shift */
        RIGHT_SHIFT,
        /** Subtract the data */
        SUBTRACT,
        /** Calculates the absolute value */
        ABS_VALUE {
            @Override
            public boolean requiresRhs() { return false; }
        };

        public boolean requiresRhs() { return true; }
    }

    public final MessageToken rhsToken;
    public final Number rhs;
    public final Operation mathOp;
    public final Boolean signed;

    public Math(Map<String, String> query) {
        rhsToken = null;

        if (!query.containsKey(FIELD_OP)) {
            throw new RuntimeException("Missing required field in URI: " + FIELD_OP);
        }
        if (opShortNames.containsKey(query.get(FIELD_OP).toLowerCase())) {
            mathOp = opShortNames.get(query.get(FIELD_OP).toLowerCase());
        } else {
            mathOp = Enum.valueOf(Operation.class, query.get(FIELD_OP).toUpperCase());
        }


        if (query.containsKey(FIELD_SIGNED)) {
            signed = Boolean.valueOf(query.get(FIELD_SIGNED));
        } else {
            signed = null;
        }

        if (mathOp.requiresRhs()) {
            if (!query.containsKey(FIELD_RHS)) {
                throw new RuntimeException("Missing required field in URI: " + FIELD_RHS);
            }

            if (query.get(FIELD_RHS).contains(".")) {
                rhs = Float.valueOf(query.get(FIELD_RHS));
            } else {
                rhs = Integer.valueOf(query.get(FIELD_RHS));
            }
        } else {
            rhs= null;
        }
    }

    /**
     * Constructs a config object with user explicitly requesting a signed or unsigned operation
     * @param op Math operation to carry out on the input
     * @param rhs Value on the right hand side fo the operation
     * @param signed True to used signed operation, false for unsigned
     */
    public Math(Operation op, Number rhs, Boolean signed) {
        if (op.requiresRhs()) {
            if (rhs == null) {
                throw new RuntimeException("rhs parameter must be a number for math operation " + op.toString());
            } else {
                this.rhs= rhs;
            }
        } else {
            this.rhs= 0;
        }

        this.rhsToken = null;
        this.mathOp = op;
        this.signed= signed;
    }

    public Math(Operation op, MessageToken rhs, Boolean signed) {
        if (op.requiresRhs() && rhs == null) {
            throw new RuntimeException("rhs parameter must be a number for math operation " + op.toString());
        }

        this.rhsToken= rhs;
        this.rhs= 0;
        this.mathOp = op;
        this.signed= signed;
    }

    /**
     * Constructs a config object with inferred signed or unsigned operation
     * @param op Math operation to carry out on the input
     * @param rhs Value on the right hand side fo the operation
     */
    public Math(Operation op, Number rhs) {
        this(op, rhs, null);
    }

    public Math(Operation op, MessageToken rhs) {
        this(op, rhs, null);
    }
}
