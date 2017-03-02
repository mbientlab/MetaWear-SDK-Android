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

import com.mbientlab.metawear.ConfigEditorBase;
import com.mbientlab.metawear.Configurable;
import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.MetaWearBoard.Module;

/**
 * Digital proximity detector for short-distance detection by AMS
 * @author Eric Tsai
 */
public interface ProximityTsl2671 extends Module, Configurable<ProximityTsl2671.ConfigEditor> {
    /**
     * Photodiodes the sensor should use for proximity detection
     * @author Eric Tsai
     */
    enum ReceiverDiode {
        /** Use the channel 0 diode, which is responsive to both visible and infrared light */
        CHANNEL_0,
        /** Use the channel 1 diode, which is responsive primarily to infrared light */
        CHANNEL_1,
        /** Use both photodiodes */
        BOTH
    }
    /**
     * Amount of current to drive the sensor
     * @author Eric Tsai
     */
    enum TransmitterDriveCurrent {
        CURRENT_100MA,
        CURRENT_50MA,
        CURRENT_25MA,
        CURRENT_12_5MA,
    }
    /**
     * Interface for configuring the sensor
     * @author Eric Tsai
     */
    interface ConfigEditor extends ConfigEditorBase {
        /**
         * Set the integration time
         * @param time    Period of time, in milliseconds, the internal ADC converts the analog signal into digital counts.  Minimum 2.72ms
         * @return Calling object
         */
        ConfigEditor integrationTime(float time);
        /**
         * Set the pulse count.  Sensitivity grows by the square root of the number of pulses
         * @param nPulses    Number of pulses to use for detection, between [1, 255]
         * @return Calling object
         */
        ConfigEditor pulseCount(byte nPulses);
        /**
         * Set the photodiode for responding to light
         * @param diode    Photodiode to use
         * @return Calling object
         */
        ConfigEditor receiverDiode(ReceiverDiode diode);
        /**
         * Set the led drive current.  For boards powered by the CR2032 battery, it is recommended to use 25mA or less.
         * @param current    Current driving the sensor
         * @return Calling object
         */
        ConfigEditor transmitterDriveCurrent(TransmitterDriveCurrent current);
    }

    /**
     * Get an implementation of the ForcedDataProducer interface for proximity ADC values, represented as
     * an integer.
     * @return Object managing the proximity data
     */
    ForcedDataProducer adc();
}
