package com.mbientlab.metawear.module;

import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.MetaWearBoard;

/**
 * Interacts with the BMM150 magnetometer on the MetaWear C Pro boards
 * @author Eric Tsai
 */
public interface Bmm150Magnetometer extends MetaWearBoard.Module {
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
    enum PowerPreset {
        LOW_POWER,
        REGULAR,
        ENHANCED_REGULAR,
        HIGH_ACCURACY
    }

    /**
     * Threshold detection types supported on the BMM150 magnetometer
     * @author Eric Tsai
     */
    enum ThresholdDetectionType {
        /** Detect when the x-axis B field drops below the threshold */
        LOW_X,
        /** Detect when the y-axis B field drops below the threshold */
        LOW_Y,
        /** Detect when the z-axis B field drops below the threshold */
        LOW_Z,
        /** Detect when the x-axis B field exceeds the threshold */
        HIGH_X,
        /** Detect when the y-axis B field exceeds the threshold */
        HIGH_Y,
        /** Detect when the z-axis B field exceeds the threshold */
        HIGH_Z
    }

    /**
     * Interface for configuring threshold detection
     * @author Eric Tsai
     */
    interface ThresholdDetectionConfigEditor {
        /**
         * Sets the level for low threshold detection
         * @param threshold    Low threshold level, between [0, 1530&#956;T]
         * @return Calling object
         */
        ThresholdDetectionConfigEditor setLowThreshold(float threshold);
        /**
         * Sets the level for high threshold detection
         * @param threshold    High threshold level, between [0, 1530&#956;T]
         * @return Calling object
         */
        ThresholdDetectionConfigEditor setHighThreshold(float threshold);
        /**
         * Write the changes to the board
         */
        void commit();
    }

    /**
     * Wrapper class encapsulating data from a threshold interrupt
     * @author Eric Tsai
     */
    interface ThresholdInterrupt {
        /**
         * Checks if the specific threshold detection type triggered the interrupt
         * @param type    Detection type to lookup
         * @return True if the type triggered the interrupt
         */
        boolean crossed(ThresholdDetectionType type);
    }

    /**
     * Selector for available data sources on the BMM150 chip
     * @author Eric Tsai
     */
    interface SourceSelector {
        /**
         * Handle data from the magnetic field measurements
         * @return Object representing B field data
         */
        DataSignal fromBField();
        /**
         * Special signal for high frequency (>100Hz) magnetic field stream.  This signal is only for streaming,
         * it does not support logging or data processing.
         * @return Object representing a high frequency acceleration stream
         */
        DataSignal fromHighFreqBField();
        /**
         * Handles data from the threshold detection interrupt
         * @return Object representing threshold detection data
         */
        DataSignal fromThreshold();
    }

    /**
     * Initiates the creation of a route for BMM150 sensor data
     * @return Selection of available data sources
     */
    SourceSelector routeData();

    /**
     * Sets the power mode to one of the preset configurations
     * @param preset    Power preset to use
     * @deprecated As of v2.6.0, replaced with the correctly spelled {@link #setPowerPreset(PowerPreset)} function
     */
    @Deprecated
    void setPowerPrsest(PowerPreset preset);
    /**
     * Sets the power mode to one of the preset configurations
     * @param preset    Power preset to use
     */
    void setPowerPreset(PowerPreset preset);
    /**
     * Enables magnetic field sampling
     */
    void disableBFieldSampling();
    /**
     * Disables magnetic field sampling
     */
    void enableBFieldSampling();

    /**
     * Configure the threshold detection
     * @return Editor object to configure the settings
     */
    ThresholdDetectionConfigEditor configureThresholdDetection();
    /**
     * Enables threshold detection
     * @param types    Threshold detection types to enable
     */
    void enableThresholdDetection(ThresholdDetectionType ... types);
    /**
     * Disables threshold detection
     */
    void disableThresholdDetection();

    /**
     * Switch the magnetometer into normal mode
     */
    void start();
    /**
     * Switch the magnetometer into sleep mode
     */
    void stop();
}
