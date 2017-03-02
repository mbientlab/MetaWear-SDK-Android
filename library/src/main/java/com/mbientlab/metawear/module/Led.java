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
 * Ultra bright RGB light emitting diode
 * @author Eric Tsai
 */
public interface Led extends Module {
    /**
     * Constant for PatternEditor.setRepeatCount indicating the pattern should repeat forever
     * @see PatternEditor#repeatCount(byte)
     */
    byte PATTERN_REPEAT_INDEFINITELY= -1;

    /**
     * Enumeration of available colors on the LED
     * @author Eric Tsai
     */
    enum Color {
        GREEN,
        RED,
        BLUE
    }

    /**
     * Enumeration of patterns from the <a href="http://projects.mbientlab.com/?p=656">blog post</a>
     * @author Eric Tsai
     */
    enum PatternPreset {
        BLINK,
        PULSE,
        SOLID
    }

    /**
     * Interface to edit pattern attributes
     * @author Eric Tsai
     */
    interface PatternEditor {
        /**
         * Set the intensity value of the high state
         * @param intensity LED intensity the high state should be in, between [0 - 31]
         * @return Calling object
         */
        PatternEditor highIntensity(byte intensity);
        /**
         * Set the intensity value of the low state
         * @param intensity LED intensity the low state should be in, between [0 - 31]
         * @return Calling object
         */
        PatternEditor lowIntensity(byte intensity);
        /**
         * Set how long the transition should take from low to high state, in milliseconds
         * @param time Transition time (ms) from low to high state
         * @return Calling object
         */
        PatternEditor riseTime(short time);
        /**
         * Set how long the pulse stays in the high state
         * @param time Length of time (ms) to spend in the high state
         * @return Calling object
         */
        PatternEditor highTime(short time);
        /**
         * Set how long the transition should take from high to low state, in milliseconds
         * @param time Length of time (ms) from high to low state
         * @return Calling object
         */
        PatternEditor fallTime(short time);
        /**
         * Set the duration of one pulse
         * @param duration Length of one pulse (ms)
         * @return Calling object
         */
        PatternEditor pulseDuration(short duration);
        /**
         * Set how long to wait before starting the pattern.  This setting is ignored on boards running firmware
         * older than v1.2.3
         * @param delay    Length of the delay (ms)
         * @return Calling object
         */
        PatternEditor delay(short delay);
        /**
         * Set how many times to repeat a pulse pattern
         * @param count Number of repetitions, use {@link #PATTERN_REPEAT_INDEFINITELY} to repeat forever
         * @return Calling object
         */
        PatternEditor repeatCount(byte count);

        /** Write the settings to the board */
        void commit();
    }

    /**
     * Edit the pattern attributes for the desired color
     * @param ledColor    Color to configure
     * @return Editor object to configure the pattern attributes
     */
    PatternEditor editPattern(Color ledColor);
    /**
     * Edit the pattern attributes for the desired color using a preset pattern as the initial attribute parameters
     * @param ledColor    Color to configure
     * @param preset      Pattern preset to use
     * @return Editor object to configure the pattern attributes
     */
    PatternEditor editPattern(Color ledColor, PatternPreset preset);

    /**
     * Play any programmed patterns and immediately plays patterns programmed later
     */
    void autoplay();

    /**
     * Play any programmed patterns
     */
    void play();
    /**
     * Pause the pattern playback
     */
    void pause();
    /**
     * Stop playing LED patterns
     * @param clear    True if the patterns should be cleared as well
     */
    void stop(boolean clear);
}
