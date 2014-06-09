/**
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
package com.mbientlab.metawear.api;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Builder for constructor a MetaWear specific BroadcastReceiver
 * @author Eric Tsai
 */
public class BroadcastReceiverBuilder {
    /** Extra Intent information for the characteristic UUID */
    public static final String CHARACTERISTIC_UUID= 
            "com.mbientlab.com.metawear.api.GattUpdateReceiver.CHARACTERISTIC_UUID";
    /** Extra Intent information for the characteristic value */
    public static final String CHARACTERISTIC_VALUE= 
            "com.mbientlab.com.metawear.api.GattUpdateReceiver.CHARACTERISTIC_VALUE";
    
    /**
     * Notification from a MetaWear module
     * @author Eric Tsai
     */
    public interface ModuleNotification {
        /**
         * Get the module the notification represents
         * @return Module enum
         * @see Commands.Module
         */
        public Commands.Module getModule();
    }
    /**
     * Notification from the accelerometer module
     * @author Eric Tsai
     */
    public abstract static class Accelerometer implements ModuleNotification {
        public final Commands.Module getModule() { return Commands.Module.ACCELEROMETER; }

        /**
         * Called when the accelerometer has sent its XYZ motion data
         * @param x X component of the motion
         * @param y Y component of the motion
         * @param z Z component of the motion
         */
        public void receivedDataValue(short x, short y, short z) { }
        /** Called when free fall is detected */
        public void inFreeFall() { }
        /** Called when free fall has stopped */
        public void stoppedFreeFall() { }
        /** Called when the orientation has changed */
        public void receivedOrientation(byte orientation) { }
    }

    /**
     * Notification from the mechanical switch module
     * @author Eric Tsai
     */
    public abstract static class MechanicalSwitch implements ModuleNotification {
        public final Commands.Module getModule() { return Commands.Module.MECHANICAL_SWITCH; }

        /** Called when the switch is pressed */
        public void pressed() { }
        /** Called when the switch is released */
        public void released() { }
    }

    /**
     * Notification from the temperature module
     * @author Eric Tsai
     */
    public abstract static class Temperature implements ModuleNotification {
        public Commands.Module getModule() { return Commands.Module.TEMPERATURE; }

        /**
         * Called when MetaWear has responded with the temperature reading
         * @param degrees Value of the temperature in Celsius
         */
        public void receivedTemperature(float degrees) { }
    }
    
    /**
     * Notification from the GPIO module 
     * @author Eric Tsai
     */
    public abstract static class GPIO implements ModuleNotification {
        public Commands.Module getModule() { return Commands.Module.GPIO; }
        
        /**
         * Called when the analog input has been read as an absolute value
         * @param value Value in mV
         */
        public void receivedAnalogInputAsAbsValue(short value) { }
        /**
         * Called when the analog input has been read as a supply ratio
         * @param value 10 bit representation of the voltage where 0 = 0V and 1023 = 3V
         */
        public void receivedAnalogInputAsSupplyRatio(short value) { }
        /**
         * Called when the digital input has been read
         * @param value Either 0 or 1
         */
        public void receivedDigitalInput(byte value) { }
    }

    /** Collection of notifications passed in from the user */ 
    private final HashMap<Byte, ArrayList<ModuleNotification>> notifications;

    /**
     * Construct a builder for the MetaWear broadcast receiver
     */
    public BroadcastReceiverBuilder() {
        notifications= new HashMap<>();
    }
    /**
     * Adds the notification to the builder
     * @param notification Requested notification of a MetaWear module 
     * @return The calling object
     */
    public BroadcastReceiverBuilder withModuleNotification(ModuleNotification notification) {
        byte registerId= notification.getModule().id;
        if (!notifications.containsKey(registerId)) {
            notifications.put(registerId, new ArrayList<ModuleNotification>());
        }
        notifications.get(registerId).add(notification);
        return this;
    }
    
    /**
     * Constructs a MetaWear specific broadcast receiver
     * @return MetaWear specific BroadcastReceiver
     */
    public BroadcastReceiver build() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] data= (byte[])intent.getExtras().get(CHARACTERISTIC_VALUE);

                switch (intent.getAction()) {
                case Actions.MetaWear.NOTIFICATION_RECEIVED:
                    byte moduleId= data[0], registerId= (byte)(0x7f & data[1]);
                    Commands.getRegister(moduleId, registerId).notifyCallbacks(notifications.get(moduleId), data);
                    break;
                }
            }
        };
    }
}