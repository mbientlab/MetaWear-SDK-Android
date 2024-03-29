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

import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBmi270;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.AmbientLightLtr329;
import com.mbientlab.metawear.module.BarometerBme280;
import com.mbientlab.metawear.module.BarometerBmp280;
import com.mbientlab.metawear.module.ColorTcs34725;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.GyroBmi270;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by etsai on 8/31/16.
 */
public class MetaWearBoardInfo {
    static final Map<Class<?>, byte[]> MODULE_RESPONSE;
    private static final Class<?>[] DEFAULT_MODULES = new Class<?>[] {
            Logging.class, DataProcessor.class, Timer.class, Macro.class
    };
    private static Map<Byte, byte[]> createModuleResponses(Class<?> ... moduleClasses) {
        HashMap<Byte, byte[]> responses = new HashMap<>();
        boolean hasDebug = false;

        if (moduleClasses != null) {
            for (Class<?> clazz : moduleClasses) {
                if (MODULE_RESPONSE.containsKey(clazz)) {
                    byte[] response = MODULE_RESPONSE.get(clazz);
                    responses.put(response[0], Arrays.copyOf(response, response.length));
                }
                hasDebug |= clazz.equals(Debug.class);
            }
        }
        for(Class<?> clazz: DEFAULT_MODULES) {
            byte[] response = MODULE_RESPONSE.get(clazz);
            responses.put(response[0], Arrays.copyOf(response, response.length));
        }

        for(short i = 1; i <= 0x19; i++) {
            byte casted = (byte) i;
            if (casted == 0xa) {
                responses.put((byte) 0xa, new byte[] {0x0a, (byte) 0x80, 0x00, 0x00, 0x1C});
            } if (!responses.containsKey(casted)) {
                responses.put(casted, new byte[] {casted, (byte) 0x80});
            }
        }

        if (!hasDebug) {
            responses.put((byte) 0xfe, new byte[] {(byte) 0xfe, (byte) 0x80});
        }

        return responses;
    }

    static {
        MODULE_RESPONSE = new HashMap<>();
        MODULE_RESPONSE.put(Switch.class, new byte[] {0x01, (byte) 0x80, 0x00, 0x00});
        MODULE_RESPONSE.put(Led.class, new byte[] {0x02, (byte) 0x80, 0x00, 0x00});
        MODULE_RESPONSE.put(AccelerometerBma255.class, new byte[] {0x03, (byte) 0x80, 0x03, 0x01});
        MODULE_RESPONSE.put(AccelerometerBmi160.class, new byte[] {0x03, (byte) 0x80, 0x01, 0x01});
        MODULE_RESPONSE.put(AccelerometerBmi270.class, new byte[] {0x03, (byte) 0x80, 0x04, 0x00});
        MODULE_RESPONSE.put(AccelerometerMma8452q.class, new byte[] {0x03, (byte) 0x80, 0x00, 0x01});
        MODULE_RESPONSE.put(Temperature.class, new byte[] {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02});
        MODULE_RESPONSE.put(Haptic.class, new byte[] {0x08, (byte) 0x80, 0x00, 0x00});
        MODULE_RESPONSE.put(DataProcessor.class, new byte[] {0x09, (byte) 0x80, 0x00, 0x03, 0x1c});
        //MODULE_RESPONSE.put(Logging.class, new byte[] {0x0b, (byte) 0x80, 0x00, 0x02, 0x08, (byte) 0x80, 0x2b, 0x00, 0x00});
        MODULE_RESPONSE.put(Logging.class, new byte[] {0x0b, (byte) 0x80, 0x00, 0x03, 0x08, 0x00, 0x00, (byte) 0xfb, 0x03, 0x00, 0x02});
        MODULE_RESPONSE.put(Timer.class, new byte[] {0x0c, (byte) 0x80, 0x00, 0x00, 0x08});
        MODULE_RESPONSE.put(SerialPassthrough.class, new byte[] {0x0d, (byte) 0x80, 0x00, 0x01});
        MODULE_RESPONSE.put(Macro.class, new byte[] {0x0f, (byte) 0x80, 0x00, 0x01, 0x08});
        MODULE_RESPONSE.put(Settings.class, new byte[] {0x11, (byte) 0x80, 0x00, 0x00});
        MODULE_RESPONSE.put(BarometerBme280.class, new byte[] {0x12, (byte) 0x80, 0x01, 0x00});
        MODULE_RESPONSE.put(BarometerBmp280.class, new byte[] {0x12, (byte) 0x80, 0x00, 0x00});
        MODULE_RESPONSE.put(Gyro.class, new byte[] {0x13, (byte) 0x80, 0x00, 0x01});
        MODULE_RESPONSE.put(GyroBmi160.class, new byte[] {0x13, (byte) 0x80, 0x00, 0x001});
        MODULE_RESPONSE.put(GyroBmi270.class, new byte[] {0x13, (byte) 0x80, 0x01, 0x00});
        MODULE_RESPONSE.put(AmbientLightLtr329.class, new byte[] {0x14, (byte) 0x80, 0x00, 0x00});
        MODULE_RESPONSE.put(MagnetometerBmm150.class, new byte[] {0x15, (byte) 0x80, 0x00, 0x01});
        MODULE_RESPONSE.put(HumidityBme280.class, new byte[] {0x16, (byte) 0x80, 0x00, 0x00});
        MODULE_RESPONSE.put(ColorTcs34725.class, new byte[] {0x17, (byte) 0x80, 0x00, 0x00});
        MODULE_RESPONSE.put(ProximityTsl2671.class, new byte[] {0x18, (byte) 0x80, 0x00, 0x00});
        MODULE_RESPONSE.put(SensorFusionBosch.class, new byte[] {0x19, (byte) 0x80, 0x00, 0x00, 0x03, 0x00, 0x06, 0x00, 0x02, 0x00, 0x01, 0x00});
        MODULE_RESPONSE.put(Debug.class, new byte[] {(byte) 0xfe, (byte) 0x80, 0x00, 0x00});
    }

    final byte[] modelNumber, hardwareRevision, manufacturer, serialNumber;
    final Map<Byte, byte[]> moduleResponses;
    final Model model;

    private MetaWearBoardInfo(Model model, String modelNumber, String hardwareRevision) {
        this.model = model;
        this.modelNumber = modelNumber.getBytes();
        this.hardwareRevision = hardwareRevision.getBytes();
        this.manufacturer = new byte[] {0x4d, 0x62, 0x69, 0x65, 0x6e, 0x74, 0x4c, 0x61, 0x62, 0x20, 0x49, 0x6e, 0x63};
        this.serialNumber = new byte[] {0x30, 0x30, 0x33, 0x42, 0x46, 0x39};
        moduleResponses = new HashMap<>();
    }

    public MetaWearBoardInfo(Model model, String modelNumber, String hardwareRevision, byte[][] moduleResponsesArray) {
        this(model, modelNumber, hardwareRevision);

        for(byte[] response: moduleResponsesArray) {
            this.moduleResponses.put(response[0], response);
        }
    }

    public MetaWearBoardInfo(String modelNumber, Class<?> ... moduleClasses) {
        this(null, modelNumber, "cafebabe");

        this.moduleResponses.putAll(createModuleResponses(moduleClasses));
    }

    public MetaWearBoardInfo(Class<?> ... moduleClasses) {
        this("deadbeef", moduleClasses);
    }

    @Override
    public String toString() {
        return model.name();
    }

    static final MetaWearBoardInfo CPRO= new MetaWearBoardInfo(Model.METAWEAR_CPRO, "2", "0.2", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x00},
            {0x03, (byte) 0x80, 0x01, 0x01},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02},
            {0x05, (byte) 0x80, 0x00, 0x00, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x00, 0x1C},
            {0x0A, (byte) 0x80, 0x00, 0x00, 0x1C},
            {0x0B, (byte) 0x80, 0x00, 0x02, 0x08, (byte) 0x80, 0x2B, 0x00, 0x00},
            {0x0C, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0D, (byte) 0x80, 0x00, 0x00},
            {0x0F, (byte) 0x80, 0x00, 0x00},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x00},
            {0x12, (byte) 0x80, 0x00, 0x00},
            {0x13, (byte) 0x80, 0x00, 0x01},
            {0x14, (byte) 0x80, 0x00, 0x00},
            {0x15, (byte) 0x80, 0x00, 0x00},
            {0x16, (byte) 0x80},
            {0x17, (byte) 0x80},
            {0x18, (byte) 0x80},
            {0x19, (byte) 0x80},
            {(byte) 0xFE, (byte) 0x80, 0x00, 0x00}
    }),
    DETECTOR= new MetaWearBoardInfo(Model.METADETECT, "2", "0.2", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x00},
            {0x03, (byte) 0x80, 0x03, 0x01},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02},
            {0x05, (byte) 0x80, 0x00, 0x01, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0a, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0b, (byte) 0x80, 0x00, 0x02, 0x08, (byte) 0x80, 0x2b, 0x00, 0x00},
            {0x0c, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0d, (byte) 0x80, 0x00, 0x00},
            {0x0f, (byte) 0x80, 0x00, 0x01, 0x08},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x03},
            {0x12, (byte) 0x80},
            {0x13, (byte) 0x80},
            {0x14, (byte) 0x80, 0x00, 0x00},
            {0x15, (byte) 0x80},
            {0x16, (byte) 0x80},
            {0x17, (byte) 0x80},
            {0x18, (byte) 0x80, 0x00, 0x00},
            {0x19, (byte) 0x80},
            {(byte) 0xfe, (byte) 0x80, 0x00, 0x00},
    }),
    ENVIRONMENT= new MetaWearBoardInfo(Model.METAENV, "2", "0.2", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x00},
            {0x03, (byte) 0x80, 0x03, 0x01},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02},
            {0x05, (byte) 0x80, 0x00, 0x01, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0a, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0b, (byte) 0x80, 0x00, 0x02, 0x08, (byte) 0x80, 0x2b, 0x00, 0x00},
            {0x0c, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0d, (byte) 0x80, 0x00, 0x00},
            {0x0f, (byte) 0x80, 0x00, 0x01, 0x08},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x03},
            {0x12, (byte) 0x80, 0x01, 0x00},
            {0x13, (byte) 0x80},
            {0x14, (byte) 0x80},
            {0x15, (byte) 0x80},
            {0x16, (byte) 0x80, 0x00, 0x00},
            {0x17, (byte) 0x80, 0x00, 0x00},
            {0x18, (byte) 0x80},
            {0x19, (byte) 0x80},
            {(byte) 0xfe, (byte) 0x80, 0x00, 0x00},
    }),
    RPRO= new MetaWearBoardInfo(Model.METAWEAR_RPRO, "1", "0.3", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x00},
            {0x03, (byte) 0x80, 0x01, 0x01},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02},
            {0x05, (byte) 0x80, 0x00, 0x00, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x00, 0x1C},
            {0x0A, (byte) 0x80, 0x00, 0x00, 0x1C},
            {0x0B, (byte) 0x80, 0x00, 0x02, 0x08, (byte) 0x80, 0x2D, 0x00, 0x00},
            {0x0C, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0D, (byte) 0x80, 0x00, 0x00},
            {0x0F, (byte) 0x80, 0x00, 0x00},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x00},
            {0x12, (byte) 0x80, 0x00, 0x00},
            {0x13, (byte) 0x80, 0x00, 0x01},
            {0x14, (byte) 0x80, 0x00, 0x00},
            {0x15, (byte) 0x80},
            {0x16, (byte) 0x80},
            {0x17, (byte) 0x80},
            {0x18, (byte) 0x80},
            {0x19, (byte) 0x80},
            {(byte) 0xFE, (byte) 0x80, 0x00, 0x00}
    }),
    R= new MetaWearBoardInfo(Model.METAWEAR_R, "0", "0.2", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x00},
            {0x03, (byte) 0x80, 0x00, 0x01},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x01},
            {0x05, (byte) 0x80, 0x00, 0x00, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x00, 0x1C},
            {0x0A, (byte) 0x80, 0x00, 0x00, 0x1C},
            {0x0B, (byte) 0x80, 0x00, 0x02, 0x08, (byte) 0x80, 0x31, 0x00, 0x00},
            {0x0C, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0D, (byte) 0x80, 0x00, 0x00},
            {0x0F, (byte) 0x80, 0x00, 0x00},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x00},
            {0x12, (byte) 0x80},
            {0x13, (byte) 0x80},
            {0x14, (byte) 0x80},
            {0x15, (byte) 0x80},
            {0x16, (byte) 0x80},
            {0x17, (byte) 0x80},
            {0x18, (byte) 0x80},
            {0x19, (byte) 0x80},
            {(byte) 0xFE, (byte) 0x80, 0x00, 0x00}
    }),
    RG= new MetaWearBoardInfo(Model.METAWEAR_RG, "1", "0.2", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x00},
            {0x03, (byte) 0x80, 0x01, 0x01},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02},
            {0x05, (byte) 0x80, 0x00, 0x00, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0a, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0b, (byte) 0x80, 0x00, 0x02, 0x08, (byte) 0x80, 0x2D, 0x00, 0x00},
            {0x0c, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0d, (byte) 0x80, 0x00, 0x00},
            {0x0f, (byte) 0x80, 0x00, 0x00},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x00},
            {0x12, (byte) 0x80},
            {0x13, (byte) 0x80, 0x00, 0x01},
            {0x14, (byte) 0x80},
            {0x15, (byte) 0x80},
            {0x16, (byte) 0x80},
            {0x17, (byte) 0x80},
            {0x18, (byte) 0x80},
            {0x19, (byte) 0x80},
            {(byte) 0xfe, (byte) 0x80, 0x00, 0x00}
    }),
    MOTION_R = new MetaWearBoardInfo(Model.METAMOTION_R, "5", "0.1", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x00},
            {0x03, (byte) 0x80, 0x01, 0x01},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02},
            {0x05, (byte) 0x80, 0x00, 0x00, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x01, 0x1c},
            {0x0a, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0b, (byte) 0x80, 0x00, 0x02, 0x08, (byte) 0x80, 0x2D, 0x00, 0x00},
            {0x0c, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0d, (byte) 0x80, 0x00, 0x01},
            {0x0f, (byte) 0x80, 0x00, 0x01, 0x08},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x05, 0x03},
            {0x12, (byte) 0x80, 0x00, 0x00},
            {0x13, (byte) 0x80, 0x00, 0x01},
            {0x14, (byte) 0x80, 0x00, 0x00},
            {0x15, (byte) 0x80, 0x00, 0x01},
            {0x16, (byte) 0x80},
            {0x17, (byte) 0x80},
            {0x18, (byte) 0x80},
            {0x19, (byte) 0x80, 0x00, 0x00, 0x03, 0x00, 0x06, 0x00, 0x02, 0x00, 0x01, 0x00},
            {(byte) 0xfe, (byte) 0x80, 0x00, 0x00}
    }),
    MOTION_RL = new MetaWearBoardInfo(Model.METAMOTION_RL, "5", "0.5", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x00},
            {0x03, (byte) 0x80, 0x01, 0x01},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02},
            {0x05, (byte) 0x80, 0x00, 0x00, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x01, 0x1c},
            {0x0a, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0b, (byte) 0x80, 0x00, 0x02, 0x08, (byte) 0x80, 0x2D, 0x00, 0x00},
            {0x0c, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0d, (byte) 0x80, 0x00, 0x01},
            {0x0f, (byte) 0x80, 0x00, 0x01, 0x08},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x05, 0x03},
            {0x12, (byte) 0x80},
            {0x13, (byte) 0x80, 0x00, 0x01},
            {0x14, (byte) 0x80},
            {0x15, (byte) 0x80, 0x00, 0x01},
            {0x16, (byte) 0x80},
            {0x17, (byte) 0x80},
            {0x18, (byte) 0x80},
            {0x19, (byte) 0x80, 0x00, 0x00, 0x03, 0x00, 0x06, 0x00, 0x02, 0x00, 0x01, 0x00},
            {(byte) 0xfe, (byte) 0x80, 0x00, 0x00}
    }),
    MOTION_S = new MetaWearBoardInfo(Model.METAMOTION_S, "8", "0.1", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x01, 0x03, 0x06},
            {0x03, (byte) 0x80, 0x04, 0x00},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02},
            {0x05, (byte) 0x80, 0x00, 0x02, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x03, 0x1c},
            {0x0a, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0b, (byte) 0x80, 0x00, 0x03, 0x08, 0x00, 0x00, (byte) 0xfb, 0x03, 0x00, 0x02},
            {0x0c, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0d, (byte) 0x80, 0x00, 0x01},
            {0x0f, (byte) 0x80, 0x00, 0x02, 0x08, 0x00},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x09, 0x07, 0x00},
            {0x12, (byte) 0x80, 0x00, 0x00},
            {0x13, (byte) 0x80, 0x01, 0x00},
            {0x14, (byte) 0x80, 0x00, 0x00},
            {0x15, (byte) 0x80, 0x00, 0x02},
            {0x16, (byte) 0x80},
            {0x17, (byte) 0x80},
            {0x18, (byte) 0x80},
            {0x19, (byte) 0x80, 0x00, 0x03, 0x03, 0x00, 0x06, 0x00, 0x02, 0x00, 0x01, 0x00},
            {(byte) 0xfe, (byte) 0x80, 0x00, 0x05, 0x03}
    }),
    TRACKER = new MetaWearBoardInfo(Model.METATRACKER, "4", "0.1", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x01, 0x03, 0x00},
            {0x03, (byte) 0x80, 0x01, 0x01},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02},
            {0x05, (byte) 0x80, 0x00, 0x02, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0a, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0b, (byte) 0x80, 0x00, 0x02, 0x08, 0x00, 0x00, 0x04, 0x00},
            {0x0c, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0d, (byte) 0x80, 0x00, 0x01},
            {0x0f, (byte) 0x80, 0x00, 0x01, 0x08},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x04},
            {0x12, (byte) 0x80, 0x01, 0x00},
            {0x13, (byte) 0x80, 0x00, 0x01},
            {0x14, (byte) 0x80, 0x00, 0x00},
            {0x15, (byte) 0x80},
            {0x16, (byte) 0x80, 0x00, 0x00},
            {0x17, (byte) 0x80},
            {0x18, (byte) 0x80},
            {0x19, (byte) 0x80},
            {(byte) 0xfe, (byte) 0x80, 0x00, 0x01}
    }),
    C = new MetaWearBoardInfo(Model.METAWEAR_C, "2", "0.3", new byte[][] {
            {0x01, (byte) 0x80, 0x00, 0x00},
            {0x02, (byte) 0x80, 0x00, 0x00},
            {0x03, (byte) 0x80, 0x01, 0x00},
            {0x04, (byte) 0x80, 0x01, 0x00, 0x00, 0x03, 0x01, 0x02},
            {0x05, (byte) 0x80, 0x00, 0x02, 0x03, 0x03, 0x03, 0x03, 0x01, 0x01, 0x01, 0x01},
            // {0x06, (byte) 0x80, 0x00, 0x00}, Deprecated NEO_PIXEL
            // {0x07, (byte) 0x80, 0x00, 0x00}, Deprecated IBEACON
            {0x08, (byte) 0x80, 0x00, 0x00},
            {0x09, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0a, (byte) 0x80, 0x00, 0x00, 0x1c},
            {0x0b, (byte) 0x80, 0x00, 0x02, 0x08, (byte) 0x80, 0x1f, 0x00, 0x00},
            {0x0c, (byte) 0x80, 0x00, 0x00, 0x08},
            {0x0d, (byte) 0x80, 0x00, 0x00},
            {0x0f, (byte) 0x80, 0x00, 0x01, 0x08},
            // {0x10, (byte) 0x80}, Deprecated GSR
            {0x11, (byte) 0x80, 0x00, 0x03},
            {0x12, (byte) 0x80},
            {0x13, (byte) 0x80, 0x00, 0x00},
            {0x14, (byte) 0x80},
            {0x15, (byte) 0x80},
            {0x16, (byte) 0x80},
            {0x17, (byte) 0x80},
            {0x18, (byte) 0x80},
            {0x19, (byte) 0x80},
            {(byte) 0xfe, (byte) 0x80, 0x00, 0x00}
    });
}
