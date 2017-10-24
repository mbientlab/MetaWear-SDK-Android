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

import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.module.ProximityTsl2671;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.PROXIMITY;

/**
 * Created by etsai on 9/19/16.
 */
class ProximityTsl2671Impl extends ModuleImplBase implements ProximityTsl2671 {
    static String createUri(DataTypeBase dataType) {
        switch (Util.clearRead(dataType.eventConfig[1])) {
            case ADC:
                return "proximity";
            default:
                return null;
        }
    }

    private final static String PRODUCER= "com.mbientlab.metawear.impl.ProximityTsl2671Impl.PRODUCER";
    private static final byte ADC= 1, MODE= 2;
    private static final long serialVersionUID = -3980380296316444383L;

    private transient ForcedDataProducer proximityProducer;

    ProximityTsl2671Impl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        mwPrivate.tagProducer(PRODUCER, new UintData(PROXIMITY, Util.setSilentRead(ADC), new DataAttributes(new byte[] {2}, (byte) 1, (byte) 0, false)));
    }

    @Override
    public ConfigEditor configure() {
        return new ConfigEditor() {
            private ReceiverDiode diode= ReceiverDiode.CHANNEL_1;
            private TransmitterDriveCurrent driveCurrent= TransmitterDriveCurrent.CURRENT_25MA;
            private byte nPulses= 1;
            private byte pTime= (byte) 0xff;

            @Override
            public ConfigEditor integrationTime(float time) {
                pTime= (byte) (256.f - time / 2.72f);
                return this;
            }

            @Override
            public ConfigEditor pulseCount(byte nPulses) {
                this.nPulses= nPulses;
                return this;
            }

            @Override
            public ConfigEditor receiverDiode(ReceiverDiode diode) {
                this.diode= diode;
                return this;
            }

            @Override
            public ConfigEditor transmitterDriveCurrent(TransmitterDriveCurrent current) {
                this.driveCurrent= current;
                return this;
            }

            @Override
            public void commit() {
                byte[] config= new byte[] {pTime, nPulses, (byte) (((diode.ordinal() + 1) << 4) | (driveCurrent.ordinal() << 6))};
                mwPrivate.sendCommand(PROXIMITY, MODE, config);
            }
        };
    }

    @Override
    public ForcedDataProducer adc() {
        if (proximityProducer == null) {
            proximityProducer = new ForcedDataProducer() {
                @Override
                public void read() {
                    mwPrivate.lookupProducer(PRODUCER).read(mwPrivate);
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, PRODUCER);
                }

                @Override
                public String name() {
                    return PRODUCER;
                }
            };
        }
        return proximityProducer;
    }
}
