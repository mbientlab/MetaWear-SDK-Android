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

import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bolts.Task;

/**
 * Created by etsai on 10/30/16.
 */

public class TestGyroYLogData extends TestLogDataBase {
    @Override
    protected String logDataFilename() {
        return "bmi160_gyro_yaxis_dl";
    }

    private static final Subscriber LOG_DATA_HANDLER= (data, env) -> ((List<Float>) env[0]).add(data.value(Float.class));

    protected Task<Route> setupLogDataRoute() {
        mwBoard.getModule(Gyro.class).configure()
                .range(Gyro.Range.FSR_250)
                .commit();
        return mwBoard.getModule(Gyro.class).angularVelocity().addRouteAsync(source -> source.split().index(1).log(LOG_DATA_HANDLER));
    }

    @Test
    public void checkGyroData() throws IOException, JSONException, InterruptedException {
        final List<Float> actual = new ArrayList<>();
        final Float[] expected;

        {
            BufferedReader br = new BufferedReader(new FileReader(new File(rootPath, "bmi160_gyro_yaxis_expected_values")));
            String line;
            StringBuilder builder = new StringBuilder();

            while ((line = br.readLine()) != null) {
                builder.append(line);
            }

            JSONArray array = new JSONArray(builder.toString());
            expected = new Float[array.length()];
            for(int i= 0; i < array.length(); i++) {
                expected[i] = (float) array.getDouble(i);
            }
        }

        setupLogDataRoute().continueWith(task -> {
            task.getResult().setEnvironment(0, actual);
            return null;
        }).waitForCompletion();

        mwBoard.getModule(Logging.class).downloadAsync()
                .continueWith(task -> {
                    Float[] actualArray= new Float[actual.size()];
                    actual.toArray(actualArray);

                    assertArrayEquals(expected, actualArray);
                    return null;
                });

        for(byte[] response: downloadResponses) {
            sendMockResponse(response);
        }
    }
}
