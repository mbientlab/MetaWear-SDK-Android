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
import com.mbientlab.metawear.module.Gsr;

import java.io.Serializable;
import java.util.Locale;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.GSR;

/**
 * Created by etsai on 9/21/16.
 */
class GsrImpl extends ModuleImplBase implements Gsr {
    private static final long serialVersionUID = 7636307854618085010L;

    private static final String CONDUCTANCE_PRODUCER_FORMAT= "com.mbientlab.metawear.impl.GsrImpl.CONDUCTANCE_PRODUCER_%d";
    private static final byte CONDUCTANCE = 0x1, CALIBRATE = 0x2, CONFIG= 0x3;

    private static class Channel implements ForcedDataProducer, Serializable {
        private static final long serialVersionUID = 5552089355271489517L;

        private final byte id;
        private transient MetaWearBoardPrivate mwPrivate;

        Channel(byte id, MetaWearBoardPrivate mwPrivate) {
            this.id= id;
            this.mwPrivate = mwPrivate;
            mwPrivate.tagProducer(name(), new UintData(GSR, Util.setSilentRead(CONDUCTANCE), id, new DataAttributes(new byte[] {4}, (byte) 1, (byte) 0, false)));
        }

        void restoreTransientVariables(MetaWearBoardPrivate mwPrivate) {
            this.mwPrivate = mwPrivate;
        }

        @Override
        public void read() {
            mwPrivate.lookupProducer(name()).read(mwPrivate);
        }

        @Override
        public Task<Route> addRouteAsync(RouteBuilder builder) {
            return mwPrivate.queueRouteBuilder(builder, name());
        }

        @Override
        public String name() {
            return String.format(Locale.US, CONDUCTANCE_PRODUCER_FORMAT, id);
        }
    }

    private final Channel[] conductanceChannels;
    GsrImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        byte[] extra= mwPrivate.lookupModuleInfo(GSR).extra;
        conductanceChannels= new Channel[extra[0]];
        for(byte i= 0; i < extra[0]; i++) {
            conductanceChannels[i]= new Channel(i, mwPrivate);
        }
    }

    @Override
    public void restoreTransientVars(MetaWearBoardPrivate mwPrivate) {
        super.restoreTransientVars(mwPrivate);

        for(Channel it: conductanceChannels) {
            it.restoreTransientVariables(mwPrivate);
        }
    }

    @Override
    public ConfigEditor configure() {
        return new ConfigEditor() {
            private ConstantVoltage newCv= ConstantVoltage.CV_500MV;
            private Gain newGain= Gain.GSR_499K;

            @Override
            public ConfigEditor constantVoltage(ConstantVoltage cv) {
                newCv= cv;
                return this;
            }

            @Override
            public ConfigEditor gain(Gain gain) {
                newGain= gain;
                return this;
            }

            @Override
            public void commit() {
                mwPrivate.sendCommand(GSR, CONFIG, new byte[] {(byte) newCv.ordinal(), (byte) newGain.ordinal()});
            }
        };
    }

    @Override
    public Channel[] channels() {
        return conductanceChannels;
    }

    @Override
    public void calibrate() {
        mwPrivate.sendCommand(new byte[] {GSR.id, CALIBRATE});
    }
}
