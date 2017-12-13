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
import com.mbientlab.metawear.impl.platform.TimedTask;
import com.mbientlab.metawear.module.Timer;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.TIMER;

/**
 * Created by etsai on 9/17/16.
 */
class TimerImpl extends ModuleImplBase implements Timer {
    private static final long serialVersionUID = -4333941915456769144L;

    private static final byte TIMER_ENTRY = 2,
        START = 3, STOP = 4, REMOVE = 5,
        NOTIFY = 6, NOTIFY_ENABLE = 7;

    private static class ScheduledTaskInner implements ScheduledTask, Serializable {
        private static final long serialVersionUID = -1777107827505055813L;

        private final byte id;
        private boolean active;
        private final LinkedList<Byte> eventCmdIds;

        private transient MetaWearBoardPrivate mwPrivate;

        ScheduledTaskInner(byte id, LinkedList<Byte> eventCmdIds, MetaWearBoardPrivate mwPrivate) {
            this.id= id;
            this.eventCmdIds = eventCmdIds;
            active= true;

            restoreTransientVars(mwPrivate);
        }

        void restoreTransientVars(MetaWearBoardPrivate mwPrivate) {
            this.mwPrivate= mwPrivate;
        }

        @Override
        public void start() {
            if (active) {
                mwPrivate.sendCommand(new byte[]{TIMER.id, TimerImpl.START, id});
            }
        }

        @Override
        public void stop() {
            if (active) {
                mwPrivate.sendCommand(new byte[]{TIMER.id, TimerImpl.STOP, id});
            }
        }

        void remove(boolean sync) {
            if (active) {
                active= false;

                if (sync) {
                    mwPrivate.sendCommand(new byte[]{TIMER.id, TimerImpl.REMOVE, id});
                    ((TimerImpl) mwPrivate.getModules().get(Timer.class)).activeTasks.remove(id);

                    EventImpl event = (EventImpl) mwPrivate.getModules().get(EventImpl.class);
                    for(Byte it: eventCmdIds) {
                        event.removeEventCommand(it);
                    }
                }
            }
        }

        @Override
        public void remove() {
            remove(true);
        }

        @Override
        public byte id() {
            return id;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }

    private final Map<Byte, ScheduledTask> activeTasks = new HashMap<>();
    private transient TimedTask<Byte> createTimerTask;

    TimerImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
    }

    @Override
    public void restoreTransientVars(MetaWearBoardPrivate mwPrivate) {
        super.restoreTransientVars(mwPrivate);

        for(ScheduledTask it: activeTasks.values()) {
            ((ScheduledTaskInner) it).restoreTransientVars(mwPrivate);
        }
    }

    @Override
    protected void init() {
        createTimerTask = new TimedTask<>();
        this.mwPrivate.addResponseHandler(new Pair<>(TIMER.id, TIMER_ENTRY), response -> createTimerTask.setResult(response[2]));
    }

    @Override
    public void tearDown() {
        for(ScheduledTask it: activeTasks.values()) {
            ((ScheduledTaskInner) it).remove(false);
        }
        activeTasks.clear();

        for(byte i = 0; i < mwPrivate.lookupModuleInfo(Constant.Module.TIMER).extra[0]; i++) {
            mwPrivate.sendCommand(new byte[] {TIMER.id, REMOVE, i});
        }
    }

    Task<DataTypeBase> create(byte[] config) {
        return createTimerTask.execute("Did not received timer id within %dms", Constant.RESPONSE_TIMEOUT,
                () -> mwPrivate.sendCommand(TIMER, TIMER_ENTRY, config)
        ).onSuccessTask(task -> Task.forResult(new UintData(TIMER, TimerImpl.NOTIFY, task.getResult(), new DataAttributes(new byte[] {}, (byte) 0, (byte) 0, false))));
    }

    @Override
    public Task<ScheduledTask> scheduleAsync(int period, boolean delay, CodeBlock mwCode) {
        return scheduleAsync(period, (short) -1, delay, mwCode);
    }

    @Override
    public Task<ScheduledTask> scheduleAsync(int period, short repetitions, boolean delay, CodeBlock mwCode) {
        byte[] config= ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(period)
                .putShort(repetitions)
                .put((byte) (delay ? 0 : 1))
                .array();

        return mwPrivate.queueTaskManager(mwCode, config);
    }

    @Override
    public ScheduledTask lookupScheduledTask(byte id) {
        return activeTasks.get(id);
    }

    ScheduledTask createTimedEventManager(byte id, LinkedList<Byte> eventCmdIds) {
        ScheduledTaskInner newTask = new ScheduledTaskInner(id, eventCmdIds, mwPrivate);
        activeTasks.put(id, newTask);
        return newTask;
    }
}
