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
package com.mbientlab.metawear.api.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.mbientlab.metawear.api.controller.DataProcessor.FilterConfig;
import com.mbientlab.metawear.api.controller.DataProcessor.FilterType;

/**
 * Builder for constructing the configuration of various data filters
 * @author Eric Tsai
 */
public abstract class FilterConfigBuilder {
    protected final byte[] parameters;
    protected final FilterType type;
    
    /**
     * Constructs a builder for configuring a data filter
     * @param size Number of bytes the filter configuration needs
     * @param type Type of filter the byte array is for
     */
    protected FilterConfigBuilder(int size, FilterType type) {
        this.parameters= new byte[size];
        this.type= type;
    }
    
    /**
     * Create a filter configuration object 
     * @return Filter configuration wrapped in an object
     */
    public FilterConfig build() {
        return new FilterConfig() {

            @Override
            public byte[] bytes() {
                return parameters;
            }

            @Override
            public FilterType type() {
                return type;
            }
            
        };
    }
    
    /**
     * Builder for filters that require explicit input and output size
     * @author Eric Tsai     
     */
    public static abstract class IOConfigBuilder extends FilterConfigBuilder {
        protected IOConfigBuilder(int size, FilterType type) {
            super(size, type);
        }
        
        /**
         * Size of the output
         * @param size Number of bytes the output will be, between [1, 4] bytes
         * @return Calling object
         */
        public IOConfigBuilder withOutputSize(byte size) {
            parameters[0]|= (size - 1);
            return this;
        }
        /**
         * Size of the input data
         * @param size Number of bytes the inputs are, between [1, 4] bytes
         * @return Calling object
         */
        public IOConfigBuilder withInputSize(byte size) {
            parameters[0]|= ((size - 1) << 2);
            return this;
        }
    }
    
    /**
     * Builder to configure the pass through filter
     * @author Eric Tsai
     * @see com.mbientlab.metawear.api.controller.DataProcessor.FilterType#PASSTHROUGH
     */
    public static class PassthroughBuilder extends FilterConfigBuilder {
        /**
         * Criteria that determines whether data is allowed to pass
         * @author etsai
         */
        public enum Mode {
            /** All data is passed through */
            ALL,
            /** Only pass data through if the reference is a non-zero value */
            CONDITIONAL,
            /** Only pass the nth entry through, as set by the reference value */
            COUNT;
        }
        public PassthroughBuilder() {
            super(3, FilterType.PASSTHROUGH);
        }
        
        /**
         * Sets what kind of pass through mode the filter should use
         * @param mode Pass through mode to use 
         * @return Calling object
         */
        public PassthroughBuilder withMode(Mode mode) {
            parameters[0]= (byte) mode.ordinal();
            return this;
        }
        /**
         * Sets the reference to determine if the pass through mode should let data through 
         * @param reference Value for the pass through modes
         * @return Calling object
         */
        public PassthroughBuilder withReference(short reference) {
            parameters[1]= (byte)(reference & 0xff);
            parameters[2]= (byte)((reference >> 8) & 0xff);
            return this;
        }
    }
    
    /**
     * Builder to configure the accumulator builder
     * @author Eric Tsai
     * @see com.mbientlab.metawear.api.controller.DataProcessor.FilterType#ACCUMULATOR 
     */
    public static class AccumulatorBuilder extends IOConfigBuilder {
        public AccumulatorBuilder() {
            super(1, FilterType.ACCUMULATOR);
        }
    }
    
    /**
     * Builder to configure the low pass filter
     * @author Eric Tsai
     * @see com.mbientlab.metawear.api.controller.DataProcessor.FilterType#LOW_PASS
     */
    public static class LowPassBuilder extends IOConfigBuilder {
        public LowPassBuilder() {
            super(2, FilterType.LOW_PASS);
        }
        
        /**
         * Sets how big the sample size should be for averaging
         * @param nSamples Number of data samples to average
         * @return Calling object
         */
        public LowPassBuilder withSampleSize(byte nSamples) {
            parameters[1]= nSamples;
            return this;
        }
    }
    /**
     * Builder to configure the peak detector filter
     * @author Eric Tsai
     */
    public static class PeakDetectorBuilder extends FilterConfigBuilder {
        public PeakDetectorBuilder() {
            super(3, FilterType.PEAK_DETECTOR);
        }
        
        public PeakDetectorBuilder withLookAhead(byte count) {
            parameters[0]= count;
            return this;
        }
        
        public PeakDetectorBuilder withDelta(short delta) {
            parameters[1]= (byte)(delta & 0xff);
            parameters[2]= (byte)((delta >> 8) & 0xff);
            return this;
        }
    }
    
    /**
     * Builder to configure the comparator filter
     * @author Eric Tsai
     * @see com.mbientlab.metawear.api.controller.DataProcessor.FilterType#COMPARATOR     
     */
    public static class ComparatorBuilder extends FilterConfigBuilder {
        /**
         * Enumeration of comparison operators
         * @author Eric Tsai
         */
        public enum Operation {
            /** Equal */
            EQ,
            /** Not equal */
            NEQ,
            /** Less than */
            LT,
            /** Less than or equal to */
            LTE,
            /** Greater than */
            GT,
            /** Greater than or equal to */
            GTE,
        }
        
        public ComparatorBuilder() {
            super(7, FilterType.COMPARATOR);
        }
        /**
         * Interprets the bytes as signed numbers
         * @return Calling object
         */
        public ComparatorBuilder withSignedCommparison() {
            parameters[0]= 1;
            return this;
        }
        /**
         * Sets the comparison operation
         * @param op Comparison operation to perform
         * @return Calling object
         */
        public ComparatorBuilder withOperation(Operation op) {
            parameters[1]= (byte) op.ordinal();
            return this;
        }
        /**
         * Sets the reference value to compare against
         * @param reference Values to compare against 
         * @return Calling object
         */
        public ComparatorBuilder withReference(int reference) {
            byte[] buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(reference).array();
            
            for(int i= 0; i < buffer.length; i++) {
                parameters[i + 3]= buffer[i];
            }
            return this;
        }
    }

    /**
     * Builder to configure the RMS filter
     * @author Eric Tsai
     * @see com.mbientlab.metawear.api.controller.DataProcessor.FilterType#ROOT_MEAN_SQUARE
     */
    public static class RMSBuilder extends IOConfigBuilder {
        public RMSBuilder() {
            super(2, FilterType.ROOT_MEAN_SQUARE);
        }
        /**
         * Operation modes the RMS filter can be in
         * @author Eric Tsai
         */
        public enum Mode {
            /** Perform root mean square calculation */
            RMS,
            /** Perform root sum square calculation */
            RSS;
        }
        /**
         * Sets the operation mode
         * @param mode Mode to use
         * @return Calling object
         */
        public RMSBuilder withMode(Mode mode) {
            parameters[1]= (byte) (mode.ordinal() & 0xf);
            return this;
        }
        /**
         * Sets the number of inputs used in the RMS calculation
         * @param nInputs Number of inputs, between [1, 8]
         * @return Calling object
         */
        public RMSBuilder withInputCount(byte nInputs) {
            parameters[0]|= (nInputs - 1) << 4;
            return this;
        }
        
        /**
         * Interprets the bytes as a signed value
         * @return Calling object
         */
        public RMSBuilder withSignedInput() {
            parameters[0]|= 0x80;
            return this;
        }
    }
    
    /**
     * Builder to configure the time delay filter 
     * @author Eric Tsai
     * @see com.mbientlab.metawear.api.controller.DataProcessor.FilterType#TIME_DELAY
     */
    public static class TimeDelayBuilder extends FilterConfigBuilder {
        /**
         * Filter modes for the time delay filter
         * @author Eric Tsai
         */
        public enum FilterMode {
            /** No change in the input value */
            ABSOLUTE,
            /** Return the difference between the current and previous value */
            DIFFERENTIAL;
        }
        
        public TimeDelayBuilder() {
            super(5, FilterType.TIME_DELAY);
        }
        
        /**
         * Sets how many bytes the input data is
         * @param size Number of bytes, between [1, 8]
         * @return Calling object
         */
        public TimeDelayBuilder withDataSize(byte size) {
            parameters[0]|= (byte) ((size - 1) & 0x7);
            return this;
        }
        /**
         * Set the filter mode for transforming the input
         * @param mode Filter mode to use
         * @return Calling object
         */
        public TimeDelayBuilder withFilterMode(FilterMode mode) {
            parameters[0]|= (byte)((mode.ordinal() & 0x7) << 3);
            return this;
        }
        /**
         * Sets the period of when data is allowed to pass
         * @param period Amount of time, in ms 
         * @return Calling object
         */
        public TimeDelayBuilder withPeriod(int period) {
            byte[] buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(period).array();
            
            System.arraycopy(buffer, 0, parameters, 1, 4);
            return this;
        }
    }
    
    /**
     * Builder to configure the math filter
     * @author Eric Tsai
     * @see com.mbientlab.metawear.api.controller.DataProcessor.FilterType#MATH
     */
    public static class MathBuilder extends IOConfigBuilder {
        /**
         * Math operations to perform on the data.  With the exception of {@link MathBuilder.Operation#SQRT}, 
         * the right hand value of the operation is specified with the 
         * {@link MathBuilder#withOperand(int)} function
         * @author Eric Tsai
         */
        public enum Operation {
            /** No operation to perform on the data */
            NO_OP,
            /** Add the data */
            ADD,
            /** Multiply the data */
            MULTIPLY,
            /** Divide the data */
            DIVIDE,
            /** Calculate the remainder */
            MODULUS,
            /** Exponentiate the data */
            EXPONENT,
            /** Calculate square root */
            SQRT,
            /** Perform left shift */
            LEFT_SHIFT,
            /** Perform right shift */
            RIGHT_SHIFT;
        }
        public MathBuilder() {
            super(6, FilterType.MATH);
        }
        
        /**
         * Set if input should be interpreted as signed data
         * @return Calling object
         */
        public MathBuilder withInputAsSigned() {
            parameters[0]|= 0x10;
            return this;
        }
        /**
         * Set the operation to perform on the data 
         * @param op Desired operation
         * @return Calling object
         */
        public MathBuilder withOperation(Operation op) {
            parameters[1]= (byte) op.ordinal();
            return this;
        }
        /**
         * Set the right hand side value for 2 input operations
         * @param rhs Value on the right side of the operation
         * @return Calling object
         */
        public MathBuilder withOperand(int rhs) {
            byte[] buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(rhs).array();
            
            for(int i= 0; i < buffer.length; i++) {
                parameters[i + 2]= buffer[i];
            }
            return this;
        }
    }
    
    /**
     * Builder to configure the sample delay filter
     * @author Eric Tsai
     * @see com.mbientlab.metawear.api.controller.DataProcessor.FilterType#SAMPLE_DELAY
     */
    public static class SampleDelayBuilder extends FilterConfigBuilder {
        public SampleDelayBuilder() {
            super(2, FilterType.SAMPLE_DELAY);
        }
        /**
         * Sets how many bytes the input data is
         * @param size Number of bytes, between [1, 4]
         * @return Calling object
         */
        public SampleDelayBuilder withDataSize(byte size) {
            parameters[0]= (byte) (size - 1);
            return this;
        }
        /**
         * Set the number of samples to collect before allowing the data through
         * @param size How many data samples to collect
         * @return Calling object
         */
        public SampleDelayBuilder withCollectionSize(byte size) {
            parameters[1]= size;
            return this;
        }
    }
    
    /**
     * Builder to configure the pulse detector filter
     * @author Eric Tsai
     * @see com.mbientlab.metawear.api.controller.DataProcessor.FilterType#PULSE_DETECTOR
     */
    public static class PulseDetectorBuilder extends FilterConfigBuilder {
        /**
         * Types of output that the filter will return
         * @author Eric Tsai
         */
        public enum OutputMode {
            /** Return the width of the pulse */
            PULSE_WIDTH,
            /** Return the area of the pulse */
            PULSE_AREA,
            /** Return the peak value of the pulse */
            PULSE_PEAK;
        }
        
        public PulseDetectorBuilder() {
            super(9, FilterType.PULSE_DETECTOR);
        }
        
        /**
         * How many bytes each data is
         * @param size Between 1 and 4 bytes
         * @return Calling object
         */
        public PulseDetectorBuilder withDataSize(byte size) {
            parameters[0]= (byte) (size - 1);
            return this;
        }
        /**
         * Sets the output mode of the filter 
         * @param mode  Output mode to use
         * @return Calling object
         */
        public PulseDetectorBuilder withOutputMode(OutputMode mode) {
            parameters[2]= (byte) mode.ordinal();
            return this;
        }
        /**
         * Sets the threshold of the filter.  The threshold is a value that the data points must 
         * cross twice: once to go above it followed by going below it.
         * @param threshold Value the data points must cross twice for a valid pulse 
         * @return Calling object
         */
        public PulseDetectorBuilder withThreshold(int threshold) {
            byte[] buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(threshold).array();
            System.arraycopy(buffer, 0, parameters, 3, buffer.length);
            
            return this;
        }
        /**
         * Number of data points to use in checking for a pulse
         * @param width Number of data points
         * @return Calling object
         */
        public PulseDetectorBuilder withWidth(short width) {
            parameters[7]= (byte)(width & 0xff);
            parameters[8]= (byte)((width >> 8) & 0xff);
            return this;
        }
    }
}
