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

import com.mbientlab.metawear.CodeBlock;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.Observer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.module.Settings;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

import bolts.Task;
import bolts.TaskCompletionSource;

import static com.mbientlab.metawear.impl.ModuleId.SETTINGS;

/**
 * Created by etsai on 9/20/16.
 */
class SettingsImpl extends ModuleImplBase implements Settings {
    private static final long serialVersionUID = -8845055245623576362L;
    private final static String BATTERY_PRODUCER= "com.mbientlab.metawear.impl.SettingsImpl.BATTERY_PRODUCER",
            BATTERY_CHARGE_PRODUCER= "com.mbientlab.metawear.impl.SettingsImpl.BATTERY_CHARGE_PRODUCER",
            BATTERY_VOLTAGE_PRODUCER= "com.mbientlab.metawear.impl.SettingsImpl.BATTERY_VOLTAGE_PRODUCER";

    private static final byte CONN_PARAMS_REVISION= 1, DISCONNECTED_EVENT_REVISION= 2, BATTERY_REVISION= 3;
    private static final float AD_INTERVAL_STEP= 0.625f, CONN_INTERVAL_STEP= 1.25f, SUPERVISOR_TIMEOUT_STEP= 10f;
    private static final byte DEVICE_NAME = 1, AD_INTERVAL = 2, TX_POWER = 3,
        START_ADVERTISING = 5,
        SCAN_RESPONSE = 7, PARTIAL_SCAN_RESPONSE = 8,
        CONNECTION_PARAMS = 9,
        DISCONNECT_EVENT = 0xa,
        BATTERY_STATE= 0xc;

    private static class AdvertisementConfigBuilder {
        private String deviceName;
        private int interval;
        private short timeout;
        private byte txPower;
        private byte[] scanResponse;

        AdvertisementConfigBuilder setDeviceName(String name) {
            this.deviceName= name;
            return this;
        }

        AdvertisementConfigBuilder setInterval(int interval) {
            this.interval= interval;
            return this;
        }

        AdvertisementConfigBuilder setTimeout(short timeout) {
            this.timeout= timeout;
            return this;
        }

        AdvertisementConfigBuilder setTxPower(byte power) {
            this.txPower= power;
            return this;
        }

        AdvertisementConfigBuilder setScanResponse(byte[] response) {
            this.scanResponse= response;
            return this;
        }

        AdvertisementConfig build() {
            return new Settings.AdvertisementConfig() {
                @Override
                public String deviceName() {
                    return deviceName;
                }

                @Override
                public int interval() {
                    return interval;
                }

                @Override
                public short timeout() {
                    return (short) (timeout & 0xff);
                }

                @Override
                public byte txPower() {
                    return txPower;
                }

                @Override
                public byte[] scanResponse() {
                    return scanResponse;
                }

                @Override
                public String toString() {
                    return String.format(Locale.US, "{Device Name: %s, Adv Interval: %d, Adv Timeout: %d, Tx Power: %d, Scan Response: %s}",
                            deviceName(), interval(), timeout(), txPower(), Util.arrayToHexString(scanResponse));
                }
            };
        }
    }
    private static class BatteryStateData extends DataTypeBase {
        private static final long serialVersionUID = -1080271339658673808L;

        BatteryStateData() {
            super(SETTINGS, Util.setSilentRead(BATTERY_STATE), new DataAttributes(new byte[] {1, 2}, (byte) 1, (byte) 0, false));
        }

        BatteryStateData(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            super(input, module, register, id, attributes);
        }

        @Override
        public DataTypeBase copy(DataTypeBase input, ModuleId module, byte register, byte id, DataAttributes attributes) {
            return new BatteryStateData(input, module, register, id, attributes);
        }

        @Override
        public Number convertToFirmwareUnits(MetaWearBoardPrivate owner, Number input) {
            return input;
        }

        @Override
        public Data createMessage(boolean logData, MetaWearBoardPrivate owner, final byte[] data, final Calendar timestamp) {
            final short voltage= ByteBuffer.wrap(data, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            final BatteryState state= new BatteryState(data[0], voltage);

            return new DataPrivate(timestamp, data) {
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
                    new UintData(SETTINGS, eventConfig[1], eventConfig[2], new DataAttributes(new byte[] {2}, (byte) 1, (byte) 1, false))
            };
        }
    }

    private DataTypeBase disconnectDummyProducer;
    private transient AdvertisementConfigBuilder adConfigBuilder;
    private transient ScheduledFuture<?> timeoutFuture, adConfigTimeoutFuture;
    private transient Queue<TaskCompletionSource<ConnectionParameters>> readConnParamsTaskSources;
    private transient Queue<TaskCompletionSource<AdvertisementConfig>> readConfigTaskSources;
    private transient Runnable taskTimeout, readAdConfigTaskTimeout;

    SettingsImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        DataTypeBase batteryProducer = new BatteryStateData();
        this.mwPrivate.tagProducer(BATTERY_PRODUCER, batteryProducer);
        this.mwPrivate.tagProducer(BATTERY_CHARGE_PRODUCER, batteryProducer.split[0]);
        this.mwPrivate.tagProducer(BATTERY_VOLTAGE_PRODUCER, batteryProducer.split[1]);

        disconnectDummyProducer= new UintData(SETTINGS, DISCONNECT_EVENT, new DataAttributes(new byte[] {}, (byte) 0, (byte) 0, false));
    }

    @Override
    protected void init() {
        readConnParamsTaskSources = new ConcurrentLinkedQueue<>();
        readConfigTaskSources = new ConcurrentLinkedQueue<>();
        taskTimeout= new Runnable() {
            @Override
            public void run() {
                readConnParamsTaskSources.poll().setError(new TimeoutException("Reading connection parameters timed out"));
                readConnectionParameters(true);
            }
        };
        readAdConfigTaskTimeout= new Runnable() {
            @Override
            public void run() {
                readConfigTaskSources.poll().setError(new TimeoutException("Reading advertising configuration timed out"));
                readAdConfig(true);
            }
        };

        this.mwPrivate.addResponseHandler(new Pair<>(SETTINGS.id, Util.setRead(DEVICE_NAME)), new MetaWearBoardImpl.RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                byte[] respBody= new byte[response.length - 2];
                System.arraycopy(response, 2, respBody, 0, respBody.length);

                adConfigBuilder= new AdvertisementConfigBuilder();
                try {
                    adConfigBuilder.setDeviceName(new String(respBody, "US-ASCII"));
                } catch (UnsupportedEncodingException e) {
                    adConfigBuilder.setDeviceName(new String(respBody));
                }
                SettingsImpl.this.mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(AD_INTERVAL)});
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(SETTINGS.id, Util.setRead(TX_POWER)), new MetaWearBoardImpl.RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                adConfigBuilder.setTxPower(response[2]);
                SettingsImpl.this.mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(SCAN_RESPONSE)});
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(SETTINGS.id, Util.setRead(SCAN_RESPONSE)), new MetaWearBoardImpl.RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                adConfigTimeoutFuture.cancel(false);

                byte[] adScanResponse = new byte[response.length - 2];
                System.arraycopy(response, 2, adScanResponse, 0, adScanResponse.length);
                adConfigBuilder.setScanResponse(adScanResponse);

                readConfigTaskSources.poll().setResult(adConfigBuilder.build());
                readAdConfig(true);
            }
        });

        if (mwPrivate.lookupModuleInfo(SETTINGS).revision >= CONN_PARAMS_REVISION) {
            this.mwPrivate.addResponseHandler(new Pair<>(SETTINGS.id, Util.setRead(AD_INTERVAL)), new MetaWearBoardImpl.RegisterResponseHandler() {
                @Override
                public void onResponseReceived(byte[] response) {
                    int intervalBytes= ((response[2] & 0xff) | (response[3] << 8)) & 0xffff;

                    adConfigBuilder.setInterval((int) (intervalBytes * AD_INTERVAL_STEP));
                    adConfigBuilder.setTimeout(response[4]);

                    SettingsImpl.this.mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(TX_POWER)});
                }
            });

            this.mwPrivate.addResponseHandler(new Pair<>(SETTINGS.id, Util.setRead(CONNECTION_PARAMS)), new MetaWearBoardImpl.RegisterResponseHandler() {
                @Override
                public void onResponseReceived(byte[] response) {
                    timeoutFuture.cancel(false);
                    final ByteBuffer buffer = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN);

                    readConnParamsTaskSources.poll().setResult(new Settings.ConnectionParameters() {
                        @Override
                        public float minConnectionInterval() {
                            return buffer.getShort(2) * SettingsImpl.CONN_INTERVAL_STEP;
                        }

                        @Override
                        public float maxConnectionInterval() {
                            return buffer.getShort(4) * SettingsImpl.CONN_INTERVAL_STEP;
                        }

                        @Override
                        public short slaveLatency() {
                            return buffer.getShort(6);
                        }

                        @Override
                        public short supervisorTimeout() {
                            return (short) (buffer.getShort(8) * SettingsImpl.SUPERVISOR_TIMEOUT_STEP);
                        }

                        @Override
                        public String toString() {
                            return String.format(Locale.US, "{min conn interval: %.2f, max conn interval: %.2f, slave latency: %d, supervisor timeout: %d}",
                                    minConnectionInterval(), maxConnectionInterval(), slaveLatency(), supervisorTimeout());
                        }
                    });

                    readConnectionParameters(true);
                }
            });
        } else {
            this.mwPrivate.addResponseHandler(new Pair<>(SETTINGS.id, Util.setRead(AD_INTERVAL)), new MetaWearBoardImpl.RegisterResponseHandler() {
                @Override
                public void onResponseReceived(byte[] response) {
                    adConfigBuilder.setInterval((((response[2] & 0xff) | (response[3] << 8)) & 0xffff));
                    adConfigBuilder.setTimeout(response[4]);

                    SettingsImpl.this.mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(TX_POWER)});
                }
            });
        }
    }

    @Override
    public ConnectionParametersEditor configureConnectionParameters() {
        return new ConnectionParametersEditor() {
            private Short minConnInterval= 6, maxConnInterval= 0x320, slaveLatency= 0, supervisorTimeout= 0x258;

            @Override
            public ConnectionParametersEditor minConnectionInterval(float interval) {
                minConnInterval= (short) (interval / CONN_INTERVAL_STEP);
                return this;
            }

            @Override
            public ConnectionParametersEditor maxConnectionInterval(float interval) {
                maxConnInterval= (short) (interval / CONN_INTERVAL_STEP);
                return this;
            }

            @Override
            public ConnectionParametersEditor slaveLatency(short latency) {
                slaveLatency= latency;
                return this;
            }

            @Override
            public ConnectionParametersEditor supervisorTimeout(short timeout) {
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
    public Task<ConnectionParameters> readConnectionParameters() {
        TaskCompletionSource<ConnectionParameters> taskSource= new TaskCompletionSource<>();
        if (mwPrivate.lookupModuleInfo(SETTINGS).revision < CONN_PARAMS_REVISION) {
            taskSource.setError(new UnsupportedOperationException("Connection parameters not supported on this firmware"));
        } else {
            readConnParamsTaskSources.add(taskSource);
            readConnectionParameters(false);
        }

        return taskSource.getTask();
    }

    @Override
    public AdvertisementConfigEditor configure() {
        return new AdvertisementConfigEditor() {
            private String newAdvName= null;
            private Short newAdvInterval= null;
            private Byte newAdvTimeout= null, newAdvTxPower= null;
            private byte[] newAdvResponse= null;

            @Override
            public AdvertisementConfigEditor deviceName(String name) {
                newAdvName = name;
                return this;
            }

            @Override
            public AdvertisementConfigEditor adInterval(short interval, byte timeout) {
                newAdvInterval= interval;
                newAdvTimeout = timeout;
                return this;
            }

            @Override
            public AdvertisementConfigEditor txPower(byte power) {
                newAdvTxPower = power;
                return this;
            }

            @Override
            public AdvertisementConfigEditor scanResponse(byte[] response) {
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

                if (newAdvInterval != null) {
                    if (mwPrivate.lookupModuleInfo(SETTINGS).revision >= CONN_PARAMS_REVISION) {
                        newAdvInterval = (short) ((newAdvInterval & 0xffff) / AD_INTERVAL_STEP);
                    }
                    mwPrivate.sendCommand(SETTINGS, AD_INTERVAL,
                            ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN).putShort(newAdvInterval).put(newAdvTimeout).array());
                }

                if (newAdvTxPower != null) {
                    mwPrivate.sendCommand(new byte[] {SETTINGS.id, TX_POWER, newAdvTxPower});
                }

                if (newAdvResponse != null) {
                    if (newAdvResponse.length >= Constant.MW_COMMAND_LENGTH) {
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
    public Task<AdvertisementConfig> readAdConfig() {
        TaskCompletionSource<AdvertisementConfig> taskSource= new TaskCompletionSource<>();

        readConfigTaskSources.add(taskSource);
        readAdConfig(false);

        return taskSource.getTask();
    }

    @Override
    public void startAdvertisement() {
        mwPrivate.sendCommand(new byte[] {SETTINGS.id, START_ADVERTISING});
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
                public Task<Route> addRoute(RouteBuilder builder) {
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
    public Task<Observer> onDisconnect(CodeBlock codeBlock) {
        if (mwPrivate.lookupModuleInfo(SETTINGS).revision < DISCONNECTED_EVENT_REVISION) {
            TaskCompletionSource<Observer> error= new TaskCompletionSource<>();
            error.setError(new UnsupportedOperationException("Responding to disconnect events on-board is not supported on this firmware"));
            return error.getTask();
        }
        return mwPrivate.queueEvent(disconnectDummyProducer, codeBlock);
    }

    private void readConnectionParameters(boolean ready) {
        if (!readConnParamsTaskSources.isEmpty() && (ready || readConnParamsTaskSources.size() == 1)) {
            mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(CONNECTION_PARAMS)});
            timeoutFuture= mwPrivate.scheduleTask(taskTimeout, 250L);
        }
    }

    private void readAdConfig(boolean ready) {
        if (!readConfigTaskSources.isEmpty() && (ready || readConfigTaskSources.size() == 1)) {
            mwPrivate.sendCommand(new byte[] {SETTINGS.id, Util.setRead(DEVICE_NAME)});
            adConfigTimeoutFuture= mwPrivate.scheduleTask(readAdConfigTaskTimeout, 1800L);
        }
    }
}
