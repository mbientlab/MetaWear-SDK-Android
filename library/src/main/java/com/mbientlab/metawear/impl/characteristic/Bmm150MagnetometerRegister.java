package com.mbientlab.metawear.impl.characteristic;

/**
 * Created by etsai on 1/7/2016.
 */
public enum Bmm150MagnetometerRegister implements Register {
    POWER_MODE {
        @Override public byte opcode() { return 0x1; }
    },
    DATA_INTERRUPT_ENABLE {
        @Override public byte opcode() { return 0x2; }
    },
    DATA_RATE {
        @Override public byte opcode() { return 0x3; }
    },
    DATA_REPETITIONS {
        @Override public byte opcode() { return 0x4; }
    },
    MAG_DATA {
        @Override public byte opcode() { return 0x5; }
    },
    THRESHOLD_INTERRUPT_ENABLE {
        @Override public byte opcode() { return 0x6; }
    },
    THRESHOLD_CONFIG {
        @Override public byte opcode() { return 0x7; }
    },
    THRESHOLD_INTERRUPT {
        @Override public byte opcode() { return 0x8; }
    },
    PACKED_MAG_DATA {
        @Override public byte opcode() { return 0x9; }
    };

    @Override
    public byte moduleOpcode() { return InfoRegister.MAGNETOMETER.moduleOpcode(); }
}
