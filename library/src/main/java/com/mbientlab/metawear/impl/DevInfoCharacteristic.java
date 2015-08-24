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

package com.mbientlab.metawear.impl;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by etsai on 6/15/2015.
 */
public enum DevInfoCharacteristic {
    MANUFACTURER_NAME("29"),
    MODEL_NUMBER("24"),
    SERIAL_NUMBER("25"),
    FIRMWARE_VERSION("26"),
    HARDWARE_VERSION("27");

    private final UUID uuid;
    private final String key;

    DevInfoCharacteristic(String uuidPart) {
        uuid= UUID.fromString(String.format("00002a%s-0000-1000-8000-00805f9b34fb", uuidPart));
        key= this.name().toLowerCase();
    }

    public UUID uuid() {
        return uuid;
    }
    public String key() {
        return key;
    }

    public static UUID serviceUuid() {
        return UUID.fromString(String.format("0000%s-0000-1000-8000-00805f9b34fb", "180a"));
    }

    private static final HashMap<UUID, DevInfoCharacteristic> uuidLookupMap;
    static {
        uuidLookupMap= new HashMap<>();
        for(DevInfoCharacteristic it: DevInfoCharacteristic.values()) {
            uuidLookupMap.put(it.uuid(), it);
        }
    }
    public static DevInfoCharacteristic uuidToDevInfoCharacteristic(UUID uuid) {
        return uuidLookupMap.get(uuid);
    }
}
