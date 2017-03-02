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

import com.mbientlab.metawear.module.Led;

import static com.mbientlab.metawear.impl.Constant.Module.LED;

/**
 * Created by etsai on 8/31/16.
 */
class LedImpl extends ModuleImplBase implements Led {
    private static final byte PLAY= 0x1, STOP= 0x2, CONFIG= 0x3;
    private static final byte REVISION_LED_DELAYED= 1;
    private static final long serialVersionUID = 5937697572396920500L;

    LedImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
    }

    @Override
    public PatternEditor editPattern(Color ledColor) {
        final byte[] command= new byte[17];
        byte[] initial= {LED.id, CONFIG, (byte) ledColor.ordinal(), 0x2};
        System.arraycopy(initial, 0, command, 0, initial.length);

        return new PatternEditor() {
            @Override
            public PatternEditor highIntensity(byte intensity) {
                command[4]= intensity;
                return this;
            }

            @Override
            public PatternEditor lowIntensity(byte intensity) {
                command[5]= intensity;
                return this;
            }

            @Override
            public PatternEditor riseTime(short time) {
                command[7]= (byte)((time >> 8) & 0xff);
                command[6]= (byte)(time & 0xff);
                return this;
            }

            @Override
            public PatternEditor highTime(short time) {
                command[9]= (byte)((time >> 8) & 0xff);
                command[8]= (byte)(time & 0xff);
                return this;
            }

            @Override
            public PatternEditor fallTime(short time) {
                command[11]= (byte)((time >> 8) & 0xff);
                command[10]= (byte)(time & 0xff);
                return this;
            }

            @Override
            public PatternEditor pulseDuration(short duration) {
                command[13]= (byte)((duration >> 8) & 0xff);
                command[12]= (byte)(duration & 0xff);
                return this;
            }

            @Override
            public PatternEditor delay(short delay) {
                if (mwPrivate.lookupModuleInfo(Constant.Module.LED).revision >= REVISION_LED_DELAYED) {
                    command[15]= (byte)((delay >> 8) & 0xff);
                    command[14]= (byte)(delay & 0xff);
                } else {
                    command[15]= 0;
                    command[14]= 0;
                }
                return this;
            }

            @Override
            public PatternEditor repeatCount(byte count) {
                command[16]= count;
                return this;
            }

            @Override
            public void commit() {
                mwPrivate.sendCommand(command);
            }
        };
    }

    @Override
    public PatternEditor editPattern(Color ledColor, PatternPreset preset) {
        PatternEditor editor= editPattern(ledColor);

        switch(preset) {
            case BLINK:
                editor.highIntensity((byte) 31)
                        .highTime((short) 50)
                        .pulseDuration((short) 500);
                break;
            case PULSE:
                editor.highIntensity((byte) 31)
                        .riseTime((short) 725)
                        .highTime((short) 500)
                        .fallTime((short) 725)
                        .pulseDuration((short) 2000);
                break;
            case SOLID:
                editor.highIntensity((byte) 31)
                        .lowIntensity((byte) 31)
                        .highTime((short) 500)
                        .pulseDuration((short) 1000);
                break;
        }
        return editor;
    }

    @Override
    public void autoplay() {
        mwPrivate.sendCommand(new byte[] {LED.id, PLAY, 2});
    }

    @Override
    public void play() {
        mwPrivate.sendCommand(new byte[] {LED.id, PLAY, 1});
    }

    @Override
    public void pause() {
        mwPrivate.sendCommand(new byte[] {LED.id, PLAY, 0});
    }

    @Override
    public void stop(boolean clear) {
        mwPrivate.sendCommand(new byte[] {LED.id, STOP, (byte) (clear ? 1 : 0)});
    }
}
