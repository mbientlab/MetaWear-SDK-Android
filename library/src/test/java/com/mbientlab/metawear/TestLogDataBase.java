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

import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by etsai on 10/30/16.
 */

abstract class TestLogDataBase extends UnitTestBase {
    static final File rootPath = new File("src/test/res");

    static Acceleration[] readAccelerationValues(String filename) throws IOException, JSONException {
        List<Acceleration> expectedList = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(new File(rootPath, filename)));
        String line;

        while ((line = br.readLine()) != null) {
            JSONArray array = new JSONArray(line);
            int[] rawBytes = new int[array.length()];
            for (int i = 0; i < array.length(); i++) {
                rawBytes[i] = array.getInt(i);
            }

            expectedList.add(new Acceleration(Float.intBitsToFloat(rawBytes[0]), Float.intBitsToFloat(rawBytes[1]), Float.intBitsToFloat(rawBytes[2])));
        }

        Acceleration[] expected = new Acceleration[expectedList.size()];
        expectedList.toArray(expected);

        return expected;
    }

    static long[] readOffsetData(String filename) throws IOException, JSONException {
        BufferedReader br = new BufferedReader(new FileReader(new File(rootPath, filename)));
        JSONArray array = new JSONArray(br.readLine());
        long[] expected= new long[array.length()];

        for (int i = 0; i < array.length(); i++) {
            expected[i] = array.getInt(i);
        }

        return expected;
    }

    List<byte[]> downloadResponses;

    protected abstract String logDataFilename();

    @BeforeEach
    public void setup() throws Exception {
        junitPlatform.boardInfo= new MetaWearBoardInfo(AccelerometerBmi160.class, Gyro.class, Logging.class);
        connectToBoard();

        downloadResponses= new ArrayList<>();
        BufferedReader br= new BufferedReader(new FileReader(new File(rootPath, logDataFilename())));
        String line;
        while((line= br.readLine()) != null) {
            JSONArray array= new JSONArray(line);
            byte[] rawBytes= new byte[array.length()];
            for(int i= 0; i < array.length(); i++) {
                rawBytes[i]= (byte) array.getInt(i);
            }

            downloadResponses.add(rawBytes);
        }
    }
}
