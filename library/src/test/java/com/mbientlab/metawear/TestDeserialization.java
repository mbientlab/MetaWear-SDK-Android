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
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Timer;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;

import bolts.Task;
import bolts.TaskCompletionSource;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by etsai on 3/13/17.
 */
@RunWith(Enclosed.class)
public class TestDeserialization {
    public static class TestDeserializeGpioAnalog extends TestGpioAnalog {
        protected Task<Route> setupAbsRef() {
            try {
                junitPlatform.boardStateSuffix = "gpio_analog";
                mwBoard.deserialize();

                return Task.forResult(mwBoard.lookupRoute(0));
            } catch (IOException | ClassNotFoundException e) {
                return Task.forError(e);
            }
        }

        protected Task<Route> setupAdc() {
            return Task.forResult(mwBoard.lookupRoute(1));
        }
    }

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

    public static class TestDeserializeGpioFeedback extends UnitTestBase {
        @Before
        public void setup() throws Exception {
            junitPlatform.firmware= "1.1.3";
            junitPlatform.boardStateSuffix = "gpio_feedback";
            mwBoard.deserialize();

            connectToBoard();
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

            assertArrayEquals(expected, junitPlatform.getCommands());
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

            assertArrayEquals(expected, junitPlatform.getCommands());
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

    public static class TestDeserializeMultiComparator extends UnitTestBase {
        @Before
        public void setup() throws Exception {
            junitPlatform.boardInfo = new MetaWearBoardInfo(Gpio.class);
            junitPlatform.firmware = "1.2.3";
            connectToBoard();


        /*
        // For editReference test
        mwBoard.getModule(Gpio.class).getVirtualPin((byte) 0x15).analogAdc().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.filter(Comparison.GTE, ComparisonOutput.ABSOLUTE, 1024, 512, 256, 128).name("multi_comp");
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                junitPlatform.boardStateSuffix = "multi_comparator";
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
        public void editReferences() throws IOException, ClassNotFoundException {
            byte[] expected = new byte[] {0x09, 0x05, 0x00, 0x06, 0x12, (byte) 0x80, 0x00, 0x00, 0x01};

            junitPlatform.boardStateSuffix = "multi_comparator";
            mwBoard.deserialize();

            mwBoard.getModule(DataProcessor.class).edit("multi_comp", DataProcessor.ComparatorEditor.class)
                    .modify(Comparison.LT, 128, 256);

            assertArrayEquals(expected, junitPlatform.getLastCommand());
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
        @Before
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
