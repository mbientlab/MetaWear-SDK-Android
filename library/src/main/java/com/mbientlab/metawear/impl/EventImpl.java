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
import com.mbientlab.metawear.MetaWearBoard.Module;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

import bolts.Task;
import bolts.TaskCompletionSource;

import static com.mbientlab.metawear.impl.Constant.Module.EVENT;

/**
 * Created by etsai on 10/26/16.
 */
class EventImpl extends ModuleImplBase implements Module {
    private static final byte ENTRY = 2, CMD_PARAMETERS = 3, REMOVE = 4, REMOVE_ALL = 5;
    private static final long serialVersionUID = 4582940681177602659L;

    transient Tuple3<Byte, Byte, Byte> feedbackParams= null;
    private transient ScheduledFuture<?> eventTimeoutFuture;
    private transient LinkedList<Byte> successfulEvents;
    private transient Queue<Pair<? extends DataTypeBase, ? extends CodeBlock>> pendingEventCodeBlocks;
    private transient TaskCompletionSource<LinkedList<Byte>> createEventsTask;
    private transient Queue<byte[]> recordedCommands;
    private transient boolean isRecording= false;
    private transient Runnable recordCmdTimeout;

    EventImpl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);
    }

    protected void init() {
        recordCmdTimeout = new Runnable() {
            @Override
            public void run() {
                for(Byte id: successfulEvents) {
                    removeEventCommand(id);
                }

                pendingEventCodeBlocks= null;
                recordedCommands= null;
                createEventsTask.setError(new TimeoutException("Timed out programming commands"));
            }
        };

        mwPrivate.addResponseHandler(new Pair<>(EVENT.id, ENTRY), new JseMetaWearBoard.RegisterResponseHandler() {
            @Override
            public void onResponseReceived(final byte[] response) {
                eventTimeoutFuture.cancel(false);

                successfulEvents.add(response[2]);

                if (!recordedCommands.isEmpty()) {
                    mwPrivate.sendCommand(recordedCommands.poll());
                    mwPrivate.sendCommand(recordedCommands.poll());

                    eventTimeoutFuture= mwPrivate.scheduleTask(recordCmdTimeout, 250L);
                } else {
                    pendingEventCodeBlocks.poll();

                    recordCommand();
                }
            }
        });
    }

    @Override
    public void tearDown() {
        mwPrivate.sendCommand(new byte[] {EVENT.id, EventImpl.REMOVE_ALL});
    }

    void removeEventCommand(byte id) {
        mwPrivate.sendCommand(new byte[]{EVENT.id, EventImpl.REMOVE, id});
    }

    Task<LinkedList<Byte>> queueEvents(Queue<Pair<? extends DataTypeBase, ? extends CodeBlock>> eventCodeBlocks) {
        successfulEvents = new LinkedList<>();
        pendingEventCodeBlocks = new LinkedList<>(eventCodeBlocks);
        createEventsTask= new TaskCompletionSource<>();
        recordCommand();

        return createEventsTask.getTask();
    }

    byte[] getEventConfig() {
        return isRecording ? pendingEventCodeBlocks.peek().first.eventConfig : null;
    }

    void convertToEventCommand(byte[] command) {
        byte[] eventConfig= getEventConfig();
        byte[] commandEntry= new byte[] {EVENT.id, EventImpl.ENTRY,
                eventConfig[0], eventConfig[1], eventConfig[2],
                command[0], command[1], (byte) (command.length - 2)};

        if (feedbackParams != null) {
            byte[] tempEntry= new byte[commandEntry.length + 2];
            System.arraycopy(commandEntry, 0, tempEntry, 0, commandEntry.length);
            tempEntry[commandEntry.length]= (byte) (0x01 | ((feedbackParams.first << 1) & 0xff) | ((feedbackParams.second << 4) & 0xff));
            tempEntry[commandEntry.length + 1]= feedbackParams.third;
            commandEntry= tempEntry;
        }
        recordedCommands.add(commandEntry);

        byte[] eventParameters= new byte[command.length];
        System.arraycopy(command, 2, eventParameters, 2, command.length - 2);
        eventParameters[0]= EVENT.id;
        eventParameters[1]= EventImpl.CMD_PARAMETERS;
        recordedCommands.add(eventParameters);
    }

    private void recordCommand() {
        if (!pendingEventCodeBlocks.isEmpty()) {
            isRecording= true;
            recordedCommands= new LinkedList<>();
            pendingEventCodeBlocks.peek().second.program();
            isRecording= false;

            mwPrivate.sendCommand(recordedCommands.poll());
            mwPrivate.sendCommand(recordedCommands.poll());
            eventTimeoutFuture= mwPrivate.scheduleTask(recordCmdTimeout, 250L);
        } else {
            createEventsTask.setResult(successfulEvents);
        }
    }
}
