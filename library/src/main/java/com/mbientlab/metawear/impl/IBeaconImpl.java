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

import com.mbientlab.metawear.DataToken;
import com.mbientlab.metawear.module.IBeacon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.UUID;

import bolts.Task;

import static com.mbientlab.metawear.impl.ModuleId.IBEACON;

/**
 * Created by etsai on 9/18/16.
 */
class IBeaconImpl extends ModuleImplBase implements IBeacon {
    private static final byte ENABLE = 0x1, AD_UUID = 0x2, MAJOR = 0x3, MINOR = 0x4,
        RX = 0x5, TX = 0x6, PERIOD = 0x7;
    private static final long serialVersionUID = 5027360264544753193L;

    private static class ConfigurationBuilder {
        private UUID uuid;
        private short major, minor, period;
        private byte rx, tx;

        ConfigurationBuilder setUuid(UUID uuid) {
            this.uuid= uuid;
            return this;
        }

        ConfigurationBuilder setMajor(short major) {
            this.major= major;
            return this;
        }

        ConfigurationBuilder setMinor(short minor) {
            this.minor= minor;
            return this;
        }

        ConfigurationBuilder setRx(byte rx) {
            this.rx= rx;
            return this;
        }

        ConfigurationBuilder setTx(byte tx) {
            this.tx= tx;
            return this;
        }

        ConfigurationBuilder setPeriod(short period) {
            this.period= period;
            return this;
        }

        Configuration build() {
            return new Configuration() {
                @Override
                public UUID uuid() {
                    return uuid;
                }

                @Override
                public short major() {
                    return major;
                }

                @Override
                public short minor() {
                    return minor;
                }

                @Override
                public byte rxPower() {
                    return rx;
                }

                @Override
                public byte txPower() {
                    return tx;
                }

                @Override
                public short period() {
                    return period;
                }

                @Override
                public String toString() {
                    return String.format(Locale.US, "{uuid: %s, major: %d, minor: %d, rx: %d, tx: %d, delay: %d}",
                            uuid(), major(), minor(), rxPower(), txPower(), period());
                }
            };
        }
    }

    private transient ConfigurationBuilder builder;
    private transient AsyncTaskManager<Configuration> readConfigTasks;

    IBeaconImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
    }

    @Override
    protected void init() {
        readConfigTasks = new AsyncTaskManager<>(mwPrivate, "Reading IBeacon configuration timed out");

        this.mwPrivate.addResponseHandler(new Pair<>(IBEACON.id, Util.setRead(AD_UUID)), new MetaWearBoardImpl.RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                builder= new ConfigurationBuilder();
                builder.setUuid(new UUID(ByteBuffer.wrap(response, 10, 8).order(ByteOrder.LITTLE_ENDIAN).getLong(),
                        ByteBuffer.wrap(response, 2, 8).order(ByteOrder.LITTLE_ENDIAN).getLong()));
                IBeaconImpl.this.mwPrivate.sendCommand(new byte[] {IBEACON.id, Util.setRead(MAJOR)});
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(IBEACON.id, Util.setRead(MAJOR)), new MetaWearBoardImpl.RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                builder.setMajor(ByteBuffer.wrap(response, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
                IBeaconImpl.this.mwPrivate.sendCommand(new byte[] {IBEACON.id, Util.setRead(MINOR)});
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(IBEACON.id, Util.setRead(MINOR)), new MetaWearBoardImpl.RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                builder.setMinor(ByteBuffer.wrap(response, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
                IBeaconImpl.this.mwPrivate.sendCommand(new byte[] {IBEACON.id, Util.setRead(RX)});
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(IBEACON.id, Util.setRead(RX)), new MetaWearBoardImpl.RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                builder.setRx(response[2]);
                IBeaconImpl.this.mwPrivate.sendCommand(new byte[] {IBEACON.id, Util.setRead(TX)});
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(IBEACON.id, Util.setRead(TX)), new MetaWearBoardImpl.RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                builder.setTx(response[2]);
                IBeaconImpl.this.mwPrivate.sendCommand(new byte[] {IBEACON.id, Util.setRead(PERIOD)});
            }
        });
        this.mwPrivate.addResponseHandler(new Pair<>(IBEACON.id, Util.setRead(PERIOD)), new MetaWearBoardImpl.RegisterResponseHandler() {
            @Override
            public void onResponseReceived(byte[] response) {
                readConfigTasks.cancelTimeout();

                builder.setPeriod(ByteBuffer.wrap(response, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
                readConfigTasks.setResult(builder.build());
            }
        });
    }

    @Override
    public ConfigEditor configure() {
        return new ConfigEditor() {
            private UUID newUuid= null;
            private Short newMajor= null, newMinor= null, newPeriod = null;
            private Byte newRxPower= null, newTxPower= null;
            private DataToken majorToken = null, newMinorDataToken = null;

            @Override
            public ConfigEditor setUUID(UUID adUuid) {
                newUuid = adUuid;
                return this;
            }

            @Override
            public ConfigEditor setMajor(short major) {
                newMajor = major;
                return this;
            }

            @Override
            public ConfigEditor setMajor(DataToken major) {
                majorToken = major;
                return this;
            }

            @Override
            public ConfigEditor setMinor(short minor) {
                newMinor = minor;
                return this;
            }

            @Override
            public ConfigEditor setMinor(DataToken minor) {
                newMinorDataToken = minor;
                return this;
            }

            @Override
            public ConfigEditor setRxPower(byte power) {
                newRxPower = power;
                return this;
            }

            @Override
            public ConfigEditor setTxPower(byte power) {
                newTxPower = power;
                return this;
            }

            @Override
            public ConfigEditor setAdPeriod(short period) {
                newPeriod = period;
                return this;
            }

            @Override
            public void commit() {
                if (newUuid != null) {
                    byte[] uuidBytes = ByteBuffer.allocate(16)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putLong(newUuid.getLeastSignificantBits())
                            .putLong(newUuid.getMostSignificantBits())
                            .array();
                    IBeaconImpl.this.mwPrivate.sendCommand(IBEACON, AD_UUID, uuidBytes);
                }

                if (newMajor != null) {
                    IBeaconImpl.this.mwPrivate.sendCommand(IBEACON, MAJOR,
                            ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(newMajor).array());
                } else if (majorToken != null) {
                    ByteBuffer buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                            .put(IBEACON.id)
                            .put(MAJOR)
                            .putShort((short) 0);
                    IBeaconImpl.this.mwPrivate.sendCommand(buffer.array(), 0, majorToken);
                }

                if (newMinor != null) {
                    IBeaconImpl.this.mwPrivate.sendCommand(IBEACON, MINOR,
                            ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(newMinor).array());
                } else if (newMinorDataToken != null) {
                    ByteBuffer buffer= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                            .put(IBEACON.id)
                            .put(MINOR)
                            .putShort((short) 0);
                    IBeaconImpl.this.mwPrivate.sendCommand(buffer.array(), 0, majorToken);
                }

                if (newRxPower != null) {
                    IBeaconImpl.this.mwPrivate.sendCommand(new byte[] {IBEACON.id, RX, newRxPower});
                }

                if (newTxPower != null) {
                    IBeaconImpl.this.mwPrivate.sendCommand(new byte[] {IBEACON.id, TX, newTxPower});
                }

                if (newPeriod != null) {
                    IBeaconImpl.this.mwPrivate.sendCommand(IBEACON, PERIOD,
                            ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(newPeriod).array());
                }
            }
        };
    }

    @Override
    public void enable() {
        mwPrivate.sendCommand(new byte[] {IBEACON.id, ENABLE, (byte) 1});
    }

    @Override
    public void disable() {
        mwPrivate.sendCommand(new byte[] {IBEACON.id, ENABLE, (byte) 0});
    }

    @Override
    public Task<Configuration> readConfiguration() {
        return readConfigTasks.queueTask(1500L, new Runnable() {
            @Override
            public void run() {
                mwPrivate.sendCommand(new byte[] {IBEACON.id, Util.setRead(AD_UUID)});
            }
        });
    }
}
