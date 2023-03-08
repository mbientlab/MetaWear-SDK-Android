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

import static com.mbientlab.metawear.Executors.IMMEDIATE_EXECUTOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.AmbientLightLtr329;
import com.mbientlab.metawear.module.BarometerBme280;
import com.mbientlab.metawear.module.BarometerBmp280;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.ColorTcs34725;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.HumidityBme280;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Macro;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.ProximityTsl2671;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.SerialPassthrough;
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Timer;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by etsai on 3/4/17.
 */

public class TestModule extends UnitTestBase {
    @Test
    public void notSupported() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNull(mwBoard.getModule(Switch.class));
            assertNull(mwBoard.getModule(Led.class));
            assertNull(mwBoard.getModule(AccelerometerBmi160.class));
            assertNull(mwBoard.getModule(AccelerometerMma8452q.class));
            assertNull(mwBoard.getModule(AccelerometerBma255.class));
            assertNull(mwBoard.getModule(Temperature.class));
            assertNull(mwBoard.getModule(Haptic.class));
            assertNotNull(mwBoard.getModule(DataProcessor.class));
            assertNotNull(mwBoard.getModule(Logging.class));
            assertNotNull(mwBoard.getModule(Timer.class));
            assertNull(mwBoard.getModule(SerialPassthrough.class));
            assertNotNull(mwBoard.getModule(Macro.class));
            assertNull(mwBoard.getModule(Settings.class));
            assertNull(mwBoard.getModule(BarometerBme280.class));
            assertNull(mwBoard.getModule(BarometerBmp280.class));
            assertNull(mwBoard.getModule(Gyro.class));
            assertNull(mwBoard.getModule(AmbientLightLtr329.class));
            assertNull(mwBoard.getModule(MagnetometerBmm150.class));
            assertNull(mwBoard.getModule(HumidityBme280.class));
            assertNull(mwBoard.getModule(ColorTcs34725.class));
            assertNull(mwBoard.getModule(ProximityTsl2671.class));
            assertNull(mwBoard.getModule(SensorFusionBosch.class));
            assertNull(mwBoard.getModule(Debug.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupSwitch() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(Switch.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(Switch.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupLed() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(Led.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(Led.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupAccelerometerBmi160() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBmi160.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertTrue(mwBoard.getModule(Accelerometer.class) instanceof AccelerometerBmi160);
            assertNotNull(mwBoard.getModule(AccelerometerBmi160.class));
            assertNull(mwBoard.getModule(AccelerometerMma8452q.class));
            assertNull(mwBoard.getModule(AccelerometerBma255.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupAccelerometerBma255() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerBma255.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertTrue(mwBoard.getModule(Accelerometer.class) instanceof AccelerometerBma255);
            assertNotNull(mwBoard.getModule(AccelerometerBma255.class));
            assertNull(mwBoard.getModule(AccelerometerMma8452q.class));
            assertNull(mwBoard.getModule(AccelerometerBmi160.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupAccelerometerMma8452q() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(AccelerometerMma8452q.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertTrue(mwBoard.getModule(Accelerometer.class) instanceof AccelerometerMma8452q);
            assertNotNull(mwBoard.getModule(AccelerometerMma8452q.class));
            assertNull(mwBoard.getModule(AccelerometerBma255.class));
            assertNull(mwBoard.getModule(AccelerometerBmi160.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupTemperature() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(Temperature.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(Temperature.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupHaptic() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(Haptic.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(Haptic.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupDataProcessor() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(DataProcessor.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(DataProcessor.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupLogging() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(Logging.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(Logging.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupTimer() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(Timer.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(Timer.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupSerialPassthrough() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(SerialPassthrough.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(SerialPassthrough.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupMacro() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(Macro.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(Macro.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupSettings() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(Settings.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(Settings.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupBarometerBme280() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(BarometerBme280.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertTrue(mwBoard.getModule(BarometerBosch.class) instanceof BarometerBme280);
            assertNotNull(mwBoard.getModule(BarometerBme280.class));
            assertNull(mwBoard.getModule(BarometerBmp280.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupBarometerBmp280() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(BarometerBmp280.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertTrue(mwBoard.getModule(BarometerBosch.class) instanceof BarometerBmp280);
            assertNotNull(mwBoard.getModule(BarometerBmp280.class));
            assertNull(mwBoard.getModule(BarometerBme280.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupGyroBmi160() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(Gyro.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(Gyro.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupAmbientLightLtr329() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(AmbientLightLtr329.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(AmbientLightLtr329.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupMagnetometerBmm150() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(MagnetometerBmm150.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(MagnetometerBmm150.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupHumidityBme280() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(HumidityBme280.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(HumidityBme280.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupColorTcs34725() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(ColorTcs34725.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(ColorTcs34725.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupProximityTsl2671() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(ProximityTsl2671.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(ProximityTsl2671.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupSensorFusionBosch() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(SensorFusionBosch.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(SensorFusionBosch.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }

    @Test
    public void lookupDebug() throws Exception {
        CountDownLatch doneSignal = new CountDownLatch(1);
        junitPlatform.boardInfo = new MetaWearBoardInfo(Debug.class);
        connectToBoardNew().addOnSuccessListener(IMMEDIATE_EXECUTOR, ignored -> {
            assertNotNull(mwBoard.getModule(Debug.class));
            doneSignal.countDown();
        });
        doneSignal.await(TEST_WAIT_TIME, TimeUnit.SECONDS);
        assertEquals(0, doneSignal.getCount());
    }
}
