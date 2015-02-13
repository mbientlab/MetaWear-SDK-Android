/*
 * Copyright 2014 MbientLab Inc. All rights reserved.
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
package com.mbientlab.metawear.api.controller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

/**
 * Controller for the temperature module
 * @author Eric Tsai
 * @see com.mbientlab.metawear.api.Module#TEMPERATURE
 */
public interface Temperature extends ModuleController {
    /**
     * Enumeration of registers under the temperature module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Retrieves the current temperature from the sensor */
        TEMPERATURE {
            @Override public byte opcode() { return 0x1; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                float degrees= ByteBuffer.wrap(data, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() * 0.125f;
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).receivedTemperature(degrees);
                }
            }
        },
        /** Sampling configuration */
        MODE {
            @Override public byte opcode() { return 0x2; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                final ByteBuffer buffer= ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                
                SamplingConfig config= new SamplingConfig() {
                    @Override
                    public int period() {
                        return ((int) buffer.getShort(2)) & 0xffff;
                    }

                    @Override
                    public float deltaDetection() {
                        return (float) ((float) buffer.getShort(4) * 0.25);
                    }

                    @Override
                    public float lowerBound() {
                        return (float) ((float) buffer.getShort(6) * 0.25);
                    }

                    @Override
                    public float upperBound() {
                        return (float) ((float) buffer.getShort(8) * 0.25);
                    }
                };
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).receivedSamplingConfig(config);
                }
            }
        },
        /** Notification for temperature changes */
        DELTA_TEMP {
            @Override public byte opcode() { return 0x3; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                final ByteBuffer buffer= ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                float reference= (float) ((float) buffer.getShort(2) * 0.25), 
                        current= (float) ((float) buffer.getShort(4) * 0.25);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).temperatureDeltaExceeded(reference, current);
                }
            }
        },
        /** Notification for crossing temperature boundaries */
        THRESHOLD_DETECT {
            @Override public byte opcode() { return 0x4; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                final ByteBuffer buffer= ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                float threshold= (float) ((float) buffer.getShort(2) * 0.25), 
                        current= (float) ((float) buffer.getShort(4) * 0.25);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).boundaryCrossed(threshold, current);
                }
            }
        },
        THERMISTOR_MODE {
            @Override public byte opcode() { return 0x5;}
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) { }            
        };
        
        @Override public Module module() { return Module.TEMPERATURE; }
    }

    /**
     * Callbacks for temperature module
     * @author Eric Tsai
     */
    public abstract static class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return Module.TEMPERATURE; }

        /**
         * Called when MetaWear has responded with the temperature reading
         * @param degrees Value of the temperature in Celsius
         */
        public void receivedTemperature(float degrees) { }
        /**
         * Called when the sampling configuration has been received
         * @param config Temperature sampling configuration
         */
        public void receivedSamplingConfig(SamplingConfig config) { }
        /**
         * Called when the current temperature has exceeded a reference point by 
         * the delta specified in {@link SamplingConfigBuilder#withTemperatureDelta(float)}
         * @param reference Reference temperature, in Celsius, the temperate delta is counted from   
         * @param current Current temperature, in Celsius
         */
        public void temperatureDeltaExceeded(float reference, float current) { }
        /**
         * Called when either the lower or upper temperature boundaries have been crossed
         * @param boundary Boundary that was crossed, in Celsius
         * @param current Current temperature, in Celsius
         */
        public void boundaryCrossed(float boundary, float current) { }
    }
    
    /**
     * Wrapper class to encapsulate the temperature sampling configuration attributes
     * @author Eric Tsai
     */
    public interface SamplingConfig {
        /** Temperature sampling period, in ms */
        public int period();
        /** Change in temperature, in Celsius, that will trigger a callback */
        public float deltaDetection();
        /** Lower boundary for range detection */
        public float lowerBound();
        /** Upper boundary for range detection */
        public float upperBound();
    }
    /**
     * Builder for creating a configuration for temperature sampling
     * @author Eric Tsai
     */
    public interface SamplingConfigBuilder {
        /** 
         * Enables silent mode, which internally is notified of the events but will not 
         * trigger the callback functions
         * @return Calling object
         */
        public SamplingConfigBuilder withSilentMode();
        /**
         * Set the temperature sampling period
         * @param period How often to sample the temperature, in ms
         * @return Calling object
         */
        public SamplingConfigBuilder withSampingPeriod(int period);
        /**
         * Set the temperature delta.  A temperature change exceeding the delta will 
         * trigger a call to {@link Callbacks#temperatureDeltaExceeded(float, float)}
         * @param delta Change in temperature, in Celsius, to detect
         * @return Calling object
         */
        public SamplingConfigBuilder withTemperatureDelta(float delta);
        /**
         * Set the temperature boundaries.  If the temperate lays outside the boundary, 
         * trigger a call to {@link Callbacks#boundaryCrossed(float, float)}  
         * @param lower Lower boundary value, in Celsius
         * @param upper Upper boundary value, in Celsius
         * @return Calling object
         */
        public SamplingConfigBuilder withTemperatureBoundary(float lower, float upper);
        /**
         * Write the configuration settings to the MetaWear board and starts the sampling
         */
        public void commit();
    }
    
    /**
     * Read the temperature reported from MetaWear.
     * When data is ready, the {@link Callbacks#receivedTemperature(float)} function 
     * will be called
     */
    public void readTemperature();
    /**
     * Enables temperature sampling and event detection
     * @return Builder to configure the sampling parameters
     */
    public SamplingConfigBuilder enableSampling();
    /**
     * Disables temperature sampling
     */
    public void disableSampling();
    
    /**
     * Puts the board in thermistor mode, which uses the thermistor to measure the temperature 
     * rather than the onboard temperature chip
     * @param analogReadPin GPIO pin the thermistor is attached to
     * @param pulldownPin GPIO pin that the pulldown resistor is attached to
     */
    public void enableThermistorMode(byte analogReadPin, byte pulldownPin);
    /**
     * Disables thermistor mode
     */
    public void disableThermistorMode();
}
