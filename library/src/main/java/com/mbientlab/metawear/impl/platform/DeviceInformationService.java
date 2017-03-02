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

/**
 * Manufacturer and/or vendor information about a device
 * @author Eric Tsai
 */
public class DeviceInformationService {
    private static final UUID SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    /** Revision for the firmware within the device */
    public static final BtleGattCharacteristic FIRMWARE_REVISION = new BtleGattCharacteristic(
            SERVICE_UUID,
            UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
    );
    /** Model number that is assigned by the device */
    public static final BtleGattCharacteristic MODEL_NUMBER = new BtleGattCharacteristic(
            SERVICE_UUID,
            UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
    );
    /** Revision for the hardware within the device */
    public static final BtleGattCharacteristic HARDWARE_REVISION = new BtleGattCharacteristic(
            SERVICE_UUID,
            UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
    );
    /** Name of the manufacturer of the device */
    public static final BtleGattCharacteristic MANUFACTURER_NAME = new BtleGattCharacteristic(
            SERVICE_UUID,
            UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    );
    /** Serial number for a particular instance of the device */
    public static final BtleGattCharacteristic SERIAL_NUMBER = new BtleGattCharacteristic(
            SERVICE_UUID,
            UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
    );
}
