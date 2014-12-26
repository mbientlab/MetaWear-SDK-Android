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
import java.util.Arrays;
import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

import static com.mbientlab.metawear.api.Module.ACCELEROMETER;


/**
 * Controller for the accelerometer module
 * @author Eric Tsai
 * @see com.mbientlab.metawear.api.Module#ACCELEROMETER
 */
public interface Accelerometer extends ModuleController {
    /**
     * Enumeration of registers for the accelerometer module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Checks module enable status and enables/disables module notifications */
        GLOBAL_ENABLE {
            @Override public byte opcode() { return 0x1; }
        },
        /** Checks motion polling status and enables/disables motion polling */
        DATA_ENABLE {
            @Override public byte opcode() { return 0x2; }
        },
        /** Sets or retrieves motion polling configuration */
        DATA_SETTINGS {
            @Override public byte opcode() { return 0x3; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte[] config= Arrays.copyOfRange(data, 2, data.length);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedConfiguration(Component.DATA, config);
                }
            }
        },
        /** Stores XYZ motion data. */
        DATA_VALUE {
            @Override public byte opcode() {return 0x4; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                short x= (short)(ByteBuffer.wrap(data, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort()), 
                        y= (short)(ByteBuffer.wrap(data, 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort()), 
                        z= (short)(ByteBuffer.wrap(data, 6, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).receivedDataValue(x, y, z);
                }
            }
        },
        /** Checks and sets movement detection status */
        FREE_FALL_ENABLE {
            @Override public byte opcode() { return 0x5; }
        },
        /** Sets or retrieves movement detection configuration */
        FREE_FALL_SETTINGS {
            @Override public byte opcode() { return 0x6; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte[] config= Arrays.copyOfRange(data, 2, data.length);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedConfiguration(Component.FREE_FALL, config);
                }
            }
        },
        /** Stores movement state, enables/disables movement detection */
        FREE_FALL_VALUE { 
            @Override public byte opcode() { return 0x7; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    final byte[] data) {
                if ((data[2] & 0x80) == 0x80) {
                    MovementData ffInfo= new MovementData() {
                        @Override
                        public boolean isAboveThreshold(Axis axis) {
                            byte mask= (byte) (2 << (2 * axis.ordinal()));
                            return (data[2] & mask) == mask;
                        }

                        @Override
                        public Direction getDirection(Axis axis) {
                            byte mask= (byte) (1 << (2 * axis.ordinal()));
                            return (data[2] & mask) == mask ? Direction.NEGATIVE : Direction.POSITIVE;
                        }
                    };
                    
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).movementDetected(ffInfo);
                        ((Callbacks) it).inFreeFall();
                    }
                }
            }
        },
        /** Sets or retrieves orientation detection status */
        ORIENTATION_ENABLE {
            @Override public byte opcode() { return 0x8; }
        },
        /** Sets or retrieves the configuration for orientation notifications */
        ORIENTATION_SETTING {
            @Override public byte opcode() { return 0x9; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte[] config= Arrays.copyOfRange(data, 2, data.length);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedConfiguration(Component.ORIENTATION, config);
                }
            }
        },
        /** Stores current orientation, and enables/disables orientation notifications*/
        ORIENTATION_VALUE {
            @Override public byte opcode() { return 0xa; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte enumOffset= (byte) (4 * (data[2] & 0x1) + ((data[2] >> 1) & 0x3));
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).receivedOrientation(data[2]);
                    ((Callbacks)it).orientationChanged(Orientation.values()[enumOffset]);
                }
            }
        },
        /** Sets or retrieves tap detection status */
        PULSE_ENABLE {
            @Override public byte opcode() { return 0xb; }
        },
        /** Sets or retrieves the configuration for tap detection */
        PULSE_SETTING {
            @Override public byte opcode() { return 0xc; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte[] config= Arrays.copyOfRange(data, 2, data.length);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedConfiguration(Component.PULSE, config);
                }
            }
        },
        /** Stores current tap information and enables/disables tap notifications */
        PULSE_STATUS {
            @Override public byte opcode() { return 0xd; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    final byte[] data) {
                if ((data[2] & 0x80) == 0x80) {
                    MovementData moveData= new MovementData() {
                        @Override
                        public boolean isAboveThreshold(Axis axis) {
                            byte mask= (byte) (0x10 << axis.ordinal());
                            return (data[2] & mask) == mask;
                        }

                        @Override
                        public Direction getDirection(Axis axis) {
                            return Direction.values()[(data[2] >> axis.ordinal()) & 0x1];
                        }
                    };
                    
                    for(ModuleCallbacks it: callbacks) {
                        if ((data[2] & 0x8) == 0x8) ((Callbacks) it).doubleTapDetected(moveData);
                        else ((Callbacks) it).singleTapDetected(moveData);
                    }
                }
            }
        },
        /** Sets or retrieves shake detection status */
        TRANSIENT_ENABLE {
            @Override public byte opcode() { return 0xe; }
        },
        /** Sets or retrieves the configuration for shake detection */
        TRANSIENT_SETTING {
            @Override public byte opcode() { return 0xf; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                byte[] config= Arrays.copyOfRange(data, 2, data.length);
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedConfiguration(Component.TRANSIENT, config);
                }
            }
        },
        /** Stores current shake information and enables/disables shake notifications */
        TRANSIENT_STATUS {
            @Override public byte opcode() { return 0x10; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    final byte[] data) {
                if ((data[2] & 0x40) == 0x40) {
                    MovementData moveData= new MovementData() {
                        @Override
                        public boolean isAboveThreshold(Axis axis) {
                            byte mask= (byte) (0x2 << (2 * axis.ordinal()));
                            return (data[2] & mask) == mask;
                        }

                        @Override
                        public Direction getDirection(Axis axis) {
                            return Direction.values()[(data[2] >> (2 * axis.ordinal())) & 0x1];
                        }
                        
                    };
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).shakeDetected(moveData);
                    }
                }
            }
        };
        
        @Override public Module module() { return ACCELEROMETER; }
        @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) { }
    }
    /**
     * Callbacks for the accelerometer module
     * @author Eric Tsai
     */
    public abstract static class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return ACCELEROMETER; }

        /**
         * Called when the configuration of an accelerometer component has been received
         * @param component Component the configuration is describing
         * @param configuration Byte representation of the configuration
         */
        public void receivedConfiguration(Component component, byte[] configuration) { }
        
        /**
         * Called when the ble radio has received accelerometer motion data.  In firmware v0.9.0, 
         * the accelerometer data will already be appropriately converted to milli Gs so there is 
         * need to call {@link com.mbientlab.metawear.api.util.BytesInterpreter#bytesToGs(byte[], short)}
         * @param x X component of acceleration, in milli Gs
         * @param y Y component of acceleration, in milli Gs
         * @param z Z component of acceleration, in milli Gs
         */
        public void receivedDataValue(short x, short y, short z) { }
        
        /**
         * Called when movement is detected.  This function will be repeatedly called 
         * while movement is detected.
         * @param moveData Movement data encapsulated in an object
         */
        public void movementDetected(MovementData moveData) { }
        
        /**
         * Called when free fall is detected
         * @deprecated As of v1.1, replaced by {@link Callbacks#movementDetected(Accelerometer.MovementData)}
         */
        @Deprecated
        public void inFreeFall() { }
        /**
         * Called when free fall has stopped
         * @deprecated As of v1.1, callback function was never properly implemented and 
         * has been replaced by {@link Callbacks#movementDetected(Accelerometer.MovementData)}
         */
        public void stoppedFreeFall() { }
        
        /**
         * Called when the orientation has changed
         * @param orientation Orientation information from the accelerometer's status register
         * @deprecated As of v1.1, replaced by 
         * {@link Callbacks#orientationChanged(Accelerometer.Orientation)} 
         */
        @Deprecated
        public void receivedOrientation(byte orientation) { }
        
        /**
         * Called when the orientation has changed
         * @param accelOrientation Orientation of the accelerometer
         */
        public void orientationChanged(Orientation accelOrientation) { }
        
        /**
         * Called when a single tap has been detected
         * @param moveData Movement data encapsulated in an object
         */
        public void singleTapDetected(MovementData moveData) { }
        /**
         * Called when a double tap has been detected
         * @param moveData Movement data encapsulated in an object
         */
        public void doubleTapDetected(MovementData moveData) { }
        
        /**
         * Called when a shake motion is detected.  This function will be continuously called 
         * as long as the set threshold is exceeded in the desired direction
         * @param moveData Movement data encapsulated in an object
         */
        public void shakeDetected(MovementData moveData) { }
    }

    /**
     * Enumeration of components in the accelerometer
     * @author Eric Tsai
     */
    public enum Component {
        /** XYZ data sampling */
        DATA(Register.DATA_ENABLE, Register.DATA_SETTINGS, Register.DATA_VALUE),
        /** Free fall or motion detection */
        FREE_FALL(Register.FREE_FALL_ENABLE, Register.FREE_FALL_SETTINGS, Register.FREE_FALL_VALUE),
        /** Orientation detection */
        ORIENTATION(Register.ORIENTATION_ENABLE, Register.ORIENTATION_SETTING, Register.ORIENTATION_VALUE),
        /** Tap detection */
        PULSE(Register.PULSE_ENABLE, Register.PULSE_SETTING, Register.PULSE_STATUS),
        /** Shake detection */
        TRANSIENT(Register.TRANSIENT_ENABLE, Register.TRANSIENT_SETTING, Register.TRANSIENT_STATUS);
        
        public final Register enable, config, status;

        /**
         * @param enable
         * @param config
         */
        private Component(Register enable, Register config, Register status) {
            this.enable= enable;
            this.config= config;
            this.status= status;
        }
        
    }
    
    /**
     * Orientation definitions for the accelerometer.  The entries are defined 
     * from the perspective of the accelerometer chip's placement and orientation, 
     * not from the MetaWear board's perspective.
     * @author Eric Tsai
     */
    public enum Orientation {
        FRONT_PORTRAIT_UP,
        FRONT_PORTRAIT_DOWN,
        FRONT_LANDSCAPE_RIGHT {
            @Override public boolean isFront() { return true; }
            @Override public boolean isPortrait() { return false; }
        },
        FRONT_LANDSCAPE_LEFT {
            @Override public boolean isFront() { return true; }
            @Override public boolean isPortrait() { return false; }
        },
        BACK_PORTRAIT_UP {
            @Override public boolean isFront() { return false; }
            @Override public boolean isPortrait() { return true; }
        },
        BACK_PORTRAIT_DOWN {
            @Override public boolean isFront() { return false; }
            @Override public boolean isPortrait() { return true; }
        },
        BACK_LANDSCAPE_RIGHT {
            @Override public boolean isFront() { return false; }
            @Override public boolean isPortrait() { return false; }
        },
        BACK_LANDSCAPE_LEFT {
            @Override public boolean isFront() { return false; }
            @Override public boolean isPortrait() { return false; }
        };
        
        public boolean isFront() { return true; }
        public boolean isPortrait() { return true; }
        
    }
    
    /**
     * Detectable tap types 
     * @author Eric Tsai
     */
    public enum TapType {
        SINGLE_TAP,
        DOUBLE_TAP;
    }
    /**
     * Axes available for motion detection.  These axis entries are relative to the 
     * orientation of the accelerometer chip.
     * @author etsai
     */
    public enum Axis {
        X,
        Y,
        Z;
    }
    
    /**
     * Wrapper class encapsulating movement information received from the board
     * @author Eric Tsai     
     */
    public interface MovementData {
        /**
         * Axis information for the detection callback functions
         * @author Eric Tsai
         */
        public enum Direction {
            /** Movement is in the positive direction */
            POSITIVE,
            /** Movement is in the negative direction */
            NEGATIVE,
        }
        
        /**
         * Returns whether or not the board exceeded the threshold on the specific axis
         * @param axis Axis to check
         * @return True if board's axis exceeded threshold, false otherwise
         */
        public boolean isAboveThreshold(Axis axis);
        /**
         * Returns the direction the board is moving in on the specific axis
         * @param axis Axis to check
         * @return Direction enum value indicating the movement direction
         */
        public Direction getDirection(Axis axis);
    }
    
    /**
     * Disable detection for the accelerometer component.  If saveConfig is false, 
     * you will need to reconfigure the detection parameters via the appropriate 
     * enable detection function.
     * @param component Component to disable
     * @param saveConfig True if the component configuration should be saved
     */
    public void disableDetection(Component component, boolean saveConfig);
    /**
     * Disable detection for all components.  If saveConfig is false, you will need 
     * to reconfigure the parameters for each component via the appropriate enable 
     * detection function.
     * @param saveConfig True if the all configurations should be saved
     */
    public void disableAllDetection(boolean saveConfig);
    /**
     * Enable tap detection.  When a tap is detected, one of the tap detected 
     * callback functions is called depending on what kind of tap was being detected
     * @param type Tap type to detected
     * @param axis Which axis to detect taps on
     * @return Configuration object to tweak the tap settings
     * @see Callbacks#singleTapDetected(Accelerometer.MovementData)
     * @see Callbacks#doubleTapDetected(Accelerometer.MovementData)
     */
    public ThresholdConfig enableTapDetection(TapType type, Axis axis);
    /**
     * Enable shake detection.  When a shake motion is detected along the given axis, 
     * the {@link Callbacks#shakeDetected(Accelerometer.MovementData)} callback 
     * function is called
     * @param axis Which axis to detect shake motion
     * @return Configuration object to tweak the shake settings
     */
    public ThresholdConfig enableShakeDetection(Axis axis);
    /**
     * Enable orientation detection.  When an orientation change is detected, the 
     * {@link Callbacks#orientationChanged(Accelerometer.Orientation)} callback function 
     * is called.
     * @return Configuration object to tweak the orientation settings
     */
    public AccelerometerConfig enableOrientationDetection();
    /**
     * Enable free fall detection.  This function is mutually exclusive with 
     * {@link #enableMotionDetection(Accelerometer.Axis...)}, you can only enable one or the
     * other.  When free fall is detected, the {@link Callbacks#movementDetected(Accelerometer.MovementData)} 
     * callback function is called
     * @return Configuration object to tweak free fall settings
     */
    public ThresholdConfig enableFreeFallDetection();
    /**
     * Enable motion detection.  This function is mutually exclusive with 
     * {@link #enableFreeFallDetection()}, you can only enable one or the
     * other.  When motion is detected, the {@link Callbacks#movementDetected(Accelerometer.MovementData)} 
     * callback function is called
     * @param axes Axis to detect motion on
     * @return Configuration object to tweak motion settings
     */
    public ThresholdConfig enableMotionDetection(Axis ... axes);
    /**
     * Enable data sampling of the XYZ axes.  When axis data is received, the 
     * {@link Callbacks#receivedDataValue(short, short, short)} callback function is called
     * @return Configuration object to tweak the data sampling settings
     */
    public SamplingConfig enableXYZSampling();
    /**
     * Starts detection / sampling of enabled components
     */
    public void startComponents();
    /**
     * Stops activity for enabled components
     */
    public void stopComponents();
    /**
     * Resets configuration and stops detection for all components
     */
    public void resetAll();
    
    /**
     * Provides optional configuration options to allow users to customize the 
     * accelerometer detection
     * @author Eric Tsai
     */
    public interface AccelerometerConfig {
        /**
         * The accelerometer will internally detect the desired event however, callback functions 
         * will not be called.   
         * @return Calling object
         */
        public AccelerometerConfig withSilentMode();
        /**
         * Get the byte representation of the configuration
         * @return Configuration as an array of bytes 
         */
        public byte[] getBytes();
    }
    /**
     * Configure threshold level for accelerometer components
     * @author Eric Tsai
     */
    public interface ThresholdConfig extends AccelerometerConfig {
        /**
         * Sets the detection threshold
         * @param gravity Threshold of the sensor, in Gs
         * @return Calling object
         */
        public ThresholdConfig withThreshold(float gravity);
    }
    
    /**
     * Configure attributes for data sampling
     * @author Eric Tsai
     */
    public interface SamplingConfig extends AccelerometerConfig {
        /**
         * Max range of the accelerometer data
         * @author Eric Tsai
         */
        public enum FullScaleRange {
            FSR_2G,
            FSR_4G,
            FSR_8G;
        }
        /**
         * Available data rates on the metawear accelerometer
         * @author Eric Tsai
         */
        public enum OutputDataRate {
            ODR_800_HZ,
            ODR_400_HZ,
            ODR_200_HZ,
            ODR_100_HZ,
            ODR_50_HZ,
            ODR_12_5_HZ,
            ODR_6_25_HZ,
            ODR_1_56_HZ;
        }
        
        /**
         * Sets the max range of the data
         * @param range Data's max range
         * @return Calling object
         */
        public SamplingConfig withFullScaleRange(FullScaleRange range);
        /**
         * Sets the sampling data rate
         * @param rate Data rate to sample at
         * @return Calling object
         */
        public SamplingConfig withOutputDataRate(OutputDataRate rate);
        /**
         * Enables high-pass filtering on the accelerometer axis data.  This version of the 
         * function does not alter the cutoff setting
         * @return Calling object
         */
        public SamplingConfig withHighPassFilter();
        /**
         * Enables high-pass filtering on the accelerometer axis data
         * @param cutoff Cutoff frequency setting between [0, 3] where 
         * 0 = highest cutoff freq and 3 = lowest cutoff freq
         * @return Calling object
         */
        public SamplingConfig withHighPassFilter(byte cutoff);
        /**
         * Disables high-pass filtering on the accelerometer axis data
         * @return Calling object
         */
        public SamplingConfig withoutHighPassFilter();
    }
    
    /**
     * Enables accelerometer activity for the given component
     * @param component Component to enable
     * @param notify True if the API should be notified of accelerometer events.  
     * If set to false, the assoicated callback function will not be called.
     */
    public void enableComponent(Component component, boolean notify);
    /**
     * Disable accelerometer activity for the given component.  Currently, this 
     * function is an alias for {@link #disableNotification(Accelerometer.Component)}
     * @param component Component to disable
     */
    public void disableComponent(Component component);
    
    /**
     * Enable notifications from a component
     * @param component Component to enable notifications from
     * @deprecated As of v1.1, use {@link #enableComponent(Accelerometer.Component, boolean)}
     */
    public void enableNotification(Component component);
    /**
     * Disable notifications from a component
     * @param component Component to disable notifications from
     * @deprecated As of v1.1, use {@link #disableComponent(Accelerometer.Component)}
     */
    public void disableNotification(Component component);
    
    /**
     * Read component configuration.  When data is ready, the 
     * {@link Callbacks#receivedConfiguration(Accelerometer.Component, byte[])} callback function 
     * will be called
     * @param component Component to read configuration from
     * @see Callbacks#receivedConfiguration(Accelerometer.Component, byte[])
     */
    public void readComponentConfiguration(Component component);
    /**
     * Set component configuration
     * @param component Component to write configuration to
     */
    public void setComponentConfiguration(Component component, byte[] data);
    
}
