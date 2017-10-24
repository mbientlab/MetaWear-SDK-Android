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
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.impl.JseMetaWearBoard;
import com.mbientlab.metawear.impl.platform.BtleGatt;
import com.mbientlab.metawear.impl.platform.BtleGattCharacteristic;
import com.mbientlab.metawear.impl.platform.IO;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import bolts.Continuation;
import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * Created by etsai on 10/9/16.
 */
public class BtleService extends Service {
    private static UUID CHARACTERISTIC_CONFIG= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static String DOWNLOAD_DIR_NAME = "download", LOG_TAG = "metawear-btle";

    private interface GattOp {
        AndroidPlatform owner();
        void execute();
        TaskCompletionSource<byte[]> taskCompletionSource();
    }

    private ScheduledFuture<?> gattTaskTimeoutFuture;
    private final Queue<GattOp> pendingGattOps = new ConcurrentLinkedQueue<>();
    private void addGattOperation(AndroidPlatform platform, GattOp task) {
        platform.nGattOps.incrementAndGet();
        pendingGattOps.add(task);
        executeGattOperation(false);
    }
    private void executeGattOperation(boolean ready) {
        if (!pendingGattOps.isEmpty() && (pendingGattOps.size() == 1 || ready)) {
            try {
                gattTaskTimeoutFuture = taskScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        GattOp task = pendingGattOps.peek();
                        task.taskCompletionSource().setError(new TimeoutException("Gatt operation timed out"));

                        task.owner().gattTaskCompleted();

                        pendingGattOps.poll();
                        executeGattOperation(true);
                    }
                }, 1000L, TimeUnit.MILLISECONDS);

                pendingGattOps.peek().execute();
            } catch (Exception e) {
                pendingGattOps.poll().taskCompletionSource().setError(e);
                executeGattOperation(true);
            }
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
                        platform.setConnectTaskError(new IllegalStateException(String.format(Locale.US, "Non-zero connection changed status (%s)", status)));
                    } else {
                        gatt.discoverServices();
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    platform.closeGatt();

                    if (platform.connectTask.get() != null && status != 0) {
                        platform.setConnectTaskError(new IllegalStateException(String.format(Locale.US, "Non-zero connection changed status (%s)", status)));
                    } else {
                        TaskCompletionSource<Void> dcTaskSource = platform.disconnectTask.getAndSet(null);

                        if (dcTaskSource == null) {
                            platform.dcHandler.onUnexpectedDisconnect(status);
                        } else {
                            dcTaskSource.setResult(null);
                            platform.dcHandler.onDisconnect();
                        }
                    }
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            final AndroidPlatform platform = btleDevices.get(gatt.getDevice());
            if (status != 0) {
                platform.closeGatt();
                platform.setConnectTaskError(new IllegalStateException(String.format(Locale.US, "Non-zero service discovery status (%d)", status)));
            } else {
                platform.cancelConnectTimeout();

                TaskCompletionSource<Void> reference = platform.connectTask.getAndSet(null);
                if (reference != null) {
                    reference.setResult(null);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            gattTaskTimeoutFuture.cancel(false);

            GattOp task = pendingGattOps.peek();
            if (status != 0) {
                task.taskCompletionSource().setError(new IllegalStateException(String.format(Locale.US, "Non-zero characteristic read status (%d)", status)));
            } else {
                task.taskCompletionSource().setResult(characteristic.getValue());
            }

            task.owner().gattTaskCompleted();

            pendingGattOps.poll();
            executeGattOperation(true);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            gattTaskTimeoutFuture.cancel(false);

            GattOp task = pendingGattOps.peek();
            if (status != 0) {
                task.taskCompletionSource().setError(new IllegalStateException(String.format(Locale.US, "Non-zero service discovery status (%d)", status)));
            } else {
                task.taskCompletionSource().setResult(characteristic.getValue());
            }

            task.owner().gattTaskCompleted();

            pendingGattOps.poll();
            executeGattOperation(true);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            btleDevices.get(gatt.getDevice()).notificationListener.onChange(characteristic.getValue());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            gattTaskTimeoutFuture.cancel(false);

            GattOp task = pendingGattOps.peek();
            if (status != 0) {
                task.taskCompletionSource().setError(new IllegalStateException(String.format(Locale.US, "Non-zero service discovery status (%d)", status)));
            } else {
                task.taskCompletionSource().setResult(null);
            }

            task.owner().gattTaskCompleted();

            pendingGattOps.poll();
            executeGattOperation(true);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            gattTaskTimeoutFuture.cancel(false);

            GattOp task = pendingGattOps.peek();
            if (status != 0) {
                task.taskCompletionSource().setError(new IllegalStateException(String.format(Locale.US, "Non-zero service discovery status (%d)", status)));
            } else {
                task.taskCompletionSource().setResult(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(rssi).array());
            }

            task.owner().gattTaskCompleted();

            pendingGattOps.poll();
            executeGattOperation(true);
        }
    };

    private class AndroidPlatform implements IO, BtleGatt {
        private final AtomicBoolean readyToClose = new AtomicBoolean();
        private final AtomicInteger nGattOps = new AtomicInteger();

        final AtomicReference<TaskCompletionSource<Void>> connectTask = new AtomicReference<>(),
                disconnectTask = new AtomicReference<>();
        BluetoothGatt androidBtGatt;
        private final AtomicReference<ScheduledFuture<?>> connectFuture = new AtomicReference<>();

        private DisconnectHandler dcHandler;
        private NotificationListener notificationListener;
        private final BluetoothDevice btDevice;
        private final MetaWearBoard board;

        AndroidPlatform(BluetoothDevice btDevice) {
            this.btDevice = btDevice;
            board = new JseMetaWearBoard(this, this, btDevice.getAddress());
        }

        void cancelConnectTimeout() {
            ScheduledFuture<?> futureReference = connectFuture.getAndSet(null);
            if (futureReference != null) {
                futureReference.cancel(false);
            }
        }

        void gattTaskCompleted() {
            int count = nGattOps.decrementAndGet();
            if (count == 0 && readyToClose.get()) {
                taskScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (androidBtGatt != null) {
                            androidBtGatt.disconnect();
                        }
                    }
                }, 1000, TimeUnit.MILLISECONDS);
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

        void setConnectTaskError(Exception e) {
            TaskCompletionSource<Void> reference = connectTask.getAndSet(null);
            if (reference != null) {
                reference.setError(e);
            }

            cancelConnectTimeout();
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
            final TaskCompletionSource<File> downloadTask = new TaskCompletionSource<>();

            new AsyncTask<String, Void, Void>() {
                @Override
                protected Void doInBackground(String... params) {
                    HttpURLConnection urlConn = null;

                    try {
                        URL fileUrl = new URL(srcUrl);
                        urlConn = (HttpURLConnection) fileUrl.openConnection();
                        InputStream ins = urlConn.getInputStream();

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

                        downloadTask.setResult(location);
                    } catch (MalformedURLException e) {
                        downloadTask.setError(e);
                    } catch (IOException e) {
                        downloadTask.setError(e);
                    } finally {
                        if (urlConn != null) {
                            urlConn.disconnect();
                        }
                    }

                    return null;
                }
            }.execute(dest);
            return downloadTask.getTask();
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

            BluetoothGattService service = androidBtGatt.getService(characteristic.serviceUuid);
            if (service == null) {
                return Task.forError(new IllegalStateException("Service \'" + characteristic.serviceUuid.toString() + "\' does not exist"));
            }

            final BluetoothGattCharacteristic androidGattChar = service.getCharacteristic(characteristic.uuid);
            if (androidGattChar == null) {
                return Task.forError(new IllegalStateException("Characteristic \'" + characteristic.serviceUuid.toString() + "\' does not exist"));
            }

            final TaskCompletionSource<byte[]> taskSource = new TaskCompletionSource<>();
            addGattOperation(this, new GattOp() {
                @Override
                public AndroidPlatform owner() {
                    return AndroidPlatform.this;
                }

                @Override
                public void execute() {
                    BluetoothGattService service = androidBtGatt.getService(characteristic.serviceUuid);
                    BluetoothGattCharacteristic androidGattChar = service.getCharacteristic(characteristic.uuid);
                    androidGattChar.setWriteType(type == WriteType.WITHOUT_RESPONSE ?
                            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE :
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    );
                    androidGattChar.setValue(value);

                    androidBtGatt.writeCharacteristic(androidGattChar);
                }

                @Override
                public TaskCompletionSource<byte[]> taskCompletionSource() {
                    return taskSource;
                }
            });

            return taskSource.getTask().onSuccessTask(new Continuation<byte[], Task<Void>>() {
                @Override
                public Task<Void> then(Task<byte[]> task) throws Exception {
                    return Task.forResult(null);
                }
            });
        }

        @Override
        public Task<byte[][]> readCharacteristicAsync(final BtleGattCharacteristic[] characteristics) {
            // Can use do this in parallel since internally, gatt operations are queued and only executed 1 by 1
            final ArrayList<Task<byte[]>> tasks = new ArrayList<>();
            for(BtleGattCharacteristic it: characteristics) {
                tasks.add(readCharacteristicAsync(it));
            }

            return Task.whenAll(tasks).onSuccessTask(new Continuation<Void, Task<byte[][]>>() {
                @Override
                public Task<byte[][]> then(Task<Void> task) throws Exception {
                    byte[][] valuesArray = new byte[tasks.size()][];
                    for (int i = 0; i < valuesArray.length; i++) {
                        valuesArray[i] = tasks.get(i).getResult();
                    }

                    return Task.forResult(valuesArray);
                }
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

            final TaskCompletionSource<byte[]> taskSource = new TaskCompletionSource<>();
            addGattOperation(this, new GattOp() {
                @Override
                public AndroidPlatform owner() {
                    return AndroidPlatform.this;
                }

                @Override
                public void execute() {
                    androidBtGatt.readCharacteristic(androidGattChar);
                }

                @Override
                public TaskCompletionSource<byte[]> taskCompletionSource() {
                    return taskSource;
                }
            });

            return taskSource.getTask();
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

            Task<Void> task;
            int charProps = androidGattChar.getProperties();
            if ((charProps & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                final TaskCompletionSource<byte[]> taskSource = new TaskCompletionSource<>();

                task = taskSource.getTask().onSuccessTask(new Continuation<byte[], Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<byte[]> task) throws Exception {
                        // Cheat here since the only characteristic we enable notifications for is the MetaWear notify characteristic
                        notificationListener = listener;
                        return Task.forResult(null);
                    }
                });

                addGattOperation(this, new GattOp() {
                    @Override
                    public AndroidPlatform owner() {
                        return AndroidPlatform.this;
                    }

                    @Override
                    public void execute() {
                        androidBtGatt.setCharacteristicNotification(androidGattChar, true);
                        BluetoothGattDescriptor descriptor = androidGattChar.getDescriptor(CHARACTERISTIC_CONFIG);
                        descriptor.setValue(listener == null ? BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        androidBtGatt.writeDescriptor(descriptor);

                    }

                    @Override
                    public TaskCompletionSource<byte[]> taskCompletionSource() {
                        return taskSource;
                    }
                });
            } else {
                task = Task.forError(new IllegalStateException(("Characteristic does not have 'notify property' bit set")));
            }
            return task;
        }

        @Override
        public Task<Void> enableNotificationsAsync(BtleGattCharacteristic characteristic, final NotificationListener listener) {
            return editNotifications(characteristic, listener);
        }

        @Override
        public Task<Void> localDisconnectAsync() {
            if (androidBtGatt == null) {
                return Task.forResult(null);
            }

            Task<Void> task = remoteDisconnectAsync();

            if (nGattOps.get() > 0) {
                readyToClose.set(true);
            } else {
                androidBtGatt.disconnect();
            }
            return task;
        }

        @Override
        public Task<Void> remoteDisconnectAsync() {
            TaskCompletionSource<Void> taskReference = connectTask.getAndSet(null);
            if (taskReference != null) {
                taskReference.setCancelled();
            }

            cancelConnectTimeout();

            taskReference = disconnectTask.get();
            if (taskReference == null) {
                taskReference = new TaskCompletionSource<>();
                disconnectTask.set(taskReference);
            }

            return taskReference.getTask();
        }

        @Override
        public Task<Void> connectAsync() {
            if (androidBtGatt != null) {
                return Task.forResult(null);
            }

            TaskCompletionSource<Void> taskReference = connectTask.get();
            if (taskReference == null) {
                taskReference = new TaskCompletionSource<>();
                connectTask.set(taskReference);

                cancelConnectTimeout();

                connectFuture.set(taskScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        cancelConnectTimeout();

                        closeGatt();

                        TaskCompletionSource<Void> reference = connectTask.getAndSet(null);
                        if (reference != null) {
                            reference.setError(new TimeoutException("Timed out establishing Bluetooth Connection"));
                        }
                    }
                }, 7500L, TimeUnit.MILLISECONDS));

                androidBtGatt = btDevice.connectGatt(BtleService.this, false, btleGattCallback);
            }

            return taskReference.getTask();
        }

        @Override
        public Task<Integer> readRssiAsync() {
            if (androidBtGatt != null) {
                final TaskCompletionSource<byte[]> taskSource = new TaskCompletionSource<>();

                addGattOperation(this, new GattOp() {
                    @Override
                    public AndroidPlatform owner() {
                        return AndroidPlatform.this;
                    }

                    @Override
                    public void execute() {
                        androidBtGatt.readRemoteRssi();
                    }

                    @Override
                    public TaskCompletionSource<byte[]> taskCompletionSource() {
                        return taskSource;
                    }
                });

                return taskSource.getTask().onSuccessTask(new Continuation<byte[], Task<Integer>>() {
                    @Override
                    public Task<Integer> then(Task<byte[]> task) throws Exception {
                        return Task.forResult(ByteBuffer.wrap(task.getResult()).order(ByteOrder.LITTLE_ENDIAN).getInt(0));
                    }
                });
            }
            return Task.forError(new IllegalStateException("No longer connected to the BTLE gatt server"));
        }
    }

    private final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(4);
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
