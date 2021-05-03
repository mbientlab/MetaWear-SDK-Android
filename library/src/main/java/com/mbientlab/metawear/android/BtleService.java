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

package com.mbientlab.metawear.android;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import com.mbientlab.metawear.BuildConfig;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.impl.JseMetaWearBoard;
import com.mbientlab.metawear.impl.platform.BtleGatt;
import com.mbientlab.metawear.impl.platform.BtleGattCharacteristic;
import com.mbientlab.metawear.impl.platform.IO;
import com.mbientlab.metawear.impl.platform.TimedTask;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import bolts.Capture;
import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * Created by etsai on 10/9/16.
 */
public class BtleService extends Service {
    private static final UUID CHARACTERISTIC_CONFIG= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final String DOWNLOAD_DIR_NAME = "download";
    private static final String LOG_TAG = "metawear-btle";

    private class GattOp {
        final String msg;
        final AndroidPlatform owner;
        final Runnable task;
        final TaskCompletionSource<byte[]> taskSource;

        private GattOp(String msg, AndroidPlatform owner, Runnable task) {
            this.msg = msg;
            this.owner = owner;
            this.task = task;
            taskSource = new TaskCompletionSource<>();
        }
    }
    private final TimedTask<byte[]> gattOpTask = new TimedTask<>();
    private final Queue<GattOp> pendingGattOps = new ConcurrentLinkedQueue<>();
    private Task<byte[]> addGattOperation(AndroidPlatform owner, String msg, Runnable task) {
        owner.nGattOps.incrementAndGet();

        GattOp newGattOp = new GattOp(msg, owner, task);
        pendingGattOps.add(newGattOp);
        executeGattOperation(false);

        return newGattOp.taskSource.getTask();
    }
    private void executeGattOperation(boolean ready) {
        if (!pendingGattOps.isEmpty() && (pendingGattOps.size() == 1 || ready)) {
            GattOp next = pendingGattOps.peek();
            gattOpTask.execute(next.msg, 1000, next.task).continueWith(task -> {
                if (task.isFaulted()) {
                    next.taskSource.setError(task.getError());
                } else if (task.isCancelled()) {
                    next.taskSource.setCancelled();
                } else {
                    next.taskSource.setResult(task.getResult());
                }

                pendingGattOps.poll();
                next.owner.gattTaskCompleted();

                executeGattOperation(true);

                return null;
            });
        }
    }

    private final Map<BluetoothDevice, AndroidPlatform> btleDevices = new HashMap<>();
    private final BluetoothGattCallback btleGattCallback= new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            final AndroidPlatform platform = btleDevices.get(gatt.getDevice());

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    if (status != 0) {
                        platform.closeGatt();
                        platform.connectTask.setError(new IllegalStateException(String.format(Locale.US, "Non-zero onConnectionStateChange status (%s)", status)));
                    } else {
                        Task.delay(1000).continueWith(ignored -> gatt.discoverServices());
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    platform.disconnected(status);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            final AndroidPlatform platform = btleDevices.get(gatt.getDevice());
            if (status != 0) {
                platform.closeGatt();
                platform.connectTask.setError(new IllegalStateException(String.format(Locale.US, "Non-zero onServicesDiscovered status (%d)", status)));
            } else {
                platform.connectTask.setResult(null);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != 0) {
                gattOpTask.setError(new IllegalStateException(String.format(Locale.US, "Non-zero onCharacteristicRead status (%d)", status)));
            } else {
                gattOpTask.setResult(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != 0) {
                gattOpTask.setError(new IllegalStateException(String.format(Locale.US, "Non-zero onCharacteristicWrite status (%d)", status)));
            } else {
                gattOpTask.setResult(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            btleDevices.get(gatt.getDevice()).notificationListener.onChange(characteristic.getValue());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status != 0) {
                gattOpTask.setError(new IllegalStateException(String.format(Locale.US, "Non-zero onDescriptorWrite status (%d)", status)));
            } else {
                gattOpTask.setResult(null);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status != 0) {
                gattOpTask.setError(new IllegalStateException(String.format(Locale.US, "Non-zero onReadRemoteRssi status (%d)", status)));
            } else {
                gattOpTask.setResult(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(rssi).array());
            }
        }
    };

    private class AndroidPlatform implements IO, BtleGatt {
        private final AtomicBoolean readyToClose = new AtomicBoolean();
        private final AtomicInteger nGattOps = new AtomicInteger();

        private final TimedTask<Void> connectTask = new TimedTask<>();
        private TaskCompletionSource<Void> disconnectTaskSrc = null;
        BluetoothGatt androidBtGatt;

        private DisconnectHandler dcHandler;
        private NotificationListener notificationListener;
        private final BluetoothDevice btDevice;
        private final MetaWearBoard board;

        AndroidPlatform(BluetoothDevice btDevice) {
            this.btDevice = btDevice;
            board = new JseMetaWearBoard(this, this, btDevice.getAddress(), BuildConfig.VERSION_NAME);
        }

        void disconnected(int status) {
            closeGatt();

            if (!connectTask.isCompleted() && status != 0) {
                connectTask.setError(new IllegalStateException(String.format(Locale.US, "Non-zero onConnectionStateChange status (%s)", status)));
            } else {
                if (disconnectTaskSrc == null || disconnectTaskSrc.getTask().isCompleted()) {
                    dcHandler.onUnexpectedDisconnect(status);
                } else {
                    dcHandler.onDisconnect();
                    disconnectTaskSrc.setResult(null);
                }
            }
        }

        void gattTaskCompleted() {
            int count = nGattOps.decrementAndGet();
            if (count == 0 && readyToClose.get()) {
                Task.delay(1000).continueWith(ignored -> {
                    if (androidBtGatt != null) {
                        androidBtGatt.disconnect();
                    }
                    return null;
                });
            }
        }

        boolean refresh()  {
            try {
                androidBtGatt.getClass().getMethod("refresh").invoke(androidBtGatt);
                return true;
            } catch (Exception e) {
                Log.w(LOG_TAG, "Error refreshing gatt cache", e);
                return false;
            }
        }

        void closeGatt() {
            readyToClose.set(false);
            if (androidBtGatt != null) {
                refresh();
                androidBtGatt.close();
                androidBtGatt = null;
            }
        }

        @Override
        public void localSave(String key, byte[] data) {
            SharedPreferences.Editor editor= BtleService.this.getSharedPreferences(btDevice.getAddress(), MODE_PRIVATE).edit();
            editor.putString(key, new String(Base64.encode(data, Base64.DEFAULT)));
            editor.apply();
        }

        @Override
        public InputStream localRetrieve(String key) {
            SharedPreferences prefs= BtleService.this.getSharedPreferences(btDevice.getAddress(), MODE_PRIVATE);
            return prefs.contains(key) ?
                    new ByteArrayInputStream(Base64.decode(prefs.getString(key, "").getBytes(), Base64.DEFAULT)) :
                    null;
        }

        @Override
        public Task<File> downloadFileAsync(final String srcUrl, final String dest) {
            final Capture<HttpURLConnection> urlConn = new Capture<>();
            return Task.callInBackground(() -> {
                URL fileUrl = new URL(srcUrl);
                urlConn.set((HttpURLConnection) fileUrl.openConnection());
                InputStream ins = urlConn.get().getInputStream();

                int code = urlConn.get().getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    File firmwareDir = new File(getFilesDir(), DOWNLOAD_DIR_NAME);
                    if (!firmwareDir.exists()) {
                        firmwareDir.mkdir();
                    }

                    File location = new File(firmwareDir, dest);
                    FileOutputStream fos = new FileOutputStream(location);

                    byte data[] = new byte[1024];
                    int count;
                    while ((count = ins.read(data)) != -1) {
                        fos.write(data, 0, count);
                    }
                    fos.close();

                    return location;
                }
                throw new IOException(String.format("Could not retrieve resource (response = %d, msg = %s)", code, urlConn.get().getResponseMessage()));
            }).continueWithTask(ignored -> {
                if (urlConn.get() != null) {
                    urlConn.get().disconnect();
                }
                return ignored;
            });
        }

        @Override
        public File findDownloadedFile(String filename) {
            File downloadFolder = new File(getFilesDir(), DOWNLOAD_DIR_NAME);
            return new File(downloadFolder, filename);
        }

        @Override
        public void logWarn(String tag, String message) {
            Log.w(tag, message);
        }

        @Override
        public void logWarn(String tag, String message, Throwable tr) {
            Log.w(tag, message, tr);
        }

        @Override
        public void onDisconnect(DisconnectHandler handler) {
            dcHandler = handler;
        }

        public boolean serviceExists(UUID gattService) {
            return androidBtGatt != null && androidBtGatt.getService(gattService) != null;
        }

        @Override
        public Task<Void> writeCharacteristicAsync(final BtleGattCharacteristic characteristic, final WriteType type, final byte[] value) {
            if (androidBtGatt == null) {
                return Task.forError(new IllegalStateException("Not connected to the BTLE gatt server"));
            }

            final BluetoothGattService service = androidBtGatt.getService(characteristic.serviceUuid);
            if (service == null) {
                return Task.forError(new IllegalStateException("Service \'" + characteristic.serviceUuid.toString() + "\' does not exist"));
            }

            final BluetoothGattCharacteristic androidGattChar = service.getCharacteristic(characteristic.uuid);
            if (androidGattChar == null) {
                return Task.forError(new IllegalStateException("Characteristic \'" + characteristic.serviceUuid.toString() + "\' does not exist"));
            }

            return addGattOperation(this, "onCharacteristicWrite not called within %dms", () -> {
                androidGattChar.setWriteType(type == WriteType.WITHOUT_RESPONSE ?
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE :
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                );
                androidGattChar.setValue(value);

                androidBtGatt.writeCharacteristic(androidGattChar);
            }).onSuccessTask(task -> Task.<Void>forResult(null));
        }

        @Override
        public Task<byte[][]> readCharacteristicAsync(final BtleGattCharacteristic[] characteristics) {
            // Can use do this in parallel since internally, gatt operations are queued and only executed 1 by 1
            final ArrayList<Task<byte[]>> tasks = new ArrayList<>();
            for(BtleGattCharacteristic it: characteristics) {
                tasks.add(readCharacteristicAsync(it));
            }

            return Task.whenAll(tasks).onSuccessTask(task -> {
                byte[][] valuesArray = new byte[tasks.size()][];
                for (int i = 0; i < valuesArray.length; i++) {
                    valuesArray[i] = tasks.get(i).getResult();
                }

                return Task.forResult(valuesArray);
            });
        }

        @Override
        public Task<byte[]> readCharacteristicAsync(final BtleGattCharacteristic characteristic) {
            if (androidBtGatt == null) {
                return Task.forError(new IllegalStateException("Not connected to the BTLE gatt server"));
            }

            BluetoothGattService service = androidBtGatt.getService(characteristic.serviceUuid);
            if (service == null) {
                return Task.forError(new IllegalStateException("Service \'" + characteristic.serviceUuid.toString() + "\' does not exist"));
            }

            final BluetoothGattCharacteristic androidGattChar = service.getCharacteristic(characteristic.uuid);
            if (androidGattChar == null) {
                return Task.forError(new IllegalStateException("Characteristic \'" + characteristic.serviceUuid.toString() + "\' does not exist"));
            }

            return addGattOperation(this, "onCharacteristicRead not called within %dms", () -> androidBtGatt.readCharacteristic(androidGattChar));
        }

        private Task<Void> editNotifications(BtleGattCharacteristic characteristic, final NotificationListener listener) {
            if (androidBtGatt == null) {
                return Task.forError(new IllegalStateException("Not connected to the BTLE gatt server"));
            }

            BluetoothGattService service = androidBtGatt.getService(characteristic.serviceUuid);
            if (service == null) {
                return Task.forError(new IllegalStateException("Service \'" + characteristic.serviceUuid.toString() + "\' does not exist"));
            }

            final BluetoothGattCharacteristic androidGattChar = service.getCharacteristic(characteristic.uuid);
            if (androidGattChar == null) {
                return Task.forError(new IllegalStateException("Characteristic \'" + characteristic.serviceUuid.toString() + "\' does not exist"));
            }

            int charProps = androidGattChar.getProperties();
            if ((charProps & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                return addGattOperation(this, "onDescriptorWrite not called within %dms", () -> {
                    androidBtGatt.setCharacteristicNotification(androidGattChar, true);
                    BluetoothGattDescriptor descriptor = androidGattChar.getDescriptor(CHARACTERISTIC_CONFIG);
                    descriptor.setValue(listener == null ? BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    androidBtGatt.writeDescriptor(descriptor);
                }).onSuccessTask(ignored -> {
                    notificationListener = listener;
                    return Task.forResult(null);
                });
            }
            return Task.forError(new IllegalStateException(("Characteristic does not have 'notify property' bit set")));
        }

        @Override
        public Task<Void> enableNotificationsAsync(BtleGattCharacteristic characteristic, final NotificationListener listener) {
            return editNotifications(characteristic, listener);
        }

        @Override
        public Task<Void> localDisconnectAsync() {
            Task<Void> task = remoteDisconnectAsync();

            if (!task.isCompleted()) {
                if (nGattOps.get() > 0) {
                    readyToClose.set(true);
                } else {
                    if (connectTask.isCompleted()) {
                        androidBtGatt.disconnect();
                    } else {
                        connectTask.cancel();
                        disconnected(0);
                    }
                }
            }
            return task;
        }

        @Override
        public Task<Void> remoteDisconnectAsync() {
            if (androidBtGatt == null) {
                return Task.forResult(null);
            }

            disconnectTaskSrc = new TaskCompletionSource<>();
            return disconnectTaskSrc.getTask();
        }

        @Override
        public Task<Void> connectAsync() {
            if (androidBtGatt != null) {
                return Task.forResult(null);
            }

            return connectTask.execute("Failed to connect and discover services within %dms", 10000,
                    () -> androidBtGatt = btDevice.connectGatt(BtleService.this, false, btleGattCallback)
            ).continueWithTask(task -> {
                if (task.isFaulted()) {
                    closeGatt();
                }
                return task;
            });
        }

        @Override
        public Task<Integer> readRssiAsync() {
            return androidBtGatt != null ?
                    addGattOperation(this, "onReadRemoteRssi not called within %dms", () -> androidBtGatt.readRemoteRssi())
                            .onSuccessTask(task -> Task.forResult(ByteBuffer.wrap(task.getResult()).order(ByteOrder.LITTLE_ENDIAN).getInt(0))) :
                    Task.forError(new IllegalStateException("No longer connected to the BTLE gatt server"));
        }
    }

    private final IBinder mBinder= new LocalBinder();

    /**
     * Provides methods for interacting with the service
     * @author Eric Tsai
     */
    public class LocalBinder extends Binder {
        /**
         * Instantiates a MetaWearBoard class
         * @param device    BluetoothDevice object corresponding to the target MetaWear board
         * @return MetaWearBoard object
         */
        public MetaWearBoard getMetaWearBoard(final BluetoothDevice device) {
            if (!btleDevices.containsKey(device)) {
                btleDevices.put(device, new AndroidPlatform(device));
            }
            return btleDevices.get(device).board;
        }
        /**
         * Removes the MetaWearBoard object associated with the BluetoothDevice object
         * @param btDevice    BluetoothDevice object corresponding to the target MetaWear board
         */
        public void removeMetaWearBoard(final BluetoothDevice btDevice) {
            AndroidPlatform value;
            if ((value = btleDevices.get(btDevice)) != null) {
                value.closeGatt();
                btleDevices.remove(btDevice);
            }
        }
        /**
         * Removes the saved serialized state of the MetaWearBoard object associated with the BluetoothDevice object
         * @param btDevice    BluetoothDevice object corresponding to the target MetaWear board
         */
        public void clearSerializedState(final BluetoothDevice btDevice) {
            SharedPreferences preferences = BtleService.this.getSharedPreferences(btDevice.getAddress(), MODE_PRIVATE);
            preferences.edit().clear().apply();
        }
        /**
         * Removes downloaded files cached on the Android device
         */
        public void clearDownloadCache() {
            File downloadFolder = new File(getFilesDir(), DOWNLOAD_DIR_NAME);
            for(File it: downloadFolder.listFiles()) {
                if (it.isFile()) {
                    it.delete();
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        for(Map.Entry<BluetoothDevice, AndroidPlatform> it: btleDevices.entrySet()) {
            it.getValue().closeGatt();
        }
        btleDevices.clear();

        super.onDestroy();
    }

}
