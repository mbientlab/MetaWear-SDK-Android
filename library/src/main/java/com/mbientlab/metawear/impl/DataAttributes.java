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

package com.mbientlab.metawear.impl;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by etsai on 9/4/16.
 */
class DataAttributes implements Serializable {
    private static final long serialVersionUID = 236031852609753664L;

    final byte[] sizes;
    final byte copies, offset;
    final boolean signed;

    DataAttributes(byte[] sizes, byte copies, byte offset, boolean signed) {
        this.sizes = sizes;
        this.copies = copies;
        this.offset = offset;
        this.signed = signed;
    }

    DataAttributes dataProcessorCopy() {
        byte[] sizesCopy = Arrays.copyOf(sizes, sizes.length);
        return new DataAttributes(sizesCopy, copies, (byte) 0, signed);
    }

    DataAttributes dataProcessorCopySize(byte newSize) {
        byte[] sizesCopy = Arrays.copyOf(sizes, sizes.length);
        Arrays.fill(sizesCopy, newSize);
        return new DataAttributes(sizesCopy, copies, (byte) 0, signed);
    }

    DataAttributes dataProcessorCopySigned(boolean newSigned) {
        byte[] sizesCopy = Arrays.copyOf(sizes, sizes.length);
        return new DataAttributes(sizesCopy, copies, (byte) 0, newSigned);
    }

    DataAttributes dataProcessorCopyCopies(byte newCopies) {
        byte[] sizesCopy = Arrays.copyOf(sizes, sizes.length);
        return new DataAttributes(sizesCopy, newCopies, (byte) 0, signed);
    }

    public byte length() {
        return (byte) (unitLength() * copies);
    }

    public byte unitLength() {
        byte sum = 0;
        for(byte elem : sizes) {
            sum+= elem;
        }

        return sum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataAttributes that = (DataAttributes) o;

        return copies == that.copies && offset == that.offset && signed == that.signed && Arrays.equals(sizes, that.sizes);

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(sizes);
        result = 31 * result + (int) copies;
        result = 31 * result + (int) offset;
        result = 31 * result + (signed ? 1 : 0);
        return result;
    }
}
