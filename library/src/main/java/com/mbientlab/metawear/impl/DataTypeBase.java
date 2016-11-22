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

import com.mbientlab.metawear.DataToken;
import com.mbientlab.metawear.Data;

import java.io.Serializable;
import java.util.Calendar;

import static com.mbientlab.metawear.impl.ModuleId.DATA_PROCESSOR;

/**
 * Created by etsai on 9/4/16.
 */
abstract class DataTypeBase implements Serializable, DataToken {
    public static final byte NO_DATA_ID= (byte) 0xff;
    private static final long serialVersionUID = 1389028730582422419L;

    public final byte[] eventConfig;
    public final DataAttributes attributes;
    public final DataTypeBase input;
    public final DataTypeBase[] split;

    DataTypeBase(ModuleId module, byte register, byte id, DataAttributes attributes) {
        this(null, module, register, id, attributes);
    }

    DataTypeBase(ModuleId module, byte register, DataAttributes attributes) {
        this(null, module, register, attributes);
    }

    DataTypeBase(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
        this.eventConfig = new byte[] {module.id, register, id};
        this.attributes= attributes;
        this.input= input;
        this.split = createSplits();
    }

    DataTypeBase(DataTypeBase input, ModuleId module, byte register, DataAttributes attributes) {
        this(input, module, register, NO_DATA_ID, attributes);
    }

    Tuple3<Byte, Byte, Byte> eventConfigAsTuple() {
        return new Tuple3<>(eventConfig[0], eventConfig[1], eventConfig[2]);
    }

    public void read(MetaWearBoardPrivate mwPrivate) {
        if (eventConfig[2] == NO_DATA_ID) {
            mwPrivate.sendCommand(new byte[] {eventConfig[0], eventConfig[1]});
        } else {
            mwPrivate.sendCommand(eventConfig);
        }
    }
    public void read(MetaWearBoardPrivate mwPrivate, byte[] parameters) {
        byte[] command= new byte[eventConfig.length + parameters.length];
        System.arraycopy(eventConfig, 0, command, 0, eventConfig.length);
        System.arraycopy(parameters, 0, command, eventConfig.length, parameters.length);

        mwPrivate.sendCommand(command);
    }

    public abstract DataTypeBase copy(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes);
    public DataTypeBase dataProcessorCopy(DataTypeBase input, DataAttributes attributes) {
        return copy(input, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, NO_DATA_ID, attributes);
    }
    public DataTypeBase dataProcessorStateCopy(DataTypeBase input, DataAttributes attributes) {
        return copy(input, DATA_PROCESSOR, Util.setSilentRead(DataProcessorImpl.STATE), NO_DATA_ID, attributes);
    }

    public Number convertToFirmwareUnits(MetaWearBoardPrivate owner, Number input) {
        return input;
    }
    public abstract Data createMessage(boolean logData, MetaWearBoardPrivate board, byte[] data, Calendar timestamp);

    protected DataTypeBase[] createSplits() {
        return null;
    }
}
