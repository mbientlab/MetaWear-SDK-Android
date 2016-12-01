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
import com.mbientlab.metawear.impl.MetaWearBoardImpl;
import com.mbientlab.metawear.impl.Pair;
import com.mbientlab.metawear.impl.Platform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
    private static UUID CHARACTERISTIC_CONFIG= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            METAWEAR_NOTIFY= UUID.fromString("326A9006-85CB-9195-D9DD-464CFBBAE75A");
    private static String FIRMWARE_DIR_NAME = "firmware";

    private final BluetoothGattCallback btleGattCallback= new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, int newState) {
            final BoardState state= mwBoardStates.get(gatt.getDevice());

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    if (status != 0) {
                        state.closeGatt();
                        if (state.connectTask.get() != null) {
                            taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                                @Override
                                public void run() {
                                    state.setConnectTaskError(new RuntimeException(String.format(Locale.US, "Non-zero connection changed status (%s)", status)));
                                }
                            }));
                        }
                    } else {
                        gatt.discoverServices();
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    state.closeGatt();

                    final BluetoothDevice device= gatt.getDevice();
                    taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            if (state.connectTask.get() != null && status != 0) {
                                state.setConnectTaskError(new RuntimeException(String.format(Locale.US, "Non-zero connection changed status (%s)", status)));
                            } else if (state.disconnectTask.get() == null) {
                                mwBoards.get(device).handleUnexpectedDisconnect(status);
                            } else {
                                state.disconnectTask.getAndSet(null).setResult(null);
                            }

                            mwBoards.get(device).disconnected();
                        }
                    }));
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            final BoardState state= mwBoardStates.get(gatt.getDevice());

            if (status != 0) {
                state.closeGatt();
                if (state.connectTask.get() != null) {
                    taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            state.setConnectTaskError(new RuntimeException(String.format(Locale.US, "Non-zero service discovery status (%s)", status)));
                        }
                    }));
                }
            } else {
                state.nDescriptors= 0;
                for (BluetoothGattService service : gatt.getServices()) {
                    if (service.getUuid().equals(MetaWearBoard.METABOOT_SERVICE_UUID)) {
                        state.inMetaBoot= true;
                    }
                    for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        int charProps = characteristic.getProperties();
                        if ((charProps & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            state.nDescriptors++;

                            gattScheduler.queueAction(new Callable<Boolean>() {
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
            }

            gattScheduler.executeNext(GattActionKey.NONE);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            gattScheduler.updateExecActionsState();
            gattScheduler.executeNext(GattActionKey.CHAR_READ);

            mwBoards.get(gatt.getDevice()).handleReadGattCharResponse(new Pair<>(characteristic.getService().getUuid(), characteristic.getUuid()), characteristic.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            gattScheduler.updateExecActionsState();
            gattScheduler.executeNext(GattActionKey.CHAR_WRITE);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(METAWEAR_NOTIFY)) {
                mwBoards.get(gatt.getDevice()).handleNotifyCharResponse(characteristic.getValue());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, final int status) {
            gattScheduler.updateExecActionsState();

            final BoardState state= mwBoardStates.get(gatt.getDevice());
            if (status != 0) {
                state.closeGatt();

                gattScheduler.executeNext(GattActionKey.DESCRIPTOR_WRITE);
                if (state.connectTask.get() != null) {
                    taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            state.setConnectTaskError(new RuntimeException(String.format(Locale.US, "Non-zero descriptor writing status (%s)", status)));
                        }
                    }));
                }
            } else {
                gattScheduler.executeNext(GattActionKey.DESCRIPTOR_WRITE);
                state.nDescriptors--;
                if (state.nDescriptors == 0) {
                    state.connectFuture.getAndSet(null).cancel(false);
                    taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            if (state.connectTask.get() != null) {
                                state.connectTask.getAndSet(null).setResult(null);
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

            final BoardState state= mwBoardStates.get(gatt.getDevice());
            taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    if (status != 0) {
                        state.rssiTask.setError(new RuntimeException(String.format(Locale.US, "Non-zero read rssi status (%s)", status)));
                    } else {
                        state.rssiTask.setResult(rssi);
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

    private static class BoardState {
        int nDescriptors;
        boolean inMetaBoot;
        final AtomicReference<TaskCompletionSource<Void>> connectTask = new AtomicReference<>(),
                disconnectTask = new AtomicReference<>();
        TaskCompletionSource<Integer> rssiTask;
        BluetoothGatt btGatt;
        final AtomicReference<ScheduledFuture<?>> connectFuture = new AtomicReference<>();
        final Runnable connectTimeout = new Runnable() {
            @Override
            public void run() {
                connectFuture.getAndSet(null).cancel(false);
                if (btGatt != null) {
                    btGatt.close();
                }
                if (connectTask.get() != null) {
                    connectTask.getAndSet(null)
                            .setError(new TimeoutException("Timed out establishing Bluetooth Connection"));
                }
            }
        };

        void closeGatt() {
            inMetaBoot = false;
            if (btGatt != null) {
                try {
                    btGatt.getClass().getMethod("refresh").invoke(btGatt);
                } catch (final Exception e) {
                    Log.e("MetaWear", "Error refreshing gatt cache", e);
                } finally {
                    btGatt.close();
                    btGatt= null;
                }
            }
        }

        void setConnectTaskError(Exception e) {
            connectFuture.getAndSet(null).cancel(false);
            connectTask.getAndSet(null).setError(e);
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
                        Log.e("MetaWear", "Background task reported an error", e);
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
    private final Map<BluetoothDevice, MetaWearBoardImpl> mwBoards= new HashMap<>();
    private final Map<BluetoothDevice, BoardState> mwBoardStates= new HashMap<>();

    /**
     * Provides methods for interacting with the service
     * @author Eric Tsai
     */
    public class LocalBinder extends Binder {
        static final String MODULE_INFO= "com.mbientlab.metawear.android.BtleService.LocalBinder.MODULE_INFO",
                BOARD_STATE= "com.mbientlab.metawear.android.BtleService.LocalBinder.BOARD_STATE";
        /**
         * Instantiates a MetaWearBoard class
         * @param btDevice    BluetoothDevice object corresponding to the target MetaWear board
         * @return MetaWearBoard object
         */
        public MetaWearBoard getMetaWearBoard(final BluetoothDevice btDevice) {
            if (!mwBoards.containsKey(btDevice)) {
                final BoardState state= new BoardState();
                mwBoardStates.put(btDevice, state);
                mwBoards.put(btDevice, new MetaWearBoardImpl(new Platform() {
                    void serializeObjects(String key, Serializable ... objects) throws IOException {
                        ByteArrayOutputStream buffer= new ByteArrayOutputStream(1024);
                        ObjectOutputStream oos= new ObjectOutputStream(buffer);

                        for(Serializable s : objects) {
                            oos.writeObject(s);
                        }
                        oos.close();

                        SharedPreferences.Editor editor= BtleService.this.getSharedPreferences(btDevice.getAddress(), MODE_PRIVATE).edit();
                        editor.putString(key, new String(Base64.encode(buffer.toByteArray(), Base64.DEFAULT)));
                        editor.apply();
                    }

                    ObjectInputStream getObjectInputStream(String key) throws IOException {
                        SharedPreferences prefs= BtleService.this.getSharedPreferences(btDevice.getAddress(), MODE_PRIVATE);
                        String infoState= prefs.getString(key, "");

                        if (!infoState.isEmpty()) {
                            ByteArrayInputStream buffer= new ByteArrayInputStream(Base64.decode(infoState.getBytes(), Base64.DEFAULT));
                            return new ObjectInputStream(buffer);
                        }
                        return null;
                    }

                    @Override
                    public void serializeBoardInfo(Serializable boardInfo) throws IOException {
                        serializeObjects(MODULE_INFO, boardInfo);
                    }

                    @Override
                    public Object deserializeBoardInfo() throws IOException, ClassNotFoundException {
                        ObjectInputStream ois = getObjectInputStream(MODULE_INFO);
                        if (ois != null) {
                            return ois.readObject();
                        }
                        return null;
                    }

                    @Override
                    public void serializeBoardState(Serializable persist) throws IOException {
                        serializeObjects(BOARD_STATE, persist);
                    }

                    @Override
                    public Object deserializeBoardState() throws IOException, ClassNotFoundException {
                        ObjectInputStream ois= getObjectInputStream(BOARD_STATE);
                        if (ois != null) {
                            return ois.readObject();
                        }
                        return null;
                    }

                    @Override
                    public void writeGattCharacteristic(final GattCharWriteType writeType, final Pair<UUID, UUID> gattCharr, final byte[] value) {
                        gattScheduler.queueAction(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                gattScheduler.setExpectedGattKey(GattActionKey.CHAR_WRITE);

                                BluetoothGattService service = state.btGatt.getService(gattCharr.first);
                                BluetoothGattCharacteristic characteristic = service.getCharacteristic(gattCharr.second);
                                characteristic.setWriteType(writeType == GattCharWriteType.WRITE_WITHOUT_RESPONSE ?
                                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE :
                                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                );
                                characteristic.setValue(value);

                                state.btGatt.writeCharacteristic(characteristic);

                                return true;
                            }
                        });
                        gattScheduler.executeNext(GattActionKey.NONE);
                    }

                    @Override
                    public void readGattCharacteristic(final Pair<UUID, UUID> gattCharr) {
                        gattScheduler.queueAction(new Callable<Boolean>() {
                            @Override
                            public Boolean call() {
                                gattScheduler.setExpectedGattKey(GattActionKey.CHAR_READ);
                                BluetoothGattService service = state.btGatt.getService(gattCharr.first);
                                BluetoothGattCharacteristic characteristic = service.getCharacteristic(gattCharr.second);

                                state.btGatt.readCharacteristic(characteristic);
                                return true;
                            }
                        });
                        gattScheduler.executeNext(GattActionKey.NONE);
                    }

                    @Override
                    public boolean inMetaBoot() {
                        return state.inMetaBoot;
                    }

                    @Override
                    public Task<Void> disconnect() {
                        Task<Void> task = boardDisconnect();
                        state.btGatt.disconnect();
                        return task;
                    }

                    @Override
                    public Task<Void> boardDisconnect() {
                        if (state.connectTask.get() != null) {
                            taskFutures.add(backgroundThreadPool.submit(new Runnable() {
                                @Override
                                public void run() {
                                    state.connectFuture.getAndSet(null).cancel(false);
                                    state.connectTask.getAndSet(null).setCancelled();
                                }
                            }));
                        }

                        if (state.disconnectTask.get() == null) {
                            state.disconnectTask.set(new TaskCompletionSource<Void>());
                        }

                        return state.disconnectTask.get().getTask();
                    }

                    @Override
                    public Task<Void> connect() {
                        if (state.connectTask.get() == null) {
                            state.connectTask.set(new TaskCompletionSource<Void>());
                            state.btGatt = btDevice.connectGatt(BtleService.this, false, btleGattCallback);

                            if (state.connectFuture.get()!= null) {
                                state.connectFuture.getAndSet(null).cancel(false);
                            }
                            state.connectFuture.set(taskScheduler.schedule(state.connectTimeout, 7500L, TimeUnit.MILLISECONDS));
                        }

                        return state.connectTask.get().getTask();
                    }

                    @Override
                    public Task<Integer> readRssiTask() {
                        TaskCompletionSource<Integer> taskSource = new TaskCompletionSource<>();
                        state.rssiTask = taskSource;
                        gattScheduler.queueAction(new Callable<Boolean>() {
                            @Override
                            public Boolean call() {
                                gattScheduler.setExpectedGattKey(GattActionKey.RSSI_READ);
                                state.btGatt.readRemoteRssi();
                                return true;
                            }
                        });
                        gattScheduler.executeNext(GattActionKey.NONE);
                        return taskSource.getTask();
                    }

                    @Override
                    public Task<File> downloadFile(final String source, final String dest) {
                        final TaskCompletionSource<File> downloadTask = new TaskCompletionSource<>();

                        new AsyncTask<String, Void, Void>() {
                            @Override
                            protected Void doInBackground(String... params) {
                                HttpURLConnection urlConn = null;

                                try {
                                    URL fileUrl = new URL(source);
                                    urlConn = (HttpURLConnection) fileUrl.openConnection();
                                    InputStream ins = urlConn.getInputStream();

                                    File firmwareDir = new File(getFilesDir(), FIRMWARE_DIR_NAME);
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
                    public File findFile(String filename) {
                        return new File(getFilesDir(), filename);
                    }
                }, btDevice.getAddress()));
            }
            return mwBoards.get(btDevice);
        }

        public void removeMetaWearBoard(final BluetoothDevice btDevice) {
            mwBoards.remove(btDevice);
        }

        public void clearSerializedState(final BluetoothDevice btDevice) {
            SharedPreferences.Editor editor= BtleService.this.getSharedPreferences(btDevice.getAddress(), MODE_PRIVATE).edit();
            editor.remove(MODULE_INFO);
            editor.remove(BOARD_STATE);
            editor.apply();
        }

        public void clearFirmwareCache() {
            File firmwareDir = new File(getFilesDir(), FIRMWARE_DIR_NAME);
            for(File it: firmwareDir.listFiles()) {
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
        for(Map.Entry<BluetoothDevice, BoardState> it: mwBoardStates.entrySet()) {
            if (it.getValue().btGatt != null) {
                it.getValue().closeGatt();
            }
        }
        mwBoardStates.clear();
        mwBoards.clear();

        super.onDestroy();
    }

}
