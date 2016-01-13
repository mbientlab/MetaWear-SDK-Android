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

package com.mbientlab.metawear.impl.characteristic;

/**
 * Created by etsai on 7/8/2015.
 */
public enum Bmi160AccelerometerRegister implements Register {
    POWER_MODE {
        @Override public byte opcode() { return 0x1; }
    },
    DATA_INTERRUPT_ENABLE {
        @Override public byte opcode() { return 0x2; }
    },
    DATA_CONFIG {
        @Override public byte opcode() { return 0x3; }
    },
    DATA_INTERRUPT {
        @Override public byte opcode() { return 0x4; }
    },
    DATA_INTERRUPT_CONFIG {
        @Override public byte opcode() { return 0x5; }
    },
    LOW_HIGH_G_INTERRUPT_ENABLE {
        @Override public byte opcode() { return 0x6; }
    },
    LOW_HIGH_G_CONFIG {
        @Override public byte opcode() { return 0x7; }
    },
    LOW_HIGH_G_INTERRUPT {
        @Override public byte opcode() { return 0x8; }
    },
    MOTION_INTERRUPT_ENABLE {
        @Override public byte opcode() { return 0x9; }
    },
    MOTION_CONFIG {
        @Override public byte opcode() { return 0xa; }
    },
    MOTION_INTERRUPT {
        @Override public byte opcode() { return 0xb; }
    },
    TAP_INTERRUPT_ENABLE {
        @Override public byte opcode() { return 0xc; }
    },
    TAP_CONFIG {
        @Override public byte opcode() { return 0xd; }
    },
    TAP_INTERRUPT {
        @Override public byte opcode() { return 0xe; }
    },
    ORIENT_INTERRUPT_ENABLE {
        @Override public byte opcode() { return 0xf; }
    },
    ORIENT_CONFIG {
        @Override public byte opcode() { return 0x10; }
    },
    ORIENT_INTERRUPT {
        @Override public byte opcode() { return 0x11; }
    },
    FLAT_INTERRUPT_ENABLE {
        @Override public byte opcode() { return 0x12; }
    },
    FLAT_CONFIG {
        @Override public byte opcode() { return 0x13; }
    },
    FLAT_INTERRUPT {
        @Override public byte opcode() { return 0x14; }
    },
    STEP_DETECTOR_INTERRUPT_ENABLE {
        @Override public byte opcode() { return 0x17; }
    },
    STEP_DETECTOR_CONFIG {
        @Override public byte opcode() { return 0x18; }
    },
    STEP_DETECTOR_INTERRUPT {
        @Override public byte opcode() { return 0x19; }
    },
    STEP_COUNTER_DATA {
        @Override public byte opcode() { return 0x1a; }
    },
    STEP_COUNTER_RESET {
        @Override public byte opcode() { return 0x1b; }
    };

    @Override
    public byte moduleOpcode() { return InfoRegister.ACCELEROMETER.moduleOpcode(); }
}
