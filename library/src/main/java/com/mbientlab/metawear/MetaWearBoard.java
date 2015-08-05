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

package com.mbientlab.metawear;

import java.util.UUID;

/**
 * Controller for communicating with a MetaWear board
 * @author Eric Tsai
 */
public interface MetaWearBoard {
    /**
     * Service UUID identifying a MetaWear board.  This uuid can be used to filter non MetaWears from a
     * Bluetooth LE scan.
     */
    UUID META_WEAR_SERVICE_UUID= UUID.fromString("326A9000-85CB-9195-D9DD-464CFBBAE75A");

    /**
     * Retrieves the MAC address of the board
     * @return Board's MAC address
     */
    String getMacAddress();

    /**
     * Base class for on-board sensors, or features supported by the board's firmware
     * @author Eric Tsai
     */
    interface Module { }
    /**
     * Retrieves a pointer to the requested module, if supported by the current board and firmware
     * @param moduleClass    Module class to lookup
     * @return Reference to the requested module, null if the BLE connection is not active
     * @throws UnsupportedModuleException If the module is not available on the board
     */
    <T extends Module> T getModule(Class<T> moduleClass) throws UnsupportedModuleException;

    /**
     * Wrapper class around the data from the device information service
     * @author Eric Tsai
     */
    interface DeviceInformation {
        /**
         * Retrieves the device's manufacturer
         * @return Manufacturer name
         */
        String manufacturer();

        /**
         * Retrieves the device's model number
         * @return Module number
         */
        String modelNumber();

        /**
         * Retrieves the device's serial number
         * @return Serial number
         */
        String serialNumber();

        /**
         * Retrieves the revision of the firmware running on the device
         * @return Firmware revision
         */
        String firmwareRevision();

        /**
         * Retrieves the revision of the hardware within the device
         * @return Hardware revision
         */
        String hardwareRevision();
    }
    /**
     * Reads supported characteristics from the Device Information service
     * @return DeviceInformation object that will be available when the read operation completes
     */
    AsyncOperation<DeviceInformation> readDeviceInformation();
    /**
     * Reads the current RSSI value
     * @return RSSI value that will be available when the read operation completes
     */
    AsyncOperation<Integer> readRssi();
    /**
     * Reads the battery level characteristic
     * @return Battery level that will be available when the read operation completes
     */
    AsyncOperation<Byte> readBatteryLevel();

    /**
     * Class for handling Bluetooth LE connection events
     * @author Eric Tsai
     */
    abstract class ConnectionStateHandler {
        /**
         * Called when a Bluetooth LE connection is successfully established
         */
        public void connected() { }

        /**
         * Called when the Bluetooth LE connection is lost
         */
        public void disconnected() { }

        /**
         * Called if a connection attempt failed
         * @param status    Status code reported by one of the BluetoothGattCallback methods
         * @param error     Error thrown by one of the BluetoothGattCallback methods
         */
        public void failure(int status, Throwable error) { }
    }
    /**
     * Sets the connection state handler
     * @param handler    Handler to use for connection events
     */
    void setConnectionStateHandler(ConnectionStateHandler handler);
    /**
     * Establish a connection to the board
     */
    void connect();
    /**
     * Close the connection to the board
     */
    void disconnect();
    /**
     * Retrieves the connection state
     * @return True if a connection is currently established
     */
    boolean isConnected();

    /**
     * Retrieves a route manager
     * @param id    Numerical ID to look up
     * @return Manager corresponding to the specified ID, null if none can be found
     */
    RouteManager getRouteManager(int id);
    /**
     * Remove all data routes
     */
    void removeRoutes();

    /**
     * Serializes the internal state of the class
     * @return Byte array representing the class' state
     */
    byte[] serializeState();
    /**
     * Updates the internal state with the values in the byte array
     * @param state    New state of the class
     */
    void deserializeState(byte[] state);
}
