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

import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.MetaWearBoard;

/**
 * Interacts with the sensor fusion algorithm on the MetaMotion boards.  When using the sensor fusion
 * algorithm, do not use the {@link Accelerometer}, {@link Gyro}, or {@link Bmm150Magnetometer} modules.
 * @author Eric Tsai
 */
public interface SensorFusion extends MetaWearBoard.Module {
    /**
     * Available acceleration ranges
     * @author Eric Tsai
     */
    enum AccRange {
        AR_2G,
        AR_4G,
        AR_8G,
        AR_16G,
    }
    /**
     * Available rotation ranges
     * @author Eric Tsai
     */
    enum GyroRange {
        GR_2000DPS,
        GR_1000DPS,
        GR_500DPS,
        GR_250DPS
    }

    enum CalibrationAccuracy {
        UNRELIABLE,
        LOW_ACCURACY,
        MEDIUM_ACCURACY,
        HIGH_ACCURACY
    }

    /**
     * Supported fusion modes
     * @author Eric Tsai
     */
    enum Mode {
        /** algorithm is inactive */
        SLEEP,
        /** Calculates absolute roeintation from accelerometer, gyro, and magnetometer */
        NDOF,
        /** Calculates relative orientation in space from accelerometer and gyro data */
        IMU_PLUS,
        /** Determines geographic direction from th Earth's magnetic field */
        COMPASS,
        /** Similar to IMUPlus except rotation is detected with the magnetometer */
        M4G
    }

    enum DataOutput {
        CORRECTED_ACC,
        CORRECTED_GYRO,
        CORRECTED_MAG,
        QUATERNION,
        EULER_ANGLES,
        GRAVITY_VECTOR,
        LINEAR_ACC
    }

    /**
     * Variant of the {@link com.mbientlab.metawear.data.CartesianFloat CartesianFloat} type that includes
     * the calibration accuracy of the data
     * @author Eric Tsai
     */
    interface CorrectedCartesianFloat {
        float x();
        float y();
        float z();
        CalibrationAccuracy accuracy();
    }

    /**
     * wrapper class for holding Euler angles.  All values returned are in degrees (&deg;)
     * @author Eric Tsai
     */
    interface EulerAngle {
        float heading();
        float pitch();
        float yaw();
        float roll();
    }
    /**
     *  Wrapper class for holding a normalized quaternion.  Each value is unit-less.
     *  @author Eric Tsai
     */
    interface Quaternion {
        float w();
        float x();
        float y();
        float z();
    }

    /**
     * Selector for available data sources from the sensor fusion algorithm
     * @author Eric Tsai
     */
    interface SourceSelector {
        DataSignal fromCorrectedAcc();
        DataSignal fromCorrectedGyro();
        DataSignal fromCorrectedMag();
        DataSignal fromGravity();
        DataSignal fromEulerAngles();
        DataSignal fromQuaternions();
        DataSignal fromLinearAcceleration();
    }

    /**
     * Interface for configuring the sensor fusion algorithm
     * @author Eric Tsai
     */
    interface ConfigEditor {
        ConfigEditor setMode(Mode mode);
        ConfigEditor setAccRange(AccRange range);
        ConfigEditor setGyroRange(GyroRange range);
        void commit();
    }
    /**
     * Initiates the creation of a route for senesor fusion data
     * @return Selection of available data sources
     */
    SourceSelector routeData();

    /**
     * Configures the algorithm
     * @return Editor object to configure the settings
     */
    ConfigEditor configure();

    /**
     * Starts the sensor fusion algorithm
     * @param output    Types of data to listen to from the algorithm
     */
    void start(DataOutput ... output);
    /**
     * Stops the sensor fusion algorithm
     */
    void stop();


}
