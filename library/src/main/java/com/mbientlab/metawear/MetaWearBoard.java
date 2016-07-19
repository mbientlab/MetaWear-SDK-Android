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

import java.io.File;
import java.io.InputStream;
import java.util.UUID;

/**
 * Controller for communicating with a MetaWear board
 * @author Eric Tsai
 */
public interface MetaWearBoard {
    /**
     * Service UUID identifying a MetaWear board.  This uuid can be used to filter non MetaWear devices
     * from a Bluetooth LE scan.
     */
    UUID METAWEAR_SERVICE_UUID= UUID.fromString("326A9000-85CB-9195-D9DD-464CFBBAE75A");
    /**
     * Old name for the MetaWear service UUID, replaced in v2.1.0
     * #deprecated Used {@link #METAWEAR_SERVICE_UUID} instead
     */
    @Deprecated
    UUID META_WEAR_SERVICE_UUID= METAWEAR_SERVICE_UUID;
    /**
     * Service UUID identifying a MetaWear board in MetaBoot mode
     */
    UUID METABOOT_SERVICE_UUID= new UUID(0x000015301212EFDEL, 0x1523785FEABCD123L);

    /**
     * Retrieves the MAC address of the board
     * @return Board's MAC address
     */
    String getMacAddress();

    /**
     * Checks if the board is in MetaBoot mode
     * @return True if it is in MetaBoot mode
     */
    boolean inMetaBootMode();

    /**
     * Class for handling notifications from the device firmware update operation
     * @author Eric Tsai
     * @deprecated The API will no longer perform firmware updates in future releases.  Instead, the dfu will be handled with Nordic's
     * <a href="https://github.com/NordicSemiconductor/Android-DFU-Library">Android DFU library</a>.
     */
    @Deprecated
    interface DfuProgressHandler {
        /**
         * Enumeration of the DFU operation states
         */
        enum State {
            /** Downloading the new firmware from the internet */
            DOWNLOADING,
            /** Preparing the board and library for the update */
            INITIALIZING,
            /** Starting the update */
            STARTING,
            /** Validating the firmware upload */
            VALIDATING,
            /** Disconnecting from the board */
            DISCONNECTING
        }

        /**
         * Called when the DFU has progressed to a new state
         * @param dfuState    New state of the update operation
         */
        void reachedCheckpoint(State dfuState);

        /**
         * Called when upload completion progress has been received.  This method also functions as
         * an implied "uploading" state.
         * @param progress    Integer between [0, 100] representing completion percentage
         */
        void receivedUploadProgress(int progress);
    }
    /**
     * Updates the firmware on the board to the latest available release.  The update requires an active internet
     * connection on your Android device and will terminate the Bluetooth connection when completed without calling the
     * {@link ConnectionStateHandler#disconnect() disconnected()} callback function.  You must be connected to the board
     * before calling this function, otherwise, it will fail.
     * @param handler    Handler for processing DFU progress notifications
     * @return Result of the operation that will be available when the DFU is finished
     * @deprecated The API will no longer perform firmware updates in future releases.  Instead, the dfu will be handled with Nordic's
     * <a href="https://github.com/NordicSemiconductor/Android-DFU-Library">Android DFU library</a>.
     * @see #downloadLatestFirmware()
     */
    @Deprecated
    AsyncOperation<Void> updateFirmware(DfuProgressHandler handler);
    /**
     * Updates the firmware on the board with a user specified firmware file.  Executing this function will terminate the Bluetooth
     * connection without calling the {@link ConnectionStateHandler#disconnect() disconnected()} callback function.  You must be connected
     * to the board before calling this feature, otherwise, it will fail.
     * @param firmwareHexPath    Path to the firmware file
     * @param handler            Handler for processing DFU progress notifications
     * @return Result of the operation that will be available when the DFU is finished
     * @deprecated The API will no longer perform firmware updates in future releases.  Instead, the dfu will be handled with Nordic's
     * <a href="https://github.com/NordicSemiconductor/Android-DFU-Library">Android DFU library</a>.
     * @see #downloadLatestFirmware()
     */
    @Deprecated
    AsyncOperation<Void> updateFirmware(File firmwareHexPath, DfuProgressHandler handler);
    /**
     * Updates the firmware on the board using data from the provided input stream.  Executing this function will terminate the Bluetooth
     * connection without calling the {@link ConnectionStateHandler#disconnect() disconnected()} callback function.  You must be connected
     * to the board before calling this feature, otherwise, it will fail.
     * @param firmwareStream    Path to the firmware file
     * @param handler            Handler for processing DFU progress notifications
     * @return Result of the operation that will be available when the DFU is finished
     * @deprecated The API will no longer perform firmware updates in future releases.  Instead, the dfu will be handled with Nordic's
     * <a href="https://github.com/NordicSemiconductor/Android-DFU-Library">Android DFU library</a>.
     * @see #downloadLatestFirmware()
     */
    @Deprecated
    AsyncOperation<Void> updateFirmware(InputStream firmwareStream, DfuProgressHandler handler);
    /**
     * Terminates a DFU in progress, resulting in a failure.  Does nothing if no DFU is in progress
     * @deprecated The API will no longer perform firmware updates in future releases.  Instead, the dfu will be handled with Nordic's
     * <a href="https://github.com/NordicSemiconductor/Android-DFU-Library">Android DFU library</a>.
     * @see #downloadLatestFirmware()
     */
    @Deprecated
    void abortFirmwareUpdate();

    /**
     * Downloads the latest firmware release for the board to your Android device.  You must be connected to the
     * board before calling this function.
     * @return Path to where the firmware resides on the Android device, available when the download is completed
     */
    AsyncOperation<File> downloadLatestFirmware();
    /**
     * Checks if there is a newer version of the firmware available for your board.  If a newer firmware version
     * exists, the operation will return true.  The firmware check requires an active internet connection on your
     * Android device.
     * @return Result of operation that will be available when the firmware check is completed
     */
    AsyncOperation<Boolean> checkForFirmwareUpdate();

    /**
     * Base class for on-board sensors or features supported by the board's firmware
     * @author Eric Tsai
     */
    interface Module { }
    /**
     * Retrieves a pointer to the requested module, if supported by the current board and firmware, and
     * the board is not in MetaBoot mode.  The API must be connected to the board to use this function.
     * @param moduleClass    Module class to lookup
     * @return Reference to the requested module, null if the BLE connection is not active
     * @throws UnsupportedModuleException if the module is not available on the board, or the board is in
     * MetaBoot mode
     */
    <T extends Module> T getModule(Class<T> moduleClass) throws UnsupportedModuleException;

    /**
     * Variant of #{@link #getModule(Class)} that does not throw a checked exception.  The API must be
     * connected to the board to use this function.
     * @param moduleClass    Module class to lookup
     * @return Reference to the module class, null if not supported, board is in MetaBoot mode, or
     * the API is not connected to the board
     */
    <T extends Module> T lookupModule(Class<T> moduleClass);

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
         * Called when a connection to the MetaWear board is established and ready to be used
         */
        public void connected() { }

        /**
         * Called when the connection is lost
         */
        public void disconnected() { }

        /**
         * Called if a connection attempt failed
         * @param status    Status code reported by one of the BluetoothGattCallback methods, -1 if a
         *                  a connection timeout occurred
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
     * Establish a connection to the board and prepare the API to communicate with the board.  If this
     * is not completed within a timeout interval, the
     * {@link com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler#failure(int, Throwable) failure} callback
     * function will be called with a TimeoutException
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
     * Removes all routes and timers from the board.  It does not reset the board so any configuration
     * changes are preserved
     */
    void tearDown();

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
