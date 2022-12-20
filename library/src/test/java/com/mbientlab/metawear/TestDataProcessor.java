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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.Passthrough;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.builder.function.Function2;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.BarometerBmp280;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.DataProcessor.PassthroughEditor;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Temperature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import bolts.Capture;
import bolts.Task;

/**
 * Created by etsai on 9/5/16.
 */
public class TestDataProcessor {
    static class TestBase extends UnitTestBase {
        @BeforeEach
        public void setup() throws Exception {
            junitPlatform.boardInfo= new MetaWearBoardInfo(Switch.class, Led.class, BarometerBmp280.class, AccelerometerBmi160.class, Gyro.class, Temperature.class);
            junitPlatform.firmware= "1.2.5";
            connectToBoard();
        }
    }

    public static class TestRms extends TestBase {
        @Test
        public void createLog() throws InterruptedException {
            byte[][] expected= new byte[][] {
                    {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x00},
                    {0x0b, 0x02, 0x09, 0x03, 0x00, 0x20}
            };

            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.map(Function1.RMS).log(null)).waitForCompletion();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    public static class TestAccShift extends TestBase {
        @Test
        public void accLogRightShift() throws InterruptedException {
            byte[][] expected = new byte[][] {
                    {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x09, 0x14, 0x08, 0x08, 0x00, 0x00, 0x00, 0x02},
                    {0x0b, 0x02, 0x09, 0x03, 0x00, 0x40}
            };

            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.map(Function2.RIGHT_SHIFT, 8).log(null)).waitForCompletion();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }

        @Test
        public void accRightShiftData() throws InterruptedException {
            float[] expected = new float[] {1.969f, 0.812f, 0.984f};
            final float[] actual = new float[3];

            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.map(Function2.RIGHT_SHIFT, 8).stream((data, env) -> {
                byte[] bytes = data.bytes();
                for(int i = 0; i < bytes.length; i++) {
                    actual[i] = (bytes[i] << 8) / data.scale();
                }
            })).waitForCompletion();

            sendMockResponse(new byte[] {0x09, 0x03, 0x00, 126, 52, 63});
            assertArrayEquals(expected, actual, 0.001f);
        }
    }

    public static class TestAccumulator extends TestBase {
        @Test
        public void setSum() throws InterruptedException {
            byte[] expected = new byte[] {0x09, 0x04, 0x01, 0x00, 0x00, 0x71, 0x02};

            mwBoard.getModule(Accelerometer.class).configure()
                    .range(16f)
                    .odr(100f)
                    .commit();
            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source ->
                    source.map(Function1.RMS).accumulate().name("rms_acc")
            ).waitForCompletion();

            mwBoard.getModule(DataProcessor.class).edit("rms_acc", DataProcessor.AccumulatorEditor.class).set(20000f);
            assertArrayEquals(expected, junitPlatform.getLastCommand());
        }
    }

    public static class TestFreefall extends TestBase {
        @BeforeEach
        public void setup() throws Exception {
            junitPlatform.addCustomModuleInfo(new byte[] {0x09, (byte) 0x80, 0x00, 0x01, 0x1c});
            super.setup();

            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source ->
                    source.map(Function1.RSS).lowpass((byte) 4).filter(ThresholdOutput.BINARY, 0.5f)
                        .multicast()
                            .to().filter(Comparison.EQ, -1).log(null)
                            .to().filter(Comparison.EQ, 1).log(null)
                        .end()
            ).waitForCompletion();
        }

        @Test
        public void create() {
            byte[][] expected= new byte[][] {
                    {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x01},
                    {0x09, 0x02, 0x09, 0x03, 0x00, 0x20, 0x03, 0x05, 0x04},
                    {0x09, 0x02, 0x09, 0x03, 0x01, 0x20, 0x0d, 0x09, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00},
                    {0x09, 0x02, 0x09, 0x03, 0x02, 0x00, 0x06, 0b00000001, (byte) 0xff},
                    {0x09, 0x02, 0x09, 0x03, 0x02, 0x00, 0x06, 0b00000001, (byte) 0x01},
                    {0x0b, 0x02, 0x09, 0x03, 0x03, 0x00},
                    {0x0b, 0x02, 0x09, 0x03, 0x04, 0x00},
            };

            assertArrayEquals(expected, junitPlatform.getCommands());
        }

        @Test
        public void checkIdentifier() {
            assertEquals("acceleration:rss?id=0:low-pass?id=1:threshold?id=2:comparison?id=3", mwBoard.lookupRoute(0).generateIdentifier(0));
            assertEquals("acceleration:rss?id=0:low-pass?id=1:threshold?id=2:comparison?id=4", mwBoard.lookupRoute(0).generateIdentifier(1));
        }
    }

    public static class TestLedController extends TestBase {
        @Test
        public void create() throws InterruptedException {
            byte[][] expected= new byte[][] {
                    {0x09, 0x02, 0x01, 0x01, (byte) 0xff, 0x00, 0x02, 0x13},
                    {0x09, 0x02, 0x09, 0x03, 0x00, 0x60, 0x09, 0x0f, 0x04, 0x02, 0x00, 0x00, 0x00, 0x00},
                    {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0b00000110, 0x01, 0x00, 0x00, 0x00},
                    {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x06, 0b00000110, 0x00, 0x00, 0x00, 0x00},
                    {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x03, 0x0f},
                    {0x0a, 0x03, 0x02, 0x02, 0x10, 0x10, 0x00, 0x00, (byte) 0xf4, 0x01, 0x00, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00, (byte) 0xff},
                    {0x0a, 0x02, 0x09, 0x03, 0x02, 0x02, 0x01, 0x01},
                    {0x0a, 0x03, 0x01},
                    {0x0a, 0x02, 0x09, 0x03, 0x03, 0x02, 0x02, 0x01},
                    {0x0a, 0x03, 0x01}
            };

            RouteCreator.createLedController(mwBoard).waitForCompletion();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    public static class TestMaths extends TestBase {
        @Test
        public void tempConverter() throws InterruptedException {
            byte[][] expected = new byte[][] {
                    {0x09, 0x02, 0x04, (byte) 0x81, 0x00, 0x20, 0x09, 0x17, 0x02, 0x12, 0x00, 0x00, 0x00, 0x00},
                    {0x09, 0x02, 0x09, 0x03, 0x00, 0x60, 0x09, 0x1f, 0x03, 0x0a, 0x00, 0x00, 0x00, 0x00},
                    {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x09, 0x1f, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00},
                    {0x09, 0x02, 0x04, (byte) 0x81, 0x00, 0x20, 0x09, 0x17, 0x01, (byte) 0x89, 0x08, 0x00, 0x00, 0x00},
                    {0x09, 0x03, 0x01},
                    {0x09, 0x07, 0x02, 0x01},
                    {0x09, 0x03, 0x01},
                    {0x09, 0x07, 0x03, 0x01},
                    {0x04, (byte) 0x81, 0x00}
            };


            final Temperature.Sensor thermometer = mwBoard.getModule(Temperature.class).findSensors(Temperature.SensorType.NRF_SOC)[0];
            thermometer.addRouteAsync(source -> source.multicast()
                    .to().stream(null)
                    .to()
                        .map(Function2.MULTIPLY, 18)
                        .map(Function2.DIVIDE, 10)
                        .map(Function2.ADD, 32)
                        .stream(null)
                    .to()
                        .map(Function2.ADD, 273.15f)
                        .stream(null)
            ).waitForCompletion();
            thermometer.read();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    public static class TestPacker extends TestBase {
        @Test
        public void createTempPacker() throws InterruptedException {
            byte[] expected = new byte[] {0x9, 0x2, 0x4, (byte) 0xc1, 0x1, 0x20, 0x10, 0x1, 0x3};
            final Temperature.Sensor thermometer = mwBoard.getModule(Temperature.class).findSensors(Temperature.SensorType.PRESET_THERMISTOR)[0];

            thermometer.addRouteAsync(source -> source.pack((byte) 4)).waitForCompletion();

            assertArrayEquals(expected, junitPlatform.getLastCommand());
        }

        @Test
        public void packedTempData() throws InterruptedException {
            final float[] expected = new float[] { 30.625f, 30.125f, 30.25f, 30.25f};
            final float[] actual = new float[4];

            final Temperature.Sensor thermometer = mwBoard.getModule(Temperature.class).findSensors(Temperature.SensorType.NRF_SOC)[0];
            thermometer.addRouteAsync(source -> source.pack((byte) 4).stream(new Subscriber() {
                int i = 0;
                @Override
                public void apply(Data data, Object... env) {
                    actual[i++] = data.value(Float.class);
                }
            })).waitForCompletion();

            byte[] response = new byte[] {0x09, 0x03, 0x00, (byte) 0xf5, 0x00, (byte) 0xf1, 0x00, (byte) 0xf2, 0x00, (byte) 0xf2, 0x00};
            sendMockResponse(response);

            assertArrayEquals(expected, actual, 0.001f);
        }

        @Test
        public void countTooHigh() throws Exception {
            Task<Route> task = mwBoard.getModule(BarometerBosch.class).pressure().addRouteAsync(source -> source.pack((byte) 5));
            task.waitForCompletion();

            assertInstanceOf(IllegalRouteOperationException.class, task.getError());
        }
    }

    public static class TestAccounter extends TestBase {
        @Test
        public void createTempAccounter() throws InterruptedException {
            byte[] expected = new byte[] {0x9, 0x2, 0x3, 0x4, (byte) 0xff, (byte) 0xa0, 0x11, 0x31, 0x3};

            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(RouteComponent::account).waitForCompletion();

            assertArrayEquals(expected, junitPlatform.getLastCommand());
        }

        @Test
        public void dataExtraction() throws InterruptedException {
            final Acceleration expected = new Acceleration(Float.intBitsToFloat(0x3c410000), Float.intBitsToFloat(0x3f12c400), Float.intBitsToFloat(0xbf4b9c00));
            final Capture<Acceleration> actual = new Capture<>();

            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.account().stream((Subscriber) (data, env) -> actual.set(data.value(Acceleration.class)))).waitForCompletion();

            byte[] response = new byte[] {0x09, 0x03, 0x00, (byte) 0xa6, 0x33, 0x0d, 0x00, (byte) 0xc1, 0x00, (byte) 0xb1, 0x24, 0x19, (byte) 0xcd};
            sendMockResponse(response);

            assertEquals(expected, actual.get());
        }

        private final Subscriber timeExtractor = new Subscriber() {
            int i = 0;
            long prev = -1;
            @Override
            public void apply(Data data, Object... env) {
                if (prev == -1) {
                    prev = data.timestamp().getTimeInMillis();
                } else {
                    ((long[]) env[0])[i++] = data.timestamp().getTimeInMillis() - prev;
                    prev = data.timestamp().getTimeInMillis();
                }
            }
        };
        @Test
        public void timeExtraction() throws InterruptedException {
            long[] expected = new long[] {10, 10, 9, 10, 11};
            final long[] actual = new long[5];

            Task<Route> task = mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.account().stream(timeExtractor));
            task.waitForCompletion();
            task.getResult().setEnvironment(0, (Object) actual);

            byte[][] responses = {
                    {0x09, 0x03, 0x00, (byte) 0xa6, 0x33, 0x0d, 0x00, (byte) 0xc1, 0x00, (byte) 0xb1, 0x24, 0x19, (byte) 0xcd},
                    {0x09, 0x03, 0x00, (byte) 0xad, 0x33, 0x0d, 0x00, (byte) 0xd4, 0x00, 0x18, 0x25, (byte) 0xc0, (byte) 0xcc},
                    {0x09, 0x03, 0x00, (byte) 0xb4, 0x33, 0x0d, 0x00, (byte) 0xc7, 0x00, 0x09, 0x25, (byte) 0xb2, (byte) 0xcc},
                    {0x09, 0x03, 0x00, (byte) 0xba, 0x33, 0x0d, 0x00, (byte) 0xc5, 0x00, 0x17, 0x25, (byte) 0xbc, (byte) 0xcc},
                    {0x09, 0x03, 0x00, (byte) 0xc1, 0x33, 0x0d, 0x00, (byte) 0xd4, 0x00, (byte) 0xe9, 0x24, (byte) 0xe4, (byte) 0xcc},
                    {0x09, 0x03, 0x00, (byte) 0xc8, 0x33, 0x0d, 0x00, (byte) 0xaf, 0x00, (byte) 0xf7, 0x24, (byte) 0xe3, (byte) 0xcc}
            };
            for(byte[] it: responses) {
                sendMockResponse(it);
            }

            assertArrayEquals(expected, actual);
        }

        @Test
        public void handleRollback() throws InterruptedException {
            long[] expected = new long[] {11, 10, 9, 10, 10};
            final long[] actual = new long[5];

            Task<Route> task = mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.account().stream(timeExtractor));
            task.waitForCompletion();
            task.getResult().setEnvironment(0, (Object) actual);

            byte[][] responses = {
                    {0x09, 0x03, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xc1, 0x00, (byte) 0xb1, 0x24, 0x19, (byte) 0xcd},
                    {0x09, 0x03, 0x00, 0x06, 0x00, 0x00, 0x00, (byte) 0xd4, 0x00, 0x18, 0x25, (byte) 0xc0, (byte) 0xcc},
                    {0x09, 0x03, 0x00, 0x0d, 0x00, 0x00, 0x00, (byte) 0xc7, 0x00, 0x09, 0x25, (byte) 0xb2, (byte) 0xcc},
                    {0x09, 0x03, 0x00, 0x13, 0x00, 0x00, 0x00, (byte) 0xc5, 0x00, 0x17, 0x25, (byte) 0xbc, (byte) 0xcc},
                    {0x09, 0x03, 0x00, 0x1a, 0x00, 0x00, 0x00, (byte) 0xd4, 0x00, (byte) 0xe9, 0x24, (byte) 0xe4, (byte) 0xcc},
                    {0x09, 0x03, 0x00, 0x21, 0x00, 0x00, 0x00, (byte) 0xaf, 0x00, (byte) 0xf7, 0x24, (byte) 0xe3, (byte) 0xcc}
            };
            for(byte[] it: responses) {
                sendMockResponse(it);
            }

            assertArrayEquals(expected, actual);
        }

        @Test
        public void noSpace() throws Exception {
            Task<Route> task = mwBoard.getModule(BarometerBosch.class).pressure().addRouteAsync(source -> source.pack((byte) 4).account());
            task.waitForCompletion();

            assertInstanceOf(IllegalRouteOperationException.class, task.getError());
        }

        @Test
        public void createCountMode() throws InterruptedException {
            byte[][] expected = new byte[][] {
                    { 0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x11, 0x30, 0x03 }
            };

            mwBoard.getModule(Accelerometer.class).acceleration()
                    .addRouteAsync(source -> source.account(RouteComponent.AccountType.COUNT))
                    .waitForCompletion();
            assertArrayEquals(expected, junitPlatform.getCommands());
        }

        @Test
        public void countData() throws InterruptedException {
            long expected = 492;
            final Capture<Long> actual = new Capture<>();

            mwBoard.getModule(Accelerometer.class).acceleration()
                    .addRouteAsync(source -> source.account(RouteComponent.AccountType.COUNT).stream(
                            (data, env) -> actual.set(data.extra(Long.class)))
                    )
                    .waitForCompletion();
            sendMockResponse(new byte[] { 0x09, 0x03, 0x00, (byte) 0xec, 0x01, 0x00, 0x00, 0x01, 0x0b, (byte) 0x9a, 0x07, 0x40, 0x40 });

            assertEquals(expected, actual.get().longValue());
        }

        @Test
        public void countAndTime() throws Exception {
            final BarometerBmp280 barometer = mwBoard.getModule(BarometerBmp280.class);
            final Accelerometer accelerometer = mwBoard.getModule(Accelerometer.class);

            Task<Route> routeTask = accelerometer.acceleration().addRouteAsync(source ->
                    source.pack((byte) 2).account(RouteComponent.AccountType.COUNT).stream((data, env) ->
                            System.out.println(data.toString()))
            ).onSuccessTask(ignored ->
                    barometer.pressure().addRouteAsync(source ->
                            source.account().stream(null)
                    )
            );
            routeTask.waitForCompletion();

            if (routeTask.getError() != null) {
                throw routeTask.getError();
            }

            final Capture<Long> prev = new Capture<>(null);
            final Capture<List<Long>> offsets = new Capture<>(new ArrayList<>());
            routeTask.getResult().resubscribe(0, (data, env) -> {
                if (prev.get() != null) {
                    offsets.get().add(data.timestamp().getTimeInMillis() - prev.get());
                }
                prev.set(data.timestamp().getTimeInMillis());
            });

            sendMockResponse(new byte[] {0x09, 0x03, 0x02, 0x72, (byte) 0xA4, 0x03, 0x00, 0x77, 0x6C, (byte) 0x84, 0x01});
            sendMockResponse(new byte[] {0x09, 0x03, 0x01, (byte) 0x8D, 0x00, 0x00, 0x00, 0x4E, (byte) 0xFF, 0x35, (byte) 0xFD, 0x79, 0x07, 0x4D, (byte) 0xFF, 0x35, (byte) 0xFD, 0x7D, 0x07});
            sendMockResponse(new byte[] {0x09, 0x03, 0x02, (byte) 0xA4, (byte) 0xA4, 0x03, 0x00, 0x05, 0x65, (byte) 0x84, 0x01});

            Long[] actualArray= new Long[offsets.get().size()];
            offsets.get().toArray(actualArray);

            Long[] expected = new Long[] {
                    73L
            };
            assertArrayEquals(expected, actualArray);
        }
    }

    public static class TestAccounterPackerChain extends TestBase {
        @BeforeEach
        public void setup() throws Exception {
            super.setup();

            final Temperature.Sensor thermometer = mwBoard.getModule(Temperature.class).findSensors(Temperature.SensorType.PRESET_THERMISTOR)[0];
            thermometer.addRouteAsync(source -> source.account().pack((byte) 2).stream(null)).waitForCompletion();
        }

        @Test
        public void createAccChain() throws InterruptedException {
            Task<Route> task = mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(
                source -> source.pack((byte) 2).account(RouteComponent.AccountType.COUNT).stream(null)
            );
            task.waitForCompletion();

            System.out.println("temperature[0]".split("[:|\\[]")[0]);
            System.out.println(task.getResult().generateIdentifier(0));
        }
        @Test
        public void createChain() {
            byte[][] expected = new byte[][] {
                    {0x9, 0x2, 0x4, (byte) 0xc1, 0x1, 0x20, 0x11, 0x31, 0x3},
                    {0x9, 0x2, 0x9, 0x3, 0x0, (byte) 0xa0, 0x10, 0x5, 0x1},
                    {0x09, 0x03, 0x01},
                    {0x09, 0x07, 0x01, 0x01}
            };

            assertArrayEquals(expected, junitPlatform.getCommands());
        }

        @Test
        public void dataExtraction() {
            float[] expected = new float[] {29.5f, 29.375f, 29.875f, 29.625f};
            final float[] actual = new float[4];

            mwBoard.lookupRoute(0).resubscribe(0, new Subscriber() {
                int i = 0;
                @Override
                public void apply(Data data, Object... env) {
                    actual[i++] = data.value(Float.class);
                }
            });
            sendMockResponse(new byte[] {0x09, 0x03, 0x01, 0x7b, 0x64, 0x02, 0x00, (byte) 0xec, 0x00, (byte) 0x92, 0x64, 0x02, 0x00, (byte) 0xeb, 0x00});
            sendMockResponse(new byte[] {0x09, 0x03, 0x01, (byte) 0xa8, 0x64, 0x02, 0x00, (byte) 0xef, 0x00, (byte) 0xbf, 0x64, 0x02, 0x00, (byte) 0xed, 0x00});

            assertArrayEquals(expected, actual, 0.001f);
        }

        @Test
        public void timeOffsets() {
            long[] expected = new long[] {33, 33, 33};
            final long[] actual = new long[3];

            mwBoard.lookupRoute(0).resubscribe(0, new Subscriber() {
                int i = 0;
                long prev = -1;
                @Override
                public void apply(Data data, Object... env) {
                    if (prev == -1) {
                        prev = data.timestamp().getTimeInMillis();
                    } else {
                        actual[i++] = data.timestamp().getTimeInMillis() - prev;
                        prev = data.timestamp().getTimeInMillis();
                    }
                }
            });

            byte[][] responses = new byte[][] {
                    {0x0b, (byte) 0x84, (byte) 0xf5, 0x62, 0x02, 0x00, 0x00},
                    {0x09, 0x03, 0x01, 0x7b, 0x64, 0x02, 0x00, (byte) 0xec, 0x00, (byte) 0x92, 0x64, 0x02, 0x00, (byte) 0xeb, 0x00},
                    {0x09, 0x03, 0x01, (byte) 0xa8, 0x64, 0x02, 0x00, (byte) 0xef, 0x00, (byte) 0xbf, 0x64, 0x02, 0x00, (byte) 0xed, 0x00}
            };
            for(byte[] it: responses) {
                sendMockResponse(it);
            }

            assertArrayEquals(expected, actual);
        }
    }

    public static class TestPackerAccounterChain extends TestBase {
        @BeforeEach
        public void setup() throws Exception {
            super.setup();

            final Temperature.Sensor thermometer = mwBoard.getModule(Temperature.class).findSensors(Temperature.SensorType.PRESET_THERMISTOR)[0];
            thermometer.addRouteAsync(source -> source.pack((byte) 4).account().stream(null)).waitForCompletion();
        }

        @Test
        public void createChain() {
            byte[][] expected = new byte[][] {
                    {0x09, 0x02, 0x04, (byte) 0xc1, 0x01, 0x20, 0x10, 0x01, 0x03},
                    {0x09, 0x02, 0x09, 0x03, 0x00, (byte) 0xe0, 0x11, 0x31, 0x03},
                    {0x09, 0x03, 0x01},
                    {0x09, 0x07, 0x01, 0x01}
            };

            assertArrayEquals(expected, junitPlatform.getCommands());
        }

        @Test
        public void dataExtraction() {
            float[] expected = new float[] {24.5f, 24.625f, 24.5f, 24.375f, 24.25f, 24.375f, 24.5f, 24.25f};
            final float[] actual = new float[8];

            mwBoard.lookupRoute(0).resubscribe(0, new Subscriber() {
                int i = 0;
                @Override
                public void apply(Data data, Object... env) {
                    actual[i++] = data.value(Float.class);
                }
            });
            sendMockResponse(new byte[] {0x09, 0x03, 0x01, 0x04, (byte) 0x85, (byte) 0xa0, 0x00, (byte) 0xc4, 0x00, (byte) 0xc5, 0x00, (byte) 0xc4, 0x00, (byte) 0xc3, 0x00});
            sendMockResponse(new byte[] {0x09, 0x03, 0x01, 0x5e, (byte) 0x85, (byte) 0xa0, 0x00, (byte) 0xc2, 0x00, (byte) 0xc3, 0x00, (byte) 0xc4, 0x00, (byte) 0xc2, 0x00});

            assertArrayEquals(expected, actual, 0.001f);
        }

        @Test
        public void timeOffsets() {
            long[] expected = new long[] {0, 0, 0, 132, 0, 0, 0, 132, 0, 0, 0, 133, 0, 0, 0};
            final long[] actual = new long[15];

            mwBoard.lookupRoute(0).resubscribe(0, new Subscriber() {
                int i = 0;
                long prev = -1;
                @Override
                public void apply(Data data, Object... env) {
                    if (prev == -1) {
                        prev = data.timestamp().getTimeInMillis();
                    } else {
                        actual[i++] = data.timestamp().getTimeInMillis() - prev;
                        prev = data.timestamp().getTimeInMillis();
                    }
                }
            });

            byte[][] responses = new byte[][] {
                    {0x0b, (byte) 0x84, 0x1c, (byte) 0x84, (byte) 0xa0, 0x00, 0x01},
                    {0x09, 0x03, 0x01, 0x04, (byte) 0x85, (byte) 0xa0, 0x00, (byte) 0xc4, 0x00, (byte) 0xc5, 0x00, (byte) 0xc4, 0x00, (byte) 0xc3, 0x00},
                    {0x09, 0x03, 0x01, 0x5e, (byte) 0x85, (byte) 0xa0, 0x00, (byte) 0xc2, 0x00, (byte) 0xc3, 0x00, (byte) 0xc4, 0x00, (byte) 0xc2, 0x00},
                    {0x09, 0x03, 0x01, (byte) 0xb8, (byte) 0x85, (byte) 0xa0, 0x00, (byte) 0xc3, 0x00, (byte) 0xc4, 0x00, (byte) 0xc3, 0x00, (byte) 0xc3, 0x00},
                    {0x09, 0x03, 0x01, 0x13, (byte) 0x86, (byte) 0xa0, 0x00, (byte) 0xc5, 0x00, (byte) 0xc3, 0x00, (byte) 0xc5, 0x00, (byte) 0xc2, 0x00},
            };
            for(byte[] it: responses) {
                sendMockResponse(it);
            }

            assertArrayEquals(expected, actual);
        }
    }

    public static class TestHighPassFilter extends TestBase {
        @Test
        public void createAccHpf() throws InterruptedException {
            byte[][] expected = new byte[][] {
                    {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x03, 0x25, 0x04, 0x02}
            };
            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.highpass((byte) 4)).waitForCompletion();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }

        @Test
        public void accHpfData() throws InterruptedException {
            Acceleration expected = new Acceleration(Float.intBitsToFloat(0xba880000), Float.intBitsToFloat(0x3b240000), Float.intBitsToFloat(0x3ab00000));
            final Capture<Acceleration> actual = new Capture<>();
            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.highpass((byte) 4).stream((Subscriber) (data, env) -> actual.set(data.value(Acceleration.class)))).waitForCompletion();

            sendMockResponse(new byte[] {0x09, 0x03, 0x00, (byte) 0xef, (byte) 0xff, 0x29, 0x00, 0x16, 0x00});
            assertEquals(expected, actual.get());
        }
    }

    public static class TestActivityMonitor extends UnitTestBase {
        private Route activityRoute, bufferStateRoute;

        @BeforeEach
        public void setup() throws Exception {
            junitPlatform.addCustomModuleInfo(new byte[] {0x09, (byte) 0x80, 0x00, 0x00, 0x1c});
            junitPlatform.boardInfo= new MetaWearBoardInfo(AccelerometerBmi160.class);
            connectToBoard();

            mwBoard.getModule(Accelerometer.class).acceleration().addRouteAsync(source -> source.map(Function1.RMS).accumulate()
                .multicast()
                    .to().limit(1000).stream((Subscriber) (data, env) -> ((Capture<Float>) env[0]).set(data.value(Float.class)))
                    .to().buffer().name("rms_accum")
                .end()).continueWithTask(task -> {
                activityRoute = task.getResult();
                return mwBoard.getModule(DataProcessor.class).state("rms_accum").addRouteAsync(source -> source.stream((Subscriber) (data, env) -> ((Capture<Float>) env[0]).set(data.value(Float.class))));
            }).continueWith(task -> {
                bufferStateRoute= task.getResult();
                return null;
            }).waitForCompletion();
        }

        @Test
        public void createRoute() {
            byte[][] expected= new byte[][] {
                    {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x00},
                    {0x09, 0x02, 0x09, 0x03, 0x00, 0x20, 0x02, 0x07},
                    {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x08, 0x03, (byte) 0xe8, 0x03, 0x00, 0x00},
                    {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x0f, 0x03},
                    {0x09, 0x03, 0x01},
                    {0x09, 0x07, 0x02, 0x01}
            };

            assertArrayEquals(expected, junitPlatform.getCommands());
        }

        @Test
        public void handleData() {
            float expected= 33.771667f;
            Capture<Float> actual= new Capture<>();

            activityRoute.setEnvironment(0, actual);

            sendMockResponse(new byte[] {0x09, 0x03, 0x02, 0x63, 0x71, 0x08, 0x00});
            assertEquals(expected, actual.get(), 0.0001f);
        }

        @Test
        public void readBufferSilent() {
            byte[] expected= new byte[] {0x9, (byte) 0xc4, 3};

            bufferStateRoute.unsubscribe(0);
            mwBoard.getModule(DataProcessor.class).state("rms_accum").read();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
        }

        @Test
        public void readBuffer() {
            byte[] expected= new byte[] {0x9, (byte) 0x84, 3};

            mwBoard.getModule(DataProcessor.class).state("rms_accum").read();
            assertArrayEquals(expected, junitPlatform.getLastCommand());
        }

        @Test
        public void bufferData() {
            float expected= 71.61182f;
            Capture<Float> actual= new Capture<>();

            bufferStateRoute.setEnvironment(0, actual);
            sendMockResponse(new byte[] {0x09, (byte) 0x84, 0x03, 0x28, (byte) 0xe7, 0x11, 0x00});
            assertEquals(expected, actual.get(), 0.0001f);
        }

        @Test
        public void checkScheme() {
            assertEquals("acceleration:rms?id=0:accumulate?id=1:time?id=2", activityRoute.generateIdentifier(0));
            assertEquals("acceleration:rms?id=0:accumulate?id=1:buffer-state?id=3", bufferStateRoute.generateIdentifier(0));
        }
    }

    public static class TestSampleDelay extends TestBase {
        @Test
        public void createHistory() throws InterruptedException {
            byte[][] expected = new byte[][] {
                    { 0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x0a, 0x05, 0x10 },
                    { 0x09, 0x02, 0x09, 0x03, 0x00, (byte) 0xa0, 0x01, 0x02, 0x00, 0x00 },
                    { 0x0a, 0x02, 0x03, 0x08, (byte) 0xff, 0x09, 0x04, 0x03 },
                    { 0x0a, 0x03, 0x01, 0x20, 0x00 }
            };

            final byte samples = 16;

            AccelerometerBmi160 accelerometer = mwBoard.getModule(AccelerometerBmi160.class);
            accelerometer.acceleration().addRouteAsync(source ->
                    source.delay(samples).limit(Passthrough.COUNT, (short) 0).name("history")
            ).waitForCompletion();

            final DataProcessor dataprocessor = mwBoard.getModule(DataProcessor.class);
            accelerometer.lowHigh().addRouteAsync(source ->
                    source.react(token -> dataprocessor.edit("history", PassthroughEditor.class).set((short) (samples * 2)))
            ).waitForCompletion();

            assertArrayEquals(expected, junitPlatform.getCommands());
        }
    }

    public static class TestFuser extends TestBase {
        @BeforeEach
        public void setup() throws Exception {
            super.setup();

            final Accelerometer acc = mwBoard.getModule(Accelerometer.class);
            final Gyro gyro = mwBoard.getModule(Gyro.class);

            Task<Route> task = gyro.angularVelocity().addRouteAsync(source ->
                    source.buffer().name("gyro-buffer")
            ).onSuccessTask(ignored ->acc.acceleration().addRouteAsync(source ->
                    source.fuse("gyro-buffer").limit(20).stream(null)
            ));
            task.waitForCompletion();

            if (task.isFaulted()) {
                throw task.getError();
            }
            if (task.isCancelled()) {
                throw new CancellationException("Task cancelled");
            }
        }

        @Test
        public void createAccGyroFusion() {
            byte[][] expected = new byte[][] {
                    {0x09, 0x02, 0x13, 0x05, (byte) 0xff, (byte) 0xa0, 0x0f, 0x05},
                    {0x09, 0x02, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x1b, 0x01, 0x00},
                    {0x09, 0x02, 0x09, 0x03, 0x01, 0x60, 0x08, 0x13, 0x14, 0x00, 0x00, 0x00},
                    {0x09, 0x03, 0x01},
                    {0x09, 0x07, 0x02, 0x01}
            };

            assertArrayEquals(expected, junitPlatform.getCommands());
        }

        @Test
        public void handleData() throws InterruptedException {
            final Capture<Acceleration> accData = new Capture<>();
            final Capture<AngularVelocity> gyroData = new Capture<>();

            Route fuserRoute = mwBoard.lookupRoute(1);
            fuserRoute.resubscribe(0, ((data, env) -> {
                Data[] values = data.value(Data[].class);
                accData.set(values[0].value(Acceleration.class));
                gyroData.set(values[1].value(AngularVelocity.class));
            }));

            final Capture<Acceleration> rawAccData = new Capture<>();
            final Capture<AngularVelocity> rawGyroData = new Capture<>();
            final Accelerometer acc = mwBoard.getModule(Accelerometer.class);
            final Gyro gyro = mwBoard.getModule(Gyro.class);

            Task<Route> task = gyro.angularVelocity().addRouteAsync(source ->
                    source.stream((data, env) -> rawGyroData.set(data.value(AngularVelocity.class)))
            ).onSuccessTask(ignored ->acc.acceleration().addRouteAsync(source ->
                    source.stream((data, env) -> rawAccData.set(data.value(Acceleration.class)))
            ));
            task.waitForCompletion();

            sendMockResponse(new byte[] {0x09, 0x03, 0x02, (byte) 0xf4, 0x0d, 0x3c, 0x39, (byte) 0x99, 0x11, 0x01, (byte) 0x80, (byte) 0xd6, (byte) 0x91, (byte) 0xd3, 0x67});
            sendMockResponse(new byte[] {0x03, 0x04, (byte) 0xf4, 0x0d, 0x3c, 0x39, (byte) 0x99, 0x11});
            sendMockResponse(new byte[] {0x13, 0x05, 0x01, (byte) 0x80, (byte) 0xd6, (byte) 0x91, (byte) 0xd3, 0x67});

            assertEquals(rawAccData.get(), accData.get());
            assertEquals(rawGyroData.get(), gyroData.get());
        }
    }
}