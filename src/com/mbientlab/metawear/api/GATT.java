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
package com.mbientlab.metawear.api;

import java.util.HashMap;
import java.util.UUID;

import com.mbientlab.metawear.api.characteristic.*;

/**
 * Generic interface describing GATT related classes
 * @author Eric Tsai
 */
public interface GATT {
    /**
     * Get the UUID of the GATT object
     * @return GATT uuid
     */
    public UUID uuid();
    
    /**
     * Interface describing a GATT characteristic 
     * @author Eric Tsai
     */
    public interface GATTCharacteristic extends GATT {
        /**
         * Get the service the characteristic belongs to
         * @return Parent GATT service
         */
        public GATTService gattService();
    }
    
    /**
     * Enumeration of GATT services on the MetaWear board
     * @author Eric Tsai
     */
    public enum GATTService implements GATT {
        /**
         * BLE standard UUID for battery level service
         * @see Battery
         */
        BATTERY("180f", Battery.values()),
        /**
         * BLE standard UUID for device information service
         * @see DeviceInformation
         */
        DEVICE_INFORMATION("180a", DeviceInformation.values()),
        /**
         * MetaWear service
         * @see MetaWear
         */
        METAWEAR(UUID.fromString("326A9000-85CB-9195-D9DD-464CFBBAE75A"), MetaWear.values());
        
        private final UUID uuid;
        private final HashMap<UUID, GATTCharacteristic> characteristics;
        
        private GATTService(String uuidPart, GATTCharacteristic[] characteristics) {
            this(UUID.fromString(String.format("0000%s-0000-1000-8000-00805f9b34fb", 
                    uuidPart)), characteristics);
        }
        private GATTService(UUID uuid, GATTCharacteristic[] characteristics) {
            this.uuid= uuid;
            this.characteristics= new HashMap<>();
            
            for(GATTCharacteristic it: characteristics) {
                this.characteristics.put(it.uuid(), it);
            }
        }
        
        /**
         * Get the GATT characteristic under the service that matches the uuid
         * @param charUUID Characteristic UUID to search
         * @return GATT characteristic matching the uuid, null if no match is found
         */
        public GATTCharacteristic getCharacteristic(UUID charUUID) {
            return characteristics.get(charUUID);
        }
        private static HashMap<UUID, GATTService> services;
        static {
            services= new HashMap<>();
            for(GATTService it: values()) {
                services.put(it.uuid(), it);
            }
        }
        /**
         * Find the GATT service with the desired uuid
         * @param uuid UUID to search
         * @return GATT service matching the uuid, null if no match is found
         */
        public static GATTService lookupGATTService(UUID uuid) {
            return services.get(uuid);
        }
        @Override
        public UUID uuid() {
            return uuid;
        }
    }
}
