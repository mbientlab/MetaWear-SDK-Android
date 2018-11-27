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

import com.mbientlab.metawear.Data;

import java.nio.ByteBuffer;
import java.util.Calendar;

import static com.mbientlab.metawear.impl.Constant.Module.DATA_PROCESSOR;

/**
 * Created by etsai on 9/5/16.
 */
class SFloatData extends DataTypeBase {
    private static final long serialVersionUID = -3269792750880261100L;

    SFloatData(Constant.Module module, byte register, byte id, DataAttributes attributes) {
        super(module, register, id, attributes);
    }

    SFloatData(Constant.Module module, byte register, DataAttributes attributes) {
        super(module, register, attributes);
    }

    SFloatData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
        super(input, module, register, id, attributes);
    }

    SFloatData(DataTypeBase input, Constant.Module module, byte register, DataAttributes attributes) {
        super(input, module, register, attributes);
    }

    @Override
    public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
        if (input == null) {
            if (this.input == null) {
                throw new NullPointerException("SFloatData cannot have null input variable");
            }
            return this.input.copy(null, module, register, id, attributes);
        }

        return new SFloatData(input, module, register, id, attributes);
    }

    @Override
    public Number convertToFirmwareUnits(MetaWearBoardPrivate mwPrivate, Number value) {
        return value.floatValue() * scale(mwPrivate);
    }

    @Override
    public Data createMessage(boolean logData, final MetaWearBoardPrivate mwPrivate, final byte[] data, final Calendar timestamp, DataPrivate.ClassToObject mapper) {
        final ByteBuffer buffer = Util.bytesToSIntBuffer(logData, data, attributes);
        final float scaled= buffer.getInt(0) / scale(mwPrivate);

        return new DataPrivate(timestamp, data, mapper) {
            @Override
            public float scale() {
                return SFloatData.this.scale(mwPrivate);
            }

            @Override
            public Class<?>[] types() {
                return new Class<?>[] {Float.class};
            }

            @Override
            public <T> T value(Class<T> clazz) {
                if (clazz.equals(Float.class)) {
                    return clazz.cast(scaled);
                }
                return super.value(clazz);
            }
        };
    }

    @Override
    Pair<? extends DataTypeBase, ? extends DataTypeBase> dataProcessorTransform(DataProcessorConfig config, DataProcessorImpl dpModule) {
        switch(config.id) {
            case DataProcessorConfig.Maths.ID: {
                DataProcessorConfig.Maths casted = (DataProcessorConfig.Maths) config;
                switch(casted.op) {
                    case ABS_VALUE: {
                        DataAttributes copy= attributes.dataProcessorCopySigned(false);
                        return new Pair<>(new UFloatData(this, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, copy), null);
                    }
                }
                break;
            }
        }
        return super.dataProcessorTransform(config, dpModule);
    }
}
