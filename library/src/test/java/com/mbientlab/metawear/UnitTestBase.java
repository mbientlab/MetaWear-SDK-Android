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

package com.mbientlab.metawear;

import com.mbientlab.metawear.JunitPlatform.MwBridge;
import com.mbientlab.metawear.impl.MetaWearBoardImpl;
import com.mbientlab.metawear.impl.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

import static com.mbientlab.metawear.MetaWearBoardInfo.*;

/**
 * Created by etsai on 9/1/16.
 */
public abstract class UnitTestBase implements MwBridge {
    public static Collection<Object[]> allBoardsParams() {
        ArrayList<Object[]> parameters= new ArrayList<>();
        for(MetaWearBoardInfo info: new MetaWearBoardInfo[] {CPRO, DETECTOR, ENVIRONMENT, RPRO, R, RG, MOTION_R}) {
            parameters.add(new Object[] {info});
        }

        return parameters;
    }

    protected final JunitPlatform btlePlaform= new JunitPlatform(this);
    protected final MetaWearBoard mwBoard= new MetaWearBoardImpl(btlePlaform, "CB:B7:49:BF:27:33");

    protected void connectToBoard() throws Exception {
        final Capture<Exception> result = new Capture<>();
        mwBoard.connectAsync().continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                if (task.isFaulted()) {
                    result.set(task.getError());
                }

                synchronized (UnitTestBase.this) {
                    UnitTestBase.this.notifyAll();
                }

                return null;
            }
        });

        synchronized (this) {
            this.wait();

            if (result.get() != null) {
                throw result.get();
            }
        }
    }

    @Override
    public void disconnected() {
        ((MetaWearBoardImpl) mwBoard).disconnected();
    }

    @Override
    public void sendMockResponse(byte[] response) {
        ((MetaWearBoardImpl) mwBoard).handleNotifyCharResponse(response);
    }

    @Override
    public void sendMockGattCharReadValue(Pair<UUID, UUID> gattChar, byte[] value) {
        ((MetaWearBoardImpl) mwBoard).handleReadGattCharResponse(gattChar, value);
    }
}
