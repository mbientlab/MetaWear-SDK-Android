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

package com.mbientlab.metawear.impl.platform;

import java.util.UUID;

import bolts.Task;

/**
 * Bluetooth GATT operations used by the API, must be implemented by the target platform
 * @author Eric Tsai
 * @version 2.0
 */
public interface BtleGatt {
    /**
     * Listener for GATT characteristic notifications
     * @author Eric Tsai
     */
    interface NotificationListener {
        /**
         * Called when the GATT characteristic's value changes
         * @param value    New value for the characteristic
         */
        void onChange(byte[] value);
    }
    /**
     * Handler for disconnect events
     * @author Eric Tsai
     */
    interface DisconnectHandler {
        /**
         * Called when the connection with the BLE device has been closed
         */
        void onDisconnect();
        /**
         * Similar to {@link #onDisconnect()} except this variant handles instances where the connection
         * was unexpectedly dropped i.e. not initiated by the API
         * @param status    Status code reported by the btle stack
         */
        void onUnexpectedDisconnect(int status);
    }
    /**
     * Write types for the GATT characteristic
     * @author Eric Tsai
     */
    enum WriteType {
        WITHOUT_RESPONSE,
        DEFAULT
    }

    /**
     * Register a handler for disconnect events
     * @param handler    Handler to respond to the dc events
     */
    void onDisconnect(DisconnectHandler handler);
    /**
     * Checks if a service exists
     * @param gattService    UUID identifying the service to lookup
     * @return True if service exists, false if not
     */
    boolean serviceExists(UUID gattService);
    /**
     * Writes a GATT characteristic and its value to the remote device
     * @param characteristic    GATT characteristic to write
     * @param type              Type of GATT write to use
     * @param value             Value to be written
     * @return Task holding the result of the operation
     */
    Task<Void> writeCharacteristicAsync(BtleGattCharacteristic characteristic, WriteType type, byte[] value);
    /**
     * Convenience method to do bulk characteristic reads
     * @param characteristics    Array of characteristics to read
     * @return Task which holds the characteristic values in order if all reads are successful
     */
    Task<byte[][]> readCharacteristicAsync(BtleGattCharacteristic[] characteristics);
    /**
     * Reads the requested characteristic's value
     * @param characteristic    Characteristic to read
     * @return Task holding the characteristic's value if successful
     */
    Task<byte[]> readCharacteristicAsync(BtleGattCharacteristic characteristic);
    /**
     * Enable notifications for the characteristic
     * @param characteristic    Characteristic to enable notifications for
     * @param listener          Listener for handling characteristic notifications
     * @return Task holding the result of the operation
     */
    Task<Void> enableNotificationsAsync(BtleGattCharacteristic characteristic, NotificationListener listener);

    /**
     * A disconnect attempted initiated by the Android device
     * @return Task holding the result of the disconnect attempt
     */
    Task<Void> localDisconnectAsync();
    /**
     * A disconnect attempt that will be initiated by the remote device
     * @return Task holding the result of the disconnect attempt
     */
    Task<Void> remoteDisconnectAsync();
    /**
     * Connects to the GATT server on the remote device
     * @return Task holding the result of the connect attempt
     */
    Task<Void> connectAsync();
    /**
     * Read the remote device's RSSI value
     * @return Task holding the RSSI value, if successful
     */
    Task<Integer> readRssiAsync();
}
