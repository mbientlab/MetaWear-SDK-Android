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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 10/8/16.
 */

public class TestDeserializeGpioFeedback extends UnitTestBase {
    @Before
    public void setup() throws Exception {
        connectToBoard();

        btlePlaform.boardStateSuffix = "gpio_feedback";
        mwBoard.deserialize();
    }

    @Test
    public void deserializeGpioFeedback() {
        byte[][] expected= {
                {0x09, 0x06, 0x00},
                {0x09, 0x06, 0x01},
                {0x09, 0x06, 0x02},
                {0x09, 0x06, 0x03},
                {0x09, 0x06, 0x04},
                {0x09, 0x06, 0x05},
                {0x09, 0x06, 0x06},
                {0x09, 0x06, 0x07},
                {0x0a, 0x04, 0x00},
                {0x0a, 0x04, 0x01},
                {0x0a, 0x04, 0x02},
                {0x0a, 0x04, 0x03},
                {0x0a, 0x04, 0x04},
                {0x0a, 0x04, 0x05},
                {0x0a, 0x04, 0x06}
        };

        mwBoard.lookupRoute(0).remove();

        assertArrayEquals(expected, btlePlaform.getCommands());
    }

    @Test
    public void tearDown() {
        byte[][] expected= {
                {0x09, 0x08},
                {0x0a, 0x05},
                {0x0b, 0x0a},
                {0x0c, 0x05, 0x00},
                {0x0c, 0x05, 0x01},
                {0x0c, 0x05, 0x02},
                {0x0c, 0x05, 0x03},
                {0x0c, 0x05, 0x04},
                {0x0c, 0x05, 0x05},
                {0x0c, 0x05, 0x06},
                {0x0c, 0x05, 0x07}
        };

        mwBoard.tearDown();

        assertArrayEquals(expected, btlePlaform.getCommands());
    }
}
