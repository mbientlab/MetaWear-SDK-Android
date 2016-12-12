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

package com.mbientlab.metawear.data;

import java.util.Locale;

/**
 * Encapsulates angular velocity data, values are in degrees per second
 * @author Eric Tsai
 */
public class AngularVelocity extends FloatVector {
    /** Degrees per second */
    protected static final String DEGS_PER_SEC= "\u00B0/s";

    public AngularVelocity(float x, float y, float z) {
        super(x, y, z);
    }

    /**
     * Gets the angular velocity around the x-axis
     * @return X-axis angular velocity
     */
    public float x() {
        return vector[0];
    }
    /**
     * Gets the angular velocity around the y-axis
     * @return Y-axis angular velocity
     */
    public float y() {
        return vector[1];
    }
    /**
     * Gets the angular velocity around the z-axis
     * @return Z-axis angular velocity
     */
    public float z() {
        return vector[2];
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "{x: %.3f%s, y: %.3f%s, z: %.3f%s}",
                x(), DEGS_PER_SEC,
                y(), DEGS_PER_SEC,
                z(), DEGS_PER_SEC
        );
    }
}
