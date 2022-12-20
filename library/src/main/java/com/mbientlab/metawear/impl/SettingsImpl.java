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

import static com.mbientlab.metawear.impl.Constant.Module.SETTINGS;

import com.mbientlab.metawear.ActiveDataProducer;
import com.mbientlab.metawear.CodeBlock;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.Observer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.impl.platform.TimedTask;
import com.mbientlab.metawear.module.Settings;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

import bolts.Capture;
import bolts.Task;

/**
 * Created by etsai on 9/20/16.
 */
class SettingsImpl extends ModuleImplBase implements Settings {
    static String createUri(DataTypeBase dataType) {
        switch (Util.clearRead(dataType.eventConfig[1])) {
            case BATTERY_STATE:
                switch(dataType.attributes.length()) {
                    case 1:
                        return "battery[0]";
                    case 2:
                        return "battery[1]";
                }
                return "battery";
            case POWER_STATUS:
                return "power-status";
            case CHARGE_STATUS:
                return "charge-status";
            default:
                return null;
        }
    }

    private static final long serialVersionUID = -8845055245623576362L;
    private final static String BATTERY_PRODUCER= "com.mbientlab.metawear.impl.SettingsImpl.BATTERY_PRODUCER",
            BATTERY_CHARGE_PRODUCER= "com.mbientlab.metawear.impl.SettingsImpl.BATTERY_CHARGE_PRODUCER",
            BATTERY_VOLTAGE_PRODUCER= "com.mbientlab.metawear.impl.SettingsImpl.BATTERY_VOLTAGE_PRODUCER",
            POWER_STATUS_PRODUCER= "com.mbientlab.metawear.impl.SettingsImpl.POWER_STATUS_PRODUCER",
            CHARGE_STATUS_PRODUCER= "com.mbientlab.metawear.impl.SettingsImpl.CHARGE_STATUS_PRODUCER";

    private static final byte CONN_PARAMS_REVISION= 1, DISCONNECTED_EVENT_REVISION= 2, BATTERY_REVISION= 3,
        CHARGE_STATUS_REVISION = 5, WHITELIST_REVISION = 6, MMS_REVISION = 9, FORCE_1MPHY_REVISION = 10;
    private static final float AD_INTERVAL_STEP= 0.625f, CONN_INTERVAL_STEP= 1.25f, SUPERVISOR_TIMEOUT_STEP= 10f;
    private static final byte DEVICE_NAME = 1, AD_PARAM = 2, TX_POWER = 3,
        START_ADVERTISING = 5,
        SCAN_RESPONSE = 7, PARTIAL_SCAN_RESPONSE = 8,
        CONNECTION_PARAMS = 9,
        DISCONNECT_EVENT = 0xa,
        BATTERY_STATE= 0xc,
        POWER_STATUS = 0x11,
        CHARGE_STATUS = 0x12,
        THREE_VOLT_POWER = 0x1c,
        FORCE_1MPHY = 0x1d;

    private static class BatteryStateData extends DataTypeBase {
        private static final long serialVersionUID = -1080271339658673808L;

        BatteryStateData() {
            super(SETTINGS, Util.setSilentRead(BATTERY_STATE), new DataAttributes(new byte[] {1, 2}, (byte) 1, (byte) 0, false));
        }

        BatteryStateData(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, Constant.Module module, byte register, byte id, DataAttributes attributes) {
            return new BatteryStateData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate mwPrivate, Number value) {
            return value;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate mwPrivate, final byte[] data, final Calendar timestamp, DataPrivate.ClassToObject mapper) {
            final float voltage= ByteBuffer.wrap(data, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() / 1000f;
            final BatteryState state= new BatteryState(data[0], voltage);

            return new DataPrivate(timestamp, data, mapper) {
                @Override
                public Class<?>[] types() {
                    return new Class<?>[]{BatteryState.class};
                }

                @Override
                public <T> T value(Class<T> clazz) {
                    if (clazz == BatteryState.class) {
                        return clazz.cast(state);
                    }
                    return super.value(clazz);
                }
            };
        }

        @Override
        public DataTypeBase[] createSplits() {
            return new DataTypeBase[] {
                    new UintData(SETTINGS, eventConfig[1], eventConfig[2], new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false)),
                    new MilliUnitsUFloatData(SETTINGS, eventConfig[1], eventConfig[2], new DataAttributes(new byte[] {2}, (byte) 1, (byte) 1, false))
            };
        }
    }

    private final DataTypeBase disconnectDummyProducer;

    private transient ActiveDataProducer powerStatus, chargeStatus;
    private transient TimedTask<byte[]> readConnParamsTask, readAdConfigTask;
    private transient TimedTask<Byte> readPowerStatusTask, readChargeStatusTask;

    SettingsImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        DataTypeBase batteryProducer = new BatteryStateData();
        this.mwPrivate.tagProducer(BATTERY_PRODUCER, batteryProducer);
        this.mwPrivate.tagProducer(BATTERY_CHARGE_PRODUCER, batteryProducer.split[0]);
        this.mwPrivate.tagProducer(BATTERY_VOLTAGE_PRODUCER, batteryProducer.split[1]);
        this.mwPrivate.tagProducer(POWER_STATUS_PRODUCER, new UintData(SETTINGS, POWER_STATUS, new DataAttributes(new byte[] { 1 }, (byte) 1, (byte) 0, false)));
        this.mwPrivate.tagProducer(CHARGE_STATUS_PRODUCER, new UintData(SETTINGS, CHARGE_STATUS, new DataAttributes(new byte[] { 1 }, (byte) 1, (byte) 0, false)));

        disconnectDummyProducer= new UintData(SETTINGS, DISCONNECT_EVENT, new DataAttributes(new byte[] {}, (byte) 0, (byte) 0, false));
    }

    @Override
    protected void init() {
        readConnParamsTask = new TimedTask<>();
        readAdConfigTask = new TimedTask<>();
        readPowerStatusTask = new TimedTask<>();
        readChargeStatusTask = new TimedTask<>();

        for(byte id: new byte[] {DEVICE_NAME, AD_PARAM, TX_POWER, SCAN_RESPONSE}) {
            this.mwPrivate.addResponseHandler(new Pair<>(SETTINGS.id, Util.setRead(id)), response -> readAdConfigTask.setResult(response));
        }
        if (mwPrivate.lookupModuleInfo(SETTINGS).revision >= CONN_PARAMS_REVISION) {
            this.mwPrivate.addResponseHandler(new Pair<>(SETTINGS.id, Util.setRead(CONNECTION_PARAMS)), response -> readConnParamsTask.setResult(response));
        }
        if (mwPrivate.lookupModuleInfo(SETTINGS).revision >= CHARGE_STATUS_REVISION) {
            this.mwPrivate.addResponseHandler(new Pair<>(SETTINGS.id, Util.setRead(POWER_STATUS)), response -> readPowerStatusTask.setResult(response[2]));
            this.mwPrivate.addResponseHandler(new Pair<>(SETTINGS.id, Util.setRead(CHARGE_STATUS)), response -> readChargeStatusTask.setResult(response[2]));
        }
    }

    @Override
    public void startBleAdvertising() {
        mwPrivate.sendCommand(new byte[] {SETTINGS.id, START_ADVERTISING});
    }

    @Override
    public BleAdvertisementConfigEditor editBleAdConfig() {
        return new BleAdvertisementConfigEditor() {
            private String newAdvName= null;
            private Short newAdvInterval= null;
            private Byte newAdvTimeout= null, newAdvTxPower= null;
            private byte[] newAdvResponse= null;

            @Override
            public BleAdvertisementConfigEditor deviceName(String name) {
                newAdvName = name;
                return this;
            }

            @Override
            public BleAdvertisementConfigEditor interval(short interval) {
                newAdvInterval= interval;
                return this;
            }

            @Override
            public BleAdvertisementConfigEditor timeout(byte timeout) {
                newAdvTimeout = timeout;
                return this;
            }

            @Override
            public BleAdvertisementConfigEditor txPower(byte power) {
                newAdvTxPower = power;
                return this;
            }

            @Override
            public BleAdvertisementConfigEditor scanResponse(byte[] response) {
                newAdvResponse = response;
                return this;
            }

            @Override
            public void commit() {
                if (newAdvName != null) {
                    try {
                        mwPrivate.sendCommand(SETTINGS, DEVICE_NAME, newAdvName.getBytes("US-ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        mwPrivate.sendCommand(SETTINGS, DEVICE_NAME, newAdvName.getBytes());
                    }
                }

                if (newAdvInterval != null || newAdvTimeout != null) {
                    if (newAdvInterval == null) {
                        newAdvInterval = 417;
                    }
                    if (newAdvTimeout == null) {
                        newAdvTimeout = 0;
                    }
                    byte revision = mwPrivate.lookupModuleInfo(SETTINGS).revision;
                    if (revision >= CONN_PARAMS_REVISION) {
                        newAdvInterval = (short) ((newAdvInterval & 0xffff) / AD_INTERVAL_STEP);
                    }
                    ByteBuffer buffer = ByteBuffer.allocate(revision >= WHITELIST_REVISION ? 4 : 3).order(ByteOrder.LITTLE_ENDIAN)
                            .putShort(newAdvInterval).put(newAdvTimeout);
                    if (revision >= WHITELIST_REVISION) {
                        buffer.put((byte) 0);
                    }
                    mwPrivate.sendCommand(SETTINGS, AD_PARAM, buffer.array());
                }

                if (newAdvTxPower != null) {
                    mwPrivate.sendCommand(new byte[] {SETTINGS.id, TX_POWER, newAdvTxPower});
                }

                if (newAdvResponse != null) {
                    if (newAdvResponse.length >= Constant.COMMAND_LENGTH) {
                        byte[] first = new byte[13], second = new byte[newAdvResponse.length - 13];
                        System.arraycopy(newAdvResponse, 0, first, 0, first.length);
                        System.arraycopy(newAdvResponse, first.length, second, 0, second.length);

                        mwPrivate.sendCommand(SETTINGS, PARTIAL_SCAN_RESPONSE, first);
                        mwPrivate.sendCommand(SETTINGS, SCAN_RESPONSE, second);
                    } else {
                        mwPrivate.sendCommand(SETTINGS, SCAN_RESPONSE, newAdvResponse);
                    }
                }
            }
        };
    }

    @Override
    public Task<BleAdvertisementConfig> readBleAdConfigAsync() {
        final Capture<String> deviceName = new Capture<>();
        final Capture<Integer> interval = new Capture<>();
        final Capture<Byte> timeout = new Capture<>(), tx = new Capture<>();

        return readAdConfigTask.execute("Did not receive device name within %dms", Constant.RESPONSE_TIMEOUT,
                () -> mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(DEVICE_NAME)})
        ).onSuccessTask(task -> {
            byte[] response = task.getResult();
            try {
                deviceName.set(new String(response, 2, response.length - 2, "US-ASCII"));
            } catch (UnsupportedEncodingException e) {
                deviceName.set(new String(response, 2, response.length - 2));
            }
            return readAdConfigTask.execute("Did not receive ad parameters within %dms", Constant.RESPONSE_TIMEOUT,
                    () -> mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(AD_PARAM)}));
        }).onSuccessTask(task -> {
            byte[] response = task.getResult();
            if (mwPrivate.lookupModuleInfo(SETTINGS).revision >= CONN_PARAMS_REVISION) {
                int intervalBytes= ((response[2] & 0xff) | (response[3] << 8)) & 0xffff;

                interval.set((int) (intervalBytes * AD_INTERVAL_STEP));
                timeout.set(response[4]);
            } else {
                interval.set(((response[2] & 0xff) | (response[3] << 8)) & 0xffff);
                timeout.set(response[4]);
            }
            return readAdConfigTask.execute("Did not receive tx power within %dms", Constant.RESPONSE_TIMEOUT,
                    () -> mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(TX_POWER)}));
        }).onSuccessTask(task -> {
            tx.set(task.getResult()[2]);
            return readAdConfigTask.execute("Did not receive scan response within %dms", Constant.RESPONSE_TIMEOUT,
                    () -> mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(SCAN_RESPONSE)}));
        }).onSuccessTask(task -> {
            byte[] scanResponse = new byte[task.getResult().length - 2];
            System.arraycopy(task.getResult(), 2, scanResponse, 0, scanResponse.length);

            return Task.forResult(new BleAdvertisementConfig(deviceName.get(), interval.get(), timeout.get(), tx.get(), scanResponse));
        });
    }

    @Override
    public BleConnectionParametersEditor editBleConnParams() {
        if (mwPrivate.lookupModuleInfo(SETTINGS).revision < CONN_PARAMS_REVISION) {
            return null;
        }

        return new BleConnectionParametersEditor() {
            private Short minConnInterval= 6, maxConnInterval= 0x320, slaveLatency= 0, supervisorTimeout= 0x258;

            @Override
            public BleConnectionParametersEditor minConnectionInterval(float interval) {
                minConnInterval= (short) (interval / CONN_INTERVAL_STEP);
                return this;
            }

            @Override
            public BleConnectionParametersEditor maxConnectionInterval(float interval) {
                maxConnInterval= (short) (interval / CONN_INTERVAL_STEP);
                return this;
            }

            @Override
            public BleConnectionParametersEditor slaveLatency(short latency) {
                slaveLatency= latency;
                return this;
            }

            @Override
            public BleConnectionParametersEditor supervisorTimeout(short timeout) {
                supervisorTimeout= (short) (timeout / SUPERVISOR_TIMEOUT_STEP);
                return this;
            }

            @Override
            public void commit() {
                ByteBuffer buffer= ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

                buffer.putShort(minConnInterval).putShort(maxConnInterval).putShort(slaveLatency).putShort(supervisorTimeout);
                mwPrivate.sendCommand(SETTINGS, CONNECTION_PARAMS, buffer.array());
            }
        };
    }

    @Override
    public Task<BleConnectionParameters> readBleConnParamsAsync() {
        if (mwPrivate.lookupModuleInfo(SETTINGS).revision < CONN_PARAMS_REVISION) {
            return Task.forError(new UnsupportedOperationException("Reading BLE connection parameters is not supported on this firmware"));
        }

        return readConnParamsTask.execute("Did not receive connection parameters within %dms", Constant.RESPONSE_TIMEOUT,
                () -> mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(CONNECTION_PARAMS)})
        ).onSuccessTask(task -> {
            final ByteBuffer buffer = ByteBuffer.wrap(task.getResult()).order(ByteOrder.LITTLE_ENDIAN);
            return Task.forResult(new BleConnectionParameters(
                    buffer.getShort(2) * SettingsImpl.CONN_INTERVAL_STEP,
                    buffer.getShort(4) * SettingsImpl.CONN_INTERVAL_STEP,
                    buffer.getShort(6),
                    (short) (buffer.getShort(8) * SettingsImpl.SUPERVISOR_TIMEOUT_STEP)));
        });
    }

    @Override
    public BatteryDataProducer battery() {
        if (mwPrivate.lookupModuleInfo(SETTINGS).revision >= BATTERY_REVISION) {
            return new BatteryDataProducer() {
                @Override
                public String chargeName() {
                    return BATTERY_CHARGE_PRODUCER;
                }

                @Override
                public String voltageName() {
                    return BATTERY_VOLTAGE_PRODUCER;
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, BATTERY_PRODUCER);
                }

                @Override
                public String name() {
                    return BATTERY_PRODUCER;
                }

                @Override
                public void read() {
                    mwPrivate.lookupProducer(BATTERY_PRODUCER).read(mwPrivate);
                }
            };
        }
        return null;
    }

    @Override
    public ActiveDataProducer powerStatus() {
        ModuleInfo info = mwPrivate.lookupModuleInfo(SETTINGS);
        if (info.revision >= CHARGE_STATUS_REVISION && (info.extra.length > 0 && (info.extra[0] & 0x1) == 0x1)) {
            if (powerStatus == null) {
                powerStatus = new ActiveDataProducer() {
                    @Override
                    public Task<Route> addRouteAsync(RouteBuilder builder) {
                        return mwPrivate.queueRouteBuilder(builder, POWER_STATUS_PRODUCER);
                    }

                    @Override
                    public String name() {
                        return POWER_STATUS_PRODUCER;
                    }
                };
            }
            return powerStatus;
        }
        return null;
    }

    @Override
    public Task<Byte> readCurrentPowerStatusAsync() {
        ModuleInfo info = mwPrivate.lookupModuleInfo(SETTINGS);
        if (info.revision >= CHARGE_STATUS_REVISION && (info.extra.length > 0 && (info.extra[0] & 0x1) == 0x1)) {
            return readPowerStatusTask.execute("Did not receive power status within %dms", Constant.RESPONSE_TIMEOUT,
                    () -> mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(POWER_STATUS)}));
        }
        return Task.forError(new UnsupportedOperationException("Reading power status not supported on this board / firmware"));
    }

    @Override
    public ActiveDataProducer chargeStatus() {
        ModuleInfo info = mwPrivate.lookupModuleInfo(SETTINGS);
        if (info.revision >= CHARGE_STATUS_REVISION && (info.extra.length > 0 && (info.extra[0] & 0x2) == 0x2)) {
            if (chargeStatus == null) {
                chargeStatus = new ActiveDataProducer() {
                    @Override
                    public Task<Route> addRouteAsync(RouteBuilder builder) {
                        return mwPrivate.queueRouteBuilder(builder, CHARGE_STATUS_PRODUCER);
                    }

                    @Override
                    public String name() {
                        return CHARGE_STATUS_PRODUCER;
                    }
                };
            }
            return chargeStatus;
        }
        return null;
    }

    @Override
    public Task<Byte> readCurrentChargeStatusAsync() {
        ModuleInfo info = mwPrivate.lookupModuleInfo(SETTINGS);
        if (info.revision >= CHARGE_STATUS_REVISION && (info.extra.length > 0 && (info.extra[0] & 0x2) == 0x2)) {
            return readChargeStatusTask.execute("Did not receive charge status within %dms", Constant.RESPONSE_TIMEOUT,
                    () -> mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(CHARGE_STATUS)}));
        }
        return Task.forError(new UnsupportedOperationException("Reading charge status not supported on this board / firmware"));
    }

    @Override
    public Task<Observer> onDisconnectAsync(CodeBlock codeBlock) {
        if (mwPrivate.lookupModuleInfo(SETTINGS).revision < DISCONNECTED_EVENT_REVISION) {
            return Task.forError(new UnsupportedOperationException("Responding to disconnect events on-board is not supported on this firmware"));
        }
        return mwPrivate.queueEvent(disconnectDummyProducer, codeBlock);
    }

    @Override
    public boolean enable3VRegulator(boolean enable) {
        if (mwPrivate.lookupModuleInfo(SETTINGS).revision >= MMS_REVISION) {
            if (enable) {
                mwPrivate.sendCommand(new byte[] {SETTINGS.id, THREE_VOLT_POWER, 0x01});
            } else {
                mwPrivate.sendCommand(new byte[] {SETTINGS.id, THREE_VOLT_POWER, 0x00});
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean enableForce1MPhy(boolean enable) {
        if (mwPrivate.lookupModuleInfo(SETTINGS).revision >= FORCE_1MPHY_REVISION) {
            if (enable) {
                mwPrivate.sendCommand(new byte[] {SETTINGS.id, FORCE_1MPHY, 0x01});
            } else {
                mwPrivate.sendCommand(new byte[] {SETTINGS.id, FORCE_1MPHY, 0x00});
            }
            return true;
        }
        return false;
    }
}
