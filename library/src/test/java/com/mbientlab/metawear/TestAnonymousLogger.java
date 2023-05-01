package com.mbientlab.metawear;

import static com.mbientlab.metawear.Executors.IMMEDIATE_EXECUTOR;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.google.android.gms.tasks.Task;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by eric on 8/26/17.
 */
@Nested
public class TestAnonymousLogger {
    private static Task<AnonymousRoute[]> retrieveLoggers(MetaWearBoard mwBoard) {
        return mwBoard.createAnonymousRoutesAsync();
    }

    static class TestBase extends UnitTestBase {
        TestBase() {
            this((byte) 0x8, (byte) 0x3);
        }

        TestBase(byte accRange, byte gyroRange) {
            junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, Gyro.class, MagnetometerBmm150.class, SensorFusionBosch.class);

            junitPlatform.addCustomResponse(new byte[]{0x3, (byte) 0x83},
                    new byte[]{0x03, (byte) 0x83, 40, accRange});
            junitPlatform.addCustomResponse(new byte[]{0x13, (byte) 0x83},
                    new byte[]{0x13, (byte) 0x83, 40, gyroRange});
            junitPlatform.addCustomResponse(new byte[]{0x19, (byte) 0x82},
                    new byte[]{0x19, (byte) 0x82, 0x1, 0xf});
        }
    }

    public static class TestAcceleration extends TestBase {
        public TestAcceleration() {
            super();

            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x00},
                    new byte[]{0x0b, (byte) 0x82, 0x03, 0x04, (byte) 0xff, 0x60});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x01},
                    new byte[]{0x0b, (byte) 0x82, 0x03, 0x04, (byte) 0xff, 0x24});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x02},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x03},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x04},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x05},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x06},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x07},
                    new byte[]{0x0b, (byte) 0x82});
        }

        @Test
        public void syncLoggers() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals(1, routes.length);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void handleDownload() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().continueWithTask(IMMEDIATE_EXECUTOR, task -> {
                    return mwBoard.createAnonymousRoutesAsync();
                }).addOnSuccessListener(IMMEDIATE_EXECUTOR, route -> {
                Acceleration expected = new Acceleration(Float.intBitsToFloat(1031077888), Float.intBitsToFloat(1033797632), Float.intBitsToFloat(1065209856));
                final Capture<Acceleration> actual = new Capture<>();
                route[0].subscribe((data, env) -> actual.set(data.value(Acceleration.class)));

                sendMockResponse(new byte[] {11,7,-96,-26,66,0,0,-11,0,61,1,-95,-26,66,0,0,-35,15,0,0});

                assertEquals(expected, actual.get());
                doneSignal.countDown();
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }
    public static class TestGyroY extends TestBase {
        public TestGyroY() {
            super();

            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x00},
                    new byte[]{0x0b, (byte) 0x82, 0x13, 0x05, (byte) 0xff, 0x22});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x01},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x02},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x03},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x04},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x05},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x06},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x07},
                    new byte[]{0x0b, (byte) 0x82});
        }

        @Test
        public void syncLoggers() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals(1, routes.length);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void handleDownload() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    float[] expected = new float[] {-0.053f, -0.015f};
                    final float actual[] = new float[2];
                    routes[0].subscribe(new Subscriber() {
                        int i = 0;
                        @Override
                        public void apply(Data data, Object... env) {
                            actual[i++] = data.value(Float.class);
                        }
                    });

                    sendMockResponse(new byte[] {11, 7, 64, 34, (byte) 223, 4, 0, (byte) 249, (byte) 255, 0, 0, 64, 61, (byte) 223, 4, 0, (byte) 254, (byte) 255, 0, 0});

                    assertArrayEquals(expected, actual, 0.001f);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void checkIdentifier() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals("angular-velocity[1]", routes[0].identifier());
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }
    public static class TestActivity extends TestBase {
        public TestActivity() {
            super();

            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x00},
                    new byte[]{0x0b, (byte) 0x82, 0x09, 0x03, 0x02, 0x60});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x01},
                    new byte[]{0x0b, (byte) 0x82, 0x09, (byte) 0xc4, 0x03, 0x60});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x02},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x03},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x04},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x05},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x06},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x07},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0x9, (byte) 0x82, 0x00},
                    new byte[]{0x09, (byte) 0x82, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x07, (byte) 0xa5, 0x00, 0x00, 0x00, 0x00, (byte) 0xd0, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
            junitPlatform.addCustomResponse(new byte[]{0x9, (byte) 0x82, 0x01},
                    new byte[]{0x09, (byte) 0x82, 0x09, 0x03, 0x00, 0x20, 0x02, 0x07, 0x00, 0x00, 0x00, 0x00, (byte) 0xd0, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
            junitPlatform.addCustomResponse(new byte[]{0x9, (byte) 0x82, 0x02},
                    new byte[]{0x09, (byte) 0x82, 0x09, 0x03, 0x01, 0x60, 0x08, 0x13, 0x30, 0x75, 0x00, 0x00, (byte) 0xd0, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
            junitPlatform.addCustomResponse(new byte[]{0x9, (byte) 0x82, 0x03},
                    new byte[]{0x09, (byte) 0x82, 0x09, 0x03, 0x01, 0x60, 0x0f, 0x03, 0x00, 0x00, 0x00, 0x00, (byte) 0xd0, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
        }

        @Test
        public void syncLoggers() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals(2, routes.length);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void checkScheme() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.createAnonymousRoutesAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                    assertEquals("acceleration:rms?id=0:accumulate?id=1:time?id=2", task[0].identifier());
                    assertEquals("acceleration:rms?id=0:accumulate?id=1:buffer-state?id=3", task[1].identifier());
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void handleDownload() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.createAnonymousRoutesAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                    float[] expected = new float[] {1.16088868f, 1793.6878f, 3545.5054f};
                    final float actual[] = new float[expected.length];
                    task[0].subscribe(new Subscriber() {
                        int i = 0;
                        @Override
                        public void apply(Data data, Object... env) {
                            actual[i++] = data.value(Float.class);
                        }
                    });

                    sendMockResponse(new byte[] {0x0b, 0x07, 0x00, 0x3c, (byte) 0xe2, 0x01, 0x00, (byte) 0x93, 0x12, 0x00, 0x00, 0x00, 0x48, 0x32, 0x02, 0x00, 0x01, 0x1b, 0x70, 0x00});
                    sendMockResponse(new byte[] {0x0b, 0x07, 0x00, 0x53, (byte) 0x82, 0x02, 0x00, 0x16, (byte) 0x98, (byte) 0xdd, 0x00});

                    assertArrayEquals(expected, actual, 0.001f);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(30, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void handleStateDownload() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    float expected = 3521.868f;
                    final Capture<Float> actual = new Capture<>();

                    routes[1].subscribe((data, env) -> actual.set(data.value(Float.class)));

                    sendMockResponse(new byte[] {0x0b, 0x07, (byte) 0xc1, (byte) 0xe9, 0x06, 0x02, 0x00, (byte) 0xe3, 0x1d, (byte) 0xdc, 0x00});

                    assertEquals(expected, actual.get(), 0.001f);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }
    public static class TestQuaternionLimiter extends TestBase {
        public TestQuaternionLimiter() {
            super();

            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x00},
                    new byte[]{0x0b, (byte) 0x82, 0x09, 0x03, 0x00, 0x60});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x01},
                    new byte[]{0x0b, (byte) 0x82, 0x09, 0x03, 0x00, 0x64});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x02},
                    new byte[]{0x0b, (byte) 0x82, 0x09, 0x03, 0x00, 0x68});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x03},
                    new byte[]{0x0b, (byte) 0x82, 0x09, 0x03, 0x00, 0x6c});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x04},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x05},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x06},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x07},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0x9, (byte) 0x82, 0x00},
                    new byte[]{0x09, (byte) 0x82, 0x19, 0x07, (byte) 0xff, (byte) 0xe0, 0x08, 0x17, 0x14, 0x00, 0x00, 0x00, (byte) 0xd0, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
        }

        @Test
        public void syncLoggers() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals(1, routes.length);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void checkScheme() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals("quaternion:time?id=0", routes[0].identifier());
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }
    public static class TestTimeout extends UnitTestBase {
        public TestTimeout() {
            junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class, Gyro.class, MagnetometerBmm150.class, SensorFusionBosch.class);
        }

        public Task<Void> setup() {
            return connectToBoard();
        }

        @Test
        public void accSyncTimeout() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.createAnonymousRoutesAsync().addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                    assertInstanceOf(TimeoutException.class, exception);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void gyroSyncTimeout() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                junitPlatform.addCustomResponse(new byte[]{0x3, (byte) 0x83},
                        new byte[]{0x03, (byte) 0x83, 40, 8});

                mwBoard.createAnonymousRoutesAsync().addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                    assertInstanceOf(TimeoutException.class, exception);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void logTimeout() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                junitPlatform.addCustomResponse(new byte[]{0x3, (byte) 0x83},
                        new byte[]{0x03, (byte) 0x83, 40, 8});
                junitPlatform.addCustomResponse(new byte[]{0x13, (byte) 0x83},
                        new byte[]{0x13, (byte) 0x83, 40, 3});

                mwBoard.createAnonymousRoutesAsync().addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                    assertInstanceOf(TimeoutException.class, exception);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void dataProcTimeout() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            setup().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                junitPlatform.addCustomResponse(new byte[]{0x3, (byte) 0x83},
                        new byte[]{0x03, (byte) 0x83, 40, 8});
                junitPlatform.addCustomResponse(new byte[]{0x13, (byte) 0x83},
                        new byte[]{0x13, (byte) 0x83, 40, 3});
                junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x00},
                        new byte[]{0x0b, (byte) 0x82, 0x09, 0x03, 0x02, 0x60});

                mwBoard.createAnonymousRoutesAsync().addOnFailureListener(IMMEDIATE_EXECUTOR, exception -> {
                    assertInstanceOf(TimeoutException.class, exception);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }
    public static class TestSplitImu extends TestBase {
        public TestSplitImu() {
            super();

            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x00},
                    new byte[]{0x0b, (byte) 0x82, 0x03, 0x04, (byte) 0xff, 0x60});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x01},
                    new byte[]{0x0b, (byte) 0x82, 0x13, 0x05, (byte) 0xff, 0x60});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x02},
                    new byte[]{0x0b, (byte) 0x82, 0x03, 0x04, (byte) 0xff, 0x24});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x03},
                    new byte[]{0x0b, (byte) 0x82, 0x13, 0x05, (byte) 0xff, 0x24});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x04},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x05},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x06},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x07},
                    new byte[]{0x0b, (byte) 0x82});
        }

        @Test
        public void syncLoggers() throws Exception {
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals(2, routes.length);
                });
            });
        }

        @Test
        public void handleDownload() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.createAnonymousRoutesAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                    AngularVelocity expected = new AngularVelocity(Float.intBitsToFloat(0x4360312c), Float.intBitsToFloat(0x432ad2bc), Float.intBitsToFloat(0x4313031f));
                    final Capture<AngularVelocity> actual = new Capture<>();
                    task[1].subscribe((data, env) -> actual.set(data.value(AngularVelocity.class)));

                    Acceleration expectedAcc = new Acceleration(Float.intBitsToFloat(1031077888), Float.intBitsToFloat(1033797632), Float.intBitsToFloat(1065209856));
                    final Capture<Acceleration> actualAcc = new Capture<>();
                    task[0].subscribe((data, env) -> actualAcc.set(data.value(Acceleration.class)));

                    sendMockResponse(new byte[] {11,7,0x60,-26,66,0,0,-11,0,61,1,0x62,-26,66,0,0,-35,15,0,0});
                    sendMockResponse(new byte[] {0x0b, 0x07, 0x61, 0x38, (byte) 0xc2, 0x01, 0x00, (byte) 0xe6, 0x72, (byte) 0x8c, 0x57, 0x63, 0x38, (byte) 0xc2, 0x01, 0x00, 0x58, 0x4b, 0x00, 0x00});

                    assertEquals(expected, actual.get());
                    assertEquals(expectedAcc, actualAcc.get());
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }
    public static class TestMultipleLoggers extends TestBase {
        public TestMultipleLoggers() {
            super();

            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x00},
                    new byte[]{0x0b, (byte) 0x82, 0x13, 0x05, (byte) 0xff, 0x60});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x01},
                    new byte[]{0x0b, (byte) 0x82, 0x13, 0x05, (byte) 0xff, 0x24});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x02},
                    new byte[]{0x0b, (byte) 0x82, 0x13, 0x05, (byte) 0xff, 0x22});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x03},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x04},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x05},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x06},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x07},
                    new byte[]{0x0b, (byte) 0x82});
        }

        @Test
        public void syncLoggers() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals(2, routes.length);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }
    public static class TestBmi160StepCounter extends TestBase {
        public TestBmi160StepCounter() {
            super();

            junitPlatform.addCustomResponse(new byte[] { 0xb, (byte) 0x82, 0x00 },
                    new byte[] { 0x0b, (byte) 0x82, 0x03, (byte) 0xda, (byte) 0xff, 0x20 });
            junitPlatform.addCustomResponse(new byte[] { 0xb, (byte) 0x82, 0x01 },
                    new byte[] { 0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[] { 0xb, (byte) 0x82, 0x02 },
                    new byte[] { 0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[] { 0xb, (byte) 0x82, 0x03 },
                    new byte[] { 0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[] { 0xb, (byte) 0x82, 0x04 },
                    new byte[] { 0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[] { 0xb, (byte) 0x82, 0x05 },
                    new byte[] { 0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[] { 0xb, (byte) 0x82, 0x06 },
                    new byte[] { 0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[] { 0xb, (byte) 0x82, 0x07 },
                    new byte[] { 0x0b, (byte) 0x82});
        }

        @Test
        public void SyncLoggers() throws Exception {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals(1, routes.length);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void CheckIdentifier() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals("step-counter", routes[0].identifier());
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }
    public static class TestFuser extends TestBase {
        public TestFuser() {
            super((byte) 0x03, (byte) 0x04);

            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x00},
                    new byte[]{0x0b, (byte) 0x82, 0x09, 0x03, 0x01, 0x60});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x01},
                    new byte[]{0x0b, (byte) 0x82, 0x09, 0x03, 0x01, 0x64});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x02},
                    new byte[]{0x0b, (byte) 0x82, 0x09, 0x03, 0x01, 0x68});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x03},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x04},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x05},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x06},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0xb, (byte) 0x82, 0x07},
                    new byte[]{0x0b, (byte) 0x82});
            junitPlatform.addCustomResponse(new byte[]{0x9, (byte) 0x82, 0x00},
                    new byte[]{0x09, (byte) 0x82, 0x13, 0x05, (byte) 0xff, (byte) 0xa0, 0x0f, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xe9, (byte) 0xff });
            junitPlatform.addCustomResponse(new byte[]{0x9, (byte) 0x82, 0x01},
                    new byte[]{0x09, (byte) 0x82, 0x03, 0x04, (byte) 0xff, (byte) 0xa0, 0x1b, 0x01, 0x00, 0x01, 0x02, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xe9, (byte) 0xff });
        }

        @Test
        public void syncLoggers() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals(1, routes.length);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void checkScheme() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                retrieveLoggers(mwBoard).addOnSuccessListener(IMMEDIATE_EXECUTOR, routes -> {
                    assertEquals("acceleration:fuser?id=1", routes[0].identifier());
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }

        @Test
        public void handleDownload() throws InterruptedException {
            CountDownLatch doneSignal = new CountDownLatch(1);
            connectToBoard().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
                mwBoard.createAnonymousRoutesAsync().addOnSuccessListener(IMMEDIATE_EXECUTOR, task -> {
                    AngularVelocity[] expectedGyro= new AngularVelocity[] {
                            new AngularVelocity(Float.intBitsToFloat(0x3E84AED4), Float.intBitsToFloat(0x3EEC18FA), Float.intBitsToFloat(0x3D3B512C)),
                            new AngularVelocity(Float.intBitsToFloat(0x3E9C18FA), Float.intBitsToFloat(0x3EFDA896), Float.intBitsToFloat(0x3DE2576A))
                    };
                    Acceleration[] expectedAcc= new Acceleration[] {
                            new Acceleration(Float.intBitsToFloat(0xBCBD0000), Float.intBitsToFloat(0x3CA48000), Float.intBitsToFloat(0x3F82DA00)),
                            new Acceleration(Float.intBitsToFloat(0xBC4A0000), Float.intBitsToFloat(0x3C9A8000), Float.intBitsToFloat(0x3F840400))
                    };

                    final AngularVelocity[] actualGyro = new AngularVelocity[2];
                    final Acceleration[] actualAcc = new Acceleration[2];
                    task[0].subscribe(new Subscriber() {
                        int i = 0;
                        @Override
                        public void apply(Data data, Object... env) {
                            Data[] values = data.value(Data[].class);
                            actualAcc[i] = values[0].value(Acceleration.class);
                            actualGyro[i] = values[1].value(AngularVelocity.class);
                            i++;
                        }
                    });

                    sendMockResponse(new byte[] {0x0b, 0x07, (byte) 0xc0, (byte) 0xac, 0x1b, 0x00, 0x00, (byte) 0x86, (byte) 0xfe, 0x49, 0x01, (byte) 0xc1, (byte) 0xac, 0x1b, 0x00, 0x00, 0x6d, 0x41, 0x44, 0x00});
                    sendMockResponse(new byte[] {0x0b, 0x07, (byte) 0xc2, (byte) 0xac, 0x1b, 0x00, 0x00, 0x79, 0x00, 0x0c, 0x00, (byte) 0xc0, (byte) 0xc8, 0x1b, 0x00, 0x00, 0x36, (byte) 0xff, 0x35, 0x01});
                    sendMockResponse(new byte[] {0x0b, 0x07, (byte) 0xc1, (byte) 0xc8, 0x1b, 0x00, 0x00, 0x02, 0x42, 0x50, 0x00, (byte) 0xc2, (byte) 0xc8, 0x1b, 0x00, 0x00, (byte) 0x82, 0x00, 0x1d, 0x00});

                    assertArrayEquals(expectedAcc, actualAcc);
                    assertArrayEquals(expectedGyro, actualGyro);
                    doneSignal.countDown();
                });
            });
            doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
        }
    }
}
