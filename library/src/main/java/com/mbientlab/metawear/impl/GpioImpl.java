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

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.impl.DataAttributes;
import com.mbientlab.metawear.impl.MetaWearBoardPrivate;
import com.mbientlab.metawear.impl.ModuleId;
import com.mbientlab.metawear.impl.ModuleImplBase;
import com.mbientlab.metawear.impl.UintData;
import com.mbientlab.metawear.impl.Util;
import com.mbientlab.metawear.module.Gpio;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;

import bolts.Task;

import static com.mbientlab.metawear.impl.ModuleId.GPIO;

/**
 * Created by etsai on 9/6/16.
 */
class GpioImpl extends ModuleImplBase implements Gpio {
    private static final String ADC_PRODUCER_FORMAT= "com.mbientlab.metawear.impl.GpioImpl.ADC_PRODUCER_$%d",
            ABS_REF_PRODUCER_FORMAT= "com.mbientlab.metawear.impl.GpioImpl.ABS_REF_PRODUCER_$%d",
            DIGITAL_PRODUCER_FORMAT= "com.mbientlab.metawear.impl.GpioImpl.DIGITAL_PRODUCER_$%d",
            MONITOR_PRODUCER_FORMAT= "com.mbientlab.metawear.impl.GpioImpl.MONITOR_PRODUCER_$%d";
    private static final byte REVISION_ENHANCED_ANALOG= 2;
    private static final byte SET_DO = 1, CLEAR_DO = 2,
            PULL_UP_DI = 3, PULL_DOWN_DI = 4, NO_PULL_DI = 5,
            READ_AI_ABS_REF = 6, READ_AI_ADC = 7, READ_DI = 8,
            PIN_CHANGE = 9, PIN_CHANGE_NOTIFY_ENABLE = 11;
    static final byte PIN_CHANGE_NOTIFY = 10;
    private static final long serialVersionUID = 8515819183631054276L;

    private static class AnalogInner implements Analog {
        private final byte pin;
        private final String producerNameFormat;
        private final MetaWearBoardPrivate owner;

        private AnalogInner(MetaWearBoardPrivate owner, byte pin, String producerNameFormat) {
            this.owner= owner;
            this.pin= pin;
            this.producerNameFormat = producerNameFormat;
        }

        @Override
        public void read(byte pullup, byte pulldown, short delay, byte virtual) {
            if (owner.lookupModuleInfo(ModuleId.GPIO).revision >= REVISION_ENHANCED_ANALOG) {
                owner.lookupProducer(name()).read(owner, new byte[] {pullup, pulldown, (byte) (delay / 4), virtual});
            } else {
                owner.lookupProducer(name()).read(owner);
            }
        }

        @Override
        public Task<Route> addRoute(RouteBuilder builder) {
            return owner.queueRouteBuilder(builder, name());
        }

        @Override
        public String name() {
            return String.format(Locale.US, producerNameFormat, pin);
        }

        @Override
        public void read() {
            if (owner.lookupModuleInfo(ModuleId.GPIO).revision >= REVISION_ENHANCED_ANALOG) {
                owner.lookupProducer(name()).read(owner, new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0, (byte) 0xff});
            } else {
                owner.lookupProducer(name()).read(owner);
            }
        }
    }
    private static class GpioPinImpl implements Pin, Serializable {
        private static final long serialVersionUID = -7164069041020837195L;
        private final byte pin;
        private final boolean virtual;
        private transient MetaWearBoardPrivate owner;

        GpioPinImpl(byte pin, boolean virtual, MetaWearBoardPrivate owner) {
            this.pin= pin;
            this.virtual= virtual;
            this.owner= owner;
        }

        void restoreTransientVar(MetaWearBoardPrivate owner) {
            this.owner= owner;
        }

        @Override
        public boolean isVirtual() {
            return virtual;
        }

        @Override
        public void setChangeType(PinChangeType type) {
            owner.sendCommand(new byte[] {GPIO.id, PIN_CHANGE, pin, (byte) (type.ordinal() + 1)});
        }

        @Override
        public void setPullMode(PullMode mode) {
            switch (mode) {
                case PULL_UP:
                    owner.sendCommand(new byte[] {GPIO.id, PULL_UP_DI, pin});
                    break;
                case PULL_DOWN:
                    owner.sendCommand(new byte[] {GPIO.id, PULL_DOWN_DI, pin});
                    break;
                case NO_PULL:
                    owner.sendCommand(new byte[] {GPIO.id, NO_PULL_DI, pin});
                    break;
            }
        }

        @Override
        public void clearOutput() {
            owner.sendCommand(new byte[] {GPIO.id, CLEAR_DO, pin});
        }

        @Override
        public void setOutput() {
            owner.sendCommand(new byte[] {GPIO.id, SET_DO, pin});
        }

        @Override
        public Analog analogAdc() {
            Analog producer= new AnalogInner(owner, pin, ADC_PRODUCER_FORMAT);

            if (!owner.hasProducer(producer.name())) {
                owner.tagProducer(producer.name(), new UintData(GPIO, Util.setSilentRead(READ_AI_ADC), pin, new DataAttributes(new byte[] {2}, (byte) 1, (byte) 0, false)));
            }

            return producer;
        }

        @Override
        public Analog analogAbsRef() {
            Analog producer=  new AnalogInner(owner, pin, ABS_REF_PRODUCER_FORMAT);
            if (!owner.hasProducer(producer.name())) {
                owner.tagProducer(producer.name(), new UintData(GPIO, Util.setSilentRead(READ_AI_ABS_REF), pin, new DataAttributes(new byte[] {2}, (byte) 1, (byte) 0, false)));
            }

            return producer;
        }

        @Override
        public ForcedDataProducer digital() {
            ForcedDataProducer producer=  new ForcedDataProducer() {
                @Override
                public Task<Route> addRoute(RouteBuilder builder) {
                    return owner.queueRouteBuilder(builder, name());
                }

                @Override
                public String name() {
                    return String.format(Locale.US, DIGITAL_PRODUCER_FORMAT, pin);
                }

                @Override
                public void read() {
                    owner.lookupProducer(name()).read(owner);
                }
            };
            if (!owner.hasProducer(producer.name())) {
                owner.tagProducer(producer.name(), new UintData(GPIO, Util.setSilentRead(READ_DI), pin, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false)));
            }

            return producer;
        }

        @Override
        public AsyncDataProducer monitor() {
            AsyncDataProducer producer=  new AsyncDataProducer() {
                @Override
                public Task<Route> addRoute(RouteBuilder builder) {
                    return owner.queueRouteBuilder(builder, name());
                }

                @Override
                public String name() {
                    return String.format(Locale.US, MONITOR_PRODUCER_FORMAT, pin);
                }

                @Override
                public void start() {
                    owner.sendCommand(new byte[]{GPIO.id, PIN_CHANGE_NOTIFY_ENABLE, pin, 1});
                }

                @Override
                public void stop() {
                    owner.sendCommand(new byte[]{GPIO.id, PIN_CHANGE_NOTIFY_ENABLE, pin, 0});
                }
            };
            if (!owner.hasProducer(producer.name())) {
                owner.tagProducer(producer.name(), new UintData(GPIO, Util.setSilentRead(READ_DI), pin, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false)));
            }
            owner.tagProducer(producer.name(), new UintData(GPIO, PIN_CHANGE_NOTIFY, pin, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false)));

            return producer;
        }
    }

    private final HashMap<Byte, GpioPinImpl> gpioPins= new HashMap<>();

    GpioImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
    }

    @Override
    public void restoreTransientVars(MetaWearBoardPrivate mwPrivate) {
        super.restoreTransientVars(mwPrivate);

        for(GpioPinImpl it: gpioPins.values()) {
            it.restoreTransientVar(mwPrivate);
        }
    }

    @Override
    public Pin createVirtualPin(byte pin) {
        if (!gpioPins.containsKey(pin)) {
            gpioPins.put(pin, new GpioPinImpl(pin, true, mwPrivate));
        }
        return gpioPins.get(pin);
    }

    @Override
    public Pin getPin(byte pin) {
        if (!gpioPins.containsKey(pin)) {
            gpioPins.put(pin, new GpioPinImpl(pin, false, mwPrivate));
        }
        return gpioPins.get(pin);
    }
}
