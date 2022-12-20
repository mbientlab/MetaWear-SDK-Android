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

import com.mbientlab.metawear.module.Timer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * Created by etsai on 3/13/17.
 */
public class TestDeserialization {
    public static class TestDeserializeI2C extends TestI2C {
        @Override
        protected Task<Route> setupI2cRoute() {
            junitPlatform.boardStateSuffix = "i2c_stream";
            try {
                mwBoard.deserialize();
                return Task.forResult(mwBoard.lookupRoute(0));
            } catch (IOException | ClassNotFoundException e) {
                return Task.forError(e);
            }


        }
    }

    public static class TestDeserializeLoggingData extends TestLoggingData {
        @Override
        protected Task<Route> setupLogDataRoute() {
            try {
                junitPlatform.boardStateSuffix = "log_acc";
                mwBoard.deserialize();

                TaskCompletionSource<Route> deserializeTask= new TaskCompletionSource<>();
                deserializeTask.setResult(mwBoard.lookupRoute((byte) 0));
                return deserializeTask.getTask();
            } catch (IOException | ClassNotFoundException e) {
                return Task.forError(e);
            }
        }

        @Override
        protected Task<Route> setupLogOffsetRoute() {
            try {
                junitPlatform.boardStateSuffix= "log_offset";
                mwBoard.deserialize();

                TaskCompletionSource<Route> deserializeTask= new TaskCompletionSource<>();
                deserializeTask.setResult(mwBoard.lookupRoute((byte) 0));
                return deserializeTask.getTask();
            } catch (IOException | ClassNotFoundException e) {
                return Task.forError(e);
            }
        }
    }

    public static class TestDeserializeSPI extends TestSPI {
        @Override
        protected Task<Route> setupSpiStream() {
            try {
                junitPlatform.boardStateSuffix = "spi_stream";
                mwBoard.deserialize();

                return Task.forResult(mwBoard.lookupRoute(0));
            } catch (IOException | ClassNotFoundException e) {
                return Task.forError(e);
            }
        }
    }

    public static class TestDeserializeTimer extends TestTimer {
        protected Task<Timer.ScheduledTask> setupTimer() {
            try {
                junitPlatform.boardStateSuffix = "timer";
                mwBoard.deserialize();

                return Task.forResult(mwBoard.getModule(Timer.class).lookupScheduledTask((byte) 0));
            } catch (IOException | ClassNotFoundException e) {
                return Task.forError(e);
            }
        }

        @Test
        public void scheduleTasks() {
            // don't need to schedule tasks since this is a deserialization test
        }
    }

    public static class TestDeserializeObserver extends UnitTestBase {
        @BeforeEach
        public void setup() throws Exception {
            junitPlatform.boardStateSuffix = "dc_observer";
            mwBoard.deserialize();

            connectToBoard();
        }

        @Test
        public void remove() {
            byte[][] expected = new byte[][] {
                    {0x0a, 0x04, 0x00},
                    {0x0a, 0x04, 0x01}
            };
            mwBoard.lookupObserver(0).remove();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }
}
