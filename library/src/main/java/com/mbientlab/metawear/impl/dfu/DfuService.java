/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/

/*
 * NORDIC SEMICONDUCTOR EXAMPLE CODE AND LICENSE AGREEMENT
 *
 * You are receiving this document because you have obtained example code ("Software") 
 * from Nordic Semiconductor ASA * ("Licensor"). The Software is protected by copyright 
 * laws and international treaties. All intellectual property rights related to the 
 * Software is the property of the Licensor. This document is a license agreement governing 
 * your rights and obligations regarding usage of the Software. Any variation to the terms 
 * of this Agreement shall only be valid if made in writing by the Licensor.
 * 
 * == Scope of license rights ==
 * 
 * You are hereby granted a limited, non-exclusive, perpetual right to use and modify the 
 * Software in order to create your own software. You are entitled to distribute the 
 * Software in original or modified form as part of your own software.
 *
 * If distributing your software in source code form, a copy of this license document shall 
 * follow with the distribution.
 *   
 * The Licensor can at any time terminate your rights under this license agreement.
 * 
 * == Restrictions on license rights ==
 * 
 * You are not allowed to distribute the Software on its own, without incorporating it into 
 * your own software.  
 * 
 * You are not allowed to remove, alter or destroy any proprietary, 
 * trademark or copyright markings or notices placed upon or contained with the Software.
 *     
 * You shall not use Licensor's name or trademarks without Licensor's prior consent.
 * 
 * == Disclaimer of warranties and limitation of liability ==
 * 
 * YOU EXPRESSLY ACKNOWLEDGE AND AGREE THAT USE OF THE SOFTWARE IS AT YOUR OWN RISK AND THAT THE 
 * SOFTWARE IS PROVIDED *AS IS" WITHOUT ANY WARRANTIES OR CONDITIONS WHATSOEVER. NORDIC SEMICONDUCTOR ASA 
 * DOES NOT WARRANT THAT THE FUNCTIONS OF THE SOFTWARE WILL MEET YOUR REQUIREMENTS OR THAT THE 
 * OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR FREE. YOU ASSUME RESPONSIBILITY FOR 
 * SELECTING THE SOFTWARE TO ACHIEVE YOUR INTENDED RESULTS, AND FOR THE *USE AND THE RESULTS 
 * OBTAINED FROM THE SOFTWARE.
 * 
 * NORDIC SEMICONDUCTOR ASA DISCLAIM ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO WARRANTIES RELATED TO: NON-INFRINGEMENT, LACK OF VIRUSES, ACCURACY OR COMPLETENESS OF RESPONSES 
 * OR RESULTS, IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL OR 
 * CONSEQUENTIAL DAMAGES OR FOR ANY DAMAGES WHATSOEVER (INCLUDING BUT NOT LIMITED TO DAMAGES FOR 
 * LOSS OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, PERSONAL INJURY, 
 * LOSS OF PRIVACY OR OTHER PECUNIARY OR OTHER LOSS WHATSOEVER) ARISING OUT OF USE OR INABILITY TO 
 * USE THE SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * REGARDLESS OF THE FORM OF ACTION, NORDIC SEMICONDUCTOR ASA AGGREGATE LIABILITY ARISING OUT OF 
 * OR RELATED TO THIS AGREEMENT SHALL NOT EXCEED THE TOTAL AMOUNT PAYABLE BY YOU UNDER THIS AGREEMENT. 
 * THE FOREGOING LIMITATIONS, EXCLUSIONS AND DISCLAIMERS SHALL APPLY TO THE MAXIMUM EXTENT ALLOWED BY 
 * APPLICABLE LAW.
 * 
 * == Dispute resolution and legal venue ==
 * 
 * Any and all disputes arising out of the rights and obligations in this license agreement shall be 
 * submitted to ordinary court proceedings. You accept the Oslo City Court as legal venue under this agreement.
 * 
 * This license agreement shall be governed by Norwegian law.
 * 
 * == Contact information ==
 * 
 * All requests regarding the Software or the API shall be directed to: 
 * Nordic Semiconductor ASA, P.O. Box 436, Skøyen, 0213 Oslo, Norway.
 * 
 * http://www.nordicsemi.com/eng/About-us/Contact-us
 */
package com.mbientlab.metawear.impl.dfu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.mbientlab.metawear.BuildConfig;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.impl.characteristic.DebugRegister;
import com.mbientlab.metawear.impl.characteristic.Registers;

public class DfuService implements Runnable {
    private static final String TAG = "DfuService";

    public static final int ERROR_MASK = 0x0100;
    public static final int ERROR_DEVICE_DISCONNECTED = ERROR_MASK | 0x00;
    public static final int ERROR_FILE_NOT_FOUND = ERROR_MASK | 0x01;
    public static final int ERROR_FILE_CLOSED = ERROR_MASK | 0x02;
    public static final int ERROR_FILE_INVALID = ERROR_MASK | 0x03;
    public static final int ERROR_FILE_IO_EXCEPTION = ERROR_MASK | 0x04;
    public static final int ERROR_SERVICE_DISCOVERY_NOT_STARTED = ERROR_MASK | 0x05;
    public static final int ERROR_SERVICE_NOT_FOUND = ERROR_MASK | 0x06;
    public static final int ERROR_CHARACTERISTICS_NOT_FOUND = ERROR_MASK | 0x07;
    public static final int ERROR_INVALID_RESPONSE = ERROR_MASK | 0x08;
    /** Look for DFU specification to get error codes */
    public static final int ERROR_REMOTE_MASK = 0x0200;
    public static final int ERROR_CONNECTION_MASK = 0x0400;

    public static final int PROGRESS_CONNECTING = -1;
    public static final int PROGRESS_STARTING = -2;
    public static final int PROGRESS_VALIDATING = -4;
    public static final int PROGRESS_DISCONNECTING = -5;
    public static final int PROGRESS_ABORTED = -7;

    /** Lock used in synchronization purposes */
    private final Object mLock = new Object();

    /** The number of the last error that has occurred or 0 if there was no error */
    private int mErrorState;
    /** The current connection state. If its value is > 0 than an error has occurred. Error number is a negative value of mConnectionState */
    private int mConnectionState;
    private final static int STATE_DISCONNECTED = 0;
    private final static int STATE_CONNECTING = -1;
    private final static int STATE_CONNECTED = -2;
    private final static int STATE_CONNECTED_AND_READY = -3; // indicates that services were discovered
    private final static int STATE_DISCONNECTING = -4;
    private final static int STATE_CLOSED = -5;

    /** Flag set when we got confirmation from the device that notifications are enabled. */
    private boolean mNotificationsEnabled;

    private final static int MAX_PACKET_SIZE = 20; // the maximum number of bytes in one packet is 20. May be less.
    /** The number of packets of firmware data to be send before receiving a new Packets receipt notification. 0 disables the packets notifications */
    private int mPacketsBeforeNotification = 10;

    private byte[] mBuffer = new byte[MAX_PACKET_SIZE];
    private HexInputStream mHexInputStream;
    private int mImageSizeInBytes;
    private int mBytesSent;
    private int mPacketsSendSinceNotification;
    private boolean mAborted;

    /** Flag indicating whether the image size has been already transfered or not */
    private boolean mImageSizeSent;
    /** Flag indicating whether the request was completed or not */
    private boolean mRequestCompleted;

    /** Latest data received from device using notification. */
    private byte[] mReceivedData = null;
    private static final int OP_CODE_RECEIVE_START_DFU_KEY = 0x01; // 1
    private static final int OP_CODE_RECEIVE_FIRMWARE_IMAGE_KEY = 0x03; // 3
    private static final int OP_CODE_RECEIVE_VALIDATE_KEY = 0x04; // 4
    private static final int OP_CODE_RECEIVE_ACTIVATE_AND_RESET_KEY = 0x05; // 5
    private static final int OP_CODE_RECEIVE_RESET_KEY = 0x06; // 6
    //  private static final int OP_CODE_PACKET_REPORT_RECEIVED_IMAGE_SIZE_KEY = 0x07; // 7
    private static final int OP_CODE_PACKET_RECEIPT_NOTIF_REQ_KEY = 0x08; // 8
    private static final int OP_CODE_RESPONSE_CODE_KEY = 0x10; // 16
    private static final int OP_CODE_PACKET_RECEIPT_NOTIF_KEY = 0x11; // 11
    private static final byte[] OP_CODE_START_DFU = new byte[] { OP_CODE_RECEIVE_START_DFU_KEY };
    private static final byte[] OP_CODE_RECEIVE_FIRMWARE_IMAGE = new byte[] { OP_CODE_RECEIVE_FIRMWARE_IMAGE_KEY };
    private static final byte[] OP_CODE_VALIDATE = new byte[] { OP_CODE_RECEIVE_VALIDATE_KEY };
    private static final byte[] OP_CODE_ACTIVATE_AND_RESET = new byte[] { OP_CODE_RECEIVE_ACTIVATE_AND_RESET_KEY };
    private static final byte[] OP_CODE_RESET = new byte[] { OP_CODE_RECEIVE_RESET_KEY };
    //  private static final byte[] OP_CODE_REPORT_RECEIVED_IMAGE_SIZE = new byte[] { OP_CODE_PACKET_REPORT_RECEIVED_IMAGE_SIZE_KEY };
    private static final byte[] OP_CODE_PACKET_RECEIPT_NOTIF_REQ = new byte[] { OP_CODE_PACKET_RECEIPT_NOTIF_REQ_KEY, 0x00, 0x00 };

    public static final int DFU_STATUS_SUCCESS = 1;
    public static final int DFU_STATUS_INVALID_STATE = 2;
    public static final int DFU_STATUS_NOT_SUPPORTED = 3;
    public static final int DFU_STATUS_DATA_SIZE_EXCEEDS_LIMIT = 4;
    public static final int DFU_STATUS_CRC_ERROR = 5;
    public static final int DFU_STATUS_OPERATION_FAILED = 6;

    private static final UUID DFU_CONTROL_POINT_UUID = new UUID(0x000015311212EFDEl, 0x1523785FEABCD123l);
    private static final UUID DFU_PACKET_UUID = new UUID(0x000015321212EFDEl, 0x1523785FEABCD123l);
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = new UUID(0x0000290200001000l, 0x800000805f9b34fbl);

    private BluetoothGattCallback dfuServiceCheckCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    mConnectionState = STATE_CONNECTED;
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    mConnectionState = STATE_DISCONNECTED;
                    synchronized (mLock) {
                        mLock.notifyAll();
                    }
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            final UUID METAWEAR_SERVICE = UUID.fromString("326A9000-85CB-9195-D9DD-464CFBBAE75A"),
                    METAWEAR_COMMAND = UUID.fromString("326A9001-85CB-9195-D9DD-464CFBBAE75A");

            BluetoothGattService dfuService = gatt.getService(MetaWearBoard.METABOOT_SERVICE_UUID);
            if (dfuService != null) {
                ///< Board already in MetaBoot mode
                gatt.disconnect();
            } else {
                BluetoothGattService mwBleService = gatt.getService(METAWEAR_SERVICE);
                BluetoothGattCharacteristic cmdRegister = mwBleService.getCharacteristic(METAWEAR_COMMAND);
                cmdRegister.setValue(Registers.buildWriteCommand(DebugRegister.JUMP_TO_BOOTLOADER));

                gatt.writeCharacteristic(cmdRegister);
            }
        }

    };
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // check whether an error occurred
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    logi("Connected to GATT server");
                    mConnectionState = STATE_CONNECTED;

                    // Attempts to discover services after successful connection.
                    // do not refresh the gatt device here!
                    final boolean success = gatt.discoverServices();
                    logi("Attempting to start service discovery... " + (success ? "succeed" : "failed"));

                    if (!success) {
                        mErrorState = ERROR_SERVICE_DISCOVERY_NOT_STARTED;
                    } else {
                        // just return here, lock will be notified when service discovery finishes
                        return;
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    logi("Disconnected from GATT server");
                    mConnectionState = STATE_DISCONNECTED;
                }
            } else {
                loge("Connection state change error: " + status + " newState: " + newState);
                mErrorState = ERROR_CONNECTION_MASK | status;
                mConnectionState = STATE_DISCONNECTED;
            }

            // notify waiting thread
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logi("Services discovered");
                mConnectionState = STATE_CONNECTED_AND_READY;
            } else {
                loge("Service discovery error: " + status);
                mErrorState = ERROR_CONNECTION_MASK | status;
            }

            // notify waiting thread
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (CLIENT_CHARACTERISTIC_CONFIG.equals(descriptor.getUuid())) {
                    // we have enabled or disabled characteristic
                    mNotificationsEnabled = descriptor.getValue()[0] == 1;
                }
            } else {
                loge("Descriptor write error: " + status);
                mErrorState = ERROR_CONNECTION_MASK | status;
            }

            // notify waiting thread
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                /*
                 * This method is called when either a CONTROL POINT or PACKET characteristic has been written.
                 * If it is the CONTROL POINT characteristic, just set the flag to true.
                 * If the PACKET characteristic was written we must:
                 *  - if the image size was written in DFU Start procedure, just set flag to true
                 *  - else
                 *      - send the next packet, if notification is not required at that moment
                 *      - do nothing, because we have to wait for the notification to confirm the data received
                 */
                if (DFU_PACKET_UUID.equals(characteristic.getUuid())) {
                    if (mImageSizeSent) {
                        // if the PACKET characteristic was written with image data, update counters
                        mBytesSent += characteristic.getValue().length;
                        mPacketsSendSinceNotification++;

                        // if a packet receipt notification is expected, or the last packet was sent, do nothing. There onCharacteristicChanged listener will catch either
                        // a packet confirmation (if there are more bytes to send) or the image received notification (it upload process was completed)
                        final boolean notificationExpected = mPacketsBeforeNotification > 0 && mPacketsSendSinceNotification == mPacketsBeforeNotification;
                        final boolean lastPacketTransfered = mBytesSent == mImageSizeInBytes;

                        if (notificationExpected || lastPacketTransfered)
                            return;

                        // when neither of them is true, send the next packet
                        try {
                            if (mAborted) {
                                // notify waiting thread
                                synchronized (mLock) {
                                    mLock.notifyAll();
                                    return;
                                }
                            }

                            final byte[] buffer = mBuffer;
                            final int size = mHexInputStream.readPacket(buffer);
                            writePacket(gatt, characteristic, buffer, size);
                            updateProgressNotification();
                            return;
                        } catch (final HexFileValidationException e) {
                            loge("Invalid HEX file");
                            mErrorState = ERROR_FILE_INVALID;
                        } catch (final IOException e) {
                            loge("Error while reading the input stream", e);
                            mErrorState = ERROR_FILE_IO_EXCEPTION;
                        }
                    } else {
                        // we've got confirmation that the image size was sent
                        mImageSizeSent = true;
                    }
                } else {
                    // if the CONTROL POINT characteristic was written just set the flag to true
                    mRequestCompleted = true;
                }
            } else {
                loge("Characteristic write error: " + status);
                mErrorState = ERROR_CONNECTION_MASK | status;
            }

            // notify waiting thread
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            final int responseType = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

            switch (responseType) {
            case OP_CODE_PACKET_RECEIPT_NOTIF_KEY:
                final BluetoothGattCharacteristic packetCharacteristic = gatt.getService(MetaWearBoard.METABOOT_SERVICE_UUID)
                        .getCharacteristic(DFU_PACKET_UUID);

                try {
                    mPacketsSendSinceNotification = 0;

                    if (mAborted)
                        break;

                    final byte[] buffer = mBuffer;
                    final int size = mHexInputStream.readPacket(buffer);
                    writePacket(gatt, packetCharacteristic, buffer, size);
                    updateProgressNotification();
                    return;
                } catch (final HexFileValidationException e) {
                    loge("Invalid HEX file");
                    mErrorState = ERROR_FILE_INVALID;
                } catch (final IOException e) {
                    loge("Error while reading the input stream", e);
                    mErrorState = ERROR_FILE_IO_EXCEPTION;
                }
                break;
            default:
                mReceivedData = characteristic.getValue();
                break;
            }

            // notify waiting thread
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }
    };

    private File firmwareHexPath;
    private URL firmwareUrl;

    private final BluetoothDevice btDevice;
    private final Context ctx;
    private final boolean inMetaBootMode;
    private final MetaWearBoard.DfuProgressHandler progressHandler;

    public DfuService(BluetoothDevice btDevice, File firmwarePath, Context ctx, boolean inMetaBootMode, MetaWearBoard.DfuProgressHandler progressHandler) {
        this(btDevice, ctx, inMetaBootMode, progressHandler);
        this.firmwareUrl= null;
        this.firmwareHexPath= firmwarePath;
    }

    public DfuService(BluetoothDevice btDevice, URL firmwareUrl, Context ctx, boolean inMetaBootMode, MetaWearBoard.DfuProgressHandler progressHandler) {
        this(btDevice, ctx, inMetaBootMode, progressHandler);
        this.firmwareUrl= firmwareUrl;
        this.firmwareHexPath= null;
    }

    private DfuService(BluetoothDevice btDevice, Context ctx, boolean inMetaBootMode, MetaWearBoard.DfuProgressHandler progressHandler) {
        this.btDevice= btDevice;
        this.ctx= ctx;
        this.inMetaBootMode= inMetaBootMode;

        if (progressHandler == null) {
            this.progressHandler= new MetaWearBoard.DfuProgressHandler() {
                @Override
                public void reachedCheckpoint(State dfuState) {

                }

                @Override
                public void receivedUploadProgress(int progress) {

                }
            };
        } else {
            this.progressHandler = progressHandler;
        }
    }

    public void abort() {
        mAborted= true;

        synchronized (mLock) {
            mLock.notifyAll();
        }
    }

    @Override
    public void run() {
        mConnectionState = STATE_DISCONNECTED;

        // read preferences
        mPacketsBeforeNotification = 10;

        HexInputStream his = null;
        try {
            // Prepare data to send, calculate stream size
            try {

                if (firmwareUrl != null) {
                    his = openInputStream(downloadFirmware());
                } else {
                    his= openInputStream(firmwareHexPath);
                }

                mImageSizeInBytes = his.sizeInBytes();
                mHexInputStream = his;
            } catch (final FileNotFoundException e) {
                throw new RuntimeException("An exception occurred while opening file", e);
            } catch (final IOException e) {
                throw new RuntimeException("An exception occurred while calculating file size", e);
            } catch (UploadAbortedException e) {
                throw new RuntimeException("Upload aborted", e);
            }

            // Let's connect to the device
            updateProgressNotification(PROGRESS_CONNECTING);

            final BluetoothGatt gatt = connect();
            // Are we connected?
            if (mErrorState > 0) { // error occurred
                final int error = mErrorState & ~ERROR_CONNECTION_MASK;

                terminateConnection(gatt, mErrorState);
                throw new RuntimeException("An error occurred while connecting to the device:" + error);
            }
            if (mAborted) {
                terminateConnection(gatt, PROGRESS_ABORTED);
                throw new RuntimeException("Upload aborted", new UploadAbortedException());
            }

            // We have connected to DFU device and services are discoverer
            final BluetoothGattService dfuService = gatt.getService(MetaWearBoard.METABOOT_SERVICE_UUID); // there was a case when the service was null. I don't know why
            if (dfuService == null) {
                terminateConnection(gatt, ERROR_SERVICE_NOT_FOUND);
                throw new RuntimeException("DFU service does not exists on the device");
            }
            final BluetoothGattCharacteristic controlPointCharacteristic = dfuService.getCharacteristic(DFU_CONTROL_POINT_UUID);
            final BluetoothGattCharacteristic packetCharacteristic = dfuService.getCharacteristic(DFU_PACKET_UUID);
            if (controlPointCharacteristic == null || packetCharacteristic == null) {
                terminateConnection(gatt, ERROR_CHARACTERISTICS_NOT_FOUND);
                throw new RuntimeException("DFU characteristics not found in the DFU service");
            }

            try {
                // enable notifications
                updateProgressNotification(PROGRESS_STARTING);
                setCharacteristicNotification(gatt, controlPointCharacteristic, true);

                try {
                    // set up the temporary variable that will hold the responses
                    byte[] response;

                    // send Start DFU command to Control Point
                    logi("Sending Start DFU command (Op Code = 1)");
                    writeOpCode(gatt, controlPointCharacteristic, OP_CODE_START_DFU);

                    // send image size in bytes to DFU Packet
                    logi("Sending image size in bytes to DFU Packet");
                    writeImageSize(gatt, packetCharacteristic, mImageSizeInBytes);

                    // a notification will come with confirmation. Let's wait for it a bit
                    response = readNotificationResponse();

                    /*
                     * The response received from the DFU device contains:
                     * +---------+--------+----------------------------------------------------+
                     * | byte no |  value |                  description                       |
                     * +---------+--------+----------------------------------------------------+
                     * |       0 |     16 | Response code                                      |
                     * |       1 |      1 | The Op Code of a request that this response is for |
                     * |       2 | STATUS | See DFU_STATUS_* for status codes                  |
                     * +---------+--------+----------------------------------------------------+
                     */
                    int status = getStatusCode(response, OP_CODE_RECEIVE_START_DFU_KEY);
                    if (status != DFU_STATUS_SUCCESS)
                        throw new RemoteDfuException("Starting DFU failed", status);

                    // Send the number of packets of firmware before receiving a receipt notification
                    final int numberOfPacketsBeforeNotification = mPacketsBeforeNotification;
                    if (numberOfPacketsBeforeNotification > 0) {
                        logi("Sending the number of packets before notifications (Op Code = 8)");
                        setNumberOfPackets(OP_CODE_PACKET_RECEIPT_NOTIF_REQ, numberOfPacketsBeforeNotification);
                        writeOpCode(gatt, controlPointCharacteristic, OP_CODE_PACKET_RECEIPT_NOTIF_REQ);
                    }

                    // Initialize firmware upload
                    logi("Sending Receive Firmware Image request (Op Code = 3)");
                    writeOpCode(gatt, controlPointCharacteristic, OP_CODE_RECEIVE_FIRMWARE_IMAGE);

                    // This allow us to calculate upload time
                    final long startTime = System.currentTimeMillis();
                    updateProgressNotification();
                    try {
                        response = uploadFirmwareImage(gatt, packetCharacteristic, his);
                    } catch (final DeviceDisconnectedException e) {
                        loge("Disconnected while sending data");
                        throw e;
                        // TODO reconnect?
                    }

                    final long endTime = System.currentTimeMillis();
                    logi("Transfer of " + mBytesSent + " bytes has taken " + (endTime - startTime) + " ms");

                    // Check the result of the operation
                    status = getStatusCode(response, OP_CODE_RECEIVE_FIRMWARE_IMAGE_KEY);
                    logi("Response received. Op Code: " + response[0] + " Req Op Code: " + response[1] + " status: " + response[2]);
                    if (status != DFU_STATUS_SUCCESS)
                        throw new RemoteDfuException("Device returned error after sending file", status);

                    updateProgressNotification(PROGRESS_VALIDATING);
                    // Send Validate request
                    logi("Sending Validate request (Op Code = 4)");
                    writeOpCode(gatt, controlPointCharacteristic, OP_CODE_VALIDATE);

                    // A notification will come with status code. Let's wait for it a bit.
                    response = readNotificationResponse();
                    status = getStatusCode(response, OP_CODE_RECEIVE_VALIDATE_KEY);
                    if (status != DFU_STATUS_SUCCESS)
                        throw new RemoteDfuException("Device returned validation error", status);

                    // Disable notifications locally (we don't need to disable them on the device, it will reset)
                    updateProgressNotification(PROGRESS_DISCONNECTING);
                    gatt.setCharacteristicNotification(controlPointCharacteristic, false);

                    // Send Activate and Reset signal.
                    logi("Sending Activate and Reset request (Op Code = 5)");
                    writeOpCode(gatt, controlPointCharacteristic, OP_CODE_ACTIVATE_AND_RESET);

                    // The device will reset so we don't have to send Disconnect signal.
                    waitUntilDisconnected();

                    // Close the device
                    refreshDeviceCache(gatt);
                    close(gatt);
                } catch (final UnknownResponseException e) {
                    final int error = ERROR_INVALID_RESPONSE;

                    // This causes GATT_ERROR 0x85 on Nexus 4 (4.4.2)
                    //                  logi(("Sending Reset command (Op Code = 6)");
                    //                  writeOpCode(gatt, controlPointCharacteristic, OP_CODE_RESET);
                    //                  sendLogBroadcast(Level.INFO, "Reset request sent");
                    terminateConnection(gatt, error);
                    throw new RuntimeException("Error uploading firmware", e);
                } catch (final RemoteDfuException e) {
                    final int error = ERROR_REMOTE_MASK | e.getErrorNumber();

                    // This causes GATT_ERROR 0x85 on Nexus 4 (4.4.2)
                    //                  logi(("Sending Reset command (Op Code = 6)");
                    //                  writeOpCode(gatt, controlPointCharacteristic, OP_CODE_RESET);
                    //                  sendLogBroadcast(Level.INFO, "Reset request sent");
                    terminateConnection(gatt, error);
                    throw new RuntimeException("Error uploading firmware", e);
                }
            } catch (final UploadAbortedException e) {
                if (mConnectionState == STATE_CONNECTED_AND_READY) {
                    try {
                        mAborted = false;
                        logi("Sending Reset command (Op Code = 6)");
                        writeOpCode(gatt, controlPointCharacteristic, OP_CODE_RESET);
                    } catch (final Exception e1) {
                        // do nothing
                    }
                }
                terminateConnection(gatt, PROGRESS_ABORTED);
                throw new RuntimeException("Upload aborted", e);
            } catch (final DeviceDisconnectedException e) {
                // TODO reconnect n times?
                if (mNotificationsEnabled) {
                    gatt.setCharacteristicNotification(controlPointCharacteristic, false);
                }
                close(gatt);
                updateProgressNotification(ERROR_DEVICE_DISCONNECTED);
                throw new RuntimeException("Connection lost", e);
            } catch (final DfuException e) {
                if (mConnectionState == STATE_CONNECTED_AND_READY) {
                    try {
                        logi("Sending Reset command (Op Code = 6)");
                        writeOpCode(gatt, controlPointCharacteristic, OP_CODE_RESET);
                    } catch (final Exception e1) {
                        // do nothing
                    }
                }
                terminateConnection(gatt, e.getErrorNumber());
                throw new RuntimeException("Error uploading firmware", e);
            }
        } finally {
            try {
                // ensure that input stream is always closed
                mHexInputStream = null;
                if (his != null)
                    his.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    private File downloadFirmware() throws UploadAbortedException {
        HttpURLConnection urlConn = null;
        progressHandler.reachedCheckpoint(MetaWearBoard.DfuProgressHandler.State.DOWNLOADING);

        try {
            urlConn = (HttpURLConnection) firmwareUrl.openConnection();

            File fileDir = ctx.getFilesDir();
            File firmwareHex = new File(fileDir, "firmware.hex");

            FileOutputStream fos = new FileOutputStream(firmwareHex);
            InputStream ins = urlConn.getInputStream();

            byte data[] = new byte[1024];
            int count;

            while (!mAborted && (count = ins.read(data)) != -1) {
                fos.write(data, 0, count);
            }

            if (mAborted) {
                throw new UploadAbortedException();
            }

            return firmwareHex;
        } catch (IOException e) {
            throw new RuntimeException("Error downloading firmware", e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }

    /**
     * Sets number of data packets that will be send before the notification will be received
     *
     * @param data
     *            control point data packet
     * @param value
     *            number of packets before receiving notification. If this value is 0, then the notification of packet receipt will be disabled by the DFU target.
     */
    private void setNumberOfPackets(final byte[] data, final int value) {
        data[1] = (byte) (value & 0xFF);
        data[2] = (byte) ((value >> 8) & 0xFF);
    }

    /**
     * Opens the binary input stream from a HEX file. A Path to the HEX file is given
     *
     * @param filePath
     *            the path to the HEX file
     * @return the binary input stream with Intel HEX data
     * @throws FileNotFoundException
     */
    private HexInputStream openInputStream(final File filePath) throws IOException {
        final InputStream is = new FileInputStream(filePath);
        return new HexInputStream(is);
    }

    /**
     * Connects to the BLE device with given address. This method is SYNCHRONOUS, it wait until the connection status change from {@link #STATE_CONNECTING} to {@link #STATE_CONNECTED_AND_READY} or an
     * error occurs.
     *
     * @return the GATT device
     */
    private BluetoothGatt connect() {
        logi("Connecting to the device...");

        BluetoothGatt gatt;

        if (!inMetaBootMode) {
            mConnectionState = STATE_CONNECTING;
            gatt = btDevice.connectGatt(ctx, false, dfuServiceCheckCallback);

            // We have to wait until the device is connected and services are discovered
            // Connection error may occur as well.
            try {
                synchronized (mLock) {
                    while (((mConnectionState == STATE_CONNECTING || mConnectionState == STATE_CONNECTED) && mErrorState == 0 && !mAborted))
                        mLock.wait();
                }
            } catch (final InterruptedException e) {
                loge("Sleeping interrupted", e);
            }

            if (mAborted) {
                return gatt;
            }

            refreshDeviceCache(gatt);
            close(gatt);
        }

        mConnectionState = STATE_CONNECTING;
		gatt = btDevice.connectGatt(ctx, false, mGattCallback);
        try {
            synchronized (mLock) {
                while (((mConnectionState == STATE_CONNECTING || mConnectionState == STATE_CONNECTED) && mErrorState == 0 && !mAborted))
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            loge("Sleeping interrupted", e);
        }
        return gatt;
    }

    /**
     * Disconnects from the device and cleans local variables in case of error. This method is SYNCHRONOUS and wait until the disconnecting process will be completed.
     *
     * @param gatt
     *            the GATT device to be disconnected
     * @param error
     *            error number
     */
    private void terminateConnection(final BluetoothGatt gatt, final int error) {
        if (mConnectionState != STATE_DISCONNECTED) {
            updateProgressNotification(PROGRESS_DISCONNECTING);

            // disable notifications
            try {
                final BluetoothGattService dfuService = gatt.getService(MetaWearBoard.METABOOT_SERVICE_UUID);
                if (dfuService != null) {
                    final BluetoothGattCharacteristic controlPointCharacteristic = dfuService.getCharacteristic(DFU_CONTROL_POINT_UUID);
                    setCharacteristicNotification(gatt, controlPointCharacteristic, false);
                }
            } catch (final DeviceDisconnectedException e) {
                // do nothing
            } catch (final DfuException e) {
                // do nothing
            } catch (final Exception e) {
                // do nothing
            }

            // Disconnect from the device
            disconnect(gatt);
        }

        // Close the device
        refreshDeviceCache(gatt);
        close(gatt);
        updateProgressNotification(error);
    }

    /**
     * Disconnects from the device. This is SYNCHRONOUS method and waits until the callback returns new state. Terminates immediately if device is already disconnected. Do not call this method
     * directly, use {@link #terminateConnection(BluetoothGatt, int)} instead.
     *
     * @param gatt
     *            the GATT device that has to be disconnected
     */
    private void disconnect(final BluetoothGatt gatt) {
        if (mConnectionState == STATE_DISCONNECTED || mConnectionState != STATE_CONNECTED && mConnectionState != STATE_CONNECTED_AND_READY)
            return;

        mConnectionState = STATE_DISCONNECTING;

        gatt.disconnect();

        // We have to wait until device gets disconnected or an error occur
        waitUntilDisconnected();
    }

    /**
     * Wait until the connection state will change to {@link #STATE_DISCONNECTED} or until an error occurs.
     */
    private void waitUntilDisconnected() {
        try {
            synchronized (mLock) {
                while (mConnectionState != STATE_DISCONNECTED && mErrorState == 0)
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            loge("Sleeping interrupted", e);
        }
    }

    /**
     * Closes the GATT device and cleans up.
     *
     * @param gatt
     *            the GATT device to be closed
     */
    private void close(final BluetoothGatt gatt) {
        logi("Cleaning up...");
        gatt.close();
        mConnectionState = STATE_CLOSED;
    }

    /**
     * Clears the device cache. After uploading new firmware the DFU target will have other services than before.
     *
     * @param gatt
     *            the GATT device to be refreshed
     */
    private void refreshDeviceCache(final BluetoothGatt gatt) {
        /*
         * There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
         */
        try {
            final Method refresh = gatt.getClass().getMethod("refresh");
            if (refresh != null) {
                final boolean success = (Boolean) refresh.invoke(gatt);
                logi("Refreshing result: " + success);
            }
        } catch (Exception e) {
            loge("An exception occured while refreshing device", e);
        }
    }

    /**
     * Checks whether the response received is valid and returns the status code.
     *
     * @param response
     *            the response received from the DFU device.
     * @param request
     *            the expected Op Code
     * @return the status code
     * @throws UnknownResponseException
     *             if response was not valid
     */
    private int getStatusCode(final byte[] response, final int request) throws UnknownResponseException {
        if (response == null || response.length != 3 || response[0] != OP_CODE_RESPONSE_CODE_KEY || response[1] != request || response[2] < 1 || response[2] > 6)
            throw new UnknownResponseException("Invalid response received", response, request);
        return response[2];
    }

    /**
     * Enables or disables the notifications for given characteristic. This method is SYNCHRONOUS and wait until the
     * {@link BluetoothGattCallback#onDescriptorWrite(BluetoothGatt, BluetoothGattDescriptor, int)} will be called or the connection state will change from {@link #STATE_CONNECTED_AND_READY}. If
     * connection state will change, or an error will occur, an exception will be thrown.
     *
     * @param gatt
     *            the GATT device
     * @param characteristic
     *            the characteristic to enable or disable notifications for
     * @param enable
     *            <code>true</code> to enable notifications, <code>false</code> to disable them
     * @throws DfuException
     * @throws UploadAbortedException
     */
    private void setCharacteristicNotification(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final boolean enable) throws DeviceDisconnectedException, DfuException,
            UploadAbortedException {
        if (mConnectionState != STATE_CONNECTED_AND_READY)
            throw new DeviceDisconnectedException("Unable to set notifications state", mConnectionState);
        mErrorState = 0;

        if (mNotificationsEnabled == enable)
            return;

        logi((enable ? "Enabling " : "Disabling") + " notifications...");

        // enable notifications locally
        gatt.setCharacteristicNotification(characteristic, enable);

        // enable notifications on the device
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);

        // We have to wait until device gets disconnected or an error occur
        try {
            synchronized (mLock) {
                while ((mNotificationsEnabled != enable && mConnectionState == STATE_CONNECTED_AND_READY && mErrorState == 0 && !mAborted))
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            loge("Sleeping interrupted", e);
        }
        if (mAborted)
            throw new UploadAbortedException();
        if (mErrorState != 0)
            throw new DfuException("Unable to set notifications state", mErrorState);
        if (mConnectionState != STATE_CONNECTED_AND_READY)
            throw new DeviceDisconnectedException("Unable to set notifications state", mConnectionState);
    }

    /**
     * Writes the operation code to the characteristic. This method is SYNCHRONOUS and wait until the
     * {@link BluetoothGattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)} will be called or the connection state will change from {@link #STATE_CONNECTED_AND_READY}.
     * If connection state will change, or an error will occur, an exception will be thrown.
     *
     * @param gatt
     *            the GATT device
     * @param characteristic
     *            the characteristic to write to. Should be the DFU CONTROL POINT
     * @param value
     *            the value to write to the characteristic
     * @throws DeviceDisconnectedException
     * @throws DfuException
     * @throws UploadAbortedException
     */
    private void writeOpCode(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] value) throws DeviceDisconnectedException, DfuException, UploadAbortedException {
        mReceivedData = null;
        mErrorState = 0;
        mRequestCompleted = false;

        characteristic.setValue(value);
        gatt.writeCharacteristic(characteristic);

        // We have to wait for confirmation
        try {
            synchronized (mLock) {
                while ((!mRequestCompleted && mConnectionState == STATE_CONNECTED_AND_READY && mErrorState == 0 && !mAborted))
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            loge("Sleeping interrupted", e);
        }
        if (mAborted)
            throw new UploadAbortedException();
        if (mErrorState != 0)
            throw new DfuException("Unable to write Op Code " + value[0], mErrorState);
        if (mConnectionState != STATE_CONNECTED_AND_READY)
            throw new DeviceDisconnectedException("Unable to write Op Code " + value[0], mConnectionState);
    }

    /**
     * Writes the image size to the characteristic. This method is SYNCHRONOUS and wait until the {@link BluetoothGattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)}
     * will be called or the connection state will change from {@link #STATE_CONNECTED_AND_READY}. If connection state will change, or an error will occur, an exception will be thrown.
     *
     * @param gatt
     *            the GATT device
     * @param characteristic
     *            the characteristic to write to. Should be the DFU PACKET
     * @param imageSize
     *            the image size in bytes
     * @throws DeviceDisconnectedException
     * @throws DfuException
     * @throws UploadAbortedException
     */
    private void writeImageSize(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int imageSize) throws DeviceDisconnectedException, DfuException,
            UploadAbortedException {
        mReceivedData = null;
        mErrorState = 0;
        mImageSizeSent = false;

        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        characteristic.setValue(imageSize, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        gatt.writeCharacteristic(characteristic);

        // We have to wait for confirmation
        try {
            synchronized (mLock) {
                while ((!mImageSizeSent && mConnectionState == STATE_CONNECTED_AND_READY && mErrorState == 0 && !mAborted))
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            loge("Sleeping interrupted", e);
        }
        if (mAborted)
            throw new UploadAbortedException();
        if (mErrorState != 0)
            throw new DfuException("Unable to write Image Size", mErrorState);
        if (mConnectionState != STATE_CONNECTED_AND_READY)
            throw new DeviceDisconnectedException("Unable to write Image Size", mConnectionState);
    }

    /**
     * Starts sending the data. This method is SYNCHRONOUS and terminates when the whole file will be uploaded or the connection status will change from {@link #STATE_CONNECTED_AND_READY}. If
     * connection state will change, or an error will occur, an exception will be thrown.
     *
     * @param gatt
     *            the GATT device (DFU target)
     * @param packetCharacteristic
     *            the characteristic to write file content to. Must be the DFU PACKET
     * @return The response value received from notification with Op Code = 3 when all bytes will be uploaded successfully.
     * @throws DeviceDisconnectedException
     *             Thrown when the device will disconnect in the middle of the transmission. The error core will be saved in {@link #mConnectionState}.
     * @throws DfuException
     *             Thrown if DFU error occur
     * @throws UploadAbortedException
     */
    private byte[] uploadFirmwareImage(final BluetoothGatt gatt, final BluetoothGattCharacteristic packetCharacteristic, final HexInputStream inputStream) throws DeviceDisconnectedException,
            DfuException, UploadAbortedException {
        mReceivedData = null;
        mErrorState = 0;

        final byte[] buffer = mBuffer;
        try {
            final int size = inputStream.readPacket(buffer);
            writePacket(gatt, packetCharacteristic, buffer, size);
        } catch (final HexFileValidationException e) {
            throw new DfuException("HEX file not valid", ERROR_FILE_INVALID);
        } catch (final IOException e) {
            throw new DfuException("Error while reading file", ERROR_FILE_IO_EXCEPTION);
        }

        try {
            synchronized (mLock) {
                while ((mReceivedData == null && mConnectionState == STATE_CONNECTED_AND_READY && mErrorState == 0 && !mAborted))
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            loge("Sleeping interrupted", e);
        }
        if (mAborted)
            throw new UploadAbortedException();
        if (mErrorState != 0)
            throw new DfuException("Uploading Firmware Image failed", mErrorState);
        if (mConnectionState != STATE_CONNECTED_AND_READY)
            throw new DeviceDisconnectedException("Uploading Firmware Image failed: device disconnected", mConnectionState);

        return mReceivedData;
    }

    /**
     * Writes the buffer to the characteristic. The maximum size of the buffer is 20 bytes. This method is ASYNCHRONOUS and returns immediately after adding the data to TX queue.
     *
     * @param gatt
     *            the GATT device
     * @param characteristic
     *            the characteristic to write to. Should be the DFU PACKET
     * @param buffer
     *            the buffer with 1-20 bytes
     * @param size
     *            the number of bytes from the buffer to send
     */
    private void writePacket(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] buffer, final int size) {
        byte[] locBuffer = buffer;
        if (buffer.length != size) {
            locBuffer = new byte[size];
            System.arraycopy(buffer, 0, locBuffer, 0, size);
        }
        characteristic.setValue(locBuffer);
        gatt.writeCharacteristic(characteristic);
        // FIXME BLE buffer overflow
        // after writing to the device with WRITE_NO_RESPONSE property the onCharacteristicWrite callback is received immediately after writing data to a buffer.
        // The real sending is much slower than adding to the buffer. This method does not return false if writing didn't succeed.. just the callback is not invoked.
        //
        // More info: this works fine on Nexus 5 (Andorid 4.4) (4.3 seconds) and on Samsung S4 (Android 4.3) (20 seconds) so this is a driver issue.
        // Nexus 4 and 7 uses Qualcomm chip, Nexus 5 and Samsung uses Broadcom chips.
    }

    /**
     * Waits until the notification will arrive. Returns the data returned by the notification. This method will block the thread if response is not ready or connection state will change from
     * {@link #STATE_CONNECTED_AND_READY}. If connection state will change, or an error will occur, an exception will be thrown.
     *
     * @return the value returned by the Control Point notification
     * @throws DeviceDisconnectedException
     * @throws DfuException
     * @throws UploadAbortedException
     */
    private byte[] readNotificationResponse() throws DeviceDisconnectedException, DfuException, UploadAbortedException {
        mErrorState = 0;
        try {
            synchronized (mLock) {
                while ((mReceivedData == null && mConnectionState == STATE_CONNECTED_AND_READY && mErrorState == 0 && !mAborted))
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            loge("Sleeping interrupted", e);
        }
        if (mAborted)
            throw new UploadAbortedException();
        if (mErrorState != 0)
            throw new DfuException("Unable to write Op Code", mErrorState);
        if (mConnectionState != STATE_CONNECTED_AND_READY)
            throw new DeviceDisconnectedException("Unable to write Op Code", mConnectionState);
        return mReceivedData;
    }

    /** Stores the last progress percent. Used to lower number of calls of {@link #updateProgressNotification(int)}. */
    private int mLastProgress = -1;

    /**
     * Creates or updates the notification in the Notification Manager. Sends broadcast with current progress to the activity.
     */
    private void updateProgressNotification() {
        final int progress = (int) (100.0f * mBytesSent / mImageSizeInBytes);
        if (mLastProgress == progress)
            return;

        mLastProgress = progress;
        updateProgressNotification(progress);
    }

    /**
     * Creates or updates the notification in the Notification Manager. Sends broadcast with given progress or error state to the activity.
     *
     * @param progress
     *            the current progress state or an error number, can be one of {@link #PROGRESS_CONNECTING}, {@link #PROGRESS_STARTING}, {@link #PROGRESS_VALIDATING},
     *            {@link #PROGRESS_DISCONNECTING}, {@link #ERROR_FILE_CLOSED}, {@link #ERROR_FILE_INVALID} , etc
     */
    private void updateProgressNotification(final int progress) {
        switch (progress) {
        case PROGRESS_CONNECTING:
            progressHandler.reachedCheckpoint(MetaWearBoard.DfuProgressHandler.State.INITIALIZING);
            break;
        case PROGRESS_STARTING:
            progressHandler.reachedCheckpoint(MetaWearBoard.DfuProgressHandler.State.STARTING);
            break;
        case PROGRESS_VALIDATING:
            progressHandler.reachedCheckpoint(MetaWearBoard.DfuProgressHandler.State.VALIDATING);
            break;
        case PROGRESS_DISCONNECTING:
            progressHandler.reachedCheckpoint(MetaWearBoard.DfuProgressHandler.State.DISCONNECTING);
            break;
        default:
            // if gte, progress is an error number
            if (progress < ERROR_MASK) {
                progressHandler.receivedUploadProgress(progress);
            }
        }
    }

    private void loge(final String message) {
        if (BuildConfig.DEBUG)
            Log.e(TAG, message);
    }

    private void loge(final String message, final Throwable e) {
        if (BuildConfig.DEBUG)
            Log.e(TAG, message, e);
    }

    private void logi(final String message) {
        if (BuildConfig.DEBUG)
            Log.i(TAG, message);
    }
}
