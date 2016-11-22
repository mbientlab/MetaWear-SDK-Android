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
import com.mbientlab.metawear.impl.DataAttributes;
import com.mbientlab.metawear.impl.MetaWearBoardPrivate;
import com.mbientlab.metawear.impl.ModuleId;
import com.mbientlab.metawear.impl.ModuleImplBase;
import com.mbientlab.metawear.impl.UintData;
import com.mbientlab.metawear.module.Switch;

import bolts.Task;

/**
 * Created by etsai on 9/4/16.
 */
class SwitchImpl extends ModuleImplBase implements Switch {
    private final static String PRODUCER= "com.mbientlab.metawear.impl.SwitchImpl.PRODUCER";
    private final static byte STATE= 0x1;
    private static final long serialVersionUID = -6054365836900403723L;

    SwitchImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        this.mwPrivate.tagProducer(PRODUCER, new UintData(ModuleId.SWITCH, STATE, new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false)));
    }

    @Override
    public Task<Route> addRoute(RouteBuilder builder) {
        return mwPrivate.queueRouteBuilder(builder, PRODUCER);
    }

    @Override
    public String name() {
        return PRODUCER;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
