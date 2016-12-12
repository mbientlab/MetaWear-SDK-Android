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

package com.mbientlab.metawear.module;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.MetaWearBoard.Module;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.data.MagneticField;

import java.util.Locale;

/**
 * Controls the sensor fusion algorithm running on the MetaMotion boards.  When using sensor fusion,
 * do not configure accelerometer, gyro, and magnetometer with their respective APIs; the sensor fusion
 * module will automatically configure those sensors based on the selected fusion mode.
 * @author Eric Tsai
 */
public interface SensorFusionBosch extends Module {
    /**
     * Container class holding corrected acceleration data, in units of g's
     * @author Eric Tsai
     */
    final class CorrectedAcceleration extends Acceleration {
        private final CalibrationAccuracy accuracy;

        public CorrectedAcceleration(float x, float y, float z, byte accuracy) {
            super(x, y, z);
            this.accuracy = CalibrationAccuracy.values()[accuracy];
        }

        public CalibrationAccuracy accuracy() {
            return accuracy;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "{x: %.3fg, y: %.3fg, z: %.3fg, accuracy: %s}", x(), y(), z(), accuracy.toString());
        }
    }
    /**
     * Container class holding corrected angular velocity data, in degrees per second
     * @author Eric Tsai
     */
    final class CorrectedAngularVelocity extends AngularVelocity {
        private final CalibrationAccuracy accuracy;

        public CorrectedAngularVelocity(float x, float y, float z, byte accuracy) {
            super(x, y, z);
            this.accuracy = CalibrationAccuracy.values()[accuracy];
        }

        public CalibrationAccuracy accuracy() {
            return accuracy;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "{x: %.3f%s, y: %.3f%s, z: %.3f%s, accuracy: %s}",
                    x(), DEGS_PER_SEC,
                    y(), DEGS_PER_SEC,
                    z(), DEGS_PER_SEC,
                    accuracy.toString()
            );
        }
    }
    /**
     * Container class holding corrected magnetic field strength data, in micro teslas
     * @author Eric Tsai
     */
    final class CorrectedMagneticField extends MagneticField {
        private final CalibrationAccuracy accuracy;

        public CorrectedMagneticField(float x, float y, float z, byte accuracy) {
            super(x, y, z);
            this.accuracy = CalibrationAccuracy.values()[accuracy];
        }

        public CalibrationAccuracy accuracy() {
            return accuracy;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "{x: %.3f%s, y: %.3f%s, z: %.3f%s, accuracy: %s}",
                    x(), MICRO_TESLA,
                    y(), MICRO_TESLA,
                    z(), MICRO_TESLA,
                    accuracy.toString()
            );
        }
    }

    /**
     * Supported data ranges for accelerometer data
     * @author Eric Tsai
     */
    enum AccRange {
        /** +/-2g */
        AR_2G,
        /** +/-4g */
        AR_4G,
        /** +/-8g */
        AR_8G,
        /** +/-16g */
        AR_16G,
    }
    /**
     * Supported data ranges for gyro data
     * @author Eric Tsai
     */
    enum GyroRange {
        /** +/-2000 degs/s */
        GR_2000DPS,
        /** +/-1000 degs/s */
        GR_1000DPS,
        /** +/-500 degs/s */
        GR_500DPS,
        /** +/-250 degs/s */
        GR_250DPS,
    }
    /**
     * Accuracy of the correct sensor data
     * @author Eric Tsai
     */
    enum CalibrationAccuracy {
        UNRELIABLE,
        LOW_ACCURACY,
        MEDIUM_ACCURACY,
        HIGH_ACCURACY
    }
    /**
     * Available sensor fusion modes
     * @author Eric Tsai
     */
    enum Mode {
        SLEEP,
        NDOF,
        IMU_PLUS,
        COMPASS,
        M4G
    }

    /**
     * Configuration editor for the sensor fusion algorithm
     * @author Eric Tsai
     */
    interface ConfigEditor {
        /**
         * Sets the sensor fusion mode
         * @param mode    New sensor fusion mode
         * @return Calling object
         */
        ConfigEditor mode(Mode mode);
        /**
         * Sets the accelerometer data range
         * @param range    New data range
         * @return Calling object
         */
        ConfigEditor accRange(AccRange range);
        /**
         * Sets the gyro data range
         * @param range    New data range
         * @return Calling object
         */
        ConfigEditor gyroRange(GyroRange range);
        /**
         * Write the changes to the algorithm
         */
        void commit();
    }
    /**
     * Configures the algorithm
     * @return Configuration editor object
     */
    ConfigEditor configure();

    /**
     * Gets the object to control corrected acceleration data
     * @return Object controlling corrected acceleration data
     */
    AsyncDataProducer correctedAcceleration();
    /**
     * Gets the object to control corrected rotation data
     * @return Object controlling corrected rotation data
     */
    AsyncDataProducer correctedRotation();
    /**
     * Gets the object to control corrected B field data
     * @return Object controlling corrected B field data
     */
    AsyncDataProducer correctedBField();
    /**
     * Gets the object to control quaternion data
     * @return Object controlling quaternion data
     */
    AsyncDataProducer quaternion();
    /**
     * Gets the object to control Euler angles data
     * @return Object controlling Euler angles data
     */
    AsyncDataProducer eulerAngles();
    /**
     * Gets the object to control gravity data
     * @return Object controlling gravity data
     */
    AsyncDataProducer gravity();
    /**
     * Gets the object to control linear acceleration data
     * @return Object controlling linear acceleration data
     */
    AsyncDataProducer linearAcceleration();

    /**
     * Starts the algorithm
     */
    void start();
    /**
     * Stops the algorithm
     */
    void stop();
}
