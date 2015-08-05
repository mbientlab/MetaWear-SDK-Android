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

package com.mbientlab.metawear.module;

import com.mbientlab.metawear.MetaWearBoard;

/**
 * Controls NeoPixel strands connected to the board
 * @author Eric Tsai
 */
public interface NeoPixel extends MetaWearBoard.Module {
    /**
     * Color ordering for the NeoPixel color values
     * @author Eric Tsai
     */
    enum ColorOrdering {
        /** Red, green, blue order */
        MW_WS2811_RGB,
        /** Red, blue, green order */
        MW_WS2811_RBG,
        /** Green, red, blue order */
        MW_WS2811_GRB,
        /** Green, blue, red order */
        MW_WS2811_GBR
    }

    /**
     * Operating speeds for a NeoPixel strand
     * @author Eric Tsai
     */
    enum StrandSpeed {
        /** 400 kHz */
        SLOW,
        /** 800 kHz */
        FAST
    }

    /**
     * Enumeration of rotation directions
     * @author Eric Tsai
     */
    enum RotationDirection {
        /** Move pixels towards the board */
        TOWARDS,
        /** Move pixels away from the board */
        AWAY
    }

    /**
     * Initialize memory on the MetaWear board for a NeoPixel strand
     * @param strand Strand number (id) to initialize, can be in the range [0, 2]
     * @param ordering Color ordering format
     * @param speed Operating speed
     * @param gpioPin GPIO pin the strand is connected to
     * @param length Number of pixels to initialize
     */
    void initializeStrand(byte strand, ColorOrdering ordering, StrandSpeed speed, byte gpioPin, byte length);
    /**
     * Free resources on the MetaWeard board for a NeoPixel strand
     * @param strand Strand index to free
     */
    void deinitializeStrand(byte strand);

    /**
     * Enables strand holding.  When enabled, the strand will not refresh with any LED changes until the hold
     * is disabled.  This allows you to form complex LED patterns without having the strand refresh with partial changes.
     * @param strand    Strand number (id) to hold
     */
    void holdStrand(byte strand);

    /**
     * Disables strand holding.  The strand will be refreshed with any LED changes programmed while the hold was active
     * @param strand    Strand number (id) to release
     */
    void releaseHold(byte strand);

    /**
     * Clear pixel states on a strand
     * @param strand Strand number to clear
     * @param start Pixel index to start clearing from
     * @param end Pixel index to clear to, inclusive
     */
    void clearStrand(byte strand, byte start, byte end);
    /**
     * Set pixel color
     * @param strand Strand number the pixel is on
     * @param pixel Index of the pixel
     * @param red Red value, between [0, 255]
     * @param green Green value, between [0, 255]
     * @param blue Blue value, between [0, 255]
     */
    void setPixel(byte strand, byte pixel, byte red, byte green, byte blue);

    /**
     * Rotate the pixels on a strand
     * @param strand Strand to rotate
     * @param direction Rotation direction
     * @param repetitions Number of times to repeat the rotation
     * @param period Amount of time, in milliseconds, between rotations
     */
    void rotate(byte strand, RotationDirection direction, byte repetitions, short period);
    /**
     * Rotate the pixels on a strand indefinitely
     * @param strand Strand to rotate
     * @param direction Rotation direction
     * @param period Amount of time, in milliseconds, between rotations
     */
    void rotate(byte strand, RotationDirection direction, short period);
    /**
     * Stops the LED rotation
     * @param strand    Strand to stop LED rotation
     */
    void stopRotation(byte strand);
}
