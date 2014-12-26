/*
 * Copyright 2014 MbientLab Inc. All rights reserved.
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
package com.mbientlab.metawear.api.controller;

import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

import static com.mbientlab.metawear.api.Module.LED;

/**
 * Controller for the LED module
 * @author Eric Tsai
 * @see com.mbientlab.metawear.api.Module#LED
 */
public interface LED extends ModuleController {
    /**
     * Enumeration of registers under the LED module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Controls playing and pausing an LED pattern */
        PLAY {
            @Override public byte opcode() { return 0x1; }
        },
        /** Stops a pattern, can also reset pattern settings */
        STOP {
            @Override public byte opcode() { return 0x2; }
        },
        /** Stores data about a pulse pattern */
        MODE {
            @Override public byte opcode() { return 0x3; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    final byte[] data) {
                ChannelData chData= new ChannelData() {
                    @Override
                    public ColorChannel channel() {
                        return ColorChannel.values[data[0]];
                    }

                    @Override
                    public byte highIntensity() {
                        return data[2];
                    }

                    @Override
                    public byte lowIntensity() {
                        // TODO Auto-generated method stub
                        return data[3];
                    }

                    @Override
                    public short riseTime() {
                        short time= data[4];
                        time= (short)((time << 8) | data[5]);
                        return time;
                    }

                    @Override
                    public short highTime() {
                        short time= data[6];
                        time= (short)((time << 8) | data[7]);
                        return time;
                    }

                    @Override
                    public short fallTime() {
                        short time= data[8];
                        time= (short)((time << 8) | data[9]);
                        return time;
                    }

                    @Override
                    public short pulseDuration() {
                        short period= data[10];
                        period= (short)((period << 8) | data[11]);
                        return period;
                    }

                    @Override
                    public short pulseOffset() {
                        short offset= data[12];
                        offset= (short)((offset << 8) | data[13]);
                        return offset;
                    }

                    @Override
                    public byte repeatCount() {
                        return data[14];
                    }
                };
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedChannelData(chData);
                }
            }
        };
        
        @Override public Module module() { return LED; }
        @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) { }
    }
    /**
     * States the LED can be in
     * @author Eric Tsai
     */
    public enum State {
        STOP,
        PLAY,
        PAUSE;
    }
    
    /**
     * Colors that can be controlled
     * @author Eric Tsai
     */
    public enum ColorChannel {
        GREEN,
        RED,
        BLUE;
        
        /** Helper variable to keep a reference of the values */
        public static ColorChannel[] values= ColorChannel.values();
    }
    
    /**
     * Wrapper class combining all the data describing an LED channel
     * @author Eric Tsai
     */
    public interface ChannelData {
        /**
         * Color the data describes
         * @return Color channel of this data object
         */
        public ColorChannel channel();
        /** LED intensity of the high state, [0 - 31] */
        public byte highIntensity();
        /** LED intensity of the low state, [0 - 31] */
        public byte lowIntensity();
        /** Transition time from low to high (ms) */
        public short riseTime();
        /** Time the pulse stays in a high state (ms) */
        public short highTime();
        /** Transition time from high to low state (ms) */
        public short fallTime();
        /** Length of time, in ms, of one pulse */
        public short pulseDuration();
        /** Length of time, in ms, to wait before starting the pattern */
        public short pulseOffset();
        /** Number of times to repeat the pattern */
        public byte repeatCount();
    }
    
    /**
     * Callbacks for the IBeacon module
     * @author Eric Tsai
     */
    public abstract static class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return LED; }
        
        /**
         * Called when channel data has been received from the board
         * @param chData Channel data of a requested color
         */
        public void receivedChannelData(ChannelData chData) { }
    }
    
    /**
     * Helper class to facilitate writing pulse information to a color channel
     * @author Eric Tsai
     */
    public interface ChannelDataWriter {
        /** Color the writing is setting */
        public ColorChannel getChannel();
        
        /**
         * Intensity value of the high state
         * @param intensity LED intensity the high state should be in, between [0 - 31]
         * @return Calling object
         */
        public ChannelDataWriter withHighIntensity(byte intensity);
        /**
         * Intensity value of the low state
         * @param intensity LED intensity the low state should be in, between [0 - 31]
         * @return Calling object
         */
        public ChannelDataWriter withLowIntensity(byte intensity);
        /**
         * How long the transition should take from low to high state, in milliseconds
         * @param time Transition time (ms) from low to high state
         * @return Calling object
         */
        public ChannelDataWriter withRiseTime(short time);
        /**
         * How long the pulse stays in the high state
         * @param time Length of time (ms) to spend in the high state
         * @return Calling object
         */
        public ChannelDataWriter withHighTime(short time);
        /**
         * How long the transition should take from high to low state, in milliseconds
         * @param time Length of time (ms) from high to low state
         * @return Calling object
         */
        public ChannelDataWriter withFallTime(short time);
        /**
         * How long one pulse is
         * @param duration Length of one pulse (ms)
         * @return Calling object
         */
        public ChannelDataWriter withPulseDuration(short duration);
        /**
         * How long to wait before starting the first pulse
         * @param offset Wait time for first pulse (ms)
         * @return Calling object
         */
        public ChannelDataWriter withPulseOffset(short offset);
        /**
         * How many times to repeat a pulse pattern
         * @param count Number of repetitions, set to 255 to repeat indefinitely
         * @return Calling object
         */
        public ChannelDataWriter withRepeatCount(byte count);
        
        /** Write the settings to the board */
        public void commit();
    }
    
    /**
     * Play or resume a pulse pattern
     * @param autoplay Set to true if a pattern should be immediately started upon writing 
     */
    public void play(boolean autoplay);
    /** Pause a pattern */
    public void pause();
    /**
     * Stop a pulse pattern and reset pulse time to 0
     * @param resetChannels True if the channel states should be reset as well
     */
    public void stop(boolean resetChannels);
    
    /**
     * Sets the color channel to read to and write from
     * @param color Color channel to interact with
     * @return A data writer to set channel properties
     */
    public ChannelDataWriter setColorChannel(ColorChannel color);
    
}
