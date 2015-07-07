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

package com.mbientlab.metawear.module;

import com.mbientlab.metawear.MetaWearBoard;

/**
 * Created by etsai on 6/18/2015.
 */
public interface Led extends MetaWearBoard.GenericModule {
    public enum ColorChannel {
        GREEN,
        RED,
        BLUE
    }

    public interface ColorChannelWriter {
        /**
         * Intensity value of the high state
         * @param intensity LED intensity the high state should be in, between [0 - 31]
         * @return Calling object
         */
        public ColorChannelWriter withHighIntensity(byte intensity);
        /**
         * Intensity value of the low state
         * @param intensity LED intensity the low state should be in, between [0 - 31]
         * @return Calling object
         */
        public ColorChannelWriter withLowIntensity(byte intensity);
        /**
         * How long the transition should take from low to high state, in milliseconds
         * @param time Transition time (ms) from low to high state
         * @return Calling object
         */
        public ColorChannelWriter withRiseTime(short time);
        /**
         * How long the pulse stays in the high state
         * @param time Length of time (ms) to spend in the high state
         * @return Calling object
         */
        public ColorChannelWriter withHighTime(short time);
        /**
         * How long the transition should take from high to low state, in milliseconds
         * @param time Length of time (ms) from high to low state
         * @return Calling object
         */
        public ColorChannelWriter withFallTime(short time);
        /**
         * How long one pulse is
         * @param duration Length of one pulse (ms)
         * @return Calling object
         */
        public ColorChannelWriter withPulseDuration(short duration);
        /**
         * How many times to repeat a pulse pattern
         * @param count Number of repetitions, set to 255 to repeat indefinitely
         * @return Calling object
         */
        public ColorChannelWriter withRepeatCount(byte count);

        /** Write the settings to the board */
        public void commit();
    }

    public ColorChannelWriter writeChannelAttributes(ColorChannel channel);
    public void play(boolean autoplay);
    public void pause();
    public void stop(boolean resetChannelAttrs);


}
