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
import com.mbientlab.metawear.data.Sign;
import com.mbientlab.metawear.data.SensorOrientation;
import com.mbientlab.metawear.data.TapType;

import java.util.HashMap;

/**
 * Extension of the {@link Accelerometer} providing general access to a Bosch accelerometer.  If you know specifically which
 * Bosch accelerometer is on your board, use the appropriate subclass instead.
 * @author Eric Tsai
 * @see AccelerometerBma255
 * @see AccelerometerBmi160
 */
public interface AccelerometerBosch extends Accelerometer {
    /**
     * Available data ranges
     * @author Eric Tsai
     */
    enum AccRange {
        /** +/-2g */
        AR_2G((byte) 0x3, 16384f, 2f),
        /** +/-4g */
        AR_4G((byte) 0x5, 8192f, 4f),
        /** +/-8g */
        AR_8G((byte) 0x8, 4096, 8f),
        /** +/-16g */
        AR_16G((byte) 0xc, 2048f, 16f);

        public final byte bitmask;
        public final float scale, range;

        private static final HashMap<Byte, AccRange> bitMasksToRange;
        static {
            bitMasksToRange= new HashMap<>();
            for(AccRange it: values()) {
                bitMasksToRange.put(it.bitmask, it);
            }
        }

        AccRange(byte bitmask, float scale, float range) {
            this.bitmask = bitmask;
            this.scale = scale;
            this.range= range;
        }

        public static AccRange bitMaskToRange(byte bitMask) {
            return bitMasksToRange.get(bitMask);
        }

        public static float[] ranges() {
            AccRange[] values= values();
            float[] ranges= new float[values.length];
            for(byte i= 0; i < ranges.length; i++) {
                ranges[i]= values[i].range;
            }

            return ranges;
        }
    }

    /**
     * Calculation modes controlling the conditions that determine the sensor's orientation
     * @author Eric Tsai
     */
    enum OrientationMode {
        /** Default mode */
        SYMMETRICAL,
        HIGH_ASYMMETRICAL,
        LOW_ASYMMETRICAL
    }
    /**
     * Configuration editor for the orientation detection algorithm
     * @author Eric Tsai
     */
    interface OrientationConfigEditor extends ConfigEditorBase {
        /**
         * Set the hysteresis offset for portrait/landscape detection
         * @param hysteresis    New offset angle, in degrees
         * @return Calling object
         */
        OrientationConfigEditor hysteresis(float hysteresis);
        /**
         * Set the orientation calculation mode
         * @param mode    New calculation mode
         * @return Calling object
         */
        OrientationConfigEditor mode(OrientationMode mode);
    }
    /**
     * On-board algorithm that detects changes in the sensor's orientation.  Data is represented as
     * a {@link SensorOrientation} object.
     * @author Eric Tsai
     */
    interface OrientationDataProducer extends AsyncDataProducer, Configurable<OrientationConfigEditor> { }
    /**
     * Get an implementation of the OrientationDataProducer interface
     * @return OrientationDataProducer object
     */
    OrientationDataProducer orientation();

    /**
     * Accelerometer agnostic interface for configuring flat detection algorithm
     * @param <T>    Type of flat detection config editor
     * @author Eric Tsai
     */
    interface FlatConfigEditor<T extends FlatConfigEditor> extends ConfigEditorBase {
        /**
         * Set the delay for which the flat value must remain stable for a flat interrupt.  The closest,
         * valid delay will be chosen depending on underlying sensor
         * @param time    Delay time for a stable value
         * @return Calling object
         */
        T holdTime(float time);
        /**
         * Set the threshold defining a flat position
         * @param angle    Threshold angle, between [0, 44.8] degrees
         * @return Calling object
         */
        T flatTheta(float angle);
    }
    /**
     * On-board algorithm that detects whether the senor is laying flat or not
     * @author Eric Tsai
     */
    interface FlatDataProducer extends AsyncDataProducer, Configurable<FlatConfigEditor<? extends FlatConfigEditor>> { }
    /**
     * Get an implementation of the FlatDataProducer interface
     * @return FlatDataProducer object
     */
    FlatDataProducer flat();

    /**
     * Wrapper class encapsulating the data from a low/high g interrupt
     * @author Eric Tsai
     */
    class LowHighResponse {
        /** True if the interrupt from from low-g motion */
        public final boolean isLow;
        /**
         * True if the interrupt from from high-g motion.  If it is not high-g motion, there is no
         * need to check the high-g variables
         */
        public final boolean isHigh;
        /** True if the x-axis triggered high-g interrupt */
        public final boolean highGx;
        /** True if the y-axis triggered high-g interrupt */
        public final boolean highGy;
        /** True if the z-axis triggered high-g interrupt */
        public final boolean highGz;
        /** Direction of the high-g motion interrupt */
        public final Sign highSign;

        public LowHighResponse(boolean isHigh, boolean isLow, boolean highGx, boolean highGy, boolean highGz, Sign highSign) {
            this.isHigh = isHigh;
            this.isLow = isLow;
            this.highGx = highGx;
            this.highGy = highGy;
            this.highGz = highGz;
            this.highSign = highSign;
        }

        @Override
        public boolean equals(Object o) {
            // Generated by IntelliJ
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LowHighResponse that = (LowHighResponse) o;

            return isHigh == that.isHigh && isLow == that.isLow &&
                    highGx == that.highGx && highGy == that.highGy && highGz == that.highGz && highSign == that.highSign;

        }

        @Override
        public int hashCode() {
            // Generated by IntelliJ
            int result = (isHigh ? 1 : 0);
            result = 31 * result + (isLow ? 1 : 0);
            result = 31 * result + (highGx ? 1 : 0);
            result = 31 * result + (highGy ? 1 : 0);
            result = 31 * result + (highGz ? 1 : 0);
            result = 31 * result + highSign.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("{low: %s, high: %s, high_x: %s, high_y: %s, high_z: %s, high_direction: %s}",
                    isLow, isLow, highGx, highGy, highGz, highSign.toString());
        }
    }
    /**
     * Interrupt modes for low-g detection
     * @author Eric Tsai
     */
    enum LowGMode {
        /** Compare |acc_x|, |acc_y|, |acc_z| with the low threshold */
        SINGLE,
        /** Compare |acc_x| + |acc_y| + |acc_z| with the low threshold */
        SUM
    }
    /**
     * Interface for configuring low/high g detection
     * @author Eric Tsai
     */
    interface LowHighConfigEditor extends ConfigEditorBase {
        /**
         * Enable low g detection on all 3 axes
         * @return Calling object
         */
        LowHighConfigEditor enableLowG();
        /**
         * Enable high g detection on the x-axis
         * @return Calling object
         */
        LowHighConfigEditor enableHighGx();
        /**
         * Enable high g detection on the y-axis
         * @return Calling object
         */
        LowHighConfigEditor enableHighGy();
        /**
         * Enable high g detection on the z-axis
         * @return Calling object
         */
        LowHighConfigEditor enableHighGz();
        /**
         * Set the minimum amount of time the acceleration must stay below (ths + hys) for an interrupt
         * @param duration    Duration between [2.5, 640] milliseconds
         * @return Calling object
         */
        LowHighConfigEditor lowDuration(int duration);
        /**
         * Set the threshold that triggers a low-g interrupt
         * @param threshold    Low-g interrupt threshold, between [0.00391, 2.0] g
         * @return Calling object
         */
        LowHighConfigEditor lowThreshold(float threshold);
        /**
         * Set the hysteresis level for low-g interrupt
         * @param hysteresis    Low-g interrupt hysteresis, between [0, 0.375]g
         * @return Calling object
         */
        LowHighConfigEditor lowHysteresis(float hysteresis);
        /**
         * Set mode for low-g detection
         * @param mode    Low-g detection mode
         * @return Calling object
         */
        LowHighConfigEditor lowGMode(LowGMode mode);
        /**
         * Set the minimum amount of time the acceleration sign does not change for an interrupt
         * @param duration    Duration between [2.5, 640] milliseconds
         * @return Calling object
         */
        LowHighConfigEditor highDuration(int duration);
        /**
         * Set the threshold for clearing high-g interrupt
         * @param threshold    High-g clear interrupt threshold
         * @return Calling object
         */
        LowHighConfigEditor highThreshold(float threshold);
        /**
         * Set the hysteresis level for clearing the high-g interrupt
         * @param hysteresis    Hysteresis for clearing high-g interrupt
         * @return Calling object
         */
        LowHighConfigEditor highHysteresis(float hysteresis);
    }
    /**
     * On-board algorithm that detects when low (i.e. free fall) or high g acceleration is measured
     * @author Eric Tsai
     */
    interface LowHighDataProducer extends AsyncDataProducer, Configurable<LowHighConfigEditor> { }
    /**
     * Get an implementation of the LowHighDataProducer interface
     * @return LowHighDataProducer object
     */
    LowHighDataProducer lowHigh();

    /**
     * Motion detection algorithms on Bosch sensors.  Only one type of motion detection can be active at a time.
     * @author Eric Tsai
     */
    interface MotionDetection {}
    /**
     * Get an implementation of the MotionDetection interface.
     * @param motionClass    Type of motion detection to use
     * @param <T>            Runtime type the returned value is casted as
     * @return MotionDetection object, null if the motion detection type is not supported
     */
    <T extends MotionDetection> T motion(Class<T> motionClass);

    /**
     * Configuration editor for no-motion detection
     * @author Eric Tsai
     */
    interface NoMotionConfigEditor extends ConfigEditorBase {
        /**
         * Set the duration
         * @param duration    Time, in milliseconds, for which no slope data points exceed the threshold
         * @return Calling object
         */
        NoMotionConfigEditor duration(int duration);
        /**
         * Set the tap threshold.  This value is shared with slow motion detection.
         * @param threshold    Threshold, in Gs, for which no slope data points must exceed
         * @return Calling object
         */
        NoMotionConfigEditor threshold(float threshold);
    }
    /**
     * Detects when the slope of acceleration data is below a threshold for a period of time.
     * @author Eric Tsai
     */
    interface NoMotionDataProducer extends MotionDetection, AsyncDataProducer, Configurable<NoMotionConfigEditor> { }
    /**
     * Wrapper class encapsulating interrupts from any motion detection
     * @author Eric Tsai
     */
    class AnyMotion {
        /** Slope sign of the triggering motion */
        public final Sign sign;
        /** True if x-axis triggered the motion interrupt */
        public final boolean xAxisActive;
        /** True if y-axis triggered the motion interrupt */
        public final boolean yAxisActive;
        /** True if z-axis triggered the motion interrupt */
        public final boolean zAxisActive;

        public AnyMotion(Sign sign, boolean xAxisActive, boolean yAxisActive, boolean zAxisActive) {
            this.sign = sign;
            this.xAxisActive = xAxisActive;
            this.yAxisActive = yAxisActive;
            this.zAxisActive = zAxisActive;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AnyMotion that = (AnyMotion) o;

            return xAxisActive == that.xAxisActive && yAxisActive == that.yAxisActive &&
                    zAxisActive == that.zAxisActive && sign == that.sign;

        }

        @Override
        public int hashCode() {
            int result = sign.hashCode();
            result = 31 * result + (xAxisActive ? 1 : 0);
            result = 31 * result + (yAxisActive ? 1 : 0);
            result = 31 * result + (zAxisActive ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return String.format("{direction: %s, x-axis active: %s, y-axis active: %s, z-axis active: %s}",
                    sign, xAxisActive, yAxisActive, zAxisActive);
        }
    }
    /**
     * Configuration editor for any-motion detection
     * @author Eric Tsai
     */
    interface AnyMotionConfigEditor extends ConfigEditorBase {
        /**
         * Set the number of consecutive slope data points that must be above the threshold for an interrupt to occur
         * @param count    Number of consecutive slope data points
         * @return Calling object
         */
        AnyMotionConfigEditor count(int count);
        /**
         * Set the threshold that the slope data points must be above
         * @param threshold    Any motion threshold, in g's
         * @return Calling object
         */
        AnyMotionConfigEditor threshold(float threshold);
    }
    /**
     * Detects when a number of consecutive slope data points is above a threshold.
     * @author Eric Tsai
     */
    interface AnyMotionDataProducer extends MotionDetection, AsyncDataProducer, Configurable<AnyMotionConfigEditor> { }
    /**
     * Configuration editor for slow-motion detection
     * @author Eric Tsai
     */
    interface SlowMotionConfigEditor extends ConfigEditorBase {
        /**
         * Set the number of consecutive slope data points that must be above the threshold for an interrupt to occur
         * @param count    Number of consecutive slope data points
         * @return Calling object
         */
        SlowMotionConfigEditor count(byte count);
        /**
         * Set the tap threshold.  This value is shared with no motion detection
         * @param threshold    Threshold, in Gs, for which no slope data points must exceed
         * @return Calling object
         */
        SlowMotionConfigEditor threshold(float threshold);
    }
    /**
     * Similar to any motion detection except no information is stored regarding what triggered the interrupt.
     * @author Eric Tsai
     */
    interface SlowMotionDataProducer extends MotionDetection, AsyncDataProducer, Configurable<SlowMotionConfigEditor> { }

    /**
     * Wrapper class encapsulating responses from tap detection
     * @author Eric Tsai
     */
    class Tap {
        /** Tap type of the response */
        public final TapType type;
        /** Sign of the triggering signal */
        public final Sign sign;

        public Tap(TapType type, Sign sign) {
            this.type = type;
            this.sign = sign;
        }

        @Override
        public boolean equals(Object o) {
            // Generated by IntelliJ
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Tap that = (Tap) o;

            return type == that.type && sign == that.sign;

        }

        @Override
        public int hashCode() {
            // Generated by IntelliJ
            int result = type.hashCode();
            result = 31 * result + sign.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("{type: %s, direction: %s}", type, sign);
        }
    }
    /**
     * Available quiet times for double tap detection
     * @author Eric Tsai
     */
    enum TapQuietTime {
        /** 30ms */
        TQT_30_MS,
        /** 20ms */
        TQT_20_MS
    }
    /**
     * Available shock times for tap detection
     * @author Eric Tsai
     */
    enum TapShockTime {
        /** 50ms */
        TST_50_MS,
        /** 75ms */
        TST_75_MS
    }
    /**
     * Available windows for double tap detection
     * @author Eric Tsai
     */
    enum DoubleTapWindow {
        /** 50ms */
        DTW_50_MS,
        /** 100ms */
        DTW_100_MS,
        /** 150ms */
        DTW_150_MS,
        /** 200ms */
        DTW_200_MS,
        /** 250ms */
        DTW_250_MW,
        /** 375ms */
        DTW_375_MS,
        /** 500ms */
        DTW_500_MS,
        /** 700ms */
        DTW_700_MS
    }
    /**
     * Configuration editor for the tap detection algorithm
     * @author Eric Tsai
     */
    interface TapConfigEditor extends ConfigEditorBase {
        /**
         * Enable double tap detection
         * @return Calling object
         */
        TapConfigEditor enableDoubleTap();
        /**
         * Enable single tap detection
         * @return Calling object
         */
        TapConfigEditor enableSingleTap();
        /**
         * Set the time that must pass before a second tap can occur
         * @param time    New quiet time
         * @return Calling object
         */
        TapConfigEditor quietTime(TapQuietTime time);
        /**
         * Set the time to lock the data in the status register
         * @param time    New shock time
         * @return Calling object
         */
        TapConfigEditor shockTime(TapShockTime time);
        /**
         * Set the length of time for a second shock to occur for a double tap
         * @param window    New double tap window
         * @return Calling object
         */
        TapConfigEditor doubleTapWindow(DoubleTapWindow window);
        /**
         * Set the threshold that the acceleration difference must exceed for a tap, in g's
         * @param threshold    New tap threshold
         * @return Calling object
         */
        TapConfigEditor threshold(float threshold);
    }
    /**
     * On-board algorithm that detects taps
     * @author Eric Tsai
     */
    interface TapDataProducer extends AsyncDataProducer, Configurable<TapConfigEditor> { }
    /**
     * Get an implementation of the TapDataProducer interface
     * @return TapDataProducer object
     */
    TapDataProducer tap();
}
