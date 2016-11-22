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

import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.module.SerialPassthrough;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bolts.Task;

import static com.mbientlab.metawear.impl.ModuleId.DATA_PROCESSOR;
import static com.mbientlab.metawear.impl.ModuleId.SERIAL_PASSTHROUGH;

/**
 * Created by etsai on 10/3/16.
 */
class SerialPassthroughImpl extends ModuleImplBase implements SerialPassthrough {
    private final static byte SPI_REVISION= 1;
    private static final byte I2C_RW = 0x1, SPI_RW = 0x2;
    private static final String I2C_PRODUCER_FORMAT= "com.mbientlab.metawear.impl.SerialPassthroughImpl.I2C_PRODUCER_%d",
            SPI_PRODUCER_FORMAT= "com.mbientlab.metawear.impl.SerialPassthroughImpl.SPI_PRODUCER_%d";
    private static final long serialVersionUID = 3950502593880962546L;

    private static class SerialPassthroughData extends ByteArrayData {
        private static final long serialVersionUID = 2683337991563998545L;

        SerialPassthroughData(byte register, byte id, byte length) {
            super(SERIAL_PASSTHROUGH, register, id, new DataAttributes(new byte[] {length}, (byte) 1, (byte) 0, false));
        }

        SerialPassthroughData(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        public DataTypeBase dataProcessorCopy(DataTypeBase input, DataAttributes attributes) {
            return new ByteArrayData(input, DATA_PROCESSOR, DataProcessorImpl.NOTIFY, NO_DATA_ID, attributes);
        }
        public DataTypeBase dataProcessorStateCopy(DataTypeBase input, DataAttributes attributes) {
            return new ByteArrayData(input, DATA_PROCESSOR, Util.setSilentRead(DataProcessorImpl.STATE), NO_DATA_ID, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            return new SerialPassthroughData(input, module, register, id, attributes);
        }

        @Override
        public void read(MetaWearBoardPrivate mwPrivate) {
            throw new UnsupportedOperationException("Serial passthrough reads require parameters");
        }

        @Override
        public void read(MetaWearBoardPrivate mwPrivate, byte[] parameters) {
            byte[] command= new byte[eventConfig.length - 1 + parameters.length];
            System.arraycopy(eventConfig, 0, command, 0, 2);
            System.arraycopy(parameters, 0, command, 2, parameters.length);

            mwPrivate.sendCommand(command);
        }
    }
    private static class I2cInner implements I2c, Serializable {
        private static final long serialVersionUID = 8518548885863146365L;
        private final byte id;
        transient MetaWearBoardPrivate owner;

        I2cInner(byte id, byte length, MetaWearBoardPrivate owner) {
            this.id= id;
            this.owner= owner;

            owner.tagProducer(name(), new SerialPassthroughData(Util.setSilentRead(I2C_RW), id, length));
        }

        void restoreTransientVars(MetaWearBoardPrivate owner) {
            this.owner= owner;
        }

        @Override
        public void read(byte deviceAddr, byte registerAddr) {
            DataTypeBase i2cProducer= owner.lookupProducer(name());
            i2cProducer.read(owner, new byte[]{deviceAddr, registerAddr, id, i2cProducer.attributes.length()});
        }

        @Override
        public Task<Route> addRoute(RouteBuilder builder) {
            return owner.queueRouteBuilder(builder, name());
        }

        @Override
        public String name() {
            return String.format(Locale.US, I2C_PRODUCER_FORMAT, id);
        }

        @Override
        public void read() {
            throw new UnsupportedOperationException("Default read function not supported for I2C, use read(byte, byte) instead");
        }
    }
    private static class SpiInner implements Spi, Serializable {
        private static final long serialVersionUID = -1781850442398737602L;

        private final byte id;
        transient MetaWearBoardPrivate owner;

        SpiInner(byte id, byte length, MetaWearBoardPrivate owner) {
            this.id= id;
            this.owner= owner;

            owner.tagProducer(name(), new SerialPassthroughData(Util.setSilentRead(SPI_RW), id, length));
        }

        void restoreTransientVars(MetaWearBoardPrivate owner) {
            this.owner= owner;
        }

        @Override
        public SpiParameterBuilder read(byte[] data) {
            final DataTypeBase spiProducer= owner.lookupProducer(name());
            final byte[] readConfig= new byte[6 + (data == null ? 0 : data.length)];
            if (data != null) {
                System.arraycopy(data, 0, readConfig, 6, data.length);
            }
            readConfig[5]= (byte) ((spiProducer.attributes.length() - 1) | (id << 4));

            return new SpiParameterBuilderInner(readConfig) {
                @Override
                public void commit() {
                    spiProducer.read(owner, config);
                }
            };
        }

        @Override
        public Task<Route> addRoute(RouteBuilder builder) {
            return owner.queueRouteBuilder(builder, name());
        }

        @Override
        public String name() {
            return String.format(Locale.US, SPI_PRODUCER_FORMAT, id);
        }

        @Override
        public void read() {
            throw new UnsupportedOperationException("Default read function not supported for SPI, use read(byte, byte) instead");
        }
    }

    private static abstract class SpiParameterBuilderInner implements SpiParameterBuilder {
        final byte[] config;

        SpiParameterBuilderInner(byte[] config) {
            this.config= config;
        }

        @Override
        public SpiParameterBuilder slaveSelectPin(byte pin) {
            config[0]= pin;
            return this;
        }

        @Override
        public SpiParameterBuilder clockPin(byte pin) {
            config[1]= pin;
            return this;
        }

        @Override
        public SpiParameterBuilder mosiPin(byte pin) {
            config[2]= pin;
            return this;
        }

        @Override
        public SpiParameterBuilder misoPin(byte pin) {
            config[3]= pin;
            return this;
        }

        @Override
        public SpiParameterBuilder lsbFirst() {
            config[4]|= 0x1;
            return this;
        }

        @Override
        public SpiParameterBuilder mode(byte mode) {
            config[4]|= (mode << 1);
            return this;
        }

        @Override
        public SpiParameterBuilder frequency(SpiFrequency freq) {
            config[4]|= (freq.ordinal() << 3);
            return this;
        }

        @Override
        public SpiParameterBuilder useNativePins() {
            config[4]|= (0x1 << 6);
            return this;
        }
    }

    private Map<Byte, I2c> i2cDataProducers= new ConcurrentHashMap<>();
    private Map<Byte, Spi> spiDataProducers = new ConcurrentHashMap<>();

    SerialPassthroughImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
    }

    @Override
    public void restoreTransientVars(MetaWearBoardPrivate mwPrivate) {
        super.restoreTransientVars(mwPrivate);

        for(I2c it: i2cDataProducers.values()) {
            ((I2cInner) it).restoreTransientVars(mwPrivate);
        }

        for(Spi it: spiDataProducers.values()) {
            ((SpiInner) it).restoreTransientVars(mwPrivate);
        }
    }

    @Override
    public I2c i2cData(final byte length, final byte id) {
        if (!i2cDataProducers.containsKey(id)) {
            i2cDataProducers.put(id, new I2cInner(id, length, mwPrivate));
        }

        return i2cDataProducers.get(id);
    }

    @Override
    public void writeI2c(byte deviceAddr, byte registerAddr, byte[] data) {
        byte[] params= new byte[data.length + 4];
        params[0]= deviceAddr;
        params[1]= registerAddr;
        params[2]= (byte) 0xff;
        params[3]= (byte) data.length;
        System.arraycopy(data, 0, params, 4, data.length);

        mwPrivate.sendCommand(SERIAL_PASSTHROUGH, I2C_RW, params);
    }

    @Override
    public Spi spiData(final byte length, final byte id) {
        if (mwPrivate.lookupModuleInfo(ModuleId.SERIAL_PASSTHROUGH).revision < SPI_REVISION) {
            return null;
        }

        if (!spiDataProducers.containsKey(id)) {
            spiDataProducers.put(id, new SpiInner(id, length, mwPrivate));
        }

        return spiDataProducers.get(id);
    }

    @Override
    public SpiParameterBuilder writeSpi(byte[] data) {
        final byte[] writeConfig= new byte[data.length + 5];
        System.arraycopy(data, 0, writeConfig, 5, data.length);

        return new SpiParameterBuilderInner(writeConfig) {
            @Override
            public void commit() {
                if (mwPrivate.lookupModuleInfo(ModuleId.SERIAL_PASSTHROUGH).revision >= SPI_REVISION) {
                    mwPrivate.sendCommand(SERIAL_PASSTHROUGH, SPI_RW, config);
                }
            }
        };
    }
}
