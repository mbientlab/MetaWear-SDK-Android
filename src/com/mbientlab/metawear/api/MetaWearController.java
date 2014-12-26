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
package com.mbientlab.metawear.api;

import com.mbientlab.metawear.api.GATT.GATTCharacteristic;

/**
 * Controller that issues commands to a MetaWear board
 * @author Eric Tsai
 */
public interface MetaWearController {
    /**
     * Generic type for classes that communicate with a MetaWear module
     * @author Eric Tsai
     * @see Module
     */
    public interface ModuleController { }
    
    /**
     * Callback functions for MetaWear specific events.  These callbacks are for 
     * receiving data from a register, triggered by a read or notify request.
     * @author Eric Tsai
     * @see Module
     * @see Register
     */
    public interface ModuleCallbacks {
        /**
         * Get the module the notification represents
         * @return Module enum
         * @see Module
         */
        public Module getModule();
    }

    /**
     * Callbacks for general Bluetooth Le events
     * @author Eric Tsai
     */
    public static abstract class DeviceCallbacks {
        public enum GattOperation {
            CONNECTION_STATE_CHANGE,
            DISCOVER_SERVICES,
            DESCRIPTOR_WRITE,
            CHARACTERISTIC_WRITE,
            CHARACTERISTIC_READ,
            RSSI_READ;
        }
        /**
         * Bluetooth device connected
         */
        public void connected() { }
        /**
         * Bluetooth device disconnected
         */
        public void disconnected() { }
        /**
         * The data from a GATT characteristic was received
         * @param characteristic Characteristic that was read
         * @param data Bytes read from the characteristic
         */
        public void receivedGATTCharacteristic(GATTCharacteristic characteristic, byte[] data) { }
        /**
         * Remote RSSI value received from the device
         * @param rssi RSSI value for the remote device
         */
        public void receivedRemoteRSSI(int rssi) { }
        /**
         * Called when a GATT operation returns an error
         * @param gattOp GATT operation that returned the error
         * @param status Status code returned in one of the 
         * {@link android.bluetooth.BluetoothGattCallback} callback functions
         */
        public void gattError(GattOperation gattOp, int status) { }
    }
    
    /**
     * Get the controller for the desired module.
     * @param module Module to control
     * @return Controller for the desired module
     */    
    public ModuleController getModuleController(Module module);

    /**
     * Reads general device information from the MetaWear board.  The information available is 
     * listed in the DeviceInformation enum and the receivedGATTCharacteristic function will be 
     * called for each characteristic that has been read
     * @see DeviceCallbacks#receivedGATTCharacteristic(com.mbientlab.metawear.api.GATT.GATTCharacteristic, byte[])
     * @see com.mbientlab.metawear.api.characteristic.DeviceInformation
     */
    public void readDeviceInformation();
    /**
     * Reads the battery level characteristic.  When data is received, the 
     * {@link DeviceCallbacks#receivedGATTCharacteristic(com.mbientlab.metawear.api.GATT.GATTCharacteristic, byte[])} callback 
     * function will be called with the BATTERY_LEVEL GATT characteristic
     * @see com.mbientlab.metawear.api.characteristic.Battery
     */
    public void readBatteryLevel();
    /**
     * Reads the remote RSSI value of the connected device.  When data is received, 
     * the {@link DeviceCallbacks#receivedRemoteRSSI(int)} callback function will be 
     * called with the RSSI value.
     */
    public void readRemoteRSSI();
    
    /**
     * Add a module callback for the broadcast receiver
     * @param callback Module callback to add
     * @return The calling object
     */
    public MetaWearController addModuleCallback(ModuleCallbacks callback);
    /**
     * Add a device callback for the broadcast receiver
     * @param callback Device callback to add
     * @return The calling object
     */
    public MetaWearController addDeviceCallback(DeviceCallbacks callback);
    /**
     * Remove all callbacks associated with this controller
     */
    public void clearCallbacks();
    /**
     * Remove specific module callback
     * @param callback
     */
    public void removeModuleCallback(ModuleCallbacks callback);
    /**
     * Remove specific device callback
     * @param callback
     */
    public void removeDeviceCallback(DeviceCallbacks callback);
    
    /**
     * Connect to the board this controller is attached to
     */
    public void connect();
    /**
     * Close the connection to the board this controller is attached to
     * @param notify True to make a call to the {@link DeviceCallbacks#disconnected()} 
     * callback function
     * @param cleanState True if any internal state associated with the board should be
     * removed.
     */
    public void close(boolean notify, boolean cleanState);
    /**
     * Convenience function for calling {@link #close(boolean, boolean)} with (false, true)
     */
    public void close();
    /**
     * Restart the connection to the board this controller is attached to
     */
    public void reconnect();
    /**
     * Gets the connection statue to a board
     * @return True if connected to a board
     */
    public boolean isConnected();
}
