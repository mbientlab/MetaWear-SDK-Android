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

import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.impl.JseMetaWearBoard.RegisterResponseHandler;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Logging;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

import static com.mbientlab.metawear.impl.Constant.Module.DATA_PROCESSOR;

/**
 * Created by etsai on 10/27/16.
 */

class StreamedDataConsumer extends DeviceDataConsumer {
    private static final long serialVersionUID = 7339116325296045121L;

    private transient RegisterResponseHandler dataResponseHandler= null;

    StreamedDataConsumer(DataTypeBase source, Subscriber subscriber) {
        super(source, subscriber);
    }

    public void enableStream(final MetaWearBoardPrivate mwPrivate) {
        addDataHandler(mwPrivate);

        if ((source.eventConfig[1] & 0x80) == 0x0) {
            if (source.eventConfig[2] == DataTypeBase.NO_DATA_ID) {
                if (mwPrivate.numDataHandlers(source.eventConfigAsTuple()) == 1) {
                    mwPrivate.sendCommand(new byte[]{source.eventConfig[0], source.eventConfig[1], 0x1});
                }
            } else {
                mwPrivate.sendCommand(new byte[] {source.eventConfig[0], source.eventConfig[1], 0x1});
                if (mwPrivate.numDataHandlers(source.eventConfigAsTuple()) == 1) {
                    if (source.eventConfig[0] == DATA_PROCESSOR.id && source.eventConfig[1] == DataProcessorImpl.NOTIFY) {
                        mwPrivate.sendCommand(new byte[]{source.eventConfig[0], DataProcessorImpl.NOTIFY_ENABLE, source.eventConfig[2], 0x1});
                    }
                }
            }
        } else {
            source.markLive();
        }
    }

    public void disableStream(MetaWearBoardPrivate mwPrivate) {
        if ((source.eventConfig[1] & 0x80) == 0x0) {
            if (source.eventConfig[2] == DataTypeBase.NO_DATA_ID) {
                if (mwPrivate.numDataHandlers(source.eventConfigAsTuple()) == 1) {
                    mwPrivate.sendCommand(new byte[]{source.eventConfig[0], source.eventConfig[1], 0x0});
                }
            } else {
                if (mwPrivate.numDataHandlers(source.eventConfigAsTuple()) == 1) {
                    if (source.eventConfig[0] == DATA_PROCESSOR.id && source.eventConfig[1] == DataProcessorImpl.NOTIFY) {
                        mwPrivate.sendCommand(new byte[]{source.eventConfig[0], DataProcessorImpl.NOTIFY_ENABLE, source.eventConfig[2], 0x0});
                    }
                }
            }
        } else {
            if (mwPrivate.numDataHandlers(source.eventConfigAsTuple()) == 1) {
                source.markSilent();
            }
        }

        mwPrivate.removeDataHandler(source.eventConfigAsTuple(), dataResponseHandler);
    }

    public void addDataHandler(final MetaWearBoardPrivate mwPrivate) {
        if (source.eventConfig[2] != DataTypeBase.NO_DATA_ID) {
            mwPrivate.addDataIdHeader(new Pair<>(source.eventConfig[0], source.eventConfig[1]));
        }
        if (dataResponseHandler == null) {
            if (source.attributes.copies > 1) {
                final byte dataUnitLength = source.attributes.unitLength();
                dataResponseHandler = response -> {
                    Calendar now = Calendar.getInstance();
                    DataProcessorImpl.Processor accounter = findParent((DataProcessorImpl) mwPrivate.getModules().get(DataProcessor.class), source, DataProcessorImpl.TYPE_ACCOUNTER);
                    RouteComponent.AccountType accountType = accounter == null ? RouteComponent.AccountType.TIME : ((DataProcessorConfig.Accounter) accounter.editor.configObj).type;
                    for(int i = 0, j = source.eventConfig[2] == DataTypeBase.NO_DATA_ID ? 2 : 3; i< source.attributes.copies && j < response.length; i++, j+= dataUnitLength) {
                        Tuple3<Calendar, Integer, Long> account = fillTimestamp(mwPrivate, accounter, response, j);
                        byte[] dataRaw = new byte[dataUnitLength - (account.second - j)];
                        System.arraycopy(response, account.second, dataRaw, 0, dataRaw.length);
                        call(source.createMessage(false, mwPrivate, dataRaw, accounter == null ? now : account.first, accountType == RouteComponent.AccountType.TIME ? null : clazz ->
                                clazz.equals(Long.class) ? account.third : null)
                        );
                    }
                };
            } else {
                dataResponseHandler = response -> {
                    byte[] dataRaw;

                    if (source.eventConfig[2] == DataTypeBase.NO_DATA_ID) {
                        dataRaw = new byte[response.length - 2];
                        System.arraycopy(response, 2, dataRaw, 0, dataRaw.length);
                    } else {
                        dataRaw = new byte[response.length - 3];
                        System.arraycopy(response, 3, dataRaw, 0, dataRaw.length);
                    }

                    RouteComponent.AccountType accountType = RouteComponent.AccountType.TIME;
                    Tuple3<Calendar, Integer, Long> account;
                    if (source.eventConfig[0] == DATA_PROCESSOR.id && source.eventConfig[1] == DataProcessorImpl.NOTIFY) {
                        DataProcessorImpl dataprocessor = (DataProcessorImpl) mwPrivate.getModules().get(DataProcessor.class);
                        DataProcessorConfig config = dataprocessor.lookupProcessor(source.eventConfig[2]).editor.configObj;
                        account = fillTimestamp(mwPrivate, dataprocessor.lookupProcessor(source.eventConfig[2]), dataRaw, 0);

                        if (account.second > 0) {
                            byte[] copy = new byte[dataRaw.length - account.second];
                            System.arraycopy(dataRaw, account.second, copy, 0, copy.length);
                            dataRaw = copy;
                            accountType = ((DataProcessorConfig.Accounter) config).type;
                        }
                    } else {
                        account = new Tuple3<>(Calendar.getInstance(), 0, 0L);
                    }

                    DataProcessorImpl.Processor packer = findParent((DataProcessorImpl) mwPrivate.getModules().get(DataProcessor.class), source, DataProcessorImpl.TYPE_PACKER);
                    if (packer != null) {
                        final byte dataUnitLength = packer.editor.source.attributes.unitLength();
                        byte[] unpacked = new byte[dataUnitLength];
                        for(int i = 0, j = 3 + account.second; i< packer.editor.source.attributes.copies && j < response.length; i++, j+= dataUnitLength) {
                            System.arraycopy(response, j, unpacked, 0, unpacked.length);
                            call(source.createMessage(false, mwPrivate, unpacked, account.first, accountType == RouteComponent.AccountType.TIME ? null : clazz ->
                                    clazz.equals(Long.class) ? account.third : null)
                            );
                        }
                    } else {
                        call(source.createMessage(false, mwPrivate, dataRaw, account.first, accountType == RouteComponent.AccountType.TIME ? null : clazz ->
                                clazz.equals(Long.class) ? account.third : null)
                        );
                    }
                };
            }
        }

        mwPrivate.addDataHandler(source.eventConfigAsTuple(), dataResponseHandler);
    }

    private static DataProcessorImpl.Processor findParent(DataProcessorImpl dataprocessor, DataTypeBase child, byte type) {
        if (child.eventConfig[0] == DATA_PROCESSOR.id && child.eventConfig[1] == DataProcessorImpl.NOTIFY) {
            DataProcessorImpl.Processor processor = dataprocessor.lookupProcessor(child.eventConfig[2]);
            if (processor.editor.config[0] == type) {
                return processor;
            }

            return findParent(dataprocessor, child.input, type);
        }
        return null;
    }

    private static Tuple3<Calendar, Integer, Long> fillTimestamp(MetaWearBoardPrivate mwPrivate, DataProcessorImpl.Processor accounter, byte[] response, int offset) {
        if (accounter != null) {
            DataProcessorConfig config = accounter.editor.configObj;
            if (config instanceof DataProcessorConfig.Accounter) {
                int size = ((DataProcessorConfig.Accounter) config).length;
                byte[] padded = new byte[8];
                System.arraycopy(response, offset, padded, 0, size);
                long tick = ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).getLong(0);

                switch(((DataProcessorConfig.Accounter) config).type) {
                    case COUNT: {
                        return new Tuple3<>(Calendar.getInstance(), size + offset, tick);
                    }
                    case TIME: {
                        LoggingImpl logging = (LoggingImpl) mwPrivate.getModules().get(Logging.class);
                        return new Tuple3<>(logging.computeTimestamp((byte) -1, tick), size + offset, tick);
                    }
                }
            }
        }
        return new Tuple3<>(Calendar.getInstance(), offset, 0L);
    }
}
