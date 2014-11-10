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
 * PROVIDED “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, 
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

import java.util.Arrays;
import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.controller.Logging.Trigger;
import com.mbientlab.metawear.api.util.LoggingTrigger;

/**
 * Controller for the data processor module
 * @author Eric Tsai
 */
public interface DataProcessor extends ModuleController {
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Enable the module */
        ENABLE {
            @Override public byte opcode() { return 0x1; }
        },
        /** Add or retrieve data about a data filter */
        FILTER_CREATE {
            @Override public byte opcode() { return 0x2; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    final byte[] data) {
                if ((data[1] & 0x80) == 0x80) {
                    final byte[] parameters= Arrays.copyOfRange(data, 7, data.length);
                    
                    final FilterConfig config= new FilterConfig() {
                        @Override
                        public byte[] bytes() {
                            return parameters;
                        }

                        @Override
                        public FilterType type() {
                            return FilterType.values()[data[6]];
                        }
                    };
                    
                    final byte[] triggerBytes= new byte[] {data[2], data[3], data[4], 
                            (byte) (data[5] & 0x1f), (byte) ((data[5] >> 5) & 0x7)}; 
                    Trigger temp= LoggingTrigger.lookupTrigger(triggerBytes);
                    final Trigger triggerObj= temp != null ? temp :
                        new Trigger() {
                            @Override
                            public com.mbientlab.metawear.api.Register register() {
                                return Module.lookupModule(triggerBytes[0]).lookupRegister(triggerBytes[1]);
                            }
    
                            @Override
                            public byte index() {
                                return triggerBytes[2];
                            }
    
                            @Override
                            public byte offset() {
                                return triggerBytes[3];
                            }
    
                            @Override
                            public byte length() {
                                return triggerBytes[4];
                            }
                        };
                    final Filter filterObj= new Filter() {

                        @Override
                        public Trigger trigger() {
                            return triggerObj;
                        }

                        @Override
                        public FilterConfig config() {
                            return config;
                        }
                        
                    };
                    
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedFilterObject(filterObj);
                    }
                } else {
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedFilterId(data[2]);
                    }
                }
            }
        },
        /** Receives notifications from filter outputs */
        FILTER_NOTIFICATION {
            @Override public byte opcode() { return 0x3; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte[] filterOutput= Arrays.copyOfRange(data, 3, data.length);
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedFilterOutput(data[2], filterOutput);
                }
            }
        },
        /** Modify or read the filter's internal state */
        FILTER_STATE {
            @Override public byte opcode() { return 0x4; }
        },
        /** Modify or read the filter's configuration */
        FILTER_CONFIGURATION {
            @Override public byte opcode() { return 0x5; }
        },
        /** Remove a filter */
        FILTER_REMOVE {
            @Override public byte opcode() { return 0x6; }
        },
        /** Enables output notification from a filter */
        FILTER_NOTIFY_ENABLE {
            @Override public byte opcode() { return 0x7; }
        };

        /* (non-Javadoc)
         * @see com.mbientlab.metawear.api.Register#module()
         */
        @Override public Module module() { return Module.DATA_PROCESSOR; }

        /* (non-Javadoc)
         * @see com.mbientlab.metawear.api.Register#notifyCallbacks(java.util.Collection, byte[])
         */
        @Override
        public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) { }
        
    }
    /**
     * Callbacks for the data processor module
     * @author Eric Tsai
     */
    public abstract class Callbacks implements ModuleCallbacks {
        @Override public final Module getModule() { return Module.DATA_PROCESSOR; }
        
        /**
         * Called when the filter user id is received 
         * @param filterId Id the user can use when referring to the desired filter
         */
        public void receivedFilterId(byte filterId) { }
        /**
         * Called when the filter attributes have been received from the board
         * @param filter Filter attributes wrapped in the Filter object
         */
        public void receivedFilterObject(Filter filter) { }
        /**
         * Called when output data is received from the filter
         * @param filterId User id corresponding to what filter the output is from 
         * @param output Filter output in byte form
         */
        public void receivedFilterOutput(byte filterId, byte[] output) { }        
    }
    
    /**
     * Wrapper object to encapsulate the configuration of a filter 
     * @author Eric Tsai
     */
    public interface FilterConfig {
        /** Byte array representing the configuration */
        public byte[] bytes();
        /** Filter type the configuration represents */
        public FilterType type();
    }
    /**
     * Wrapper object to encapsulate the attributes of a filter
     * @author Eric Tsai
     */
    public interface Filter {
        /** Trigger the filter is using for its input */
        public Trigger trigger();
        /** Configuration of the filter */
        public FilterConfig config();
    }
    
    /**
     * Enumeration of filters
     * @author Eric Tsai
     */
    public enum FilterType {
        /** Pass the data through, untouched */
        PASSTHROUGH,
        /** Accumulates received data as a running sum */
        ACCUMULATOR,
        /** Averages received data */
        LOW_PASS,
        NO_OP,
        NO_OP_2,
        /** Compares input data against a reference value */
        COMPARATOR,
        /** Performs root mean square over a set of data */
        ROOT_MEAN_SQUARE,
        /** Only allows data through during certain time intervals */
        TIME_DELAY,
        /** Perform simple math operations on the data */
        MATH,
        /** Only allow data through once a certain amount has been accumulated */
        SAMPLE_DELAY;
    }
    
    /**
     * Enable the data processor module
     */
    public void enableModule();
    /**
     * Disable the data processor module
     */
    public void disableModule();
    /**
     * Reads attributes of the filter id.  When the data is ready, the 
     * {@link Callbacks#receivedFilterObject(Filter)} callback function will be called
     * @param filterId ID of the filter to lookup
     */
    public void filterIdToObject(byte filterId);
    /**
     * Takes the output from the source filter as the input of filter being added.  
     * When the filter has been added the {@link Callbacks#receivedFilterId(byte)} 
     * callback function will be called with a user id representing the new filter
     * @param srcFilterId ID of the filter to use an the input
     * @param srcSize How many bytes the filter output is, between [1, 8] bytes
     * @param config Configuration of the filter consuming the data
     */
    public void chainFilters(byte srcFilterId, byte srcSize, FilterConfig config);
    /**
     * Adds a filter that operates on the trigger output.  Filter triggers can have up 
     * to a length of 8 bytes. When the filter has been added, the 
     * {@link Callbacks#receivedFilterId(byte)} callback function will be called 
     * with a user id representing the new filter
     * @param trigger Trigger to filter data on
     * @param config Configuration of the filter to add
     */
    public void addFilter(Trigger trigger, FilterConfig config);
    /**
     * Set the configuration of a filter
     * @param filterId ID of the filter to modify
     * @param config New configuration of the filter
     */
    public void setFilterConfiguration(byte filterId, FilterConfig config);
    /**
     * Resets the internal state of the filter
     * @param filterId ID of the filter to reset
     */
    public void resetFilterState(byte filterId);
    /**
     * Remove a filter
     * @param filterId User id of the filter
     */
    public void removeFilter(byte filterId);
    /**
     * Enable notifications on a filter output.  Data from the filter will be pass in through 
     * the {@link Callbacks#receivedFilterOutput(byte, byte[])} function
     * @param filterId User id of the filter
     */
    public void enableFilterNotify(byte filterId);
    /**
     * Disable notifications on a filter output
     * @param filterId User id of the filter
     */
    public void disableFilterNotify(byte filterId);
}
