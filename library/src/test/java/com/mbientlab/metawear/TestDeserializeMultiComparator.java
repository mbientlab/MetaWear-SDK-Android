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

import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.module.DataProcessor;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 10/31/16.
 */

public class TestDeserializeMultiComparator extends UnitTestBase {
    @Before
    public void setup() throws Exception {
        btlePlaform.firmware = "1.2.3";
        connectToBoard();


        /*
        // For editReference test
        mwBoard.getModule(Gpio.class).createVirtualPin((byte) 0x15).analogAdc().addRoute(new RouteBuilder() {
            @Override
            public void configure(RouteElement source) {
                source.filter(Comparison.GTE, ComparisonOutput.ABSOLUTE, 1024, 512, 256, 128).name("multi_comp");
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                btlePlaform.boardStateSuffix = "multi_comparator";
                mwBoard.serialize();

                synchronized (TestDeserializeMultiComparator.this) {
                    TestDeserializeMultiComparator.this.notifyAll();
                }
                return null;
            }
        });

        synchronized (this) {
            this.wait();
        }
        */
    }

    @Test
    public void editReferences() {
        byte[] expected = new byte[] {0x09, 0x05, 0x00, 0x06, 0x12, (byte) 0x80, 0x00, 0x00, 0x01};

        btlePlaform.boardStateSuffix = "multi_comparator";
        mwBoard.deserialize();

        mwBoard.getModule(DataProcessor.class).edit("multi_comp", DataProcessor.ComparatorEditor.class)
                .modify(Comparison.LT, 128, 256);

        assertArrayEquals(expected, btlePlaform.getLastCommand());
    }
}
