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
 * PROVIDED “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, 
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

import java.nio.ByteBuffer;
import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

import static com.mbientlab.metawear.api.Module.NEO_PIXEL;

/**
 * Controller for the Neo Pixel module
 * @author Eric Tsai
 * @see com.mbientlab.metawear.api.Module#NEO_PIXEL
 */
public interface NeoPixel extends ModuleController {
    /**
     * Enumeration of registers under the Neo Pixel module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Initializes a strand and retrieves a strand state */
        INITIALIZE {
            @Override public byte opcode() { return 0x1; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks, 
                    byte[] data) {
                StrandSpeed speed= StrandSpeed.values[(byte)(data[3] & 0xf)];
                ColorOrdering order= ColorOrdering.values[(byte)((data[3] >> 4) & 0xf)];
                for(ModuleCallbacks it: callbacks) 
                    ((Callbacks)it).receivedStrandState(data[2], order, speed, data[4], data[5]);
            }
        },
        /** Sets and retrives a strand's hold state */
        HOLD {
            @Override public Module module() { return NEO_PIXEL; }
            @Override public byte opcode() { return 0x2; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks, 
                    byte[] data) {
                for(ModuleCallbacks it: callbacks) ((Callbacks)it).receivedHoldState(data[2], data[3]);
            }
        },
        /** Clears pixels on a strand */
        CLEAR {
            @Override public Module module() { return NEO_PIXEL; }
            @Override public byte opcode() { return 0x3; }
        },
        /** Sets or retrieves pixel color on a strand */ 
        PIXEL {
            @Override public Module module() { return NEO_PIXEL; }
            @Override public byte opcode() { return 0x4; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks, 
                    byte[] data) {
                for(ModuleCallbacks it: callbacks) 
                    ((Callbacks)it).receivedPixelColor(data[2], data[3], data[4], data[5], data[6]);
            }
        },
        /** Sets or retrieves rotation state */
        ROTATE {
            @Override public Module module() { return NEO_PIXEL; }
            @Override public byte opcode() { return 0x5; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks, 
                    byte[] data) {
                RotationDirection direction= RotationDirection.values[data[3]];
                short delay= ByteBuffer.wrap(data, 5, 2).getShort();
                for(ModuleCallbacks it: callbacks)
                    ((Callbacks)it).receivedRotatationState(data[2], direction, data[4], delay);
            }
        },
        /** Frees up the resources on a strand */
        DEINITIALIZE {
            @Override public Module module() { return NEO_PIXEL; }
            @Override public byte opcode() { return 0x6; }
        };
        
        @Override public Module module() { return NEO_PIXEL; }
        @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks, 
                byte[] data) { }
    }
    /**
     * Callbacks for NeoPixel module
     * @author Eric Tsai
     */
    public abstract static class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return Module.NEO_PIXEL; }
        
        /**
         * Called when the strand state has been read
         * @param strandIndex Strand index read
         * @param order Color ordering of the specific strand
         * @param speed Interface speed of the strand
         * @param pin Pin number on the MetaWear board the NeoPixel strand is connected to 
         * @param strandLength Number of pixels on the strand
         */
        public void receivedStrandState(byte strandIndex, ColorOrdering order, StrandSpeed speed, byte pin, byte strandLength) { }
        /**
         * Called when the strand's hold state has been read
         * @param strandIndex Strand index read
         * @param state 0 if disabled, 1 if enabled
         */
        public void receivedHoldState(byte strandIndex, byte state) { }
        /**
         * Called when a pixel color has been read
         * @param strandIndex Strand index the pixel resides on
         * @param pixel Index of the pixel
         * @param red Red color value
         * @param green Green color value
         * @param blue Blue color value
         */
        public void receivedPixelColor(byte strandIndex, byte pixel, byte red, byte green, byte blue) { }
        /**
         * Called when the rotate state of a strand has been read 
         * @param strandIndex Strand index read
         * @param direction 0 if shifting away from the board, 1 if shifting towards the board
         * @param repetitions Number of times the rotation will occur, -1 if rotation is happening indefinitely
         * @param period Delay between rotations in milliseconds
         */
        public void receivedRotatationState(byte strandIndex, RotationDirection direction, byte repetitions, short period) { }
    }
    
    /**
     * Color ordering for the NeoPixel color values
     * @author Eric Tsai
     */
    public enum ColorOrdering {
        /** Red, green, blue order */
        MW_WS2811_RGB,
        /** Red, blue, green order */
        MW_WS2811_RBG,
        /** Green, red, blue order */
        MW_WS2811_GRB,
        /** Green, blue, red order */
        MW_WS2811_GBR;
        
        public static final ColorOrdering values[]= values();
    }
    /**
     * Operating speeds for a NeoPixel strand
     * @author Eric Tsai
     */
    public enum StrandSpeed {
        /** 400 kHz */
        SLOW,
        /** 800 kHz */
        FAST;
        
        public static final StrandSpeed values[]= values();
    }
    
    /**
     * Enumeration of rotation directions
     * @author Eric Tsai
     */
    public enum RotationDirection {
        /** Move pixels away from the board */
        AWAY,
        /** Move pixels towards the board */
        TOWARDS;
        
        public static final RotationDirection values[]= values();
    }
    
    /**
     * Initialize a NeoPixel strand
     * @param strand Index to initialize
     * @param ordering Color ordering format
     * @param speed Operating speed
     * @param ioPin MetaWear pin number the strand is connected to
     * @param length Number of pixels to initialize
     */
    public void initializeStrand(byte strand, ColorOrdering ordering, StrandSpeed speed, byte ioPin, byte length);
    /**
     * Read the state of a NeoPixel strand.  When data is available, the receivedStrandState function will be called
     * @param strand Strand index to read information on
     * @see Callbacks#receivedStrandState(byte, 
     * com.mbientlab.metawear.api.controller.NeoPixel.ColorOrdering, 
     * com.mbientlab.metawear.api.controller.NeoPixel.StrandSpeed, byte, byte)
     */
    public void readStrandState(byte strand);
    /**
     * Set the hold state on a strand 
     * @param strand Strand to set
     * @param holdState 0 to disable, 1 to enable
     */
    public void holdStrand(byte strand, byte holdState);
    /**
     * Read the hold state of a NeoPixel strand.  When data is available, the receivedHoldState function will be called
     * @param strand Strand index to read the hold state on
     * @see Callbacks#receivedHoldState(byte, byte)
     */
    public void readHoldState(byte strand);
    /**
     * Clear pixel states on a strand
     * @param strand Strand index to clear
     * @param start Pixel index to start clearing from
     * @param end Pixel index to clear to
     */
    public void clearStrand(byte strand, byte start, byte end);
    /**
     * Set pixel color
     * @param strand Strand index the pixel is on
     * @param pixel Index of the pixel
     * @param red Red value
     * @param green Green value
     * @param blue Blue value
     */
    public void setPixel(byte strand, byte pixel, byte red, byte green, byte blue);
    /**
     * Read pixel color.  When data is available, the receivedPixelColor function will be called
     * @param strand Strand index the pixel resides on
     * @param pixel Pixel index to read the color from
     * @see Callbacks#receivedPixelColor(byte, byte, byte, byte, byte)
     */
    public void readPixelState(byte strand, byte pixel);
    /**
     * Rotate the pixels on a strand 
     * @param strand Strand to rotate
     * @param direction 0 to shift away from the board, 1 to shift towards
     * @param repetitions Number of extra times to repeat the rotation, -1 to rotate indefinitely
     * @param period Amount of time, in milliseconds, between rotations
     */
    public void rotateStrand(byte strand, RotationDirection direction, byte repetitions, short period);
    /**
     * Read rotation state of a NeoPixel strand.  When data is available, the receivedRotatationStatefunction will be called
     * @param strand Strand index to read
     * @see Callbacks#receivedRotatationState(byte, 
     * com.mbientlab.metawear.api.controller.NeoPixel.RotationDirection, byte, short)
     */
    public void readRotationState(byte strand);
    /**
     * Free resources on the NeoPixel strand
     * @param strand Strand index to free
     */
    public void deinitializeStrand(byte strand);
    
}
