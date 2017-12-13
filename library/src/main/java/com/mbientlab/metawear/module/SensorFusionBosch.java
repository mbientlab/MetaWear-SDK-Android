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
import com.mbientlab.metawear.ConfigEditorBase;
import com.mbientlab.metawear.Configurable;
import com.mbientlab.metawear.MetaWearBoard.Module;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.data.MagneticField;
import com.mbientlab.metawear.data.Quaternion;

import java.util.Locale;

import bolts.Task;

/**
 * Algorithm combining accelerometer, gyroscope, and magnetometer data for Bosch sensors.  When using
 * sensor fusion, do not configure the accelerometer, gyro, and magnetometer with their respective interface;
 * the algorithm will automatically configure those sensors based on the selected fusion mode.
 * @author Eric Tsai
 */
public interface SensorFusionBosch extends Module, Configurable<SensorFusionBosch.ConfigEditor> {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            CorrectedAcceleration that = (CorrectedAcceleration) o;

            return accuracy == that.accuracy;

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + accuracy.hashCode();
            return result;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            CorrectedAngularVelocity that = (CorrectedAngularVelocity) o;

            return accuracy == that.accuracy;

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + accuracy.hashCode();
            return result;
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
            return String.format(Locale.US, "{x: %.9fT, y: %.9fT, z: %.9fT, accuracy: %s}",
                    x(), y(), z(), accuracy.toString()
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            CorrectedMagneticField that = (CorrectedMagneticField) o;

            return accuracy == that.accuracy;

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + accuracy.hashCode();
            return result;
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
    interface ConfigEditor extends ConfigEditorBase {
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
         * Extra configuration settings for the accelerometer
         * @param settings Additional accelerometer settings
         * @return Calling object
         */
        ConfigEditor accExtra(Object ... settings);
        /**
         * Extra configuration settings for the gyroscope
         * @param settings Additional gyroscope settings
         * @return Calling object
         */
        ConfigEditor gyroExtra(Object ... settings);
    }

    /**
     * Get an implementation of the AsyncDataProducer interface for corrected acceleration data,
     * represented by the {@link CorrectedAcceleration} class.
     * @return AsyncDataProducer Object for corrected acceleration data
     */
    AsyncDataProducer correctedAcceleration();
    /**
     * Get an implementation of the AsyncDataProducer interface for corrected angular velocity data,
     * represented by the {@link CorrectedAngularVelocity} class.
     * @return AsyncDataProducer Object for corrected angular velocity data
     */
    AsyncDataProducer correctedAngularVelocity();
    /**
     * Get an implementation of the AsyncDataProducer interface for corrected magnetic field data,
     * represented by the {@link CorrectedMagneticField} class.
     * @return AsyncDataProducer Object for corrected magnetic field data
     */
    AsyncDataProducer correctedMagneticField();
    /**
     * Get an implementation of the AsyncDataProducer interface for quaternion data,
     * represented by the {@link Quaternion} class.
     * @return AsyncDataProducer Object for quaternion data
     */
    AsyncDataProducer quaternion();
    /**
     * Get an implementation of the AsyncDataProducer interface for euler angles,
     * represented by the {@link EulerAngles} class.
     * @return AsyncDataProducer Object for euler angles
     */
    AsyncDataProducer eulerAngles();
    /**
     * Get an implementation of the AsyncDataProducer interface for the acceleration from gravity vector,
     * represented by the {@link Acceleration} class.
     * @return AsyncDataProducer Object for acceleration from gravity
     */
    AsyncDataProducer gravity();
    /**
     * Get an implementation of the AsyncDataProducer interface for linear acceleration,
     * represented by the {@link Acceleration} class.
     * @return AsyncDataProducer Object for linear acceleration
     */
    AsyncDataProducer linearAcceleration();

    /**
     * Start the algorithm
     */
    void start();
    /**
     * Stop the algorithm
     */
    void stop();

    /**
     * Pulls the current sensor fusion configuration from the sensor
     * @return Task that is completed when the settings are received
     */
    Task<Void> pullConfigAsync();
}
