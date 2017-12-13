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
import com.mbientlab.metawear.DataToken;
import com.mbientlab.metawear.Observer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.impl.JseMetaWearBoard.RegisterResponseHandler;
import com.mbientlab.metawear.module.Timer;

import java.util.Collection;
import java.util.Map;

import bolts.Task;

/**
 * Created by etsai on 8/31/16.
 */
interface MetaWearBoardPrivate {
    Task<Void> boardDisconnect();
    void sendCommand(byte[] command);
    void sendCommand(byte[] command, int dest, DataToken input);
    void sendCommand(Constant.Module module, byte register, byte ... parameters);
    void sendCommand(Constant.Module module, byte register, byte id, byte ... parameters);

    void tagProducer(String name, DataTypeBase producer);
    DataTypeBase lookupProducer(String name);
    boolean hasProducer(String name);
    void removeProducerTag(String name);

    ModuleInfo lookupModuleInfo(Constant.Module id);
    Collection<DataTypeBase> getDataTypes();
    Map<Class<? extends com.mbientlab.metawear.MetaWearBoard.Module>, com.mbientlab.metawear.MetaWearBoard.Module> getModules();
    void addDataIdHeader(Pair<Byte, Byte> key);
    void addDataHandler(Tuple3<Byte, Byte, Byte> key, RegisterResponseHandler handler);
    void addResponseHandler(Pair<Byte, Byte> key, RegisterResponseHandler handler);
    void removeDataHandler(Tuple3<Byte, Byte, Byte> key, RegisterResponseHandler handler);
    int numDataHandlers(Tuple3<Byte, Byte, Byte> key);

    void removeProcessor(boolean sync, byte id);
    void removeRoute(int id);
    void removeEventManager(int id);

    Task<Route> queueRouteBuilder(RouteBuilder builder, String producerTag);
    Task<Timer.ScheduledTask> queueTaskManager(CodeBlock mwCode, byte[] timerConfig);
    Task<Observer> queueEvent(DataTypeBase owner, CodeBlock codeBlock);

    void logWarn(String message);

    Version getFirmwareVersion();
}
