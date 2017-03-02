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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import bolts.Task;

/**
 * Object representing a MetaWear board
 * @author Eric Tsai
 */
public interface MetaWearBoard {
    /**
     * UUID identifying the MetaWear GATT service and the advertising UUID.  This UUID can be used to remove
     * non MetaWear devices from am Bluetooth LE scan.
     */
    UUID METAWEAR_GATT_SERVICE = UUID.fromString("326A9000-85CB-9195-D9DD-464CFBBAE75A");
    /**
     * UUID identifying the characteristic under the MetaWear GATT service, defined by {@link #METAWEAR_GATT_SERVICE},
     * that MetaWear uses to communicate with the local device.  Enable notifications on this characteristic.
     */
    UUID METAWEAR_NOTIFY_CHAR = UUID.fromString("326A9006-85CB-9195-D9DD-464CFBBAE75A");
    /**
     * UUID identifying a MetaWear board in MetaBoot mode.  A MetaWear board advertising with this UUID indicates
     * it is in MetaBoot mode.
     */
    UUID METABOOT_SERVICE = UUID.fromString("00001530-1212-efde-1523-785feabcd123");

    /**
     * Determines the board model of the currently connected device
     * @return Board model, null if unable to determine
     */
    Model getModel();
    /**
     * Same behavior as {@link #getModel()} except the returned value is a friendly name rather than an enum
     * @return Board model as string
     */
    String getModelString();
    /**
     * Retrieves the MAC address of the board
     * @return Board's MAC address
     */
    String getMacAddress();

    /**
     * Reads the current RSSI value
     * @return Task holding the returned RSSI value
     */
    Task<Integer> readRssiAsync();
    /**
     * Reads the battery level characteristic
     * @return Task holding the battery level
     */
    Task<Byte> readBatteryLevelAsync();
    /**
     * Reads supported characteristics from the Device Information service
     * @return Task holding the device information
     */
    Task<DeviceInformation> readDeviceInformationAsync();
    /**
     * Downloads the latest firmware release for the board to your local device.  You must be connected to the
     * board before calling this function.
     * @return Task holding the file pointing to where the downloaded firmware resides on the local device
     */
    Task<File> downloadLatestFirmwareAsync();
    /**
     * Checks if there is a newer version of the firmware available for your board.  The firmware check requires
     * you to be connected to your board and an active internet connection on your local device.
     * @return Task holding the result of the firmware check, true if a firmware update is available
     */
    Task<Boolean> checkForFirmwareUpdateAsync();

    /**
     * Establishes a Bluetooth Low Energy connection to the MetaWear board
     * @return Task holding the result of the connect attempt
     */
    Task<Void> connectAsync();
    /**
     * Establishes a Bluetooth Low Energy connection to the MetaWear board
     * @param delay    How long to wait (in milliseconds) before attempting to connect
     * @return Task holding the result of the connect attempt
     */
    Task<Void> connectAsync(long delay);
    /**
     * Disconnects from the board and cancels pending {@link #connectAsync()} tasks
     * @return Task holding the result of the disconnect attempt
     */
    Task<Void> disconnectAsync();

    /**
     * Handler for when the API is not expecting a disconnect event
     * @author Eric Tsai
     */
    interface UnexpectedDisconnectHandler {
        /**
         * Callback method that is invoked when the Bluetooth connection is unexpectedly dropped
         * @param status    Status from the connection changed callback
         */
        void disconnected(int status);
    }
    /**
     * Set a handler for unexpected disconnects
     * @param handler    Handler for unexpected disconnects
     */
    void onUnexpectedDisconnect(UnexpectedDisconnectHandler handler);

    /**
     * Gets the connection state
     * @return True if a btle connection is active, false otherwise
     */
    boolean isConnected();
    /**
     * Checks if the board is in the MetaBoot (bootloader) mode.  If it is, you will not be able to interact
     * with the board outside of reading RSSI values and updating firmware.
     * @return True if the board is in MetaBoot mode, false otherwise
     */
    boolean inMetaBootMode();

    /**
     * Sensor, peripheral, or firmware feature
     * @author Eric Tsai
     */
    interface Module { }
    /**
     * Retrieves a reference to the requested module if supported.  You must connected to the board before
     * calling this function and the board must not be in MetaBoot mode
     * @param moduleClass   Module class to lookup
     * @param <T>           Runtime type the return value is casted as
     * @return Reference to the requested module, null if the board is not connected, module not supported, or board is in MetaBoot mode
     */
    <T extends Module> T getModule(Class<T> moduleClass);
    /**
     * Retrieves a reference to the requested module if supported, throws a checked exception if the function fails.
     * You must connected to the board before calling this function and the board must not be in MetaBoot mode
     * @param moduleClass   ModuleId class to lookup
     * @param <T>           Runtime type the return value is casted as
     * @return Reference to the requested module
     * @throws UnsupportedModuleException If the requested module is not supported or the board is in MetaBoot mode
     */
    <T extends Module> T getModuleOrThrow(Class<T> moduleClass) throws UnsupportedModuleException;

    /**
     * Retrieves a route
     * @param id    Numerical ID to look up
     * @return Route corresponding to the specified ID, null if none can be found
     */
    Route lookupRoute(int id);
    /**
     * Retrieves an observer
     * @param id    Numerical ID to look up
     * @return Observer corresponding to the specified ID, null if none can be found
     */
    Observer lookupObserver(int id);
    /**
     * Removes all routes and resources allocated on the board (observers, data processors, timers, and loggers)
     */
    void tearDown();

    /**
     * Serialize object state and write the state to the local disk
     * @throws IOException If the internal OutputStream throws an exception
     */
    void serialize() throws IOException;
    /**
     * Serialize object state and write the state to the provided output stream
     * @param outs    Output stream to write to
     * @throws IOException If the provided OutputStream throws an exception
     */
    void serialize(OutputStream outs) throws IOException;
    /**
     * Restore serialized state from the local disk if available
     * @throws IOException If the internal InputStream throws an exception
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    void deserialize() throws IOException, ClassNotFoundException;
    /**
     * Restore serialized state from the provided input stream
     * @param ins    Input stream to read from
     * @throws IOException If the provided InputStream throws an exception
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    void deserialize(InputStream ins) throws IOException, ClassNotFoundException;
}