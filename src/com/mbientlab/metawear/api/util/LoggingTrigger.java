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
package com.mbientlab.metawear.api.util;

import java.util.HashMap;

import com.mbientlab.metawear.api.Register;
import com.mbientlab.metawear.api.controller.Accelerometer;
import com.mbientlab.metawear.api.controller.Logging.Trigger;
import com.mbientlab.metawear.api.controller.MechanicalSwitch;
import com.mbientlab.metawear.api.controller.Temperature;

/**
 * Collection of predefined triggers for the logging module.  These triggers can also be used 
 * for the DataProcessor class.
 * @author Eric Tsai
 */
public enum LoggingTrigger implements Trigger {
    /** Logs events for the push button switch */
    SWITCH {
        @Override public Register register() { return MechanicalSwitch.Register.SWITCH_STATE; }
    },
    TEMPERATURE {
        @Override public Register register() { return Temperature.Register.TEMPERATURE; }
        @Override public byte length() {return 2;}
    },
    /** Logs the X axis values of the accelerometer data output */
    ACCELEROMETER_X_AXIS {
        @Override public Register register() { return Accelerometer.Register.DATA_VALUE; }
        @Override public byte length() { return 2; }
    },
    ACCELEROMETER_XY_AXIS {
        @Override public Register register() { return Accelerometer.Register.DATA_VALUE; }
        @Override public byte length() { return 4; }
    },
    /** Logs the Y axis values of the accelerometer data output */
    ACCELEROMETER_Y_AXIS {
        @Override public Register register() { return Accelerometer.Register.DATA_VALUE; }
        @Override public byte offset() { return 2; }
        @Override public byte length() { return 2; }
    },
    ACCELEROMETER_YZ_AXIS {
        @Override public Register register() { return Accelerometer.Register.DATA_VALUE; }
        @Override public byte offset() { return 2; }
        @Override public byte length() { return 4; }
    },
    /** Logs the Z axis values of the accelerometer data output */
    ACCELEROMETER_Z_AXIS {
        @Override public Register register() { return Accelerometer.Register.DATA_VALUE; }
        @Override public byte offset() { return 4; }
        @Override public byte length() { return 2; }
    },
    ACCELEROMETER_ORIENTATION {
        @Override public Register register() { return Accelerometer.Register.ORIENTATION_VALUE; }
    };
    
    /**
     * Converts bytes describing a trigger to its corresponding enum entry 
     * @param triggerBytes Bytes describing a trigger
     * @return Corresponding enum entry, or null if none is found
     */
    public static Trigger lookupTrigger(byte[] triggerBytes) {
        return bytesToTrigger.get(triggerBytes);
    }
    
    private final static HashMap<byte[], Trigger> bytesToTrigger;
    
    static {
        bytesToTrigger= new HashMap<>();
        for(LoggingTrigger it: LoggingTrigger.values()) {
            bytesToTrigger.put(new byte[] {it.register().module().opcode, it.register().opcode(), 
                    it.index(), it.offset(), it.length()}, it);
        }
    }

    /* (non-Javadoc)
     * @see com.mbientlab.metawear.api.controller.Logging.Trigger#index()
     */
    @Override public byte index() { return (byte) 0xff; }

    /* (non-Javadoc)
     * @see com.mbientlab.metawear.api.controller.Logging.Trigger#offset()
     */
    @Override public byte offset() { return 0; }

    /* (non-Javadoc)
     * @see com.mbientlab.metawear.api.controller.Logging.Trigger#length()
     */
    @Override public byte length() { return 1; }

}
