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

import com.mbientlab.metawear.MetaWearBoard.Module;

/**
 * A brand of RGB led strips by Adafruit
 * @author Eric Tsai
 */
public interface NeoPixel extends Module {
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
     * Represents a NeoPixel strand
     * @author Eric Tsai
     */
    interface Strand {
        /**
         * Enumeration of rotation directions
         * @author Eric Tsai
         */
        enum RotationDirection {
            /** Move LED color patterns towards the board */
            TOWARDS,
            /** Move LED color patterns away from the board */
            AWAY
        }

        /**
         * Free resources allocated by the firmware for this strand.  After calling free,
         * this object is no longer valid and should be discarded
         */
        void free();
        /**
         * Enables strand holding.  When enabled, the strand will not refresh with any LED changes until the hold
         * is disabled.  This allows you to form complex LED patterns without having the strand refresh with partial changes.
         */
        void hold();
        /**
         * Disables strand holding.  The strand will be refreshed with any LED changes programmed while the hold was active
         */
        void release();
        /**
         * Clears the LEDs in the given range
         * @param start Led index to start clearing from
         * @param end   Led index to clear to, exclusive
         */
        void clear(byte start, byte end);
        /**
         * Set and LED's rgb values
         * @param index LED index to set, from [0, nLeds - 1]
         * @param red Red value, between [0, 255]
         * @param green Green value, between [0, 255]
         * @param blue Blue value, between [0, 255]
         */
        void setRgb(byte index, byte red, byte green, byte blue);
        /**
         * Rotate the LED color patterns on a strand
         * @param direction Rotation direction
         * @param repetitions Number of times to repeat the rotation
         * @param period Amount of time, in milliseconds, between rotations
         */
        void rotate(RotationDirection direction, byte repetitions, short period);
        /**
         * Rotate the LED color patterns on a strand indefinitely
         * @param direction Rotation direction
         * @param period Amount of time, in milliseconds, between rotations
         */
        void rotate(RotationDirection direction, short period);
        /**
         * Stops the LED rotation
         */
        void stopRotation();

        /**
         * Return the number of Leds initialized for the strand
         * @return Number of initialized LEDs
         */
        int nLeds();
    }

    /**
     * Initialize memory on the MetaWear board for a NeoPixel strand
     * @param strand Strand number (id) to initialize, can be in the range [0, 2]
     * @param ordering Color ordering format
     * @param speed Operating speed
     * @param gpioPin GPIO pin the strand is connected to
     * @param length Number of LEDs to use
     * @return Object representing the initialized strand
     */
    Strand initializeStrand(byte strand, ColorOrdering ordering, StrandSpeed speed, byte gpioPin, byte length);
    /**
     * Find the object corresponding to the strand number
     * @param strand    Strand number to look up
     * @return Strand object matching the number, null if no match is found
     */
    Strand lookupStrand(byte strand);
}
