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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Logging;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bolts.Task;


/**
 * Created by etsai on 9/3/16.
 */
public class TestLoggingData extends TestLogDataBase {
    @Override
    protected String logDataFilename() {
        return "bmi160_log_dl";
    }

    protected Task<Route> setupLogDataRoute() {
        mwBoard.getModule(Accelerometer.class).configure()
                .range(8f)
                .commit();
        return mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source ->
                source.log((data, env) -> ((List<Acceleration>) env[0]).add(data.value(Acceleration.class)))
        );
    }

    @Test
    public void checkAccelerometerData() throws InterruptedException, IOException, JSONException {
        final List<Acceleration> actual= new ArrayList<>();
        final Acceleration[] expected = readAccelerationValues("bmi160_expected_values");

        Task<Route> task =setupLogDataRoute();
        task.waitForCompletion();
        task.getResult().setEnvironment(0, actual);

        // For TestDeserializeLoggingData
        junitPlatform.boardStateSuffix = "log_acc";
        mwBoard.serialize();

        Task<Void> dlTask = mwBoard.getModule(Logging.class).downloadAsync();
        for(byte[] response: downloadResponses) {
            sendMockResponse(response);
        }
        dlTask.waitForCompletion();

        Acceleration[] actualArray= new Acceleration[actual.size()];
        actual.toArray(actualArray);

        assertArrayEquals(expected, actualArray);
    }

    private static Data offset_handler_prev = null;
    private static int offset_handler_i = 0;
    private static final Subscriber LOG_TIME_OFFSET_HANDLER= (data, env) -> {
        if (offset_handler_prev != null) {
            ((long[]) env[0])[offset_handler_i++]= data.timestamp().getTimeInMillis() - offset_handler_prev.timestamp().getTimeInMillis();
        }
        offset_handler_prev = data;
    };
    private static void resetOffsetHandlerState() {
        offset_handler_prev = null;
        offset_handler_i = 0;
    }

    protected Task<Route> setupLogOffsetRoute() {
        return mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.log(LOG_TIME_OFFSET_HANDLER));
    }

    @Test
    public void checkTimeOffsets() throws Exception {
        resetOffsetHandlerState();

        final long[] expected = readOffsetData("bmi160_expected_offsets"), actual = new long[expected.length];

        Task<Route> task = setupLogOffsetRoute();
        task.waitForCompletion();
        if (task.isFaulted()) {
            throw task.getError();
        }
        task.getResult().setEnvironment(0, (Object) actual);

        // For TestDeserializeLoggingData
        junitPlatform.boardStateSuffix = "log_offset";
        mwBoard.serialize();

        Task<Void> dlTask = mwBoard.getModule(Logging.class).downloadAsync();
        for(byte[] response: downloadResponses) {
            sendMockResponse(response);
        }
        dlTask.waitForCompletion();

        assertArrayEquals(expected, actual);
    }

    @Test
    public void checkRollover() throws Exception {
        resetOffsetHandlerState();

        Task<Route> task = setupLogOffsetRoute();
        task.waitForCompletion();

        final long[] actual = new long[1];
        if (task.isFaulted()) {
            throw task.getError();
        }
        task.getResult().setEnvironment(0, (Object) actual);

        Task<Void> dlTask = mwBoard.getModule(Logging.class).downloadAsync();

        sendMockResponse(new byte[] {0x0b, (byte) 0x84, 0x15, 0x04, 0x00, 0x00, 0x05});
        sendMockResponse(new byte[] { 11, 7,
                -95, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, -111, -17, 0, 0,
                -96, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, -128, -1, -73, -1 });
        sendMockResponse(new byte[] { 11, 7, -95, 13, 0, 0, 0, 116, -17, 0, 0, -96, 13, 0, 0, 0, 125, -1, -70, -1 });
        sendMockResponse(new byte[] { 11, 8, 0, 0, 0, 0});
        dlTask.waitForCompletion();

        assertArrayEquals(new long[] { 21 }, actual);
    }
}
