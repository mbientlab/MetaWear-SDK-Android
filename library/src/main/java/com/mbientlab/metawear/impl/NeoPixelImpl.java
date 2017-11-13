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

import com.mbientlab.metawear.module.NeoPixel;

import java.util.HashMap;

import static com.mbientlab.metawear.impl.Constant.Module.NEO_PIXEL;

/**
 * Created by etsai on 9/18/16.
 */
class NeoPixelImpl extends ModuleImplBase implements NeoPixel {
    private static final byte INITIALIZE= 1,
            HOLD= 2,
            CLEAR= 3, SET_COLOR= 4,
            ROTATE= 5,
            FREE= 6;
    private static final long serialVersionUID = -3877020058618686105L;
    private final HashMap<Byte, Byte> activeStrands= new HashMap<>();

    NeoPixelImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
        this.mwPrivate= mwPrivate;
    }

    @Override
    public Strand initializeStrand(byte strand, ColorOrdering ordering, StrandSpeed speed, byte gpioPin, byte length) {
        activeStrands.put(strand, length);
        mwPrivate.sendCommand(new byte[] {NEO_PIXEL.id, INITIALIZE, strand, (byte)(speed.ordinal() << 2 | ordering.ordinal()), gpioPin, length});
        return createStrandObj(strand, length);
    }

    @Override
    public Strand lookupStrand(byte strand) {
        if (activeStrands.containsKey(strand)) {
            return createStrandObj(strand, activeStrands.get(strand));
        }
        return null;
    }

    private Strand createStrandObj(final byte strand, final byte length) {
        return new Strand() {
            @Override
            public void free() {
                activeStrands.remove(strand);
                mwPrivate.sendCommand(new byte[] {NEO_PIXEL.id, FREE, strand});
            }

            @Override
            public void hold() {
                mwPrivate.sendCommand(new byte[] {NEO_PIXEL.id, HOLD, strand, (byte) 1});
            }

            @Override
            public void release() {
                mwPrivate.sendCommand(new byte[] {NEO_PIXEL.id, HOLD, strand, (byte) 0});
            }

            @Override
            public void clear(byte start, byte end) {
                mwPrivate.sendCommand(new byte[] {NEO_PIXEL.id, CLEAR, strand, start, end});
            }

            @Override
            public void setRgb(byte index, byte red, byte green, byte blue) {
                mwPrivate.sendCommand(new byte[] {NEO_PIXEL.id, SET_COLOR, strand, index, red, green, blue});
            }

            @Override
            public void rotate(RotationDirection direction, byte repetitions, short period) {
                mwPrivate.sendCommand(new byte[] {NEO_PIXEL.id, ROTATE, strand, (byte)direction.ordinal(), repetitions,
                        (byte)(period & 0xff), (byte)(period >> 8 & 0xff)});
            }

            @Override
            public void rotate(RotationDirection direction, short period) {
                rotate(direction, (byte) -1, period);
            }

            @Override
            public void stopRotation() {
                mwPrivate.sendCommand(new byte[] {NEO_PIXEL.id, ROTATE, strand, 0x0, 0x0, 0x0, 0x0});
            }

            @Override
            public int nLeds() {
                return length;
            }
        };
    }
}
