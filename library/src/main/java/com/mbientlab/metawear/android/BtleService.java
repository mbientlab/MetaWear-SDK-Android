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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * Created by etsai on 10/9/16.
 */
public class BtleService extends Service {
    private static UUID CHARACTERISTIC_CONFIG= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static String DOWNLOAD_DIR_NAME = "download", LOG_TAG = "metawear-btle";

    private final BluetoothGattCallback btleGattCallback= new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, int newState) {
            final AndroidPlatform platform= btleDevices.get(gatt.getDevice());

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    if (status != 0) {
                        platform.closeGatt();
                        if (platform.connectTask.get() != null) {
                            taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                                @Override
                                public void run() {
                                    platform.setConnectTaskError(new RuntimeException(String.format(Locale.US, "Non-zero connection changed status (%s)", status)));
                                }
                            }));
                        }
                    } else {
                        gatt.discoverServices();
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    platform.closeGatt();

                    final BluetoothDevice device= gatt.getDevice();
                    taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            if (platform.connectTask.get() != null && status != 0) {
                                platform.setConnectTaskError(new RuntimeException(String.format(Locale.US, "Non-zero connection changed status (%s)", status)));
                                btleDevices.get(device).gattCallback.onDisconnect();
                            } else if (platform.disconnectTask.get() == null) {
                                btleDevices.get(device).gattCallback.onUnexpectedDisconnect(status);
                            } else {
                                platform.disconnectTask.getAndSet(null).setResult(null);
                                btleDevices.get(device).gattCallback.onDisconnect();
                            }
                        }
                    }));
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            final AndroidPlatform platform= btleDevices.get(gatt.getDevice());

            if (status != 0) {
                platform.closeGatt();
                if (platform.connectTask.get() != null) {
                    taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            platform.setConnectTaskError(new RuntimeException(String.format(Locale.US, "Non-zero service discovery status (%s)", status)));
                        }
                    }));
                }
            } else {
                final BluetoothGattService service = gatt.getService(MetaWearBoard.METAWEAR_GATT_SERVICE);
                if (service == null) {
                    boolean dfuServiceExists = false;
                    platform.nDescriptors = 0;
                    ArrayList<Callable<Boolean>> enNotifyActions = new ArrayList<>();

                    for (final BluetoothGattService it : gatt.getServices()) {
                        if (it.getUuid().equals(MetaWearBoard.METABOOT_SERVICE)) {
                            dfuServiceExists= true;
                        }
                        for (final BluetoothGattCharacteristic characteristic : it.getCharacteristics()) {
                            int charProps = characteristic.getProperties();
                            if ((charProps & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                platform.nDescriptors++;
                                enNotifyActions.add(new Callable<Boolean>() {
                                    @Override
                                    public Boolean call() {
                                        gatt.setCharacteristicNotification(characteristic, true);

                                        gattScheduler.setExpectedGattKey(GattActionKey.DESCRIPTOR_WRITE);
                                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_CONFIG);
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        gatt.writeDescriptor(descriptor);

                                        return true;
                                    }
                                });
                            }
                        }
                    }

                    if (!dfuServiceExists) {
                        platform.closeGatt();
                        taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                            @Override
                            public void run() {
                                platform.setConnectTaskError(new RuntimeException("Bluetooth LE device is not recognized by the API"));
                            }
                        }));
                    } else {
                        gattScheduler.queueActions(enNotifyActions);
                    }
                } else {
                    platform.nDescriptors= 1;

                    gattScheduler.queueAction(new Callable<Boolean>() {
                        @Override
                        public Boolean call() {
                            BluetoothGattCharacteristic characteristic = service.getCharacteristic(MetaWearBoard.METAWEAR_NOTIFY_CHAR);
                            gatt.setCharacteristicNotification(characteristic, true);

                            gattScheduler.setExpectedGattKey(GattActionKey.DESCRIPTOR_WRITE);
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_CONFIG);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);

                            return true;
                        }
                    });

                }
            }

            gattScheduler.executeNext(GattActionKey.NONE);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            gattScheduler.updateExecActionsState();
            gattScheduler.executeNext(GattActionKey.CHAR_READ);

            btleDevices.get(gatt.getDevice()).gattCallback.onCharRead(
                    new BtleGattCharacteristic(characteristic.getService().getUuid(), characteristic.getUuid()),
                    characteristic.getValue()
            );
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            gattScheduler.updateExecActionsState();
            gattScheduler.executeNext(GattActionKey.CHAR_WRITE);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            btleDevices.get(gatt.getDevice()).gattCallback.onMwNotifyCharChanged(characteristic.getValue());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, final int status) {
            gattScheduler.updateExecActionsState();

            final AndroidPlatform platform= btleDevices.get(gatt.getDevice());
            if (status != 0) {
                platform.closeGatt();

                gattScheduler.executeNext(GattActionKey.DESCRIPTOR_WRITE);
                if (platform.connectTask.get() != null) {
                    taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            platform.setConnectTaskError(new RuntimeException(String.format(Locale.US, "Non-zero descriptor writing status (%s)", status)));
                        }
                    }));
                }
            } else {
                gattScheduler.executeNext(GattActionKey.DESCRIPTOR_WRITE);
                platform.nDescriptors--;
                if (platform.nDescriptors == 0) {
                    platform.connectFuture.getAndSet(null).cancel(false);
                    taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            if (platform.connectTask.get() != null) {
                                platform.connectTask.getAndSet(null).setResult(null);
                            }
                        }
                    }));
                    gattScheduler.executeNext(GattActionKey.NONE);
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, final int status) {
            gattScheduler.updateExecActionsState();
            gattScheduler.executeNext(GattActionKey.RSSI_READ);

            final AndroidPlatform platform= btleDevices.get(gatt.getDevice());
            taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    if (status != 0) {
                        platform.rssiTask.getAndSet(null).setError(new RuntimeException(String.format(Locale.US, "Non-zero read RSSI status (%s)", status)));
                    } else {
                        platform.rssiTask.getAndSet(null).setResult(rssi);
                    }
                }
            }));
        }
    };

    private enum GattActionKey {
        NONE,
        DESCRIPTOR_WRITE,
        CHAR_READ,
        CHAR_WRITE,
        RSSI_READ
    }
    private class GattActionScheduler {
        final int GATT_FORCE_EXEC_DELAY= 1000;

        private ConcurrentLinkedQueue<Callable<Boolean>> actions= new ConcurrentLinkedQueue<>();
        private AtomicBoolean isExecActions= new AtomicBoolean();
        private GattActionKey gattKey= GattActionKey.NONE;
        private ScheduledFuture<?> gattForceExecFuture;
        private final Runnable gattForceExec= new Runnable() {
            @Override
            public void run() {
                gattScheduler.setExpectedGattKey(GattActionKey.NONE);
                gattScheduler.executeNext(GattActionKey.NONE);
            }
        };

        void setExpectedGattKey(GattActionKey newKey) {
            gattKey= newKey;
        }

        void updateExecActionsState() {
            gattForceExecFuture.cancel(false);
            isExecActions.set(!actions.isEmpty());
        }

        void queueAction(Callable<Boolean> newAction) {
            actions.add(newAction);
        }

        void queueActions(Collection<Callable<Boolean>> newActions) {
            actions.addAll(newActions);
        }

        void executeNext(GattActionKey key) {
            if (!actions.isEmpty()) {
                if (key == gattKey || !isExecActions.get()) {
                    isExecActions.set(true);
                    boolean lastResult= false;
                    try {
                        while (!actions.isEmpty() && !(lastResult = actions.poll().call())) {
                        }

                        if (lastResult) {
                            gattForceExecFuture = taskScheduler.schedule(gattForceExec, GATT_FORCE_EXEC_DELAY,  TimeUnit.MILLISECONDS);
                        }
                    } catch (Exception ex) {
                        lastResult= false;
                    }

                    if (!lastResult && actions.isEmpty()) {
                        isExecActions.set(false);
                        gattKey= GattActionKey.NONE;
                    }
                }
            } else {
                isExecActions.set(false);
                gattKey= GattActionKey.NONE;
            }
        }
    }

    private class AndroidPlatform implements IO, BtleGatt {
        int nDescriptors;
        final AtomicReference<TaskCompletionSource<Void>> connectTask = new AtomicReference<>(),
                disconnectTask = new AtomicReference<>();
        final AtomicReference<TaskCompletionSource<Integer>> rssiTask = new AtomicReference<>();
        BluetoothGatt androidBtGatt;
        final AtomicReference<ScheduledFuture<?>> connectFuture = new AtomicReference<>();
        final Runnable connectTimeout = new Runnable() {
            @Override
            public void run() {
                connectFuture.getAndSet(null).cancel(false);
                closeGatt();
                if (connectTask.get() != null) {
                    connectTask.getAndSet(null).setError(new TimeoutException("Timed out establishing Bluetooth Connection"));
                }
            }
        };

        private final BluetoothDevice btDevice;
        private final MetaWearBoard board;

        BtleGatt.Callback gattCallback;

        AndroidPlatform(BluetoothDevice btDevice) {
            this.btDevice = btDevice;
            board = new JseMetaWearBoard(this, this, btDevice.getAddress());
        }

        void closeGatt() {
            if (androidBtGatt != null) {
                try {
                    androidBtGatt.getClass().getMethod("refresh").invoke(androidBtGatt);
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "Error refreshing gatt cache", e);
                } finally {
                    androidBtGatt.close();
                    androidBtGatt = null;
                }
            }
        }

        void setConnectTaskError(Exception e) {
            connectFuture.getAndSet(null).cancel(false);
            connectTask.getAndSet(null).setError(e);
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
        public void registerCallback(Callback callback) {
            gattCallback = callback;
        }

        @Override
        public void writeCharacteristic(final BtleGattCharacteristic gattChar, final BtleGatt.GattCharWriteType writeType, final byte[] value) {
            gattScheduler.queueAction(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    gattScheduler.setExpectedGattKey(GattActionKey.CHAR_WRITE);

                    BluetoothGattService service = androidBtGatt.getService(gattChar.serviceUuid);
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(gattChar.uuid);
                    characteristic.setWriteType(writeType == BtleGatt.GattCharWriteType.WRITE_WITHOUT_RESPONSE ?
                            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE :
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    );
                    characteristic.setValue(value);

                    androidBtGatt.writeCharacteristic(characteristic);

                    return true;
                }
            });
            gattScheduler.executeNext(GattActionKey.NONE);
        }

        @Override
        public void readCharacteristic(final BtleGattCharacteristic gattChar) {
            gattScheduler.queueAction(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    gattScheduler.setExpectedGattKey(GattActionKey.CHAR_READ);
                    BluetoothGattService service = androidBtGatt.getService(gattChar.serviceUuid);
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(gattChar.uuid);

                    androidBtGatt.readCharacteristic(characteristic);
                    return true;
                }
            });
            gattScheduler.executeNext(GattActionKey.NONE);
        }

        @Override
        public boolean serviceExists(UUID serviceUuid) {
            return androidBtGatt.getService(serviceUuid) != null;
        }

        @Override
        public Task<Void> disconnectAsync() {
            Task<Void> task = boardDisconnectAsync();
            androidBtGatt.disconnect();
            return task;
        }

        @Override
        public Task<Void> boardDisconnectAsync() {
            if (connectTask.get() != null) {
                taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        connectFuture.getAndSet(null).cancel(false);
                        connectTask.getAndSet(null).setCancelled();
                    }
                }));
            }

            if (disconnectTask.get() == null) {
                disconnectTask.set(new TaskCompletionSource<Void>());
            }

            return disconnectTask.get().getTask();
        }

        @Override
        public Task<Void> connectAsync() {
            if (connectTask.get() == null) {
                connectTask.set(new TaskCompletionSource<Void>());
                androidBtGatt = btDevice.connectGatt(BtleService.this, false, btleGattCallback);

                if (connectFuture.get()!= null) {
                    connectFuture.getAndSet(null).cancel(false);
                }
                connectFuture.set(taskScheduler.schedule(connectTimeout, 7500L, TimeUnit.MILLISECONDS));
            }

            return connectTask.get().getTask();
        }

        @Override
        public Task<Integer> readRssiAsync() {
            if (androidBtGatt != null) {
                if (rssiTask.get() == null) {
                    rssiTask.set(new TaskCompletionSource<Integer>());
                    gattScheduler.queueAction(new Callable<Boolean>() {
                        @Override
                        public Boolean call() {
                            gattScheduler.setExpectedGattKey(GattActionKey.RSSI_READ);
                            androidBtGatt.readRemoteRssi();
                            return true;
                        }
                    });
                    gattScheduler.executeNext(GattActionKey.NONE);
                }
                return rssiTask.get().getTask();
            }
            return Task.forError(new RuntimeException("No active BLE connection"));
        }
    }

    private final static long FUTURE_CHECKER_PERIOD= 1000L;
    private final ConcurrentLinkedQueue<Future<?>> taskFutures = new ConcurrentLinkedQueue<>();
    private final ExecutorService backgroundThreadPool= Executors.newCachedThreadPool();
    private final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(4);
    private final Runnable futureChecker = new Runnable() {
        @Override
        public void run() {
            ConcurrentLinkedQueue<Future<?>> notYetCompleted= new ConcurrentLinkedQueue<>();
            while (!taskFutures.isEmpty()) {
                Future<?> next= taskFutures.poll();
                if (next.isDone()) {
                    try {
                        next.get();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Background task reported an error", e);
                    }
                } else {
                    notYetCompleted.add(next);
                }
            }

            taskFutures.addAll(notYetCompleted);
            taskScheduler.schedule(futureChecker, FUTURE_CHECKER_PERIOD, TimeUnit.MILLISECONDS);
        }
    };

    private final IBinder mBinder= new LocalBinder();
    private final GattActionScheduler gattScheduler= new GattActionScheduler();
    private final Map<BluetoothDevice, AndroidPlatform> btleDevices = new HashMap<>();

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
            btleDevices.remove(btDevice);
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
    public void onCreate() {
        taskScheduler.schedule(futureChecker, FUTURE_CHECKER_PERIOD, TimeUnit.MILLISECONDS);

        super.onCreate();
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
