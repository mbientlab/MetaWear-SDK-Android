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
        /** Checks and sets free fall detection status */
        FREE_FALL_ENABLE {
            @Override public byte opcode() { return 0x5; }
        },
        /** Sets or retrieves free fall detection configuration */
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
        /** Stores free fall state, enables/disables free fall detection */
        FREE_FALL_VALUE { 
            @Override public byte opcode() { return 0x7; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    final byte[] data) {
                if ((data[2] & 0x80) == 0x80) {
                    FreeFallInfo ffInfo= new FreeFallInfo() {
                        @Override
                        public boolean isAboveThreshold(Axis axis) {
                            byte mask= (byte) (2 << (2 * axis.ordinal()));
                            return (data[2] & mask) == mask;
                        }

                        @Override
                        public byte getDirection(Axis axis) {
                            byte mask= (byte) (1 << (2 * axis.ordinal()));
                            return (byte) ((data[2] & mask) == mask ? -1 : 1);
                        }
                    };
                    
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).freeFallDetected(ffInfo);
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
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks)it).receivedOrientation(data[2]);
                    ((Callbacks)it).orientationChanged(LaPoOrientation.values()[(data[2] >> 1) & 0x3], 
                            BaFroOrientation.values()[data[2] & 0x1]);
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
                    byte[] data) {
                if ((data[2] & 0x80) == 0x80) {
                    AxisEvent zEvent= (data[2] & 0x40) == 0x40 ? AxisEvent.values()[(data[2] & 0x4) >> 2] : AxisEvent.NOT_ACTIVE, 
                            yEvent= (data[2] & 0x20) == 0x20 ? AxisEvent.values()[(data[2] & 0x2) >> 1] : AxisEvent.NOT_ACTIVE,
                            xEvent= (data[2] & 0x10) == 0x10 ? AxisEvent.values()[(data[2] & 0x1)] : AxisEvent.NOT_ACTIVE;
                            
                    for(ModuleCallbacks it: callbacks) {
                        if ((data[2] & 0x8) == 0x8) ((Callbacks) it).doubleTapDetected(xEvent, yEvent, zEvent);
                        else ((Callbacks) it).tapDetected(xEvent, yEvent, zEvent);
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
                    byte[] data) {
                if ((data[2] & 0x40) == 0x40) {
                    AxisEvent zEvent= (data[2] & 0x20) == 0x20 ? AxisEvent.values()[(data[2] & 0x10) >> 4] : AxisEvent.NOT_ACTIVE, 
                            yEvent= (data[2] & 0x8) == 0x8 ? AxisEvent.values()[(data[2] & 0x4) >> 2] : AxisEvent.NOT_ACTIVE,
                            xEvent= (data[2] & 0x2) == 0x2 ? AxisEvent.values()[(data[2] & 0x1)] : AxisEvent.NOT_ACTIVE;
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).shakeDetected(xEvent, yEvent, zEvent);
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
         * Called when the ble radio has received accelerometer motion data 
         * @param x X component of acceleration, in raw bytes
         * @param y Y component of acceleration, in raw bytes
         * @param z Z component of acceleration, in raw bytes
         */
        public void receivedDataValue(short x, short y, short z) { }
        
        /**
         * Called when free fall is detected.  This function will be repeatedly called 
         * while free fall is detected.
         * @param ffInfo Free fall information encapsulated in an object
         */
        public void freeFallDetected(FreeFallInfo ffInfo) { }
        
        /**
         * Called when free fall is detected
         * @deprecated As of v1.1, replaced by {@link Callbacks#freeFallDetected(Accelerometer.FreeFallInfo)}
         */
        @Deprecated
        public void inFreeFall() { }
        /**
         * Called when free fall has stopped
         * @deprecated As of v1.1, callback function was never properly implemented and 
         * has been replaced by {@link Callbacks#freeFallDetected(Accelerometer.FreeFallInfo)}
         */
        public void stoppedFreeFall() { }
        
        /**
         * Called when the orientation has changed
         * @param orientation Orientation information from the accelerometer's status register
         * @deprecated As of v1.1, replaced by 
         * {@link Callbacks#orientationChanged(Accelerometer.LaPoOrientation, Accelerometer.BaFroOrientation)} 
         */
        @Deprecated
        public void receivedOrientation(byte orientation) { }
        
        /**
         * Called when the orientation has changed
         * @param lapo Landscape/Portrait orientation
         * @param bafro Back/Front orientation
         */
        public void orientationChanged(LaPoOrientation lapo, BaFroOrientation bafro) { }
        
        /**
         * Called when a single tap has been detected
         * @param xEvent Event information for the X axis
         * @param yEvent Event information for the Y axis
         * @param zEvent Event information for the Z axis
         */
        public void tapDetected(AxisEvent xEvent, AxisEvent yEvent, AxisEvent zEvent) { }
        /**
         * Called when a double tap has been detected
         * @param xEvent Event information for the X axis
         * @param yEvent Event information for the Y axis
         * @param zEvent Event information for the Z axis
         */
        public void doubleTapDetected(AxisEvent xEvent, AxisEvent yEvent, AxisEvent zEvent) { }
        
        /**
         * Called when a shake motion is detected.  This function will be continuously called 
         * as long as the set threshold is exceeded in the desired direction
         * @param xEvent Event information for the X axis
         * @param yEvent Event information for the Y axis
         * @param zEvent Event information for the Z axis
         */
        public void shakeDetected(AxisEvent xEvent, AxisEvent yEvent, AxisEvent zEvent) { }
    }

    /**
     * Enumeration of components in the accelerometer
     * @author Eric Tsai
     */
    public enum Component {
        /** XYZ data sampling */
        DATA(Register.DATA_ENABLE, Register.DATA_SETTINGS, Register.DATA_VALUE),
        /** Free fall detection */
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
     * Axis information for the detection callback functions
     * @author Eric Tsai
     */
    public enum AxisEvent {
        POSITIVE_POLARITY,
        NEGATIVE_POLARITY,
        NOT_ACTIVE;
    }
    
    /**
     * Orientation modes for landscape and portrait arrangements
     * @author Eric Tsai
     */
    public enum LaPoOrientation {
        PORTRAIT_UP,
        PORTRAIT_DOWN,
        LANDSCAPE_RIGHT,
        LANDSCAPE_LEFT;
    }
    
    /**
     * Orientation modes for back and front arrangements
     * @author Eric Tsai
     */
    public enum BaFroOrientation {
        FRONT,
        BACK;
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
        X_AXIS,
        Y_AXIS,
        Z_AXIS;
    }
    
    /**
     * Wrapper class encapsulating free fall information received from the board
     * @author Eric Tsai     
     */
    public interface FreeFallInfo {
        /**
         * Returns whether or not the board exceeded the threshold on the specific axis
         * @param axis Axis to check
         * @return True if board's axis exceeded threshold, false otherwise
         */
        public boolean isAboveThreshold(Axis axis);
        /**
         * Returns the direction the board is moving in on the specific axis
         * @param axis Axis to check
         * @return -1 if board was moving in the negative axis direction, 1 if 
         * board was in the positive direction
         */
        public byte getDirection(Axis axis);
    }
    
    /**
     * Disable the accelerometer component.  If saveConfig is false, you will need 
     * to reconfigure the component via the appropriate enable detection function.
     * @param component Component to disable
     * @param saveConfig True if the component configuration should be saved
     */
    public void disableComponent(Component component, boolean saveConfig);
    /**
     * Disable all enabled components.  If saveConfig is false, you will need 
     * to reconfigure the component via the appropriate enable detection function.
     * @param saveConfig True if the all configurations should be saved
     */
    public void disableComponents(boolean saveConfig);
    /**
     * Enable tap detection
     * @param type Tap type to detected
     * @param axis Which axis to detect taps on
     * @return Configuration object to tweak the tap settings
     */
    public ThresholdConfig enableTapDetection(TapType type, Axis axis);
    /**
     * Enable shake detection
     * @param axis Which axis to detect shake motion
     * @return Configuration object to tweak the shake settings
     */
    public ThresholdConfig enableShakeDetection(Axis axis);
    /**
     * Enable orientation detection
     * @return Configuration object to tweak the orientation settings
     */
    public AccelerometerConfig enableOrientationDetection();
    /**
     * Enable free fall detection
     * @return Configuration object to tweak the free fall settings
     */
    public ThresholdConfig enableFreeFallDetection();
    /**
     * Enable data sampling of the XYZ axes
     * @return Configuration object to tweak the data sampling settings
     */
    public SamplingConfig enableXYZSampling();
    /**
     * Starts detection / sampling of enabled components
     */
    public void startActivities();
    /**
     * Stops activity for enabled components
     */
    public void stopActivities();
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
         * Disables ble notifications but will still internally detect the desired event
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
    }
    
    /**
     * Enables accelerometer activity for the given component
     * @param component Component to enable
     * @param notify True if notifications should be sent via ble radio
     */
    public void enableActivity(Component component, boolean notify);
    /**
     * Disable accelerometer activity for the given component.  Currently, this 
     * function is an alias for {@link #disableNotification(Accelerometer.Component)}
     * @param component Component to disable
     */
    public void disableActivity(Component component);
    
    /**
     * Enable notifications from a component
     * @param component Component to enable notifications from
     * @deprecated As of v1.1, use {@link #enableActivity(Accelerometer.Component, boolean)}
     */
    public void enableNotification(Component component);
    /**
     * Disable notifications from a component
     * @param component Component to disable notifications from
     * @deprecated As of v1.1, use {@link #disableActivity(Accelerometer.Component)}
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
