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

import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.module.IBeacon;
import com.mbientlab.metawear.module.Switch;

import org.junit.Before;
import org.junit.Test;

import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 10/18/16.
 */
public class TestIBeacon extends UnitTestBase {
    private IBeacon ibeacon;

    @Before
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(Switch.class, IBeacon.class);
        connectToBoard();

        ibeacon= mwBoard.getModule(IBeacon.class);
    }

    @Test
    public void setMajorFeedback() throws InterruptedException {
        byte[][] expected= new byte[][] {
                {0x09, 0x02, 0x01, 0x01, (byte) 0xff, 0x00, 0x02, 0x13},
                {0x0a, 0x02, 0x09, 0x03, 0x00, 0x07, 0x03, 0x02, 0x09, 0x00},
                {0x0a, 0x03, 0x00, 0x00},
                {0x07, 0x01, 0x01}
        };

        Switch mwSwitch= mwBoard.getModule(Switch.class);
        mwSwitch.state().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.count().react(new RouteComponent.Action() {
                    @Override
                    public void execute(DataToken token) {
                        ibeacon.configure()
                                .major(token)
                                .commit();
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                ibeacon.enable();
                synchronized (TestIBeacon.this) {
                    TestIBeacon.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }
}
