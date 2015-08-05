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
 * Communicates with the ultra-bright LED
 * @author Eric Tsai
 */
public interface Led extends MetaWearBoard.Module {
    /**
     * Available colors on the LED
     * @author Eric Tsai
     */
    enum ColorChannel {
        GREEN,
        RED,
        BLUE
    }

    /**
     * Interface for configuring the color channels of the LED
     * @author Eric Tsai
     */
    interface ColorChannelEditor {
        /**
         * Intensity value of the high state
         * @param intensity LED intensity the high state should be in, between [0 - 31]
         * @return Calling object
         */
        ColorChannelEditor setHighIntensity(byte intensity);
        /**
         * Intensity value of the low state
         * @param intensity LED intensity the low state should be in, between [0 - 31]
         * @return Calling object
         */
        ColorChannelEditor setLowIntensity(byte intensity);
        /**
         * How long the transition should take from low to high state, in milliseconds
         * @param time Transition time (ms) from low to high state
         * @return Calling object
         */
        ColorChannelEditor setRiseTime(short time);
        /**
         * How long the pulse stays in the high state
         * @param time Length of time (ms) to spend in the high state
         * @return Calling object
         */
        ColorChannelEditor setHighTime(short time);
        /**
         * How long the transition should take from high to low state, in milliseconds
         * @param time Length of time (ms) from high to low state
         * @return Calling object
         */
        ColorChannelEditor setFallTime(short time);
        /**
         * How long one pulse is
         * @param duration Length of one pulse (ms)
         * @return Calling object
         */
        ColorChannelEditor setPulseDuration(short duration);
        /**
         * How many times to repeat a pulse pattern
         * @param count Number of repetitions, set to 255 to repeat indefinitely
         * @return Calling object
         */
        ColorChannelEditor setRepeatCount(byte count);

        /** Write the settings to the board */
        void commit();
    }

    /**
     * Configures the pulse paramters for a color channel
     * @param channel    Channel to modify
     * @return Editor object to configure various settings
     */
    ColorChannelEditor configureColorChannel(ColorChannel channel);

    /**
     * Play or resume patterns
     * @param autoplay    True if a pattern should immediately begin upon being programmed
     */
    void play(boolean autoplay);

    /**
     * Pause the LED patterns
     */
    void pause();

    /**
     * Stop all patterns and reset the pulse time to 0
     * @param resetChannelAttrs    True to clear all patterns
     */
    void stop(boolean resetChannelAttrs);


}
