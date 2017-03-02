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

import com.mbientlab.metawear.MetaWearBoard;

import java.util.UUID;

import bolts.Task;

/**
 * Bluetooth GATT operations used by the API, must be implemented by the target platform
 * @author Eric Tsai
 */
public interface BtleGatt {
    /**
     * Defines callback functions for handling asynchronous notifications from the GATT server.
     * @author Eric Tsai
     */
    interface Callback {
        /**
         * Handles disconnect events that were not initiated by the API
         * @param status    Status code reported by the btle stack
         */
        void onUnexpectedDisconnect(int status);
        /**
         * Handles a disconnect event initiated through the API
         */
        void onDisconnect();
        /**
         * Handles characteristic changes received by the {@link MetaWearBoard#METAWEAR_NOTIFY_CHAR} characteristic
         * @param value    New value of the characteristic
         */
        void onMwNotifyCharChanged(byte[] value);
        /**
         * Handles responses from a GATT read operation
         * @param characteristic    Characteristic that was read
         * @param value             Value of the read
         */
        void onCharRead(BtleGattCharacteristic characteristic, byte[] value);
    }
    /**
     * Register callback functions to handle notifications from the GATT server
     * @param callback    Callbacks that will be run
     */
    void registerCallback(Callback callback);

    /**
     * Types of GATT write operation that can be performed
     * @author Eric Tsai
     */
    enum GattCharWriteType {
        /** Write characteristic with an ack requested from the remote device */
        WRITE_WITH_RESPONSE,
        /** Write characteristic without requiring an ack from the remote device */
        WRITE_WITHOUT_RESPONSE
    }
    /**
     * Write a value to the GATT characteristic
     * @param gattChar     Characteristic to write
     * @param writeType    Type of write to perform
     * @param value        Value to write
     */
    void writeCharacteristic(BtleGattCharacteristic gattChar, GattCharWriteType writeType, byte[] value);
    /**
     * Read the value of a GATT characteristic.  Received values are passed to the {@link Callback#onCharRead(BtleGattCharacteristic, byte[])}
     * callback function
     * @param gattChar    Characteristic to read
     */
    void readCharacteristic(BtleGattCharacteristic gattChar);

    /**
     * Checks if a service exists
     * @param serviceUuid    UUID identifying the service to lookup
     * @return True if service exists, false if not
     */
    boolean serviceExists(UUID serviceUuid);

    /**
     * A disconnect attempted initiated by the local device
     * @return Task holding the result of the disconnect attempt
     */
    Task<Void> disconnectAsync();
    /**
     * A disconnect attempt that will be initiated by the remote device
     * @return Task holding the result of the disconnect attempt
     */
    Task<Void> boardDisconnectAsync();
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
