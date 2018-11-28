/*
 * Copyright 2014-2018 MbientLab Inc. All rights reserved.
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
 *   hello@mbientlab.com.
 */

package com.mbientlab.metawear.impl;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.module.DataProcessor;

import java.util.Calendar;

public class ArrayData extends DataTypeBase {
    private static final long serialVersionUID = 4427138245810712009L;

    ArrayData(Constant.Module module, byte register, byte id, DataAttributes attributes) {
        super(module, register, id, attributes);
    }

    ArrayData(DataTypeBase input, Constant.Module module, byte register, DataAttributes attributes) {
        super(input, module, register, attributes);
    }

    ArrayData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
        super(input, module, register, id, attributes);
    }

    @Override
    public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
        return new ArrayData(input, module, register, id, attributes);
    }

    @Override
    public Number convertToFirmwareUnits(MetaWearBoardPrivate mwPrivate, Number value) {
        return value;
    }

    @Override
    public Data createMessage(boolean logData, final MetaWearBoardPrivate mwPrivate, final byte[] data, final Calendar timestamp, DataPrivate.ClassToObject mapper) {
        DataProcessorImpl dpModules = (DataProcessorImpl) mwPrivate.getModules().get(DataProcessor.class);
        DataProcessorImpl.Processor fuser = dpModules.activeProcessors.get(eventConfig[2]);

        while(!(fuser.editor.configObj instanceof DataProcessorConfig.Fuser)) {
            fuser = dpModules.activeProcessors.get(fuser.editor.source.input.eventConfig[2]);
        }

        DataTypeBase source = fuser.editor.source.input == null ? fuser.editor.source : fuser.editor.source.input;
        int offset = 0;
        final Data[] unwrappedData = new Data[fuser.editor.config.length + 1];
        unwrappedData[0] = source.createMessage(logData, mwPrivate, data, timestamp, mapper);
        offset+= source.attributes.length();

        for(int i = 2; i < fuser.editor.config.length; i++) {
            DataProcessorImpl.Processor value = dpModules.activeProcessors.get(fuser.editor.config[i]);
            // buffer state holds the actual data type
            byte[] portion = new byte[value.state.attributes.length()];

            System.arraycopy(data, offset, portion, 0, portion.length);
            unwrappedData[i - 1] = value.state.createMessage(logData, mwPrivate, portion, timestamp, mapper);

            offset+= value.state.attributes.length();
            i++;
        }

        return new DataPrivate(timestamp, data, mapper) {
            @Override
            public Class<?>[] types() {
                return new Class<?>[] { Data[].class };
            }

            @Override
            public float scale() {
                return ArrayData.this.scale(mwPrivate);
            }

            @Override
            public <T> T value(Class<T> clazz) {
                if (clazz.equals(Data[].class)) {
                    return clazz.cast(unwrappedData);
                }
                return super.value(clazz);
            }
        };
    }
}
