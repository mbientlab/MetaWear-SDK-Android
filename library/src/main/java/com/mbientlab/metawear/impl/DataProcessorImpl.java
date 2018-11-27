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

import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.impl.platform.TimedTask;
import com.mbientlab.metawear.module.DataProcessor;

import java.io.Serializable;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import bolts.Capture;
import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.DATA_PROCESSOR;

/**
 * Created by etsai on 9/5/16.
 */
class DataProcessorImpl extends ModuleImplBase implements DataProcessor {
    static String createUri(DataTypeBase dataType, DataProcessorImpl dataprocessor, Version firmware, byte revision) {
        byte register = Util.clearRead(dataType.eventConfig[1]);
        switch (register) {
            case NOTIFY:
            case STATE:
                Processor processor = dataprocessor.lookupProcessor(dataType.eventConfig[2]);
                DataProcessorConfig config = DataProcessorConfig.from(firmware, revision, processor.editor.config);

                return config.createUri(register == STATE, dataType.eventConfig[2]);
            default:
                return null;
        }
    }

    private static final long serialVersionUID = -7439066046235167486L;
    static final byte TIME_PASSTHROUGH_REVISION = 1, ENHANCED_STREAMING_REVISION = 2, HPF_REVISION = 2, EXPANDED_DELAY = 2, FUSE_REVISION = 3;
    static final byte TYPE_ACCOUNTER = 0x11, TYPE_PACKER = 0x10;

    static abstract class EditorImplBase implements Editor, Serializable {
        private static final long serialVersionUID = 4723697652659135045L;

        public byte[] config;
        public final DataTypeBase source;

        transient DataProcessorConfig configObj;
        protected transient MetaWearBoardPrivate mwPrivate;

        EditorImplBase(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            this.configObj = configObj;
            this.config= configObj.build();
            this.source= source;
            this.mwPrivate = mwPrivate;
        }

        void restoreTransientVars(MetaWearBoardPrivate mwPrivate) {
            this.mwPrivate = mwPrivate;
            configObj = DataProcessorConfig.from(mwPrivate.getFirmwareVersion(), mwPrivate.lookupModuleInfo(Constant.Module.DATA_PROCESSOR).revision, config);
        }
    }
    static class NullEditor extends EditorImplBase {
        private static final long serialVersionUID = -6221412334731005999L;

        NullEditor(DataProcessorConfig configObj, DataTypeBase source, MetaWearBoardPrivate mwPrivate) {
            super(configObj, source, mwPrivate);
        }
    }

    static class Processor implements Serializable {
        private static final long serialVersionUID = -156262560425526526L;

        public final DataTypeBase state;
        public final EditorImplBase editor;

        Processor(DataTypeBase state, Editor editor) {
            this.editor= (EditorImplBase) editor;
            this.state= state;
        }
    }

    static class ProcessorEntry {
        byte id, offset, length;
        byte[] source;
        byte[] config;
    }

    static final byte ADD= 2,
        NOTIFY = 3,
        STATE = 4,
        PARAMETER = 5,
        REMOVE = 6,
        NOTIFY_ENABLE = 7,
        REMOVE_ALL = 8;

    final Map<Byte, Processor> activeProcessors= new HashMap<>();
    final Map<String, Byte> nameToIdMapping = new HashMap<>();

    private transient TimedTask<byte[]> pullProcessorConfigTask, createProcessorTask;

    DataProcessorImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
    }

    @Override
    void restoreTransientVars(MetaWearBoardPrivate mwPrivate) {
        super.restoreTransientVars(mwPrivate);

        for(Processor it: activeProcessors.values()) {
            it.editor.restoreTransientVars(mwPrivate);
        }
    }

    protected void init() {
        pullProcessorConfigTask = new TimedTask<>();
        createProcessorTask = new TimedTask<>();

        this.mwPrivate.addResponseHandler(new Pair<>(DATA_PROCESSOR.id, Util.setRead(ADD)), response -> pullProcessorConfigTask.setResult(response));
        this.mwPrivate.addResponseHandler(new Pair<>(DATA_PROCESSOR.id, ADD), response -> createProcessorTask.setResult(response));
    }

    void removeProcessor(boolean sync, byte id) {
        if (sync) {
            Processor target = activeProcessors.get(id);
            mwPrivate.sendCommand(new byte[]{DATA_PROCESSOR.id, DataProcessorImpl.REMOVE, target.editor.source.eventConfig[2]});
        }

        activeProcessors.remove(id);
    }
    public void tearDown() {
        activeProcessors.clear();
        nameToIdMapping.clear();
        mwPrivate.sendCommand(new byte[] {DATA_PROCESSOR.id, REMOVE_ALL});
    }

    Task<Queue<Byte>> queueDataProcessors(Queue<Processor> pendingProcessors) {
        final Queue<Byte> ids = new LinkedList<>();
        final Capture<Boolean> terminate = new Capture<>(false);

        return Task.forResult(null).continueWhile(() -> !terminate.get() && !pendingProcessors.isEmpty(), ignored -> {
            final Processor current= pendingProcessors.poll();
            DataTypeBase input= current.editor.source.input;

            if (current.editor.configObj instanceof DataProcessorConfig.Fuser) {
                ((DataProcessorConfig.Fuser) current.editor.configObj).syncFilterIds(this);
            }

            final byte[] filterConfig= new byte[input.eventConfig.length + 1 + current.editor.config.length];
            filterConfig[input.eventConfig.length]= (byte) (((input.attributes.length() - 1) << 5) | input.attributes.offset);
            System.arraycopy(input.eventConfig, 0, filterConfig, 0, input.eventConfig.length);
            System.arraycopy(current.editor.config, 0, filterConfig, input.eventConfig.length + 1, current.editor.config.length);

            return createProcessorTask.execute("Did not receive data processor id within %dms", Constant.RESPONSE_TIMEOUT,
                    () -> mwPrivate.sendCommand(DATA_PROCESSOR, ADD, filterConfig)
            ).continueWithTask(task -> {
                if (task.isFaulted()) {
                    terminate.set(true);
                    return Task.<Void>forError(task.getError());
                }
                byte id = task.getResult()[2];
                current.editor.source.eventConfig[2]= id;
                if (current.state != null) {
                    current.state.eventConfig[2] = id;
                }
                activeProcessors.put(id, current);
                ids.add(id);

                return Task.forResult(null);
            });
        }).continueWithTask(task -> {
            if (task.isFaulted()) {
                for(byte it: ids) {
                    removeProcessor(true, it);
                }
                return Task.forError(task.getError());
            }
            return Task.forResult(ids);
        });
    }

    @Override
    public <T extends Editor> T edit(String name, Class<T> editorClass) {
        return editorClass.cast(activeProcessors.get(nameToIdMapping.get(name)).editor);
    }

    @Override
    public ForcedDataProducer state(final String name) {
        try {
            DataTypeBase state = activeProcessors.get(nameToIdMapping.get(name)).state;

            if (state != null) {
                ForcedDataProducer stateProducer = new ForcedDataProducer() {
                    @Override
                    public Task<Route> addRouteAsync(RouteBuilder builder) {
                        return mwPrivate.queueRouteBuilder(builder, name());
                    }

                    @Override
                    public String name() {
                        return String.format(Locale.US, "%s_state", name);
                    }

                    @Override
                    public void read() {
                        mwPrivate.lookupProducer(name()).read(mwPrivate);
                    }
                };
                mwPrivate.tagProducer(stateProducer.name(), state);
                return stateProducer;
            }
            return null;
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    void assignNameToId(Map<String, Processor> taggedProcessors) {
        for(Map.Entry<String, Processor> it: taggedProcessors.entrySet()) {
            nameToIdMapping.put(it.getKey(), it.getValue().editor.source.eventConfig[2]);
        }
    }

    Processor lookupProcessor(byte id) {
        return activeProcessors.get(id);
    }

    void addProcessor(byte id, DataTypeBase state, DataTypeBase source, DataProcessorConfig config) {
        activeProcessors.put(id, new Processor(state, new NullEditor(config, source, mwPrivate)));
    }

    Task<Deque<ProcessorEntry>> pullChainAsync(byte id) {
        final Capture<Boolean> terminate = new Capture<>(false);
        final Deque<ProcessorEntry> result = new LinkedList<>();
        final Capture<Byte> nextId = new Capture<>(id);

        return Task.forResult(null).continueWhile(() -> !terminate.get(), ignored ->
            pullProcessorConfigTask.execute("Did not received data processor config within %dms", Constant.RESPONSE_TIMEOUT,
                    () -> mwPrivate.sendCommand(new byte[] {DATA_PROCESSOR.id, Util.setRead(ADD), nextId.get()})
        ).continueWithTask(task -> {
            if (task.isFaulted()) {
                terminate.set(true);
                return Task.<Void>forError(task.getError());
            }

            byte[] response = task.getResult();

            ProcessorEntry entry = new ProcessorEntry();
            entry.id = nextId.get();
            entry.offset = (byte) (response[5] & 0x1f);
            entry.length = (byte) (((response[5] >> 5) & 0x7) + 1);

            entry.source = new byte[3];
            System.arraycopy(response, 2, entry.source, 0, entry.source.length);

            entry.config = new byte[response.length - 6];
            System.arraycopy(response, 6, entry.config, 0, entry.config.length);

            result.push(entry);

            nextId.set(response[4]);
            terminate.set(!(response[2] == DATA_PROCESSOR.id && response[3] == NOTIFY));

            return Task.forResult(null);
        })).onSuccessTask(ignored -> Task.forResult(result));
    }
}
