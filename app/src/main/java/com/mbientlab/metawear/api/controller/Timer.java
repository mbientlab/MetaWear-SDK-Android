package com.mbientlab.metawear.api.controller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;

import static com.mbientlab.metawear.api.Module.TIMER;

/**
 * Controller for the Timer module
 * @author Eric Tsai
 */
public interface Timer extends ModuleController {
    /**
     * Registers under the Timer module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Enables / Disables the module */
        ENABLE {
            @Override public byte opcode() { return 0x1; }
        },
        /** Timer configuration */
        TIMER_ENTRY {
            @Override public byte opcode() { return 0x2; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                if ((data[1] & 0x80) == 0x80) {
                    int period= ByteBuffer.wrap(data, 2, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    short repeat= ByteBuffer.wrap(data, 6, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
                    boolean delay= data[8] == 0;
                    
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedTimerConfig(period, repeat, delay);
                    }
                } else {
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedTimerId(data[2]);
                    }
                }
            }
        },
        /** Starts a timer */
        START {
            @Override public byte opcode() { return 0x3; }
        },
        /** Stops a timer */
        STOP {
            @Override public byte opcode() { return 0x4; }
        },
        /** Removes a timer from the board */
        REMOVE {
            @Override public byte opcode() { return 0x5; }
        },
        /** Receives timer notifications */
        TIMER_NOTIFY {
            @Override public byte opcode() { return 0x6; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedNotification(data[2]);
                }
            }
        },
        /** Enables / Disables notifications from a timer */
        TIMER_NOTIFY_ENABLE {
            @Override public byte opcode() { return 0x7; }
        };
        
        @Override public Module module() { return TIMER; }
        @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) { }
    }
    /**
     * Callbacks for the Timer module
     * @author Eric Tsai
     */
    public abstract static class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return TIMER; }
        
        /**
         * Called when the timer id has been received from the board
         * @param timerId ID for referring to the specific timer configuration passed in 
         * via {@link Timer#addTimer(int, short, boolean)}
         */
        public void receivedTimerId(byte timerId) { }
        /**
         * Called when a timer notification is received from the board
         * @param timerId Timer ID that is sending the notification
         */
        public void receivedNotification(byte timerId) { }
        /**
         * Called when timer configuration has been received from the board
         * @param period
         * @param repeat
         * @param delay
         */
        public void receivedTimerConfig(int period, short repeat, boolean delay) { }
    }
    
    /**
     * Add an onboard timer
     * @param period How often the timer should send a notification, in ms
     * @param repeat How many times to repeat the notification
     * @param delay True if the first notification should be delayed for one 
     * period worth of time
     */
    public void addTimer(int period, short repeat, boolean delay);
    /**
     * Start timer notifications 
     * @param timerId Timer ID to start
     */
    public void startTimer(byte timerId);
    /**
     * Stop timer notifications
     * @param timerId Timer ID to stop
     */
    public void stopTimer(byte timerId);
    /**
     * Remove a timer from the board
     * @param timerId Timer ID to remove
     */
    public void removeTimer(byte timerId);
    /**
     * Enables ble notifications for a timer.  This results in calls to the 
     * {@link Callbacks#receivedNotification(byte)} callback function.
     * @param timerId Timer ID to receive notifications from
     */
    public void enableNotification(byte timerId);
    /**
     * Disables calls to the {@link Callbacks#receivedNotification(byte)} callback function
     * @param timerId Timer ID to disable notifications from
     */
    public void disableNotification(byte timerId);
    
}
