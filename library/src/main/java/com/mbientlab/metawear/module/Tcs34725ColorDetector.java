/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.module;

import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.MetaWearBoard;

/**
 * Controls the TCS34725 color detector.  This sensor is only available on
 * MetaDetector boards.
 * @author Eric Tsai
 */
public interface Tcs34725ColorDetector extends MetaWearBoard.Module {
    /**
     * Analog gain scales
     * @author Eric Tsai
     */
    enum Gain {
        GAIN_1X,
        GAIN_4X,
        GAIN_16X,
        GAIN_60X
    }

    /**
     * Wrapper class encapsulating ADC data from the sensor
     * @author Eric Tsai
     */
    interface ColorAdc {
        /**
         * ADC value from an unfiltered photodiode
         * @return Clear ADC value
         */
        int clear();
        /**
         * ADC value from a red filtered photodiode
         * @return Red ADC value
         */
        int red();
        /**
         * ADC value from a green filtered photodiode
         * @return Green ADC value
         */
        int green();
        /**
         * ADC value from a blue filtered photodiode
         * @return Blue ADC value
         */
        int blue();
    }

    /**
     * Interface for configuring the color detector
     * @author Eric Tsai
     */
    interface ConfigEditor {
        /**
         * Set the integration time.  This impacts both resolution and sensitivity of the ADC values.
         * @param time    Between [2.4, 614.4] milliseconds
         * @return Calling object
         */
        ConfigEditor setIntegrationTime(float time);
        /**
         * Sets the analog gain
         * @param gain    Gain scale
         * @return Calling object
         */
        ConfigEditor setGain(Gain gain);
        /**
         * Write the changes to the board
         */
        void commit();
    }

    /**
     * Selector for color detector data sources
     * @author Eric Tsai
     */
    interface SourceSelector {
        /**
         * Handle data from the color detector
         * @param silent    Same value as the silent parameter for calling {@link #readColorAdc(boolean)}
         * @return Object representing sensor data
         */
        DataSignal fromSensor(boolean silent);
    }

    /**
     * Read color ADC values from the detector
     * @param silent    True if read should be silent
     */
    void readColorAdc(boolean silent);
    /**
     * Configure the color detector
     * @return Editor object to configure the detector
     */
    ConfigEditor configure();
    /**
     * Initiates the creation of a route for TCS34725 detector data
     * @return Selection of available data sources
     */
    SourceSelector routeData();
}
