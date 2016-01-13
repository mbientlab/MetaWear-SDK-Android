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
     * Selector for available data sources on the BMM150 chip
     * @author Eric Tsai
     */
    interface SourceSelector {
        /**
         * Handle data from the magnetic field measurements
         * @return Object representing B field data
         */
        DataSignal fromBField();
    }

    /**
     * Initiates the creation of a route for BMM150 sensor data
     * @return Selection of available data sources
     */
    SourceSelector routeData();

    /**
     * Sets the power mode to one of the preset configurations
     * @param preset    Power preset to use
     */
    void setPowerPrsest(PowerPreset preset);
    /**
     * Enables magnetic field sampling
     */
    void disableBFieldSampling();
    /**
     * Disables magnetic field sampling
     */
    void enableBFieldSampling();

    /**
     * Switch the magnetometer into normal mode
     */
    void start();
    /**
     * Switch the magnetometer into sleep mode
     */
    void stop();
}
