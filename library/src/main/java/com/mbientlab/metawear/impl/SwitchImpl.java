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

import com.mbientlab.metawear.ActiveDataProducer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.impl.platform.TimedTask;
import com.mbientlab.metawear.module.Switch;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.SWITCH;

/**
 * Created by etsai on 9/4/16.
 */
class SwitchImpl extends ModuleImplBase implements Switch {
    static String createUri(DataTypeBase dataType) {
        switch (Util.clearRead(dataType.eventConfig[1])) {
            case STATE:
                return "switch";
            default:
                return null;
        }
    }

    private final static String PRODUCER= "com.mbientlab.metawear.impl.SwitchImpl.PRODUCER";
    private final static byte STATE= 0x1;
    private static final long serialVersionUID = -6054365836900403723L;

    private transient ActiveDataProducer state;
    private transient TimedTask<Byte> stateTasks;

    SwitchImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        this.mwPrivate.tagProducer(PRODUCER, new UintData(SWITCH, STATE, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false)));
    }

    @Override
    protected void init() {
        stateTasks = new TimedTask<>();
        this.mwPrivate.addResponseHandler(new Pair<>(SWITCH.id, Util.setRead(STATE)), response -> stateTasks.setResult(response[2]));
    }

    @Override
    public ActiveDataProducer state() {
        if (state == null) {
            state = new ActiveDataProducer() {
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
        return state;
    }

    @Override
    public Task<Byte> readCurrentStateAsync() {
        return stateTasks.execute("Did not received button state within %dms",  Constant.RESPONSE_TIMEOUT,
                () -> mwPrivate.sendCommand(new byte[] {SWITCH.id, Util.setRead(STATE)}));
    }
}
