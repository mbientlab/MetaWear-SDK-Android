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

/**
 * Created by etsai on 9/20/16.
 */
public interface MagnetometerBmm150 extends Module {
    /**
     * Preset power modes for the magnetometer as outlined in the specs sheet.
     * <table>
     *     <thead>
     *         <tr>
     *             <th>Setting</th>
     *             <th>ODR</th>
     *             <th>Average Current</th>
     *             <th>Noise</th>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>LOW_POWER</td>
     *             <td>10Hz</td>
     *             <td>170&#956;A</td>
     *             <td>1.0&#956;T (xy axis), 1.4&#956;T (z axis)</td>
     *         </tr>
     *         <tr>
     *             <td>REGULAR</td>
     *             <td>10Hz</td>
     *             <td>0.5mA</td>
     *             <td>0.6&#956;T</td>
     *         </tr>
     *         <tr>
     *             <td>ENHANCED_REGULAR</td>
     *             <td>10Hz</td>
     *             <td>0.8mA</td>
     *             <td>0.5&#956;T</td>
     *         </tr>
     *         <tr>
     *             <td>HIGH_ACCURACY</td>
     *             <td>20Hz</td>
     *             <td>4.9mA</td>
     *             <td>0.3&#956;T</td>
     *         </tr>
     *     </tbody>
     * </table>
     * @author Eric Tsai
     */
    enum Preset {
        LOW_POWER,
        REGULAR,
        ENHANCED_REGULAR,
        HIGH_ACCURACY
    }
    /**
     * Supported output data rates for the BMM150 sensor
     * @author Eric Tsai
     */
    enum OutputDataRate {
        ODR_10_HZ,
        ODR_2_HZ,
        ODR_6_HZ,
        ODR_8_HZ,
        ODR_15_HZ,
        ODR_20_HZ,
        ODR_25_HZ,
        ODR_30_HZ
    }

    interface MagneticFieldDataProducer extends AsyncDataProducer {
        String xAxisName();
        String yAxisName();
        String zAxisName();
    }
    MagneticFieldDataProducer magneticField();
    AsyncDataProducer packedMagneticField();

    interface ConfigEditor {
        ConfigEditor xyReps(short reps);
        ConfigEditor zReps(short reps);
        ConfigEditor outputDataRate(OutputDataRate odr);
        void commit();
    }
    ConfigEditor configure();

    /**
     * Sets the power mode to one of the preset configurations
     * @param preset    Preset to use
     */
    void usePreset(Preset preset);

    /**
     * Switch the magnetometer into normal mode
     */
    void start();
    /**
     * Switch the magnetometer into sleep mode
     */
    void stop();
}
