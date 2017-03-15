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

import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;

import org.json.JSONException;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import bolts.Continuation;
import bolts.Task;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 9/3/16.
 */
public class TestLoggingData extends TestLogDataBase {
    private final AtomicBoolean shouldWait= new AtomicBoolean();

    @Override
    protected String logDataFilename() {
        return "bmi160_log_dl";
    }

    private static final Subscriber LOG_DATA_HANDLER= new Subscriber() {
        @Override
        public void apply(Data data, Object ... env) {
            ((List<Acceleration>) env[0]).add(data.value(Acceleration.class));
        }
    };

    protected Task<Route> setupLogDataRoute() {
        shouldWait.set(true);
        mwBoard.getModule(Accelerometer.class).configure()
                .range(8f)
                .commit();
        return mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.log(LOG_DATA_HANDLER);
            }
        });
    }

    @Test
    public void checkAccelerometerData() throws InterruptedException, IOException, JSONException {
        final List<Acceleration> actual= new ArrayList<>();
        final Acceleration[] expected = readAccelerationValues("bmi160_expected_values");

        setupLogDataRoute().continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                task.getResult().setEnvironment(0, actual);
                synchronized (TestLoggingData.this) {
                    TestLoggingData.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            if (shouldWait.get()) {
                this.wait();
            }

            // For TestDeserializeLoggingData
            junitPlatform.boardStateSuffix = "log_acc";
            mwBoard.serialize();

            mwBoard.getModule(Logging.class).downloadAsync()
                    .continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(Task<Void> task) throws Exception {
                            Acceleration[] actualArray= new Acceleration[actual.size()];
                            actual.toArray(actualArray);

                            assertArrayEquals(expected, actualArray);
                            return null;
                        }
                    });

            for(byte[] response: downloadResponses) {
                sendMockResponse(response);
            }
        }
    }

    private static final Subscriber LOG_TIME_OFFSET_HANDLER= new Subscriber() {
        private transient Data prev= null;
        private transient int i= 0;

        @Override
        public void apply(Data data, Object ... env) {
            if (prev != null) {
                ((long[]) env[0])[i]= data.timestamp().getTimeInMillis() - prev.timestamp().getTimeInMillis();
                i++;
            }
            prev= data;
        }
    };

    protected Task<Route> setupLogOffsetRoute() {
        shouldWait.set(true);
        return mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.log(LOG_TIME_OFFSET_HANDLER);
            }
        });
    }

    @Test
    public void checkTimeOffsets() throws InterruptedException, IOException, JSONException {
        final long[] expected = readOffsetData("bmi160_expected_offsets"), actual = new long[expected.length];

        setupLogOffsetRoute().continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                task.getResult().setEnvironment(0, (Object) actual);

                synchronized (TestLoggingData.this) {
                    TestLoggingData.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            if (shouldWait.get()) {
                this.wait();
            }

            // For TestDeserializeLoggingData
            junitPlatform.boardStateSuffix = "log_offset";
            mwBoard.serialize();

            mwBoard.getModule(Logging.class).downloadAsync()
                    .continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(Task<Void> task) throws Exception {
                            assertArrayEquals(expected, actual);
                            return null;
                        }
                    });

            for(byte[] response: downloadResponses) {
                sendMockResponse(response);
            }
        }
    }
}
